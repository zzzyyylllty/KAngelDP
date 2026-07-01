package io.github.zzzyyylllty.kangeldungeon.util.stats

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 持久化玩家统计管理器 — 使用 SQLite 追踪跨地牢玩家统计。
 * 统计字段：completions, fails, mob_kills, deaths, time_played, last_played
 */
object PlayerStatsManager {

    private var connection: Connection? = null

    /** 内存缓存：playerUUID -> PlayerStats，加速频繁读取 */
    private val statsCache = ConcurrentHashMap<UUID, PlayerStats>()

    /** statsCache 最大条目数，超出时触发清理 */
    private const val STATS_CACHE_MAX_SIZE = 5000

    /** SQLite 连接锁（Connection 非线程安全） */
    private val dbLock = Any()

    /**
     * 玩家统计数据结构
     */
    data class PlayerStats(
        val playerUUID: UUID,
        var completions: Int = 0,
        var fails: Int = 0,
        var mobKills: Int = 0,
        var deaths: Int = 0,
        var timePlayed: Long = 0L,
        var lastPlayed: Long = 0L
    ) {
        /** 总游戏次数 */
        val totalGames: Int get() = completions + fails
        /** 通关率 */
        val completionRate: Double get() = if (totalGames > 0) completions.toDouble() / totalGames else 0.0
    }

    /**
     * 初始化数据库连接和表结构
     */
    fun initialize() {
        try {
            val dbFile = java.io.File(KAngelDungeon.dataFolder, "player_stats.db")
            connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            val stmt = connection!!.createStatement()
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS player_stats (
                    player_uuid TEXT PRIMARY KEY,
                    completions INTEGER NOT NULL DEFAULT 0,
                    fails INTEGER NOT NULL DEFAULT 0,
                    mob_kills INTEGER NOT NULL DEFAULT 0,
                    deaths INTEGER NOT NULL DEFAULT 0,
                    time_played INTEGER NOT NULL DEFAULT 0,
                    last_played INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            stmt.close()
            devLog("PlayerStatsManager initialized with SQLite database")
        } catch (e: Exception) {
            KAngelDungeon.console?.sendMessage("§4[KAngelDungeon] Failed to initialize player stats database: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 关闭数据库连接
     */
    fun shutdown() {
        try {
            connection?.close()
            statsCache.clear()
        } catch (_: Exception) {}
    }

    /** 清理缓存中最早的一半条目，防止无限增长 */
    private fun evictCacheIfNeeded() {
        if (statsCache.size > STATS_CACHE_MAX_SIZE) {
            val iter = statsCache.entries.iterator()
            var removed = 0
            val target = STATS_CACHE_MAX_SIZE / 2
            while (iter.hasNext() && removed < target) {
                iter.next()
                iter.remove()
                removed++
            }
        }
    }

    /**
     * 获取玩家统计（先查缓存，再查 DB）
     */
    fun getStats(playerUUID: UUID): PlayerStats {
        statsCache[playerUUID]?.let { return it }
        val stats = loadFromDatabase(playerUUID) ?: PlayerStats(playerUUID = playerUUID)
        statsCache[playerUUID] = stats
        evictCacheIfNeeded()
        return stats
    }

    /**
     * 从 DB 加载统计
     */
    private fun loadFromDatabase(playerUUID: UUID): PlayerStats? {
        val conn = connection ?: return null
        synchronized(dbLock) {
        try {
            conn.prepareStatement("SELECT * FROM player_stats WHERE player_uuid = ?").use { stmt ->
                stmt.setString(1, playerUUID.toString())
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        PlayerStats(
                            playerUUID = playerUUID,
                            completions = rs.getInt("completions"),
                            fails = rs.getInt("fails"),
                            mobKills = rs.getInt("mob_kills"),
                            deaths = rs.getInt("deaths"),
                            timePlayed = rs.getLong("time_played"),
                            lastPlayed = rs.getLong("last_played")
                        ).also { statsCache[playerUUID] = it }
                    } else null
                }
            }
        } catch (e: Exception) {
            devLog("Failed to load stats for $playerUUID: ${e.message}")
            return null
        }
        }
    }

    /**
     * 保存统计到 DB（同步写入）
     */
    private fun saveToDatabase(stats: PlayerStats) {
        val conn = connection ?: return
        synchronized(dbLock) {
        try {
            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO player_stats
                (player_uuid, completions, fails, mob_kills, deaths, time_played, last_played)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, stats.playerUUID.toString())
                stmt.setInt(2, stats.completions)
                stmt.setInt(3, stats.fails)
                stmt.setInt(4, stats.mobKills)
                stmt.setInt(5, stats.deaths)
                stmt.setLong(6, stats.timePlayed)
                stmt.setLong(7, stats.lastPlayed)
                stmt.executeUpdate()
            }
        } catch (e: Exception) {
            devLog("Failed to save stats for ${stats.playerUUID}: ${e.message}")
        }
        }
    }

    // ===== 统计更新方法 =====

    /**
     * 记录地牢通关
     */
    fun recordCompletion(playerUUID: UUID) {
        val stats = getStats(playerUUID)
        stats.completions++
        stats.lastPlayed = System.currentTimeMillis()
        saveToDatabase(stats)
    }

    /**
     * 记录地牢失败
     */
    fun recordFail(playerUUID: UUID) {
        val stats = getStats(playerUUID)
        stats.fails++
        stats.lastPlayed = System.currentTimeMillis()
        saveToDatabase(stats)
    }

    /**
     * 记录击杀
     */
    fun recordMobKill(playerUUID: UUID, count: Int = 1) {
        val stats = getStats(playerUUID)
        stats.mobKills += count
        saveToDatabase(stats)
    }

    /**
     * 记录死亡
     */
    fun recordDeath(playerUUID: UUID) {
        val stats = getStats(playerUUID)
        stats.deaths++
        saveToDatabase(stats)
    }

    /**
     * 更新游戏时间（秒）
     */
    fun recordTimePlayed(playerUUID: UUID, seconds: Long) {
        val stats = getStats(playerUUID)
        stats.timePlayed += seconds
        stats.lastPlayed = System.currentTimeMillis()
        saveToDatabase(stats)
    }

    /**
     * 在地牢完成/失败时批量更新统计
     * 注意：每位玩家记录从加入时刻到结束的实际游戏时间，而非 dungeon 全程
     */
    fun recordDungeonEnd(instance: DungeonInstance, success: Boolean) {
        val now = System.currentTimeMillis()
        val startedAt = instance.startedAt ?: return

        val playerJoinTimes = instance.playerJoinTimes
        for (uuid in instance.players) {
            val stats = getStats(uuid)
            if (success) stats.completions++ else stats.fails++
            val joinTime = playerJoinTimes[uuid] ?: startedAt
            stats.timePlayed += (now - joinTime) / 1000
            stats.lastPlayed = now
            saveToDatabase(stats)
        }
    }

    /**
     * 获取玩家总统计（从缓存或 DB）
     */
    fun getPlayerStats(playerUUID: UUID): PlayerStats = getStats(playerUUID)

    /**
     * 获取玩家总通关数
     */
    fun getCompletions(playerUUID: UUID): Int = getStats(playerUUID).completions

    /**
     * 获取玩家总失败数
     */
    fun getFails(playerUUID: UUID): Int = getStats(playerUUID).fails

    /**
     * 获取玩家总击杀数
     */
    fun getMobKills(playerUUID: UUID): Int = getStats(playerUUID).mobKills

    /**
     * 获取玩家总游戏时间（秒）
     */
    fun getTimePlayed(playerUUID: UUID): Long = getStats(playerUUID).timePlayed
}

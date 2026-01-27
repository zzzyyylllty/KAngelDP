package io.github.zzzyyylllty.kangeldungeon.data

import io.github.zzzyyylllty.kangeldungeon.util.DungeonHelper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地牢模板 - 定义地牢的静态配置信息
 */
data class DungeonTemplate(
    val name: String,
    val displayName: String = name,
    val description: List<String> = emptyList(),

    // 地图配置
    val schematicFile: String? = null, // Schematic文件路径
    val worldTemplate: String? = null, // 世界模板文件夹名
    val spawnPoint: Location, // 出生点（相对坐标）

    // 玩家限制
    val minPlayers: Int = 1,
    val maxPlayers: Int = 5,
    val requiredLevel: Int = 0,

    // 时间限制
    val timeLimit: Double? = 3600.0, // 秒
    val preparationTime: Double? = 30.0, // 准备时间（秒）

    // 其他设置
    val allowRespawn: Boolean = false,
    val keepInventory: Boolean = false,
    val pvpEnabled: Boolean = false,
    val naturalRegeneration: Boolean = true,

    // 权限要求
    val requiredPermission: String? = null,

    // 冷却时间（秒）
    val cooldown: Long = 0
) {

}

/**
 * 地牢实例 - 运行中的地牢副本
 */
data class DungeonInstance(
    val templateName: String,
    val uuid: UUID = UUID.randomUUID(),

    // 玩家管理
    val players: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
    val deadPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
    val leaderUUID: UUID,

    // 时间管理
    val createdAt: Long = System.currentTimeMillis(),
    var startedAt: Long? = null,
    var completedAt: Long? = null,

    // 状态管理
    var state: DungeonState = DungeonState.PREPARING,

    // 统计信息
    var meta: DungeonMeta,

    var mobsKilled: Int = 0,
    var bossesKilled: Int = 0,

    // 位置信息
    val spawnLocation: Location,
    val exitLocation: Location? = null,
) {

    /**
     * 获取Bukkit世界名称
     */
    val worldName: String
        get() = DungeonHelper.getWorldName(templateName, uuid)

    /**
     * 获取Bukkit世界对象
     */
    val world: World?
        get() = Bukkit.getWorld(DungeonHelper.getWorldName(templateName, uuid))

    /**
     * 获取Bukkit世界对象
     */
    fun getWorld(): World? = Bukkit.getWorld(DungeonHelper.getWorldName(templateName, uuid))

    /**
     * 获取在线玩家列表
     */
    fun getOnlinePlayers(): List<Player> {
        return players.mapNotNull { Bukkit.getPlayer(it) }
    }

    /**
     * 获取队长
     */
    fun getLeader(): Player? = Bukkit.getPlayer(leaderUUID)

    /**
     * 添加玩家
     */
    fun addPlayer(player: Player): Boolean {
        if (state != DungeonState.PREPARING && state != DungeonState.ACTIVE) {
            return false
        }
        return players.add(player.uniqueId)
    }

    /**
     * 移除玩家
     */
    fun removePlayer(player: Player) {
        players.remove(player.uniqueId)
    }

    /**
     * 玩家死亡
     */
    fun playerDied(player: Player) {
        deadPlayers.add(player.uniqueId)
        meta.add()
    }

    /**
     * 开始地牢
     */
    fun start() {
        if (state == DungeonState.PREPARING) {
            state = DungeonState.ACTIVE
            startedAt = System.currentTimeMillis()
        }
    }

    /**
     * 完成地牢
     */
    fun complete(success: Boolean) {
        state = if (success) DungeonState.COMPLETED else DungeonState.FAILED
        completedAt = System.currentTimeMillis()
    }

    /**
     * 获取已用时间（秒）
     */
    fun getElapsedTime(): Double {
        val start = startedAt ?: return 0.0
        val end = completedAt ?: System.currentTimeMillis()
        return (start - end).toDouble()/1000
    }

    /**
     * 获取剩余时间（秒）
     */
    fun getRemainingTime(template: DungeonTemplate): Double {
        val elapsed = getElapsedTime()
        return (template.timeLimit - elapsed).coerceAtLeast(0.0)
    }

    /**
     * 是否超时
     */
    fun isTimedOut(template: DungeonTemplate): Boolean {
        return getElapsedTime() >= template.timeLimit
    }

    /**
     * 所有玩家是否都死亡
     */
    fun areAllPlayersDead(): Boolean {
        return players.isNotEmpty() && players.all { it in deadPlayers }
    }

    /**
     * 增加击杀数
     */
    fun incrementMobKills() {
        mobsKilled++
        statistics.mobsKilled++
    }

    /**
     * 增加Boss击杀数
     */
    fun incrementBossKills() {
        bossesKilled++
        statistics.bossesKilled++
    }

    /**
     * 更新目标进度
     */
    fun updateObjective(objectiveId: String, progress: Int, required: Int) {
        objectives[objectiveId] = ObjectiveProgress(progress, required)
    }

    /**
     * 检查是否完成所有目标
     */
    fun areAllObjectivesCompleted(): Boolean {
        return objectives.values.all { it.isCompleted() }
    }
}

enum class DungeonState{
    PREPARING,
    ACTIVE,
    FAILED,
    COMPLETED
}

data class DungeonMeta(
    val meta: LinkedHashMap<String, Double> = LinkedHashMap(),
) {
    fun add(key: String, value: Any) {
        if (meta[key] == null) meta[key] = value.toDouble()
        else meta[key]?.plus(value.toDouble())
    }
}
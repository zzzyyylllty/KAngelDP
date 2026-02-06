package io.github.zzzyyylllty.kangeldungeon.data

import io.github.zzzyyylllty.kangeldungeon.util.CastHelper
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.Vector
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
    val schematicFile: String? = null, // Schematic文件
    val worldTemplate: String? = null, // 世界模板文件夹名
    val spawnPoint: Location, // 出生点（相对坐标）

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

    // 位置信息
    val spawnVector: Vector
) {

}

/**
 * 地牢实例 - 运行中的地牢副本
 * @param templateName 地牢模板名称
 * @param uuid 地牢的UUID
 * @param players 地牢玩家
 * @param deadPlayers 地牢死亡玩家
 * @param leaderUUID 地牢小队领导者
 * @param createdAt 创建时间
 * @param startedAt 开始时间
 * @param completedAt 完成时间
 * @param state 地牢状态
 * @param meta 地牢元数据
 * @param spawnLocation 地牢出生点
 * @param exitLocation 地牢退出位置
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

    // 位置信息
    val spawnLocation: Location
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
     * 获取在线地牢玩家列表
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
        meta.add("player.dead", 1)
        meta.add("player.dead.${player.name}", 1)
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
     * null代表没有时间限制
     */
    fun getRemainingTime(template: DungeonTemplate): Double? {
        val elapsed = getElapsedTime()
        return (template.timeLimit?.minus(elapsed))?.coerceAtLeast(0.0)
    }

    /**
     * 是否超时
     */
    fun isTimedOut(template: DungeonTemplate): Boolean {
        return template.timeLimit?.let { getElapsedTime() >= it } ?: false
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
        meta.add("mob.kill", 1)
    }

    /**
     * 增加特定怪击杀数
     */
    fun incrementMobKills(mobName: String) {
        meta.add("mob.kill", 1)
        meta.add("mob.kill.${mobName}", 1)
    }

    /**
     * 增加Boss击杀数
     * 不会同步增加怪物击杀数
     */
    fun incrementBossKills() {
        meta.add("boss.kill", 1)
    }

    /**
     * 增加特定Boss击杀数
     * 不会同步增加怪物击杀数
     */
    fun incrementBossKills(mobName: String) {
        meta.add("boss.kill", 1)
        meta.add("boss.kill.${mobName}", 1)
    }
}

enum class DungeonState{
    PREPARING,
    ACTIVE,
    FAILED,
    COMPLETED
}

/**
 * 地牢统计信息及元数据
 * player.dead 所有玩家死亡次数
 * player.dead.<玩家名称> 特定玩家死亡次数
 * boss.kill BOSS击杀数
 * boss.kill.<BOSS名称> BOSS击杀数
 * mob.kill 生物击杀
 * mob.kill.<mob> 生物击杀
 */
data class DungeonMeta(
    val meta: LinkedHashMap<String, Any?> = LinkedHashMap(),
) {
    fun add(key: String, value: Any?) {
        if (meta[key] == null) meta[key] = value
        else meta[key] = CastHelper.increaseAny(meta[key], value)
    }
    fun set(key: String, value: Any?) {
        meta[key] = value
    }
    fun get(key: String): Any? {
        return meta[key]
    }
    fun getAsDouble(key: String): Double? {
        return meta[key]?.toString()?.toDouble()
    }
    fun getAsInt(key: String): Int? {
        return meta[key]?.toString()?.toInt()
    }
    fun getAsLong(key: String): Long? {
        return meta[key]?.toString()?.toLong()
    }
    fun getAsString(key: String): String? {
        return meta[key].toString()
    }
}
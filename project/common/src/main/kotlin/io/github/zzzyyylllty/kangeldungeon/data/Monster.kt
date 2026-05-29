package io.github.zzzyyylllty.kangeldungeon.data

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Monster config - corresponds to root keys in monster YAML files
 */
data class MonsterConfig(
    val id: String,
    val monsters: List<MonsterSpawnEntry> = emptyList(),
    val agent: MonsterAgent? = null,
    // 激活控制
    val active: Boolean = true,
    // 重新生成冷却（tick），0 = 不自动重生
    val respawnCooldown: Long = 0,
    // 最小激活距离（方块），0 = 不限。玩家距离小于此值时不激活
    val activationRangeMin: Double = 0.0,
    // 最大激活距离（方块），-1 = 不限。玩家距离大于此值时不激活
    val activationRangeMax: Double = -1.0,
    // JS 条件表达式，返回 true 才允许生成（为空则不检查）
    val spawnCondition: String? = null,
    // 生成延迟（tick），地牢开始后延迟指定 tick 才自动生成，0 = 不延迟
    val spawnDelay: Long = 0,
    // 最大重生次数（-1 = 无限，0 = 不重生，N = 最多 N 次）
    val maxRespawns: Int = -1,
    // 重生条件（JS），与 spawnCondition 独立，仅在重试时检查
    val respawnCondition: String? = null,
    // 拴绳范围（方块），0 = 不限。怪物超出此距离将被拉回生成点
    val leashRange: Double = 0.0,
    // 生命倍率（1.0 = 默认）
    val healthMultiplier: Double = 1.0,
    // 伤害倍率（1.0 = 默认）
    val damageMultiplier: Double = 1.0,
    // 生成间隔（tick），同一组内每个怪物依次生成的间隔，0 = 同时全部生成
    val spawnInterval: Long = 0,
    // 优先级，数值越高越先生成（多个组同时触发时）
    val priority: Int = 0
)

data class MonsterSpawnEntry(
    val mob: String,
    val location: MonsterLocation,
    val amount: Int = 1,
    val scattered: Int = 0,
    val level: Int = 0
)

data class MonsterLocation(
    val x: Double,
    val y: Double,
    val z: Double
)

data class MonsterAgent(
    val onSpawn: String? = null,
    val onAllKilled: String? = null,
    val onRespawn: String? = null,  // 重生时触发
    val onEachKill: String? = null   // 每击杀一个怪物时触发
)

/**
 * Runtime monster group instance tracking state per dungeon
 */
data class MonsterInstance(
    val config: MonsterConfig,
    val dungeonInstance: DungeonInstance,
    val spawnedMobs: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
    var allKilled: Boolean = false,
    // 运行时激活状态（可 JS 动态切换）
    @Volatile
    var active: Boolean = config.active,
    // 运行时冷却覆盖（可 JS 修改），-1 = 使用 config 值
    @Volatile
    var respawnCooldownTicks: Long = -1,
    // 运行时最小激活距离覆盖（可 JS 修改），null = 使用 config 值
    @Volatile
    var activationRangeMin: Double? = null,
    // 运行时最大激活距离覆盖（可 JS 修改），null = 使用 config 值
    @Volatile
    var activationRangeMax: Double? = null,
    // 上次重生时间（tick）
    @Volatile
    var lastRespawnTime: Long = 0,
    // 已重生次数
    @Volatile
    var respawnCount: Int = 0
)

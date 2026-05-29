package io.github.zzzyyylllty.kangeldungeon.data

import org.bukkit.util.Vector

/**
 * 图标配置
 */
data class IconConfig(
    val material: String = "DIAMOND_SWORD",
    val parameters: Map<String, Any?> = emptyMap()
)

/**
 * keepInventory 配置
 */
data class KeepInventoryConfig(
    val enabled: Boolean = false,
    val requiredLives: Boolean = false
)

/**
 * 命令模式
 */
enum class CommandMode {
    BLACKLIST, WHITELIST
}

/**
 * 方块控制模式
 */
enum class BlockControlMode {
    BLACKLIST, WHITELIST
}

/**
 * 方块放置/破坏控制配置
 */
data class BlockControlConfig(
    val mode: BlockControlMode = BlockControlMode.BLACKLIST,
    val list: List<String> = emptyList()
)

/**
 * 命令配置
 */
data class CommandConfig(
    val mode: CommandMode = CommandMode.BLACKLIST,
    val list: List<String> = emptyList()
)

/**
 * 计时器模式
 */
enum class TimerMode {
    COUNTDOWN, STOPWATCH
}

/**
 * 计时器配置
 */
data class TimerConfig(
    val mode: TimerMode = TimerMode.COUNTDOWN,
    val start: Int = 600
)

/**
 * 自然恢复配置
 */
data class HealthRegainConfig(
    val food: Boolean = false,
    val saturation: Boolean = false,
    val potions: Boolean = false,
    val other: Boolean = false
) {
    /**
     * 是否有任何恢复方式启用
     */
    val isAnyEnabled: Boolean
        get() = food || saturation || potions || other
}

/**
 * 原版游戏选项配置
 */
data class VanillaOptions(
    val hungry: Boolean = true,
    val healthRegain: HealthRegainConfig = HealthRegainConfig(),
    val durability: Boolean = true,
    val itemsDrop: Boolean = true,
    val itemsPickup: Boolean = true,
    val spawnpoint: Vector? = null
)

/**
 * 死亡模式
 */
enum class DeathMode {
    /** 等待复活（默认），尸体停留原地，等待脚本触发 respawnPlayer 或自动复活 */
    RESPAWN,
    /** 旁观模式，玩家死后进入旁观者模式，可自由飞行观看 */
    SPECTATE,
    /** 强制附身观看，死后自动附身到随机存活队友视角 */
    POSSESS,
    /** 直接退出地牢，传送回进入前的位置 */
    LEAVE
}

/**
 * 死亡配置
 */
data class DeathConfig(
    val mode: DeathMode = DeathMode.RESPAWN,
    /** 最大复活次数，-1 表示无限制 */
    val maxRespawns: Int = 0,
    /** 自动复活延迟（秒），0 表示不自动复活，需手动触发 */
    val autoRespawnDelay: Int = 0,
    /** 复活时是否保留背包（覆盖 global keepInventory） */
    val keepInventoryOnRespawn: Boolean = false,
    /** 复活后是否传送回出生点 */
    val respawnAtSpawn: Boolean = true
)

/**
 * 通用游戏设置
 */
data class GameplayGeneralConfig(
    @Deprecated("Use death.mode == LEAVE instead", ReplaceWith("death.mode == DeathMode.LEAVE"))
    val leaveOnDeath: Boolean = false,
    val adventureMode: Boolean = true,
    val minPlayers: Int = 1,
    val maxPlayers: Int = 5,
    val allowParty: Boolean = true,
    val keepInventory: KeepInventoryConfig = KeepInventoryConfig(),
    val bannedItems: List<String> = emptyList(),
    val blockPlace: BlockControlConfig = BlockControlConfig(),
    val blockBreak: BlockControlConfig = BlockControlConfig(),
    val death: DeathConfig = DeathConfig()
)

/**
 * 地牢元数据模板配置
 */
data class MetaConfig(
    val global: Map<String, Any?> = emptyMap(),
    val player: Map<String, Any?> = emptyMap()
)

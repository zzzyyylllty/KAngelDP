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
    val list: Set<String> = emptySet()
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

// ==================== 扩展配置（option.yml 新增） ====================

/**
 * 加入地牢要求配置
 */
data class JoinRequirementsConfig(
    val minLevel: Int = 0,
    val requiredPermissions: List<String> = emptyList(),
    val requiredItems: List<RequiredItemConfig> = emptyList(),
    val requiredMoney: Double = 0.0
)

data class RequiredItemConfig(
    val material: String,
    val amount: Int = 1,
    val take: Boolean = false
)

/**
 * 视觉效果配置（地牢生命周期各阶段的标题/声音）
 */
data class VisualEffectsConfig(
    val startTitle: String? = null,
    val startSubtitle: String? = null,
    val startSound: SoundOption? = null,
    val completeTitle: String? = null,
    val completeSubtitle: String? = null,
    val completeSound: SoundOption? = null,
    val failTitle: String? = null,
    val failSubtitle: String? = null,
    val failSound: SoundOption? = null
)

data class SoundOption(
    val sound: String? = null,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
)

/**
 * 环境控制配置
 */
data class EnvironmentConfig(
    val allowFlight: Boolean? = null,
    val gameMode: String? = null,
    val flySpeed: Float? = null,
    val walkSpeed: Float? = null,
    val potionEffects: List<PotionEffectOption> = emptyList(),
    val worldBorder: WorldBorderOption? = null,
    val timeLock: Long? = null,
    val weatherLock: String? = null
)

data class PotionEffectOption(
    val type: String,
    val amplifier: Int = 0,
    val duration: Int = 30
)

data class WorldBorderOption(
    val size: Double = 256.0,
    val centerX: Double = 0.0,
    val centerZ: Double = 0.0
)

/**
 * 通关奖励配置
 */
data class RewardsConfig(
    val completeCommands: List<String> = emptyList(),
    val failCommands: List<String> = emptyList(),
    val completeItems: List<RewardItemOption> = emptyList(),
    val completeMoney: Double = 0.0,
    val completeExperience: Int = 0,
    val perPlayer: Boolean = true
)

data class RewardItemOption(
    val material: String,
    val amount: Int = 1
)

/**
 * 最大死亡操作
 */
enum class MaxDeathAction {
    SPECTATE, LOBBY, KICK
}

/**
 * 杂项配置
 */
data class MiscConfig(
    val joinWhileRunning: Boolean = false,
    val maxDeaths: Int = -1,
    val kickOnMaxDeaths: MaxDeathAction = MaxDeathAction.SPECTATE,
    val titleJoin: String? = null,
    val titleLeave: String? = null,
    /** 断线重连超时（秒），0=禁用重连，默认5分钟 */
    val reconnectTimeout: Int = 300
)

/**
 * 玩家放置方块配置
 */
data class PlayerBlocksConfig(
    val trackPlaced: Boolean = false,
    val clearOnEnd: Boolean = false,
    val maxBlocksPerPlayer: Int = -1
)

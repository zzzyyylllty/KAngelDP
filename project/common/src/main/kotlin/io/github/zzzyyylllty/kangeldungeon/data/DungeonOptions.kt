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

// ==================== BossBar 配置（option.yml） ====================

/**
 * BossBar 颜色
 */
enum class BossBarColorOption {
    BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
}

/**
 * BossBar 样式
 */
enum class BossBarStyleOption {
    SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
}

/**
 * 计时器 BossBar 配置
 */
data class TimerBossBarConfig(
    val enabled: Boolean = false,
    val color: BossBarColorOption = BossBarColorOption.WHITE,
    val style: BossBarStyleOption = BossBarStyleOption.SOLID,
    val title: String = "<gold>⏱ %time% | 存活: %alive%/%total%</gold>",
    val prepColor: BossBarColorOption = BossBarColorOption.GREEN,
    val prepStyle: BossBarStyleOption = BossBarStyleOption.SOLID,
    val prepTitle: String = "<green>准备中: %time%</green>",
    val completeColor: BossBarColorOption = BossBarColorOption.BLUE,
    val completeStyle: BossBarStyleOption = BossBarStyleOption.SOLID,
    val completeTitle: String = "<blue>✔ 通关! %time%</blue>",
    val failColor: BossBarColorOption = BossBarColorOption.RED,
    val failStyle: BossBarStyleOption = BossBarStyleOption.SOLID,
    val failTitle: String = "<red>✘ 失败</red>"
)

/**
 * Boss 血量 BossBar 配置（配合怪物组的 boss: true）
 */
data class BossHealthBarConfig(
    val enabled: Boolean = false,
    val color: BossBarColorOption = BossBarColorOption.RED,
    val style: BossBarStyleOption = BossBarStyleOption.SOLID,
    val title: String = "<red>%boss_name% %hp%/%max_hp%</red>"
)

/**
 * BossBar 总配置
 */
data class BossBarConfig(
    val timer: TimerBossBarConfig = TimerBossBarConfig(),
    val bossHealth: BossHealthBarConfig = BossHealthBarConfig()
)

// ==================== 计分板配置（option.yml） ====================

/**
 * 单行计分板配置
 */
data class ScoreboardLine(
    val text: String = "",
    /** 使用 PAPI 变量（如 %player_name%）而非仅内部变量 */
    val usePapi: Boolean = false
)

/**
 * 计分板配置
 */
data class ScoreboardConfig(
    val enabled: Boolean = false,
    val title: String = "<gold>KAngelDungeon</gold>",
    val lines: List<ScoreboardLine> = defaultScoreboardLines(),
    /** 更新间隔（tick），默认 20tick = 1秒 */
    val updateInterval: Int = 20
)

fun defaultScoreboardLines(): List<ScoreboardLine> = listOf(
    ScoreboardLine(""),
    ScoreboardLine(" <yellow>地牢:</yellow> %dungeon_name%"),
    ScoreboardLine(" <yellow>状态:</yellow> %state%"),
    ScoreboardLine(" <yellow>时间:</yellow> %time%"),
    ScoreboardLine(""),
    ScoreboardLine(" <yellow>存活:</yellow> %alive%<gray>/</gray>%total%"),
    ScoreboardLine(" <yellow>击杀:</yellow> %kills%"),
    ScoreboardLine(""),
    ScoreboardLine("<gray>play.example.com</gray>")
)

// ==================== 战利品箱配置（loot/ 目录） ====================

/**
 * 战利品箱物品项
 */
data class LootChestItem(
    val material: String,
    val amount: Int = 1,
    /** 概率 0.0 ~ 1.0，不填或 -1 则表示权重模式 */
    val chance: Double = -1.0,
    /** 权重（chance 为 -1 时使用权重随机） */
    val weight: Int = 1,
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    /** 附魔列表，格式: "SHARPNESS:3" */
    val enchantments: List<String> = emptyList(),
    /** 自定义 NBT JSON */
    val nbt: String? = null
)

/**
 * 战利品箱配置
 */
data class LootChestConfig(
    val id: String,
    /** 世界坐标列表，支持 "x y z" 格式 */
    val positions: List<String> = emptyList(),
    /** 每次地牢是否重新填充 */
    val refresh: Boolean = true,
    /** 最少物品数 */
    val minItems: Int = 1,
    /** 最多物品数 */
    val maxItems: Int = 3,
    /** 物品池 */
    val items: List<LootChestItem> = emptyList(),
    /** 关联的 LithiumCarbon frame-crate 配置 ID（不为空时额外生成展示框物资箱） */
    val frameCrate: String? = null,
    /** 展示框朝向（如 NORTH, SOUTH, EAST, WEST, UP），默认 UP */
    val frameCrateFacing: String = "UP"
)

// ==================== 难度动态缩放配置（option.yml） ====================

/**
 * 难度动态缩放配置
 */
data class DifficultyScalingConfig(
    val enabled: Boolean = false,
    /** 基准人数（不缩放） */
    val basePlayers: Int = 3,
    /** 额外每人增加的生命倍率 */
    val healthMultiplierPerExtra: Double = 0.2,
    /** 额外每人增加的伤害倍率 */
    val damageMultiplierPerExtra: Double = 0.1,
    /** 少每人降低的生命倍率 */
    val healthMultiplierPerLess: Double = 0.15,
    /** 少每人降低的伤害倍率 */
    val damageMultiplierPerLess: Double = 0.1
)

// ==================== 地牢聊天配置（config.yml / option.yml） ====================

/**
 * 地牢聊天配置
 */
data class DungeonChatConfig(
    val enabled: Boolean = false,
    /** 聊天格式，支持 %player% %message% %dungeon% */
    val format: String = "<gray>[<red>Dungeon</red>]</gray> <yellow>%player%</yellow><gray>:</gray> %message%",
    /** 是否自动转发地牢内普通聊天 */
    val autoRoute: Boolean = true,
    /** 命令别名 */
    val commandAlias: String = "dchat"
)

package io.github.zzzyyylllty.kangeldungeon.data

/**
 * Obstacle config - corresponds to root keys in obstacle YAML files
 */
data class ObstacleConfig(
    val id: String,
    val agent: ObstacleAgent? = null,
    val openDelaySeconds: Double = 3.0,
    val activeDurationSeconds: Double = 10.0,
    val openingAnimation: ObstacleAnimation? = null,
    val closingAnimation: ObstacleAnimation? = null,
    val obstacles: Map<String, GateObstacle> = emptyMap()
)

data class ObstacleAgent(
    val onPrepare: String? = null,
    val onStart: String? = null
)

data class ObstacleAnimation(
    val enabled: Boolean = true,
    val particle: String? = null,
    val particleCount: Int = 10,
    val sound: String? = null,
    val volume: Double = 1.0,
    val pitch: Double = 1.0,
    val durationTicks: Int = 20,
    val intervalTicks: Int = 2
)

data class GateObstacle(
    val id: String,
    val mode: String = "RESTORE_BLOCKS",
    val blocks: Map<String, BlockPos>? = null,
    val cuboid: CuboidDef? = null,
    val sequentialConfig: SequentialConfig? = null
)

data class BlockPos(
    val x: Int,
    val y: Int,
    val z: Int
)

data class CuboidDef(
    val pos1: BlockPos,
    val pos2: BlockPos
)

data class SequentialConfig(
    val enabled: Boolean = true,
    val openDirection: String = "LEFT_TO_RIGHT",
    val reverseOnClose: Boolean = true,
    val blocksPerStep: Int = 1,
    val stepDelayTicks: Int = 2,
    val openEffect: EffectConfig? = null,
    val closeEffect: EffectConfig? = null
)

data class EffectConfig(
    val enabled: Boolean = true,
    val particle: String? = null,
    val count: Int = 8
)

/**
 * Runtime obstacle instance tracking state per dungeon
 */
data class ObstacleInstance(
    val config: ObstacleConfig,
    val dungeonInstance: DungeonInstance,
    var state: ObstacleState = ObstacleState.PREPARING,
    var activatedAt: Long? = null
)

enum class ObstacleState {
    PREPARING,
    ACTIVE,
    OPEN,
    CLOSED
}

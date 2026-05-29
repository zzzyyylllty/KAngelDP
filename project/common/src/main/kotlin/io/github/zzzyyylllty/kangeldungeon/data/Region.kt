package io.github.zzzyyylllty.kangeldungeon.data

/**
 * Region config — corresponds to root keys in region YAML files
 *
 * YAML format:
 *   regionName:
 *     from: "x1 y1 z1"
 *     to: "x2 y2 z2"
 *     agent:
 *       onEnter: |-
 *         player.sendMessage("You entered region " + region.name)
 *       onLeave: |-
 *         player.sendMessage("You left region " + region.name)
 */
data class RegionConfig(
    val id: String,
    val from: RegionPos,
    val to: RegionPos,
    val agent: RegionAgent? = null
)

data class RegionPos(
    val x: Int,
    val y: Int,
    val z: Int
)

data class RegionAgent(
    val onEnter: String? = null,
    val onLeave: String? = null
)

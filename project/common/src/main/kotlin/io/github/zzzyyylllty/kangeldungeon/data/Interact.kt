package io.github.zzzyyylllty.kangeldungeon.data

/**
 * Interact config — corresponds to root keys in dungeon interact YAML files
 */
data class InteractConfig(
    val id: String,
    val pos: String,
    val agent: InteractAgent? = null
)

data class InteractAgent(
    val onActive: String? = null,
    val onPost: String? = null
)

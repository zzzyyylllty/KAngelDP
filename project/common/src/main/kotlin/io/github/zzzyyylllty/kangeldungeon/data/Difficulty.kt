package io.github.zzzyyylllty.kangeldungeon.data

/**
 * 难度配置 - 对应 dungeon/<name>/difficulty.yml
 *
 * 不同难度可以拥有不同的起始 meta 和生命周期脚本。
 */
data class DifficultyConfig(
    val id: String,
    val display: String = id,
    val description: String = "",
    // 该难度下的起始 meta 覆盖（合并到 DungeonInstance.meta）
    val meta: Map<String, Any?> = emptyMap(),
    // 该难度下的代理脚本（trigger -> JS），与 option.yml 的 agent 独立，在对应阶段额外执行
    val agents: Map<String, String> = emptyMap()
)

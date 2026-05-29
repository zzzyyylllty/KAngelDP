package io.github.zzzyyylllty.kangeldungeon.data

/**
 * 地牢任务配置 - 监听地牢内事件并执行 JS 脚本
 */
data class TaskConfig(
    val id: String,
    val trigger: String,                          // 触发器类型: MOB_KILL, PLAYER_DEATH, PLAYER_JOIN, PLAYER_LEAVE, DUNGEON_START, DUNGEON_COMPLETE, DUNGEON_FAIL, REGION_ENTER, REGION_LEAVE, MONSTER_GROUP_CLEAR, MONSTER_SPAWN, CUSTOM
    val filters: Map<String, String> = emptyMap(), // 过滤条件，如 mob_type: SKELETON, config_id: postironbars
    val agent: TaskAgent? = null,                  // 触发时执行的脚本
    val maxExecutions: Int = -1,                   // 最大执行次数，-1 无限制
    val cooldown: Long = 0                         // 冷却时间（tick）
)

data class TaskAgent(
    val onTrigger: String?                         // 触发时执行的 JS 代码
)

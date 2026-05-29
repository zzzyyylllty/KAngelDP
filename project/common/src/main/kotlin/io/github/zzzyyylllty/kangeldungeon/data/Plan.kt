package io.github.zzzyyylllty.kangeldungeon.data

/**
 * 地牢计划 - 定时执行的 JS 脚本任务
 * 通过 dungeon/<id>/plan/ 目录下的 YAML 文件定义
 *
 * YAML 格式:
 *   planName:
 *     trigger: BEGIN       # PREPARE / BEGIN / END / FAIL
 *     delay: 200           # 首次执行延迟（tick，20 tick = 1秒）
 *     period: 200          # 重复执行间隔（tick），null 表示只执行一次
 *     async: false         # 是否异步执行
 *     agent:
 *       onRun: |-          # 要执行的 JS 脚本
 *         instance.sendMessageToAllPlayers("hello");
 */
data class Plan(
    val name: String,
    val trigger: String,
    val delay: Int? = null,
    val period: Int? = null,
    val async: Boolean = false,
    val onRun: String? = null
)

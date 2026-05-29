package io.github.zzzyyylllty.kangeldungeon.data

/**
 * 奖励类型枚举
 */
enum class RewardType {
    /** 给予物品（支持多物品源，如 minecraft / craftengine / itemsadder） */
    ITEM,
    /** 执行控制台命令 */
    COMMAND,
    /** 执行 JS 脚本 */
    SCRIPT,
    /** 执行 Agent 脚本（带预定义触发器上下文） */
    AGENT
}

/**
 * 单个奖励配置
 *
 * 支持两种模式:
 * - weight模式（默认）: 所有奖励按权重从池中抽取 min_rewards~max_rewards 个
 * - chance模式: 每个奖励独立按 chance% 概率判定，通过则固定给予
 *
 * 同一Kit内的所有奖励应使用相同模式（混用会导致歧义，此时优先按chance处理）
 */
data class KitReward(
    val type: RewardType,
    val weight: Int = 1,
    /** chance模式专用：百分比概率 (0-100)，设置后该奖励独立判定 */
    val chance: Int? = null,
    // ITEM 类型参数
    val source: String? = null,
    val item: String? = null,
    val amount: Int = 1,
    val parameters: Map<String, Any?>? = null,
    val components: Map<String, Any?>? = null,
    // COMMAND 类型参数
    val command: String? = null,
    // SCRIPT 类型参数
    val script: String? = null,
    // AGENT 类型参数
    val agentTrigger: String? = null,
    val agentScript: String? = null,
    /** 获得该奖励时发送给玩家的消息（MiniMessage格式） */
    val message: String? = null
)

/**
 * Kit（礼包/奖励包）配置
 *
 * 两种模式:
 * - weight（默认）: 从奖励池按权重抽取 min_rewards~max_rewards 个
 * - chance: 每个奖励独立按 chance% 概率判定，通过则固定获得
 *
 * 对应 YAML 格式:
 * ```yaml
 * # weight模式
 * my_kit:
 *   rewards:
 *     - type: item
 *       source: minecraft
 *       item: diamond
 *       weight: 50
 *   min_rewards: 1
 *   max_rewards: 2
 *
 * # chance模式
 * my_kit:
 *   rewards:
 *     - type: item
 *       source: minecraft
 *       item: diamond
 *       chance: 30    # 30% 独立概率
 * ```
 */
data class KitConfig(
    val name: String,
    val displayName: String? = null,
    val icon: String? = null,
    val rewards: List<KitReward>,
    val minRewards: Int = 1,
    val maxRewards: Int = 1,
    /** 每个玩家的冷却时间（秒），0 表示无冷却 */
    val cooldown: Int = 0,
    /** Kether 条件表达式列表，全部通过才能打开 */
    val conditions: List<String>? = null,
    /** 全服广播消息（MiniMessage格式，支持 %player% / %kit% 占位符） */
    val broadcastMessage: String? = null,
    /** 消息映射: open / cooldown / condition_fail */
    val messages: Map<String, String>? = null
) {
    /** 是否为 chance 模式（任一奖励设置了 chance 则视为 chance 模式） */
    val isChanceMode: Boolean get() = rewards.any { it.chance != null }
}

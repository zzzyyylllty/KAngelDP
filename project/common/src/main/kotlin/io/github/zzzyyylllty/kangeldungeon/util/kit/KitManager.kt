package io.github.zzzyyylllty.kangeldungeon.util.kit

import io.github.zzzyyylllty.kangeldungeon.data.KitReward
import io.github.zzzyyylllty.kangeldungeon.data.RewardType
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Kit（奖励包）管理器
 *
 * 提供加权随机抽奖、奖励执行等功能，供 DungeonInstance.openKit() 调用。
 */
object KitManager {

    /**
     * 根据权重随机抽取指定数量的奖励（weight模式）
     * @param rewards 候选奖励列表
     * @param count 抽取数量（不超过 rewards.size）
     * @return 抽取到的奖励列表（不重复）
     */
    fun rollRewards(rewards: List<KitReward>, count: Int): List<KitReward> {
        if (rewards.isEmpty() || count <= 0) return emptyList()

        if (count >= rewards.size) return rewards.toList()

        val result = mutableListOf<KitReward>()
        val pool = rewards.toMutableList()
        var totalWeight = pool.sumOf { it.weight.coerceAtLeast(1) }

        repeat(count.coerceAtMost(rewards.size)) {
            if (totalWeight <= 0) return@repeat
            var roll = Random.nextInt(totalWeight)
            for ((index, reward) in pool.withIndex()) {
                roll -= reward.weight.coerceAtLeast(1)
                if (roll < 0) {
                    result.add(reward)
                    totalWeight -= reward.weight.coerceAtLeast(1)
                    pool.removeAt(index)
                    break
                }
            }
        }

        return result
    }

    /**
     * 按独立概率判定奖励（chance模式）
     * 每个奖励独立按 chance% 概率判定，通过的奖励全部执行
     * @param rewards 候选奖励列表
     * @return 判定通过的奖励列表
     */
    fun rewardsByChance(rewards: List<KitReward>): List<KitReward> {
        return rewards.filter { reward ->
            val chance = reward.chance?.coerceIn(0, 100) ?: 0
            chance > 0 && Random.nextInt(100) < chance
        }
    }

    /**
     * 执行单条奖励
     * @param reward 奖励配置
     * @param player 目标玩家
     * @param instance 地牢实例（可能为 null，此时 SCRIPT/AGENT 类型无法执行）
     * @return 是否成功执行
     */
    fun executeReward(reward: KitReward, player: Player, instance: Any? = null): Boolean {
        val success = try {
            when (reward.type) {
                RewardType.ITEM -> executeItemReward(reward, player)
                RewardType.COMMAND -> executeCommandReward(reward, player)
                RewardType.SCRIPT -> executeScriptReward(reward, player, instance)
                RewardType.AGENT -> executeAgentReward(reward, player, instance)
            }
        } catch (e: Exception) {
            warningL("KitRewardExecutionFailed", reward.type.name, player.name, e.message ?: "Unknown")
            false
        }
        if (success) {
            reward.message?.let { player.sendMessage(MiniMessage.miniMessage().deserialize(it)) }
        }
        return success
    }

    /**
     * 执行 ITEM 类型奖励
     */
    private fun executeItemReward(reward: KitReward, player: Player): Boolean {
        val source = reward.source ?: run {
            warningL("KitRewardMissingSource", player.name)
            return false
        }
        val itemId = reward.item ?: run {
            warningL("KitRewardMissingItem", player.name)
            return false
        }

        val itemStack = if (source.equals("minecraft", ignoreCase = true) ||
            source.equals("mc", ignoreCase = true) ||
            source.equals("vanilla", ignoreCase = true)
        ) {
            OmniItemParser.parseMinecraftItem(source, itemId, reward.parameters, reward.components, reward.amount)
        } else {
            OmniItemParser.parseExternalItem(source, itemId, player, reward.parameters, reward.components, reward.amount)
        }

        itemStack?.let {
            player.inventory.addItem(it).values.forEach { leftover ->
                player.world.dropItem(player.location, leftover)
            }
            return true
        }
        return false
    }

    /**
     * 执行 COMMAND 类型奖励
     */
    private fun executeCommandReward(reward: KitReward, player: Player): Boolean {
        val command = reward.command ?: return false
        val parsed = command.replace("%player%", player.name)
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed)
    }

    /**
     * 执行 SCRIPT 类型奖励
     */
    private fun executeScriptReward(reward: KitReward, player: Player, instance: Any?): Boolean {
        val script = reward.script ?: return false
        val vars = mutableMapOf<String, Any?>("player" to player)
        if (instance != null) vars["instance"] = instance
        GraalJsUtil.cachedEval(script, vars)
        return true
    }

    /**
     * 执行 AGENT 类型奖励
     */
    private fun executeAgentReward(reward: KitReward, player: Player, instance: Any?): Boolean {
        val script = reward.agentScript ?: return false
        val trigger = reward.agentTrigger ?: "onKitReward"
        val vars = mutableMapOf<String, Any?>("player" to player)
        if (instance != null) vars["instance"] = instance
        GraalJsUtil.cachedEval(script, vars)
        return true
    }

    /**
     * 检查玩家是否在Kit冷却中
     * @return 剩余冷却毫秒数，null 表示不在冷却中
     */
    fun checkCooldown(player: Player, dungeonName: String, kitName: String): Long? {
        val cooldownId = "kit.$dungeonName.$kitName"
        if (io.github.zzzyyylllty.kangeldungeon.data.DataUtil.isInCooldown(player, cooldownId)) {
            return io.github.zzzyyylllty.kangeldungeon.data.DataUtil.getCooldownLeftLong(player, cooldownId)
        }
        return null
    }

    /**
     * 为玩家应用Kit冷却
     */
    fun applyCooldown(player: Player, dungeonName: String, kitName: String, seconds: Int) {
        val cooldownId = "kit.$dungeonName.$kitName"
        io.github.zzzyyylllty.kangeldungeon.data.DataUtil.setCooldown(player, cooldownId, seconds.toDouble())
    }
}

/**
 * OmniItem 解析辅助（与 OmniItem 互操作）
 */
internal object OmniItemParser {
    fun parseMinecraftItem(source: String, itemId: String, parameters: Map<String, Any?>?, components: Map<String, Any?>?, amount: Int): ItemStack? {
        val omni = io.github.zzzyyylllty.kangeldungeon.data.OmniItem(
            source = source,
            item = itemId,
            parameters = parameters,
            components = components
        )
        return omni.build(player = null, overrideAmount = amount)
    }

    fun parseExternalItem(source: String, itemId: String, player: Player, parameters: Map<String, Any?>?, components: Map<String, Any?>?, amount: Int): ItemStack? {
        val omni = io.github.zzzyyylllty.kangeldungeon.data.OmniItem(
            source = source,
            item = itemId,
            parameters = parameters,
            components = components
        )
        return omni.build(player = player, overrideAmount = amount)
    }
}

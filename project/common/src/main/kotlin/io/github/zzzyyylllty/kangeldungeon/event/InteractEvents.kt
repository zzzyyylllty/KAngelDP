package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.InteractConfig
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import taboolib.common.platform.event.SubscribeEvent

/**
 * 地牢交互事件监听 - 监听玩家右键点击方块，触发对应地牢的交互 agent 脚本
 */
object InteractListener {

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 只处理右键点击方块
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val player = event.player

        // 查找玩家当前所在地牢
        val instance = findDungeonInstance(player.location.world?.name) ?: return

        // 验证玩家是该地牢的成员
        if (player.uniqueId !in instance.players) return

        // 获取该地牢的交互配置
        val interacts = KAngelDungeon.dungeonInteractConfigs[instance.templateName] ?: return
        if (interacts.isEmpty()) return

        // 构造点击位置的 key "x y z"
        val clickPos = "${block.x} ${block.y} ${block.z}"

        // 匹配交互配置
        val config = interacts.values.find { it.pos == clickPos } ?: return

        // 仅当 agent 有 onActive 处理时取消原版交互（onActive 替换了原版行为）
        // 仅有 onPost 时不取消，允许原版交互（如打开箱子）与脚本效果共存
        if (config.agent?.onActive != null) {
            event.isCancelled = true
        }

        // 执行交互 agent 脚本
        runInteractAgent(config, instance, player, event)
    }

    /**
     * 根据世界名查找对应的地牢实例
     */
    private fun findDungeonInstance(worldName: String?): DungeonInstance? {
        if (worldName == null) return null
        val instanceUuid = KAngelDungeon.worldInstanceIndex[worldName] ?: return null
        return KAngelDungeon.dungeonInstances[instanceUuid]
    }

    /**
     * 执行交互 agent 脚本（onActive → onPost）
     */
    private fun runInteractAgent(
        config: InteractConfig,
        instance: DungeonInstance,
        player: Player,
        event: PlayerInteractEvent? = null
    ) {
        val agent = config.agent ?: return
        val data = defaultData + mapOf(
            "instance" to instance,
            "template" to instance.getTemplate(),
            "player" to player,
            "interact" to config,
            "event" to event,
            "block" to event?.clickedBlock,
            "action" to event?.action,
            "clickedPos" to (event?.clickedBlock?.let { "${it.x} ${it.y} ${it.z}" })
        )

        try {
            agent.onActive?.let { GraalJsUtil.cachedEval(it, data) }
            agent.onPost?.let { GraalJsUtil.cachedEval(it, data) }
            instance.meta.add("interact.trigger", 1)
            instance.meta.add("interact.trigger.${config.id}", 1)
        } catch (e: Exception) {
            warningL("WarningInteractAgentFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }
}

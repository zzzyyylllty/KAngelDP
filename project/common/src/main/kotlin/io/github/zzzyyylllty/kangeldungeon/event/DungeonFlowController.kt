package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.BlockControlMode
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地牢流程控制器 - 监听各种事件以强制执行地牢配置中的限制。
 * 包括：方块放置/破坏黑白名单、禁用物品互动、命令过滤、原版机制控制。
 */
object DungeonFlowController : Listener {

    // ==================== 工具方法 ====================

    /**
     * 根据世界名查找地牢实例
     */
    private fun findDungeonInstance(worldName: String?): DungeonInstance? {
        if (worldName == null) return null
        val instanceUuid = KAngelDungeon.worldInstanceIndex[worldName] ?: return null
        return KAngelDungeon.dungeonInstances[instanceUuid]
    }

    /**
     * 检查玩家是否在地牢中且地牢处于活跃状态
     */
    private fun getActiveDungeon(player: Player): DungeonInstance? {
        val instance = findDungeonInstance(player.location.world?.name) ?: return null
        if (player.uniqueId !in instance.players) return null
        return instance
    }

    /**
     * 检查物品是否被 bannedItems 禁用
     */
    private fun isItemBanned(item: ItemStack?, bannedItems: List<String>): Boolean {
        if (item == null || bannedItems.isEmpty()) return false
        return item.type.name in bannedItems
    }

    /**
     * 检查方块是否被 BlockControlConfig 允许放置/破坏
     */
    private fun isBlockAllowed(material: Material, config: io.github.zzzyyylllty.kangeldungeon.data.BlockControlConfig): Boolean {
        val matName = material.name
        return when (config.mode) {
            BlockControlMode.BLACKLIST -> matName !in config.list
            BlockControlMode.WHITELIST -> matName in config.list
        }
    }

    // ==================== 方块放置控制 ====================

    @SubscribeEvent
    fun onBlockPlace(event: BlockPlaceEvent) {
        val instance = getActiveDungeon(event.player) ?: return
        val template = instance.getTemplate() ?: return
        val config = template.gameplayGeneral.blockPlace
        if (config.list.isEmpty()) return
        if (!isBlockAllowed(event.block.type, config)) {
            event.isCancelled = true
            devLog("Blocked block place: ${event.block.type.name} by ${event.player.name}")
            return
        }
        // 玩家放置方块追踪
        val pbCfg = template.playerBlocks
        // maxBlocksPerPlayer 限制（在追踪前检查，不消耗配额）
        if (pbCfg.maxBlocksPerPlayer >= 0) {
            val countKey = "${instance.worldName}:${event.player.name}"
            val currentCount = KAngelDungeon.playerPlacedBlockCount.getOrDefault(countKey, 0)
            if (currentCount >= pbCfg.maxBlocksPerPlayer) {
                event.isCancelled = true
                devLog("Blocked block place (max blocks per player): ${event.player.name}")
                return
            }
        }
        // 追踪放置的方块（保存原方块数据用于恢复）
        if (pbCfg.trackPlaced && pbCfg.clearOnEnd) {
            val worldName = instance.worldName
            val replacedState = event.blockReplacedState
            val key = "${event.block.x},${event.block.y},${event.block.z}"
            KAngelDungeon.playerPlacedBlocks
                .getOrPut(worldName) { ConcurrentHashMap() }
                .putIfAbsent(key, replacedState.blockData.asString)
        }
        // 递增计数器
        if (pbCfg.maxBlocksPerPlayer >= 0) {
            val countKey = "${instance.worldName}:${event.player.name}"
            KAngelDungeon.playerPlacedBlockCount.merge(countKey, 1, Int::plus)
        }
    }

    // ==================== 方块破坏控制 ====================

    @SubscribeEvent
    fun onBlockBreak(event: BlockBreakEvent) {
        val instance = getActiveDungeon(event.player) ?: return
        val template = instance.getTemplate() ?: return
        val config = template.gameplayGeneral.blockBreak
        if (config.list.isEmpty()) return
        if (!isBlockAllowed(event.block.type, config)) {
            event.isCancelled = true
            devLog("Blocked block break: ${event.block.type.name} by ${event.player.name}")
            return
        }
        // breakable-blocks 白名单检查（额外限制层，非空时仅允许列表中的方块）
        val breakable = template.breakableBlocks
        if (breakable.isNotEmpty() && event.block.type.name !in breakable) {
            event.isCancelled = true
            devLog("Blocked block break (not in breakable-blocks): ${event.block.type.name} by ${event.player.name}")
            return
        }
    }

    // ==================== 禁用物品互动 ====================

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val instance = getActiveDungeon(event.player) ?: return
        val template = instance.getTemplate() ?: return
        val bannedItems = template.gameplayGeneral.bannedItems
        if (bannedItems.isEmpty()) return
        val item = event.item ?: return
        if (isItemBanned(item, bannedItems)) {
            event.isCancelled = true
            devLog("Blocked banned item use: ${item.type.name} by ${event.player.name}")
        }
    }

    // ==================== 命令过滤 ====================

    @SubscribeEvent
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        val instance = getActiveDungeon(event.player) ?: return
        val template = instance.getTemplate() ?: return
        val cmdConfig = template.commandConfig
        if (cmdConfig.list.isEmpty()) return
        val cmd = event.message.split(" ").first().lowercase()
        val isBlocked = when (cmdConfig.mode) {
            io.github.zzzyyylllty.kangeldungeon.data.CommandMode.BLACKLIST -> cmd in cmdConfig.list
            io.github.zzzyyylllty.kangeldungeon.data.CommandMode.WHITELIST -> cmd !in cmdConfig.list
        }
        if (isBlocked) {
            event.isCancelled = true
            devLog("Blocked command: $cmd by ${event.player.name}")
        }
    }

    // ==================== 原版机制控制 ====================

    @SubscribeEvent
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val instance = getActiveDungeon(entity) ?: return
        val template = instance.getTemplate() ?: return
        if (!template.vanillaOptions.hungry) {
            event.isCancelled = true
        }
    }

    @SubscribeEvent
    fun onItemDrop(event: PlayerDropItemEvent) {
        val instance = getActiveDungeon(event.player) ?: return
        val template = instance.getTemplate() ?: return
        if (!template.vanillaOptions.itemsDrop) {
            event.isCancelled = true
        }
    }

    @SubscribeEvent
    fun onItemPickup(event: PlayerPickupItemEvent) {
        val instance = getActiveDungeon(event.player) ?: return
        val template = instance.getTemplate() ?: return
        if (!template.vanillaOptions.itemsPickup) {
            event.isCancelled = true
        }
    }

    // PlayerItemDamageEvent is deprecated in Paper 1.20.5+, use ItemDamageEvent instead
    @Suppress("DEPRECATION")
    @SubscribeEvent
    fun onItemDamage(event: org.bukkit.event.player.PlayerItemDamageEvent) {
        val instance = getActiveDungeon(event.player) ?: return
        val template = instance.getTemplate() ?: return
        if (!template.vanillaOptions.durability) {
            event.isCancelled = true
        }
    }

    // ==================== 自然恢复控制 ====================

    @SubscribeEvent
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val instance = getActiveDungeon(entity) ?: return
        val template = instance.getTemplate() ?: return
        val hr = template.vanillaOptions.healthRegain
        if (!hr.isAnyEnabled) return  // all enabled, let vanilla behavior through

        val cancel = when (event.regainReason.name) {
            "SATIATED", "EATING" -> !hr.food
            "REGEN" -> !hr.saturation
            "MAGIC", "MAGIC_REGEN", "MAGIC_HEALING", "HEALING_POTION" -> !hr.potions
            else -> !hr.other
        }
        if (cancel) {
            event.isCancelled = true
            devLog("Blocked health regain (${event.regainReason}) for ${entity.name}")
        }
    }

    // ==================== PVP 控制 ====================

    @SubscribeEvent
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.entity !is Player || event.damager !is Player) return
        val victim = event.entity as Player
        val damager = event.damager as Player

        val victimInstance = getActiveDungeon(victim)
        val damagerInstance = getActiveDungeon(damager)

        // 双方都不在地牢
        if (victimInstance == null && damagerInstance == null) return

        // 确定地牢实例（双方必须在同一地牢）
        val instance = when {
            victimInstance != null && damagerInstance != null -> {
                if (victimInstance.uuid != damagerInstance.uuid) return  // 不同地牢不处理
                victimInstance
            }
            else -> return  // 一方在地牢一方不在，不处理（让命令过滤等机制处理）
        }

        val template = instance.getTemplate()
        if (template != null && !template.pvpEnabled) {
            event.isCancelled = true
            devLog("Blocked PVP: ${damager.name} -> ${victim.name} in dungeon ${instance.templateName}")
        }
    }

    // ==================== 传送逃逸防护 ====================

    @SubscribeEvent
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        val instance = getActiveDungeon(player) ?: return
        val to = event.to ?: return
        val from = event.from ?: return

        // 允许同世界传送（末影珍珠、紫颂果等）
        if (to.world == from.world) return

        // 允许插件发起的传送（地牢系统自身的传送逻辑）
        if (event.cause == PlayerTeleportEvent.TeleportCause.PLUGIN) return

        // 阻止跨世界传送逃出地牢
        event.isCancelled = true
        devLog("Blocked teleport (${event.cause}) for ${player.name} from dungeon ${instance.templateName}")
    }

    @SubscribeEvent
    fun onPlayerPortal(event: org.bukkit.event.player.PlayerPortalEvent) {
        val player = event.player
        val instance = getActiveDungeon(player) ?: return
        event.isCancelled = true
        devLog("Blocked portal for ${player.name} in dungeon ${instance.templateName}")
    }

    // ==================== 地牢聊天自动路由 ====================

    @SubscribeEvent
    fun onAsyncChat(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
        val player = event.player

        // 检查玩家是否退出了自动路由
        if (player.uniqueId in KAngelDungeon.dungeonChatOptOut) return

        val instanceUuid = KAngelDungeon.playerToInstanceIndex[player.uniqueId] ?: return
        val instance = KAngelDungeon.dungeonInstances[instanceUuid] ?: return
        val config = instance.getTemplate()?.dungeonChat ?: return
        if (!config.enabled || !config.autoRoute) return

        // 自动将地牢玩家的普通聊天也转发给同地牢成员
        // 取消原事件（防止全服广播），我们手动发送给同地牢玩家
        event.isCancelled = true

        val formatted = config.format
            .replace("%player%", player.name)
            .replace("%message%", event.message)
            .replace("%dungeon%", instance.templateName)

        val component = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(formatted)
        // 异步事件中，对 instance.players 做防御性拷贝避免 ConcurrentModificationException
        val playersSnapshot = synchronized(instance.players) { instance.players.toSet() }
        for (uuid in playersSnapshot) {
            val target = org.bukkit.Bukkit.getPlayer(uuid) ?: continue
            target.sendMessage(component)
        }
        // 发送给控制台（替代取消后丢失的日志）
        org.bukkit.Bukkit.getConsoleSender().sendMessage(
            net.kyori.adventure.text.Component.text("[DC:${instance.templateName}] ${player.name}: ${event.message}")
        )
    }
}

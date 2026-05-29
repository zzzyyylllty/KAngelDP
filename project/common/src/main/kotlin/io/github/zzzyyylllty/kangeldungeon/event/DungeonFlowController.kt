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
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent

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
}

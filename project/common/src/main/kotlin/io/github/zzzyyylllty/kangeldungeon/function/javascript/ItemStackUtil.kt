package io.github.zzzyyylllty.kangeldungeon.function.javascript

import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common.platform.function.submit
import taboolib.module.nms.ItemTag
import taboolib.module.nms.getItemTag
import taboolib.module.nms.setItemTag
import java.util.concurrent.CompletableFuture

object ItemStackUtil {

    private fun runOnMain(action: () -> Unit) {
        if (Bukkit.isPrimaryThread()) action() else submit { action() }
    }

    // ==================== TabooLib ItemTag ====================

    fun getItemTag(itemStack: ItemStack): ItemTag {
        return itemStack.getItemTag()
    }

    fun setItemTag(itemStack: ItemStack, tag: ItemTag): ItemStack {
        return itemStack.setItemTag(tag)
    }

    fun setItemTagDirect(itemStack: ItemStack, tag: ItemTag): ItemStack {
        return tag.saveTo(itemStack)
    }

    // ==================== 创建物品 ====================

    /**
     * 创建物品（支持 MiniMessage 名称和 lore）
     * JS: ItemStackUtil.createItem("DIAMOND_SWORD", 1, "<gold>Excalibur</gold>", ["<gray>Legendary sword</gray>"])
     */
    fun createItem(material: String, amount: Int = 1, name: String? = null, lore: List<String>? = null): ItemStack? {
        val mat = Material.getMaterial(material.uppercase()) ?: return null
        val item = ItemStack(mat, amount.coerceAtLeast(1))
        val meta = item.itemMeta ?: return item
        if (name != null) meta.displayName(mmUtil.deserialize(name))
        if (!lore.isNullOrEmpty()) {
            meta.lore(lore.map { mmUtil.deserialize(it) })
        }
        item.itemMeta = meta
        return item
    }

    /**
     * 创建简易物品
     * JS: ItemStackUtil.createItemStack("DIAMOND", 5)
     */
    fun createItemStack(material: String, amount: Int = 1): ItemStack? {
        val mat = Material.getMaterial(material.uppercase()) ?: return null
        return ItemStack(mat, amount.coerceAtLeast(1))
    }

    // ==================== 物品操作 ====================

    /**
     * 给予玩家物品
     * JS: ItemStackUtil.addItemToPlayer(player, itemStack)
     */
    fun addItemToPlayer(player: Player, itemStack: ItemStack): Boolean {
        runOnMain {
            val leftover = player.inventory.addItem(itemStack)
            if (leftover.isNotEmpty()) {
                for ((_, item) in leftover) {
                    player.world.dropItem(player.location, item)
                }
            }
        }
        return true
    }

    /**
     * 从玩家背包移除指定材料
     * @return 是否成功移除足够数量
     * JS: ItemStackUtil.removeItemFromPlayer(player, "DIAMOND", 5)
     */
    fun removeItemFromPlayer(player: Player, material: String, amount: Int): Boolean {
        val mat = Material.getMaterial(material.uppercase()) ?: return false
        var needed = amount.coerceAtLeast(1)
        if (!Bukkit.isPrimaryThread()) {
            val future = CompletableFuture<Boolean>()
            submit { future.complete(removeItemFromPlayer(player, material, amount)) }
            return try { future.get(5, java.util.concurrent.TimeUnit.SECONDS) } catch (_: Exception) { false }
        }
        if (countItem(player, material) < needed) return false
        for (item in player.inventory.contents) {
            if (item == null || item.type != mat) continue
            val remove = minOf(needed, item.amount)
            item.amount -= remove
            needed -= remove
            if (needed <= 0) break
        }
        return true
    }

    /**
     * 统计玩家背包中某材料的数量
     * JS: ItemStackUtil.countItem(player, "DIAMOND")
     */
    fun countItem(player: Player, material: String): Int {
        val mat = Material.getMaterial(material.uppercase()) ?: return 0
        return player.inventory.contents.filterNotNull()
            .filter { it.type == mat }
            .sumOf { it.amount }
    }

    /**
     * 检查玩家是否有足够数量的物品
     * JS: ItemStackUtil.hasItem(player, "DIAMOND", 3)
     */
    fun hasItem(player: Player, material: String, amount: Int = 1): Boolean {
        return countItem(player, material) >= amount.coerceAtLeast(1)
    }

    // ==================== 手持物品 ====================

    /**
     * 获取玩家主手的物品
     * JS: ItemStackUtil.getItemInMainHand(player)
     */
    fun getItemInMainHand(player: Player): ItemStack? {
        val item = player.inventory.itemInMainHand
        return if (item.type.isAir) null else item
    }

    /**
     * 获取玩家副手的物品
     * JS: ItemStackUtil.getItemInOffHand(player)
     */
    fun getItemInOffHand(player: Player): ItemStack? {
        val item = player.inventory.itemInOffHand
        return if (item.type.isAir) null else item
    }

    /**
     * 设置玩家主手物品
     * JS: ItemStackUtil.setItemInMainHand(player, itemStack)
     */
    fun setItemInMainHand(player: Player, itemStack: ItemStack) {
        runOnMain { player.inventory.setItemInMainHand(itemStack) }
    }

    /**
     * 设置玩家副手物品
     * JS: ItemStackUtil.setItemInOffHand(player, itemStack)
     */
    fun setItemInOffHand(player: Player, itemStack: ItemStack) {
        runOnMain { player.inventory.setItemInOffHand(itemStack) }
    }

    // ==================== 装备 ====================

    /**
     * 设置玩家头盔
     * JS: ItemStackUtil.setHelmet(player, itemStack)
     */
    fun setHelmet(player: Player, itemStack: ItemStack) {
        runOnMain { player.inventory.helmet = itemStack }
    }

    /**
     * 设置玩家胸甲
     * JS: ItemStackUtil.setChestplate(player, itemStack)
     */
    fun setChestplate(player: Player, itemStack: ItemStack) {
        runOnMain { player.inventory.chestplate = itemStack }
    }

    /**
     * 设置玩家护腿
     * JS: ItemStackUtil.setLeggings(player, itemStack)
     */
    fun setLeggings(player: Player, itemStack: ItemStack) {
        runOnMain { player.inventory.leggings = itemStack }
    }

    /**
     * 设置玩家靴子
     * JS: ItemStackUtil.setBoots(player, itemStack)
     */
    fun setBoots(player: Player, itemStack: ItemStack) {
        runOnMain { player.inventory.boots = itemStack }
    }

    // ==================== 查询 ====================

    /**
     * 获取物品显示名称
     * JS: ItemStackUtil.getItemName(itemStack)
     */
    fun getItemName(itemStack: ItemStack): String {
        val meta = itemStack.itemMeta
        return if (meta != null && meta.hasDisplayName()) {
            meta.displayName()?.let { mmUtil.serialize(it) } ?: itemStack.type.name
        } else {
            itemStack.type.name
        }
    }

    /**
     * 设置物品显示名称（返回新物品，不修改原物品）
     * JS: ItemStackUtil.setItemName(itemStack, "<gold>Golden Sword</gold>")
     */
    fun setItemName(itemStack: ItemStack, name: String): ItemStack {
        val clone = itemStack.clone()
        val meta = clone.itemMeta ?: return clone
        meta.displayName(mmUtil.deserialize(name))
        clone.itemMeta = meta
        return clone
    }

    /**
     * 获取物品材料名
     * JS: ItemStackUtil.getItemType(itemStack)
     */
    fun getItemType(itemStack: ItemStack): String {
        return itemStack.type.name
    }

    /**
     * 获取物品数量
     * JS: ItemStackUtil.getItemAmount(itemStack)
     */
    fun getItemAmount(itemStack: ItemStack): Int {
        return itemStack.amount
    }

    /**
     * 判断物品是否为空气
     * JS: ItemStackUtil.isAir(itemStack)
     */
    fun isAir(itemStack: ItemStack): Boolean {
        return itemStack.type.isAir
    }

    /**
     * 判断两个物品是否相似（类型相同）
     * JS: ItemStackUtil.isSimilar(item1, item2)
     */
    fun isSimilar(item1: ItemStack, item2: ItemStack): Boolean {
        return item1.isSimilar(item2)
    }

    /**
     * 克隆物品
     * JS: ItemStackUtil.cloneItem(itemStack)
     */
    fun cloneItem(itemStack: ItemStack): ItemStack {
        return itemStack.clone()
    }

    /**
     * 设置物品 lore（返回新物品）
     * JS: ItemStackUtil.setLore(itemStack, ["<gray>Line 1</gray>", "<gray>Line 2</gray>"])
     */
    fun setLore(itemStack: ItemStack, lore: List<String>): ItemStack {
        val clone = itemStack.clone()
        val meta = clone.itemMeta ?: return clone
        meta.lore(lore.map { mmUtil.deserialize(it) })
        clone.itemMeta = meta
        return clone
    }

    /**
     * 获取物品 lore
     * JS: ItemStackUtil.getLore(itemStack)
     */
    fun getLore(itemStack: ItemStack): List<String>? {
        val meta = itemStack.itemMeta ?: return null
        return meta.lore()?.map { mmUtil.serialize(it) }
    }

    /**
     * 设置物品是否不可破坏
     * JS: ItemStackUtil.setUnbreakable(itemStack, true)
     */
    fun setUnbreakable(itemStack: ItemStack, unbreakable: Boolean): ItemStack {
        val clone = itemStack.clone()
        val meta = clone.itemMeta ?: return clone
        meta.setUnbreakable(unbreakable)
        clone.itemMeta = meta
        return clone
    }
}

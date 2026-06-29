package io.github.zzzyyylllty.kangeldungeon.function.javascript

import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmUtil
import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmLegacySectionUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times.times
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.function.submit
import java.time.Duration

object PlayerUtil {

    /**
     * 确保在主线程执行 Bukkit API 调用
     */
    private fun runOnMain(action: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            action()
        } else {
            submit { action() }
        }
    }

    // ==================== 药水效果 ====================

    @Suppress("DEPRECATION")
    private fun getPotionEffectType(type: String): PotionEffectType? {
        return PotionEffectType.getByName(type)
    }

    fun addPotionEffect(player: Player, type: String, duration: Int = 30, amplifier: Int = 0, ambient: Boolean = true, particles: Boolean = true, icon: Boolean = true) {
        val effectType = getPotionEffectType(type) ?: return
        runOnMain {
            player.addPotionEffect(PotionEffect(effectType, duration, amplifier, ambient, particles, icon))
        }
    }

    fun addPotionEffect(player: Player, type: String, duration: Int = 30, amplifier: Int = 0) {
        val effectType = getPotionEffectType(type) ?: return
        runOnMain {
            player.addPotionEffect(PotionEffect(effectType, duration, amplifier))
        }
    }

    fun removePotionEffect(player: Player, type: String) {
        val effectType = getPotionEffectType(type) ?: return
        runOnMain { player.removePotionEffect(effectType) }
    }

    // ==================== Title / 消息 ====================

    fun showTitle(player: Player, title: Component, subTitle: Component, durationIn: Int = 30, duration: Int = 30, durationOut: Int = 30) {
        val fadeIn = Duration.ofMillis(durationIn.toLong() * 50)
        val stay = Duration.ofMillis(duration.toLong() * 50)
        val fadeOut = Duration.ofMillis(durationOut.toLong() * 50)
        runOnMain { player.showTitle(Title.title(title, subTitle, times(fadeIn, stay, fadeOut))) }
    }

    /**
     * 使用 MiniMessage 格式发送标题
     * JS: PlayerUtil.sendTitle(player, "<gold>You Win!", "<green>Congratulations</green>", 10, 70, 20)
     */
    fun sendTitle(player: Player, title: String, subtitle: String = "", fadeIn: Int = 10, stay: Int = 70, fadeOut: Int = 20) {
        val titleComponent = mmUtil.deserialize(title)
        val subComponent = if (subtitle.isNotBlank()) mmUtil.deserialize(subtitle) else Component.empty()
        val t = Title.Times.times(
            Duration.ofMillis(fadeIn.toLong() * 50),
            Duration.ofMillis(stay.toLong() * 50),
            Duration.ofMillis(fadeOut.toLong() * 50)
        )
        runOnMain { player.showTitle(Title.title(titleComponent, subComponent, t)) }
    }

    /**
     * 使用 MiniMessage 发送消息
     * JS: PlayerUtil.sendMessage(player, "<red>You died!</red>")
     */
    fun sendMessage(player: Player, message: String) {
        val component = mmUtil.deserialize(message)
        runOnMain { player.sendMessage(component) }
    }

    /**
     * 使用 MiniMessage 发送 ActionBar
     * JS: PlayerUtil.sendActionBar(player, "<yellow>Loading...</yellow>")
     */
    fun sendActionBar(player: Player, message: String) {
        val component = mmUtil.deserialize(message)
        runOnMain { player.sendActionBar(component) }
    }

    // ==================== 生命/饱食 ====================

    /**
     * 完全治愈玩家
     * JS: PlayerUtil.heal(player)
     */
    fun heal(player: Player) {
        runOnMain {
            player.health = player.maxHealth
            player.foodLevel = 20
            player.saturation = 5f
            player.fireTicks = 0
        }
    }

    /**
     * 设置玩家生命值
     * JS: PlayerUtil.setHealth(player, 20.0)
     */
    fun setHealth(player: Player, health: Double) {
        runOnMain { player.health = health.coerceIn(0.0, player.maxHealth) }
    }

    /**
     * 设置玩家食物值
     * JS: PlayerUtil.setFood(player, 20)
     */
    fun setFood(player: Player, food: Int) {
        runOnMain { player.foodLevel = food.coerceIn(0, 20) }
    }

    /**
     * 设置玩家饱和度
     * JS: PlayerUtil.setSaturation(player, 5.0)
     */
    fun setSaturation(player: Player, saturation: Float) {
        runOnMain { player.saturation = saturation.coerceIn(0f, 20f) }
    }

    /**
     * 设置玩家经验等级
     * JS: PlayerUtil.setLevel(player, 10)
     */
    fun setLevel(player: Player, level: Int) {
        runOnMain { player.level = level.coerceAtLeast(0) }
    }

    // ==================== 背包 ====================

    /**
     * 给予玩家物品
     * JS: PlayerUtil.giveItem(player, itemStack)
     */
    fun giveItem(player: Player, itemStack: ItemStack) {
        runOnMain {
            val leftover = player.inventory.addItem(itemStack)
            for ((_, left) in leftover) {
                player.world.dropItem(player.location, left)
            }
        }
    }

    /**
     * 按材料和数量给予物品
     * JS: PlayerUtil.giveItemStack(player, "DIAMOND", 5)
     */
    fun giveItemStack(player: Player, material: String, amount: Int = 1) {
        val mat = Material.getMaterial(material.uppercase()) ?: return
        val item = ItemStack(mat, amount.coerceAtLeast(1))
        runOnMain { giveItem(player, item) }
    }

    /**
     * 从玩家背包移除物品
     * @return 是否成功移除（数量不足时返回 false）
     * JS: PlayerUtil.takeItem(player, "DIAMOND", 5)
     */
    fun takeItem(player: Player, material: String, amount: Int): Boolean {
        val mat = Material.getMaterial(material.uppercase()) ?: return false
        var needed = amount.coerceAtLeast(1)
        if (!Bukkit.isPrimaryThread()) return false
        val inv = player.inventory
        if (countItem(player, material) < needed) return false
        for (item in inv.contents) {
            if (item == null || item.type != mat || item.amount <= 0) continue
            val remove = minOf(needed, item.amount)
            item.amount -= remove
            needed -= remove
            if (needed <= 0) break
        }
        return true
    }

    /**
     * 统计玩家背包中某材料的数量
     * JS: PlayerUtil.countItem(player, "DIAMOND")
     */
    fun countItem(player: Player, material: String): Int {
        val mat = Material.getMaterial(material.uppercase()) ?: return 0
        return player.inventory.contents.filterNotNull()
            .filter { it.type == mat }
            .sumOf { it.amount }
    }

    /**
     * 检查玩家是否有足够数量的物品
     * JS: PlayerUtil.hasItem(player, "DIAMOND", 5)
     */
    fun hasItem(player: Player, material: String, amount: Int = 1): Boolean {
        return countItem(player, material) >= amount.coerceAtLeast(1)
    }

    /**
     * 清空玩家背包
     * JS: PlayerUtil.clearInventory(player)
     */
    fun clearInventory(player: Player) {
        runOnMain { player.inventory.clear() }
    }

    // ==================== 游戏模式 ====================

    /**
     * 设置玩家游戏模式
     * JS: PlayerUtil.setGameMode(player, "SPECTATOR")
     */
    fun setGameMode(player: Player, gamemode: String) {
        val mode = try { GameMode.valueOf(gamemode.uppercase()) } catch (_: Exception) { return }
        runOnMain { player.gameMode = mode }
    }

    // ==================== 传送 ====================

    /**
     * 传送玩家到坐标
     * JS: PlayerUtil.teleport(player, 100, 64, 100)
     * JS: PlayerUtil.teleport(player, 100, 64, 100, "world_name")
     */
    fun teleport(player: Player, x: Double, y: Double, z: Double, worldName: String? = null) {
        val world = if (worldName != null) Bukkit.getWorld(worldName) else player.world
        if (world != null) runOnMain { player.teleport(Location(world, x, y, z)) }
    }

    // ==================== 伤害/击杀 ====================

    /**
     * 对玩家造成伤害
     * JS: PlayerUtil.damage(player, 10.0)
     */
    fun damage(player: Player, amount: Double) {
        runOnMain { player.damage(amount.coerceAtLeast(0.0)) }
    }

    /**
     * 踢出玩家
     * JS: PlayerUtil.kick(player, "Game over!")
     */
    fun kick(player: Player, reason: String = "") {
        runOnMain {
            player.kickPlayer(
                if (reason.isNotBlank()) mmLegacySectionUtil.serialize(mmUtil.deserialize(reason)) else "Kicked"
            )
        }
    }

    // ==================== 查询 ====================

    /**
     * 通过名字获取玩家（在线）
     * JS: PlayerUtil.getPlayer("playerName")
     */
    fun getPlayer(name: String): Player? {
        return Bukkit.getPlayerExact(name)
    }

    /**
     * 检查玩家是否在线
     * JS: PlayerUtil.isOnline("playerName")
     */
    fun isOnline(playerName: String): Boolean {
        return Bukkit.getPlayerExact(playerName) != null
    }

    /**
     * 获取在线玩家列表
     * JS: PlayerUtil.getOnlinePlayers()
     */
    fun getOnlinePlayers(): List<Player> {
        return Bukkit.getOnlinePlayers().toList()
    }

    // ==================== 飞行 ====================

    /**
     * 设置玩家飞行状态
     * JS: PlayerUtil.setAllowFlight(player, true)
     */
    fun setAllowFlight(player: Player, allow: Boolean) {
        runOnMain { player.allowFlight = allow }
    }

    /**
     * 设置玩家飞行速度 (默认 0.1)
     * JS: PlayerUtil.setFlySpeed(player, 0.2)
     */
    fun setFlySpeed(player: Player, speed: Float) {
        runOnMain { player.flySpeed = speed.coerceIn(-1f, 1f) }
    }

    /**
     * 设置玩家行走速度 (默认 0.2)
     * JS: PlayerUtil.setWalkSpeed(player, 0.5)
     */
    fun setWalkSpeed(player: Player, speed: Float) {
        runOnMain { player.walkSpeed = speed.coerceIn(-1f, 1f) }
    }

    // ==================== 权限 ====================

    /**
     * 检查玩家权限
     * JS: PlayerUtil.hasPermission(player, "kangeldungeon.admin")
     */
    fun hasPermission(player: Player, permission: String): Boolean {
        return player.hasPermission(permission)
    }
}

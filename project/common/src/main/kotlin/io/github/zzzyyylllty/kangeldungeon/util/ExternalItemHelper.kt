package io.github.zzzyyylllty.kangeldungeon.util

import cn.gtemc.itembridge.api.ItemBridge
import cn.gtemc.itembridge.api.context.BuildContext
import cn.gtemc.itembridge.core.BukkitItemBridge
import io.github.zzzyyylllty.kangeldungeon.logger.infoL
import io.github.zzzyyylllty.kangeldungeon.logger.severeL
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


object ExternalItemHelper {
    var itemBridge: ItemBridge<ItemStack?, Player?>? =
        BukkitItemBridge.builder()
            .onHookSuccess({ p -> infoL("ExternalItemHookSuccess", p) })
            .onHookFailure({ p, e -> severeL("ExternalItemHookFailed", p, e.message ?: "Unknown") })
            .detectSupportedPlugins()
            .build()

    /**
     * 通过外部物品源构建物品（无玩家上下文）
     * itemBridge.build("craftengine", "default:topaz")
     * itemBridge.build("minecraft", "diamond")
     *
     * @param source 物品源插件名，如 "craftengine"、"minecraft"、"itemsadder"
     * @param itemId 物品ID，如 "default:topaz"、"diamond"
     * @return 构建成功的 ItemStack，失败返回 null
     */
    fun build(source: String, itemId: String): ItemStack? {
        return try {
            itemBridge?.build(source, itemId)?.get()
        } catch (e: Exception) {
            severeL("ExternalItemBuildFailed", source, itemId, e.message ?: "Unknown")
            null
        }
    }

    /**
     * 通过外部物品源构建物品（带玩家上下文，用于玩家头颅等物品）
     * itemBridge.build("craftengine", "default:topaz", player)
     *
     * @param source 物品源插件名
     * @param itemId 物品ID
     * @param player 玩家上下文
     * @return 构建成功的 ItemStack，失败返回 null
     */
    fun build(source: String, itemId: String, player: Player): ItemStack? {
        return try {
            itemBridge?.build(source, player, itemId)?.get()
        } catch (e: Exception) {
            severeL("ExternalItemBuildForPlayerFailed", source, itemId, player.name, e.message ?: "Unknown")
            null
        }
    }
}
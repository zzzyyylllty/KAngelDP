package io.github.zzzyyylllty.kangeldungeon.util

import cn.gtemc.itembridge.api.ItemBridge
import cn.gtemc.itembridge.api.context.BuildContext
import cn.gtemc.itembridge.core.BukkitItemBridge
import io.github.zzzyyylllty.kangeldungeon.logger.infoS
import io.github.zzzyyylllty.kangeldungeon.logger.severeS
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


object ExternalItemHelper {
    var itemBridge: ItemBridge<ItemStack?, Player?>? =
        BukkitItemBridge.builder()
            .onHookSuccess({ p -> infoS("Hooked External item source: $p") })
            .onHookFailure({ p, e -> severeS("Failed to hook External item " + p + ", because " + e.message) })
            .detectSupportedPlugins()
            .build()

}
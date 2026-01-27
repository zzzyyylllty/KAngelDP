package io.github.zzzyyylllty.kangeldungeon.function.javascript

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.nms.ItemTag
import taboolib.module.nms.getItemTag
import taboolib.module.nms.setItemTag

object ItemStackUtil {
    fun getItemTag(itemStack: ItemStack): ItemTag {
        return itemStack.getItemTag()
    }

    // 该方法 不会 不会 不会 改变原本物品!
    fun setItemTag(itemStack: ItemStack, tag: ItemTag): ItemStack {
        return itemStack.setItemTag(tag)
    }

    // 该方法会改变原本物品!
    fun setItemTagDirect(itemStack: ItemStack, tag: ItemTag): ItemStack {
        return tag.saveTo(itemStack)
    }
}

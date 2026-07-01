package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.lang
import io.github.zzzyyylllty.kangeldungeon.editor.util.langMsg
import io.github.zzzyyylllty.kangeldungeon.editor.util.langStr
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest

/**
 * Reusable confirm-delete dialog.
 */
object ConfirmDelete {

    fun open(player: Player, entryName: String, onConfirm: () -> Unit) {
        player.openMenu<Chest>(player.langStr("title.confirmDelete")) {
            rows(3)
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26).forEach { set(it, GuiItems.border()) }

            set(12, GuiItems.compItem(Material.LIME_DYE, player.lang("common.save"), listOf(
                player.lang("confirm.areYouSure"),
                player.lang("confirm.name", entryName),
                Component.empty(),
                player.lang("confirm.undoable")
            )))
            set(14, GuiItems.compItem(Material.RED_DYE, player.lang("common.cancel")))

            onClick(lock = true) { event ->
                when (event.rawSlot) {
                    12 -> {
                        player.closeInventory()
                        player.langMsg("confirm.deleted", entryName)
                        onConfirm()
                    }
                    14 -> player.closeInventory()
                }
            }

            handLocked(true)
        }
    }
}

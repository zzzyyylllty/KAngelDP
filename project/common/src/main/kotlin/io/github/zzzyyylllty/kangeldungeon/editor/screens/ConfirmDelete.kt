package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest

/**
 * Reusable confirm-delete dialog.
 */
object ConfirmDelete {

    fun open(player: Player, entryName: String, onConfirm: () -> Unit) {
        player.openMenu<Chest>("§cConfirm Delete") {
            rows(3)
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26).forEach { set(it, GuiItems.border()) }

            set(12, GuiItems.buildItem(Material.LIME_DYE) {
                name = "<green>✔ Delete"
                lore(
                    "<red>Are you sure?",
                    "<yellow>$entryName",
                    "",
                    "<gray>This action cannot be undone."
                )
            })
            set(14, GuiItems.buildItem(Material.RED_DYE) {
                name = "<red>✘ Cancel"
            })

            onClick(lock = true) { event ->
                when (event.rawSlot) {
                    12 -> {
                        player.closeInventory()
                        player.sendMessage("§cDeleted: $entryName")
                        onConfirm()
                    }
                    14 -> player.closeInventory()
                }
            }

            handLocked(true)
        }
    }
}

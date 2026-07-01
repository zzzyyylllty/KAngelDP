package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest

/**
 * Global configurations screen — access global kits and loot chests.
 */
object GlobalConfigs {

    fun open(player: Player) {
        player.openMenu<Chest>("§8Global Configurations") {
            rows(3)
            for (i in 0..26) set(i, GuiItems.border())

            set(11, GuiItems.buildItem(Material.CHEST) {
                name = "<gold>Global Kits</gold>"
                lore(
                    "<gray>Edit shared kit configurations",
                    "<gray>Located in <white>kits/</white> directory",
                    "",
                    "<gray><italic>Click to edit"
                )
            })

            set(13, GuiItems.buildItem(Material.BARREL) {
                name = "<gold>Global Loot Chests</gold>"
                lore(
                    "<gray>Edit shared loot chest configs",
                    "<gray>Located in <white>loot/</white> directory",
                    "",
                    "<gray><italic>Click to edit"
                )
            })

            set(15, GuiItems.buildItem(Material.COMMAND_BLOCK) {
                name = "<red>Global Scripts</red>"
                lore(
                    "<red>Script editing not available</red>",
                    "<red>in the in-game editor.</red>"
                )
            })

            onClick(lock = true) { event ->
                when (event.rawSlot) {
                    11 -> KitEditor.openGlobalList(player)
                    13 -> LootEditor.openGlobalList(player)
                    15 -> player.sendMessage("§cScript editing is not available in the GUI editor.")
                    22 -> MainMenu.open(player)
                }
            }

            set(22, GuiItems.backButton())

            handLocked(true)
        }
    }
}

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
 * Global configurations screen — access global kits and loot chests.
 */
object GlobalConfigs {

    fun open(player: Player) {
        player.openMenu<Chest>(player.langStr("title.globalConfig")) {
            rows(3)
            for (i in 0..26) set(i, GuiItems.border())

            set(11, GuiItems.compItem(Material.CHEST, player.lang("global.kitsName"), listOf(
                player.lang("global.kitsLore1"),
                player.lang("global.kitsLore2"),
                Component.empty(),
                player.lang("common.clickEdit")
            )))

            set(13, GuiItems.compItem(Material.BARREL, player.lang("global.lootName"), listOf(
                player.lang("global.lootLore1"),
                player.lang("global.lootLore2"),
                Component.empty(),
                player.lang("common.clickEdit")
            )))

            set(15, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("global.scriptsName"), listOf(
                player.lang("global.scriptsLore1"),
                player.lang("global.scriptsLore2")
            )))

            onClick(lock = true) { event ->
                when (event.rawSlot) {
                    11 -> KitEditor.openGlobalList(player)
                    13 -> LootEditor.openGlobalList(player)
                    15 -> player.langMsg("script.unavailable")
                    22 -> MainMenu.open(player)
                }
            }

            set(22, GuiItems.backButton())

            handLocked(true)
        }
    }
}

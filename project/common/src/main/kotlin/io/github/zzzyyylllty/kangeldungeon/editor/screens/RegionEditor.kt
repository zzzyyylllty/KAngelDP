package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.InputPrompts
import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import java.io.File

object RegionEditor {

    fun openList(player: Player, dungeonName: String) {
        val files = YamlIO.listYamlFiles(dungeonName, "region")
        val allData = linkedMapOf<String, MutableMap<String, Any?>>()
        for (f in files) {
            val data = YamlIO.loadYaml(f)
            data.forEach { (k, v) ->
                if (v is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    allData[k] = (v as Map<String, Any?>).toMutableMap()
                }
            }
        }

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>("§8Regions: $dungeonName") {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())

            elements { allData.entries.toList() }

            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                GuiItems.buildItem(Material.FILLED_MAP) {
                    name = "<yellow>$id"
                    val from = data["from"] ?: "?"
                    val to = data["to"] ?: "?"
                    lore("<gray>From: <white>$from", "<gray>To: <white>$to", "", "<gray><italic>Click to edit", "<red><italic>Shift+Click to delete")
                }
            }

            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    ConfirmDelete.open(player, entry.key) { deleteEntry(dungeonName, entry.key); openList(player, dungeonName) }
                } else {
                    openEditor(player, dungeonName, entry.key, entry.value)
                }
            }

            onClick(getSlot('A')) {
                InputPrompts.textInput(player, "Region ID", null) { id ->
                    val data = linkedMapOf<String, Any?>("from" to "0 0 0", "to" to "10 10 10", "agent" to linkedMapOf<String, Any?>())
                    openEditor(player, dungeonName, id, data as MutableMap<String, Any?>)
                }
            }

            set(getSlot('A'), GuiItems.addButton())
            set(getSlot('L'), GuiItems.backButton())
            onClick(getSlot('L')) { CategoryMenu.open(player, dungeonName) }
            set(getSlot('N'), GuiItems.nextPage())
            setNextPage(getSlot('N')) { _, _ -> GuiItems.nextPage() }

            handLocked(true)
        }
    }

    private fun openEditor(player: Player, dungeonName: String, id: String, data: MutableMap<String, Any?>) {
        val session = EditorSession.get(player)
        session.enterDungeon(dungeonName)

        fun render() {
            player.openMenu<taboolib.module.ui.type.Chest>("§8Region: $id") {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.fieldItem(Material.COMPASS, "from (x y z)", data["from"]))
                set(10, GuiItems.fieldItem(Material.COMPASS, "to (x y z)", data["to"]))
                set(13, GuiItems.scriptPlaceholder("agent.onEnter"))
                set(14, GuiItems.scriptPlaceholder("agent.onLeave"))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.textInput(player, "From (x y z)", data["from"] as? String) { data["from"] = it; render() }
                        10 -> InputPrompts.textInput(player, "To (x y z)", data["to"] as? String) { data["to"] = it; render() }
                        49 -> { saveEntry(dungeonName, id, data); player.sendMessage("§aSaved region '$id'!"); render() }
                        50 -> openList(player, dungeonName)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(dungeonName: String, id: String, data: Map<String, Any?>) {
        val file = File(YamlIO.dungeonFolder(dungeonName), "region/$id.yml")
        YamlIO.saveYaml(file, linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(dungeonName: String, id: String) {
        val file = File(YamlIO.dungeonFolder(dungeonName), "region/$id.yml")
        if (file.exists()) file.delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 50; 'L' -> 45; 'N' -> 53; else -> 0 }
}

package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.InputPrompts
import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.PageableChest
import java.io.File

object InteractEditor {

    fun openList(player: Player, dungeonName: String) {
        val files = YamlIO.listYamlFiles(dungeonName, "interact")
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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>("§8Interactions: $dungeonName") {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())

            elements { allData.entries.toList() }

            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                GuiItems.buildItem(Material.LEVER) {
                    name = "<yellow>$id"
                    lore("<gray>Pos: <white>${data["pos"] ?: "not set"}", "", "<gray><italic>Click to edit", "<red><italic>Shift+Click to delete")
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
                InputPrompts.textInput(player, "Interact ID", null) { id ->
                    val data = linkedMapOf<String, Any?>("pos" to "0 0 0", "agent" to linkedMapOf<String, Any?>())
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
        EditorSession.get(player).enterDungeon(dungeonName)

        fun render() {
            player.openMenu<Chest>("§8Interact: $id") {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.fieldItem(Material.COMPASS, "pos (x y z)", data["pos"]))
                set(13, GuiItems.scriptPlaceholder("agent.onActive"))
                set(14, GuiItems.scriptPlaceholder("agent.onPost"))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.textInput(player, "Pos (x y z)", data["pos"] as? String) { data["pos"] = it; render() }
                        49 -> { saveEntry(dungeonName, id, data); player.sendMessage("§aSaved interact '$id'!"); render() }
                        50 -> openList(player, dungeonName)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(dungeonName: String, id: String, data: Map<String, Any?>) {
        YamlIO.saveYaml(File(YamlIO.dungeonFolder(dungeonName), "interact/$id.yml"), linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(dungeonName: String, id: String) {
        File(YamlIO.dungeonFolder(dungeonName), "interact/$id.yml").delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 50; 'L' -> 45; 'N' -> 53; else -> 0 }
}

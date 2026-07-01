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

object DifficultyEditor {

    fun openList(player: Player, dungeonName: String) {
        val file = File(YamlIO.dungeonFolder(dungeonName), "difficulty.yml")
        val allData = YamlIO.loadYaml(file)
        val difficulties = linkedMapOf<String, MutableMap<String, Any?>>()
        val raw = allData["difficulties"]
        if (raw is Map<*, *>) {
            raw.forEach { (k, v) ->
                if (v is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    difficulties[k.toString()] = (v as Map<String, Any?>).toMutableMap()
                }
            }
        }

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>("§8Difficulty: $dungeonName") {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { difficulties.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                GuiItems.buildItem(Material.NETHER_STAR) {
                    name = "<yellow>$id"
                    lore(
                        "<gray>Display: <white>${data["display"] ?: id}",
                        "<gray>Desc: <white>${(data["description"] as? String)?.take(30) ?: ""}",
                        "", "<gray><italic>Click to edit", "<red><italic>Shift+Click to delete"
                    )
                }
            }
            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    ConfirmDelete.open(player, entry.key) { difficulties.remove(entry.key); saveFile(file, difficulties); openList(player, dungeonName) }
                } else openEditor(player, dungeonName, entry.key, entry.value)
            }
            onClick(getSlot('A')) {
                InputPrompts.textInput(player, "Difficulty ID", null) { id ->
                    val data = linkedMapOf<String, Any?>("display" to id, "description" to "", "meta" to linkedMapOf<String, Any?>())
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
            player.openMenu<Chest>("§8Difficulty: $id") {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.fieldItem(Material.OAK_SIGN, "display", data["display"]))
                set(10, GuiItems.fieldItem(Material.PAPER, "description", data["description"]))
                set(16, GuiItems.scriptPlaceholder("agents (onStart/onComplete/onFail)"))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.textInput(player, "Display Name", data["display"] as? String) { data["display"] = it; render() }
                        10 -> InputPrompts.textInput(player, "Description", data["description"] as? String) { data["description"] = it; render() }
                        49 -> {
                            val file = File(YamlIO.dungeonFolder(dungeonName), "difficulty.yml")
                            val allData = YamlIO.loadYaml(file)
                            @Suppress("UNCHECKED_CAST")
                            val diffs = (allData.getOrPut("difficulties") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)
                            diffs[id] = data
                            YamlIO.saveYaml(file, allData)
                            KAngelDungeon.reloadCustomConfig(async = true)
                            player.sendMessage("§aSaved difficulty '$id'!")
                            render()
                        }
                        50 -> openList(player, dungeonName)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveFile(file: File, data: Map<String, MutableMap<String, Any?>>) {
        YamlIO.saveYaml(file, linkedMapOf("difficulties" to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 50; 'L' -> 45; 'N' -> 53; else -> 0 }
}

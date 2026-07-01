package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.InputPrompts
import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import io.github.zzzyyylllty.kangeldungeon.editor.util.lang
import io.github.zzzyyylllty.kangeldungeon.editor.util.langMsg
import io.github.zzzyyylllty.kangeldungeon.editor.util.langStr
import net.kyori.adventure.text.Component
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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(player.langStr("title.difficultyList", dungeonName)) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { difficulties.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                GuiItems.compItem(Material.NETHER_STAR, player.lang("difficulty.name", id), listOf(
                    player.lang("difficulty.display", data["display"] ?: id),
                    player.lang("difficulty.desc", (data["description"] as? String)?.take(30) ?: ""),
                    Component.empty(),
                    player.lang("common.clickEdit"),
                    player.lang("common.shiftDelete")
                ))
            }
            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    ConfirmDelete.open(player, entry.key) { difficulties.remove(entry.key); saveFile(file, difficulties); openList(player, dungeonName) }
                } else openEditor(player, dungeonName, entry.key, entry.value)
            }
            onClick(getSlot('A')) {
                InputPrompts.textInput(player, player.langStr("inputTitle.difficultyId"), null) { id ->
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
            player.openMenu<Chest>(player.langStr("title.difficultyEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.fieldItem(Material.OAK_SIGN, "display", data["display"]))
                set(10, GuiItems.fieldItem(Material.PAPER, "description", data["description"]))
                @Suppress("UNCHECKED_CAST")
                val diffAgents = (data.getOrPut("agents") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)
                set(16, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("difficulty.agents", diffAgents.size), listOf(
                    player.lang("common.clickEdit")
                )))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.textInput(player, "Display Name", data["display"] as? String) { data["display"] = it; render() }
                        10 -> InputPrompts.textInput(player, "Description", data["description"] as? String) { data["description"] = it; render() }
                        16 -> openDifficultyAgentsEditor(player, diffAgents, { render() })
                        49 -> {
                            val file = File(YamlIO.dungeonFolder(dungeonName), "difficulty.yml")
                            val allData = YamlIO.loadYaml(file)
                            @Suppress("UNCHECKED_CAST")
                            val diffs = (allData.getOrPut("difficulties") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)
                            diffs[id] = data
                            YamlIO.saveYaml(file, allData)
                            KAngelDungeon.reloadCustomConfig(async = true)
                            player.langMsg("difficulty.saved", id)
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

    private fun openDifficultyAgentsEditor(player: Player, agents: MutableMap<String, Any?>, rerender: () -> Unit) {
        val hooks = listOf("onStart", "onComplete", "onFail")
        player.openMenu<Chest>(player.langStr("title.diffAgents")) {
            rows(3)
            for (i in 0..26) set(i, GuiItems.border())
            hooks.forEachIndexed { idx, hook ->
                val slot = 10 + idx
                set(slot, GuiItems.fieldItem(Material.COMMAND_BLOCK, "$hook (JS)", agents[hook]))
            }
            set(22, GuiItems.backButton())
            onClick(lock = true) { event ->
                when (event.rawSlot) {
                    in 10..12 -> {
                        val hook = hooks[event.rawSlot - 10]
                        InputPrompts.multilineInput(player, "$hook JS", agents[hook] as? String) { js ->
                            agents[hook] = js
                            openDifficultyAgentsEditor(player, agents, rerender)
                        }
                    }
                    22 -> rerender()
                }
            }
            handLocked(true)
        }
    }

    private fun saveFile(file: File, data: Map<String, MutableMap<String, Any?>>) {
        YamlIO.saveYaml(file, linkedMapOf("difficulties" to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

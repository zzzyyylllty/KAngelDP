package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.InputPrompts
import io.github.zzzyyylllty.kangeldungeon.editor.util.SelectionTool
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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(player.langStr("title.interactList", dungeonName)) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())

            elements { allData.entries.toList() }

            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                GuiItems.compItem(Material.LEVER, player.lang("monster.name", id), listOf(
                    player.lang("interact.pos", data["pos"] ?: "not set"),
                    Component.empty(),
                    player.lang("common.clickEdit"),
                    player.lang("common.shiftDelete")
                ))
            }

            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    ConfirmDelete.open(player, entry.key) { deleteEntry(dungeonName, entry.key); openList(player, dungeonName) }
                } else {
                    openEditor(player, dungeonName, entry.key, entry.value)
                }
            }

            onClick(getSlot('A')) {
                InputPrompts.textInput(player, player.langStr("inputTitle.interactId"), null) { id ->
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
        val session = EditorSession.get(player)
        session.enterDungeon(dungeonName)

        fun render() {
            player.openMenu<Chest>(player.langStr("title.interactEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                @Suppress("UNCHECKED_CAST")
                val interactAgent = (data.getOrPut("agent") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)
                set(9, GuiItems.compItem(Material.COMPASS, player.lang("inputTitle.location"), listOf(
                    player.lang("interact.pos", data["pos"]?.toString() ?: "not set"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))

                // Point selection tools
                set(10, GuiItems.compItem(Material.TARGET, player.lang("interact.setPos"), listOf(
                    player.lang("loc.currentPosLore")
                )))
                set(11, GuiItems.compItem(Material.LEVER, player.lang("interact.pickPos"), listOf(
                    player.lang("interact.pickLore")
                )))
                set(12, GuiItems.compItem(Material.WOODEN_AXE, player.lang("region.wandLabel"), listOf(
                    player.lang("interact.wandLore")
                )))
                set(15, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", interactAgent["onActive"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(16, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", interactAgent["onPost"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.textInput(player, player.langStr("inputTitle.location"), data["pos"] as? String) { data["pos"] = it; render() }
                        10 -> {
                            data["pos"] = session.formatPos(player.location)
                            player.langMsg("loc.set")
                            render()
                        }
                        11 -> {
                            session.wandCallback = { loc ->
                                data["pos"] = session.formatPos(loc)
                                render()
                            }
                            player.closeInventory()
                            player.langMsg("loc.pickHint")
                        }
                        12 -> {
                            SelectionTool.giveWand(player)
                            render()
                        }
                        15 -> InputPrompts.multilineInput(player, player.langStr("inputTitle.agentScript"), interactAgent["onActive"] as? String) { interactAgent["onActive"] = it; render() }
                        16 -> InputPrompts.multilineInput(player, player.langStr("inputTitle.agentScript"), interactAgent["onPost"] as? String) { interactAgent["onPost"] = it; render() }
                        49 -> { saveEntry(dungeonName, id, data); player.langMsg("interact.saved", id); render() }
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

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

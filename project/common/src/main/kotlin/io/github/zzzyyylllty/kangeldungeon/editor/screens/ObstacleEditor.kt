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

object ObstacleEditor {

    fun openList(player: Player, dungeonName: String) {
        val files = YamlIO.listYamlFiles(dungeonName, "obstacle")
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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(player.langStr("title.obstacleList", dungeonName)) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val gateCount = (data["obstacles"] as? Map<*, *>)?.size ?: 0
                GuiItems.compItem(Material.IRON_DOOR, player.lang("monster.name", id), listOf(
                    player.lang("obstacle.gates", gateCount.toString()),
                    player.lang("obstacle.openDelay", data["openDelaySeconds"]?.toString() ?: "?"),
                    player.lang("obstacle.activeDuration", data["activeDurationSeconds"]?.toString() ?: "?"),
                    Component.empty(),
                    player.lang("common.clickEdit"),
                    player.lang("common.shiftDelete")
                ))
            }
            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    ConfirmDelete.open(player, entry.key) { deleteEntry(dungeonName, entry.key); openList(player, dungeonName) }
                } else openEditor(player, dungeonName, entry.key, entry.value)
            }
            onClick(getSlot('A')) {
                InputPrompts.textInput(player, player.langStr("inputTitle.obstacleId"), null) { id ->
                    val data = linkedMapOf<String, Any?>("openDelaySeconds" to 3.0, "activeDurationSeconds" to 10.0, "obstacles" to linkedMapOf<String, Any?>())
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
            player.openMenu<Chest>(player.langStr("title.obstacleEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                @Suppress("UNCHECKED_CAST")
                val obstacleAgent = (data.getOrPut("agent") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)
                set(9, GuiItems.numberItem("openDelaySeconds", data["openDelaySeconds"] as? Number))
                set(10, GuiItems.numberItem("activeDurationSeconds", data["activeDurationSeconds"] as? Number))
                set(11, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", obstacleAgent["onPrepare"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(12, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", obstacleAgent["onStart"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))

                val obstaclesCount = (data["obstacles"] as? Map<*, *>)?.size ?: 0
                set(16, GuiItems.compItem(Material.IRON_BLOCK, player.lang("obstacle.gateTitle"), listOf(
                    player.lang("obstacle.gateCount", obstaclesCount.toString()),
                    Component.empty(),
                    player.lang("common.clickManage")
                )))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.doubleInput(player, "Open Delay (seconds)", (data["openDelaySeconds"] as? Number)?.toDouble()) { data["openDelaySeconds"] = it; render() }
                        10 -> InputPrompts.doubleInput(player, "Active Duration (seconds)", (data["activeDurationSeconds"] as? Number)?.toDouble()) { data["activeDurationSeconds"] = it; render() }
                        11 -> InputPrompts.multilineInput(player, player.langStr("inputTitle.agentScript"), obstacleAgent["onPrepare"] as? String) { obstacleAgent["onPrepare"] = it; render() }
                        12 -> InputPrompts.multilineInput(player, player.langStr("inputTitle.agentScript"), obstacleAgent["onStart"] as? String) { obstacleAgent["onStart"] = it; render() }
                        16 -> openGatesEditor(player, id, data)
                        49 -> { saveEntry(dungeonName, id, data); player.langMsg("obstacle.saved", id); render() }
                        50 -> openList(player, dungeonName)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun openGatesEditor(player: Player, obstacleId: String, parentData: MutableMap<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val gates = (parentData["obstacles"] as? MutableMap<String, Any?>) ?: linkedMapOf<String, Any?>()

        player.openMenu<PageableChest<MutableMap.MutableEntry<String, Any?>>>(player.langStr("title.gates")) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { gates.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (gid, gateData) = entry
                val modeStr = if (gateData is Map<*, *>) {
                    gateData["mode"] as? String ?: "RESTORE_BLOCKS"
                } else {
                    "simple"
                }
                GuiItems.compItem(Material.IRON_BARS, player.lang("monster.name", gid), listOf(
                    player.lang("gate.details", modeStr),
                    Component.empty(),
                    player.lang("common.clickEdit"),
                    player.lang("common.shiftDelete")
                ))
            }
            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    gates.remove(entry.key)
                    parentData["obstacles"] = gates
                    openGatesEditor(player, obstacleId, parentData)
                } else {
                    openGateEditor(player, gates, entry.key, obstacleId, parentData)
                }
            }
            onClick(getSlot('A')) {
                InputPrompts.textInput(player, player.langStr("inputTitle.gateId"), null) { gid ->
                    gates[gid] = linkedMapOf<String, Any?>("mode" to "RESTORE_BLOCKS")
                    parentData["obstacles"] = gates
                    openGatesEditor(player, obstacleId, parentData)
                }
            }
            set(getSlot('A'), GuiItems.addButton())
            set(getSlot('L'), GuiItems.backButton())
            onClick(getSlot('L')) { openEditor(player, EditorSession.get(player).dungeonName ?: "", obstacleId, parentData) }
            set(getSlot('N'), GuiItems.nextPage())
            setNextPage(getSlot('N')) { _, _ -> GuiItems.nextPage() }
            handLocked(true)
        }
    }

    private fun openGateEditor(player: Player, gates: MutableMap<String, Any?>, gateId: String, obstacleId: String, parentData: MutableMap<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val gateData = ((gates[gateId] as? Map<String, Any?>)
            ?.toMutableMap() ?: linkedMapOf<String, Any?>("mode" to "RESTORE_BLOCKS"))

        val modes = listOf("RESTORE_BLOCKS")

        fun render() {
            player.openMenu<Chest>(player.langStr("title.gateEditor", gateId)) {
                rows(3)
                for (i in 0..26) set(i, GuiItems.border())

                set(10, GuiItems.enumItem(player.langStr("gate.mode"), gateData["mode"] as? String, modes))

                set(22, GuiItems.saveButton())
                set(24, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        10 -> InputPrompts.enumSelect(player, player.langStr("inputTitle.gateMode"), modes, gateData["mode"] as? String) {
                            gateData["mode"] = it; gates[gateId] = gateData; parentData["obstacles"] = gates; render()
                        }
                        22 -> {
                            gates[gateId] = gateData
                            parentData["obstacles"] = gates
                            openGatesEditor(player, obstacleId, parentData)
                        }
                        24 -> openGatesEditor(player, obstacleId, parentData)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(dungeonName: String, id: String, data: Map<String, Any?>) {
        YamlIO.saveYaml(File(YamlIO.dungeonFolder(dungeonName), "obstacle/$id.yml"), linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(dungeonName: String, id: String) {
        File(YamlIO.dungeonFolder(dungeonName), "obstacle/$id.yml").delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

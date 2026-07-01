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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>("§8Obstacles: $dungeonName") {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val gateCount = (data["obstacles"] as? Map<*, *>)?.size ?: 0
                GuiItems.buildItem(Material.IRON_DOOR) {
                    name = "<yellow>$id"
                    lore(
                        "<gray>Gates: <white>$gateCount",
                        "<gray>OpenDelay: <white>${data["openDelaySeconds"]}s",
                        "<gray>ActiveDuration: <white>${data["activeDurationSeconds"]}s",
                        "", "<gray><italic>Click to edit", "<red><italic>Shift+Click to delete"
                    )
                }
            }
            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    ConfirmDelete.open(player, entry.key) { deleteEntry(dungeonName, entry.key); openList(player, dungeonName) }
                } else openEditor(player, dungeonName, entry.key, entry.value)
            }
            onClick(getSlot('A')) {
                InputPrompts.textInput(player, "Obstacle ID", null) { id ->
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
            player.openMenu<Chest>("§8Obstacle: $id") {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.numberItem("openDelaySeconds", data["openDelaySeconds"] as? Number))
                set(10, GuiItems.numberItem("activeDurationSeconds", data["activeDurationSeconds"] as? Number))
                set(11, GuiItems.scriptPlaceholder("agent.onPrepare"))
                set(12, GuiItems.scriptPlaceholder("agent.onStart"))

                val obstaclesCount = (data["obstacles"] as? Map<*, *>)?.size ?: 0
                set(16, GuiItems.buildItem(Material.IRON_BLOCK) {
                    name = "<yellow>Gates / Obstacles</yellow>"
                    lore("<gray>Count: <white>$obstaclesCount", "", "<gray><italic>Click to manage")
                })

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.doubleInput(player, "Open Delay (seconds)", (data["openDelaySeconds"] as? Number)?.toDouble()) { data["openDelaySeconds"] = it; render() }
                        10 -> InputPrompts.doubleInput(player, "Active Duration (seconds)", (data["activeDurationSeconds"] as? Number)?.toDouble()) { data["activeDurationSeconds"] = it; render() }
                        16 -> openGatesEditor(player, id, data)
                        49 -> { saveEntry(dungeonName, id, data); player.sendMessage("§aSaved obstacle '$id'!"); render() }
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

        player.openMenu<PageableChest<MutableMap.MutableEntry<String, Any?>>>("§8Gates") {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { gates.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (gid, gateData) = entry
                val details = if (gateData is Map<*, *>) {
                    val mode = gateData["mode"] ?: "RESTORE_BLOCKS"
                    "Mode: $mode"
                } else "simple"
                GuiItems.buildItem(Material.IRON_BARS) {
                    name = "<yellow>$gid</yellow>"
                    lore("<gray>$details", "", "<gray><italic>Click to edit", "<red><italic>Shift+Click to remove")
                }
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
                InputPrompts.textInput(player, "Gate ID", null) { gid ->
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
            player.openMenu<Chest>("§8Gate: $gateId") {
                rows(3)
                for (i in 0..26) set(i, GuiItems.border())

                set(10, GuiItems.enumItem("mode", gateData["mode"] as? String, modes))

                set(22, GuiItems.saveButton())
                set(24, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        10 -> InputPrompts.enumSelect(player, "Gate Mode", modes, gateData["mode"] as? String) {
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

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 50; 'L' -> 45; 'N' -> 53; else -> 0 }
}

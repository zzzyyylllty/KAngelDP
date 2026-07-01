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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(player.langStr("title.regionList", dungeonName)) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())

            elements { allData.entries.toList() }

            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val from = data["from"] ?: "?"
                val to = data["to"] ?: "?"
                GuiItems.compItem(Material.FILLED_MAP, player.lang("monster.name", id), listOf(
                    player.lang("region.from", from),
                    player.lang("region.to", to),
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
                InputPrompts.textInput(player, player.langStr("inputTitle.regionId"), null) { id ->
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
            player.openMenu<Chest>(player.langStr("title.regionEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "W###S###B")
                set('#', GuiItems.border())

                @Suppress("UNCHECKED_CAST")
                val regionAgent = (data.getOrPut("agent") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)

                // Column 1: from/to positions
                set(9, GuiItems.compItem(Material.COMPASS, player.lang("inputTitle.location"), listOf(
                    player.lang("region.from", data["from"]?.toString() ?: "?"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(10, GuiItems.compItem(Material.COMPASS, player.lang("inputTitle.location"), listOf(
                    player.lang("region.to", data["to"]?.toString() ?: "?"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))

                // Point selection tools (row 2)
                val p1 = session.pos1
                val p2 = session.pos2
                set(11, GuiItems.compItem(Material.TARGET, player.lang("region.setFromLabel"), listOf(
                    player.lang("region.clickPos1"),
                    player.lang("region.currentPos1", if (p1 != null) session.formatPos(p1) else "not set")
                )))
                set(12, GuiItems.compItem(Material.TARGET, player.lang("region.setToLabel"), listOf(
                    player.lang("region.clickPos2"),
                    player.lang("region.currentPos2", if (p2 != null) session.formatPos(p2) else "not set")
                )))
                set(13, GuiItems.compItem(Material.FILLED_MAP, player.lang("region.useBoth"), listOf(
                    player.lang("region.bothLine1"),
                    player.lang("region.bothLine2", if (p1 != null) session.formatPos(p1) else "?"),
                    player.lang("region.bothLine3", if (p2 != null) session.formatPos(p2) else "?")
                )))
                set(14, GuiItems.compItem(Material.WOODEN_AXE, player.lang("region.wandLabel"), listOf(
                    player.lang("region.wandLore")
                )))
                set(15, GuiItems.compItem(Material.LEVER, player.lang("region.pickFrom"), listOf(
                    player.lang("region.pickLore"),
                    player.lang("region.pickLore2")
                )))
                set(16, GuiItems.compItem(Material.LEVER, player.lang("region.pickTo"), listOf(
                    player.lang("region.pickLore"),
                    player.lang("region.pickLore2")
                )))

                // Agent scripts
                set(20, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", regionAgent["onEnter"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(21, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", regionAgent["onLeave"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.textInput(player, player.langStr("inputTitle.location"), data["from"] as? String) { data["from"] = it; render() }
                        10 -> InputPrompts.textInput(player, player.langStr("inputTitle.location"), data["to"] as? String) { data["to"] = it; render() }
                        11 -> {
                            val loc = session.pos1
                            if (loc != null) { data["from"] = session.formatPos(loc); render() }
                            else player.langMsg("region.pos1NotSet")
                        }
                        12 -> {
                            val loc = session.pos2
                            if (loc != null) { data["to"] = session.formatPos(loc); render() }
                            else player.langMsg("region.pos2NotSet")
                        }
                        13 -> {
                            val loc1 = session.pos1
                            val loc2 = session.pos2
                            if (loc1 != null && loc2 != null) {
                                val fx = minOf(loc1.blockX, loc2.blockX)
                                val fy = minOf(loc1.blockY, loc2.blockY)
                                val fz = minOf(loc1.blockZ, loc2.blockZ)
                                val tx = maxOf(loc1.blockX, loc2.blockX)
                                val ty = maxOf(loc1.blockY, loc2.blockY)
                                val tz = maxOf(loc1.blockZ, loc2.blockZ)
                                data["from"] = "$fx $fy $fz"
                                data["to"] = "$tx $ty $tz"
                                player.langMsg("region.setRegion", "$fx $fy $fz", "$tx $ty $tz")
                                render()
                            } else {
                                player.langMsg("region.bothRequired")
                            }
                        }
                        14 -> {
                            SelectionTool.giveWand(player)
                            render()
                        }
                        15 -> {
                            session.wandCallback = { loc ->
                                data["from"] = session.formatPos(loc)
                                render()
                            }
                            player.closeInventory()
                            player.langMsg("loc.pickHint")
                        }
                        16 -> {
                            session.wandCallback = { loc ->
                                data["to"] = session.formatPos(loc)
                                render()
                            }
                            player.closeInventory()
                            player.langMsg("loc.pickHint")
                        }
                        20 -> InputPrompts.multilineInput(player, player.langStr("inputTitle.agentScript"), regionAgent["onEnter"] as? String) { regionAgent["onEnter"] = it; render() }
                        21 -> InputPrompts.multilineInput(player, player.langStr("inputTitle.agentScript"), regionAgent["onLeave"] as? String) { regionAgent["onLeave"] = it; render() }
                        49 -> { saveEntry(dungeonName, id, data); player.langMsg("region.saved", id); render() }
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

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; 'W' -> 51; else -> 0 }
}

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

object PlanEditor {

    private val TRIGGERS = listOf("PREPARE", "BEGIN", "END", "FAIL")

    fun openList(player: Player, dungeonName: String) {
        val files = YamlIO.listYamlFiles(dungeonName, "plan")
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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(player.langStr("title.planList", dungeonName)) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                GuiItems.compItem(Material.CLOCK, player.lang("plan.name", id), listOf(
                    player.lang("plan.trigger", data["trigger"] ?: "?"),
                    player.lang("plan.delay", data["delay"] ?: "0"),
                    player.lang("plan.period", data["period"] ?: "once"),
                    player.lang("plan.async", data["async"] ?: false),
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
                InputPrompts.textInput(player, player.langStr("inputTitle.planId"), null) { id ->
                    val data = linkedMapOf<String, Any?>("trigger" to "BEGIN", "async" to false)
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
            player.openMenu<Chest>(player.langStr("title.planEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.enumItem("trigger", data["trigger"] as? String, TRIGGERS))
                set(10, GuiItems.numberItem("delay (ticks)", data["delay"] as? Number))
                set(11, GuiItems.numberItem("period (ticks, 0=once)", data["period"] as? Number))
                set(12, GuiItems.toggleItem("async", data["async"] as? Boolean ?: false))
                set(16, GuiItems.fieldItem(Material.COMMAND_BLOCK, "onRun (JS)", data["onRun"]))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.enumSelect(player, "Trigger", TRIGGERS, data["trigger"] as? String) { data["trigger"] = it; render() }
                        10 -> InputPrompts.intInput(player, "Delay (ticks)", (data["delay"] as? Number)?.toInt()) { data["delay"] = it; render() }
                        11 -> InputPrompts.intInput(player, "Period (0=once)", (data["period"] as? Number)?.toInt()) { data["period"] = it; render() }
                        12 -> { data["async"] = !(data["async"] as? Boolean ?: false); render() }
                        16 -> InputPrompts.multilineInput(player, "onRun JS", data["onRun"] as? String) { data["onRun"] = it; render() }
                        49 -> { saveEntry(dungeonName, id, data); player.langMsg("plan.saved", id); render() }
                        50 -> openList(player, dungeonName)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(dungeonName: String, id: String, data: Map<String, Any?>) {
        YamlIO.saveYaml(File(YamlIO.dungeonFolder(dungeonName), "plan/$id.yml"), linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(dungeonName: String, id: String) {
        File(YamlIO.dungeonFolder(dungeonName), "plan/$id.yml").delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

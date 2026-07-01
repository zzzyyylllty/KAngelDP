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

object TaskEditor {

    private val TRIGGERS = listOf(
        "MOB_KILL", "PLAYER_DEATH", "PLAYER_JOIN", "PLAYER_LEAVE",
        "DUNGEON_START", "DUNGEON_COMPLETE", "DUNGEON_FAIL",
        "REGION_ENTER", "REGION_LEAVE", "MONSTER_GROUP_CLEAR",
        "MONSTER_SPAWN", "KIT_OPEN", "BLOCK_BREAK", "BLOCK_PLACE",
        "DAMAGE_TAKEN", "DAMAGE_DEALT", "CUSTOM"
    )

    fun openList(player: Player, dungeonName: String) {
        val files = YamlIO.listYamlFiles(dungeonName, "task")
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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(player.langStr("title.taskList", dungeonName)) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val maxExecStr = data["maxExecutions"]?.toString() ?: player.langStr("task.infinity")
                GuiItems.compItem(Material.TRIPWIRE_HOOK, player.lang("task.name", id), listOf(
                    player.lang("task.trigger", data["trigger"] ?: "?"),
                    player.lang("task.maxExec", maxExecStr),
                    player.lang("task.cooldown", data["cooldown"] ?: 0),
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
                InputPrompts.textInput(player, player.langStr("inputTitle.taskId"), null) { id ->
                    val data = linkedMapOf<String, Any?>("trigger" to "CUSTOM", "filters" to linkedMapOf<String, Any?>(), "maxExecutions" to -1, "cooldown" to 0)
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
            player.openMenu<Chest>(player.langStr("title.taskEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                @Suppress("UNCHECKED_CAST")
                val taskAgent = (data.getOrPut("agent") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)
                set(9, GuiItems.enumItem("trigger", data["trigger"] as? String, TRIGGERS))
                set(10, GuiItems.numberItem("maxExecutions (-1=∞)", data["maxExecutions"] as? Number))
                set(11, GuiItems.numberItem("cooldown (ticks)", data["cooldown"] as? Number))
                set(12, GuiItems.numberItem("priority", data["priority"] as? Number))
                set(16, GuiItems.fieldItem(Material.COMMAND_BLOCK, "agent.onTrigger (JS)", taskAgent["onTrigger"]))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.enumSelect(player, "Trigger", TRIGGERS, data["trigger"] as? String) { data["trigger"] = it; render() }
                        10 -> InputPrompts.intInput(player, "Max Executions", (data["maxExecutions"] as? Number)?.toInt()) { data["maxExecutions"] = it; render() }
                        11 -> InputPrompts.intInput(player, "Cooldown (ticks)", (data["cooldown"] as? Number)?.toInt()) { data["cooldown"] = it; render() }
                        12 -> InputPrompts.intInput(player, "Priority", (data["priority"] as? Number)?.toInt()) { data["priority"] = it; render() }
                        16 -> InputPrompts.multilineInput(player, "onTrigger JS", taskAgent["onTrigger"] as? String) { taskAgent["onTrigger"] = it; render() }
                        49 -> { saveEntry(dungeonName, id, data); player.langMsg("task.saved", id); render() }
                        50 -> openList(player, dungeonName)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(dungeonName: String, id: String, data: Map<String, Any?>) {
        YamlIO.saveYaml(File(YamlIO.dungeonFolder(dungeonName), "task/$id.yml"), linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(dungeonName: String, id: String) {
        File(YamlIO.dungeonFolder(dungeonName), "task/$id.yml").delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

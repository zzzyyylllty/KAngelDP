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
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.PageableChest
import java.io.File

object KitEditor {

    fun openList(player: Player, dungeonName: String) {
        openEditorList(player, dungeonName, "dungeon")
    }

    fun openGlobalList(player: Player) {
        openEditorList(player, "global", "global")
    }

    private fun openEditorList(player: Player, identifier: String, mode: String) {
        val folder = if (mode == "global") File(KAngelDungeon.dataFolder, "kits")
        else File(YamlIO.dungeonFolder(identifier), "kit")

        val allData = linkedMapOf<String, MutableMap<String, Any?>>()
        if (folder.exists()) {
            folder.listFiles { f -> f.extension in setOf("yml", "yaml") }?.forEach { file ->
                val data = YamlIO.loadYaml(file)
                data.forEach { (k, v) ->
                    if (v is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        allData[k] = (v as Map<String, Any?>).toMutableMap()
                    }
                }
            }
        }

        val title = if (mode == "global") player.langStr("title.kitGlobal") else player.langStr("title.kitList", identifier)

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(title) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val rewardsCount = (data["rewards"] as? List<*>)?.size ?: 0
                GuiItems.compItem(
                    Material.CHEST,
                    player.lang("kit.name", id),
                    listOf(
                        player.lang("kit.rewards", rewardsCount.toString()),
                        player.lang("kit.cooldown", (data["cooldown"])?.toString() ?: "0"),
                        player.lang("kit.minMax", (data["minRewards"] ?: 1).toString(), (data["maxRewards"] ?: 1).toString()),
                        Component.empty(),
                        player.lang("common.clickEdit"),
                        player.lang("common.shiftDelete")
                    )
                )
            }
            onClick { event, entry ->
                if (event.clickEvent().isShiftClick) {
                    ConfirmDelete.open(player, entry.key) {
                        deleteEntry(identifier, entry.key, mode)
                        openEditorList(player, identifier, mode)
                    }
                } else openEditor(player, identifier, entry.key, entry.value, mode)
            }
            onClick(getSlot('A')) {
                InputPrompts.textInput(player, player.langStr("inputTitle.kitId"), null) { id ->
                    val data = linkedMapOf<String, Any?>("rewards" to emptyList<Any>(), "minRewards" to 1, "maxRewards" to 1, "cooldown" to 0)
                    openEditor(player, identifier, id, data as MutableMap<String, Any?>, mode)
                }
            }
            set(getSlot('A'), GuiItems.addButton())
            set(getSlot('L'), GuiItems.backButton())
            onClick(getSlot('L')) {
                if (mode == "global") GlobalConfigs.open(player) else CategoryMenu.open(player, identifier)
            }
            set(getSlot('N'), GuiItems.nextPage())
            setNextPage(getSlot('N')) { _, _ -> GuiItems.nextPage() }
            handLocked(true)
        }
    }

    private fun openEditor(player: Player, identifier: String, id: String, data: MutableMap<String, Any?>, mode: String) {
        EditorSession.get(player).enterDungeon(identifier)

        fun render() {
            player.openMenu<Chest>(player.langStr("title.kitEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.fieldItem(Material.OAK_SIGN, "displayName", data["displayName"]))
                set(10, GuiItems.fieldItem(Material.DIAMOND, "icon (material)", data["icon"]))
                set(11, GuiItems.numberItem("cooldown (seconds)", data["cooldown"] as? Number))
                set(12, GuiItems.numberItem("minRewards", data["minRewards"] as? Number))
                set(13, GuiItems.numberItem("maxRewards", data["maxRewards"] as? Number))
                set(14, GuiItems.fieldItem(Material.PAPER, "broadcastMessage", data["broadcastMessage"]))

                val rewardsCount = (data["rewards"] as? List<*>)?.size ?: 0
                set(24, GuiItems.compItem(
                    Material.GOLD_INGOT,
                    player.lang("kit.rewardsTitle"),
                    listOf(
                        player.lang("kit.rewardsCount", rewardsCount.toString()),
                        Component.empty(),
                        player.lang("common.clickManage")
                    )
                ))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.textInput(player, "Display Name", data["displayName"] as? String) { data["displayName"] = it; render() }
                        10 -> InputPrompts.textInput(player, "Icon Material", data["icon"] as? String) { data["icon"] = it; render() }
                        11 -> InputPrompts.intInput(player, "Cooldown (sec)", (data["cooldown"] as? Number)?.toInt()) { data["cooldown"] = it; render() }
                        12 -> InputPrompts.intInput(player, "Min Rewards", (data["minRewards"] as? Number)?.toInt()) { data["minRewards"] = it; render() }
                        13 -> InputPrompts.intInput(player, "Max Rewards", (data["maxRewards"] as? Number)?.toInt()) { data["maxRewards"] = it; render() }
                        14 -> InputPrompts.textInput(player, "Broadcast Message", data["broadcastMessage"] as? String) { data["broadcastMessage"] = it; render() }
                        24 -> openKitRewardsEditor(player, identifier, id, data, mode)
                        49 -> { saveEntry(identifier, id, data, mode); player.langMsg("kit.saved", id); render() }
                        50 -> openEditorList(player, identifier, mode)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun openKitRewardsEditor(player: Player, identifier: String, kitId: String, parentData: MutableMap<String, Any?>, mode: String) {
        @Suppress("UNCHECKED_CAST")
        val rewards = (parentData["rewards"] as? MutableList<Map<String, Any?>>)?.toMutableList() ?: mutableListOf()

        player.openMenu<PageableChest<Map<String, Any?>>>(player.langStr("title.kitRewards")) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { rewards.toList() }
            onGenerate { _, reward, _, _ ->
                val type = reward["type"] as? String ?: "?"
                val itemName = reward["item"] as? String ?: reward["command"] as? String ?: ""
                val rewardNameStr = "<yellow>[$type] ${reward["source"] ?: ""} $itemName</yellow>"
                GuiItems.compItem(
                    Material.GOLD_NUGGET,
                    MiniMessage.miniMessage().deserialize(rewardNameStr),
                    listOf(
                        player.lang("reward.weight", (reward["weight"]?.toString() ?: "1")),
                        player.lang("reward.chance", (reward["chance"]?.toString() ?: "weight-based")),
                        player.lang("reward.amount", (reward["amount"]?.toString() ?: "1")),
                        Component.empty(),
                        player.lang("common.clickEdit"),
                        player.lang("common.shiftDelete")
                    )
                )
            }
            onClick { event, entry ->
                val idx = rewards.indexOf(entry)
                if (event.clickEvent().isShiftClick) {
                    rewards.removeAt(idx)
                    parentData["rewards"] = rewards
                    openKitRewardsEditor(player, identifier, kitId, parentData, mode)
                } else editKitReward(player, rewards, idx, parentData, identifier, kitId, mode)
            }
            onClick(getSlot('A')) {
                val newReward = linkedMapOf<String, Any?>("type" to "ITEM", "source" to "minecraft", "item" to "diamond", "amount" to 1, "weight" to 10)
                rewards.add(newReward)
                parentData["rewards"] = rewards
                editKitReward(player, rewards, rewards.size - 1, parentData, identifier, kitId, mode)
            }
            set(getSlot('A'), GuiItems.addButton())
            set(getSlot('L'), GuiItems.backButton())
            onClick(getSlot('L')) { openEditor(player, identifier, kitId, parentData, mode) }
            set(getSlot('N'), GuiItems.nextPage())
            setNextPage(getSlot('N')) { _, _ -> GuiItems.nextPage() }
            handLocked(true)
        }
    }

    private fun editKitReward(player: Player, list: MutableList<Map<String, Any?>>, index: Int, parentData: MutableMap<String, Any?>, identifier: String, kitId: String, mode: String) {
        val entry = list[index].toMutableMap()

        fun render() {
            player.openMenu<Chest>(player.langStr("title.kitReward", index.toString())) {
                rows(5)
                for (i in 0..44) set(i, GuiItems.border())

                set(9, GuiItems.enumItem("type", entry["type"] as? String, listOf("ITEM", "COMMAND", "SCRIPT", "AGENT")))
                set(10, GuiItems.fieldItem(Material.COMPASS, "source", entry["source"]))
                set(11, GuiItems.fieldItem(Material.DIAMOND, "item", entry["item"]))
                set(12, GuiItems.fieldItem(Material.OAK_SIGN, "amount", entry["amount"]))
                set(13, GuiItems.fieldItem(Material.TRIPWIRE_HOOK, "weight", entry["weight"]))
                set(14, GuiItems.fieldItem(Material.EXPERIENCE_BOTTLE, "chance (%)", entry["chance"]))
                set(15, GuiItems.fieldItem(Material.COMMAND_BLOCK, "command", entry["command"]))

                set(18, GuiItems.fieldItem(Material.BOOK, "script (name)", entry["script"]))
                set(19, GuiItems.enumItem("agentTrigger", entry["agentTrigger"] as? String, listOf("ONCE", "ON_KILL", "ON_COMPLETE")))
                set(20, GuiItems.fieldItem(Material.COMMAND_BLOCK, "agentScript (JS)", entry["agentScript"]))

                set(40, GuiItems.saveButton())
                set(41, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> InputPrompts.enumSelect(player, "Reward Type", listOf("ITEM", "COMMAND", "SCRIPT", "AGENT"), entry["type"] as? String) { entry["type"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        10 -> InputPrompts.textInput(player, "Source", entry["source"] as? String) { entry["source"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        11 -> InputPrompts.textInput(player, "Item ID", entry["item"] as? String) { entry["item"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        12 -> InputPrompts.intInput(player, "Amount", (entry["amount"] as? Number)?.toInt()) { entry["amount"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        13 -> InputPrompts.intInput(player, "Weight", (entry["weight"] as? Number)?.toInt()) { entry["weight"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        14 -> InputPrompts.intInput(player, "Chance (-1=weight)", (entry["chance"] as? Number)?.toInt()) { entry["chance"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        15 -> InputPrompts.textInput(player, "Command", entry["command"] as? String) { entry["command"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        18 -> InputPrompts.textInput(player, "Script Name", entry["script"] as? String) { entry["script"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        19 -> InputPrompts.enumSelect(player, "Agent Trigger", listOf("ONCE", "ON_KILL", "ON_COMPLETE"), entry["agentTrigger"] as? String) { entry["agentTrigger"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        20 -> InputPrompts.multilineInput(player, "Agent Script JS", entry["agentScript"] as? String) { entry["agentScript"] = it; list[index] = entry; parentData["rewards"] = list.toMutableList(); render() }
                        40 -> { parentData["rewards"] = list.toMutableList(); openKitRewardsEditor(player, identifier, kitId, parentData, mode) }
                        41 -> openKitRewardsEditor(player, identifier, kitId, parentData, mode)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(identifier: String, id: String, data: Map<String, Any?>, mode: String) {
        val folder = if (mode == "global") File(KAngelDungeon.dataFolder, "kits") else File(YamlIO.dungeonFolder(identifier), "kit")
        YamlIO.saveYaml(File(folder, "$id.yml"), linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(identifier: String, id: String, mode: String) {
        val folder = if (mode == "global") File(KAngelDungeon.dataFolder, "kits") else File(YamlIO.dungeonFolder(identifier), "kit")
        File(folder, "$id.yml").delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

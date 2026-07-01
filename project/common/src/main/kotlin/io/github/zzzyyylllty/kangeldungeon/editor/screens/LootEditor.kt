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

object LootEditor {

    fun openList(player: Player, dungeonName: String) {
        openEditorList(player, dungeonName, "dungeon")
    }

    fun openGlobalList(player: Player) {
        openEditorList(player, "global", "global")
    }

    private fun openEditorList(player: Player, identifier: String, mode: String) {
        val folder = if (mode == "global") File(KAngelDungeon.dataFolder, "loot")
        else File(YamlIO.dungeonFolder(identifier), "loot")

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

        val title = if (mode == "global") player.langStr("title.lootGlobal") else player.langStr("title.lootList", identifier)

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(title) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val itemsCount = (data["items"] as? List<*>)?.size ?: 0
                val positions = (data["positions"] as? List<*>)?.size ?: 0
                GuiItems.compItem(
                    Material.BARREL,
                    player.lang("loot.name", id),
                    listOf(
                        player.lang("loot.items", itemsCount.toString()),
                        player.lang("loot.positions", positions.toString()),
                        player.lang("loot.refresh", (data["refresh"]?.toString() ?: "true")),
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
                InputPrompts.textInput(player, player.langStr("inputTitle.lootId"), null) { id ->
                    val data = linkedMapOf<String, Any?>("positions" to emptyList<String>(), "refresh" to true, "minItems" to 1, "maxItems" to 3, "items" to emptyList<Any>())
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
            player.openMenu<Chest>(player.langStr("title.lootEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.toggleItem("refresh", data["refresh"] as? Boolean ?: true))
                set(10, GuiItems.numberItem("minItems", data["minItems"] as? Number))
                set(11, GuiItems.numberItem("maxItems", data["maxItems"] as? Number))
                set(12, GuiItems.fieldItem(Material.COMPASS, "positions", (data["positions"] as? List<*>)?.joinToString(", ") { it.toString() }))
                set(13, GuiItems.fieldItem(Material.ITEM_FRAME, "frameCrate (LithiumCarbon)", data["frameCrate"]))
                set(14, GuiItems.fieldItem(Material.COMPASS, "frameCrateFacing", data["frameCrateFacing"] ?: "UP"))

                val itemsCount = (data["items"] as? List<*>)?.size ?: 0
                set(24, GuiItems.compItem(
                    Material.CHEST,
                    player.lang("loot.itemsTitle"),
                    listOf(
                        player.lang("loot.itemsCount", itemsCount.toString()),
                        Component.empty(),
                        player.lang("common.clickManage")
                    )
                ))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> { data["refresh"] = !(data["refresh"] as? Boolean ?: true); render() }
                        10 -> InputPrompts.intInput(player, "Min Items", (data["minItems"] as? Number)?.toInt()) { data["minItems"] = it; render() }
                        11 -> InputPrompts.intInput(player, "Max Items", (data["maxItems"] as? Number)?.toInt()) { data["maxItems"] = it; render() }
                        12 -> InputPrompts.stringListInput(player, "Positions (x y z each)", (data["positions"] as? List<String>) ?: emptyList()) { data["positions"] = it; render() }
                        13 -> InputPrompts.textInput(player, "Frame Crate ID", data["frameCrate"] as? String) { data["frameCrate"] = it; render() }
                        14 -> InputPrompts.textInput(player, "Frame Crate Facing", data["frameCrateFacing"] as? String) { data["frameCrateFacing"] = it; render() }
                        24 -> openLootItemsEditor(player, identifier, id, data, mode)
                        49 -> { saveEntry(identifier, id, data, mode); player.langMsg("loot.saved", id); render() }
                        50 -> openEditorList(player, identifier, mode)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun openLootItemsEditor(player: Player, identifier: String, lootId: String, parentData: MutableMap<String, Any?>, mode: String) {
        @Suppress("UNCHECKED_CAST")
        val items = (parentData["items"] as? MutableList<Map<String, Any?>>)?.toMutableList() ?: mutableListOf()

        player.openMenu<PageableChest<Map<String, Any?>>>(player.langStr("title.lootItems")) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { items.toList() }
            onGenerate { _, item, _, _ ->
                GuiItems.compItem(
                    Material.DIAMOND,
                    MiniMessage.miniMessage().deserialize("<yellow>${item["material"] ?: "?"}</yellow>"),
                    listOf(
                        MiniMessage.miniMessage().deserialize("<gray>Amount: <white>${item["amount"] ?: 1}"),
                        MiniMessage.miniMessage().deserialize("<gray>Chance: <white>${item["chance"] ?: "-1 (weight)"}"),
                        MiniMessage.miniMessage().deserialize("<gray>Weight: <white>${item["weight"] ?: 1}"),
                        MiniMessage.miniMessage().deserialize("<gray>Enchants: <white>${(item["enchantments"] as? List<*>)?.joinToString(", ") ?: "none"}"),
                        Component.empty(),
                        player.lang("common.clickEdit"),
                        player.lang("common.shiftDelete")
                    )
                )
            }
            onClick { event, entry ->
                val idx = items.indexOf(entry)
                if (event.clickEvent().isShiftClick) {
                    items.removeAt(idx)
                    parentData["items"] = items
                    openLootItemsEditor(player, identifier, lootId, parentData, mode)
                } else editLootItem(player, items, idx, parentData, identifier, lootId, mode)
            }
            onClick(getSlot('A')) {
                items.add(linkedMapOf("material" to "STONE", "amount" to 1, "chance" to -1.0, "weight" to 10, "enchantments" to emptyList<String>()))
                parentData["items"] = items
                editLootItem(player, items, items.size - 1, parentData, identifier, lootId, mode)
            }
            set(getSlot('A'), GuiItems.addButton())
            set(getSlot('L'), GuiItems.backButton())
            onClick(getSlot('L')) { openEditor(player, identifier, lootId, parentData, mode) }
            set(getSlot('N'), GuiItems.nextPage())
            setNextPage(getSlot('N')) { _, _ -> GuiItems.nextPage() }
            handLocked(true)
        }
    }

    private fun editLootItem(player: Player, list: MutableList<Map<String, Any?>>, index: Int, parentData: MutableMap<String, Any?>, identifier: String, lootId: String, mode: String) {
        val entry = list[index].toMutableMap()

        fun render() {
            player.openMenu<Chest>(player.langStr("title.lootItem", index.toString())) {
                rows(3)
                for (i in 0..26) set(i, GuiItems.border())

                set(10, GuiItems.fieldItem(Material.DIAMOND, "material", entry["material"]))
                set(11, GuiItems.fieldItem(Material.OAK_SIGN, "amount", entry["amount"]))
                set(12, GuiItems.fieldItem(Material.EXPERIENCE_BOTTLE, "chance (-1=weight)", entry["chance"]))
                set(13, GuiItems.fieldItem(Material.TRIPWIRE_HOOK, "weight (if chance=-1)", entry["weight"]))
                set(14, GuiItems.fieldItem(Material.ENCHANTED_BOOK, "enchantments (format: POWER:3)", (entry["enchantments"] as? List<*>)?.joinToString(", ")))

                set(22, GuiItems.saveButton())
                set(24, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        10 -> InputPrompts.textInput(player, "Material", entry["material"] as? String) { entry["material"] = it; list[index] = entry; parentData["items"] = list.toMutableList(); render() }
                        11 -> InputPrompts.intInput(player, "Amount", (entry["amount"] as? Number)?.toInt()) { entry["amount"] = it; list[index] = entry; parentData["items"] = list.toMutableList(); render() }
                        12 -> InputPrompts.doubleInput(player, "Chance (-1=weight mode)", (entry["chance"] as? Number)?.toDouble()) { entry["chance"] = it; list[index] = entry; parentData["items"] = list.toMutableList(); render() }
                        13 -> InputPrompts.intInput(player, "Weight", (entry["weight"] as? Number)?.toInt()) { entry["weight"] = it; list[index] = entry; parentData["items"] = list.toMutableList(); render() }
                        14 -> InputPrompts.stringListInput(player, "Enchantments (POWER:3 per line)", (entry["enchantments"] as? List<String>) ?: emptyList()) { entry["enchantments"] = it; list[index] = entry; parentData["items"] = list.toMutableList(); render() }
                        22 -> { parentData["items"] = list.toMutableList(); openLootItemsEditor(player, identifier, lootId, parentData, mode) }
                        24 -> openLootItemsEditor(player, identifier, lootId, parentData, mode)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(identifier: String, id: String, data: Map<String, Any?>, mode: String) {
        val folder = if (mode == "global") File(KAngelDungeon.dataFolder, "loot") else File(YamlIO.dungeonFolder(identifier), "loot")
        val file = File(folder, "$id.yml")
        YamlIO.saveYaml(file, linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(identifier: String, id: String, mode: String) {
        val folder = if (mode == "global") File(KAngelDungeon.dataFolder, "loot") else File(YamlIO.dungeonFolder(identifier), "loot")
        File(folder, "$id.yml").delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

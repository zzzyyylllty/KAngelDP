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

object MonsterEditor {

    fun openList(player: Player, dungeonName: String) {
        val files = YamlIO.listYamlFiles(dungeonName, "monster")
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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>(player.langStr("title.monsterList", dungeonName)) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val monsters = (data["monsters"] as? List<*>)?.size ?: 0
                val activeStr = if (data["active"] != false) "<green>${data["active"] ?: true}" else "<red>${data["active"] ?: true}"
                GuiItems.compItem(Material.ZOMBIE_HEAD, player.lang("monster.name", id), listOf(
                    player.lang("monster.active", activeStr),
                    player.lang("common.entries", monsters.toString()),
                    player.lang("monster.priority", (data["priority"] ?: 0).toString()),
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
                InputPrompts.textInput(player, player.langStr("inputTitle.monsterId"), null) { id ->
                    val data = linkedMapOf<String, Any?>("active" to true, "monsters" to emptyList<Any>())
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
            player.openMenu<Chest>(player.langStr("title.monsterEditor", id)) {
                rows(6)
                map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
                set('#', GuiItems.border())

                set(9, GuiItems.toggleItem("active", data["active"] as? Boolean ?: true))
                set(10, GuiItems.numberItem("priority", data["priority"] as? Number))
                set(11, GuiItems.numberItem("respawnCooldown (ticks)", data["respawnCooldown"] as? Number))
                set(12, GuiItems.numberItem("spawnDelay (ticks)", data["spawnDelay"] as? Number))
                set(13, GuiItems.numberItem("spawnInterval (ticks)", data["spawnInterval"] as? Number))
                set(14, GuiItems.numberItem("maxRespawns (-1=∞)", data["maxRespawns"] as? Number))
                set(15, GuiItems.numberItem("healthMultiplier", data["healthMultiplier"] as? Number))
                set(16, GuiItems.numberItem("damageMultiplier", data["damageMultiplier"] as? Number))

                set(18, GuiItems.numberItem("activationRangeMin", data["activationRangeMin"] as? Number))
                set(19, GuiItems.numberItem("activationRangeMax (-1=∞)", data["activationRangeMax"] as? Number))
                set(20, GuiItems.numberItem("leashRange (0=off)", data["leashRange"] as? Number))

                @Suppress("UNCHECKED_CAST")
                val monsterAgent = (data.getOrPut("agent") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>)
                set(24, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", data["spawnCondition"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(25, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", data["respawnCondition"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))

                val monsters = (data["monsters"] as? List<*>)?.size ?: 0
                set(27, GuiItems.compItem(Material.ZOMBIE_SPAWN_EGG, player.lang("monster.spawnEntries"), listOf(
                    player.lang("monster.spawnCount", monsters.toString()),
                    Component.empty(),
                    player.lang("common.clickManage")
                )))

                set(34, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", monsterAgent["onSpawn"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(35, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", monsterAgent["onAllKilled"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(36, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", monsterAgent["onRespawn"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(37, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("inputTitle.agentScript"), listOf(
                    player.lang("field.current", monsterAgent["onEachKill"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> { data["active"] = !(data["active"] as? Boolean ?: true); render() }
                        10 -> InputPrompts.intInput(player, player.langStr("inputTitle.priority"), (data["priority"] as? Number)?.toInt()) { data["priority"] = it; render() }
                        11 -> InputPrompts.intInput(player, "Respawn Cooldown (ticks)", (data["respawnCooldown"] as? Number)?.toInt()) { data["respawnCooldown"] = it; render() }
                        12 -> InputPrompts.intInput(player, "Spawn Delay (ticks)", (data["spawnDelay"] as? Number)?.toInt()) { data["spawnDelay"] = it; render() }
                        13 -> InputPrompts.intInput(player, "Spawn Interval (ticks)", (data["spawnInterval"] as? Number)?.toInt()) { data["spawnInterval"] = it; render() }
                        14 -> InputPrompts.intInput(player, player.langStr("inputTitle.maxRespawns"), (data["maxRespawns"] as? Number)?.toInt()) { data["maxRespawns"] = it; render() }
                        15 -> InputPrompts.doubleInput(player, "Health Multiplier", (data["healthMultiplier"] as? Number)?.toDouble()) { data["healthMultiplier"] = it; render() }
                        16 -> InputPrompts.doubleInput(player, "Damage Multiplier", (data["damageMultiplier"] as? Number)?.toDouble()) { data["damageMultiplier"] = it; render() }
                        18 -> InputPrompts.doubleInput(player, "Activation Range Min", (data["activationRangeMin"] as? Number)?.toDouble()) { data["activationRangeMin"] = it; render() }
                        19 -> InputPrompts.doubleInput(player, "Activation Range Max", (data["activationRangeMax"] as? Number)?.toDouble()) { data["activationRangeMax"] = it; render() }
                        20 -> InputPrompts.doubleInput(player, "Leash Range", (data["leashRange"] as? Number)?.toDouble()) { data["leashRange"] = it; render() }
                        24 -> InputPrompts.multilineInput(player, "spawnCondition JS", data["spawnCondition"] as? String) { data["spawnCondition"] = it; render() }
                        25 -> InputPrompts.multilineInput(player, "respawnCondition JS", data["respawnCondition"] as? String) { data["respawnCondition"] = it; render() }
                        27 -> openSpawnEntries(player, data, id)
                        34 -> InputPrompts.multilineInput(player, "onSpawn JS", monsterAgent["onSpawn"] as? String) { monsterAgent["onSpawn"] = it; render() }
                        35 -> InputPrompts.multilineInput(player, "onAllKilled JS", monsterAgent["onAllKilled"] as? String) { monsterAgent["onAllKilled"] = it; render() }
                        36 -> InputPrompts.multilineInput(player, "onRespawn JS", monsterAgent["onRespawn"] as? String) { monsterAgent["onRespawn"] = it; render() }
                        37 -> InputPrompts.multilineInput(player, "onEachKill JS", monsterAgent["onEachKill"] as? String) { monsterAgent["onEachKill"] = it; render() }
                        49 -> { saveEntry(dungeonName, id, data); player.langMsg("monster.saved", id); render() }
                        50 -> openList(player, dungeonName)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun openSpawnEntries(player: Player, parentData: MutableMap<String, Any?>, monsterId: String) {
        @Suppress("UNCHECKED_CAST")
        val entries = (parentData["monsters"] as? MutableList<Map<String, Any?>>)?.toMutableList() ?: mutableListOf()

        player.openMenu<PageableChest<Map<String, Any?>>>(player.langStr("title.spawnEntries")) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { entries.toList() }
            onGenerate { _, entry, _, _ ->
                GuiItems.compItem(Material.ZOMBIE_SPAWN_EGG, player.lang("monster.name", entry["mob"] ?: "unknown"), listOf(
                    player.lang("spawn.location", entry["location"]?.toString() ?: "?"),
                    player.lang("spawn.amount", (entry["amount"] ?: 1).toString()),
                    player.lang("spawn.scatter", (entry["scattered"] ?: 0).toString()),
                    player.lang("spawn.level", (entry["level"] ?: 0).toString()),
                    Component.empty(),
                    player.lang("common.clickEdit"),
                    player.lang("common.shiftDelete")
                ))
            }
            onClick { event, entry ->
                val idx = entries.indexOf(entry)
                if (event.clickEvent().isShiftClick) {
                    entries.removeAt(idx)
                    parentData["monsters"] = entries
                    openSpawnEntries(player, parentData, monsterId)
                } else {
                    editSpawnEntry(player, entries, idx, parentData, monsterId)
                }
            }
            onClick(getSlot('A')) {
                entries.add(linkedMapOf("mob" to "minecraft:zombie", "location" to "0 0 0", "amount" to 1))
                parentData["monsters"] = entries
                editSpawnEntry(player, entries, entries.size - 1, parentData, monsterId)
            }
            set(getSlot('A'), GuiItems.addButton())
            set(getSlot('L'), GuiItems.backButton())
            onClick(getSlot('L')) { openEditor(player, EditorSession.get(player).dungeonName ?: "", monsterId, parentData) }
            set(getSlot('N'), GuiItems.nextPage())
            setNextPage(getSlot('N')) { _, _ -> GuiItems.nextPage() }
            handLocked(true)
        }
    }

    private fun editSpawnEntry(player: Player, list: MutableList<Map<String, Any?>>, index: Int, parentData: MutableMap<String, Any?>, monsterId: String) {
        val entry = list[index].toMutableMap()
        val session = EditorSession.get(player)

        fun render() {
            player.openMenu<Chest>(player.langStr("title.spawnEntry", index.toString())) {
                rows(3)
                for (i in 0..26) set(i, GuiItems.border())

                set(10, GuiItems.compItem(Material.ZOMBIE_HEAD, player.lang("inputTitle.mobType"), listOf(
                    player.lang("field.current", entry["mob"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(11, GuiItems.compItem(Material.COMPASS, player.lang("inputTitle.location"), listOf(
                    player.lang("field.current", entry["location"]?.toString() ?: "none"),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(12, GuiItems.compItem(Material.OAK_SIGN, player.lang("inputTitle.amount"), listOf(
                    player.lang("field.current", (entry["amount"] ?: 1).toString()),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(13, GuiItems.compItem(Material.FIREWORK_ROCKET, player.lang("inputTitle.scatter"), listOf(
                    player.lang("field.current", (entry["scattered"] ?: 0).toString()),
                    Component.empty(),
                    player.lang("field.clickEdit")
                )))
                set(14, GuiItems.compItem(Material.TARGET, player.lang("loc.currentPos"), listOf(
                    player.lang("loc.currentPosLore")
                )))
                set(15, GuiItems.compItem(Material.LEVER, player.lang("loc.pickBlock"), listOf(
                    player.lang("loc.pickBlockLore"),
                    player.lang("loc.pickBlockLore2")
                )))

                set(22, GuiItems.saveButton())
                set(24, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        10 -> InputPrompts.textInput(player, player.langStr("inputTitle.mobType"), entry["mob"] as? String) { entry["mob"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
                        11 -> InputPrompts.textInput(player, player.langStr("inputTitle.location"), entry["location"] as? String) { entry["location"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
                        12 -> InputPrompts.intInput(player, player.langStr("inputTitle.amount"), (entry["amount"] as? Number)?.toInt()) { entry["amount"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
                        13 -> InputPrompts.intInput(player, player.langStr("inputTitle.scatter"), (entry["scattered"] as? Number)?.toInt()) { entry["scattered"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
                        14 -> {
                            entry["location"] = session.formatPos(player.location)
                            list[index] = entry
                            parentData["monsters"] = list.toMutableList()
                            player.langMsg("loc.set")
                            render()
                        }
                        15 -> {
                            session.wandCallback = { loc ->
                                entry["location"] = session.formatPos(loc)
                                list[index] = entry
                                parentData["monsters"] = list.toMutableList()
                                render()
                            }
                            player.closeInventory()
                            player.langMsg("loc.pickHint")
                        }
                        22 -> { parentData["monsters"] = list.toMutableList(); openSpawnEntries(player, parentData, monsterId) }
                        24 -> openSpawnEntries(player, parentData, monsterId)
                    }
                }
                handLocked(true)
            }
        }
        render()
    }

    private fun saveEntry(dungeonName: String, id: String, data: Map<String, Any?>) {
        YamlIO.saveYaml(File(YamlIO.dungeonFolder(dungeonName), "monster/$id.yml"), linkedMapOf(id to data))
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun deleteEntry(dungeonName: String, id: String) {
        File(YamlIO.dungeonFolder(dungeonName), "monster/$id.yml").delete()
        KAngelDungeon.reloadCustomConfig(async = true)
    }

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 49; 'L' -> 45; 'N' -> 53; else -> 0 }
}

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

        player.openMenu<PageableChest<Map.Entry<String, MutableMap<String, Any?>>>>("§8Monsters: $dungeonName") {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { allData.entries.toList() }
            onGenerate { _, entry, _, _ ->
                val (id, data) = entry
                val monsters = (data["monsters"] as? List<*>)?.size ?: 0
                GuiItems.buildItem(Material.ZOMBIE_HEAD) {
                    name = "<yellow>$id"
                    lore(
                        "<gray>Active: <${if (data["active"] != false) "green" else "red"}>${data["active"] ?: true}",
                        "<gray>Monsters: <white>$monsters entries",
                        "<gray>Priority: <white>${data["priority"] ?: 0}",
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
                InputPrompts.textInput(player, "Monster Group ID", null) { id ->
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
            player.openMenu<Chest>("§8Monster: $id") {
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

                set(24, GuiItems.scriptPlaceholder("spawnCondition"))
                set(25, GuiItems.scriptPlaceholder("respawnCondition"))

                val monsters = (data["monsters"] as? List<*>)?.size ?: 0
                set(27, GuiItems.buildItem(Material.ZOMBIE_SPAWN_EGG) {
                    name = "<yellow>Monster Spawn Entries</yellow>"
                    lore("<gray>Count: <white>$monsters", "", "<gray><italic>Click to manage")
                })

                set(34, GuiItems.scriptPlaceholder("agent.onSpawn"))
                set(35, GuiItems.scriptPlaceholder("agent.onAllKilled"))
                set(36, GuiItems.scriptPlaceholder("agent.onRespawn"))
                set(37, GuiItems.scriptPlaceholder("agent.onEachKill"))

                set(49, GuiItems.saveButton())
                set(50, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        9 -> { data["active"] = !(data["active"] as? Boolean ?: true); render() }
                        10 -> InputPrompts.intInput(player, "Priority", (data["priority"] as? Number)?.toInt()) { data["priority"] = it; render() }
                        11 -> InputPrompts.intInput(player, "Respawn Cooldown (ticks)", (data["respawnCooldown"] as? Number)?.toInt()) { data["respawnCooldown"] = it; render() }
                        12 -> InputPrompts.intInput(player, "Spawn Delay (ticks)", (data["spawnDelay"] as? Number)?.toInt()) { data["spawnDelay"] = it; render() }
                        13 -> InputPrompts.intInput(player, "Spawn Interval (ticks)", (data["spawnInterval"] as? Number)?.toInt()) { data["spawnInterval"] = it; render() }
                        14 -> InputPrompts.intInput(player, "Max Respawns", (data["maxRespawns"] as? Number)?.toInt()) { data["maxRespawns"] = it; render() }
                        15 -> InputPrompts.doubleInput(player, "Health Multiplier", (data["healthMultiplier"] as? Number)?.toDouble()) { data["healthMultiplier"] = it; render() }
                        16 -> InputPrompts.doubleInput(player, "Damage Multiplier", (data["damageMultiplier"] as? Number)?.toDouble()) { data["damageMultiplier"] = it; render() }
                        18 -> InputPrompts.doubleInput(player, "Activation Range Min", (data["activationRangeMin"] as? Number)?.toDouble()) { data["activationRangeMin"] = it; render() }
                        19 -> InputPrompts.doubleInput(player, "Activation Range Max", (data["activationRangeMax"] as? Number)?.toDouble()) { data["activationRangeMax"] = it; render() }
                        20 -> InputPrompts.doubleInput(player, "Leash Range", (data["leashRange"] as? Number)?.toDouble()) { data["leashRange"] = it; render() }
                        27 -> openSpawnEntries(player, data, id)
                        49 -> { saveEntry(dungeonName, id, data); player.sendMessage("§aSaved monster '$id'!"); render() }
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

        player.openMenu<PageableChest<Map<String, Any?>>>("§8Spawn Entries") {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "L###A###N")
            slotsBy('@')
            set('#', GuiItems.border())
            elements { entries.toList() }
            onGenerate { _, entry, _, _ ->
                GuiItems.buildItem(Material.ZOMBIE_SPAWN_EGG) {
                    name = "<yellow>${entry["mob"] ?: "unknown"}</yellow>"
                    lore(
                        "<gray>Location: <white>${entry["location"] ?: "?"}",
                        "<gray>Amount: <white>${entry["amount"] ?: 1}",
                        "<gray>Scatter: <white>${entry["scattered"] ?: 0}",
                        "<gray>Level: <white>${entry["level"] ?: 0}",
                        "", "<gray><italic>Click to edit", "<red><italic>Shift+Click to remove"
                    )
                }
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

        fun render() {
            player.openMenu<Chest>("§8Spawn Entry #$index") {
                rows(3)
                for (i in 0..26) set(i, GuiItems.border())

                set(10, GuiItems.fieldItem(Material.ZOMBIE_HEAD, "mob", entry["mob"]))
                set(11, GuiItems.fieldItem(Material.COMPASS, "location (x y z)", entry["location"]))
                set(12, GuiItems.fieldItem(Material.OAK_SIGN, "amount", entry["amount"]))
                set(13, GuiItems.fieldItem(Material.FIREWORK_ROCKET, "scattered", entry["scattered"]))

                set(22, GuiItems.saveButton())
                set(24, GuiItems.backButton())

                onClick(lock = true) { event ->
                    when (event.rawSlot) {
                        10 -> InputPrompts.textInput(player, "Mob type", entry["mob"] as? String) { entry["mob"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
                        11 -> InputPrompts.textInput(player, "Location (x y z)", entry["location"] as? String) { entry["location"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
                        12 -> InputPrompts.intInput(player, "Amount", (entry["amount"] as? Number)?.toInt()) { entry["amount"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
                        13 -> InputPrompts.intInput(player, "Scatter radius", (entry["scattered"] as? Number)?.toInt()) { entry["scattered"] = it; list[index] = entry; parentData["monsters"] = list.toMutableList(); render() }
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

    private fun getSlot(c: Char): Int = when (c) { 'A' -> 50; 'L' -> 45; 'N' -> 53; else -> 0 }
}

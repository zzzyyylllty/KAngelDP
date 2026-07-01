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

/**
 * Multi-page editor for dungeon option.yml.
 * 4 pages covering all settings.
 */
object DungeonSettings {

    private val mm = MiniMessage.miniMessage()

    private const val PREV_PAGE = 0
    private const val NEXT_PAGE = 8
    private const val SAVE = 49
    private const val BACK = 50

    fun open(player: Player, dungeonName: String) {
        val session = EditorSession.get(player)
        session.enterDungeon(dungeonName)
        val file = YamlIO.dungeonFile(dungeonName, "", "option.yml")
        session.loadFile(file)
        showPage(player, 0)
    }

    private fun showPage(player: Player, page: Int) {
        val session = EditorSession.get(player)
        val data = session.currentData ?: return
        val dungeonName = session.dungeonName ?: return
        val titles = listOf("Display & Map", "Gameplay", "Death & Environment", "Rewards & Misc")
        val title = player.langStr("title.settings", (page + 1).toString(), titles.size.toString(), player.langStr("settings.page${page + 1}"))

        player.openMenu<Chest>(title) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "####S###B")
            set('#', GuiItems.border())

            // Navigation row
            set(NEXT_PAGE, if (page < titles.size - 1) GuiItems.compItem(Material.SPECTRAL_ARROW, player.lang("settings.next")) else GuiItems.border())
            set(PREV_PAGE, if (page > 0) GuiItems.compItem(Material.SPECTRAL_ARROW, player.lang("settings.prev")) else GuiItems.border())

            when (page) {
                0 -> renderPage1(data)
                1 -> renderPage2(data)
                2 -> renderPage3(player, data)
                3 -> renderPage4(data)
            }

            // Bottom bar
            set(SAVE, GuiItems.saveButton())
            set(BACK, GuiItems.backButton())

            onClick(lock = true) { event ->
                when (event.rawSlot) {
                    PREV_PAGE -> if (page > 0) showPage(player, page - 1)
                    NEXT_PAGE -> if (page < titles.size - 1) showPage(player, page + 1)
                    SAVE -> {
                        session.dirty = false
                        YamlIO.saveYaml(YamlIO.dungeonFile(dungeonName, "", "option.yml"), data)
                        KAngelDungeon.reloadCustomConfig(async = true)
                        player.langMsg("settings.saved")
                        showPage(player, page)
                    }
                    BACK -> CategoryMenu.open(player, dungeonName)
                    in 9..44 -> handleClick(page, event.rawSlot, player, data, dungeonName, {
                        session.dirty = true
                        showPage(player, page)
                    })
                }
            }
            handLocked(true)
        }
    }

    // ==================== Page 1: Display & Map ====================

    private fun Chest.renderPage1(data: MutableMap<String, Any?>) {
        val display = getSection(data, "display")
        val map = getSection(data, "map")
        val icon = getSection(display, "icon")
        val gameplay = getSection(data, "gameplay")
        val general = getSection(gameplay, "general")

        setField(9, Material.OAK_SIGN, "display.name", display["name"])
        setField(10, Material.PAPER, "display.description", display["description"])
        setField(11, Material.DIAMOND, "display.icon.material", icon["material"])

        setField(12, Material.GRASS_BLOCK, "map.type", map["type"])
        setField(13, Material.MAP, "map.source", map["source"])
        setField(14, Material.COMPASS, "map.spawn (x y z)", formatSpawn(map["spawn"]))

        setField(15, Material.STRUCTURE_VOID, "schematicFile", data["schematicFile"])
        setField(16, Material.GRASS_BLOCK, "worldTemplate", data["worldTemplate"])

        setField(18, Material.CLOCK, "timeLimit (seconds)", general["timeLimit"])
        setField(19, Material.CLOCK, "preparationTime (seconds)", general["preparationTime"])
        setField(20, Material.TRIPWIRE_HOOK, "requiredPermission", general["requiredPermission"])
        setField(21, Material.COMPASS, "playerSpawnOffset", data["playerSpawnOffset"])
    }

    // ==================== Page 2: Gameplay ====================

    private fun Chest.renderPage2(data: MutableMap<String, Any?>) {
        val gameplay = getSection(data, "gameplay")
        val general = getSection(gameplay, "general")
        val commands = getSection(gameplay, "commands")
        val seq = getSection(gameplay, "sequence")
        val timer = getSection(seq, "timer")
        val vanilla = getSection(data, "vanillaOptions")
        val healthRegain = getSection(vanilla, "healthRegain")

        setToggle(9, "allowRespawn", general)
        setToggle(10, "keepInventory", general)
        setToggle(11, "pvpEnabled", general)
        val nrEnabled = healthRegain.values.any { it == true }
        set(12, GuiItems.toggleItem("naturalRegeneration", nrEnabled))
        setToggle(13, "adventureMode", general)

        setNumber(14, "gameplay.general.minPlayers", general["minPlayers"])
        setNumber(15, "gameplay.general.maxPlayers", general["maxPlayers"])
        setToggle(16, "Allow Party", general, "allowParty")

        setField(18, Material.PAPER, "gameplay.general.bannedItems", general["bannedItems"])
        setField(19, Material.OAK_SIGN, "gameplay.commands.mode", commands["mode"])
        setField(20, Material.PAPER, "gameplay.commands.list", commands["list"])
        setField(21, Material.OAK_SIGN, "gameplay.sequence.timer.mode", timer["mode"])
        setNumber(22, "gameplay.sequence.timer.start (sec)", timer["start"])

        setToggle(24, "Hungry", vanilla, "hungry")
        setToggle(25, "Durability", vanilla, "durability")
        setToggle(26, "Items Drop", vanilla, "itemsDrop")
        setToggle(27, "Items Pickup", vanilla, "itemsPickup")
        setToggle(28, "Food", healthRegain, "food")
        setToggle(29, "Saturation", healthRegain, "saturation")
        setToggle(30, "Potions", healthRegain, "potions")
        setToggle(31, "Other", healthRegain, "other")

        setField(33, Material.COMMAND_BLOCK, "gameRules", data["gameRules"])
        setField(34, Material.PAPER, "breakableBlocks", data["breakableBlocks"])
        val pb = getSection(data, "player-blocks")
        setToggle(35, "Track Placed", pb, "trackPlaced")
        setToggle(36, "Clear On End", pb, "clearOnEnd")
    }

    // ==================== Page 3: Death & Environment ====================

    private fun Chest.renderPage3(player: Player, data: MutableMap<String, Any?>) {
        val gameplay = getSection(data, "gameplay")
        val general = getSection(gameplay, "general")
        val death = getSection(general, "death")
        val keepInv = getSection(general, "keepInventory")
        val env = getSection(data, "environment")
        val visual = getSection(data, "visual")

        // Death settings
        setField(9, Material.SKELETON_SKULL, "death.mode", death["mode"])
        setNumber(10, "death.maxRespawns", death["maxRespawns"])
        setNumber(11, "death.autoRespawnDelay (sec)", death["autoRespawnDelay"])
        setToggle(12, "Keep Inventory On Respawn", death, "keepInventoryOnRespawn")
        setToggle(13, "Respawn At Spawn", death, "respawnAtSpawn")
        setToggle(14, "Keeper Inventory", keepInv, "enabled")

        // Environment
        setToggle(18, "Allow Flight", env, "allowFlight")
        setField(19, Material.OAK_SIGN, "environment.gameMode", env["gameMode"])
        setNumber(20, "environment.flySpeed", env["flySpeed"]?.toString()?.toDoubleOrNull())
        setNumber(21, "environment.walkSpeed", env["walkSpeed"]?.toString()?.toDoubleOrNull())

        setNumber(23, "environment.worldBorder.size", getSection(env, "worldBorder")["size"]?.toString()?.toDoubleOrNull())
        setNumber(24, "environment.timeLock (tick)", env["timeLock"]?.toString()?.toLongOrNull())
        setField(25, Material.OAK_SIGN, "environment.weatherLock", env["weatherLock"])
        setField(26, Material.POTION, "environment.potionEffects", env["potionEffects"])

        // Visual effects (placeholders for script fields)
        setField(27, Material.OAK_SIGN, "visual.startTitle", visual["startTitle"])
        setField(28, Material.OAK_SIGN, "visual.completeTitle", visual["completeTitle"])
        setField(29, Material.OAK_SIGN, "visual.failTitle", visual["failTitle"])

        // Lifecycle agent hooks (editable JS scripts)
        val agentSection = getSection(data, "agent")
        val agentCount = agentSection.size
        set(31, GuiItems.compItem(Material.COMMAND_BLOCK, player.lang("field.lifecycleAgents"), listOf(
            player.lang("field.lifecycleCount", agentCount.toString()),
            Component.empty(),
            player.lang("common.clickManage")
        )))
        set(32, GuiItems.compItem(Material.BARRIER, player.lang("field.joinRequirements"), listOf(
            player.lang("script.line1"),
            player.lang("script.line2"),
            Component.empty(),
            player.lang("script.line3"),
            player.lang("script.line4")
        )))
        set(33, GuiItems.fieldItem(Material.IRON_INGOT, "join-requirements.minLevel", getSection(data, "join-requirements")["minLevel"]))
        set(34, GuiItems.fieldItem(Material.GOLD_INGOT, "join-requirements.requiredMoney", getSection(data, "join-requirements")["requiredMoney"]))
        set(35, GuiItems.fieldItem(Material.PAPER, "join-requirements.requiredPermissions", getSection(data, "join-requirements")["requiredPermissions"]))
    }

    // ==================== Page 4: Rewards & Misc ====================

    private fun Chest.renderPage4(data: MutableMap<String, Any?>) {
        val rewards = getSection(data, "rewards")
        val misc = getSection(data, "misc")
        val bb = getSection(data, "boss-bar")
        val timerBb = getSection(bb, "timer")
        val bossHealth = getSection(bb, "bossHealth")
        val sb = getSection(data, "scoreboard")
        val ds = getSection(data, "difficulty-scaling")
        val dc = getSection(data, "dungeon-chat")
        val pb = getSection(data, "player-blocks")

        // Rewards
        setField(9, Material.PAPER, "rewards.completeCommands", rewards["completeCommands"])
        setField(10, Material.PAPER, "rewards.failCommands", rewards["failCommands"])
        setField(11, Material.GOLD_INGOT, "rewards.completeMoney", rewards["completeMoney"])
        setField(12, Material.EXPERIENCE_BOTTLE, "rewards.completeExperience", rewards["completeExperience"])
        setToggle(13, "rewards.perPlayer", rewards, "perPlayer")

        // Misc
        setToggle(18, "misc.joinWhileRunning", misc, "joinWhileRunning")
        setNumber(19, "misc.maxDeaths", misc["maxDeaths"]?.toString()?.toIntOrNull())
        setField(20, Material.OAK_SIGN, "misc.kickOnMaxDeaths", misc["kickOnMaxDeaths"])
        setNumber(21, "misc.reconnectTimeout (sec)", misc["reconnectTimeout"]?.toString()?.toIntOrNull())

        // Player blocks
        setNumber(23, "player-blocks.maxBlocksPerPlayer", pb["maxBlocksPerPlayer"]?.toString()?.toIntOrNull())

        // BossBar
        setToggle(24, "boss-bar.timer.enabled", timerBb, "enabled")
        setField(25, Material.OAK_SIGN, "boss-bar.timer.color", timerBb["color"])
        setField(26, Material.OAK_SIGN, "boss-bar.timer.style", timerBb["style"])
        setToggle(27, "boss-bar.bossHealth.enabled", bossHealth, "enabled")
        setField(28, Material.OAK_SIGN, "boss-bar.bossHealth.color", bossHealth["color"])

        // Scoreboard
        setToggle(31, "scoreboard.enabled", sb, "enabled")
        setField(32, Material.OAK_SIGN, "scoreboard.title", sb["title"])
        setNumber(33, "scoreboard.updateInterval (ticks)", sb["updateInterval"]?.toString()?.toIntOrNull())

        // Difficulty scaling
        setToggle(34, "difficulty-scaling.enabled", ds, "enabled")
        setNumber(35, "difficulty-scaling.basePlayers", ds["basePlayers"]?.toString()?.toIntOrNull())

        // Dungeon chat
        setToggle(36, "dungeon-chat.enabled", dc, "enabled")
        setField(37, Material.OAK_SIGN, "dungeon-chat.format", dc["format"])
        setField(38, Material.OAK_SIGN, "dungeon-chat.commandAlias", dc["commandAlias"])
    }

    // ==================== Click Handling ====================

    private fun handleClick(page: Int, slot: Int, player: Player, data: MutableMap<String, Any?>, dungeonName: String, rerender: () -> Unit) {
        when (page) {
            0 -> handlePage1Click(slot, player, data, rerender)
            1 -> handlePage2Click(slot, player, data, rerender)
            2 -> handlePage3Click(slot, player, data, rerender)
            3 -> handlePage4Click(slot, player, data, rerender)
        }
    }

    private fun handlePage1Click(slot: Int, player: Player, data: MutableMap<String, Any?>, r: () -> Unit) {
        val display = getSection(data, "display")
        val icon = getSection(display, "icon")
        val map = getSection(data, "map")
        val gameplay = getSection(data, "gameplay")
        val general = getSection(gameplay, "general")

        when (slot) {
            9 -> text(player, "Display Name", display["name"] as? String) { display["name"] = it; r() }
            10 -> text(player, "Description", display["description"] as? String) { display["description"] = it; r() }
            11 -> text(player, "Icon Material", icon["material"] as? String) { icon["material"] = it; r() }
            12 -> enumSelect(player, "Map Type", listOf("MAP", "SCHEMATIC"), map["type"] as? String) { map["type"] = it; r() }
            13 -> text(player, "Map Source", map["source"] as? String) { map["source"] = it; r() }
            14 -> text(player, "Spawn (x y z)", formatSpawn(map["spawn"])) { parseSpawn(it)?.let { v -> map["spawn"] = v }; r() }
            15 -> text(player, "Schematic File", data["schematicFile"] as? String) { data["schematicFile"] = it.ifEmpty { null }; r() }
            16 -> text(player, "World Template", data["worldTemplate"] as? String) { data["worldTemplate"] = it.ifEmpty { null }; r() }
            18 -> number(player, "Time Limit (seconds)", general["timeLimit"] as? Number) { general["timeLimit"] = it.toDouble(); r() }
            19 -> number(player, "Preparation Time (seconds)", general["preparationTime"] as? Number) { general["preparationTime"] = it.toDouble(); r() }
            20 -> text(player, "Required Permission", general["requiredPermission"] as? String) { general["requiredPermission"] = it.ifEmpty { null }; r() }
            21 -> text(player, "Player Spawn Offset", data["playerSpawnOffset"] as? String) { data["playerSpawnOffset"] = it; r() }
        }
    }

    private fun handlePage2Click(slot: Int, player: Player, data: MutableMap<String, Any?>, r: () -> Unit) {
        val gameplay = getSection(data, "gameplay")
        val general = getSection(gameplay, "general")
        val commands = getSection(gameplay, "commands")
        val seq = getSection(gameplay, "sequence")
        val timer = getSection(seq, "timer")
        val vanilla = getSection(data, "vanillaOptions")
        val healthRegain = getSection(vanilla, "healthRegain")

        when (slot) {
            9 -> toggle(general, "allowRespawn", r)
            10 -> toggle(general, "keepInventory", r)
            11 -> toggle(general, "pvpEnabled", r)
            12 -> {
                val currentlyOn = healthRegain.values.any { it == true }
                for (key in healthRegain.keys) {
                    healthRegain[key] = !currentlyOn
                }
                r()
            }
            13 -> toggle(general, "adventureMode", r)
            14 -> number(player, "Min Players", general["minPlayers"] as? Number) { general["minPlayers"] = it.toInt(); r() }
            15 -> number(player, "Max Players", general["maxPlayers"] as? Number) { general["maxPlayers"] = it.toInt(); r() }
            16 -> toggle(general, "allowParty", r)
            18 -> listInput(player, "Banned Items", general["bannedItems"] as? List<String>) { general["bannedItems"] = it; r() }
            19 -> enumSelect(player, "Command Mode", listOf("BLACKLIST", "WHITELIST"), commands["mode"] as? String) { commands["mode"] = it; r() }
            20 -> listInput(player, "Command List", commands["list"] as? List<String>) { commands["list"] = it; r() }
            21 -> enumSelect(player, "Timer Mode", listOf("COUNTDOWN", "STOPWATCH"), timer["mode"] as? String) { timer["mode"] = it; r() }
            22 -> number(player, "Timer Start (sec)", timer["start"] as? Number) { timer["start"] = it.toInt(); r() }
            24 -> toggle(vanilla, "hungry", r)
            25 -> toggle(vanilla, "durability", r)
            26 -> toggle(vanilla, "itemsDrop", r)
            27 -> toggle(vanilla, "itemsPickup", r)
            28 -> toggle(healthRegain, "food", r)
            29 -> toggle(healthRegain, "saturation", r)
            30 -> toggle(healthRegain, "potions", r)
            31 -> toggle(healthRegain, "other", r)
            33 -> text(player, "GameRules (json)", data["gameRules"]?.toString()) { /* maps are complex, show raw */ r() }
            34 -> listInput(player, "Breakable Blocks", data["breakableBlocks"] as? List<String>) { data["breakableBlocks"] = it; r() }
            35 -> toggle(getSection(data, "player-blocks"), "trackPlaced", r)
            36 -> toggle(getSection(data, "player-blocks"), "clearOnEnd", r)
        }
    }

    private fun handlePage3Click(slot: Int, player: Player, data: MutableMap<String, Any?>, r: () -> Unit) {
        val gameplay = getSection(data, "gameplay")
        val general = getSection(gameplay, "general")
        val death = getSection(general, "death")
        val keepInv = getSection(general, "keepInventory")
        val env = getSection(data, "environment")
        val visual = getSection(data, "visual")
        val jr = getSection(data, "join-requirements")

        when (slot) {
            9 -> enumSelect(player, "Death Mode", listOf("RESPAWN", "SPECTATE", "POSSESS", "LEAVE"), death["mode"] as? String) { death["mode"] = it; r() }
            10 -> number(player, "Max Respawns", death["maxRespawns"] as? Number) { death["maxRespawns"] = it.toInt(); r() }
            11 -> number(player, "Auto Respawn Delay (sec)", death["autoRespawnDelay"] as? Number) { death["autoRespawnDelay"] = it.toInt(); r() }
            12 -> toggle(death, "keepInventoryOnRespawn", r)
            13 -> toggle(death, "respawnAtSpawn", r)
            14 -> toggle(keepInv, "enabled", r)
            18 -> toggle(env, "allowFlight", r)
            19 -> text(player, "Game Mode", env["gameMode"] as? String) { env["gameMode"] = it; r() }
            20 -> number(player, "Fly Speed", env["flySpeed"]?.toString()?.toDoubleOrNull()) { env["flySpeed"] = it.toFloat(); r() }
            21 -> number(player, "Walk Speed", env["walkSpeed"]?.toString()?.toDoubleOrNull()) { env["walkSpeed"] = it.toFloat(); r() }
            23 -> number(player, "World Border Size", getSection(env, "worldBorder")["size"]?.toString()?.toDoubleOrNull()) { getSection(env, "worldBorder")["size"] = it.toDouble(); r() }
            24 -> numberLong(player, "Time Lock (tick)", env["timeLock"]?.toString()?.toLongOrNull()) { env["timeLock"] = it; r() }
            25 -> text(player, "Weather Lock", env["weatherLock"] as? String) { env["weatherLock"] = it; r() }
            26 -> listInput(player, "Potion Effects", env["potionEffects"] as? List<String>) { env["potionEffects"] = it; r() }
            27 -> text(player, "Start Title", visual["startTitle"] as? String) { visual["startTitle"] = it; r() }
            28 -> text(player, "Complete Title", visual["completeTitle"] as? String) { visual["completeTitle"] = it; r() }
            29 -> text(player, "Fail Title", visual["failTitle"] as? String) { visual["failTitle"] = it; r() }
            31 -> openLifecycleAgentsEditor(player, data)
            33 -> number(player, "Min Level", jr["minLevel"] as? Number) { jr["minLevel"] = it.toInt(); r() }
            34 -> number(player, "Required Money", jr["requiredMoney"] as? Number) { jr["requiredMoney"] = it.toDouble(); r() }
            35 -> listInput(player, "Required Permissions", jr["requiredPermissions"] as? List<String>) { jr["requiredPermissions"] = it; r() }
        }
    }

    private fun handlePage4Click(slot: Int, player: Player, data: MutableMap<String, Any?>, r: () -> Unit) {
        val rewards = getSection(data, "rewards")
        val misc = getSection(data, "misc")
        val bb = getSection(data, "boss-bar")
        val timerBb = getSection(bb, "timer")
        val bossHealth = getSection(bb, "bossHealth")
        val sb = getSection(data, "scoreboard")
        val ds = getSection(data, "difficulty-scaling")
        val dc = getSection(data, "dungeon-chat")
        val pb = getSection(data, "player-blocks")

        when (slot) {
            9 -> listInput(player, "Complete Commands", rewards["completeCommands"] as? List<String>) { rewards["completeCommands"] = it; r() }
            10 -> listInput(player, "Fail Commands", rewards["failCommands"] as? List<String>) { rewards["failCommands"] = it; r() }
            11 -> number(player, "Complete Money", rewards["completeMoney"] as? Number) { rewards["completeMoney"] = it.toDouble(); r() }
            12 -> number(player, "Complete Experience", rewards["completeExperience"] as? Number) { rewards["completeExperience"] = it.toInt(); r() }
            13 -> toggle(rewards, "perPlayer", r)
            18 -> toggle(misc, "joinWhileRunning", r)
            19 -> int2(player, "Max Deaths", misc["maxDeaths"]?.toString()?.toIntOrNull()) { misc["maxDeaths"] = it; r() }
            20 -> enumSelect(player, "Kick On Max Deaths", listOf("SPECTATE", "LOBBY", "KICK"), misc["kickOnMaxDeaths"] as? String) { misc["kickOnMaxDeaths"] = it; r() }
            21 -> int2(player, "Reconnect Timeout", misc["reconnectTimeout"]?.toString()?.toIntOrNull()) { misc["reconnectTimeout"] = it; r() }
            23 -> int2(player, "Max Blocks/Player", pb["maxBlocksPerPlayer"]?.toString()?.toIntOrNull()) { pb["maxBlocksPerPlayer"] = it; r() }
            24 -> toggle(timerBb, "enabled", r)
            25 -> text(player, "Timer Bar Color", timerBb["color"] as? String) { timerBb["color"] = it; r() }
            26 -> text(player, "Timer Bar Style", timerBb["style"] as? String) { timerBb["style"] = it; r() }
            27 -> toggle(bossHealth, "enabled", r)
            28 -> text(player, "Boss Health Color", bossHealth["color"] as? String) { bossHealth["color"] = it; r() }
            31 -> toggle(sb, "enabled", r)
            32 -> text(player, "Scoreboard Title", sb["title"] as? String) { sb["title"] = it; r() }
            33 -> int2(player, "Update Interval", sb["updateInterval"]?.toString()?.toIntOrNull()) { sb["updateInterval"] = it; r() }
            34 -> toggle(ds, "enabled", r)
            35 -> int2(player, "Base Players", ds["basePlayers"]?.toString()?.toIntOrNull()) { ds["basePlayers"] = it; r() }
            36 -> toggle(dc, "enabled", r)
            37 -> text(player, "Chat Format", dc["format"] as? String) { dc["format"] = it; r() }
            38 -> text(player, "Chat Command Alias", dc["commandAlias"] as? String) { dc["commandAlias"] = it; r() }
        }
    }

    // ==================== Lifecycle Agent Editor ====================

    private val LIFECYCLE_HOOKS = listOf("onStart", "onComplete", "onFail", "onLeave", "onLeaveFail")

    private fun openLifecycleAgentsEditor(player: Player, data: MutableMap<String, Any?>) {
        val agent = getSection(data, "agent")

        player.openMenu<PageableChest<String>>(player.langStr("title.lifecycleAgents")) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#########")
            slotsBy('@')
            set('#', GuiItems.border())

            elements { LIFECYCLE_HOOKS }

            onGenerate { _, hook, _, _ ->
                @Suppress("UNCHECKED_CAST")
                val hookSection = (agent[hook] as? Map<String, Any?>) ?: emptyMap()
                val gjs = hookSection["gjs"] as? String ?: ""
                val lore = mutableListOf<Component>()
                if (gjs.isNotEmpty()) {
                    val preview = if (gjs.length > 40) gjs.take(40) + "..." else gjs
                    lore.add(mm.deserialize("<gray>JS: <white>$preview"))
                } else {
                    lore.add(player.lang("field.lifecycleNoScript"))
                }
                lore.add(Component.empty())
                lore.add(player.lang("field.lifecycleClickEdit"))
                GuiItems.compItem(Material.COMMAND_BLOCK, mm.deserialize("<yellow>$hook</yellow>"), lore)
            }

            onClick { _, hook ->
                @Suppress("UNCHECKED_CAST")
                var hookSection = agent[hook] as? MutableMap<String, Any?>
                if (hookSection == null) {
                    hookSection = linkedMapOf<String, Any?>("trigger" to "ONCE")
                    agent[hook] = hookSection
                }
                val currentJs = hookSection["gjs"] as? String ?: ""
                InputPrompts.multilineInput(player, "$hook JS", currentJs) { js ->
                    hookSection["gjs"] = js
                    openLifecycleAgentsEditor(player, data)
                }
            }

            handLocked(true)
        }
    }

    // ==================== Helpers ====================

    private fun Chest.setField(slot: Int, mat: Material, label: String, value: Any?) {
        set(slot, GuiItems.fieldItem(mat, label, value))
    }

    private fun Chest.setToggle(slot: Int, label: String, map: Map<String, Any?>, key: String = label) {
        set(slot, GuiItems.toggleItem(label, map[key] as? Boolean ?: false))
    }

    private fun Chest.setNumber(slot: Int, label: String, value: Any?) {
        set(slot, GuiItems.numberItem(label, value as? Number))
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSection(data: MutableMap<String, Any?>, key: String): MutableMap<String, Any?> {
        val existing = data[key]
        if (existing is MutableMap<*, *>) return existing as MutableMap<String, Any?>
        val newMap = linkedMapOf<String, Any?>()
        data[key] = newMap
        return newMap
    }

    private fun formatSpawn(spawn: Any?): String {
        if (spawn is Map<*, *>) {
            return "${spawn["x"] ?: 0} ${spawn["y"] ?: 100} ${spawn["z"] ?: 0}"
        }
        return spawn?.toString() ?: "0 100 0"
    }

    private fun parseSpawn(str: String): Map<String, Int>? {
        val parts = str.trim().split(" ")
        if (parts.size < 3) return null
        val x = parts[0].toIntOrNull() ?: return null
        val y = parts[1].toIntOrNull() ?: return null
        val z = parts[2].toIntOrNull() ?: return null
        return linkedMapOf("x" to x, "y" to y, "z" to z)
    }

    // Input wrappers
    private fun text(player: Player, title: String, current: String?, callback: (String) -> Unit) {
        InputPrompts.textInput(player, title, current, callback)
    }

    private fun number(player: Player, title: String, current: Number?, callback: (Number) -> Unit) {
        InputPrompts.numberInput(player, title, current, callback)
    }

    private fun int2(player: Player, title: String, current: Int?, callback: (Int) -> Unit) {
        InputPrompts.intInput(player, title, current, callback)
    }

    private fun numberLong(player: Player, title: String, current: Long?, callback: (Long) -> Unit) {
        InputPrompts.textInput(player, title, current?.toString()) { it.toLongOrNull()?.let(callback) }
    }

    private fun toggle(map: MutableMap<String, Any?>, key: String, rerender: () -> Unit) {
        map[key] = !(map[key] as? Boolean ?: false)
        rerender()
    }

    private fun enumSelect(player: Player, title: String, values: List<String>, current: String?, callback: (String) -> Unit) {
        InputPrompts.enumSelect(player, title, values, current, callback)
    }

    private fun listInput(player: Player, title: String, current: List<String>?, callback: (List<String>) -> Unit) {
        InputPrompts.stringListInput(player, title, current, callback)
    }
}

package io.github.zzzyyylllty.kangeldungeon.data.load

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.config
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.dungeonTemplates
import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.logger.infoL
import io.github.zzzyyylllty.kangeldungeon.logger.severeS
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import org.bukkit.util.Vector
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

fun loadDungeonFiles() {
    infoL("DungeonLoad")
    val dungeonFolder = File(getDataFolder(), "dungeon")
    if (!dungeonFolder.exists()) {
        warningL("DungeonFolderRegen")
        // 释放示例地牢文件
        releaseResourceFile("dungeon/sample/option.yml")
        releaseResourceFile("dungeon/sample/region/sample.yml")
        releaseResourceFile("dungeon/sample/monster/monster.yml")
        releaseResourceFile("dungeon/sample/interact/sample.yml")
        releaseResourceFile("dungeon/sample/script/sample.yml")
        releaseResourceFile("dungeon/sample/obstacle/ironbars.yml")
        releaseResourceFile("dungeon/sample/plan/simpleTimerPlan.yml")
        releaseResourceFile("dungeon/sample/kit/rewards.yml")
        releaseResourceFile("dungeon/sample/task/sample.yml")
        releaseResourceFile("dungeon/sample/difficulty.yml")
        releaseResourceFile("dungeon/sample/script/sample.js")
        // releaseResourceFile 同步释放后文件夹已存在，继续加载
    }
    // 确保 schematics 文件夹存在（用于 WorldEdit .schem 文件）
    val schematicsFolder = File(getDataFolder(), "schematics")
    if (!schematicsFolder.exists()) {
        schematicsFolder.mkdirs()
        infoL("SchematicsFolderCreated")
    }
    val files = dungeonFolder.listFiles()
    if (files == null || files.isEmpty()) {
        warningL("NoDungeonFiles")
        return
    }
    // 加载全局Kit（所有地牢通用）
    loadGlobalKitFiles()

    // 加载全局 JS 脚本（scripts/ 目录下的 .js 文件）
    loadGlobalScriptFiles()

    // 加载全局战利品箱（loot/ 目录）
    loadGlobalLootChestFiles()

    for (file in files) {
        // 如果是目录，加载其中的文件
        if (file.isDirectory) {
            file.listFiles()?.forEach { loadDungeonFile(it) }
        } else {
            loadDungeonFile(file)
        }
    }
}

fun loadDungeonFile(file: File) {
    devLog("Loading dungeon file ${file.name}")

    if (file.isDirectory) {
        file.listFiles()?.forEach { loadDungeonFile(it) }
        return
    }

    // 检测 script/ 子目录下的文件，作为地牢脚本加载
    if (file.parentFile.name == "script") {
        // .js 文件直接加载为 JS 脚本（无需 YAML 包装）
        if (file.name.endsWith(".js", ignoreCase = true)) {
            val dungeonName = file.parentFile.parentFile.name
            loadDungeonJsScriptFile(dungeonName, file)
            return
        }
        loadDungeonScriptFile(file)
        return
    }

    // 检测 monster/ 子目录下的文件，作为地牢怪物配置加载
    if (file.parentFile.name == "monster") {
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonMonsterFile(dungeonName, file)
        return
    }

    // 检测 obstacle/ 子目录下的文件，作为地牢障碍物配置加载
    if (file.parentFile.name == "obstacle") {
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonObstacleFile(dungeonName, file)
        return
    }

    // 检测 loot/ 子目录下的文件，作为地牢战利品箱配置加载
    if (file.parentFile.name == "loot") {
        if (!file.name.endsWith(".yml", ignoreCase = true) && !file.name.endsWith(".yaml", ignoreCase = true)) return
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonLootChestFile(dungeonName, file)
        return
    }

    // 检测 interact/ 子目录下的文件，作为地牢交互配置加载
    if (file.parentFile.name == "interact") {
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonInteractFile(dungeonName, file)
        return
    }

    // 检测 plan/ 子目录下的文件，作为地牢计划配置加载
    if (file.parentFile.name == "plan") {
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonPlanFile(dungeonName, file)
        return
    }

    // 检测 kit/ 子目录下的文件，作为地牢Kit奖励包配置加载
    if (file.parentFile.name == "kit") {
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonKitFile(dungeonName, file)
        return
    }

    // 检测 task/ 子目录下的文件，作为地牢任务配置加载
    if (file.parentFile.name == "task") {
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonTaskFile(dungeonName, file)
        return
    }

    // 处理 option.yml — 将其作为单个地牢模板加载，模板名取父目录名
    if (file.name.equals("option.yml", ignoreCase = true)) {
        val map = multiExtensionLoader(file)
        if (map != null) {
            val dungeonName = file.parentFile.name
            loadDungeon(dungeonName, map, file.parentFile.name)
            devLog("Loaded dungeon template from option.yml: $dungeonName")
        } else {
            ReloadDiagnostics.error("ReloadErrorFileParse", file.absolutePath)
        }
        return
    }

    // 检测 region/ 子目录下的文件，作为地牢区域配置加载
    if (file.parentFile.name == "region") {
        val dungeonName = file.parentFile.parentFile.name
        loadDungeonRegionFile(dungeonName, file)
        return
    }

    // 兼容旧版 region.yml 位于地牢根目录（dungeon/<name>/region.yml）
    if (file.name.equals("region.yml", ignoreCase = true)) {
        val dungeonName = file.parentFile.name
        loadDungeonRegionFile(dungeonName, file)
        return
    }

    // 检测 difficulty.yml — 作为地牢难度配置加载
    if (file.name.equals("difficulty.yml", ignoreCase = true)) {
        val dungeonName = file.parentFile.name
        loadDungeonDifficultyFile(dungeonName, file)
        return
    }

    // 不处理其他文件（如 region.yml 等非地牢模板文件）
    devLog("Skipping non-template file: ${file.name}")
}

@Suppress("UNCHECKED_CAST")
fun loadDungeon(key: String, arg: Map<String, Any?>, folderName: String) {
    // ===== 解析 display =====
    val display = arg["display"] as? Map<String, Any?> ?: emptyMap()
    val displayName = display["name"] as? String ?: key
    val description = (display["description"] as? String)?.lines() ?: emptyList()
    val icon = parseIcon(display["icon"] as? Map<String, Any?>)

    // ===== 解析 map =====
    val mapConfig = arg["map"] as? Map<String, Any?> ?: emptyMap()
    val mapType = mapConfig["type"] as? String ?: "MAP"
    val source = mapConfig["source"] as? String
    if (source == null) {
        ReloadDiagnostics.error("ReloadErrorMapSource", key)
    }
    val schematicFile = if (mapType.equals("SCHEMATIC", ignoreCase = true)) source else null
    val worldTemplate = if (mapType.equals("MAP", ignoreCase = true)) source else null
    val spawnConfig = (mapConfig["spawn"]) as? Map<String, Any?> ?: emptyMap()
    if (spawnConfig.isEmpty()) {
        ReloadDiagnostics.warn("ReloadWarnMapSpawn", key)
    }
    val spawnVector = Vector(
        (spawnConfig["x"] as? Number)?.toDouble() ?: 0.0,
        (spawnConfig["y"] as? Number)?.toDouble() ?: 100.0,
        (spawnConfig["z"] as? Number)?.toDouble() ?: 0.0
    )

    // ===== 解析 meta =====
    val metaSection = arg["meta"] as? Map<String, Any?> ?: emptyMap()
    val globalMeta = metaSection["global"] as? Map<String, Any?> ?: emptyMap()
    val playerMeta = metaSection["player"] as? Map<String, Any?> ?: emptyMap()
    val metaConfig = MetaConfig(global = globalMeta, player = playerMeta)

    // ===== 解析 gameplay =====
    val gameplay = arg["gameplay"] as? Map<String, Any?> ?: emptyMap()
    val general = gameplay["general"] as? Map<String, Any?> ?: emptyMap()

    val timeLimit = (general["timeLimit"] as? Number)?.toDouble() ?: 3600.0
    if (timeLimit <= 0) {
        io.github.zzzyyylllty.kangeldungeon.util.devLog("Dungeon '$key' timeLimit=$timeLimit (<=0), timeout disabled")
    }
    val preparationTime = (general["preparationTime"] as? Number)?.toDouble() ?: 30.0
    val allowRespawn = general["allowRespawn"] as? Boolean ?: false
    val pvpEnabled = general["pvpEnabled"] as? Boolean ?: false
    val requiredPermission = general["requiredPermission"] as? String

    // keepInventory 支持 Boolean 简写或详细 Map 配置
    val keepInventoryRaw = general["keepInventory"]
    val keepInventoryConfig = when (keepInventoryRaw) {
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = keepInventoryRaw as Map<String, Any?>
            KeepInventoryConfig(
                enabled = map["enabled"] as? Boolean ?: false,
                requiredLives = map["requiredLives"] as? Boolean ?: false
            )
        }
        is Boolean -> KeepInventoryConfig(enabled = keepInventoryRaw, requiredLives = false)
        else -> KeepInventoryConfig()
    }

    // 解析 bannedItems
    val bannedItems = (general["bannedItems"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    // 解析 blockPlace / blockBreak 控制
    fun parseBlockControl(section: Map<String, Any?>?): BlockControlConfig {
        if (section == null) return BlockControlConfig()
        val mode = try {
            BlockControlMode.valueOf((section["mode"] as? String)?.uppercase() ?: "BLACKLIST")
        } catch (_: IllegalArgumentException) { BlockControlMode.BLACKLIST }
        val list = (section["list"] as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
        return BlockControlConfig(mode = mode, list = list)
    }
    val blockPlace = parseBlockControl(general["blockPlace"] as? Map<String, Any?>)
    val blockBreak = parseBlockControl(general["blockBreak"] as? Map<String, Any?>)

    // 解析 gameplay.general 其他字段
    val leaveOnDeath = general["leaveOnDeath"] as? Boolean ?: false
    val adventureMode = general["adventureMode"] as? Boolean ?: true
    val minPlayers = (general["minPlayers"] as? Number)?.toInt() ?: 1
    val maxPlayers = (general["maxPlayers"] as? Number)?.toInt() ?: 5
    val allowParty = general["allowParty"] as? Boolean ?: true

    // 解析 Death 配置
    val deathSection = general["death"] as? Map<String, Any?>
    val deathConfig = parseDeathConfig(deathSection)

    // ===== 解析 commands =====
    val commandsSection = gameplay["commands"] as? Map<String, Any?> ?: emptyMap()
    val commandMode = try {
        CommandMode.valueOf((commandsSection["mode"] as? String)?.uppercase() ?: "BLACKLIST")
    } catch (_: IllegalArgumentException) { CommandMode.BLACKLIST }
    val commandList = (commandsSection["list"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    // ===== 解析 sequence.timer =====
    val sequenceSection = gameplay["sequence"] as? Map<String, Any?> ?: emptyMap()
    val timerSection = sequenceSection["timer"] as? Map<String, Any?> ?: emptyMap()
    val timerMode = try {
        TimerMode.valueOf((timerSection["mode"] as? String)?.uppercase() ?: "COUNTDOWN")
    } catch (_: IllegalArgumentException) { TimerMode.COUNTDOWN }
    val timerStart = (timerSection["start"] as? Number)?.toInt() ?: 600

    // ===== 解析 vanillaOptions =====
    val vanillaSection = arg["vanillaOptions"] as? Map<String, Any?> ?: emptyMap()
    val hungry = vanillaSection["hungry"] as? Boolean ?: true
    val durability = vanillaSection["durability"] as? Boolean ?: true
    val itemsDrop = vanillaSection["itemsDrop"] as? Boolean ?: true
    val itemsPickup = vanillaSection["itemsPickup"] as? Boolean ?: true

    // 解析 healthRegain 子项
    val healthRegainSection = vanillaSection["healthRegain"] as? Map<String, Any?>
    val healthRegainConfig = HealthRegainConfig(
        food = (healthRegainSection?.get("food") as? Boolean) ?: false,
        saturation = (healthRegainSection?.get("saturation") as? Boolean) ?: false,
        potions = (healthRegainSection?.get("potions") as? Boolean) ?: false,
        other = (healthRegainSection?.get("other") as? Boolean) ?: false
    )
    // naturalRegeneration 向后兼容：只要有任何方式恢复则为 true
    val naturalRegeneration = healthRegainConfig.isAnyEnabled

    // 解析 spawnpoint (格式: "x y z")
    val spawnpointStr = vanillaSection["spawnpoint"] as? String
    val spawnpointVector = parseSpawnpointString(spawnpointStr)

    // ===== 解析 gameRules（游戏规则） =====
    val gameRules: Map<String, Any> = (vanillaSection["gameRules"] as? Map<String, Any?>)?.mapValues { (_, v) ->
        when (v) {
            is Boolean -> v
            is Number -> v.toInt()
            else -> v.toString()
        }
    } ?: emptyMap()

    // ===== 解析 agent（代理脚本） =====
    val agentSection = arg["agent"] as? Map<String, Any?> ?: emptyMap()
    val agentMap = LinkedHashMap<String, Agent>()
    for ((trigger, script) in agentSection) {
        if (script is String && script.isNotBlank()) {
            agentMap[trigger] = Agent(trigger = trigger, gjs = script)
        }
    }
    val agents = if (agentMap.isNotEmpty()) Agents(agentMap) else null

    // ===== 解析 breakable-blocks（可破坏方块白名单） =====
    val breakableBlocks = (arg["breakable-blocks"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    // ===== 解析 player-blocks（玩家放置方块） =====
    val pbSection = arg["player-blocks"] as? Map<String, Any?> ?: emptyMap()
    val playerBlocks = PlayerBlocksConfig(
        trackPlaced = pbSection["track-placed"] as? Boolean ?: false,
        clearOnEnd = pbSection["clear-on-end"] as? Boolean ?: false,
        maxBlocksPerPlayer = (pbSection["max-blocks-per-player"] as? Number)?.toInt() ?: -1
    )

    // ===== 解析 join-requirements（加入要求） =====
    val joinReqSection = arg["join-requirements"] as? Map<String, Any?> ?: emptyMap()
    val joinMinLevel = (joinReqSection["min-level"] as? Number)?.toInt() ?: 0
    val joinReqPermissions = (joinReqSection["required-permissions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val joinReqMoney = (joinReqSection["required-money"] as? Number)?.toDouble() ?: 0.0
    val joinReqItems = mutableListOf<RequiredItemConfig>()
    val reqItemsRaw = joinReqSection["required-items"] as? List<Map<String, Any?>>
    if (reqItemsRaw != null) {
        for (itemMap in reqItemsRaw) {
            val mat = itemMap["material"] as? String ?: continue
            val amt = (itemMap["amount"] as? Number)?.toInt() ?: 1
            val take = itemMap["take"] as? Boolean ?: false
            joinReqItems.add(RequiredItemConfig(material = mat, amount = amt, take = take))
        }
    }

    // ===== 解析 visual（视觉效果） =====
    fun parseSoundOption(section: Any?): SoundOption? {
        val map = section as? Map<String, Any?> ?: return null
        return SoundOption(
            sound = map["sound"] as? String,
            volume = (map["volume"] as? Number)?.toFloat() ?: 1.0f,
            pitch = (map["pitch"] as? Number)?.toFloat() ?: 1.0f
        )
    }
    val visualSection = arg["visual"] as? Map<String, Any?> ?: emptyMap()
    val visualEffects = VisualEffectsConfig(
        startTitle = visualSection["start-title"] as? String,
        startSubtitle = visualSection["start-subtitle"] as? String,
        startSound = parseSoundOption(visualSection["start-sound"]),
        completeTitle = visualSection["complete-title"] as? String,
        completeSubtitle = visualSection["complete-subtitle"] as? String,
        completeSound = parseSoundOption(visualSection["complete-sound"]),
        failTitle = visualSection["fail-title"] as? String,
        failSubtitle = visualSection["fail-subtitle"] as? String,
        failSound = parseSoundOption(visualSection["fail-sound"])
    )

    // ===== 解析 environment（环境控制） =====
    val envSection = arg["environment"] as? Map<String, Any?> ?: emptyMap()
    val envAllowFlight = envSection["allow-fly"] as? Boolean
    val envGameMode = envSection["game-mode"] as? String
    val envFlySpeed = (envSection["fly-speed"] as? Number)?.toFloat()
    val envWalkSpeed = (envSection["walk-speed"] as? Number)?.toFloat()
    val envTimeLock = (envSection["time-lock"] as? Number)?.toLong()
    val envWeatherLock = envSection["weather-lock"] as? String
    val envWbSection = envSection["world-border"] as? Map<String, Any?>
    val envWorldBorder = if (envWbSection != null) {
        WorldBorderOption(
            size = (envWbSection["size"] as? Number)?.toDouble() ?: 256.0,
            centerX = (envWbSection["center-x"] as? Number)?.toDouble() ?: 0.0,
            centerZ = (envWbSection["center-z"] as? Number)?.toDouble() ?: 0.0
        )
    } else null
    val envPotionEffects = mutableListOf<PotionEffectOption>()
    val potionRaw = envSection["potion-effects"] as? List<Map<String, Any?>>
    if (potionRaw != null) {
        for (pe in potionRaw) {
            val type = pe["type"] as? String ?: continue
            val amp = (pe["amplifier"] as? Number)?.toInt() ?: 0
            val dur = (pe["duration"] as? Number)?.toInt() ?: 30
            envPotionEffects.add(PotionEffectOption(type = type, amplifier = amp, duration = dur))
        }
    }
    val environment = EnvironmentConfig(
        allowFlight = envAllowFlight,
        gameMode = envGameMode,
        flySpeed = envFlySpeed,
        walkSpeed = envWalkSpeed,
        potionEffects = envPotionEffects,
        worldBorder = envWorldBorder,
        timeLock = envTimeLock,
        weatherLock = envWeatherLock
    )

    // ===== 解析 rewards（通关奖励） =====
    val rewardsSection = arg["rewards"] as? Map<String, Any?> ?: emptyMap()
    val completeCmds = (rewardsSection["complete-commands"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val failCmds = (rewardsSection["fail-commands"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val completeMoney = (rewardsSection["complete-money"] as? Number)?.toDouble() ?: 0.0
    val completeExp = (rewardsSection["complete-experience"] as? Number)?.toInt() ?: 0
    val perPlayer = rewardsSection["per-player"] as? Boolean ?: true
    val completeItems = mutableListOf<RewardItemOption>()
    val itemsRaw = rewardsSection["complete-items"] as? List<Map<String, Any?>>
    if (itemsRaw != null) {
        for (im in itemsRaw) {
            val mat = im["material"] as? String ?: continue
            val amt = (im["amount"] as? Number)?.toInt() ?: 1
            completeItems.add(RewardItemOption(material = mat, amount = amt))
        }
    }
    val rewardsConfig = RewardsConfig(
        completeCommands = completeCmds,
        failCommands = failCmds,
        completeItems = completeItems,
        completeMoney = completeMoney,
        completeExperience = completeExp,
        perPlayer = perPlayer
    )

    // ===== 解析 misc（杂项） =====
    val miscSection = arg["misc"] as? Map<String, Any?> ?: emptyMap()
    val joinWhileRunning = miscSection["join-while-running"] as? Boolean ?: false
    val maxDeaths = (miscSection["max-deaths"] as? Number)?.toInt() ?: -1
    val kickOnMaxDeaths = try {
        MaxDeathAction.valueOf((miscSection["kick-on-max-deaths"] as? String)?.uppercase() ?: "SPECTATE")
    } catch (_: IllegalArgumentException) { MaxDeathAction.SPECTATE }
    val titleJoin = miscSection["title-join"] as? String
    val titleLeave = miscSection["title-leave"] as? String
    val reconnectTimeout = (miscSection["reconnect-timeout"] as? Number)?.toInt() ?: 300
    val miscConfig = MiscConfig(
        joinWhileRunning = joinWhileRunning,
        maxDeaths = maxDeaths,
        kickOnMaxDeaths = kickOnMaxDeaths,
        titleJoin = titleJoin,
        titleLeave = titleLeave,
        reconnectTimeout = reconnectTimeout
    )

    // ===== 解析 boss-bar（BossBar 配置） =====
    val bossBarSection = arg["boss-bar"] as? Map<String, Any?> ?: emptyMap()
    val timerBbSection = bossBarSection["timer"] as? Map<String, Any?> ?: emptyMap()
    val bossHbSection = bossBarSection["boss-health"] as? Map<String, Any?> ?: emptyMap()

    fun parseBbColor(v: Any?): BossBarColorOption = try {
        BossBarColorOption.valueOf((v as? String)?.uppercase() ?: "WHITE")
    } catch (_: Exception) { BossBarColorOption.WHITE }
    fun parseBbStyle(v: Any?): BossBarStyleOption = try {
        BossBarStyleOption.valueOf((v as? String)?.uppercase() ?: "SOLID")
    } catch (_: Exception) { BossBarStyleOption.SOLID }

    val timerBossBar = TimerBossBarConfig(
        enabled = timerBbSection["enabled"] as? Boolean ?: false,
        color = parseBbColor(timerBbSection["color"]),
        style = parseBbStyle(timerBbSection["style"]),
        title = timerBbSection["title"] as? String ?: "<gold>⏱ %time% | 存活: %alive%/%total%</gold>",
        prepColor = parseBbColor(timerBbSection["prep-color"]),
        prepStyle = parseBbStyle(timerBbSection["prep-style"]),
        prepTitle = timerBbSection["prep-title"] as? String ?: "<green>准备中: %time%</green>",
        completeColor = parseBbColor(timerBbSection["complete-color"]),
        completeStyle = parseBbStyle(timerBbSection["complete-style"]),
        completeTitle = timerBbSection["complete-title"] as? String ?: "<blue>✔ 通关! %time%</blue>",
        failColor = parseBbColor(timerBbSection["fail-color"]),
        failStyle = parseBbStyle(timerBbSection["fail-style"]),
        failTitle = timerBbSection["fail-title"] as? String ?: "<red>✘ 失败</red>"
    )
    val bossHealthBar = BossHealthBarConfig(
        enabled = bossHbSection["enabled"] as? Boolean ?: false,
        color = parseBbColor(bossHbSection["color"]),
        style = parseBbStyle(bossHbSection["style"]),
        title = bossHbSection["title"] as? String ?: "<red>%boss_name% %hp%/%max_hp%</red>"
    )
    val bossBarConfig = BossBarConfig(timer = timerBossBar, bossHealth = bossHealthBar)

    // ===== 解析 scoreboard（计分板配置） =====
    val sbSection = arg["scoreboard"] as? Map<String, Any?> ?: emptyMap()
    val sbEnabled = sbSection["enabled"] as? Boolean ?: false
    val sbTitle = sbSection["title"] as? String ?: "<gold>KAngelDungeon</gold>"
    val sbUpdateInterval = (sbSection["update-interval"] as? Number)?.toInt() ?: 20
    val sbLines = mutableListOf<ScoreboardLine>()
    val sbLinesRaw = sbSection["lines"] as? List<*>
    if (sbLinesRaw != null) {
        for (item in sbLinesRaw) {
            when (item) {
                is String -> sbLines.add(ScoreboardLine(text = item, usePapi = false))
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = item as Map<String, Any?>
                    sbLines.add(ScoreboardLine(
                        text = map["text"] as? String ?: "",
                        usePapi = map["use-papi"] as? Boolean ?: false
                    ))
                }
                else -> devLog("Ignoring scoreboard line of unexpected type: ${item?.javaClass?.name}")
            }
        }
    } else {
        sbLines.addAll(defaultScoreboardLines())
    }
    val scoreboardConfig = ScoreboardConfig(
        enabled = sbEnabled,
        title = sbTitle,
        lines = sbLines,
        updateInterval = sbUpdateInterval
    )

    // ===== 解析 difficulty-scaling（难度动态缩放） =====
    val dsSection = arg["difficulty-scaling"] as? Map<String, Any?> ?: emptyMap()
    val difficultyScalingConfig = DifficultyScalingConfig(
        enabled = dsSection["enabled"] as? Boolean ?: false,
        basePlayers = (dsSection["base-players"] as? Number)?.toInt() ?: 3,
        healthMultiplierPerExtra = (dsSection["health-multiplier-per-extra"] as? Number)?.toDouble() ?: 0.2,
        damageMultiplierPerExtra = (dsSection["damage-multiplier-per-extra"] as? Number)?.toDouble() ?: 0.1,
        healthMultiplierPerLess = (dsSection["health-multiplier-per-less"] as? Number)?.toDouble() ?: 0.15,
        damageMultiplierPerLess = (dsSection["damage-multiplier-per-less"] as? Number)?.toDouble() ?: 0.1
    )

    // ===== 解析 dungeon-chat（地牢聊天） =====
    val dcSection = arg["dungeon-chat"] as? Map<String, Any?> ?: emptyMap()
    val dungeonChatConfig = DungeonChatConfig(
        enabled = dcSection["enabled"] as? Boolean ?: false,
        format = dcSection["format"] as? String ?: "<gray>[<red>Dungeon</red>]</gray> <yellow>%player%</yellow><gray>:</gray> %message%",
        autoRoute = dcSection["auto-route"] as? Boolean ?: true,
        commandAlias = dcSection["command-alias"] as? String ?: "dchat"
    )

    // ===== 创建地牢模板 =====
    val template = DungeonTemplate(
        name = key,
        displayName = displayName,
        description = description,
        icon = icon,
        schematicFile = schematicFile,
        worldTemplate = worldTemplate,
        schematicPasteLocation = spawnVector.clone() as Vector,
        playerSpawnOffset = spawnVector.clone() as Vector,
        timeLimit = timeLimit,
        preparationTime = preparationTime,
        allowRespawn = allowRespawn,
        keepInventory = keepInventoryConfig.enabled,
        pvpEnabled = pvpEnabled,
        naturalRegeneration = naturalRegeneration,
        requiredPermission = requiredPermission,
        agents = agents,
        // 新字段
        gameplayGeneral = GameplayGeneralConfig(
            leaveOnDeath = leaveOnDeath,
            adventureMode = adventureMode,
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            allowParty = allowParty,
            keepInventory = keepInventoryConfig,
            bannedItems = bannedItems,
            blockPlace = blockPlace,
            blockBreak = blockBreak,
        death = deathConfig
        ),
        commandConfig = CommandConfig(
            mode = commandMode,
            list = commandList
        ),
        timerConfig = TimerConfig(
            mode = timerMode,
            start = timerStart
        ),
        vanillaOptions = VanillaOptions(
            hungry = hungry,
            healthRegain = healthRegainConfig,
            durability = durability,
            itemsDrop = itemsDrop,
            itemsPickup = itemsPickup,
            spawnpoint = spawnpointVector
        ),
        gameRules = gameRules,
        metaConfig = metaConfig,
        joinRequirements = JoinRequirementsConfig(
            minLevel = joinMinLevel,
            requiredPermissions = joinReqPermissions,
            requiredItems = joinReqItems,
            requiredMoney = joinReqMoney
        ),
        visualEffects = visualEffects,
        environment = environment,
        rewardsConfig = rewardsConfig,
        miscConfig = miscConfig,
        breakableBlocks = breakableBlocks,
        playerBlocks = playerBlocks,
        bossBar = bossBarConfig,
        scoreboard = scoreboardConfig,
        difficultyScaling = difficultyScalingConfig,
        dungeonChat = dungeonChatConfig
    )

    dungeonTemplates[key] = template
    devLog("Loaded dungeon template: $key")
}

/**
 * 解析图标配置
 */
private fun parseIcon(data: Map<String, Any?>?): IconConfig? {
    if (data == null) return null
    val material = data["material"] as? String ?: return null
    val parameters = data["parameters"] as? Map<String, Any?> ?: emptyMap()
    return IconConfig(material = material, parameters = parameters)
}

/**
 * 解析 spawnpoint 字符串 "x y z" 为 Vector
 */
private fun parseSpawnpointString(value: String?): Vector? {
    if (value == null) return null
    val parts = value.trim().split("\\s+".toRegex())
    if (parts.size < 3) return null
    val x = parts[0].toDoubleOrNull() ?: return null
    val y = parts[1].toDoubleOrNull() ?: return null
    val z = parts[2].toDoubleOrNull() ?: return null
    return Vector(x, y, z)
}

/**
 * 加载地牢脚本文件（位于 dungeon/<dungeon-id>/script/ 目录下）
 * YAML 格式：
 *   scriptName:
 *     onRun: |-
 *       JS code
 *     onPost: |-
 *       JS code
 */
fun loadDungeonScriptFile(file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.script"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }

    val dungeonId = file.parentFile.parentFile.name
    val map = multiExtensionLoader(file) ?: run {
        ReloadDiagnostics.warn("ReloadWarnFileParse", file.absolutePath)
        return
    }

    val dungeonScriptMap = KAngelDungeon.dungeonScripts.getOrPut(dungeonId) { ConcurrentHashMap() }

    for (entry in map.entries) {
        val scriptName = entry.key
        val scriptData = entry.value as? Map<String, Any?> ?: continue

        val script = DungeonScript(
            name = scriptName,
            onRun = scriptData["onRun"] as? String,
            onPost = scriptData["onPost"] as? String
        )
        if (script.onRun == null && script.onPost == null) {
            ReloadDiagnostics.warn("ReloadWarnScriptNoop", dungeonId, scriptName)
        }
        dungeonScriptMap[scriptName] = script
        devLog("Loaded dungeon script: $dungeonId/$scriptName")
    }
}

/**
 * 加载自建 .js 脚本文件（位于 dungeon/<dungeon-id>/script/ 目录下）
 * 文件名（不含扩展名）作为脚本名，文件全部内容作为 onRun 代码
 */
fun loadDungeonJsScriptFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.script"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val scriptName = file.nameWithoutExtension
    val content = try {
        file.readText()
    } catch (e: Exception) {
        devLog("Failed to read JS script file: ${file.absolutePath}")
        return
    }
    if (content.isBlank()) {
        ReloadDiagnostics.warn("ReloadWarnScriptNoop", dungeonName, scriptName)
        return
    }
    val dungeonScriptMap = KAngelDungeon.dungeonScripts.getOrPut(dungeonName) { ConcurrentHashMap() }
    dungeonScriptMap[scriptName] = DungeonScript(name = scriptName, onRun = content, onPost = null)
    devLog("Loaded dungeon JS script: $dungeonName/$scriptName")
}

/**
 * 加载全局 JS 脚本文件（位于 scripts/ 目录下）
 * 存储在 KAngelDungeon.globalScripts 中，所有地牢可通过 instance.runScript() 调用
 */
fun loadGlobalScriptFiles() {
    val scriptsFolder = File(getDataFolder(), "scripts")
    if (!scriptsFolder.exists()) {
        scriptsFolder.mkdirs()
        return
    }
    val files = scriptsFolder.listFiles() ?: return
    for (file in files) {
        if (file.isFile && file.name.endsWith(".js", ignoreCase = true)) {
            if (!checkRegexMatch(file.name, (config["file-load.script"] ?: ".*").toString())) {
                devLog("${file.name} not match regex, skipping...")
                continue
            }
            val scriptName = file.nameWithoutExtension
            val content = try {
                file.readText()
            } catch (e: Exception) {
                devLog("Failed to read global script: ${file.name}")
                continue
            }
            if (content.isBlank()) continue
            KAngelDungeon.globalScripts[scriptName] = DungeonScript(name = scriptName, onRun = content, onPost = null)
            devLog("Loaded global JS script: $scriptName")
        }
    }
}

/**
 * 加载地牢怪物配置文件（位于 dungeon/<dungeon-id>/monster/ 目录下）
 * 存储到 dungeonMonsterConfigs[dungeonName][monsterId]
 */
fun loadDungeonMonsterFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.monster"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonMonsterConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }
    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val config = parseMonsterConfig(key, value)
        if (config != null) {
            dungeonMap[key] = config
            devLog("Loaded per-dungeon monster: $dungeonName/$key")
        } else {
            ReloadDiagnostics.warn("ReloadWarnMonsterParse", dungeonName, key)
        }
    }
}

/**
 * 加载地牢障碍物配置文件（位于 dungeon/<dungeon-id>/obstacle/ 目录下）
 * 存储到 dungeonObstacleConfigs[dungeonName][obstacleId]
 */
fun loadDungeonObstacleFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.obstacle"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonObstacleConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }
    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val config = parseObstacleConfig(key, value)
        if (config != null) {
            dungeonMap[key] = config
            devLog("Loaded per-dungeon obstacle: $dungeonName/$key")
        } else {
            ReloadDiagnostics.warn("ReloadWarnObstacleParse", dungeonName, key)
        }
    }
}

/**
 * 加载地牢交互配置文件（位于 dungeon/<dungeon-id>/interact/ 目录下）
 * 存储到 dungeonInteractConfigs[dungeonName][interactId]
 */
fun loadDungeonInteractFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.interact"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonInteractConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }
    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val config = parseInteractConfig(key, value)
        if (config != null) {
            dungeonMap[key] = config
            devLog("Loaded per-dungeon interact: $dungeonName/$key")
        } else {
            ReloadDiagnostics.warn("ReloadWarnInteractParse", dungeonName, key)
        }
    }
}

/**
 * 加载地牢计划配置文件（位于 dungeon/<dungeon-id>/plan/ 目录下）
 * YAML 格式：
 *   planName:
 *     trigger: BEGIN       # PREPARE / BEGIN / END / FAIL
 *     delay: 200           # 首次执行延迟（tick）
 *     period: 200          # 重复间隔（tick），不设置则表示只执行一次
 *     async: false         # 是否异步执行
 *     agent:
 *       onRun: |-          # 要执行的 JS 脚本
 *         instance.sendMessageToAllPlayers("hello");
 */
fun loadDungeonPlanFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.plan"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonPlanConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }

    for (entry in map.entries) {
        val planName = entry.key
        val data = entry.value as? Map<String, Any?> ?: continue

        val trigger = data["trigger"] as? String ?: "BEGIN"
        val delay = (data["delay"] as? Number)?.toInt()
        val period = (data["period"] as? Number)?.toInt()
        val async = data["async"] as? Boolean ?: false
        val agentSection = data["agent"] as? Map<String, Any?>
        val onRun = agentSection?.get("onRun") as? String

        if (onRun == null) {
            ReloadDiagnostics.warn("ReloadWarnPlanNoRun", dungeonName, planName)
        }
        val plan = Plan(
            name = planName,
            trigger = trigger,
            delay = delay,
            period = period,
            async = async,
            onRun = onRun
        )
        dungeonMap[planName] = plan
        devLog("Loaded dungeon plan: $dungeonName/$planName (trigger=$trigger, delay=$delay, period=$period)")
    }
}

/**
 * 加载地牢区域配置文件（位于 dungeon/<dungeon-id>/region/ 目录下）
 * YAML 格式：
 *   regionName:
 *     from: "x1 y1 z1"
 *     to: "x2 y2 z2"
 *     agent:
 *       onEnter: |-
 *         player.sendMessage("Entered!");
 *       onLeave: |-
 *         player.sendMessage("Left!");
 */
fun loadDungeonRegionFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.region"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonRegionConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }
    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val config = parseRegionConfig(key, value)
        if (config != null) {
            dungeonMap[key] = config
            devLog("Loaded per-dungeon region: $dungeonName/$key")
        } else {
            ReloadDiagnostics.warn("ReloadWarnRegionParse", dungeonName, key)
        }
    }
}

/**
 * 解析区域配置
 */
fun parseRegionConfig(id: String, data: Map<String, Any?>): RegionConfig? {
    try {
        val fromStr = data["from"] as? String ?: return null
        val toStr = data["to"] as? String ?: return null
        val fromParts = fromStr.split(" ").mapNotNull { it.toIntOrNull() }
        val toParts = toStr.split(" ").mapNotNull { it.toIntOrNull() }
        if (fromParts.size < 3 || toParts.size < 3) return null

        val agentRaw = data["agent"] as? Map<String, Any?>
        val agent = if (agentRaw != null) {
            RegionAgent(
                onEnter = agentRaw["onEnter"] as? String,
                onLeave = agentRaw["onLeave"] as? String
            )
        } else null

        return RegionConfig(
            id = id,
            from = RegionPos(fromParts[0], fromParts[1], fromParts[2]),
            to = RegionPos(toParts[0], toParts[1], toParts[2]),
            agent = agent
        )
    } catch (e: Exception) {
        devLog("Failed to parse region config '$id': ${e.message}")
        return null
    }
}

/**
 * 解析交互配置
 */
fun parseInteractConfig(id: String, data: Map<String, Any?>): InteractConfig? {
    try {
        val pos = data["pos"] as? String ?: return null
        val agentRaw = data["agent"] as? Map<String, Any?>
        val agent = if (agentRaw != null) {
            InteractAgent(
                onActive = agentRaw["onActive"] as? String,
                onPost = agentRaw["onPost"] as? String
            )
        } else null
        return InteractConfig(id = id, pos = pos, agent = agent)
    } catch (e: Exception) {
        devLog("Failed to parse interact config '$id': ${e.message}")
        return null
    }
}

/**
 * 加载地牢任务配置文件（位于 dungeon/<dungeon-id>/task/ 目录下）
 * YAML 格式：
 *   taskId:
 *     trigger: MOB_KILL
 *     filters:
 *       mob_type: ZOMBIE
 *     maxExecutions: 10
 *     cooldown: 100
 *     agent:
 *       onTrigger: |-
 *         instance.sendMessageToAllPlayers("Task triggered!");
 */
fun loadDungeonTaskFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.task"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonTaskConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }
    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val taskConfig = parseTaskConfig(key, value)
        if (taskConfig != null) {
            dungeonMap[key] = taskConfig
            devLog("Loaded dungeon task: $dungeonName/$key")
        } else {
            ReloadDiagnostics.warn("ReloadWarnTaskParse", dungeonName, key)
        }
    }
}

/**
 * 解析任务配置
 */
fun parseTaskConfig(id: String, data: Map<String, Any?>): TaskConfig? {
    try {
        val trigger = data["trigger"] as? String ?: return null
        val filtersRaw = data["filters"] as? Map<String, Any?>
        val filters = filtersRaw?.mapValues { (_, v) -> v.toString() } ?: emptyMap()
        val maxExecutions = (data["maxExecutions"] as? Number)?.toInt() ?: -1
        val cooldown = (data["cooldown"] as? Number)?.toLong() ?: 0L
        val priority = (data["priority"] as? Number)?.toInt() ?: 0
        val agentRaw = data["agent"] as? Map<String, Any?>
        val agent = if (agentRaw != null) {
            TaskAgent(onTrigger = agentRaw["onTrigger"] as? String)
        } else null
        return TaskConfig(
            id = id,
            trigger = trigger,
            filters = filters,
            maxExecutions = maxExecutions,
            cooldown = cooldown,
            priority = priority,
            agent = agent
        )
    } catch (e: Exception) {
        devLog("Failed to parse task config '$id': ${e.message}")
        return null
    }
}

/**
 * 加载地牢难度配置文件（位于 dungeon/<dungeon-id>/difficulty.yml）
 * YAML 格式：
 *   difficulties:
 *     easy:
 *       display: "Easy"
 *       description: "For beginners"
 *       meta:
 *         global:
 *           health_multiplier: 0.8
 *       agents:
 *         onStart: |-
 *           instance.sendMessageToAllPlayers("Easy mode!");
 *     normal: ...
 *     hard: ...
 */
fun loadDungeonDifficultyFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.difficulty"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val difficultiesSection = map["difficulties"] as? Map<String, Any?> ?: return
    if (difficultiesSection.isEmpty()) {
        ReloadDiagnostics.warn("ReloadWarnDifficultyEmpty", dungeonName)
        return
    }
    val dungeonMap = KAngelDungeon.dungeonDifficultyConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }

    for (entry in difficultiesSection.entries) {
        val diffId = entry.key
        val data = entry.value as? Map<String, Any?> ?: continue

        val display = data["display"] as? String ?: diffId
        val description = data["description"] as? String ?: ""
        val metaSection = data["meta"] as? Map<String, Any?> ?: emptyMap()
        val agentsSection = data["agents"] as? Map<String, Any?> ?: emptyMap()

        // 合并 meta（global + player）
        val mergedMeta = mutableMapOf<String, Any?>()
        val globalMeta = metaSection["global"] as? Map<String, Any?> ?: emptyMap()
        val playerMeta = metaSection["player"] as? Map<String, Any?> ?: emptyMap()
        mergedMeta["global"] = globalMeta
        mergedMeta["player"] = playerMeta

        // 收集代理脚本
        val agents = mutableMapOf<String, String>()
        for ((trigger, script) in agentsSection) {
            if (script is String && script.isNotBlank()) {
                agents[trigger] = script
            }
        }

        val config = DifficultyConfig(
            id = diffId,
            display = display,
            description = description,
            meta = mergedMeta,
            agents = agents
        )
        dungeonMap[diffId] = config
        devLog("Loaded dungeon difficulty: $dungeonName/$diffId")
    }
}

/**
 * 获取难度起始 meta 合并后的 Map（用于创建地牢时初始化 meta）
 */
fun getDifficultyGlobalMeta(dungeonName: String, difficultyId: String?): Map<String, Any?> {
    if (difficultyId == null) return emptyMap()
    val diffConfig = KAngelDungeon.dungeonDifficultyConfigs[dungeonName]?.get(difficultyId) ?: return emptyMap()
    @Suppress("UNCHECKED_CAST")
    return (diffConfig.meta["global"] as? Map<String, Any?>) ?: emptyMap()
}

/**
 * 加载地牢战利品箱配置文件（位于 dungeon/<dungeon-id>/loot/ 目录下）
 */
fun loadDungeonLootChestFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (config["file-load.loot"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonLootChestConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }
    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val config = parseLootChestConfig(key, value)
        if (config != null) {
            dungeonMap[key] = config
            devLog("Loaded per-dungeon loot chest: $dungeonName/$key")
        } else {
            ReloadDiagnostics.warn("ReloadWarnLootChestParse", dungeonName, key)
        }
    }
}

/**
 * 解析战利品箱配置
 */
fun parseLootChestConfig(id: String, data: Map<String, Any?>): LootChestConfig? {
    try {
        val positions = (data["positions"] as? List<*>)?.mapNotNull { it?.toString() } ?: return null
        if (positions.isEmpty()) return null
        val refresh = data["refresh"] as? Boolean ?: true
        val minItems = (data["min-items"] as? Number)?.toInt() ?: 1
        val maxItems = (data["max-items"] as? Number)?.toInt() ?: 3
        val itemsRaw = data["items"] as? List<*> ?: emptyList<Any>()
        val items = mutableListOf<LootChestItem>()
        for (raw in itemsRaw) {
            val im = raw as? Map<*, *> ?: continue
            @Suppress("UNCHECKED_CAST")
            val itemMap = im as Map<String, Any?>
            val mat = itemMap["material"] as? String ?: continue
            val amt = (itemMap["amount"] as? Number)?.toInt() ?: 1
            val chance = (itemMap["chance"] as? Number)?.toDouble() ?: -1.0
            val weight = (itemMap["weight"] as? Number)?.toInt() ?: 1
            val displayName = itemMap["display-name"] as? String
            val lore = (itemMap["lore"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val enchantments = (itemMap["enchantments"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val nbt = itemMap["nbt"] as? String
            items.add(LootChestItem(
                material = mat.uppercase(), amount = amt, chance = chance,
                weight = weight, displayName = displayName, lore = lore,
                enchantments = enchantments, nbt = nbt
            ))
        }
        val frameCrate = data["frame-crate"] as? String
        val frameCrateFacing = (data["frame-crate-facing"] as? String)?.uppercase() ?: "UP"
        return LootChestConfig(
            id = id, positions = positions, refresh = refresh,
            minItems = minItems.coerceAtLeast(1), maxItems = maxItems.coerceAtLeast(minItems),
            items = items, frameCrate = frameCrate, frameCrateFacing = frameCrateFacing
        )
    } catch (e: Exception) {
        devLog("Failed to parse loot chest config '$id': ${e.message}")
        return null
    }
}

/**
 * 加载全局战利品箱配置文件（位于 loot/ 目录下，所有地瓜通用）
 */
fun loadGlobalLootChestFiles() {
    val lootFolder = File(getDataFolder(), "loot")
    if (!lootFolder.exists()) {
        lootFolder.mkdirs()
        return
    }
    lootFolder.listFiles()?.forEach { file ->
        if (file.isFile && (file.name.endsWith(".yml") || file.name.endsWith(".yaml"))) {
            if (!checkRegexMatch(file.name, (config["file-load.loot"] ?: ".*").toString())) return@forEach
            val map = multiExtensionLoader(file) ?: return@forEach
            for (entry in map.entries) {
                val key = entry.key
                val value = entry.value as? Map<String, Any?> ?: continue
                val cfg = parseLootChestConfig(key, value) ?: continue
                KAngelDungeon.lootChestConfigs[key] = cfg
                devLog("Loaded global loot chest: $key")
            }
        }
    }
}

private fun parseDeathConfig(section: Map<String, Any?>?): DeathConfig {
    if (section == null) {
        return DeathConfig()
    }
    val modeStr = section["mode"] as? String ?: "RESPAWN"
    val mode = try {
        DeathMode.valueOf(modeStr.uppercase())
    } catch (_: IllegalArgumentException) {
        ReloadDiagnostics.warn("ReloadWarnDeathMode", modeStr)
        DeathMode.RESPAWN
    }
    val maxRespawns = (section["maxRespawns"] as? Number)?.toInt() ?: 0
    val autoRespawnDelay = (section["autoRespawnDelay"] as? Number)?.toInt() ?: 0
    val keepInventoryOnRespawn = section["keepInventoryOnRespawn"] as? Boolean ?: false
    val respawnAtSpawn = section["respawnAtSpawn"] as? Boolean ?: true
    return DeathConfig(
        mode = mode,
        maxRespawns = maxRespawns,
        autoRespawnDelay = autoRespawnDelay,
        keepInventoryOnRespawn = keepInventoryOnRespawn,
        respawnAtSpawn = respawnAtSpawn
    )
}
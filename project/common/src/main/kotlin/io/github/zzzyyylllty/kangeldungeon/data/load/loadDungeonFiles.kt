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
    val c = ConfigUtil

    // ===== 解析 display =====
    val display = arg["display"] as? Map<String, Any?> ?: emptyMap()
    val displayName = display["name"] as? String ?: key
    val description = (display["description"] as? String)?.lines() ?: emptyList()
    val icon = parseIcon(display["icon"] as? Map<String, Any?>)

    // ===== 解析 map =====
    val mapConfig = arg["map"] as? Map<String, Any?> ?: emptyMap()
    val mapType = mapConfig["type"] as? String ?: "MAP"
    val source = mapConfig["source"] as? String
    val schematicFile = if (mapType.equals("SCHEMATIC", ignoreCase = true)) source else null
    val worldTemplate = if (mapType.equals("MAP", ignoreCase = true)) source else null
    val spawnConfig = (mapConfig["spawn"]) as? Map<String, Any?> ?: emptyMap()
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

    val timeLimit = (general["TimeLimit"] as? Number)?.toDouble() ?: 3600.0
    val preparationTime = (general["PreparationTime"] as? Number)?.toDouble() ?: 30.0
    val allowRespawn = general["AllowRespawn"] as? Boolean ?: false
    val keepInventory = general["KeepInventory"] as? Boolean ?: false
    val pvpEnabled = general["PvPEnabled"] as? Boolean ?: false
    val requiredPermission = general["RequiredPermission"] as? String

    // 解析 keep-inventory 详细配置
    val keepInvSection = general["keep-inventory"] as? Map<String, Any?>
    val keepInventoryConfig = KeepInventoryConfig(
        enabled = (keepInvSection?.get("enabled") as? Boolean) ?: keepInventory,
        requiredLives = (keepInvSection?.get("required-lives") as? Boolean) ?: false
    )

    // 解析 bannedItems
    val bannedItems = (general["bannedItems"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    // 解析 block-place / block-break 控制
    fun parseBlockControl(section: Map<String, Any?>?): BlockControlConfig {
        if (section == null) return BlockControlConfig()
        val mode = try {
            BlockControlMode.valueOf((section["mode"] as? String)?.uppercase() ?: "BLACKLIST")
        } catch (_: IllegalArgumentException) { BlockControlMode.BLACKLIST }
        val list = (section["list"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        return BlockControlConfig(mode = mode, list = list)
    }
    val blockPlace = parseBlockControl(general["block-place"] as? Map<String, Any?>)
    val blockBreak = parseBlockControl(general["block-break"] as? Map<String, Any?>)

    // 解析 gameplay.general 其他字段
    val leaveOnDeath = general["LeaveOnDeath"] as? Boolean ?: false
    val adventureMode = general["adventureMode"] as? Boolean ?: true
    val minPlayers = (general["MinPlayers"] as? Number)?.toInt() ?: 1
    val maxPlayers = (general["MaxPlayers"] as? Number)?.toInt() ?: 5
    val allowParty = general["allowParty"] as? Boolean ?: true

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

    // ===== 解析 vanilla-options =====
    val vanillaSection = arg["vanilla-options"] as? Map<String, Any?> ?: emptyMap()
    val hungry = vanillaSection["hungry"] as? Boolean ?: true
    val durability = vanillaSection["durability"] as? Boolean ?: true
    val itemsDrop = vanillaSection["items-drop"] as? Boolean ?: true
    val itemsPickup = vanillaSection["items-pickup"] as? Boolean ?: true

    // 解析 HealthRegain 子项
    val healthRegainSection = vanillaSection["HealthRegain"] as? Map<String, Any?>
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

    // ===== 解析 game-rules（游戏规则） =====
    val gameRules: Map<String, Any> = (vanillaSection["game-rules"] as? Map<String, Any?>)?.mapValues { (_, v) ->
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
            blockBreak = blockBreak
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
        metaConfig = metaConfig
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
    val map = multiExtensionLoader(file) ?: return

    val dungeonScriptMap = KAngelDungeon.dungeonScripts.getOrPut(dungeonId) { ConcurrentHashMap() }

    for (entry in map.entries) {
        val scriptName = entry.key
        val scriptData = entry.value as? Map<String, Any?> ?: continue

        val script = DungeonScript(
            name = scriptName,
            onRun = scriptData["onRun"] as? String,
            onPost = scriptData["onPost"] as? String
        )
        dungeonScriptMap[scriptName] = script
        devLog("Loaded dungeon script: $dungeonId/$scriptName")
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
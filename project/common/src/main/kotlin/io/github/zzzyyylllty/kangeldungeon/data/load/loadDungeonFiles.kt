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
        releaseResourceFile("dungeon/sample/region.yml")
        releaseResourceFile("dungeon/sample/monster.yml")
        releaseResourceFile("dungeon/sample/script/sample.yml")
        // 不要 return，继续加载已释放的文件
        Unit
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

    // 检查文件扩展名是否匹配配置的正则表达式
    if (!checkRegexMatch(file.name, (config["file-load.dungeon"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }

    val map = multiExtensionLoader(file)
    if (map != null) {
        for (entry in map.entries) {
            val key = entry.key
            val value = entry.value
            (value as? Map<String, Any?>)?.let { arg -> loadDungeon(key, arg, file.parentFile.name) }
        }
    } else {
        devLog("Map is null, skipping.")
    }
}

fun loadDungeon(key: String, arg: Map<String, Any?>, folderName: String) {
    val c = ConfigUtil

    // 解析显示信息
    val display = arg["display"] as? Map<String, Any?> ?: emptyMap()
    val displayName = display["name"] as? String ?: key
    val description = (display["description"] as? String)?.lines() ?: emptyList()

    // 解析地图配置
    val mapConfig = arg["map"] as? Map<String, Any?> ?: emptyMap()
    val mapType = mapConfig["type"] as? String ?: "MAP"
    val source = mapConfig["source"] as? String

    // 确定是schematic文件还是世界模板
    val schematicFile = if (mapType.equals("SCHEMATIC", ignoreCase = true)) source else null
    val worldTemplate = if (mapType.equals("MAP", ignoreCase = true)) source else null

    // 解析出生点
    val spawnConfig = (mapConfig["spawn"]) as? Map<String, Any?> ?: emptyMap()
    val spawnLoc = Vector(
        (spawnConfig["x"] as? Number)?.toDouble() ?: 0.0,
        (spawnConfig["y"] as? Number)?.toDouble() ?: 100.0,
        (spawnConfig["z"] as? Number)?.toDouble() ?: 0.0
    )
    val spawnVector = spawnLoc

    // 解析游戏设置
    val gameplay = arg["gameplay"] as? Map<String, Any?> ?: emptyMap()
    val general = gameplay["general"] as? Map<String, Any?> ?: emptyMap()

    val timeLimit = general["TimeLimit"] as? Double ?: 3600.0
    val preparationTime = general["PreparationTime"] as? Double ?: 30.0
    val allowRespawn = general["AllowRespawn"] as? Boolean ?: false
    val keepInventory = general["KeepInventory"] as? Boolean ?: false
    val pvpEnabled = general["PvPEnabled"] as? Boolean ?: false
    val naturalRegeneration = (arg["vanilla-options"] as? Map<String, Any?>)?.get("HealthRegain") as? Boolean ?: true

    // 权限要求
    val requiredPermission = general["RequiredPermission"] as? String

    // 解析代理脚本 (agent)
    val agentSection = arg["agent"] as? Map<String, Any?> ?: emptyMap()
    val agentMap = LinkedHashMap<String, Agent>()
    for ((trigger, script) in agentSection) {
        if (script is String && script.isNotBlank()) {
            agentMap[trigger] = Agent(trigger = trigger, gjs = script)
        }
    }
    val agents = if (agentMap.isNotEmpty()) Agents(agentMap) else null

    // 创建地牢模板
    val template = DungeonTemplate(
        name = key,
        displayName = displayName,
        description = description,
        schematicFile = schematicFile,
        worldTemplate = worldTemplate,
        schematicPasteLocation = spawnVector,
        playerSpawnOffset = spawnVector,
        timeLimit = timeLimit,
        preparationTime = preparationTime,
        allowRespawn = allowRespawn,
        keepInventory = keepInventory,
        pvpEnabled = pvpEnabled,
        naturalRegeneration = naturalRegeneration,
        requiredPermission = requiredPermission,
        agents = agents
    )

    dungeonTemplates[key] = template
    devLog("Loaded dungeon template: $key")
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
    if (!checkRegexMatch(file.name, (config["file-load.dungeon"] ?: ".*").toString())) {
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
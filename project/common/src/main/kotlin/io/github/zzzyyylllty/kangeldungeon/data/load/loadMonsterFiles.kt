package io.github.zzzyyylllty.kangeldungeon.data.load

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.config
import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.logger.infoL
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File

@Deprecated("Use per-dungeon monster/ loading inside dungeon directories instead", ReplaceWith("loadDungeonFiles()"))
fun loadMonsterFiles() {
    infoL("MonsterLoad")
    val monsterFolder = File(getDataFolder(), "monsters")
    if (!monsterFolder.exists()) {
        warningL("MonsterFolderRegen")
        releaseResourceFile("monsters/sample.yml")
    }
    val files = monsterFolder.listFiles()
    if (files == null || files.isEmpty()) {
        warningL("NoMonsterFiles")
        return
    }
    var loadedCount = 0
    for (file in files) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { if (loadMonsterFile(it)) loadedCount++ }
        } else {
            if (loadMonsterFile(file)) loadedCount++
        }
    }
    devLog("Loaded $loadedCount monster config(s)")
}

fun loadMonsterFile(file: File): Boolean {
    if (!checkRegexMatch(file.name, (config["file-load.monster"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return false
    }

    val map = multiExtensionLoader(file) ?: return false

    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val config = parseMonsterConfig(key, value)
        if (config != null) {
            KAngelDungeon.monsterConfigs[key] = config
            devLog("Loaded monster config: $key")
        }
    }
    return true
}

@Suppress("UNCHECKED_CAST")
fun parseMonsterConfig(id: String, data: Map<String, Any?>): MonsterConfig? {
    try {
        val monstersRaw = data["monsters"] as? List<Any?> ?: emptyList()
        val monsters = monstersRaw.mapNotNull { entry ->
            val m = entry as? Map<String, Any?> ?: return@mapNotNull null
            parseMonsterSpawnEntry(m)
        }

        val agentRaw = data["agent"] as? Map<String, Any?>
        val agent = if (agentRaw != null) {
            MonsterAgent(
                onSpawn = agentRaw["onSpawn"] as? String,
                onAllKilled = agentRaw["onAllKilled"] as? String,
                onRespawn = agentRaw["onRespawn"] as? String,
                onEachKill = agentRaw["onEachKill"] as? String
            )
        } else null

        val active = data["active"] as? Boolean ?: true
        val respawnCooldown = (data["respawnCooldown"] as? Number)?.toLong() ?: 0
        val activationRangeMin = (data["activationRangeMin"] as? Number)?.toDouble() ?: 0.0
        val activationRangeMax = (data["activationRangeMax"] as? Number)?.toDouble() ?: -1.0
        val spawnCondition = data["spawnCondition"] as? String
        val spawnDelay = (data["spawnDelay"] as? Number)?.toLong() ?: 0
        val maxRespawns = (data["maxRespawns"] as? Number)?.toInt() ?: -1
        val respawnCondition = data["respawnCondition"] as? String
        val leashRange = (data["leashRange"] as? Number)?.toDouble() ?: 0.0
        val healthMultiplier = (data["healthMultiplier"] as? Number)?.toDouble() ?: 1.0
        val damageMultiplier = (data["damageMultiplier"] as? Number)?.toDouble() ?: 1.0
        val spawnInterval = (data["spawnInterval"] as? Number)?.toLong() ?: 0
        val priority = (data["priority"] as? Number)?.toInt() ?: 0

        return MonsterConfig(
            id = id,
            monsters = monsters,
            agent = agent,
            active = active,
            respawnCooldown = respawnCooldown,
            activationRangeMin = activationRangeMin,
            activationRangeMax = activationRangeMax,
            spawnCondition = spawnCondition,
            spawnDelay = spawnDelay,
            maxRespawns = maxRespawns,
            respawnCondition = respawnCondition,
            leashRange = leashRange,
            healthMultiplier = healthMultiplier,
            damageMultiplier = damageMultiplier,
            spawnInterval = spawnInterval,
            priority = priority
        )
    } catch (e: Exception) {
        devLog("Failed to parse monster config '$id': ${e.message}")
        return null
    }
}

private fun parseMonsterSpawnEntry(data: Map<String, Any?>): MonsterSpawnEntry? {
    val mob = data["mob"] as? String ?: return null
    val locationStr = data["location"] as? String ?: return null
    val parts = locationStr.split(",")
    if (parts.size != 3) return null

    val location = MonsterLocation(
        x = parts[0].trim().toDoubleOrNull() ?: 0.0,
        y = parts[1].trim().toDoubleOrNull() ?: 64.0,
        z = parts[2].trim().toDoubleOrNull() ?: 0.0
    )

    return MonsterSpawnEntry(
        mob = mob,
        location = location,
        amount = (data["amount"] as? Number)?.toInt() ?: 1,
        scattered = (data["scattered"] as? Number)?.toInt() ?: 0,
        level = (data["level"] as? Number)?.toInt() ?: 0
    )
}

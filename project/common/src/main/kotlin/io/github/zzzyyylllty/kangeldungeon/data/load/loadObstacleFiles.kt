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

@Deprecated("Use per-dungeon obstacle/ loading inside dungeon directories instead", ReplaceWith("loadDungeonFiles()"))
fun loadObstacleFiles() {
    infoL("ObstacleLoad")
    val obstacleFolder = File(getDataFolder(), "obstacles")
    if (!obstacleFolder.exists()) {
        warningL("ObstacleFolderRegen")
        releaseResourceFile("obstacles/ironbars.yml")
    }
    val files = obstacleFolder.listFiles()
    if (files == null || files.isEmpty()) {
        warningL("NoObstacleFiles")
        return
    }
    var loadedCount = 0
    for (file in files) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { if (loadObstacleFile(it)) loadedCount++ }
        } else {
            if (loadObstacleFile(file)) loadedCount++
        }
    }
    devLog("Loaded $loadedCount obstacle config(s)")
}

fun loadObstacleFile(file: File): Boolean {
    if (!checkRegexMatch(file.name, (config["file-load.obstacle"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return false
    }

    val map = multiExtensionLoader(file) ?: return false

    for (entry in map.entries) {
        val key = entry.key
        val value = entry.value as? Map<String, Any?> ?: continue
        val config = parseObstacleConfig(key, value)
        if (config != null) {
            KAngelDungeon.obstacleConfigs[key] = config
            devLog("Loaded obstacle config: $key")
        }
    }
    return true
}

@Suppress("UNCHECKED_CAST")
fun parseObstacleConfig(id: String, data: Map<String, Any?>): ObstacleConfig? {
    try {
        val agentRaw = data["agent"] as? Map<String, Any?>
        val agent = if (agentRaw != null) {
            ObstacleAgent(
                onPrepare = agentRaw["onPrepare"] as? String,
                onStart = agentRaw["onStart"] as? String
            )
        } else null

        val openDelay = (data["openDelaySeconds"] as? Number)?.toDouble() ?: 3.0
        val activeDuration = (data["activeDurationSeconds"] as? Number)?.toDouble() ?: 10.0

        val openingAnim = parseAnimation(data["openingAnimation"] as? Map<String, Any?>)
        val closingAnim = parseAnimation(data["closingAnimation"] as? Map<String, Any?>)

        val obstacles = mutableMapOf<String, GateObstacle>()
        val obstaclesRaw = data["obstacles"] as? Map<String, Any?> ?: emptyMap()
        for ((gateId, gateData) in obstaclesRaw) {
            val gate = gateData as? Map<String, Any?> ?: continue
            obstacles[gateId] = parseGateObstacle(gateId, gate)
        }

        return ObstacleConfig(
            id = id,
            agent = agent,
            openDelaySeconds = openDelay,
            activeDurationSeconds = activeDuration,
            openingAnimation = openingAnim,
            closingAnimation = closingAnim,
            obstacles = obstacles
        )
    } catch (e: Exception) {
        devLog("Failed to parse obstacle config '$id': ${e.message}")
        return null
    }
}

private fun parseAnimation(data: Map<String, Any?>?): ObstacleAnimation? {
    if (data == null) return null
    return ObstacleAnimation(
        enabled = (data["enabled"] as? Boolean) ?: true,
        particle = data["particle"] as? String,
        particleCount = (data["particleCount"] as? Number)?.toInt() ?: 10,
        sound = data["sound"] as? String,
        volume = (data["volume"] as? Number)?.toDouble() ?: 1.0,
        pitch = (data["pitch"] as? Number)?.toDouble() ?: 1.0,
        durationTicks = (data["durationTicks"] as? Number)?.toInt() ?: 20,
        intervalTicks = (data["intervalTicks"] as? Number)?.toInt() ?: 2
    )
}

@Suppress("UNCHECKED_CAST")
private fun parseGateObstacle(id: String, data: Map<String, Any?>): GateObstacle {
    val mode = (data["mode"] as? String) ?: "RESTORE_BLOCKS"

    val blocksRaw = data["blocks"] as? Map<String, Any?>
    val blocks = blocksRaw?.mapValues { (_, v) ->
        val b = v as? Map<String, Any?> ?: return@mapValues null
        BlockPos(
            x = (b["x"] as? Number)?.toInt() ?: 0,
            y = (b["y"] as? Number)?.toInt() ?: 0,
            z = (b["z"] as? Number)?.toInt() ?: 0
        )
    }?.filterValues { it != null }?.mapValues { it.value!! }

    val cuboidRaw = data["cuboid"] as? Map<String, Any?>
    val cuboid = cuboidRaw?.let { parseCuboid(it) }

    val seqRaw = data["sequentialConfig"] as? Map<String, Any?>
    val sequentialConfig = if (seqRaw != null) {
        SequentialConfig(
            enabled = (seqRaw["enabled"] as? Boolean) ?: true,
            openDirection = (seqRaw["openDirection"] as? String) ?: "LEFT_TO_RIGHT",
            reverseOnClose = (seqRaw["reverseOnClose"] as? Boolean) ?: true,
            blocksPerStep = (seqRaw["blocksPerStep"] as? Number)?.toInt() ?: 1,
            stepDelayTicks = (seqRaw["stepDelayTicks"] as? Number)?.toInt() ?: 2,
            openEffect = parseEffect(seqRaw["openEffect"] as? Map<String, Any?>),
            closeEffect = parseEffect(seqRaw["closeEffect"] as? Map<String, Any?>)
        )
    } else null

    return GateObstacle(
        id = id,
        mode = mode,
        blocks = blocks,
        cuboid = cuboid,
        sequentialConfig = sequentialConfig
    )
}

private fun parseCuboid(data: Map<String, Any?>): CuboidDef? {
    val pos1Raw = data["pos1"] as? Map<String, Any?> ?: return null
    val pos2Raw = data["pos2"] as? Map<String, Any?> ?: return null
    return CuboidDef(
        pos1 = BlockPos(
            x = (pos1Raw["x"] as? Number)?.toInt() ?: 0,
            y = (pos1Raw["y"] as? Number)?.toInt() ?: 0,
            z = (pos1Raw["z"] as? Number)?.toInt() ?: 0
        ),
        pos2 = BlockPos(
            x = (pos2Raw["x"] as? Number)?.toInt() ?: 0,
            y = (pos2Raw["y"] as? Number)?.toInt() ?: 0,
            z = (pos2Raw["z"] as? Number)?.toInt() ?: 0
        )
    )
}

private fun parseEffect(data: Map<String, Any?>?): EffectConfig? {
    if (data == null) return null
    return EffectConfig(
        enabled = (data["enabled"] as? Boolean) ?: true,
        particle = data["particle"] as? String,
        count = (data["count"] as? Number)?.toInt() ?: 8
    )
}

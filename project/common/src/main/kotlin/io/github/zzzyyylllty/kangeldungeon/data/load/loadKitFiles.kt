package io.github.zzzyyylllty.kangeldungeon.data.load

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.KitConfig
import io.github.zzzyyylllty.kangeldungeon.data.KitReward
import io.github.zzzyyylllty.kangeldungeon.data.RewardType
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import io.github.zzzyyylllty.kangeldungeon.logger.severeL
import io.github.zzzyyylllty.kangeldungeon.data.load.ReloadDiagnostics
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 加载全局Kit配置文件（位于 kits/ 目录下，所有地牢通用）
 */
fun loadGlobalKitFiles() {
    val kitFolder = File(taboolib.common.platform.function.getDataFolder(), "kits")
    if (!kitFolder.exists()) {
        kitFolder.mkdirs()
        taboolib.common.platform.function.releaseResourceFile("kits/sample.yml", false)
    }
    val files = kitFolder.listFiles() ?: return
    var loadedCount = 0
    for (file in files) {
        if (file.isDirectory) continue
        if (loadKitFile(file)) loadedCount++
    }
    devLog("Loaded $loadedCount global kit config(s)")
}

private fun loadKitFile(file: File): Boolean {
    if (!checkRegexMatch(file.name, (KAngelDungeon.config["file-load.kit"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return false
    }
    val map = multiExtensionLoader(file) ?: return false
    for (entry in map.entries) {
        val kitName = entry.key
        val data = entry.value as? Map<String, Any?> ?: continue
        val config = parseKitConfig(kitName, data)
        if (config != null) {
            KAngelDungeon.kitConfigs[kitName] = config
            devLog("Loaded global kit: $kitName (${config.rewards.size} rewards)")
        } else {
            ReloadDiagnostics.warn("ReloadWarnKitParse", "global", kitName, "unknown error")
        }
    }
    return true
}

/**
 * 加载地牢Kit配置文件（位于 dungeon/<dungeon-id>/kit/ 目录下）
 *
 * YAML 格式:
 * ```yaml
 * reward_chest:
 *   display:
 *     name: "<gold>通关奖励"
 *     material: CHEST
 *   rewards:
 *     - type: item
 *       source: minecraft
 *       item: diamond
 *       amount: 1
 *       weight: 50
 *     - type: command
 *       command: "give %player% minecraft:diamond 1"
 *       weight: 30
 *     - type: script
 *       script: |-
 *         player.sendMessage("恭喜通关！");
 *       weight: 20
 *   min_rewards: 1
 *   max_rewards: 2
 * ```
 */
fun loadDungeonKitFile(dungeonName: String, file: File) {
    if (!checkRegexMatch(file.name, (KAngelDungeon.config["file-load.kit"] ?: ".*").toString())) {
        devLog("${file.name} not match regex, skipping...")
        return
    }
    val map = multiExtensionLoader(file) ?: return
    val dungeonMap = KAngelDungeon.dungeonKitConfigs.getOrPut(dungeonName) { ConcurrentHashMap() }

    for (entry in map.entries) {
        val kitName = entry.key
        val data = entry.value as? Map<String, Any?> ?: continue
        val config = parseKitConfig(kitName, data)
        if (config != null) {
            dungeonMap[kitName] = config
            devLog("Loaded dungeon kit: $dungeonName/$kitName (${config.rewards.size} rewards)")
        } else {
            ReloadDiagnostics.warn("ReloadWarnKitParse", dungeonName, kitName, "unknown error")
        }
    }
}

/**
 * 解析单个Kit配置
 */
@Suppress("UNCHECKED_CAST")
fun parseKitConfig(name: String, data: Map<String, Any?>): KitConfig? {
    return try {
        // 解析 display
        val display = data["display"] as? Map<String, Any?> ?: emptyMap()
        val displayName = display["name"] as? String
        val icon = display["material"] as? String

        // 解析 rewards
        val rewardsRaw = data["rewards"]
        val rewardsList = when (rewardsRaw) {
            is List<*> -> rewardsRaw
            is Map<*, *> -> listOf(rewardsRaw)
            else -> emptyList<Any?>()
        }

        val rewards = rewardsList.mapNotNull { raw ->
            val rewardMap = raw as? Map<String, Any?> ?: return@mapNotNull null
            parseKitReward(rewardMap)
        }

        if (rewards.isEmpty()) {
            devLog("Kit '$name' has no valid rewards, skipping")
            return null
        }

        // 解析 min/max
        val minRewards = (data["min_rewards"] as? Number)?.toInt() ?: 1
        val maxRewards = (data["max_rewards"] as? Number)?.toInt() ?: 1

        // Parse cooldown (seconds)
        val cooldown = (data["cooldown"] as? Number)?.toInt() ?: 0

        // Parse conditions (Kether expressions)
        val conditions: List<String>? = when (val c = data["conditions"]) {
            is List<*> -> c.mapNotNull { it?.toString() }.ifEmpty { null }
            is String -> listOf(c)
            else -> null
        }

        // Parse broadcast_message
        val broadcastMessage = data["broadcast_message"] as? String

        // Parse messages section
        val messages: Map<String, String>? = (data["messages"] as? Map<*, *>)
            ?.mapKeys { it.key.toString() }
            ?.mapValues { it.value.toString() }
            ?.ifEmpty { null }

        KitConfig(
            name = name,
            displayName = displayName,
            icon = icon,
            rewards = rewards,
            minRewards = minRewards.coerceAtLeast(1),
            maxRewards = maxRewards.coerceAtLeast(minRewards),
            cooldown = cooldown,
            conditions = conditions,
            broadcastMessage = broadcastMessage,
            messages = messages
        )
    } catch (e: Exception) {
        severeL("KitLoadFailed", name, e.message ?: "Unknown")
        ReloadDiagnostics.warn("ReloadWarnKitParse", name, e.message ?: "Unknown")
        null
    }
}

/**
 * 解析单条奖励
 */
@Suppress("UNCHECKED_CAST")
fun parseKitReward(data: Map<String, Any?>): KitReward? {
    val typeStr = (data["type"] as? String)?.uppercase() ?: return null
    val type = try {
        RewardType.valueOf(typeStr)
    } catch (e: IllegalArgumentException) {
        devLog("Unknown reward type: $typeStr")
        return null
    }

    val weight = (data["weight"] as? Number)?.toInt() ?: 1
    val chance = (data["chance"] as? Number)?.toInt()

    return KitReward(
        type = type,
        weight = weight,
        chance = chance,
        // ITEM
        source = data["source"] as? String,
        item = data["item"] as? String,
        amount = (data["amount"] as? Number)?.toInt() ?: 1,
        parameters = data["parameters"] as? Map<String, Any?>,
        components = data["components"] as? Map<String, Any?>,
        // COMMAND
        command = data["command"] as? String,
        // SCRIPT
        script = data["script"] as? String,
        // AGENT
        agentTrigger = data["agent-trigger"] as? String,
        agentScript = data["agent-script"] as? String,
        // Per-reward message
        message = data["message"] as? String
    )
}

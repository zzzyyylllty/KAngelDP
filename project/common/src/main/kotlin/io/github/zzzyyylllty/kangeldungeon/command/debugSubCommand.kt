package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.blockRegenMap
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.util.devMode
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.monster.MonsterManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.bool
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.asLangText

private fun CommandSender.stateLabel(state: DungeonState): String = when (state) {
    DungeonState.PREPARING -> asLangText("DungeonStatePreparing")
    DungeonState.ACTIVE -> asLangText("DungeonStateActive")
    DungeonState.COMPLETED -> asLangText("DungeonStateCompleted")
    DungeonState.FAILED -> asLangText("DungeonStateFailed")
}

@CommandHeader(
    name = "kangeldungeondebug",
    aliases = ["dgd", "dungeondebug"],
    permission = "kangeldungeon.command.debug",
    description = "DEBUG Command of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = true,
)
object DebugCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    @CommandBody
    val getBlockRegenMap = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val map = blockRegenMap
            if (map.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("DebugNoBlockRegen"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("DebugBlockRegenHeader", map.size.toString()))
            for ((key, uuids) in map) {
                sender.sendStringAsComponent(sender.asLangText("DebugBlockRegenEntry", key, uuids.size.toString()))
            }
        }
    }

    /**
     * /kangeldungeon debug getTemplates
     * 列出所有已加载的地牢模板详情
     */
    @CommandBody
    val getTemplates = subCommand {
        execute<CommandSender> { sender, context, argument ->
                val templates = KAngelDungeon.dungeonTemplates
                if (templates.isEmpty()) {
                    sender.sendStringAsComponent(sender.asLangText("DebugNoTemplates"))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("DebugTemplateHeader", templates.size.toString()))
                for ((name, template) in templates) {
                    sender.sendStringAsComponent(sender.asLangText("DebugTemplateName", template.displayName, name))
                    sender.sendStringAsComponent(sender.asLangText("DebugTemplatePlayers", template.gameplayGeneral.minPlayers.toString(), template.gameplayGeneral.maxPlayers.toString()))
                    sender.sendStringAsComponent(sender.asLangText("DebugTemplateTimeLimit", template.timeLimit?.toInt()?.toString() ?: "∞"))
                    sender.sendStringAsComponent(sender.asLangText("DebugTemplatePrepTime", template.preparationTime?.toInt()?.toString() ?: "0"))
                    sender.sendStringAsComponent(sender.asLangText("DebugTemplateRespawn", template.allowRespawn.toString()))
                    sender.sendStringAsComponent(sender.asLangText("DebugTemplatePVP", template.pvpEnabled.toString()))
                    sender.sendStringAsComponent(sender.asLangText("DebugTemplateWorldTemplate", template.worldTemplate ?: sender.asLangText("ValueUnknown")))
                }
        }
    }

    /**
     * /kangeldungeon debug getInstances
     * 列出所有活跃地牢实例
     */
    @CommandBody
    val getInstances = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val instances = KAngelDungeon.dungeonInstances
            if (instances.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("DebugNoInstances"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("DebugInstanceHeader", instances.size.toString()))
            for ((uuid, instance) in instances) {
                val stateLabel = sender.stateLabel(instance.state)
                val elapsed = instance.getElapsedTime().toInt()
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceEntry",
                    instance.templateName, stateLabel,
                    instance.getAlivePlayerCount().toString(),
                    instance.getPlayerCount().toString(),
                    elapsed.toString(), uuid.toString()))
            }
        }
    }

    /**
     * /kangeldungeon debug getMonsterConfigs [dungeonName]
     * 显示地牢的怪物配置
     */
    @CommandBody
    val getMonsterConfigs = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val configs = KAngelDungeon.dungeonMonsterConfigs
                if (dungeonName == "*") {
                    if (configs.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoMonsterConfigs"))
                        return@execute
                    }
                    for ((dungeon, map) in configs) {
                        sender.sendStringAsComponent(sender.asLangText("DebugMonsterHeader", dungeon, map.size.toString()))
                        for ((id, config) in map) {
                            sender.sendStringAsComponent(sender.asLangText("DebugMonsterEntry", id, config.monsters.size.toString()))
                        }
                    }
                } else {
                    val map = configs[dungeonName]
                    if (map.isNullOrEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoMonsterForDungeon", dungeonName))
                        return@execute
                    }
                    sender.sendStringAsComponent(sender.asLangText("DebugMonsterHeader", dungeonName, map.size.toString()))
                    for ((id, config) in map) {
                        sender.sendStringAsComponent(" <gold>$id</gold>")
                        config.monsters.forEachIndexed { i, entry ->
                            val prefix = if (i == config.monsters.lastIndex) "└" else "├"
                            sender.sendStringAsComponent(sender.asLangText("DebugMonsterDetail", prefix, entry.mob, entry.amount.toString(), entry.location.x.toString(), entry.location.y.toString(), entry.location.z.toString()))
                        }
                        if (config.monsters.isEmpty()) {
                            sender.sendStringAsComponent(sender.asLangText("DebugMonsterNoEntries"))
                        }
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getObstacleConfigs [dungeonName]
     * 显示地牢的障碍物配置
     */
    @CommandBody
    val getObstacleConfigs = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val dungeonConfigs = KAngelDungeon.dungeonObstacleConfigs
                val globalConfigs = KAngelDungeon.obstacleConfigs

                if (dungeonName == "*") {
                    if (dungeonConfigs.isEmpty() && globalConfigs.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoObstacleConfigs"))
                        return@execute
                    }
                    if (globalConfigs.isNotEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugObstacleGlobalHeader", globalConfigs.size.toString()))
                        for ((id, config) in globalConfigs) {
                            sender.sendStringAsComponent(sender.asLangText("DebugObstacleEntry", id, config.activeDurationSeconds.toString(), config.obstacles.size.toString()))
                        }
                    }
                    for ((dungeon, map) in dungeonConfigs) {
                        sender.sendStringAsComponent(sender.asLangText("DebugObstacleHeader", dungeon, map.size.toString()))
                        for ((id, config) in map) {
                            sender.sendStringAsComponent(sender.asLangText("DebugObstacleEntry", id, config.activeDurationSeconds.toString(), config.obstacles.size.toString()))
                        }
                    }
                } else {
                    val perDungeon = dungeonConfigs[dungeonName] ?: emptyMap()
                    val merged = perDungeon + globalConfigs
                    if (merged.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoObstacleForDungeon", dungeonName))
                        return@execute
                    }
                    sender.sendStringAsComponent(sender.asLangText("DebugObstacleHeader", dungeonName, merged.size.toString()))
                    for ((id, config) in merged) {
                        val source = if (config.id in perDungeon) sender.asLangText("DebugSourceLocal") else sender.asLangText("DebugSourceGlobal")
                        sender.sendStringAsComponent(sender.asLangText("DebugObstacleDetail", id, source))
                        sender.sendStringAsComponent(sender.asLangText("DebugObstacleOpenDelay", config.openDelaySeconds.toString()))
                        sender.sendStringAsComponent(sender.asLangText("DebugObstacleActiveDuration", config.activeDurationSeconds.toString()))
                        sender.sendStringAsComponent(sender.asLangText("DebugObstacleBlockCount", config.obstacles.size.toString()))
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getInteractConfigs [dungeonName]
     * 显示地牢的交互配置
     */
    @CommandBody
    val getInteractConfigs = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val configs = KAngelDungeon.dungeonInteractConfigs
                if (dungeonName == "*") {
                    if (configs.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoInteractConfigs"))
                        return@execute
                    }
                    for ((dungeon, map) in configs) {
                        sender.sendStringAsComponent(sender.asLangText("DebugInteractHeader", dungeon, map.size.toString()))
                        for ((id, config) in map) {
                            val hasScript = if (config.agent != null) sender.asLangText("DebugYes") else sender.asLangText("DebugNo")
                            sender.sendStringAsComponent(sender.asLangText("DebugInteractEntry", id, config.pos.toString(), hasScript))
                        }
                    }
                } else {
                    val map = configs[dungeonName]
                    if (map.isNullOrEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoInteractForDungeon", dungeonName))
                        return@execute
                    }
                    sender.sendStringAsComponent(sender.asLangText("DebugInteractHeader", dungeonName, map.size.toString()))
                    for ((id, config) in map) {
                        val hasScript = if (config.agent != null) sender.asLangText("DebugYes") else sender.asLangText("DebugNo")
                        sender.sendStringAsComponent(sender.asLangText("DebugInteractEntry", id, config.pos.toString(), hasScript))
                        config.agent?.let { agent ->
                            if (agent.onActive != null) sender.sendStringAsComponent(sender.asLangText("DebugInteractActive"))
                            if (agent.onPost != null) sender.sendStringAsComponent(sender.asLangText("DebugInteractPost"))
                        } ?: sender.sendStringAsComponent(sender.asLangText("DebugInteractNoScript"))
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getPlans [dungeonName]
     * 显示地牢的计划（定时任务）配置
     */
    @CommandBody
    val getPlans = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val configs = KAngelDungeon.dungeonPlanConfigs
                if (dungeonName == "*") {
                    if (configs.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoPlanConfigs"))
                        return@execute
                    }
                    for ((dungeon, map) in configs) {
                        sender.sendStringAsComponent(sender.asLangText("DebugPlanHeader", dungeon, map.size.toString()))
                        for ((id, plan) in map) {
                            sender.sendStringAsComponent(sender.asLangText("DebugPlanEntry", id, plan.trigger, (plan.delay ?: 0).toString(), plan.period?.toString() ?: sender.asLangText("DebugPlanOneShot"), plan.async.toString()))
                        }
                    }
                } else {
                    val map = configs[dungeonName]
                    if (map.isNullOrEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoPlanForDungeon", dungeonName))
                        return@execute
                    }
                    sender.sendStringAsComponent(sender.asLangText("DebugPlanHeader", dungeonName, map.size.toString()))
                    for ((id, plan) in map) {
                        sender.sendStringAsComponent(sender.asLangText("DebugPlanDetail", id, plan.trigger))
                        sender.sendStringAsComponent(sender.asLangText("DebugPlanDelay", (plan.delay ?: 0).toString()))
                        val intervalPrefix = if (plan.period != null) "├" else "└"
                        sender.sendStringAsComponent(sender.asLangText("DebugPlanInterval", intervalPrefix, plan.period?.toString() ?: sender.asLangText("DebugPlanOneShot")))
                        if (plan.period != null) {
                            sender.sendStringAsComponent(sender.asLangText("DebugPlanAsync", plan.async.toString()))
                        }
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getRegionConfigs [dungeonName]
     * 显示地牢的区域配置
     */
    @CommandBody
    val getRegionConfigs = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val configs = KAngelDungeon.dungeonRegionConfigs
                if (dungeonName == "*") {
                    if (configs.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoRegionConfigs"))
                        return@execute
                    }
                    for ((dungeon, map) in configs) {
                        sender.sendStringAsComponent(sender.asLangText("DebugRegionHeader", dungeon, map.size.toString()))
                        for ((id, config) in map) {
                            val hasEnter = if (config.agent?.onEnter != null) sender.asLangText("DebugYes") else sender.asLangText("DebugNo")
                            val hasLeave = if (config.agent?.onLeave != null) sender.asLangText("DebugYes") else sender.asLangText("DebugNo")
                            sender.sendStringAsComponent(sender.asLangText("DebugRegionEntry", id,
                                config.from.x.toString(), config.from.y.toString(), config.from.z.toString(),
                                config.to.x.toString(), config.to.y.toString(), config.to.z.toString(),
                                hasEnter, hasLeave))
                        }
                    }
                } else {
                    val map = configs[dungeonName]
                    if (map.isNullOrEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoRegionForDungeon", dungeonName))
                        return@execute
                    }
                    sender.sendStringAsComponent(sender.asLangText("DebugRegionHeader", dungeonName, map.size.toString()))
                    for ((id, config) in map) {
                        sender.sendStringAsComponent(sender.asLangText("DebugRegionDetail", id))
                        sender.sendStringAsComponent(sender.asLangText("DebugRegionRange",
                            config.from.x.toString(), config.from.y.toString(), config.from.z.toString(),
                            config.to.x.toString(), config.to.y.toString(), config.to.z.toString()))
                        config.agent?.let { agent ->
                            if (agent.onEnter != null) {
                                val preview = agent.onEnter.take(60).replace("\n", "\\n")
                                sender.sendStringAsComponent(sender.asLangText("DebugRegionOnEnter", preview))
                            }
                            if (agent.onLeave != null) {
                                val preview = agent.onLeave.take(60).replace("\n", "\\n")
                                sender.sendStringAsComponent(sender.asLangText("DebugRegionOnLeave", preview))
                            }
                            if (agent.onEnter == null && agent.onLeave == null) {
                                sender.sendStringAsComponent(sender.asLangText("DebugRegionNoAgent"))
                            }
                        } ?: sender.sendStringAsComponent(sender.asLangText("DebugRegionNoAgent"))
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getScripts [dungeonName]
     * 显示地牢的脚本配置
     */
    @CommandBody
    val getScripts = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val scripts = KAngelDungeon.dungeonScripts
                if (dungeonName == "*") {
                    if (scripts.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoScripts"))
                        return@execute
                    }
                    for ((dungeon, map) in scripts) {
                        sender.sendStringAsComponent(sender.asLangText("DebugScriptHeader", dungeon, map.size.toString()))
                        for ((id, script) in map) {
                            val hasOnRun = if (script.onRun != null) sender.asLangText("DebugYes") else sender.asLangText("DebugNo")
                            val hasOnPost = if (script.onPost != null) sender.asLangText("DebugYes") else sender.asLangText("DebugNo")
                            sender.sendStringAsComponent(sender.asLangText("DebugScriptEntry", id, hasOnRun, hasOnPost))
                        }
                    }
                } else {
                    val map = scripts[dungeonName]
                    if (map.isNullOrEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoScriptForDungeon", dungeonName))
                        return@execute
                    }
                    sender.sendStringAsComponent(sender.asLangText("DebugScriptHeader", dungeonName, map.size.toString()))
                    for ((id, script) in map) {
                        sender.sendStringAsComponent(" <gold>$id</gold>")
                        if (script.onRun != null) {
                            val preview = script.onRun.take(80).replace("\n", "\\n")
                            sender.sendStringAsComponent(sender.asLangText("DebugScriptOnRun", preview))
                        }
                        if (script.onPost != null) {
                            val preview = script.onPost.take(80).replace("\n", "\\n")
                            sender.sendStringAsComponent(sender.asLangText("DebugScriptOnPost", preview))
                        }
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getDungeonOptions <dungeon>
     * 显示地牢的选项配置（option.yml 中的游戏设置等）
     */
    @CommandBody
    val getDungeonOptions = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val template = KAngelDungeon.dungeonTemplates[dungeonName]
                if (template == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugOptionTemplateNotFound", dungeonName))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("DebugOptionHeader", dungeonName))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionDisplayName", template.displayName))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionGameSettings"))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionMinPlayers", template.gameplayGeneral.minPlayers.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionMaxPlayers", template.gameplayGeneral.maxPlayers.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionAllowParty", template.gameplayGeneral.allowParty.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionLeaveOnDeath", template.gameplayGeneral.leaveOnDeath.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionAdventureMode", template.gameplayGeneral.adventureMode.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionKeepInventory", template.gameplayGeneral.keepInventory.enabled.toString(), template.gameplayGeneral.keepInventory.requiredLives.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionTimer", template.timerConfig.mode.name, template.timerConfig.start.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionCommand", template.commandConfig.mode.name, template.commandConfig.list.size.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionVanilla"))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionHungry", template.vanillaOptions.hungry.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionDurability", template.vanillaOptions.durability.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionItemsDrop", template.vanillaOptions.itemsDrop.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionItemsPickup", template.vanillaOptions.itemsPickup.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionHealthRegain", template.vanillaOptions.healthRegain.food.toString(), template.vanillaOptions.healthRegain.saturation.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionTimeInfo", template.timeLimit?.toInt()?.toString() ?: "∞", template.preparationTime?.toInt()?.toString() ?: "0"))
                sender.sendStringAsComponent(sender.asLangText("DebugOptionPVPRespawn", template.pvpEnabled.toString(), template.allowRespawn.toString()))
            }
        }
    }

    /**
     * /kangeldungeon debug getDevMode
     * 查看或切换开发模式
     */
    @CommandBody
    val getDevMode = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val current = KAngelDungeon.devMode
            sender.sendStringAsComponent(sender.asLangText("DebugDevModeStatus",
                if (current) sender.asLangText("StatusEnabled") else sender.asLangText("StatusDisabled")))
        }
    }

    /**
     * /kangeldungeon debug setDevMode <true/false>
     * 切换开发模式
     */
    @CommandBody
    val setDevMode = subCommand {
        bool("mode") {
            execute<CommandSender> { sender, context, argument ->
                val mode = context.bool("mode")
                devMode(mode)
                sender.sendStringAsComponent(sender.asLangText("DebugDevModeSet",
                    if (mode) sender.asLangText("StatusOn") else sender.asLangText("StatusOff")))
            }
        }
    }

    /**
     * /kangeldungeon debug getMemoryInfo
     * 显示插件各缓存映射的大小
     */
    @CommandBody
    val getMemoryInfo = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryHeader"))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelTemplates"), KAngelDungeon.dungeonTemplates.size.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelInstances"), KAngelDungeon.dungeonInstances.size.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelScripts"), KAngelDungeon.dungeonScripts.values.sumOf { it.size }.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelObstaclesDungeon"), KAngelDungeon.dungeonObstacleConfigs.values.sumOf { it.size }.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelObstaclesGlobal"), KAngelDungeon.obstacleConfigs.size.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelMonstersDungeon"), KAngelDungeon.dungeonMonsterConfigs.values.sumOf { it.size }.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelMonstersGlobal"), KAngelDungeon.monsterConfigs.size.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelInteracts"), KAngelDungeon.dungeonInteractConfigs.values.sumOf { it.size }.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelPlans"), KAngelDungeon.dungeonPlanConfigs.values.sumOf { it.size }.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelRegions"), KAngelDungeon.dungeonRegionConfigs.values.sumOf { it.size }.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelBlockRegen"), blockRegenMap.size.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelGjsCache"), KAngelDungeon.gjsScriptCache.size.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelKetherCache"), KAngelDungeon.ketherScriptCache.size.toString()))
            sender.sendStringAsComponent(sender.asLangText("DebugMemoryEntry", sender.asLangText("DebugMemoryLabelJsCache"), KAngelDungeon.jsScriptCache.size.toString()))
        }
    }

    /**
     * /kangeldungeon debug getConfig [key]
     * 获取配置值
     */
    @CommandBody
    val getConfig = subCommand {
        dynamic("key") {
            execute<CommandSender> { sender, context, argument ->
                val key = context["key"]
                val value = KAngelDungeon.config.get(key)
                sender.sendStringAsComponent(sender.asLangText("DebugConfigValue", key, value.toString()))
            }
        }
    }

    // ==================== 新增调试命令 ====================

    /**
     * /kangeldungeon debug checkWorld <uuid>
     * 检查地牢世界的加载状态和信息
     */
    @CommandBody
    val checkWorld = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInstanceNotFound", uuidStr))
                    return@execute
                }
                val world = instance.world
                if (world == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugNoWorldForDungeon", instance.worldName))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("DebugWorldCheckHeader", instance.worldName))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldName", world.name))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldEnv", world.environment.name))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldDifficulty", world.difficulty.name))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldPlayers", world.players.size.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldEntities", world.entityCount.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldTime", world.time.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldPVP", world.pvp.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugWorldAutoSave", world.isAutoSave.toString()))
            }
        }
    }

    /**
     * /kangeldungeon debug evalInstance <uuid> <js>
     * 在指定的地牢实例上下文中执行 JavaScript 脚本
     * 脚本中可使用变量: instance, template, server, sender
     */
    @CommandBody
    val evalInstance = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            dynamic("script") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val uuidStr = context["uuid"]
                        val script = context["script"]
                        val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                        if (uuid == null) {
                            sender.sendStringAsComponent(sender.asLangText("DebugInvalidUUID", uuidStr))
                            return@submitAsync
                        }
                        val instance = KAngelDungeon.dungeonInstances[uuid]
                        if (instance == null) {
                            sender.sendStringAsComponent(sender.asLangText("DebugInstanceNotFound", uuidStr))
                            return@submitAsync
                        }
                        val template = instance.getTemplate()
                        val vars = defaultData + mapOf(
                            "instance" to instance,
                            "template" to template,
                            "sender" to sender
                        )
                        sender.sendStringAsComponent(sender.asLangText("DebugEvalScript", script))
                        try {
                            val ret = GraalJsUtil.directEval(script, vars)
                            sender.sendStringAsComponent(sender.asLangText("DebugEvalSuccess", ret.toString()))
                        } catch (e: Exception) {
                            sender.sendStringAsComponent(sender.asLangText("DebugEvalFailed", e.message ?: "Unknown"))
                        }
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug runScript <uuid> <scriptName>
     * 手动运行指定地牢实例中的一个脚本配置
     */
    @CommandBody
    val runScript = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            dynamic("scriptName") {
                suggestion<CommandSender> { _, _ ->
                    KAngelDungeon.dungeonScripts.values.flatMap { it.keys }.distinct().toList()
                }
                execute<CommandSender> { sender, context, argument ->
                    val uuidStr = context["uuid"]
                    val scriptName = context["scriptName"]
                    val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                    if (uuid == null) {
                        sender.sendStringAsComponent(sender.asLangText("DebugInvalidUUID", uuidStr))
                        return@execute
                    }
                    val instance = KAngelDungeon.dungeonInstances[uuid]
                    if (instance == null) {
                        sender.sendStringAsComponent(sender.asLangText("DebugInstanceNotFound", uuidStr))
                        return@execute
                    }
                    val result = instance.runScript(scriptName)
                    if (result) {
                        sender.sendStringAsComponent(sender.asLangText("DebugScriptExecuted", scriptName, instance.templateName))
                    } else {
                        sender.sendStringAsComponent(sender.asLangText("DebugScriptNotFound", scriptName))
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug spawnTestMobs <uuid> <configId>
     * 在指定地牢实例中生成测试怪物
     */
    @CommandBody
    val spawnTestMobs = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances
                    .filter { it.value.state == DungeonState.ACTIVE }
                    .map { it.key.toString() }.toList()
            }
            dynamic("configId") {
                suggestion<CommandSender> { _, _ ->
                    val allConfigs = KAngelDungeon.dungeonMonsterConfigs.values.flatMap { it.keys } +
                        KAngelDungeon.monsterConfigs.keys
                    allConfigs.distinct().toList()
                }
                execute<CommandSender> { sender, context, argument ->
                    val uuidStr = context["uuid"]
                    val configId = context["configId"]
                    val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                    if (uuid == null) {
                        sender.sendStringAsComponent(sender.asLangText("DebugInvalidUUID", uuidStr))
                        return@execute
                    }
                    val instance = KAngelDungeon.dungeonInstances[uuid]
                    if (instance == null) {
                        sender.sendStringAsComponent(sender.asLangText("DebugInstanceNotFound", uuidStr))
                        return@execute
                    }
                    if (instance.state != DungeonState.ACTIVE) {
                        sender.sendStringAsComponent(sender.asLangText("DebugDungeonNotActive", instance.templateName))
                        return@execute
                    }
                    val success = instance.spawnMonsters(configId)
                    if (success) {
                        sender.sendStringAsComponent(sender.asLangText("DebugSpawnSuccess", configId, instance.templateName))
                    } else {
                        sender.sendStringAsComponent(sender.asLangText("DebugSpawnFailed", configId))
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug checkBlockRegen
     * 检查方块恢复追踪数据
     */
    @CommandBody
    val checkBlockRegen = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val map = KAngelDungeon.blockRegenMap
            if (map.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("DebugNoBlockRegen"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("DebugBlockRegenHeader", map.size.toString()))
            for ((key, uuids) in map) {
                sender.sendStringAsComponent(sender.asLangText("DebugBlockRegenEntry", key, uuids.size.toString()))
            }
        }
    }

    // ==================== 新增详细调试命令 ====================

    /**
     * /kangeldungeon debug getInstance <uuid>
     * 显示地牢实例的完整详细信息
     */
    @CommandBody
    val getInstance = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInstanceNotFound", uuidStr))
                    return@execute
                }

                val stateLabel = sender.stateLabel(instance.state)
                val world = instance.world
                val worldStatus = if (world != null) sender.asLangText("StatusOnline") else sender.asLangText("StatusUnloaded")
                val aliveCount = instance.getAlivePlayerCount()
                val deadCount = instance.getDeadPlayerCount()
                val totalPlayers = instance.getPlayerCount()
                val offlineCount = totalPlayers - aliveCount - deadCount
                val leader = instance.getLeaderName() ?: "?"
                val elapsed = instance.getElapsedTime().toInt()
                val template = instance.getTemplate()
                val timeLimit = template?.timeLimit?.toInt()?.toString() ?: "∞"
                val remaining = if (template?.timeLimit != null) {
                    instance.getRemainingTime(template)?.toInt()?.toString() ?: "0"
                } else "∞"

                val createdStr = formatTimestamp(instance.createdAt)
                val startedStr = instance.startedAt?.let { formatTimestamp(it) } ?: sender.asLangText("ValueNone")

                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailHeader", uuidStr.take(8)))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailTemplate", instance.templateName))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailState", stateLabel))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailWorld", instance.worldName, worldStatus))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailPlayers", totalPlayers.toString(), aliveCount.toString(), deadCount.toString(), offlineCount.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailLeader", leader))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailTimes", createdStr, startedStr, elapsed.toString()))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailTimeLimit", timeLimit, remaining))
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailWorldReady", instance.worldReady.toString()))

                // 玩家列表
                for (p in instance.getOnlinePlayers()) {
                    val status = if (p.uniqueId in instance.deadPlayers)
                        sender.asLangText("DebugPlayerStatusDead")
                    else
                        sender.asLangText("DebugPlayerStatusAlive")
                    val health = "%.1f".format(p.health)
                    val isLeader = if (p.uniqueId == instance.leaderUUID) "★" else " "
                    sender.sendStringAsComponent(sender.asLangText("DebugPlayerEntry", isLeader + p.name, health, status, p.world.name))
                }
                for (playerUuid in instance.players) {
                    if (Bukkit.getPlayer(playerUuid) == null) {
                        val name = Bukkit.getOfflinePlayer(playerUuid).name ?: playerUuid.toString().take(8)
                        sender.sendStringAsComponent(sender.asLangText("DebugPlayerEntry", name, "?", sender.asLangText("DebugPlayerStatusOffline"), "?"))
                    }
                }

                // Meta
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailMeta", instance.meta.keys().size.toString()))

                // 活跃计划
                val activePlans = instance.getActivePlanNames()
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailActivePlans", activePlans.size.toString()))

                // 活跃怪物
                val activeMonstersMap = instance.getActiveMonsters()
                sender.sendStringAsComponent(sender.asLangText("DebugInstanceDetailActiveMonsters", activeMonstersMap.size.toString()))
            }
        }
    }

    /**
     * /kangeldungeon debug getKits <dungeon>
     * 显示地牢的Kit奖励包配置
     */
    @CommandBody
    val getKits = subCommand {
        dynamic("dungeon") {
            execute<CommandSender> { sender, context, argument ->
                val dungeonName = context["dungeon"]
                val configs = KAngelDungeon.dungeonKitConfigs
                if (dungeonName == "*") {
                    if (configs.isEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoKitForDungeon", "*"))
                        return@execute
                    }
                    for ((dungeon, map) in configs) {
                        sender.sendStringAsComponent(sender.asLangText("DebugKitHeader", dungeon, map.size.toString()))
                        for ((id, kit) in map) {
                            val mode = if (kit.isChanceMode) sender.asLangText("DebugKitChanceMode") else sender.asLangText("DebugKitWeightMode")
                            sender.sendStringAsComponent(sender.asLangText("DebugKitEntry", id, kit.rewards.size.toString(), mode, kit.minRewards.toString(), kit.maxRewards.toString()))
                        }
                    }
                } else {
                    val map = configs[dungeonName]
                    if (map.isNullOrEmpty()) {
                        sender.sendStringAsComponent(sender.asLangText("DebugNoKitForDungeon", dungeonName))
                        return@execute
                    }
                    sender.sendStringAsComponent(sender.asLangText("DebugKitHeader", dungeonName, map.size.toString()))
                    for ((id, kit) in map) {
                        val mode = if (kit.isChanceMode) sender.asLangText("DebugKitChanceMode") else sender.asLangText("DebugKitWeightMode")
                        sender.sendStringAsComponent(sender.asLangText("DebugKitEntry", id, kit.rewards.size.toString(), mode, kit.minRewards.toString(), kit.maxRewards.toString()))
                        kit.rewards.forEachIndexed { i, reward ->
                            when (reward.type) {
                                io.github.zzzyyylllty.kangeldungeon.data.RewardType.ITEM -> {
                                    val src = reward.source ?: "?"
                                    val itemName = reward.item ?: "?"
                                    sender.sendStringAsComponent(sender.asLangText("DebugKitRewardItem", (i + 1).toString(), src, itemName, reward.amount.toString(), reward.weight.toString()))
                                }
                                io.github.zzzyyylllty.kangeldungeon.data.RewardType.COMMAND -> {
                                    val cmd = reward.command?.take(40) ?: "?"
                                    sender.sendStringAsComponent(sender.asLangText("DebugKitRewardCommand", (i + 1).toString(), cmd, reward.weight.toString()))
                                }
                                io.github.zzzyyylllty.kangeldungeon.data.RewardType.SCRIPT -> {
                                    sender.sendStringAsComponent(sender.asLangText("DebugKitRewardScript", (i + 1).toString(), reward.weight.toString()))
                                }
                                io.github.zzzyyylllty.kangeldungeon.data.RewardType.AGENT -> {
                                    val trigger = reward.agentTrigger ?: "?"
                                    sender.sendStringAsComponent(sender.asLangText("DebugKitRewardAgent", (i + 1).toString(), trigger, reward.weight.toString()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getActivePlans <uuid>
     * 显示地牢实例中正在运行的计划
     */
    @CommandBody
    val getActivePlans = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInstanceNotFound", uuidStr))
                    return@execute
                }
                val planNames = instance.getActivePlanNames()
                if (planNames.isEmpty()) {
                    sender.sendStringAsComponent(sender.asLangText("DebugNoActivePlans"))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("DebugActivePlansHeader", instance.templateName, planNames.size.toString()))
                val planConfigs = KAngelDungeon.dungeonPlanConfigs[instance.templateName] ?: emptyMap()
                for (name in planNames) {
                    val trigger = planConfigs[name]?.trigger ?: "?"
                    sender.sendStringAsComponent(sender.asLangText("DebugActivePlanEntry", name, trigger))
                }
            }
        }
    }

    /**
     * /kangeldungeon debug getActiveMonsters <uuid>
     * 显示地牢实例中活跃的怪物组
     */
    @CommandBody
    val getActiveMonsters = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DebugInstanceNotFound", uuidStr))
                    return@execute
                }
                val activeMobs = instance.getActiveMonsters()
                if (activeMobs.isEmpty()) {
                    sender.sendStringAsComponent(sender.asLangText("DebugNoActiveMonsters"))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("DebugActiveMonstersHeader", instance.templateName, activeMobs.size.toString()))
                for ((configId, mobInst) in activeMobs) {
                    sender.sendStringAsComponent(sender.asLangText("DebugActiveMonsterEntry", configId, mobInst.spawnedMobs.size.toString(), mobInst.allKilled.toString()))
                    mobInst.config.monsters.forEachIndexed { i, entry ->
                        sender.sendStringAsComponent(sender.asLangText("DebugActiveMonsterMob", (i + 1).toString(), entry.mob, entry.location.x.toString(), entry.location.y.toString(), entry.location.z.toString()))
                    }
                }
            }
        }
    }
}

/** 格式化时间戳为 "yyyy-MM-dd HH:mm:ss" */
private fun formatTimestamp(ms: Long): String {
    return try {
        java.time.Instant.ofEpochMilli(ms)
            .atZone(java.time.ZoneId.systemDefault())
            .format(io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.dateTimeFormatter)
    } catch (e: Exception) { "?" }
}

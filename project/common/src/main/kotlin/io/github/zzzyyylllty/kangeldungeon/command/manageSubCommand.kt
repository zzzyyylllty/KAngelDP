package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.team.TeamManager
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.platform.util.asLangText

private fun CommandSender.stateLabel(state: DungeonState): String = when (state) {
    DungeonState.PREPARING -> asLangText("DungeonStatePreparing")
    DungeonState.ACTIVE -> asLangText("DungeonStateActive")
    DungeonState.COMPLETED -> asLangText("DungeonStateCompleted")
    DungeonState.FAILED -> asLangText("DungeonStateFailed")
}

@CommandHeader(
    name = "kangeldungeonmanage",
    aliases = ["dgm", "dungeonm"],
    permission = "kangeldungeon.command.dungeon",
    description = "Dungeon management commands.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object DungeonCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    /**
     * 列出所有可用地牢模板
     */
    @CommandBody
    val templates = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val templates = KAngelDungeon.dungeonTemplates
            if (templates.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("DungeonNoTemplates"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("DungeonTemplateHeader"))
            for ((name, template) in templates) {
                sender.sendStringAsComponent(
                    sender.asLangText("DungeonTemplateEntry",
                        template.displayName, name,
                        template.gameplayGeneral.minPlayers.toString(), template.gameplayGeneral.maxPlayers.toString(),
                        template.timeLimit?.toInt()?.toString() ?: "0")
                )
            }
        }
    }

    /**
     * 创建并自动开始地牢
     * /kangeldungeon dungeon create <template> [difficulty] [player] [players]
     */
    @CommandBody
    val create = subCommand {
        dynamic("template") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonTemplates.keys.toList()
            }
            // Only template -> sender as leader, default difficulty
            execute<CommandSender> { sender, context, argument ->
                val leader = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@execute
                }
                val defaultDiff = getDefaultDifficulty(context["template"])
                createDungeonAndStart(sender, context["template"], leader, null, defaultDiff)
            }
            // template [difficulty] [player] [players]
            dynamic("difficulty", optional = true) {
                suggestion<CommandSender> { _, args ->
                    val template = args["template"]
                    KAngelDungeon.dungeonDifficultyConfigs[template]?.keys?.toList() ?: emptyList()
                }
                // template difficulty only -> sender as leader
                execute<CommandSender> { sender, context, argument ->
                    val leader = sender as? Player ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@execute
                    }
                    val diff = context["difficulty"].takeIf { it.isNotBlank() }
                    createDungeonAndStart(sender, context["template"], leader, null, diff)
                }
                player("player") {
                    execute<CommandSender> { sender, context, argument ->
                        val leader = context.player("player").castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                            return@execute
                        }
                        val diff = context["difficulty"].takeIf { it.isNotBlank() }
                        createDungeonAndStart(sender, context["template"], leader, null, diff)
                    }
                    dynamic("players") {
                        suggestion<CommandSender> { _, _ ->
                            Bukkit.getOnlinePlayers().map { it.name }.toList()
                        }
                        execute<CommandSender> { sender, context, argument ->
                            val leader = context.player("player").castSafely<Player>() ?: run {
                                sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                                return@execute
                            }
                            val diff = context["difficulty"].takeIf { it.isNotBlank() }
                            createDungeonAndStart(sender, context["template"], leader, context["players"], diff)
                        }
                    }
                }
            }
            // template [player] [players] (no difficulty)
            player("player") {
                execute<CommandSender> { sender, context, argument ->
                    val leader = context.player("player").castSafely<Player>() ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                        return@execute
                    }
                    val defaultDiff = getDefaultDifficulty(context["template"])
                    createDungeonAndStart(sender, context["template"], leader, null, defaultDiff)
                }
                dynamic("players") {
                    suggestion<CommandSender> { _, _ ->
                        Bukkit.getOnlinePlayers().map { it.name }.toList()
                    }
                    execute<CommandSender> { sender, context, argument ->
                        val leader = context.player("player").castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                            return@execute
                        }
                        val defaultDiff = getDefaultDifficulty(context["template"])
                        createDungeonAndStart(sender, context["template"], leader, context["players"], defaultDiff)
                    }
                }
            }
        }
    }

    /**
     * 开始地牢
     * /kangeldungeon dungeon start <uuid>
     */
    @CommandBody
    val start = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances
                    .filter { it.value.state == DungeonState.PREPARING }
                    .map { it.key.toString() }
                    .toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                    return@execute
                }
                val success = instance.start()
                if (success) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonStarted", instance.templateName))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("DungeonCannotStart", instance.templateName))
                }
            }
        }
    }

    /**
     * 停止/结束地牢
     * /kangeldungeon dungeon stop <uuid>
     */
    @CommandBody
    val stop = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances
                    .filter { it.value.state == DungeonState.ACTIVE || it.value.state == DungeonState.PREPARING }
                    .map { it.key.toString() }
                    .toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                    return@execute
                }
                val success = instance.fail()
                if (success) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonStopped", instance.templateName))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("DungeonCannotStop", instance.templateName))
                }
            }
        }
    }

    /**
     * 列出活跃地牢实例
     * /kangeldungeon dungeon list
     */
    @CommandBody
    val list = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val instances = KAngelDungeon.dungeonInstances
            if (instances.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("DungeonNoInstances"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("DungeonListHeader", instances.size.toString()))
            for ((uuid, instance) in instances) {
                val stateLabel = sender.stateLabel(instance.state)
                val elapsed = instance.getElapsedTime().toInt()
                val diff = instance.getDifficulty() ?: "N/A"
                sender.sendStringAsComponent(
                    sender.asLangText("DungeonListEntry",
                        instance.templateName, stateLabel, diff,
                        instance.getAlivePlayerCount().toString(), instance.getPlayerCount().toString(),
                        elapsed.toString(), uuid.toString())
                )
            }
        }
    }

    /**
     * 查看地牢详情
     * /kangeldungeon dungeon info <uuid>
     */
    @CommandBody
    val info = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("DungeonInfoHeader"))
                sender.sendStringAsComponent(sender.asLangText("DungeonInfoTemplate", instance.templateName))
                sender.sendStringAsComponent(sender.asLangText("DungeonInfoUUID", uuid.toString()))
                sender.sendStringAsComponent(sender.asLangText("DungeonInfoState", instance.state.name))
                val diffId = instance.getDifficulty()
                if (diffId != null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInfoDifficulty", diffId))
                }
                sender.sendStringAsComponent(sender.asLangText("DungeonInfoPlayers", instance.getAlivePlayerCount().toString(), instance.getPlayerCount().toString()))
                sender.sendStringAsComponent(sender.asLangText("DungeonInfoOnlinePlayers", instance.getOnlinePlayerNames().joinToString(", ") { "<green>$it</green>" }))
                sender.sendStringAsComponent(sender.asLangText("DungeonInfoElapsed", instance.getElapsedTime().toInt().toString()))
                val template = instance.getTemplate()
                if (template != null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInfoTimeLimit", template.timeLimit?.toInt()?.toString() ?: sender.asLangText("ValueUnlimited")))
                }
            }
        }
    }

    /**
     * 加入地牢
     * /kangeldungeon dungeon join <uuid>
     */
    @CommandBody
    val join = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances
                    .filter { it.value.state == DungeonState.PREPARING || it.value.state == DungeonState.ACTIVE }
                    .map { it.key.toString() }
                    .toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@execute
                }
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                    return@execute
                }
                val dungeonTemplate = instance.getTemplate()
                val permission = dungeonTemplate?.requiredPermission
                if (permission != null && !player.hasPermission(permission)) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonNoPermission", permission))
                    return@execute
                }
                // 先让队长加入，队长加入成功后才拉队员
                val leaderAdded = instance.addPlayer(player)
                if (!leaderAdded) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonCannotJoin"))
                    return@execute
                }
                // 队伍集成：如果队长加入成功，自动将所有在线队员也加入
                if (TeamManager.isEnabled && dungeonTemplate?.gameplayGeneral?.allowParty == true) {
                    val party = TeamManager.getTeam(player.uniqueId)
                    if (party != null && party.isLeader(player.uniqueId)) {
                        for (memberId in party.members) {
                            if (memberId != player.uniqueId) {
                                val member = Bukkit.getPlayer(memberId)
                                if (member != null && member.isOnline) {
                                    if (!instance.addPlayer(member)) {
                                        sender.sendStringAsComponent(sender.asLangText("DungeonAddPlayerFailed", member.name))
                                    }
                                }
                            }
                        }
                    }
                }
                sender.sendStringAsComponent(sender.asLangText("DungeonJoined", instance.templateName))
            }
        }
    }

    /**
     * 离开地牢
     * /kangeldungeon dungeon leave
     */
    @CommandBody
    val leave = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            val instanceUuid = KAngelDungeon.playerToInstanceIndex[player.uniqueId]
            val instance = if (instanceUuid != null) KAngelDungeon.dungeonInstances[instanceUuid] else null
            if (instance == null) {
                sender.sendStringAsComponent(sender.asLangText("DungeonNotInDungeon"))
                return@execute
            }
            val success = instance.removePlayer(player)
            if (success) {
                sender.sendStringAsComponent(sender.asLangText("DungeonLeft", instance.templateName))
            } else {
                sender.sendStringAsComponent(sender.asLangText("DungeonCannotLeave"))
            }
        }
    }

    /**
     * 传送至地牢世界
     * /kangeldungeon dungeon tp <uuid>
     */
    @CommandBody
    val tp = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances
                    .filter { it.value.state == DungeonState.ACTIVE || it.value.state == DungeonState.PREPARING }
                    .map { it.key.toString() }
                    .toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@execute
                }
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                    return@execute
                }
                // 检查权限
                val dungeonTemplate = instance.getTemplate()
                val permission = dungeonTemplate?.requiredPermission
                if (permission != null && !player.hasPermission(permission)) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonNoTeleportPermission", permission))
                    return@execute
                }
                // 先尝试将玩家加入地牢（会自动缓存原位置、传送并处理事件）
                if (player.uniqueId !in instance.players) {
                    if (!instance.addPlayer(player)) {
                        sender.sendStringAsComponent(sender.asLangText("DungeonAddPlayerFailed", player.name))
                        return@execute
                    }
                } else {
                    player.teleport(instance.spawnLocation)
                }
                sender.sendStringAsComponent(sender.asLangText("DungeonTeleported", instance.templateName))
            }
        }
    }

    /**
     * 手动完成地牢
     * /kangeldungeon dungeon complete <uuid>
     */
    @CommandBody
    val complete = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances
                    .filter { it.value.state == DungeonState.ACTIVE }
                    .map { it.key.toString() }
                    .toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                    return@execute
                }
                val success = instance.complete()
                if (success) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonCompleted", instance.templateName))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("DungeonCannotComplete", instance.templateName))
                }
            }
        }
    }

    /**
     * 将玩家踢出地牢
     * /kangeldungeon dungeon kick <player>
     */
    @CommandBody
    val kick = subCommand {
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                val target = context.player("player").castSafely<Player>()
                if (target == null) {
                    sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                    return@execute
                }
                val instanceUuid = KAngelDungeon.playerToInstanceIndex[target.uniqueId]
                val instance = if (instanceUuid != null) KAngelDungeon.dungeonInstances[instanceUuid] else null
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonPlayerNotInDungeon"))
                    return@execute
                }
                val success = instance.removePlayer(target)
                if (success) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonKickSuccess", target.name, instance.templateName))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("DungeonKickFailed"))
                }
            }
        }
    }

    /**
     * 强制离开地牢（跳过 onLeave 条件检查）
     * /kangeldungeon dungeon forceleave
     */
    @CommandBody
    val forceleave = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            if (!sender.hasPermission("kangeldungeon.command.forceleave")) {
                sender.sendStringAsComponent(sender.asLangText("DungeonNoPermissionForceLeave"))
                return@execute
            }
            val instanceUuid = KAngelDungeon.playerToInstanceIndex[player.uniqueId]
            val instance = if (instanceUuid != null) KAngelDungeon.dungeonInstances[instanceUuid] else null
            if (instance == null) {
                sender.sendStringAsComponent(sender.asLangText("DungeonNotInDungeon"))
                return@execute
            }
            val success = instance.forceRemovePlayer(player)
            if (success) {
                sender.sendStringAsComponent(sender.asLangText("DungeonForceLeft", instance.templateName))
            } else {
                sender.sendStringAsComponent(sender.asLangText("DungeonCannotLeave"))
            }
        }
    }

    /**
     * 强制踢出玩家（跳过 onLeave 条件检查）
     * /kangeldungeon dungeon forcekick <player>
     */
    @CommandBody
    val forcekick = subCommand {
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                val target = context.player("player").castSafely<Player>()
                if (target == null) {
                    sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                    return@execute
                }
                val instanceUuid = KAngelDungeon.playerToInstanceIndex[target.uniqueId]
                val instance = if (instanceUuid != null) KAngelDungeon.dungeonInstances[instanceUuid] else null
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonPlayerNotInDungeon"))
                    return@execute
                }
                val success = instance.forceRemovePlayer(target)
                if (success) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonForceKickSuccess", target.name, instance.templateName))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("DungeonKickFailed"))
                }
            }
        }
    }

    /**
     * 将玩家添加到指定地牢
     * /kangeldungeon dungeon addplayer <uuid> <player>
     */
    @CommandBody
    val addplayer = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances
                    .filter { it.value.state == DungeonState.PREPARING || it.value.state == DungeonState.ACTIVE }
                    .map { it.key.toString() }
                    .toList()
            }
            player("player") {
                execute<CommandSender> { sender, context, argument ->
                    val target = context.player("player").castSafely<Player>() ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                        return@execute
                    }
                    val uuidStr = context["uuid"]
                    val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                    if (uuid == null) {
                        sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                        return@execute
                    }
                    val instance = KAngelDungeon.dungeonInstances[uuid]
                    if (instance == null) {
                        sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                        return@execute
                    }
                    val template = instance.getTemplate()
                    val permission = template?.requiredPermission
                    if (permission != null && !target.hasPermission(permission)) {
                        sender.sendStringAsComponent(sender.asLangText("DungeonAddPlayerNoPerm", target.name, permission))
                        return@execute
                    }
                    val success = instance.addPlayer(target)
                    if (success) {
                        sender.sendStringAsComponent(sender.asLangText("DungeonAddPlayerSuccess", target.name, instance.templateName))
                    } else {
                        sender.sendStringAsComponent(sender.asLangText("DungeonAddPlayerFailed"))
                    }
                }
            }
        }
    }

    /**
     * 列出地牢中的所有玩家及其状态
     * /kangeldungeon dungeon listplayers <uuid>
     */
    @CommandBody
    val listplayers = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("DungeonInstanceNotFound", uuidStr))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("DungeonListPlayersHeader", instance.templateName))
                sender.sendStringAsComponent(sender.asLangText("DungeonListPlayersLeader", instance.getLeaderName() ?: sender.asLangText("ValueUnknown")))
                for (pUuid in instance.players) {
                    val player = Bukkit.getPlayer(pUuid)
                    val status = when {
                        player == null -> sender.asLangText("DungeonPlayerStatusOffline")
                        pUuid in instance.deadPlayers -> sender.asLangText("DungeonPlayerStatusDead")
                        else -> sender.asLangText("DungeonPlayerStatusOnline")
                    }
                    val name = player?.name ?: Bukkit.getOfflinePlayer(pUuid).name ?: pUuid.toString().take(8)
                    sender.sendStringAsComponent(sender.asLangText("DungeonListPlayerEntry", name, status))
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    /** 获取模板的默认难度：优先取 "normal"，否则取第一个，没有则返回 null */
    private fun getDefaultDifficulty(templateName: String): String? {
        val difficulties = KAngelDungeon.dungeonDifficultyConfigs[templateName] ?: return null
        if (difficulties.isEmpty()) return null
        return if (difficulties.containsKey("normal")) "normal" else difficulties.keys.firstOrNull()
    }

    private fun createDungeonAndStart(sender: CommandSender, templateName: String, leader: Player, extraPlayersStr: String?, difficultyId: String? = null) {
        val template = KAngelDungeon.dungeonTemplates[templateName]
        if (template == null) {
            sender.sendStringAsComponent(sender.asLangText("DungeonTemplateNotExist", templateName))
            return
        }

        // 检查权限
        val permission = template.requiredPermission
        if (permission != null && !leader.hasPermission(permission)) {
            sender.sendStringAsComponent(sender.asLangText("DungeonNoPermissionCreate", leader.name, permission))
            return
        }

        val minPlayers = template.gameplayGeneral.minPlayers
        val maxPlayers = template.gameplayGeneral.maxPlayers

        // 收集手动指定的额外玩家
        var manualPlayers = emptyList<Player>()
        if (!extraPlayersStr.isNullOrBlank()) {
            manualPlayers = extraPlayersStr.split(" ").mapNotNull { name ->
                val p = Bukkit.getPlayerExact(name.trim())
                if (p != null) {
                    if (permission != null && !p.hasPermission(permission)) {
                        sender.sendStringAsComponent(sender.asLangText("DungeonPlayerSkipPermission", p.name, permission))
                        null
                    } else p
                } else null
            }
        }

        // 队伍集成：如果队长在队伍中且 allowParty 为 true，自动包含所有在线队员
        val partyMembers = if (template.gameplayGeneral.allowParty && TeamManager.isEnabled) {
            val party = TeamManager.getTeam(leader.uniqueId)
            if (party != null) {
                party.members.mapNotNull { memberId ->
                    if (memberId == leader.uniqueId) null
                    else {
                        val member = Bukkit.getPlayer(memberId)
                        if (member != null && member.isOnline) {
                            if (permission != null && !member.hasPermission(permission)) {
                                sender.sendStringAsComponent(sender.asLangText("DungeonMemberSkipPermission", member.name, permission))
                                null
                            } else member
                        } else null
                    }
                }
            } else emptyList()
        } else emptyList()

        // 合并所有玩家（leader + 队伍成员 + 手动指定），去重
        val allExtra = (partyMembers + manualPlayers).distinct()
        val totalPlayers = 1 + allExtra.size

        // 检查人数限制
        if (totalPlayers < minPlayers) {
            sender.sendStringAsComponent(sender.asLangText("DungeonMinPlayers", minPlayers.toString(), totalPlayers.toString()))
            return
        }
        if (totalPlayers > maxPlayers) {
            sender.sendStringAsComponent(sender.asLangText("DungeonMaxPlayers", maxPlayers.toString(), totalPlayers.toString()))
            return
        }

        val players = mutableListOf(leader)
        players.addAll(allExtra)
        val dungeonUUID = DungeonHelper.createDungeon(templateName, players, leader, emptyMap(), difficultyId = difficultyId) { uuid ->
            val instance = KAngelDungeon.dungeonInstances[uuid]
            if (instance != null) {
                val template = instance.getTemplate()
                val prepTime = template?.preparationTime ?: 0.0
                if (prepTime > 0 && instance.isPreparing()) {
                    // 有准备时间：由 tick 倒计时自动开始，这里只报告成功
                    sender.sendStringAsComponent(sender.asLangText("DungeonCreateSuccess", templateName))
                } else {
                    // 无准备时间或已错过 PREPARING 状态：立即开始或确认已开始
                    if (instance.start() || instance.state == DungeonState.ACTIVE) {
                        sender.sendStringAsComponent(sender.asLangText("DungeonCreateSuccess", templateName))
                    } else {
                        sender.sendStringAsComponent(sender.asLangText("DungeonCreateFailed", templateName))
                    }
                }
            } else {
                sender.sendStringAsComponent(sender.asLangText("DungeonCreateSuccessNoStart", templateName))
            }
        }
        if (dungeonUUID == null) {
            sender.sendStringAsComponent(sender.asLangText("DungeonCreateFailed", templateName))
        }
    }
}

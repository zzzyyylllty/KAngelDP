package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import io.github.zzzyyylllty.kangeldungeon.util.region.RegionManager
import org.bukkit.Bukkit
import org.bukkit.World
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

/**
 * Usage: /kangeldungeonadmin
 *          ├── info                  插件综合信息
 *          ├── stopall               强制停止所有活跃地牢
 *          ├── purge                 清理已完成/失败的实例
 *          ├── maintenance [on/off]  维护模式
 *          ├── save                  保存所有数据
 *          ├── worlds                地牢世界列表
 *          ├── unloadworld <world>   卸载地牢世界
 *          ├── playerinfo <player>   玩家地牢信息
 *          ├── blacklist
 *          │   ├── add <player> [reason]
 *          │   ├── remove <player>
 *          │   └── list
 *          ├── meta <uuid>
 *          │   ├── list
 *          │   ├── get <key>
 *          │   ├── set <key> <value>
 *          │   ├── add <key> <value>
 *          │   └── delete <key>
 *          ├── instance <uuid>       实例详情
 *          ├── broadcast <uuid> <message>
 *          ├── heal <uuid>           治疗地牢中所有玩家
 *          └── kickall               踢出所有地牢中的玩家
 * */

@CommandHeader(
    name = "kangeldungeonadmin",
    aliases = ["kda", "dungeonadmin"],
    permission = "kangeldungeon.command.admin",
    description = "Admin commands of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object AdminCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    // ==================== 综合信息 ====================

    /**
     * /kda info
     * 显示插件的综合状态信息
     */
    @CommandBody
    val info = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val totalInstances = KAngelDungeon.dungeonInstances.size
            val active = KAngelDungeon.dungeonInstances.count { it.value.state == DungeonState.ACTIVE }
            val preparing = KAngelDungeon.dungeonInstances.count { it.value.state == DungeonState.PREPARING }
            val completed = KAngelDungeon.dungeonInstances.count { it.value.state == DungeonState.COMPLETED }
            val failed = KAngelDungeon.dungeonInstances.count { it.value.state == DungeonState.FAILED }
            val templates = KAngelDungeon.dungeonTemplates.size
            val maintenance = KAngelDungeon.maintenanceMode
            val blacklisted = KAngelDungeon.blacklistedPlayers.size

            sender.sendStringAsComponent(sender.asLangText("AdminInfoHeader"))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoMaintenance",
                if (maintenance) sender.asLangText("StatusEnabled") else sender.asLangText("StatusDisabled")))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoDevMode",
                if (KAngelDungeon.devMode) sender.asLangText("StatusEnabled") else sender.asLangText("StatusDisabled")))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoTemplates", templates.toString()))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoInstances", totalInstances.toString(), active.toString(), preparing.toString(), completed.toString(), failed.toString()))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoBlacklisted",
                if (blacklisted > 0) "<red>$blacklisted</red>" else "<green>0</green>"))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoScripts", KAngelDungeon.dungeonScripts.values.sumOf { it.size }.toString()))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoObstacles", (KAngelDungeon.dungeonObstacleConfigs.values.sumOf { it.size } + KAngelDungeon.obstacleConfigs.size).toString()))
            sender.sendStringAsComponent(sender.asLangText("AdminInfoMonsters", (KAngelDungeon.dungeonMonsterConfigs.values.sumOf { it.size } + KAngelDungeon.monsterConfigs.size).toString()))
        }
    }

    // ==================== 强制停止所有 ====================

    /**
     * /kda stopall
     * 强制停止所有活跃地牢实例
     */
    @CommandBody
    val stopall = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val activeInstances = KAngelDungeon.dungeonInstances.values
                .filter { it.state == DungeonState.ACTIVE || it.state == DungeonState.PREPARING }
            if (activeInstances.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("AdminNoActiveInstances"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("AdminStoppingInstances", activeInstances.size.toString()))
            var successCount = 0
            for (instance in activeInstances) {
                try {
                    if (instance.fail()) {
                        instance.sendMessageToAllPlayers(sender.asLangText("AdminStopNotify"))
                        successCount++
                    }
                } catch (e: Exception) {
                    sender.sendStringAsComponent(sender.asLangText("AdminStopError", instance.templateName, instance.uuid.toString(), e.message ?: "Unknown"))
                }
            }
            sender.sendStringAsComponent(sender.asLangText("AdminStopSuccess", successCount.toString(), activeInstances.size.toString()))
        }
    }

    // ==================== 清理已完成/失败实例 ====================

    /**
     * /kda purge
     * 清理所有已完成/已失败的地牢实例
     */
    @CommandBody
    val purge = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val finishedInstances = KAngelDungeon.dungeonInstances.values
                .filter { it.state == DungeonState.COMPLETED || it.state == DungeonState.FAILED }
            if (finishedInstances.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("AdminNoFinishedInstances"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("AdminPurgingInstances", finishedInstances.size.toString()))
            var successCount = 0
            for (instance in finishedInstances) {
                try {
                    instance.stopAllPlans()
                    RegionManager.clearWorld(instance.worldName)
                    DungeonHelper.unloadDungeonWorld(instance)
                    successCount++
                } catch (e: Exception) {
                    sender.sendStringAsComponent(sender.asLangText("AdminPurgeError", instance.templateName, instance.uuid.toString(), e.message ?: "Unknown"))
                }
            }
            sender.sendStringAsComponent(sender.asLangText("AdminPurgeSuccess", successCount.toString(), finishedInstances.size.toString()))
        }
    }

    // ==================== 维护模式 ====================

    /**
     * /kda maintenance [on/off]
     * 查看或切换维护模式
     */
    @CommandBody
    val maintenance = subCommand {
        dynamic("mode") {
            suggestion<CommandSender> { _, _ -> listOf("on", "off") }
            execute<CommandSender> { sender, context, argument ->
                val mode = context["mode"]
                when (mode.lowercase()) {
                    "on" -> {
                        KAngelDungeon.maintenanceMode = true
                        sender.sendStringAsComponent(sender.asLangText("AdminMaintenanceOn"))
                        Bukkit.getOnlinePlayers().forEach { player ->
                            if (!player.hasPermission("kangeldungeon.command.admin")) {
                                player.sendStringAsComponent(sender.asLangText("AdminMaintenanceNotify"))
                            }
                        }
                    }
                    "off" -> {
                        KAngelDungeon.maintenanceMode = false
                        sender.sendStringAsComponent(sender.asLangText("AdminMaintenanceOff"))
                    }
                    else -> sender.sendStringAsComponent(sender.asLangText("AdminMaintenanceUsage"))
                }
            }
        }
        execute<CommandSender> { sender, context, argument ->
            val status = KAngelDungeon.maintenanceMode
            sender.sendStringAsComponent(
                sender.asLangText("AdminMaintenanceStatus",
                    if (status) sender.asLangText("StatusEnabled") else sender.asLangText("StatusDisabled"))
            )
        }
    }

    // ==================== 保存数据 ====================

    /**
     * /kda save
     * 保存所有地牢数据（目前仅保存日志信息）
     */
    @CommandBody
    val save = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendStringAsComponent(sender.asLangText("AdminSavingDungeons"))
            val instances = KAngelDungeon.dungeonInstances.values.toList()
            val now = KAngelDungeon.dateTimeFormatter.format(java.time.LocalDateTime.now())
            var saved = 0
            for (instance in instances) {
                try {
                    val state = instance.state.name
                    val playerCount = instance.getPlayerCount()
                    val elapsed = instance.getElapsedTime().toInt()
                    sender.sendStringAsComponent(sender.asLangText("AdminSaveSnapshot", now, instance.templateName, state, playerCount.toString(), elapsed.toString()))
                    saved++
                } catch (_: Exception) {}
            }
            sender.sendStringAsComponent(sender.asLangText("AdminSaveSuccess", saved.toString()))
        }
    }

    // ==================== 地牢世界管理 ====================

    /**
     * /kda worlds
     * 列出所有地牢世界
     */
    @CommandBody
    val worlds = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val instances = KAngelDungeon.dungeonInstances
            if (instances.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("AdminNoWorlds"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("AdminWorldListHeader", instances.size.toString()))
            for ((uuid, instance) in instances) {
                val world = instance.world
                val worldStatus = if (world != null) sender.asLangText("StatusLoaded") else sender.asLangText("StatusUnloaded")
                val playerCount = world?.players?.size ?: 0
                val stateLabel = sender.stateLabel(instance.state)
                sender.sendStringAsComponent(
                    sender.asLangText("AdminWorldEntry", instance.templateName, stateLabel, instance.worldName, worldStatus, playerCount.toString(), uuid.toString())
                )
            }
        }
    }

    /**
     * /kda unloadworld <world>
     * 卸载指定地牢世界
     */
    @CommandBody
    val unloadworld = subCommand {
        dynamic("world") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.values.mapNotNull { it.world?.name }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val worldName = context["world"]
                val world = Bukkit.getWorld(worldName)
                if (world == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminWorldNotLoaded", worldName))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances.values.firstOrNull { it.worldName == worldName }
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminWorldNotDungeon", worldName))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("AdminWorldUnloading", worldName))
                try {
                    world.players.forEach { player ->
                        val prev = DungeonHelper.playerPreviousLocations.remove(player.uniqueId)
                        if (prev != null) {
                            player.teleport(prev)
                        } else {
                            val spawn = Bukkit.getWorlds().first().spawnLocation
                            player.teleport(spawn)
                        }
                    }
                    instance.stopAllPlans()
                    RegionManager.clearWorld(instance.worldName)
                    DungeonHelper.unloadDungeonWorld(instance)
                    sender.sendStringAsComponent(sender.asLangText("AdminWorldUnloaded", worldName))
                } catch (e: Exception) {
                    sender.sendStringAsComponent(sender.asLangText("AdminWorldUnloadError", e.message ?: "Unknown"))
                }
            }
        }
    }

    // ==================== 玩家信息 ====================

    /**
     * /kda playerinfo <player>
     * 查看玩家的地牢相关信息
     */
    @CommandBody
    val playerinfo = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, argument ->
                val playerName = context["player"]
                val player = Bukkit.getPlayerExact(playerName)
                if (player == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerNotOnline", playerName))
                    return@execute
                }

                val isBlacklisted = KAngelDungeon.blacklistedPlayers.containsKey(playerName.lowercase())
                val currentDungeon = KAngelDungeon.dungeonInstances.values.firstOrNull {
                    it.players.contains(player.uniqueId)
                }
                val isDead = currentDungeon?.deadPlayers?.contains(player.uniqueId) == true
                val isLeader = currentDungeon?.leaderUUID == player.uniqueId

                val historyCount = KAngelDungeon.dungeonInstances.values.count {
                    it.players.contains(player.uniqueId) && it.isFinished()
                }

                sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoHeader", playerName))
                sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoUUID", player.uniqueId.toString()))
                sender.sendStringAsComponent(
                    sender.asLangText("AdminPlayerInfoBlacklist",
                        if (isBlacklisted) sender.asLangText("StatusEnabled") else sender.asLangText("StatusDisabled"))
                )
                if (isBlacklisted) {
                    val reason = KAngelDungeon.blacklistedPlayers[playerName.lowercase()]
                    if (reason != null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoBanReason", reason))
                    }
                }
                if (currentDungeon != null) {
                    val stateLabel = sender.stateLabel(currentDungeon.state)
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoCurrentDungeon", currentDungeon.templateName, stateLabel))
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoDungeonUUID", currentDungeon.uuid.toString()))
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoIsLeader",
                        if (isLeader) sender.asLangText("StatusYes") else sender.asLangText("StatusNo")))
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoIsDead",
                        if (isDead) sender.asLangText("StatusYes") else sender.asLangText("StatusNo")))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoNoDungeon"))
                }
                sender.sendStringAsComponent(sender.asLangText("AdminPlayerInfoHistory", historyCount.toString()))
            }
        }
    }

    // ==================== 黑名单管理 ====================

    @CommandBody
    val blacklist = AdminBlacklistCommand

    // ==================== 地牢元数据管理 ====================

    @CommandBody
    val meta = AdminMetaCommand

    // ==================== 实例详情 ====================

    /**
     * /kda instance <uuid>
     * 查看地牢实例的详细信息
     */
    @CommandBody
    val instance = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                    return@execute
                }

                val world = instance.world
                val template = instance.getTemplate()
                val elapsed = instance.getElapsedTime().toInt()
                val elapsedFormatted = "%02d:%02d".format(elapsed / 60, elapsed % 60)
                val remaining = template?.let { instance.getRemainingTime(it) }

                sender.sendStringAsComponent(sender.asLangText("AdminInstanceHeader"))
                sender.sendStringAsComponent(sender.asLangText("AdminInstanceTemplate", instance.templateName))
                sender.sendStringAsComponent(sender.asLangText("AdminInstanceUUID", uuidStr))
                sender.sendStringAsComponent(sender.asLangText("AdminInstanceState", sender.stateLabel(instance.state)))
                val worldStatus = if (world != null) sender.asLangText("StatusLoaded") else sender.asLangText("StatusUnloaded")
                sender.sendStringAsComponent(sender.asLangText("AdminInstanceWorld", instance.worldName, worldStatus))
                sender.sendStringAsComponent(sender.asLangText("AdminInstancePlayers",
                    instance.getPlayerCount().toString(),
                    instance.getAlivePlayerCount().toString(),
                    instance.getDeadPlayerCount().toString()))
                val onlineNames = instance.getOnlinePlayerNames().joinToString(", ") { "<green>$it</green>" }
                sender.sendStringAsComponent(sender.asLangText("AdminInstanceOnlinePlayers", onlineNames))
                sender.sendStringAsComponent(sender.asLangText("AdminInstanceLeader", instance.getLeaderName() ?: "null"))
                if (template != null) {
                    val timeLimit = template.timeLimit ?: 0.0
                    if (timeLimit > 0) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInstanceTimeLimit", timeLimit.toLong().toString()))
                    }
                    sender.sendStringAsComponent(sender.asLangText("AdminInstanceElapsed", elapsedFormatted))
                    if (remaining != null) {
                        val remainingFormatted = "%02d:%02d".format(remaining / 60, remaining % 60)
                        sender.sendStringAsComponent(sender.asLangText("AdminInstanceTimeRemaining", remainingFormatted))
                    }
                }
                val mobKills = instance.meta.getAsInt("mob.kill") ?: 0
                if (mobKills > 0) {
                    sender.sendStringAsComponent(sender.asLangText("AdminInstanceMobKills", mobKills.toString()))
                }
            }
        }
    }

    // ==================== 广播 ====================

    /**
     * /kda broadcast <uuid> <message>
     * 向地牢中所有玩家发送消息
     */
    @CommandBody
    val broadcast = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            dynamic("message") {
                execute<CommandSender> { sender, context, argument ->
                    val uuidStr = context["uuid"]
                    val message = context["message"]
                    val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                    if (uuid == null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                        return@execute
                    }
                    val instance = KAngelDungeon.dungeonInstances[uuid]
                    if (instance == null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                        return@execute
                    }
                    instance.sendMessageToAllPlayers(message)
                    sender.sendStringAsComponent(sender.asLangText("AdminBroadcastSent", instance.templateName, message))
                }
            }
        }
    }

    // ==================== 治疗 ====================

    /**
     * /kda heal <uuid>
     * 治疗地牢中所有玩家
     */
    @CommandBody
    val heal = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                    return@execute
                }
                instance.healAllPlayers()
                sender.sendStringAsComponent(sender.asLangText("AdminHealedAll", instance.templateName))
            }
        }
    }

    // ==================== 踢出所有 ====================

    /**
     * /kda forceleave <uuid> <player>
     * 强制将指定玩家从指定地牢中移除（跳过 onLeave 检查）
     */
    @CommandBody
    val forceleave = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
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
                        sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                        return@execute
                    }
                    val instance = KAngelDungeon.dungeonInstances[uuid]
                    if (instance == null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
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
    }

    /**
     * /kda kickall
     * 将所有地牢中的玩家踢出地牢，并清理空实例
     */
    @CommandBody
    val kickall = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val allInstances = KAngelDungeon.dungeonInstances.values.toList()
            if (allInstances.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("AdminNoActiveInstances"))
                return@execute
            }
            var kicked = 0
            for (instance in allInstances) {
                val playerUUIDs = instance.players.toList()
                for (playerUUID in playerUUIDs) {
                    val player = Bukkit.getPlayer(playerUUID)
                    if (player != null && player.isOnline) {
                        try {
                            instance.forceRemovePlayer(player)
                            player.sendStringAsComponent(sender.asLangText("AdminKickAllNotify"))
                            kicked++
                        } catch (_: Exception) {}
                    } else {
                        // 离线玩家：清理追踪数据
                        instance.players.remove(playerUUID)
                        instance.deadPlayers.remove(playerUUID)
                        KAngelDungeon.playerToInstanceIndex.remove(playerUUID, instance.uuid)
                        DungeonHelper.playerPreviousLocations.remove(playerUUID)
                    }
                }
                // 踢出所有玩家后清理空实例
                if (instance.players.isEmpty()) {
                    try {
                        instance.stopAllPlans()
                        RegionManager.clearWorld(instance.worldName)
                        DungeonHelper.unloadDungeonWorld(instance)
                    } catch (_: Exception) {}
                }
            }
            sender.sendStringAsComponent(sender.asLangText("AdminKickAllSuccess", kicked.toString()))
        }
    }
}

/**
 * /kda blacklist 子命令
 */
@CommandHeader(
    name = "kangeldungeonblacklist",
    aliases = [],
    permission = "kangeldungeon.command.admin",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object AdminBlacklistCommand {

    /**
     * /kda blacklist add <player> [reason]
     */
    @CommandBody
    val add = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, argument ->
                val playerName = context["player"]
                val key = playerName.lowercase()
                if (KAngelDungeon.blacklistedPlayers.containsKey(key)) {
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerBlacklistExists", playerName))
                    return@execute
                }
                KAngelDungeon.blacklistedPlayers[key] = sender.asLangText("ValueUnknown")
                sender.sendStringAsComponent(sender.asLangText("AdminPlayerBlacklisted", playerName))

                val target = Bukkit.getPlayerExact(playerName)
                if (target != null) {
                    val instance = KAngelDungeon.dungeonInstances.values.firstOrNull {
                        it.players.contains(target.uniqueId)
                    }
                    if (instance != null) {
                        instance.removePlayer(target)
                        target.sendStringAsComponent(sender.asLangText("AdminPlayerBlacklistKicked"))
                    }
                    target.sendStringAsComponent(sender.asLangText("AdminPlayerBlacklistNotify"))
                }
            }
        }
        dynamic("reason") {
            execute<CommandSender> { sender, context, argument ->
                val playerName = context["player"] ?: return@execute
                val reason = context["reason"] ?: sender.asLangText("ValueUnknown")
                val key = playerName.lowercase()
                KAngelDungeon.blacklistedPlayers[key] = reason
                sender.sendStringAsComponent(sender.asLangText("AdminPlayerBlacklistReason", playerName, reason))

                val target = Bukkit.getPlayerExact(playerName)
                if (target != null) {
                    val instance = KAngelDungeon.dungeonInstances.values.firstOrNull {
                        it.players.contains(target.uniqueId)
                    }
                    if (instance != null) {
                        instance.removePlayer(target)
                        target.sendStringAsComponent(sender.asLangText("AdminPlayerBlacklistKicked"))
                    }
                    target.sendStringAsComponent(sender.asLangText("AdminPlayerBlacklistNotifyReason", reason))
                }
            }
        }
    }

    /**
     * /kda blacklist remove <player>
     */
    @CommandBody
    val remove = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.blacklistedPlayers.keys.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val playerName = context["player"]
                val key = playerName.lowercase()
                val removed = KAngelDungeon.blacklistedPlayers.remove(key)
                if (removed != null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerUnbanSuccess", playerName))
                    val target = Bukkit.getPlayerExact(playerName)
                    target?.sendStringAsComponent(sender.asLangText("AdminPlayerUnbanNotify"))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("AdminPlayerUnbanNotFound", playerName))
                }
            }
        }
    }

    /**
     * /kda blacklist list
     */
    @CommandBody
    val list = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val blacklist = KAngelDungeon.blacklistedPlayers
            if (blacklist.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("AdminBlacklistEmpty"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("AdminBlacklistHeader", blacklist.size.toString()))
            for ((name, reason) in blacklist) {
                val target = Bukkit.getPlayerExact(name)
                val online = if (target != null && target.isOnline) sender.asLangText("StatusOnline") else sender.asLangText("StatusOffline")
                sender.sendStringAsComponent(sender.asLangText("AdminBlacklistEntry", name, online, reason))
            }
        }
    }
}

/**
 * /kda meta 子命令
 */
@CommandHeader(
    name = "kangeldungeonmeta",
    aliases = [],
    permission = "kangeldungeon.command.admin",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object AdminMetaCommand {

    /**
     * /kda meta <uuid> list
     */
    @CommandBody
    val list = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                    return@execute
                }
                val metaData = instance.meta
                val keys = metaData.keys()
                if (keys.isEmpty()) {
                    sender.sendStringAsComponent(sender.asLangText("AdminMetaNoData"))
                    return@execute
                }
                sender.sendStringAsComponent(sender.asLangText("AdminMetaHeader", instance.templateName, keys.size.toString()))
                for (key in keys.sorted()) {
                    val value = metaData.get(key)
                    sender.sendStringAsComponent(sender.asLangText("AdminMetaEntry", key, value.toString()))
                }
            }
        }
    }

    /**
     * /kda meta <uuid> get <key>
     */
    @CommandBody
    val get = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            dynamic("key") {
                execute<CommandSender> { sender, context, argument ->
                    val uuidStr = context["uuid"]
                    val key = context["key"]
                    val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                    if (uuid == null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                        return@execute
                    }
                    val instance = KAngelDungeon.dungeonInstances[uuid]
                    if (instance == null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                        return@execute
                    }
                    val value = instance.meta.get(key)
                    sender.sendStringAsComponent(sender.asLangText("AdminMetaGetValue", instance.templateName, key, value.toString()))
                }
            }
        }
    }

    /**
     * /kda meta <uuid> set <key> <value>
     */
    @CommandBody
    val set = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            dynamic("key") {
                dynamic("value") {
                    execute<CommandSender> { sender, context, argument ->
                        val uuidStr = context["uuid"]
                        val key = context["key"]
                        val value = context["value"]
                        val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                        if (uuid == null) {
                            sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                            return@execute
                        }
                        val instance = KAngelDungeon.dungeonInstances[uuid]
                        if (instance == null) {
                            sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                            return@execute
                        }
                        val parsedValue: Any = value.toIntOrNull() ?: value.toDoubleOrNull() ?: value
                        instance.meta.set(key, parsedValue)
                        sender.sendStringAsComponent(sender.asLangText("AdminMetaSetSuccess", instance.templateName, key, parsedValue.toString()))
                    }
                }
            }
        }
    }

    /**
     * /kda meta <uuid> add <key> <value>
     */
    @CommandBody
    val add = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            dynamic("key") {
                dynamic("value") {
                    execute<CommandSender> { sender, context, argument ->
                        val uuidStr = context["uuid"]
                        val key = context["key"]
                        val valueStr = context["value"]
                        val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                        if (uuid == null) {
                            sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                            return@execute
                        }
                        val instance = KAngelDungeon.dungeonInstances[uuid]
                        if (instance == null) {
                            sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                            return@execute
                        }
                        val value: Any = valueStr.toIntOrNull() ?: valueStr.toDoubleOrNull() ?: valueStr
                        instance.meta.add(key, value)
                        sender.sendStringAsComponent(sender.asLangText("AdminMetaAddSuccess", instance.templateName, key, value.toString(), instance.meta.get(key).toString()))
                    }
                }
            }
        }
    }

    /**
     * /kda meta <uuid> delete <key>
     */
    @CommandBody
    val delete = subCommand {
        dynamic("uuid") {
            suggestion<CommandSender> { _, _ ->
                KAngelDungeon.dungeonInstances.map { it.key.toString() }.toList()
            }
            dynamic("key") {
                execute<CommandSender> { sender, context, argument ->
                    val uuidStr = context["uuid"]
                    val key = context["key"]
                    val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                    if (uuid == null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInvalidUUID", uuidStr))
                        return@execute
                    }
                    val instance = KAngelDungeon.dungeonInstances[uuid]
                    if (instance == null) {
                        sender.sendStringAsComponent(sender.asLangText("AdminInstanceNotFound", uuidStr))
                        return@execute
                    }
                    val oldValue = instance.meta.get(key)
                    instance.meta.remove(key)
                    sender.sendStringAsComponent(sender.asLangText("AdminMetaDeleteSuccess", instance.templateName, key, oldValue.toString()))
                }
            }
        }
    }
}

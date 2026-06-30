package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.team.TeamManager
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

@CommandHeader(
    name = "kangeldungeonparty",
    aliases = ["kdparty", "party", "kdp"],
    permission = "kangeldungeon.command.party",
    description = "Party management commands.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.TRUE,
    newParser = false,
)
object PartyCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    /**
     * /kdparty create - 创建队伍
     */
    @CommandBody
    val create = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            if (!TeamManager.isEnabled) {
                sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                return@execute
            }
            val team = TeamManager.createTeam(player)
            if (team != null) {
                sender.sendStringAsComponent(sender.asLangText("PartyCreateSuccess"))
            } else {
                sender.sendStringAsComponent(sender.asLangText("PartyCreateFailed"))
            }
        }
    }

    /**
     * /kdparty invite <player> - 邀请玩家
     */
    @CommandBody
    val invite = subCommand {
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: return@execute
                val target = context.player("player").castSafely<Player>() ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                    return@execute
                }
                if (!TeamManager.isEnabled) {
                    sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                    return@execute
                }
                val team = TeamManager.getTeam(player.uniqueId)
                if (team == null) {
                    sender.sendStringAsComponent(sender.asLangText("PartyNotInTeam"))
                    return@execute
                }
                if (!team.isLeader(player.uniqueId)) {
                    sender.sendStringAsComponent(sender.asLangText("PartyLeaderOnlyCommand"))
                    return@execute
                }
                if (player.uniqueId == target.uniqueId) {
                    sender.sendStringAsComponent(sender.asLangText("PartyInviteSelf"))
                    return@execute
                }
                val success = TeamManager.sendInvite(team.teamId, player, target)
                if (success) {
                    player.sendStringAsComponent(sender.asLangText("PartyInviteSent", target.name))
                    target.sendStringAsComponent(sender.asLangText("PartyInviteReceived", player.name, team.teamId.toString().take(8)))
                } else {
                    player.sendStringAsComponent(sender.asLangText("PartyTargetInTeam"))
                }
            }
        }
    }

    /**
     * /kdparty join <teamId> - 接受邀请加入队伍
     */
    @CommandBody
    val join = subCommand {
        dynamic("teamId") {
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@execute
                }
                if (!TeamManager.isEnabled) {
                    sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                    return@execute
                }
                if (TeamManager.getTeam(player.uniqueId) != null) {
                    sender.sendStringAsComponent(sender.asLangText("PartyAlreadyInTeam"))
                    return@execute
                }
                val teamIdStr = context["teamId"]
                val teamId = try { java.util.UUID.fromString(teamIdStr) } catch (e: Exception) { null }
                if (teamId == null) {
                    sender.sendStringAsComponent(sender.asLangText("PartyInvalidId", teamIdStr))
                    return@execute
                }
                val invite = TeamManager.getInvite(player.uniqueId, teamId)
                if (invite == null) {
                    sender.sendStringAsComponent(sender.asLangText("PartyNoInviteFromTeam"))
                    return@execute
                }
                if (invite.isExpired) {
                    TeamManager.removeInvite(teamId, player.uniqueId)
                    sender.sendStringAsComponent(sender.asLangText("PartyInviteExpired"))
                    return@execute
                }
                val success = TeamManager.acceptInvite(player, teamId)
                if (success) {
                    sender.sendStringAsComponent(sender.asLangText("PartyJoinSuccess"))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("PartyJoinFailed"))
                }
            }
        }
    }

    /**
     * /kdparty leave - 离开队伍
     */
    @CommandBody
    val leave = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            if (!TeamManager.isEnabled) {
                sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                return@execute
            }
            val team = TeamManager.getTeam(player.uniqueId)
            if (team == null) {
                sender.sendStringAsComponent(sender.asLangText("PartyNotInTeam"))
                return@execute
            }
            if (team.isLeader(player.uniqueId)) {
                sender.sendStringAsComponent(sender.asLangText("PartyLeaveFailedLeader"))
                return@execute
            }
            val success = TeamManager.removeMember(team.teamId, player, player)
            if (success) {
                sender.sendStringAsComponent(sender.asLangText("PartyLeaveSuccess"))
                // Notify party members
                for (memberId in team.members) {
                    val member = Bukkit.getPlayer(memberId)
                    member?.sendStringAsComponent(member.asLangText("PartyLeaveNotification", player.name))
                }
            }
        }
    }

    /**
     * /kdparty kick <player> - 踢出队员
     */
    @CommandBody
    val kick = subCommand {
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: return@execute
                val target = context.player("player").castSafely<Player>() ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                    return@execute
                }
                if (!TeamManager.isEnabled) {
                    sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                    return@execute
                }
                if (player.uniqueId == target.uniqueId) {
                    sender.sendStringAsComponent(sender.asLangText("PartyKickSelf"))
                    return@execute
                }
                val team = TeamManager.getTeam(player.uniqueId)
                if (team == null || !team.isLeader(player.uniqueId)) {
                    sender.sendStringAsComponent(sender.asLangText("PartyLeaderOnlyCommand"))
                    return@execute
                }
                if (!team.isMember(target.uniqueId)) {
                    sender.sendStringAsComponent(sender.asLangText("PartyTargetNotInTeam"))
                    return@execute
                }
                val success = TeamManager.kickMember(team.teamId, target, player)
                if (success) {
                    player.sendStringAsComponent(sender.asLangText("PartyKickSuccess", target.name))
                    target.sendStringAsComponent(sender.asLangText("PartyKicked"))
                }
            }
        }
    }

    /**
     * /kdparty transfer <player> - 转让队长
     */
    @CommandBody
    val transfer = subCommand {
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: return@execute
                val target = context.player("player").castSafely<Player>() ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                    return@execute
                }
                if (!TeamManager.isEnabled) {
                    sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                    return@execute
                }
                if (player.uniqueId == target.uniqueId) {
                    sender.sendStringAsComponent(sender.asLangText("PartyTransferSelf"))
                    return@execute
                }
                val team = TeamManager.getTeam(player.uniqueId)
                if (team == null || !team.isLeader(player.uniqueId)) {
                    sender.sendStringAsComponent(sender.asLangText("PartyLeaderOnlyCommand"))
                    return@execute
                }
                if (!team.isMember(target.uniqueId)) {
                    sender.sendStringAsComponent(sender.asLangText("PartyTransferNotMember"))
                    return@execute
                }
                val success = TeamManager.transferLeader(team.teamId, target, player)
                if (success) {
                    player.sendStringAsComponent(sender.asLangText("PartyTransferSuccess", target.name))
                    target.sendStringAsComponent(sender.asLangText("PartyTransferReceived"))
                }
            }
        }
    }

    /**
     * /kdparty disband - 解散队伍
     */
    @CommandBody
    val disband = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            if (!TeamManager.isEnabled) {
                sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                return@execute
            }
            val team = TeamManager.getTeam(player.uniqueId)
            if (team == null) {
                sender.sendStringAsComponent(sender.asLangText("PartyNotInTeam"))
                return@execute
            }
            if (!team.isLeader(player.uniqueId)) {
                sender.sendStringAsComponent(sender.asLangText("PartyLeaderOnlyCommand"))
                return@execute
            }
            val success = TeamManager.disbandTeam(team.teamId, player)
            if (success) {
                sender.sendStringAsComponent(sender.asLangText("PartyDisbandSuccess"))
            } else {
                sender.sendStringAsComponent(sender.asLangText("PartyDisbandFailed"))
            }
        }
    }

    /**
     * /kdparty info - 查看队伍信息
     */
    @CommandBody
    val info = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            if (!TeamManager.isEnabled) {
                sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                return@execute
            }
            val team = TeamManager.getTeam(player.uniqueId)
            if (team == null) {
                sender.sendStringAsComponent(sender.asLangText("PartyNotInTeam"))
                return@execute
            }
            val leaderName = Bukkit.getOfflinePlayer(team.leader).name ?: "Unknown"
            sender.sendStringAsComponent(sender.asLangText("PartyInfoHeader"))
            sender.sendStringAsComponent(sender.asLangText("PartyInfoLeader", leaderName))
            sender.sendStringAsComponent(sender.asLangText("PartyInfoMembers", team.members.size.toString()))
            for (memberId in team.members) {
                val memberName = Bukkit.getOfflinePlayer(memberId).name ?: "Unknown"
                val member = Bukkit.getPlayer(memberId)
                if (member != null && member.isOnline) {
                    sender.sendStringAsComponent(sender.asLangText("PartyInfoMember", memberName))
                } else {
                    sender.sendStringAsComponent(sender.asLangText("PartyInfoOffline", memberName))
                }
            }
        }
    }

    /**
     * /kdparty invites - 查看待处理的邀请
     */
    @CommandBody
    val invites = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            if (!TeamManager.isEnabled) {
                sender.sendStringAsComponent(sender.asLangText("PartySystemDisabled"))
                return@execute
            }
            val pending = TeamManager.getInvitesForPlayer(player.uniqueId)
            if (pending.isEmpty()) {
                sender.sendStringAsComponent(sender.asLangText("PartyNoInvites"))
                return@execute
            }
            sender.sendStringAsComponent(sender.asLangText("PartyInvitesHeader"))
            for (invite in pending) {
                val inviterName = Bukkit.getOfflinePlayer(invite.inviter).name ?: "Unknown"
                val remaining = (invite.expiresAt - System.currentTimeMillis()) / 1000
                sender.sendStringAsComponent(sender.asLangText("PartyInviteEntry", inviterName, invite.teamId.toString().take(8), remaining.toString()))
            }
        }
    }
}

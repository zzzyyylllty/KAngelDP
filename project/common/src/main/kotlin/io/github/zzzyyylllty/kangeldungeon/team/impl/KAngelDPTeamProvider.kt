package io.github.zzzyyylllty.kangeldungeon.team.impl

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.event.*
import io.github.zzzyyylllty.kangeldungeon.team.Team
import io.github.zzzyyylllty.kangeldungeon.team.TeamInvite
import io.github.zzzyyylllty.kangeldungeon.team.TeamProvider
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class KAngelDPTeamProvider : TeamProvider {

    override val providerName = "kangeldp"

    override val maxTeamSize: Int
        get() = KAngelDungeon.config.getInt("party.max-size", 5)

    override val inviteTimeoutSeconds: Long
        get() = KAngelDungeon.config.getLong("party.invite-timeout", 60)

    private val teams = ConcurrentHashMap<UUID, Team>()
    private val playerTeamMap = ConcurrentHashMap<UUID, UUID>()
    private val invites = ConcurrentHashMap<UUID, MutableMap<UUID, TeamInvite>>()

    // ===== Query =====

    override fun getTeam(player: UUID): Team? {
        val teamId = playerTeamMap[player] ?: return null
        return teams[teamId]
    }

    override fun getTeamById(teamId: UUID): Team? = teams[teamId]

    // ===== Mutations =====

    override fun createTeam(leader: Player): Team? {
        val teamId = UUID.randomUUID()
        val members = ConcurrentHashMap.newKeySet<UUID>().apply { add(leader.uniqueId) }
        val team = Team(teamId = teamId, leader = leader.uniqueId, members = members)

        // 先写入 teams，再原子写入 playerTeamMap，
        // 避免 getTeam() 查到 playerTeamMap 却查不到 teams 的 TOCTOU 窗口
        teams[teamId] = team

        val existing = playerTeamMap.putIfAbsent(leader.uniqueId, teamId)
        if (existing != null) {
            teams.remove(teamId)
            return null
        }

        val preEvent = TeamCreatePreEvent(leader)
        preEvent.call()
        if (preEvent.isCancelled) {
            playerTeamMap.remove(leader.uniqueId, teamId)
            teams.remove(teamId)
            return null
        }

        TeamCreatePostEvent(team, leader).call()
        return team
    }

    override fun disbandTeam(teamId: UUID, executor: Player): Boolean {
        val team = teams[teamId] ?: return false
        if (team.leader != executor.uniqueId) return false

        val preEvent = TeamDisbandPreEvent(team, executor)
        preEvent.call()
        if (preEvent.isCancelled) return false

        // 先移除队伍再清理 player 映射，防止 addMember 在中间窗口写入
        teams.remove(teamId)
        team.members.forEach { playerTeamMap.remove(it) }
        invites.remove(teamId)

        TeamDisbandPostEvent(team, executor).call()
        return true
    }

    override fun addMember(teamId: UUID, player: Player, executor: Player): Boolean {
        val team = teams[teamId] ?: return false
        if (team.members.size >= maxTeamSize) return false

        // 原子操作检查玩家是否已有队伍
        val existing = playerTeamMap.putIfAbsent(player.uniqueId, teamId)
        if (existing != null) return false

        val preEvent = TeamJoinPreEvent(team, player, executor)
        preEvent.call()
        if (preEvent.isCancelled) {
            playerTeamMap.remove(player.uniqueId, teamId)
            return false
        }

        team.members.add(player.uniqueId)

        TeamJoinPostEvent(team, player, executor).call()
        return true
    }

    override fun removeMember(teamId: UUID, player: Player, executor: Player): Boolean {
        val team = teams[teamId] ?: return false
        if (player.uniqueId == team.leader) return false

        val preEvent = TeamLeavePreEvent(team, player, executor)
        preEvent.call()
        if (preEvent.isCancelled) return false

        team.members.remove(player.uniqueId)
        playerTeamMap.remove(player.uniqueId)

        TeamLeavePostEvent(team, player, executor).call()

        if (team.members.isEmpty()) {
            teams.remove(teamId)
            invites.remove(teamId)
        }
        return true
    }

    override fun transferLeader(teamId: UUID, newLeader: Player, executor: Player): Boolean {
        val team = teams[teamId] ?: return false
        if (team.leader != executor.uniqueId) return false
        if (newLeader.uniqueId !in team.members) return false
        if (newLeader.uniqueId == executor.uniqueId) return false

        val preEvent = TeamTransferPreEvent(team, newLeader, executor)
        preEvent.call()
        if (preEvent.isCancelled) return false

        team.leader = newLeader.uniqueId

        TeamTransferPostEvent(team, newLeader, executor).call()
        return true
    }

    override fun kickMember(teamId: UUID, target: Player, executor: Player): Boolean {
        val team = teams[teamId] ?: return false
        if (team.leader != executor.uniqueId) return false
        if (target.uniqueId == team.leader) return false
        if (target.uniqueId !in team.members) return false

        val preEvent = TeamKickPreEvent(team, target, executor)
        preEvent.call()
        if (preEvent.isCancelled) return false

        team.members.remove(target.uniqueId)
        playerTeamMap.remove(target.uniqueId)

        TeamKickPostEvent(team, target, executor).call()

        if (team.members.isEmpty()) {
            teams.remove(teamId)
            invites.remove(teamId)
        }
        return true
    }

    // ===== Invites =====

    override fun sendInvite(teamId: UUID, inviter: Player, invited: Player): Boolean {
        val team = teams[teamId] ?: return false
        if (team.leader != inviter.uniqueId) return false
        if (invited.uniqueId in team.members) return false
        if (playerTeamMap.containsKey(invited.uniqueId)) return false
        if (team.members.size >= maxTeamSize) return false

        val event = TeamInviteEvent(team, inviter, invited)
        event.call()
        if (event.isCancelled) return false

        val teamInvites = invites.getOrPut(teamId) { ConcurrentHashMap() }
        teamInvites[invited.uniqueId] = TeamInvite(
            teamId = teamId,
            inviter = inviter.uniqueId,
            invited = invited.uniqueId,
            expiresAt = System.currentTimeMillis() + inviteTimeoutSeconds * 1000
        )
        return true
    }

    override fun acceptInvite(player: Player, teamId: UUID): Boolean {
        val invite = invites[teamId]?.get(player.uniqueId) ?: return false
        if (invite.isExpired) {
            invites[teamId]?.remove(player.uniqueId)
            return false
        }

        val team = teams[teamId] ?: return false
        if (team.members.size >= maxTeamSize) return false

        invites[teamId]?.remove(player.uniqueId)
        return addMember(teamId, player, player)
    }

    override fun getInvite(player: UUID, teamId: UUID): TeamInvite? {
        return invites[teamId]?.get(player)
    }

    override fun getInvitesForPlayer(player: UUID): List<TeamInvite> {
        sweepExpiredInvites()
        return invites.values.mapNotNull { it[player] }
    }

    private fun sweepExpiredInvites() {
        val now = System.currentTimeMillis()
        for ((teamId, teamInvites) in invites) {
            teamInvites.entries.removeIf { (_, invite) -> invite.expiresAt < now }
            if (teamInvites.isEmpty()) invites.remove(teamId)
        }
    }

    override fun removeInvite(teamId: UUID, player: UUID) {
        invites[teamId]?.remove(player)
    }

    // ===== Lifecycle =====

    override fun onPlayerDisconnect(player: Player) {
        // 清理玩家相关的所有邀请（作为受邀者或邀请者）
        for ((teamId, teamInvites) in invites) {
            teamInvites.remove(player.uniqueId)
            teamInvites.entries.removeIf { (_, invite) -> invite.inviter == player.uniqueId }
            if (teamInvites.isEmpty()) invites.remove(teamId)
        }
    }

    override fun onPlayerReconnect(player: Player) {}
}

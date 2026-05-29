package io.github.zzzyyylllty.kangeldungeon.team

import org.bukkit.entity.Player
import java.util.UUID

interface TeamProvider {

    val providerName: String
    val maxTeamSize: Int

    // === Query ===

    fun getTeam(player: UUID): Team?
    fun getTeamById(teamId: UUID): Team?

    // === Mutations ===

    fun createTeam(leader: Player): Team?
    fun disbandTeam(teamId: UUID, executor: Player): Boolean
    fun addMember(teamId: UUID, player: Player, executor: Player): Boolean
    fun removeMember(teamId: UUID, player: Player, executor: Player): Boolean
    fun kickMember(teamId: UUID, target: Player, executor: Player): Boolean
    fun transferLeader(teamId: UUID, newLeader: Player, executor: Player): Boolean

    // === Invites ===

    fun sendInvite(teamId: UUID, inviter: Player, invited: Player): Boolean
    fun acceptInvite(player: Player, teamId: UUID): Boolean
    fun getInvite(player: UUID, teamId: UUID): TeamInvite?
    fun getInvitesForPlayer(player: UUID): List<TeamInvite>
    fun removeInvite(teamId: UUID, player: UUID)

    val inviteTimeoutSeconds: Long

    // === Lifecycle ===

    fun onPlayerDisconnect(player: Player)
    fun onPlayerReconnect(player: Player)
}

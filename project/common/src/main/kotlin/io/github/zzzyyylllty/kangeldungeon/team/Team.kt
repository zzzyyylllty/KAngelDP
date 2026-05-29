package io.github.zzzyyylllty.kangeldungeon.team

import java.util.UUID

/**
 * Represents a party/team.
 */
data class Team(
    val teamId: UUID,
    var leader: UUID,
    val members: MutableSet<UUID>,
    val createdAt: Long = System.currentTimeMillis()
) {
    val size: Int get() = members.size
    fun isLeader(player: UUID): Boolean = leader == player
    fun isMember(player: UUID): Boolean = player in members
}

/**
 * Represents a pending invitation.
 */
data class TeamInvite(
    val teamId: UUID,
    val inviter: UUID,
    val invited: UUID,
    val expiresAt: Long
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
}

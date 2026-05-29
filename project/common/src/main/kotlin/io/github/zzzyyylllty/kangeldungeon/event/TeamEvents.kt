package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.team.Team
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class TeamCreatePreEvent(
    val leader: Player,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class TeamCreatePostEvent(
    val team: Team,
    val leader: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class TeamDisbandPreEvent(
    val team: Team,
    val executor: Player,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class TeamDisbandPostEvent(
    val team: Team,
    val executor: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class TeamInviteEvent(
    val team: Team,
    val inviter: Player,
    val invited: Player,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class TeamJoinPreEvent(
    val team: Team,
    val player: Player,
    val executor: Player,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class TeamJoinPostEvent(
    val team: Team,
    val player: Player,
    val executor: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class TeamLeavePreEvent(
    val team: Team,
    val player: Player,
    val executor: Player,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class TeamLeavePostEvent(
    val team: Team,
    val player: Player,
    val executor: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class TeamKickPreEvent(
    val team: Team,
    val target: Player,
    val executor: Player,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class TeamKickPostEvent(
    val team: Team,
    val target: Player,
    val executor: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

class TeamTransferPreEvent(
    val team: Team,
    val newLeader: Player,
    val oldLeader: Player,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

class TeamTransferPostEvent(
    val team: Team,
    val newLeader: Player,
    val oldLeader: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

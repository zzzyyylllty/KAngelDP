package io.github.zzzyyylllty.kangeldungeon.team

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.logger.infoL
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.team.impl.KAngelDPTeamProvider
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID

object TeamManager {

    @Volatile
    private var provider: TeamProvider? = null

    val isEnabled: Boolean get() = provider != null

    val activeProviderName: String get() = provider?.providerName ?: "none"

    fun registerProvider(newProvider: TeamProvider) {
        if (provider != null) {
            warningL("TeamProviderOverride", provider!!.providerName, newProvider.providerName)
        }
        provider = newProvider
        infoL("TeamProviderRegistered", newProvider.providerName)
    }

    fun unregisterProvider() {
        provider?.let {
            infoL("TeamProviderUnregistered", it.providerName)
        }
        provider = null
    }

    fun initialize() {
        val mode = KAngelDungeon.config.getString("party.mode", "none") ?: "none"
        when (mode.lowercase()) {
            "kangeldp" -> {
                registerProvider(KAngelDPTeamProvider())
            }
            "none" -> {
                infoL("PartyModeDisabled")
            }
            else -> {
                infoL("PartyModeExternal", mode)
            }
        }
    }

    fun getProvider(): TeamProvider? = provider

    // === Convenience Delegates ===

    fun getTeam(player: UUID): Team? = provider?.getTeam(player)
    fun getTeamById(teamId: UUID): Team? = provider?.getTeamById(teamId)
    fun createTeam(leader: Player): Team? = provider?.createTeam(leader)
    fun disbandTeam(teamId: UUID, executor: Player): Boolean =
        provider?.disbandTeam(teamId, executor) ?: false
    fun addMember(teamId: UUID, player: Player, executor: Player): Boolean =
        provider?.addMember(teamId, player, executor) ?: false
    fun removeMember(teamId: UUID, player: Player, executor: Player): Boolean =
        provider?.removeMember(teamId, player, executor) ?: false
    fun kickMember(teamId: UUID, target: Player, executor: Player): Boolean =
        provider?.kickMember(teamId, target, executor) ?: false
    fun transferLeader(teamId: UUID, newLeader: Player, executor: Player): Boolean =
        provider?.transferLeader(teamId, newLeader, executor) ?: false
    fun sendInvite(teamId: UUID, inviter: Player, invited: Player): Boolean =
        provider?.sendInvite(teamId, inviter, invited) ?: false
    fun acceptInvite(player: Player, teamId: UUID): Boolean =
        provider?.acceptInvite(player, teamId) ?: false
    fun getInvite(player: UUID, teamId: UUID): TeamInvite? =
        provider?.getInvite(player, teamId)
    fun getInvitesForPlayer(player: UUID): List<TeamInvite> =
        provider?.getInvitesForPlayer(player) ?: emptyList()
    fun removeInvite(teamId: UUID, player: UUID) =
        provider?.removeInvite(teamId, player)

    // === Player Lifecycle ===

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        provider?.onPlayerDisconnect(event.player)
    }

    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        provider?.onPlayerReconnect(event.player)
    }
}

package io.github.zzzyyylllty.kangeldungeon

import io.github.zzzyyylllty.kangeldungeon.team.TeamManager
import io.github.zzzyyylllty.kangeldungeon.team.TeamProvider

interface KAngelDungeonAPI {

    val teamManager: TeamManager

    fun registerTeamProvider(provider: TeamProvider)

    fun getTeamProvider(): TeamProvider?

    companion object {
        val instance: KAngelDungeonAPI get() = KAngelDungeon
    }
}

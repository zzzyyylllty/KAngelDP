package io.github.zzzyyylllty.kangeldungeon

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonTemplate
import io.github.zzzyyylllty.kangeldungeon.data.LootChestConfig
import io.github.zzzyyylllty.kangeldungeon.team.TeamManager
import io.github.zzzyyylllty.kangeldungeon.team.TeamProvider
import io.github.zzzyyylllty.kangeldungeon.util.stats.PlayerStatsManager
import java.util.UUID

interface KAngelDungeonAPI {

    val teamManager: TeamManager

    fun registerTeamProvider(provider: TeamProvider)

    fun getTeamProvider(): TeamProvider?

    // ===== 拓展功能 API =====

    /** 获取玩家跨地牢统计 */
    fun getPlayerStats(playerUUID: UUID): PlayerStatsManager.PlayerStats

    /** 检查玩家是否在旁观地牢 */
    fun isSpectating(playerUUID: UUID): Boolean

    /** 获取活跃的地牢实例列表 */
    fun getActiveInstances(): Collection<DungeonInstance>

    /** 获取地牢模板 */
    fun getDungeonTemplate(name: String): DungeonTemplate?

    /** 查找玩家所在的地牢实例 */
    fun findPlayerInstance(playerUUID: UUID): DungeonInstance?

    /** 获取战利品箱配置 */
    fun getLootChestConfig(id: String): LootChestConfig?

    companion object {
        val instance: KAngelDungeonAPI get() = KAngelDungeon
    }
}

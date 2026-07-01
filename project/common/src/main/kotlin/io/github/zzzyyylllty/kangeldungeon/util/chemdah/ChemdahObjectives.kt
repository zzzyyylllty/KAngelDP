package io.github.zzzyyylllty.kangeldungeon.util.chemdah

import ink.ptms.chemdah.core.quest.QuestLoader.register
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import io.github.zzzyyylllty.kangeldungeon.util.chemdah.obj.*

/**
 * Chemdah 任务系统挂钩 - 注册所有自定义任务目标
 *
 * 在 Chemdah 加载后（LifeCycle.ENABLE）调用 register() 注册所有目标。
 * 首次加载时扫描 @Dependency 注解，仅在依赖存在时注册。
 */
object ChemdahObjectives {

    @JvmStatic
    fun register() {
        // ===== 地牢目标 =====
        DungeonEnterObjective.register()
        DungeonCompleteObjective.register()
        DungeonFailObjective.register()
        DungeonMobKillObjective.register()
        DungeonDeathObjective.register()
        DungeonRespawnObjective.register()
        DungeonBossKillObjective.register()
        DungeonSurviveObjective.register()
        DungeonNoDeathObjective.register()
        DungeonRatingObjective.register()
        DungeonChestOpenObjective.register()

        // ===== 实体目标 =====
        KillEntityObjective.register()
        TameEntityObjective.register()
        BreedEntityObjective.register()
        ShearEntityObjective.register()

        // ===== 方块目标 =====
        BlockBreakObjective.register()
        BlockPlaceObjective.register()
        BlockInteractObjective.register()

        // ===== 物品目标 =====
        ItemCraftObjective.register()
        ItemSmeltObjective.register()
        ItemEnchantObjective.register()
        ItemPickupObjective.register()
        ItemConsumeObjective.register()
        ItemFishObjective.register()

        // ===== 玩家目标 =====
        PlayerDamageDealtObjective.register()
        PlayerDamageTakenObjective.register()
        PlayerLevelObjective.register()
        PlayerCommandObjective.register()
        PlayerStatisticObjective.register()

        // ===== 位置目标 =====
        PositionReachObjective.register()
        BiomeEnterObjective.register()
        WorldEnterObjective.register()
    }
}

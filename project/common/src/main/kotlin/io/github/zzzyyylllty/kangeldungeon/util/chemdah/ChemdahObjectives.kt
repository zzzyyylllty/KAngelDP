package io.github.zzzyyylllty.kangeldungeon.util.chemdah

import ink.ptms.chemdah.core.quest.QuestLoader.register
import ink.ptms.chemdah.core.quest.objective.Dependency
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import io.github.zzzyyylllty.kangeldungeon.event.*

/**
 * Chemdah 任务系统挂钩 - 仅在 Chemdah 插件存在时通过反射加载
 */
object ChemdahObjectives {

    fun register() {
        DungeonEnterObjective.register()
        DungeonCompleteObjective.register()
        DungeonFailObjective.register()
        DungeonMobKillObjective.register()
    }

    @Dependency("KAngelDungeon")
    object DungeonEnterObjective : ObjectiveCountableI<DungeonPlayerJoinPostEvent>() {
        override val name = "kangeldp enter"
        override val event = DungeonPlayerJoinPostEvent::class.java

        init {
            handler { it.player }
            addSimpleCondition("dungeon") { data, e ->
                data.asList().any { it.equals(e.instance.templateName, true) }
            }
        }
    }

    @Dependency("KAngelDungeon")
    object DungeonCompleteObjective : ObjectiveCountableI<DungeonPlayerCompleteEvent>() {
        override val name = "kangeldp complete"
        override val event = DungeonPlayerCompleteEvent::class.java

        init {
            handler { it.player }
            addSimpleCondition("dungeon") { data, e ->
                data.asList().any { it.equals(e.instance.templateName, true) }
            }
        }
    }

    @Dependency("KAngelDungeon")
    object DungeonFailObjective : ObjectiveCountableI<DungeonPlayerFailEvent>() {
        override val name = "kangeldp fail"
        override val event = DungeonPlayerFailEvent::class.java

        init {
            handler { it.player }
            addSimpleCondition("dungeon") { data, e ->
                data.asList().any { it.equals(e.instance.templateName, true) }
            }
        }
    }

    @Dependency("KAngelDungeon")
    object DungeonMobKillObjective : ObjectiveCountableI<DungeonMobKillEvent>() {
        override val name = "kangeldp kill"
        override val event = DungeonMobKillEvent::class.java

        init {
            handler { it.player }
            addSimpleCondition("dungeon") { data, e ->
                data.asList().any { it.equals(e.instance.templateName, true) }
            }
            addSimpleCondition("mob-type") { data, e ->
                data.asList().any { it.equals(e.mobType, true) }
            }
            addSimpleCondition("mob-name") { data, e ->
                data.asList().any { it.equals(e.mobName, true) }
            }
            addSimpleCondition("mob-id") { data, e ->
                data.asList().any { it.equals(e.mobId, true) }
            }
            addSimpleCondition("min-level") { data, e ->
                data.toDouble() <= e.level
            }
            addSimpleCondition("max-level") { data, e ->
                data.toDouble() >= e.level
            }
        }
    }
}

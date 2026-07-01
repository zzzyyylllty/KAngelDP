package io.github.zzzyyylllty.kangeldungeon.util.chemdah.obj

import ink.ptms.chemdah.core.PlayerProfile
import ink.ptms.chemdah.core.quest.Task
import ink.ptms.chemdah.core.quest.objective.Dependency
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import io.github.zzzyyylllty.kangeldungeon.event.*
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEvent

// ==================== 地牢进入 ====================

@Dependency("KAngelDungeon")
object DungeonEnterObjective : ObjectiveCountableI<DungeonPlayerJoinPostEvent>() {
    override val name = "kangeldp enter"
    override val event = DungeonPlayerJoinPostEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
        addSimpleCondition("difficulty") { data, e ->
            data.asList().any { it.equals(e.instance.difficultyId, true) }
        }
        addSimpleCondition("party") { data, e ->
            val required = data.asList().firstOrNull()?.toBoolean() ?: false
            if (required) e.instance.players.size > 1 else true
        }
    }
}

// ==================== 地牢通关 ====================

@Dependency("KAngelDungeon")
object DungeonCompleteObjective : ObjectiveCountableI<DungeonPlayerCompleteEvent>() {
    override val name = "kangeldp complete"
    override val event = DungeonPlayerCompleteEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
        addSimpleCondition("difficulty") { data, e ->
            data.asList().any { it.equals(e.instance.difficultyId, true) }
        }
        addSimpleCondition("party") { data, e ->
            val required = data.asList().firstOrNull()?.toBoolean() ?: false
            if (required) e.instance.players.size > 1 else true
        }
        addSimpleCondition("min-players") { data, e ->
            e.instance.players.size >= data.toInt()
        }
        addSimpleCondition("max-deaths") { data, e ->
            (e.instance.playerMeta[e.player.uniqueId]?.get("death") as? Number)?.toInt()
                ?.let { it <= data.toInt() } ?: true
        }
    }
}

// ==================== 地牢失败 ====================

@Dependency("KAngelDungeon")
object DungeonFailObjective : ObjectiveCountableI<DungeonPlayerFailEvent>() {
    override val name = "kangeldp fail"
    override val event = DungeonPlayerFailEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
        addSimpleCondition("difficulty") { data, e ->
            data.asList().any { it.equals(e.instance.difficultyId, true) }
        }
    }
}

// ==================== 地牢击杀怪物 ====================

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

// ==================== 地牢死亡 ====================

@Dependency("KAngelDungeon")
object DungeonDeathObjective : ObjectiveCountableI<DungeonPlayerDeathEvent>() {
    override val name = "kangeldp death"
    override val event = DungeonPlayerDeathEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
    }
}

// ==================== 地牢复活 ====================

@Dependency("KAngelDungeon")
object DungeonRespawnObjective : ObjectiveCountableI<DungeonPlayerRespawnEvent>() {
    override val name = "kangeldp respawn"
    override val event = DungeonPlayerRespawnEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
    }
}

// ==================== 地牢击杀Boss ====================

@Dependency("KAngelDungeon")
object DungeonBossKillObjective : ObjectiveCountableI<DungeonBossKillEvent>() {
    override val name = "kangeldp boss"
    override val event = DungeonBossKillEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
        addSimpleCondition("boss-name") { data, e ->
            data.asList().any { it.equals(e.bossName, true) }
        }
        addSimpleCondition("boss-id") { data, e ->
            data.asList().any { it.equals(e.bossId, true) }
        }
    }
}

// ==================== 地牢生存时间 ====================

@Dependency("KAngelDungeon")
object DungeonSurviveObjective : ObjectiveCountableI<DungeonPlayerCompleteEvent>() {
    override val name = "kangeldp survive"
    override val event = DungeonPlayerCompleteEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
        addSimpleCondition("difficulty") { data, e ->
            data.asList().any { it.equals(e.instance.difficultyId, true) }
        }
        addSimpleCondition("min-seconds") { data, e ->
            val joinTime = e.instance.playerJoinTimes[e.player.uniqueId] ?: return@addSimpleCondition false
            val elapsed = (System.currentTimeMillis() - joinTime) / 1000
            elapsed >= data.toLong()
        }
    }
}

// ==================== 无死亡通关 ====================

@Dependency("KAngelDungeon")
object DungeonNoDeathObjective : ObjectiveCountableI<DungeonPlayerCompleteEvent>() {
    override val name = "kangeldp perfect"
    override val event = DungeonPlayerCompleteEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: DungeonPlayerCompleteEvent): Int {
        val deaths = event.instance.playerMeta[event.player.uniqueId]?.get("death") as? Number ?: 0
        return if (deaths.toInt() == 0) 1 else 0
    }
}

// ==================== 地牢评价通关 ====================

@Dependency("KAngelDungeon")
object DungeonRatingObjective : ObjectiveCountableI<DungeonPlayerCompleteEvent>() {
    override val name = "kangeldp rating"
    override val event = DungeonPlayerCompleteEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("dungeon") { data, e ->
            data.asList().any { it.equals(e.instance.templateName, true) }
        }
        addSimpleCondition("min-rating") { data, e ->
            val deaths = e.instance.playerMeta[e.player.uniqueId]?.get("death") as? Number ?: 0
            val kills = e.instance.playerMeta[e.player.uniqueId]?.get("mob.kill") as? Number ?: 0
            val time = e.instance.completedAt?.let { (it - (e.instance.startedAt ?: it)) / 1000 } ?: 0L
            // 评分算法：基础分 100 - 死亡*10 + 击杀*2 - 耗时因子
            val score = (100 - deaths.toInt() * 10 + kills.toInt() * 2 - (time / 60).toInt()).coerceIn(0, 100)
            score >= data.toInt()
        }
        addConditionVariable("rating") { e ->
            val deaths = e.instance.playerMeta[e.player.uniqueId]?.get("death") as? Number ?: 0
            val kills = e.instance.playerMeta[e.player.uniqueId]?.get("mob.kill") as? Number ?: 0
            val time = e.instance.completedAt?.let { (it - (e.instance.startedAt ?: it)) / 1000 } ?: 0L
            (100 - deaths.toInt() * 10 + kills.toInt() * 2 - (time / 60).toInt()).coerceIn(0, 100).toString()
        }
    }
}

// ==================== 地牢打开战利品箱 ====================

@Dependency("KAngelDungeon")
object DungeonChestOpenObjective : ObjectiveCountableI<org.bukkit.event.inventory.InventoryOpenEvent>() {
    override val name = "kangeldp chest"
    override val event = org.bukkit.event.inventory.InventoryOpenEvent::class.java

    init {
        handler { event ->
            val player = event.player as? org.bukkit.entity.Player ?: return@handler null
            val instance = io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.findPlayerInstance(player.uniqueId) ?: return@handler null
            val invType = event.inventory.type
            if (invType != org.bukkit.event.inventory.InventoryType.CHEST &&
                invType != org.bukkit.event.inventory.InventoryType.BARREL) return@handler null
            player
        }
        addSimpleCondition("dungeon") { data, e ->
            val player = e.player as? org.bukkit.entity.Player ?: return@addSimpleCondition false
            val instance = io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.findPlayerInstance(player.uniqueId) ?: return@addSimpleCondition false
            data.asList().any { it.equals(instance.templateName, true) }
        }
    }
}

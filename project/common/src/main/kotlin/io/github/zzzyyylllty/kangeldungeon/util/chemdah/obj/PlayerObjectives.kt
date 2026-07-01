package io.github.zzzyyylllty.kangeldungeon.util.chemdah.obj

import ink.ptms.chemdah.core.PlayerProfile
import ink.ptms.chemdah.core.quest.Task
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerLevelChangeEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerStatisticIncrementEvent

// ==================== 造成伤害 ====================

object PlayerDamageDealtObjective : ObjectiveCountableI<EntityDamageByEntityEvent>() {
    override val name = "player damage dealt"
    override val event = EntityDamageByEntityEvent::class.java

    init {
        handler { event ->
            if (event.damager !is org.bukkit.entity.Player) return@handler null
            event.damager as? org.bukkit.entity.Player
        }
        addSimpleCondition("target-type") { data, e ->
            data.asList().any { it.equals(e.entityType.name, true) }
        }
        addSimpleCondition("damage-cause") { data, e ->
            data.asList().any { it.equals(e.cause.name, true) }
        }
        addSimpleCondition("min-damage") { data, e ->
            e.finalDamage >= data.toDouble()
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.entity.world.name, true) }
        }
        addSimpleCondition("held-item") { data, e ->
            val player = e.damager as? org.bukkit.entity.Player ?: return@addSimpleCondition false
            val item = player.inventory.itemInMainHand
            data.asList().any { it.equals(item.type.name, true) }
        }
        addConditionVariable("target-type") { e -> e.entityType.name }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: EntityDamageByEntityEvent): Int = event.finalDamage.toInt().coerceAtLeast(1)
}

// ==================== 受到伤害 ====================

object PlayerDamageTakenObjective : ObjectiveCountableI<EntityDamageEvent>() {
    override val name = "player damage taken"
    override val event = EntityDamageEvent::class.java

    init {
        handler { event ->
            if (event.entity !is org.bukkit.entity.Player) return@handler null
            event.entity as? org.bukkit.entity.Player
        }
        addSimpleCondition("damage-cause") { data, e ->
            data.asList().any { it.equals(e.cause.name, true) }
        }
        addSimpleCondition("min-damage") { data, e ->
            e.finalDamage >= data.toDouble()
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.entity.world.name, true) }
        }
        addSimpleCondition("attacker-type") { data, e ->
            val damager = if (e is EntityDamageByEntityEvent) e.damager else null
            damager != null && data.asList().any { it.equals(damager.type.name, true) }
        }
        addConditionVariable("damage-cause") { e -> e.cause.name }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: EntityDamageEvent): Int = event.finalDamage.toInt().coerceAtLeast(1)
}

// ==================== 达到等级 ====================

object PlayerLevelObjective : ObjectiveCountableI<PlayerLevelChangeEvent>() {
    override val name = "player level"
    override val event = PlayerLevelChangeEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("min-level") { data, e ->
            e.newLevel >= data.toInt()
        }
        addSimpleCondition("max-level") { data, e ->
            e.newLevel <= data.toInt()
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addConditionVariable("new-level") { e -> e.newLevel.toString() }
        addConditionVariable("old-level") { e -> e.oldLevel.toString() }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: PlayerLevelChangeEvent): Int = 1
}

// ==================== 执行指令 ====================

object PlayerCommandObjective : ObjectiveCountableI<PlayerCommandPreprocessEvent>() {
    override val name = "player command"
    override val event = PlayerCommandPreprocessEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("command") { data, e ->
            data.asList().any { e.message.startsWith(it, ignoreCase = true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addConditionVariable("command-used") { e -> e.message.split(" ").first() }
        addConditionVariable("full-command") { e -> e.message }
    }
}

// ==================== 统计里程碑 ====================

object PlayerStatisticObjective : ObjectiveCountableI<PlayerStatisticIncrementEvent>() {
    override val name = "player statistic"
    override val event = PlayerStatisticIncrementEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("statistic") { data, e ->
            data.asList().any { it.equals(e.statistic.name, true) }
        }
        addSimpleCondition("entity-type") { data, e ->
            e.entityType?.let { et -> data.asList().any { it.equals(et.name, true) } } ?: data.asList().isEmpty()
        }
        addSimpleCondition("material") { data, e ->
            e.material?.let { m -> data.asList().any { it.equals(m.name, true) } } ?: data.asList().isEmpty()
        }
        addSimpleCondition("min-value") { data, e ->
            e.newValue >= data.toInt()
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addConditionVariable("statistic-name") { e -> e.statistic.key.key }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: PlayerStatisticIncrementEvent): Int {
        return event.newValue - event.previousValue
    }
}

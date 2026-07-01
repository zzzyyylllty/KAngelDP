package io.github.zzzyyylllty.kangeldungeon.util.chemdah.obj

import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import org.bukkit.entity.*
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityTameEvent
import org.bukkit.event.player.PlayerShearEntityEvent

// ==================== 击杀实体 ====================

object KillEntityObjective : ObjectiveCountableI<EntityDeathEvent>() {
    override val name = "entity kill"
    override val event = EntityDeathEvent::class.java

    init {
        handler { event ->
            val entity = event.entity
            if (entity is Player) return@handler null
            entity.killer
        }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.entityType.name, true) }
        }
        addSimpleCondition("name") { data, e ->
            val name = (e.entity as? org.bukkit.entity.LivingEntity)?.customName()?.toString() ?: e.entity.type.name
            data.asList().any { name.contains(it, ignoreCase = true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.entity.world.name, true) }
        }
        addSimpleCondition("spawn-reason") { data, e ->
            val reason = try { (e.entity as? LivingEntity)?.entitySpawnReason?.name } catch (_: Exception) { null }
            reason != null && data.asList().any { it.equals(reason, true) }
        }
        // 击杀特定实体类型的数量：可通过 goal.amount 控制
        addConditionVariable("killed-type") { e -> e.entityType.name }
        addConditionVariable("killed-name") { e -> (e.entity as? org.bukkit.entity.LivingEntity)?.customName()?.toString() ?: e.entity.type.name }
    }
}

// ==================== 驯服动物 ====================

object TameEntityObjective : ObjectiveCountableI<EntityTameEvent>() {
    override val name = "entity tame"
    override val event = EntityTameEvent::class.java

    init {
        handler { it.owner as? Player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.entityType.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.entity.world.name, true) }
        }
        addConditionVariable("tamed-type") { e -> e.entityType.name }
    }
}

// ==================== 繁殖动物 ====================

object BreedEntityObjective : ObjectiveCountableI<EntityBreedEvent>() {
    override val name = "entity breed"
    override val event = EntityBreedEvent::class.java

    init {
        handler { it.breeder as? Player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.entityType.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.entity.world.name, true) }
        }
        addConditionVariable("bred-type") { e -> e.entityType.name }
    }
}

// ==================== 剪羊毛/实体 ====================

object ShearEntityObjective : ObjectiveCountableI<PlayerShearEntityEvent>() {
    override val name = "entity shear"
    override val event = PlayerShearEntityEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.entity.type.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.entity.world.name, true) }
        }
    }
}

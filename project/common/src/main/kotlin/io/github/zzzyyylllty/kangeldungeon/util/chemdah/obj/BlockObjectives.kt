package io.github.zzzyyylllty.kangeldungeon.util.chemdah.obj

import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import org.bukkit.Material
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

// ==================== 破坏方块 ====================

object BlockBreakObjective : ObjectiveCountableI<BlockBreakEvent>() {
    override val name = "block break"
    override val event = BlockBreakEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.block.type.name, true) }
        }
        addSimpleCondition("material") { data, e ->
            data.asList().any { it.equals(e.block.type.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.block.world.name, true) }
        }
        addSimpleCondition("biome") { data, e ->
            data.asList().any { it.equals(e.block.biome.toString(), true) }
        }
        addSimpleCondition("position") { data, e ->
            data.toPosition().inside(e.block.location)
        }
        addSimpleCondition("held-item") { data, e ->
            val item = e.player.inventory.itemInMainHand
            data.asList().any { it.equals(item.type.name, true) }
        }
        addSimpleCondition("drop-items") { data, e ->
            val drop = data.asList().firstOrNull()?.toBoolean() ?: true
            if (!drop) e.isDropItems = false
            true
        }
        addConditionVariable("broken-type") { e -> e.block.type.name }
        addConditionVariable("broken-world") { e -> e.block.world.name }
    }
}

// ==================== 放置方块 ====================

object BlockPlaceObjective : ObjectiveCountableI<BlockPlaceEvent>() {
    override val name = "block place"
    override val event = BlockPlaceEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.block.type.name, true) }
        }
        addSimpleCondition("material") { data, e ->
            data.asList().any { it.equals(e.block.type.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.block.world.name, true) }
        }
        addSimpleCondition("biome") { data, e ->
            data.asList().any { it.equals(e.block.biome.toString(), true) }
        }
        addSimpleCondition("position") { data, e ->
            data.toPosition().inside(e.block.location)
        }
        addSimpleCondition("against") { data, e ->
            data.asList().any { it.equals(e.blockAgainst.type.name, true) }
        }
        addConditionVariable("placed-type") { e -> e.block.type.name }
    }
}

// ==================== 交互方块 ====================

object BlockInteractObjective : ObjectiveCountableI<PlayerInteractEvent>() {
    override val name = "block interact"
    override val event = PlayerInteractEvent::class.java

    init {
        handler { event ->
            if (event.clickedBlock == null) return@handler null
            event.player
        }
        addSimpleCondition("type") { data, e ->
            e.clickedBlock?.let { data.asList().any { d -> d.equals(it.type.name, true) } } ?: false
        }
        addSimpleCondition("material") { data, e ->
            e.clickedBlock?.let { data.asList().any { d -> d.equals(it.type.name, true) } } ?: false
        }
        addSimpleCondition("action") { data, e ->
            data.asList().any { it.equals(e.action.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            e.clickedBlock?.let { data.asList().any { d -> d.equals(it.world.name, true) } } ?: false
        }
        addSimpleCondition("position") { data, e ->
            e.clickedBlock?.let { data.toPosition().inside(it.location) } ?: false
        }
        addSimpleCondition("held-item") { data, e ->
            val item = e.player.inventory.itemInMainHand
            data.asList().any { it.equals(item.type.name, true) }
        }
    }
}

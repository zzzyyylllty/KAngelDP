package io.github.zzzyyylllty.kangeldungeon.util.chemdah.obj

import ink.ptms.chemdah.core.PlayerProfile
import ink.ptms.chemdah.core.quest.Task
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerChangedWorldEvent

// ==================== 到达位置 ====================

object PositionReachObjective : ObjectiveCountableI<PlayerMoveEvent>() {
    override val name = "position"
    override val event = PlayerMoveEvent::class.java

    init {
        handler { event ->
            val from = event.from
            val to = event.to ?: return@handler null
            if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return@handler null
            event.player
        }
        addSimpleCondition("position") { data, e ->
            data.toPosition().inside(e.player.location)
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addSimpleCondition("biome") { data, e ->
            e.to?.let { data.asList().any { d -> d.equals(it.block.biome.toString(), true) } } ?: false
        }
        addSimpleCondition("min-y") { data, e ->
            e.player.location.blockY >= data.toInt()
        }
        addSimpleCondition("max-y") { data, e ->
            e.player.location.blockY <= data.toInt()
        }
        addConditionVariable("world-name") { e -> e.player.world.name }
        addConditionVariable("biome-name") { e -> e.player.location.block.biome.toString() }
        addConditionVariable("x") { e -> e.player.location.blockX.toString() }
        addConditionVariable("y") { e -> e.player.location.blockY.toString() }
        addConditionVariable("z") { e -> e.player.location.blockZ.toString() }
    }
}

// ==================== 进入生物群系 ====================

object BiomeEnterObjective : ObjectiveCountableI<PlayerMoveEvent>() {
    override val name = "biome"
    override val event = PlayerMoveEvent::class.java

    init {
        handler { event ->
            val from = event.from
            val to = event.to ?: return@handler null
            if (from.blockX == to.blockX && from.blockZ == to.blockZ) return@handler null
            val fromBiome = from.block.biome
            val toBiome = to.block.biome
            if (fromBiome == toBiome) return@handler null
            event.player
        }
        addSimpleCondition("biome") { data, e ->
            e.to?.let { data.asList().any { d -> d.equals(it.block.biome.toString(), true) } } ?: false
        }
        addSimpleCondition("from-biome") { data, e ->
            data.asList().any { it.equals(e.from.block.biome.toString(), true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addConditionVariable("biome-name") { e -> e.to?.block?.biome?.toString() ?: "UNKNOWN" }
    }
}

// ==================== 切换世界 ====================

object WorldEnterObjective : ObjectiveCountableI<PlayerChangedWorldEvent>() {
    override val name = "world"
    override val event = PlayerChangedWorldEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addSimpleCondition("from-world") { data, e ->
            data.asList().any { it.equals(e.from.name, true) }
        }
        addConditionVariable("world-name") { e -> e.player.world.name }
        addConditionVariable("from-world-name") { e -> e.from.name }
    }
}

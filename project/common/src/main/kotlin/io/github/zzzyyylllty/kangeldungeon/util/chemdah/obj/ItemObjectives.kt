package io.github.zzzyyylllty.kangeldungeon.util.chemdah.obj

import ink.ptms.chemdah.core.PlayerProfile
import ink.ptms.chemdah.core.quest.Task
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceExtractEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerFishEvent

// ==================== 合成物品 ====================

object ItemCraftObjective : ObjectiveCountableI<CraftItemEvent>() {
    override val name = "item craft"
    override val event = CraftItemEvent::class.java

    init {
        handler { it.whoClicked as? org.bukkit.entity.Player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.currentItem?.type?.name, true) }
        }
        addSimpleCondition("material") { data, e ->
            data.asList().any { it.equals(e.currentItem?.type?.name, true) }
        }
        addSimpleCondition("recipe") { data, e ->
            data.asList().any { it.equals(e.recipe?.toString(), true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.whoClicked.world.name, true) }
        }
        addConditionVariable("crafted-type") { e -> e.currentItem?.type?.name ?: "UNKNOWN" }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: CraftItemEvent): Int {
        return event.currentItem?.amount ?: 1
    }
}

// ==================== 熔炼/烧制 ====================

object ItemSmeltObjective : ObjectiveCountableI<FurnaceExtractEvent>() {
    override val name = "item smelt"
    override val event = FurnaceExtractEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.itemType.name, true) }
        }
        addSimpleCondition("material") { data, e ->
            data.asList().any { it.equals(e.itemType.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addConditionVariable("smelted-type") { e -> e.itemType.name }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: FurnaceExtractEvent): Int = event.itemAmount
}

// ==================== 附魔物品 ====================

object ItemEnchantObjective : ObjectiveCountableI<EnchantItemEvent>() {
    override val name = "item enchant"
    override val event = EnchantItemEvent::class.java

    init {
        handler { it.enchanter }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.item.type.name, true) }
        }
        addSimpleCondition("material") { data, e ->
            data.asList().any { it.equals(e.item.type.name, true) }
        }
        addSimpleCondition("enchantment") { data, e ->
            e.enchantsToAdd.keys.any { ench -> data.asList().any { it.equals(ench.key.key, true) } }
        }
        addSimpleCondition("min-level") { data, e ->
            e.enchantsToAdd.values.all { it >= data.toInt() }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.enchanter.world.name, true) }
        }
        addConditionVariable("enchanted-type") { e -> e.item.type.name }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: EnchantItemEvent): Int = 1
}

// ==================== 拾取物品 ====================

object ItemPickupObjective : ObjectiveCountableI<PlayerAttemptPickupItemEvent>() {
    override val name = "item pickup"
    override val event = PlayerAttemptPickupItemEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.item.itemStack.type.name, true) }
        }
        addSimpleCondition("material") { data, e ->
            data.asList().any { it.equals(e.item.itemStack.type.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addSimpleCondition("position") { data, e ->
            data.toPosition().inside(e.player.location)
        }
        addConditionVariable("picked-type") { e -> e.item.itemStack.type.name }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: PlayerAttemptPickupItemEvent): Int = event.item.itemStack.amount
}

// ==================== 食用/饮用 ====================

object ItemConsumeObjective : ObjectiveCountableI<PlayerItemConsumeEvent>() {
    override val name = "item consume"
    override val event = PlayerItemConsumeEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("type") { data, e ->
            data.asList().any { it.equals(e.item.type.name, true) }
        }
        addSimpleCondition("material") { data, e ->
            data.asList().any { it.equals(e.item.type.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addConditionVariable("consumed-type") { e -> e.item.type.name }
    }
}

// ==================== 钓鱼 ====================

object ItemFishObjective : ObjectiveCountableI<PlayerFishEvent>() {
    override val name = "item fish"
    override val event = PlayerFishEvent::class.java

    init {
        handler { it.player }
        addSimpleCondition("state") { data, e ->
            data.asList().any { it.equals(e.state.name, true) }
        }
        addSimpleCondition("world") { data, e ->
            data.asList().any { it.equals(e.player.world.name, true) }
        }
        addConditionVariable("fish-state") { e -> e.state.name }
    }

    override fun getCount(profile: PlayerProfile, task: Task, event: PlayerFishEvent): Int {
        return if (event.state == org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH) 1 else 0
    }
}

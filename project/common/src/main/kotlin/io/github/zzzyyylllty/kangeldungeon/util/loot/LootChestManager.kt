package io.github.zzzyyylllty.kangeldungeon.util.loot

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonTemplate
import io.github.zzzyyylllty.kangeldungeon.data.LootChestConfig
import io.github.zzzyyylllty.kangeldungeon.data.LootChestItem
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 战利品箱管理器 — 在地牢创建时自动填充配置的箱子，
 * 支持权重/概率两种随机模式，以及自定义物品 NBT 和附魔。
 */
object LootChestManager {

    private val mm = MiniMessage.miniMessage()

    /** 已 loot 的箱子位置追踪：worldName -> set of "x,y,z" */
    private val lootedChests = ConcurrentHashMap<String, MutableSet<String>>()

    /** LithiumCarbon 是否已加载 */
    private val isLithiumCarbonLoaded by lazy {
        try {
            Class.forName("io.github.zzzyyylllty.lithiumcarbon.LithiumCarbon")
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 初始化地牢的战利品箱（地牢世界创建后调用）
     * 查找所有配置的 loot 位置，填充对应箱子
     */
    fun initializeChests(instance: DungeonInstance, world: World) {
        val template = instance.getTemplate() ?: return
        val worldName = instance.worldName
        val tracked = lootedChests.getOrPut(worldName) { ConcurrentHashMap.newKeySet() }

        // 合并本 dungeon 的 loot 配置 + 全局 loot 配置
        val chestConfigs = getChestConfigs(template.name)

        for ((id, config) in chestConfigs) {
            for (posStr in config.positions) {
                val loc = parseLocation(posStr, world) ?: continue
                val block = loc.block
                val key = "${loc.blockX},${loc.blockY},${loc.blockZ}"

                // 如果该箱子已被 loot 且不刷新，跳过
                if (key in tracked && !config.refresh) continue

                if (block.state is Chest) {
                    fillChest(block, config, id)
                    tracked.add(key)
                    devLog("Filled loot chest '$id' at $key in $worldName")
                } else {
                    devLog("Loot chest '$id' position $posStr is not a chest, skipping")
                }

                // 如果配置了 LithiumCarbon frame-crate，在此位置额外生成展示框物资箱
                val fcId = config.frameCrate
                if (!fcId.isNullOrBlank()) {
                    spawnFrameCrate(loc, fcId, config.frameCrateFacing)
                }
            }
        }
    }

    /**
     * 填充单个箱子
     */
    private fun fillChest(block: Block, config: LootChestConfig, configId: String) {
        val chest = block.state as? Chest ?: return
        val inventory = chest.inventory
        inventory.clear()

        val chosenItems = selectItems(config)

        for (item in chosenItems) {
            val emptySlot = inventory.firstEmpty()
            if (emptySlot == -1) break
            inventory.setItem(emptySlot, createItemStack(item))
        }
    }

    /**
     * 随机选取物品（支持 chance 和 weight 两种模式）
     */
    private fun selectItems(config: LootChestConfig): List<LootChestItem> {
        if (config.items.isEmpty()) return emptyList()
        val count = Random.nextInt(config.minItems, config.maxItems + 1)

        // 检测模式：所有物品都有 chance 则用概率模式，否则用权重模式
        val hasChance = config.items.all { it.chance >= 0 }
        return if (hasChance) {
            // 概率模式：每个物品独立判定
            config.items.filter { Random.nextDouble() < it.chance }.take(count)
        } else {
            // 权重模式：按权重随机抽取（不可重复）
            val pool = config.items.toMutableList()
            val result = mutableListOf<LootChestItem>()
            repeat(count.coerceAtMost(pool.size)) {
                if (pool.isEmpty()) return result
                val currentTotal = pool.sumOf { it.weight.coerceAtLeast(1) }
                var roll = Random.nextInt(currentTotal)
                val idx = pool.indexOfFirst { item ->
                    roll -= item.weight.coerceAtLeast(1)
                    roll < 0
                }.coerceAtLeast(0)
                result.add(pool.removeAt(idx))
            }
            result
        }
    }

    /**
     * 创建物品 ItemStack（支持显示名、Lore、附魔、NBT）
     */
    private fun createItemStack(item: LootChestItem): ItemStack {
        val mat = try {
            Material.valueOf(item.material.uppercase())
        } catch (_: Exception) {
            Material.STONE
        }
        val stack = ItemStack(mat, item.amount.coerceAtLeast(1))
        val meta = stack.itemMeta ?: return stack

        // 显示名
        if (item.displayName != null) {
            meta.displayName(mm.deserialize(item.displayName))
        }
        // Lore
        if (item.lore.isNotEmpty()) {
            meta.lore(item.lore.map { mm.deserialize(it) })
        }
        // 附魔
        for (enchStr in item.enchantments) {
            val parts = enchStr.split(":")
            if (parts.size < 2) continue
            val ench = try {
                org.bukkit.enchantments.Enchantment.getByName(parts[0].uppercase())
            } catch (_: Exception) { null }
            if (ench != null) {
                val level = parts[1].toIntOrNull() ?: 1
                meta.addEnchant(ench, level, true)
            }
        }
        stack.itemMeta = meta

        // NBT（使用反射兼容各 Paper 版本，modifyItemStack 返回序列化字符串需反序列化为 ItemStack）
        if (item.nbt != null) {
            try {
                val serialized = Bukkit.getUnsafe().modifyItemStack(stack, item.nbt)
                val method = ItemStack::class.java.getMethod("deserializeString", String::class.java)
                val result = method.invoke(null, serialized)
                if (result is ItemStack) return result
            } catch (_: Exception) {}
        }

        return stack
    }

    /**
     * 清理地牢的 loot 追踪数据（世界卸载时调用）
     */
    fun clearWorld(worldName: String) {
        lootedChests.remove(worldName)
    }

    /**
     * 通过反射调用 LithiumCarbon FrameCrateManager.spawnFrame 生成展示框物资箱
     * LithiumCarbon 为软依赖，未安装时静默跳过
     */
    private fun spawnFrameCrate(location: Location, configId: String, facing: String) {
        if (!isLithiumCarbonLoaded) return
        try {
            val clazz = Class.forName("io.github.zzzyyylllty.lithiumcarbon.frame.FrameCrateManager")
            val instance = clazz.getField("INSTANCE").get(null)
            clazz.getMethod("spawnFrame", Location::class.java, String::class.java, Player::class.java, String::class.java)
                .invoke(instance, location, configId, null, facing)
            devLog("Spawned LithiumCarbon frame crate '$configId' at ${location.blockX},${location.blockY},${location.blockZ}")
        } catch (e: Exception) {
            devLog("Failed to spawn LithiumCarbon frame crate '$configId': ${e.message}")
        }
    }

    /**
     * 获取地牢配置 + 全局配置的合并 loot 配置表
     */
    private fun getChestConfigs(dungeonName: String): Map<String, LootChestConfig> {
        val combined = LinkedHashMap<String, LootChestConfig>()
        // 全局配置作为基础
        combined.putAll(KAngelDungeon.lootChestConfigs)
        // 本 dungeons 配置覆盖
        KAngelDungeon.dungeonLootChestConfigs[dungeonName]?.let { combined.putAll(it) }
        return combined
    }

    /**
     * 解析 "x y z" 格式坐标为 Location
     */
    private fun parseLocation(posStr: String, world: World): Location? {
        val parts = posStr.split(" ")
        if (parts.size < 3) return null
        val x = parts[0].toIntOrNull() ?: return null
        val y = parts[1].toIntOrNull() ?: return null
        val z = parts[2].toIntOrNull() ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }
}

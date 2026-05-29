package io.github.zzzyyylllty.kangeldungeon.util.obstacle

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.event.*
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.util.Vector
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 障碍物管理器 - 管理所有地牢实例中的障碍物生命周期
 */
object ObstacleManager {

    // 保存的方块状态: worldName -> savedBlocks key -> BlockData
    private val savedBlockStates = ConcurrentHashMap<String, MutableMap<String, BlockData>>()

    // 活跃的障碍物实例
    val activeObstacles = ConcurrentHashMap<String, MutableSet<ObstacleInstance>>()

    // 已开启的障碍物追踪: worldName -> Set<configId>，防止重复开启
    private val openedObstacles = ConcurrentHashMap<String, MutableSet<String>>()

    // 自动关闭定时任务: worldName -> configId -> PlatformExecutor.PlatformTask，用于取消提前手动开启的障碍物
    private val autoCloseTasks = ConcurrentHashMap<String, MutableMap<String, PlatformExecutor.PlatformTask>>()

    /**
     * 准备障碍物（预先保存方块状态、预留位置）
     */
    fun prepareObstacle(instance: DungeonInstance, config: ObstacleConfig): Boolean {
        val world = instance.world ?: return false
        val event = ObstaclePreparePreEvent(instance, config)
        event.call()
        if (event.isCancelled) return false

        try {
            // 保存所有栅栏区域的方块状态
            for ((gateId, gate) in config.obstacles) {
                saveBlocks(world, gate)
            }

            KAngelDungeon.blockRegenMap
                .getOrPut(instance.worldName) { ConcurrentHashMap.newKeySet() }
                .add(instance.uuid)

            // 执行 onPrepare 代理脚本
            runObstacleAgent(config.agent?.onPrepare, instance, config)

            ObstaclePreparePostEvent(instance, config).call()
            instance.meta.add("obstacle.prepare", 1)
            instance.meta.add("obstacle.prepare.${config.id}", 1)
            return true
        } catch (e: Exception) {
            warningL("WarningObstaclePrepareFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 激活障碍物（关闭栅栏门，放置方块阻挡玩家）
     * 支持 openDelaySeconds 延迟激活和 activeDurationSeconds 自动关闭
     */
    fun activateObstacle(instance: DungeonInstance, config: ObstacleConfig): Boolean {
        val world = instance.world ?: return false
        val event = ObstacleActivatePreEvent(instance, config)
        event.call()
        if (event.isCancelled) return false

        val worldKey = instance.worldName

        if (config.openDelaySeconds > 0) {
            // 延迟激活：先记录为 PREPARING 状态，延迟后再放置方块
            val obstacleInstance = ObstacleInstance(
                config = config,
                dungeonInstance = instance,
                state = ObstacleState.PREPARING,
                activatedAt = System.currentTimeMillis()
            )
            activeObstacles
                .getOrPut(worldKey) { ConcurrentHashMap.newKeySet() }
                .add(obstacleInstance)

            submit(delay = (config.openDelaySeconds * 20).toLong()) {
                if (instance.state != DungeonState.ACTIVE) return@submit
                doActivateBlocks(instance, config, obstacleInstance)
            }
            return true
        }

        return doActivateBlocks(instance, config)
    }

    /**
     * 实际放置方块并启动自动关闭定时器
     */
    private fun doActivateBlocks(instance: DungeonInstance, config: ObstacleConfig, existingObstacle: ObstacleInstance? = null): Boolean {
        val world = instance.world ?: return false
        val worldKey = instance.worldName

        try {
            for ((gateId, gate) in config.obstacles) {
                placeGateBlocks(world, gate, config)
            }

            playAnimation(world, config.openingAnimation, config)

            val obstacleInstance = existingObstacle ?: ObstacleInstance(
                config = config,
                dungeonInstance = instance,
                state = ObstacleState.ACTIVE,
                activatedAt = System.currentTimeMillis()
            )
            if (existingObstacle != null) {
                obstacleInstance.state = ObstacleState.ACTIVE
                obstacleInstance.activatedAt = System.currentTimeMillis()
            } else {
                activeObstacles
                    .getOrPut(worldKey) { ConcurrentHashMap.newKeySet() }
                    .add(obstacleInstance)
            }

            runObstacleAgent(config.agent?.onStart, instance, config, mapOf("obstacleInstance" to obstacleInstance))

            ObstacleActivatePostEvent(instance, config).call()
            instance.meta.add("obstacle.activate", 1)
            instance.meta.add("obstacle.activate.${config.id}", 1)

            // 自动关闭定时器
            if (config.activeDurationSeconds > 0) {
                val task = submit(delay = (config.activeDurationSeconds * 20).toLong()) {
                    if (instance.state == DungeonState.ACTIVE) {
                        openObstacle(instance, config)
                    }
                }
                autoCloseTasks
                    .getOrPut(worldKey) { ConcurrentHashMap() }
                    .put(config.id, task)
            }

            return true
        } catch (e: Exception) {
            warningL("WarningObstacleActivateFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 打开障碍物（移除方块恢复通行）
     * 已开启过的障碍物不再重复执行
     */
    fun openObstacle(instance: DungeonInstance, config: ObstacleConfig): Boolean {
        val worldKey = instance.worldName
        val opened = openedObstacles.getOrPut(worldKey) { ConcurrentHashMap.newKeySet() }
        if (!opened.add(config.id)) return false
        val result = openObstacleInternal(instance, config)
        if (!result) opened.remove(config.id)
        return result
    }

    /**
     * 强制打开障碍物（跳过已开启检查）
     */
    fun openObstacleForce(instance: DungeonInstance, config: ObstacleConfig): Boolean {
        val result = openObstacleInternal(instance, config)
        if (result) {
            openedObstacles.getOrPut(instance.worldName) { ConcurrentHashMap.newKeySet() }.add(config.id)
        }
        return result
    }

    /**
     * 打开障碍物内部实现
     */
    private fun openObstacleInternal(instance: DungeonInstance, config: ObstacleConfig): Boolean {
        val world = instance.world ?: return false
        val event = ObstacleOpenPreEvent(instance, config)
        event.call()
        if (event.isCancelled) return false

        // 取消自动关闭定时器
        autoCloseTasks[instance.worldName]?.remove(config.id)?.cancel()

        try {
            for ((gateId, gate) in config.obstacles) {
                removeGateBlocks(world, gate, config)
            }

            // 播放关闭动画（障碍物打开 = 栅栏消失 = 播放closingAnimation）
            playAnimation(world, config.closingAnimation, config)

            // 更新障碍物实例状态（使用 filter+removeAll 避免 ConcurrentHashMap-backed Set 的 removeIf 线程安全问题）
            activeObstacles[instance.worldName]?.let { obstacles ->
                val toRemove = obstacles.filter { it.config.id == config.id }
                if (toRemove.isNotEmpty()) {
                    obstacles.removeAll(toRemove.toSet())
                }
            }

            ObstacleOpenPostEvent(instance, config).call()
            instance.meta.add("obstacle.open", 1)
            instance.meta.add("obstacle.open.${config.id}", 1)
            return true
        } catch (e: Exception) {
            warningL("WarningObstacleOpenFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 恢复已保存的方块状态（彻底清理）
     */
    fun restoreBlocks(instance: DungeonInstance) {
        val worldName = instance.worldName
        val saved = savedBlockStates[worldName] ?: return
        val world = Bukkit.getWorld(worldName) ?: return

        for ((key, blockData) in saved) {
            val parts = key.split(",")
            if (parts.size == 3) {
                val x = parts[0].toInt()
                val y = parts[1].toInt()
                val z = parts[2].toInt()
                world.setBlockData(x, y, z, blockData)
            }
        }

        savedBlockStates.remove(worldName)
        activeObstacles.remove(worldName)
        openedObstacles.remove(worldName)
        autoCloseTasks.remove(worldName)?.values?.forEach { it.cancel() }
        KAngelDungeon.blockRegenMap.remove(worldName)
    }

    // ==================== 内部方块操作 ====================

    /**
     * 保存栅栏区域内的方块状态
     */
    private fun saveBlocks(world: World, gate: GateObstacle) {
        val blocks = getGateBlocks(gate)
        val worldKey = world.name
        val states = savedBlockStates.getOrPut(worldKey) { ConcurrentHashMap() }

        for ((index, pos) in blocks.withIndex()) {
            val block = world.getBlockAt(pos.x, pos.y, pos.z)
            val key = "${pos.x},${pos.y},${pos.z}"
            if (key !in states) {
                states[key] = block.blockData.clone()
            }
        }
    }

    /**
     * 放置栅栏方块（使用 AIR 作为前景方块，即移除方块来"打开"通道）
     * 在 RESTORE_BLOCKS 模式下，关闭栅栏 = 将保存的方块放回去
     */
    private fun placeGateBlocks(world: World, gate: GateObstacle, config: ObstacleConfig) {
        val seqConfig = gate.sequentialConfig
        val blocks = getGateBlocks(gate)

        if (seqConfig?.enabled == true) {
            // 顺序放置
            animateGateBlocks(world, gate, config, blocks, removeBlocks = false)
        } else {
            // 直接放置
            for (pos in blocks) {
                restoreBlockAt(world, pos)
            }
        }
    }

    /**
     * 移除栅栏方块（打开通道 = 替换为 AIR）
     */
    private fun removeGateBlocks(world: World, gate: GateObstacle, config: ObstacleConfig) {
        val seqConfig = gate.sequentialConfig
        val blocks = getGateBlocks(gate)

        if (seqConfig?.enabled == true) {
            // 顺序移除（反向 = 开门）
            animateGateBlocks(world, gate, config, blocks, removeBlocks = true)
        } else {
            // 直接移除
            for (pos in blocks) {
                world.getBlockAt(pos.x, pos.y, pos.z).setBlockData(
                    Bukkit.createBlockData("minecraft:air"), false
                )
            }
        }
    }

    /**
     * 顺序放置/移除方块（带动画效果）
     */
    private fun animateGateBlocks(
        world: World, gate: GateObstacle, config: ObstacleConfig,
        blocks: List<BlockPos>, removeBlocks: Boolean
    ) {
        val seqConfig = gate.sequentialConfig ?: return
        val sorted = sortBlocks(blocks, seqConfig.openDirection, removeBlocks && seqConfig.reverseOnClose)
        val effect = if (removeBlocks) seqConfig.closeEffect else seqConfig.openEffect

        var delay = 0
        var index = 0
        while (index < sorted.size) {
            val batch = sorted.subList(index, (index + seqConfig.blocksPerStep).coerceAtMost(sorted.size))
            val currentDelay = delay

            submit(delay = currentDelay.toLong()) {
                for (pos in batch) {
                    if (removeBlocks) {
                        world.getBlockAt(pos.x, pos.y, pos.z).setBlockData(
                            Bukkit.createBlockData("minecraft:air"), false
                        )
                    } else {
                        restoreBlockAt(world, pos)
                    }
                    // 播放步骤效果
                    if (effect?.enabled == true && effect.particle != null) {
                        try {
                            val particle = Particle.valueOf(effect.particle.uppercase())
                            world.spawnParticle(
                                particle,
                                Location(world, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5),
                                effect.count, 0.3, 0.3, 0.3, 0.01
                            )
                        } catch (_: Exception) {}
                    }
                }
            }

            index += seqConfig.blocksPerStep
            delay += seqConfig.stepDelayTicks
        }
    }

    /**
     * 根据方向排序方块列表
     */
    private fun sortBlocks(blocks: List<BlockPos>, direction: String, reverse: Boolean): List<BlockPos> {
        val sorted = when (direction.uppercase()) {
            "LEFT_TO_RIGHT" -> blocks.sortedBy { it.x }
            "RIGHT_TO_LEFT" -> blocks.sortedByDescending { it.x }
            "TOP_TO_BOTTOM" -> blocks.sortedByDescending { it.y }
            "BOTTOM_TO_TOP" -> blocks.sortedBy { it.y }
            "FRONT_TO_BACK" -> blocks.sortedBy { it.z }
            "BACK_TO_FRONT" -> blocks.sortedByDescending { it.z }
            else -> blocks.sortedBy { it.x }
        }
        return if (reverse) sorted.reversed() else sorted
    }

    /**
     * 恢复指定位置的已保存方块
     */
    private fun restoreBlockAt(world: World, pos: BlockPos) {
        val worldKey = world.name
        val key = "${pos.x},${pos.y},${pos.z}"
        val savedData = savedBlockStates[worldKey]?.get(key)
        if (savedData != null) {
            world.setBlockData(pos.x, pos.y, pos.z, savedData)
        }
    }

    /**
     * 获取栅栏的所有方块坐标
     */
    private fun getGateBlocks(gate: GateObstacle): List<BlockPos> {
        val result = LinkedHashSet<BlockPos>()

        // 显式定义的 blocks
        if (gate.blocks != null) {
            result.addAll(gate.blocks.values)
        }

        // cuboid 区域
        if (gate.cuboid != null) {
            val p1 = gate.cuboid.pos1
            val p2 = gate.cuboid.pos2
            val minX = minOf(p1.x, p2.x)
            val maxX = maxOf(p1.x, p2.x)
            val minY = minOf(p1.y, p2.y)
            val maxY = maxOf(p1.y, p2.y)
            val minZ = minOf(p1.z, p2.z)
            val maxZ = maxOf(p1.z, p2.z)

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        result.add(BlockPos(x, y, z))
                    }
                }
            }
        }

        return result.toList()
    }

    // ==================== 动画 ====================

    /**
     * 播放障碍物动画
     */
    fun playAnimation(world: World, animation: ObstacleAnimation?, config: ObstacleConfig) {
        if (animation == null || !animation.enabled) return

        if (animation.particle != null) {
            try {
                val particle = Particle.valueOf(animation.particle.uppercase())
                // 在每个栅栏区域中心播放心脏粒子
                for (gate in config.obstacles.values) {
                    val center = getGateCenterDouble(gate) ?: continue
                    repeat(animation.particleCount) {
                        world.spawnParticle(
                            particle,
                            Location(world,
                                center.first + (Math.random() - 0.5) * 2,
                                center.second + (Math.random() - 0.5) * 2,
                                center.third + (Math.random() - 0.5) * 2
                            ),
                            1, 0.0, 0.0, 0.0, 0.01
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // 播放音效
        if (animation.sound != null) {
            try {
                val sound = Sound.valueOf(animation.sound.uppercase())
                for (gate in config.obstacles.values) {
                    val center = getGateCenterDouble(gate) ?: continue
                    world.playSound(
                        Location(world, center.first, center.second, center.third),
                        sound, animation.volume.toFloat(), animation.pitch.toFloat()
                    )
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * 计算栅栏区域中心坐标（双精度，避免整数除法精度丢失）
     */
    private fun getGateCenterDouble(gate: GateObstacle): Triple<Double, Double, Double>? {
        val cuboid = gate.cuboid ?: return null
        return Triple(
            (cuboid.pos1.x + cuboid.pos2.x) / 2.0,
            (cuboid.pos1.y + cuboid.pos2.y) / 2.0,
            (cuboid.pos1.z + cuboid.pos2.z) / 2.0
        )
    }

    // ==================== 代理脚本 ====================

    private fun runObstacleAgent(script: String?, instance: DungeonInstance, config: ObstacleConfig, extraVars: Map<String, Any?> = emptyMap()) {
        if (script.isNullOrBlank()) return
        try {
            val data = defaultData + mapOf(
                "instance" to instance,
                "template" to instance.getTemplate(),
                "obstacle" to config
            ) + extraVars
            GraalJsUtil.cachedEval(script, data)
        } catch (e: Exception) {
            warningL("WarningObstacleAgentFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }
}

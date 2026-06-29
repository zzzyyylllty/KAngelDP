package io.github.zzzyyylllty.kangeldungeon.util.task

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 任务管理器 - 监听地牢事件并执行对应的 JS 脚本
 */
object TaskManager {

    // 任务执行计数: instanceUUID -> (taskId -> count)
    private val executionCounts = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Int>>()
    // 任务上次执行时间(tick): instanceUUID -> (taskId -> lastTick)
    private val lastExecuted = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()

    /**
     * 获取地牢的所有任务配置
     */
    fun getTaskConfigs(instance: DungeonInstance): Map<String, TaskConfig> {
        return KAngelDungeon.dungeonTaskConfigs[instance.templateName] ?: emptyMap()
    }

    /**
     * 根据 trigger 触发地牢中所有匹配的任务
     * @param instance 地牢实例
     * @param trigger 触发器类型
     * @param context 传递给 JS 脚本的额外变量
     */
    fun triggerTasks(instance: DungeonInstance, trigger: String, context: Map<String, Any?> = emptyMap()) {
        val configs = getTaskConfigs(instance)
        if (configs.isEmpty()) return

        val currentTick = java.lang.System.currentTimeMillis() / 50L

        // 按 priority 升序排序（低优先级先执行，高优先级后执行）
        val sorted = configs.entries.sortedBy { (_, config) -> config.priority }

        for ((id, config) in sorted) {
            if (!config.trigger.equals(trigger, ignoreCase = true)) continue
            if (!matchesFilters(config, context)) continue

            // 原子性地检查并声明执行权限（count + cooldown）
            if (config.maxExecutions > 0 || config.cooldown > 0) {
                val countMap = executionCounts.computeIfAbsent(instance.uuid) { ConcurrentHashMap() }
                synchronized(countMap) {
                    if (config.maxExecutions > 0) {
                        val current = countMap.getOrDefault(id, 0)
                        if (current >= config.maxExecutions) continue
                    }
                    if (config.cooldown > 0) {
                        val lastTickMap = lastExecuted.computeIfAbsent(instance.uuid) { ConcurrentHashMap() }
                        val last = lastTickMap.getOrDefault(id, 0L)
                        if (currentTick - last < config.cooldown) continue
                    }
                    // 原子声明：预占执行位（防止并发重复执行）
                    if (config.maxExecutions > 0) {
                        countMap.merge(id, 1) { old, new -> old + new }
                    }
                    if (config.cooldown > 0) {
                        lastExecuted.computeIfAbsent(instance.uuid) { ConcurrentHashMap() }[id] = currentTick
                    }
                }
            }

            executeTask(instance, id, config, context)
        }
    }

    /**
     * 手动触发指定任务（CUSTOM 触发器或任意触发器），跳过冷却和次数限制
     * JS: instance.triggerTask("taskId")
     * @return 是否找到并执行了任务
     */
    fun triggerTask(instance: DungeonInstance, taskId: String): Boolean {
        val config = KAngelDungeon.dungeonTaskConfigs[instance.templateName]?.get(taskId) ?: return false
        executeTask(instance, taskId, config, emptyMap())
        return true
    }

    /**
     * 获取任务的执行次数
     */
    fun getExecutionCount(instance: DungeonInstance, taskId: String): Int {
        return executionCounts[instance.uuid]?.get(taskId) ?: 0
    }

    /**
     * 重置任务的执行次数
     */
    fun resetExecutionCount(instance: DungeonInstance, taskId: String) {
        executionCounts[instance.uuid]?.remove(taskId)
    }

    /**
     * 清理地牢的所有任务跟踪数据
     */
    fun clearInstance(instance: DungeonInstance) {
        executionCounts.remove(instance.uuid)
        lastExecuted.remove(instance.uuid)
    }

    /**
     * 清理所有地牢的任务跟踪数据（reload 时调用）
     */
    fun clearAll() {
        executionCounts.clear()
        lastExecuted.clear()
    }

    /**
     * 检查任务的过滤条件是否匹配
     */
    private fun matchesFilters(config: TaskConfig, context: Map<String, Any?>): Boolean {
        if (config.filters.isEmpty()) return true

        for ((key, expected) in config.filters) {
            val actual = context[key]?.toString() ?: return false
            if (!actual.equals(expected, ignoreCase = true)) return false
        }
        return true
    }

    /**
     * 执行单个任务脚本
     */
    private fun executeTask(instance: DungeonInstance, taskId: String, config: TaskConfig, context: Map<String, Any?>) {
        val script = config.agent?.onTrigger ?: return
        if (script.isBlank()) return

        try {
            val data = defaultData + mapOf(
                "instance" to instance,
                "template" to instance.getTemplate(),
                "task" to config,
                "taskId" to taskId
            ) + context

            GraalJsUtil.cachedEval(script, data)
        } catch (e: Exception) {
            warningL("WarningTaskExecutionFailed", taskId, instance.templateName, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }
}

/**
 * 任务触发器监听器 - 监听 Bukkit 事件并触发对应的地牢任务
 */
object TaskTriggerListener {

    @SubscribeEvent
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val instanceUuid = KAngelDungeon.playerToInstanceIndex[player.uniqueId] ?: return
        val instance = KAngelDungeon.dungeonInstances[instanceUuid] ?: return
        if (instance.state != DungeonState.ACTIVE) return

        TaskManager.triggerTasks(instance, "BLOCK_BREAK", mapOf(
            "playerName" to player.name,
            "player" to player,
            "block" to event.block,
            "blockType" to event.block.type.name,
            "location" to event.block.location
        ))
    }

    @SubscribeEvent
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val instanceUuid = KAngelDungeon.playerToInstanceIndex[player.uniqueId] ?: return
        val instance = KAngelDungeon.dungeonInstances[instanceUuid] ?: return
        if (instance.state != DungeonState.ACTIVE) return

        TaskManager.triggerTasks(instance, "BLOCK_PLACE", mapOf(
            "playerName" to player.name,
            "player" to player,
            "block" to event.block,
            "blockType" to event.block.type.name,
            "location" to event.block.location
        ))
    }

    @SubscribeEvent
    fun onDamageTaken(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player) return
        val instanceUuid = KAngelDungeon.playerToInstanceIndex[entity.uniqueId] ?: return
        val instance = KAngelDungeon.dungeonInstances[instanceUuid] ?: return
        if (instance.state != DungeonState.ACTIVE) return

        TaskManager.triggerTasks(instance, "DAMAGE_TAKEN", mapOf(
            "playerName" to entity.name,
            "player" to entity,
            "damage" to event.finalDamage,
            "damageCause" to event.cause.name,
            "location" to entity.location
        ))
    }

    @SubscribeEvent
    fun onDamageDealt(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Player) return
        val instanceUuid = KAngelDungeon.playerToInstanceIndex[damager.uniqueId] ?: return
        val instance = KAngelDungeon.dungeonInstances[instanceUuid] ?: return
        if (instance.state != DungeonState.ACTIVE) return

        TaskManager.triggerTasks(instance, "DAMAGE_DEALT", mapOf(
            "playerName" to damager.name,
            "player" to damager,
            "damage" to event.finalDamage,
            "damageCause" to event.cause.name,
            "targetType" to event.entity.type.name,
            "target" to event.entity,
            "location" to damager.location
        ))
    }
}

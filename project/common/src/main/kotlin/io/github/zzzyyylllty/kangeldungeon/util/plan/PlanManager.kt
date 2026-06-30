package io.github.zzzyyylllty.kangeldungeon.util.plan

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 计划管理器 - 管理地牢中的定时任务（计划）
 *
 * 使用 submit 处理同步计划，async=true 处理异步计划：
 * - async=false: 通过 submit(delay, period) 在主线程调度
 * - async=true: 通过 submit(async=true, delay, period) 异步执行（均可追踪/取消）
 */
object PlanManager {

    // instance uuid -> (planName -> PlatformTask)
    private val activePlanTasks = ConcurrentHashMap<UUID, ConcurrentHashMap<String, PlatformExecutor.PlatformTask>>()

    /**
     * 根据触发器名称启动匹配的计划
     */
    fun startPlansForTrigger(instance: DungeonInstance, trigger: String) {
        val planMap = KAngelDungeon.dungeonPlanConfigs[instance.templateName] ?: return
        val matchingPlans = planMap.values.filter { it.trigger.equals(trigger, ignoreCase = true) }
        if (matchingPlans.isEmpty()) return

        val instanceTasks = activePlanTasks.getOrPut(instance.uuid) { ConcurrentHashMap() }

        for (plan in matchingPlans) {
            if (instanceTasks.containsKey(plan.name)) continue
            val task = schedulePlan(instance, plan)
            if (task != null) {
                instanceTasks[plan.name] = task
            }
        }
    }

    /**
     * 调度单个计划任务
     * async=false → submit（主线程）
     * async=true  → submit(async=true)（异步，均可追踪/取消）
     */
    private fun schedulePlan(instance: DungeonInstance, plan: Plan): PlatformExecutor.PlatformTask? {
        val delay = (plan.delay ?: 0).toLong()

        return if (plan.async) {
            if (plan.period != null) {
                submit(async = true, delay = delay, period = plan.period.toLong()) { executePlanScript(instance, plan) }
            } else {
                submit(async = true, delay = delay) { executePlanScript(instance, plan) }
            }
        } else {
            if (plan.period != null) {
                submit(delay = delay, period = plan.period.toLong()) { executePlanScript(instance, plan) }
            } else {
                submit(delay = delay) { executePlanScript(instance, plan) }
            }
        }
    }

    /**
     * 执行计划的 JS 脚本
     */
    private fun executePlanScript(instance: DungeonInstance, plan: Plan) {
        // 检查实例是否仍然有效（可能在异步延迟期间被清理）
        if (instance.isFinished() || !KAngelDungeon.dungeonInstances.containsKey(instance.uuid)) {
            stopAllPlans(instance)
            return
        }
        if (plan.onRun == null) return
        val data = defaultData + mapOf(
            "instance" to instance,
            "template" to instance.getTemplate(),
            "plan" to plan,
            "trigger" to plan.trigger
        )
        try {
            GraalJsUtil.cachedEval(plan.onRun, data)
        } catch (e: Exception) {
            warningL("WarningPlanExecutionFailed", plan.name, instance.templateName, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 停止地牢所有进行中的计划
     */
    fun stopAllPlans(instance: DungeonInstance) {
        val instanceTasks = activePlanTasks.remove(instance.uuid) ?: return
        for ((_, task) in instanceTasks) {
            task.cancel()
        }
    }

    /**
     * 清理地牢实例的计划数据
     */
    fun clearInstance(instance: DungeonInstance) {
        stopAllPlans(instance)
    }

    /**
     * 检查指定计划是否活跃
     */
    fun isPlanActive(instance: DungeonInstance, planName: String): Boolean {
        return activePlanTasks[instance.uuid]?.containsKey(planName) ?: false
    }

    /**
     * 获取活跃计划名称列表
     */
    fun getActivePlanNames(instance: DungeonInstance): List<String> {
        return activePlanTasks[instance.uuid]?.keys?.toList() ?: emptyList()
    }
}

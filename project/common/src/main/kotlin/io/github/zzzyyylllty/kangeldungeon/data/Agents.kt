package io.github.zzzyyylllty.kangeldungeon.data

import com.google.gson.Gson
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeonAPI
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonCustomScriptDataLoadEvent
import io.github.zzzyyylllty.kangeldungeon.function.javascript.EventUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.ItemStackUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.PlayerUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.ThreadUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.EntityUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.BlockUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.RandomUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.MathUtil
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import io.github.zzzyyylllty.kangeldungeon.util.kit.KitManager
import io.github.zzzyyylllty.kangeldungeon.util.monster.MonsterManager
import io.github.zzzyyylllty.kangeldungeon.util.obstacle.ObstacleManager
import io.github.zzzyyylllty.kangeldungeon.util.plan.PlanManager
import io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager
import io.github.zzzyyylllty.kangeldungeon.util.region.RegionManager
import io.github.zzzyyylllty.kangeldungeon.function.kether.evalKether
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper
import io.github.zzzyyylllty.kangeldungeon.function.javascript.Sys
//import io.github.zzzyyylllty.kangeldungeon.util.jsonUtil
import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmJsonUtil
import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmLegacyAmpersandUtil
import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmLegacySectionUtil
import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmUtil
import io.github.zzzyyylllty.kangeldungeon.util.toBooleanTolerance
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import javax.script.CompiledScript
import javax.script.SimpleBindings

var defaultData = LinkedHashMap<String, Any?>()

/**
 * 供 JS 脚本安全调用的 Bukkit 方法包装。
 * 使用 open class + instance 避免 Kotlin object 的 GraalJS 互操作问题。
 */
open class ScriptBukkit {
    fun broadcast(component: Any?) {
        if (component is net.kyori.adventure.text.Component) {
            org.bukkit.Bukkit.broadcast(component)
        } else if (component != null) {
            org.bukkit.Bukkit.broadcast(net.kyori.adventure.text.Component.text(component.toString()))
        }
    }
}

val ScriptBukkitInstance = ScriptBukkit()

@Awake(LifeCycle.ENABLE)
fun registerExternalData() {
    defaultData.putAll(
        linkedMapOf(
            "mmUtil" to mmUtil,
            "mmJsonUtil" to mmJsonUtil,
            "mmLegacySectionUtil" to mmLegacySectionUtil,
            "mmLegacyAmpersandUtil" to mmLegacyAmpersandUtil,
//            "jsonUtils" to jsonUtils,
            "ItemStackUtil" to ItemStackUtil,
            "EventUtil" to EventUtil,
            "ThreadUtil" to ThreadUtil,
            "PlayerUtil" to PlayerUtil,
            "EntityUtil" to EntityUtil,
            "BlockUtil" to BlockUtil,
            "RandomUtil" to RandomUtil,
            "MathUtil" to MathUtil,
            "DungeonAPI" to KAngelDungeonAPI::class.java,
            "Math" to Math::class.java,
            "System" to System::class.java,
            "Sys" to Sys,
            "Bukkit" to ScriptBukkitInstance,
            "Gson" to Gson::class.java,
            "TargetSelectorHelper" to TargetSelectorHelper,
            "ObstacleManager" to ObstacleManager,
            "MonsterManager" to MonsterManager,
            "RegionManager" to RegionManager,
            "PlanManager" to PlanManager,
            "KitManager" to KitManager,
            "DungeonHelper" to DungeonHelper,
            "TaskManager" to TaskManager
        ))
    val event = KAngelDungeonCustomScriptDataLoadEvent(defaultData)
    event.call()
    defaultData = event.defaultData
}

data class Agents(
    val agents: LinkedHashMap<String, Agent>
) {
    fun runAgent(agent: String, extraVariables: Map<String, Any?>, player: Player?): Boolean? {
        return agents[agent]?.runAgent(extraVariables, player)
    }
}

data class Agent(
    val trigger: String,
    val gjs: String?,
){
    fun runAgent(extraVariables: Map<String, Any?>, player: Player?): Boolean {
        val data = defaultData + extraVariables + mapOf("player" to player, "trigger" to trigger)
        return gjs?.let {
            GraalJsUtil.cachedEval(it, data)
        }?.toBooleanTolerance() ?: true
    }
}
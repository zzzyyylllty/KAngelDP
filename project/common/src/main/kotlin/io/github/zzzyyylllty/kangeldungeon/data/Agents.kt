package io.github.zzzyyylllty.kangeldungeon.data

import com.google.gson.Gson
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeonAPI
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonCustomScriptDataLoadEvent
import io.github.zzzyyylllty.kangeldungeon.function.javascript.EventUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.ItemStackUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.PlayerUtil
import io.github.zzzyyylllty.kangeldungeon.function.javascript.ThreadUtil
import io.github.zzzyyylllty.kangeldungeon.function.kether.evalKether
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper
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
            "DungeonAPI" to KAngelDungeonAPI::class.java,
            "Math" to Math::class.java,
            "System" to System::class.java,
            "Bukkit" to Bukkit::class.java,
            "Gson" to Gson::class.java,
            "TargetSelectorHelper" to TargetSelectorHelper
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
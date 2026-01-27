package io.github.zzzyyylllty.kangeldungeon.data.load


// import org.yaml.snakeyaml.Yaml
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.config
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.regenTemplates
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.regenTemplatesByBlock
import io.github.zzzyyylllty.kangeldungeon.data.Agents
import io.github.zzzyyylllty.kangeldungeon.data.Condition
import io.github.zzzyyylllty.kangeldungeon.data.OmniItem
import io.github.zzzyyylllty.kangeldungeon.data.RegenBlock
import io.github.zzzyyylllty.kangeldungeon.data.RegenItem
import io.github.zzzyyylllty.kangeldungeon.data.RegenTemplate
import io.github.zzzyyylllty.kangeldungeon.logger.infoL
import io.github.zzzyyylllty.kangeldungeon.logger.severeS
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.ExternalItemHelper
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import io.github.zzzyyylllty.kangeldungeon.util.toBooleanTolerance
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common5.compileJS
import java.io.File
import kotlin.String
import kotlin.collections.forEach
import kotlin.collections.set

fun loadRegenFiles() {
    infoL("RegenBlockLoad")
    if (!File(getDataFolder(), "regen").exists()) {
        warningL("RegenBlockRegen")
        releaseResourceFile("regen/iron.yml")
    }
    val files = File(getDataFolder(), "regen").listFiles()
    for (file in files) {
        // If directory load file in it...
        if (file.isDirectory) file.listFiles()?.forEach {
            loadRegenFile(it)
        }
        else loadRegenFile(file)
    }
}
fun loadRegenFile(file: File) {
    devLog("Loading file ${file.name}")

    if (file.isDirectory) file.listFiles()?.forEach {
        loadRegenFile(it)
    } else {
        if (!checkRegexMatch(file.name, (config["file-load.regen"] ?: ".*").toString())) {
            devLog("${file.name} not match regex, skipping...")
            return
        }
        val map = multiExtensionLoader(file)
        if (map != null) for (it in map.entries) {
            val key = it.key
            val value = map[key] ?: continue
            (value as Map<String, Any?>?)?.let { arg -> loadRegen(key, arg) }
        } else {
            devLog("Map is null, skipping.")
        }
    }
}

fun loadRegen(key: String, arg: Map<String, Any?>) {
    val c = ConfigUtil

    val block = c.getBlock(arg["block"])
    val tempBlock = c.getBlock(arg["temp-block"])
    val post = c.getConditions(arg["post-conditions"] ?: arg["post-condition"])
    val condition = c.getConditions(arg["conditions"] ?: arg["condition"])
    val period = arg["period"].toString().toDoubleOrNull() ?: 10.0
    val vanilla = (arg["vanilla-drops"] ?: arg["vanilla"])?.toBooleanTolerance() ?: true
    val rawItems = (arg["items"] ?: arg["item"]) as? List<Any?>? ?: emptyList()
    val items = mutableListOf<RegenItem>()
    for (rawItem in rawItems) {
        if (rawItem is String) {
            items.add(RegenItem(c.getItem(rawItem) ?: OmniItem("minecraft", "DIAMOND"), 1, null))
        } else if (rawItem is Map<*,*>) {
            items.add(RegenItem(c.getItem(rawItem["item"]) ?: OmniItem("minecraft", "DIAMOND"), rawItem["amount"], c.getConditions(rawItem["conditions"] ?: rawItem["condition"])))
        }
    }

    val breakCondition = c.getConditions(arg["break-conditions"] ?: arg["break-condition"])

    if (block == null) severeS("Target Regen Block cannot be null!")
    block?.let {
        val template = RegenTemplate(
            key,
            it,
            post,
            condition,
            breakCondition,
            period,
            tempBlock,
            vanilla,
            items,
            c.getAgents(arg)
        )
        val bTemplate = regenTemplatesByBlock[block]
        if (bTemplate != null) {
            regenTemplatesByBlock[block]?.add(key)
        } else {
            regenTemplatesByBlock[block] = mutableListOf(key)
        }
        regenTemplates[key] = template
    }
}

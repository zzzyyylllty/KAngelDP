package io.github.zzzyyylllty.kangeldungeon.data.load

import io.github.zzzyyylllty.kangeldungeon.util.serialize.parseToMap
import java.io.File
import kotlin.io.extension
import kotlin.io.readText

fun multiExtensionLoader(file: File): Map<String, Any?>? {

    val format = when (val extension = file.extension.lowercase()) {
        "yml" -> "yaml"
        "tml" -> "toml"
        else -> extension
    }
    return parseToMap(file.readText(Charsets.UTF_8), format)
}

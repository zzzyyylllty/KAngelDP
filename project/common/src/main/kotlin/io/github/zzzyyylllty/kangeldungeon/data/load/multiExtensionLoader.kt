package io.github.zzzyyylllty.kangeldungeon.data.load

import io.github.zzzyyylllty.kangeldungeon.util.serialize.parseToMap
import java.io.File
import java.util.Locale
import kotlin.io.extension
import kotlin.io.readText

val locale: Locale? by lazy { Locale.getDefault() }

fun String.toLowerCase(): String {
    return lowercase(locale ?: Locale.getDefault())
}
fun String.toUpperCase(): String {
    return uppercase(locale ?: Locale.getDefault())
}

fun multiExtensionLoader(file: File): Map<String, Any?>? {

    val format = when (val extension = file.extension.toLowerCase()) {
        "yml" -> "yaml"
        "tml" -> "toml"
        else -> extension
    }
    return parseToMap(file.readText(), format)
}

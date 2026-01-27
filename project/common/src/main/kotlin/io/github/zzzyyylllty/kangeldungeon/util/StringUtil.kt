package io.github.zzzyyylllty.kangeldungeon.util

import java.util.Locale

val loc: Locale? by lazy { Locale.getDefault() }

fun String.toLowerCase(): String {
    return lowercase(loc ?: Locale.getDefault())
}
fun String.toUpperCase(): String {
    return uppercase(loc ?: Locale.getDefault())
}



fun Any?.asListEnhanced() : List<String>? {
    if (this == null) return null
    val thisList = if (this is List<*>) this else listOf(this)
    val list = mutableListOf<String>()
    for (string in thisList) {
        if (string == null) continue
        list.addAll(string.toString().split("\n","<br>", ignoreCase = true))
    }
    if (!list.isEmpty() && list.last() == "") list.removeLast()
    return list
}

fun Any?.asListedStringEnhanced() : String? {
    return this.asListEnhanced()?.joinToString("\n")
}

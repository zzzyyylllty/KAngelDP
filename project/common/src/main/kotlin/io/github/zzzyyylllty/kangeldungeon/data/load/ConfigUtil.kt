package io.github.zzzyyylllty.kangeldungeon.data.load

import kotlin.collections.set
import kotlin.let
import kotlin.text.isEmpty
import kotlin.text.matches
import kotlin.text.split
import kotlin.text.toInt
import kotlin.text.toLong
import kotlin.text.toRegex

object ConfigUtil {
    fun getString(input: Any?): String? {
        return input?.toString()
    }
    fun getInt(input: Any?): Int? {
        return input?.toString()?.toInt()
    }

    fun getLong(input: Any?): Long? {
        return input?.toString()?.toLong()
    }
    fun getDeep(input: Any?, location: String): Any? {
        if (input == null || location.isEmpty()) return null

        val keys = location.split(".")
        var current: Any? = input

        for (key in keys) {
            if (current !is Map<*, *>) return null
            current = current[key]
        }
        return current
    }


}
fun checkRegexMatch(input: String, regex: String): Boolean {
    return input.matches(regex.toRegex())
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

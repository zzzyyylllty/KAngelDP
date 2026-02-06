package io.github.zzzyyylllty.kangeldungeon.util

object CastHelper {
    fun smartCast(input: String?): Any? {
        if (input == null) return null
        return when {
            input.matches(Regex("^-?\\d{1,10}$")) -> {
                try {
                    input.toInt()
                } catch (e: NumberFormatException) {
                    input // 默认为 String
                }
            }
            input.matches(Regex("^-?\\d+$")) -> {
                try {
                    input.toLong()
                } catch (e: NumberFormatException) {
                    input // 默认为 String
                }
            }
            input.matches(Regex("^-?\\d+\\.\\d+$")) -> {
                try {
                    input.toDouble()
                } catch (e: NumberFormatException) {
                    input // 默认为 String
                }
            }
            input.matches(Regex("^(true|false)$", RegexOption.IGNORE_CASE)) -> input.toBoolean()
            else -> input // 默认为 String
        }
    }
    fun increaseAny(first: Any?, second: Any?): Any? {
        if (first == null) return second
        if (second == null) return first
        return if (first is Int) {
            first.plus(second.toString().toInt())
        } else if (first is Double) {
            first.plus(second.toString().toDouble())
        } else if (first is Float) {
            first.plus(second.toString().toFloat())
        } else if (first is String) {
            first.plus(second.toString())
        } else if (first is Long) {
            first.plus(second.toString().toLong())
        } else {
            first.toString().plus(second.toString())
        }
    }
}

package io.github.zzzyyylllty.kangeldungeon.function.javascript

/**
 * 在 GraalJS 安全模式下，java.lang.System 的静态方法被阻止访问。
 * 此对象提供常用 System 功能的安全包装，绑定到 JS 全局变量 "Sys"。
 *
 * JS 用法:
 *   Sys.currentTimeMillis()         // → 返回当前毫秒时间戳
 *   Sys.nanoTime()                  // → 返回纳秒时间戳
 *   Sys.println("message")          // → 输出到控制台（stdout）
 *   Sys.printerr("error message")   // → 输出到控制台（stderr）
 *   Sys.getProperty("os.name")      // → 读取系统属性
 *   Sys.sleep(1000)                 // → 睡眠指定毫秒
 *   Sys.newDate()                   // → 创建 java.util.Date
 *   Sys.newDate(millis)             // → 从毫秒创建 java.util.Date
 */
open class SafeSystemUtil {

    fun currentTimeMillis(): Long = System.currentTimeMillis()

    fun nanoTime(): Long = System.nanoTime()

    fun println(message: Any?) {
        System.out.println(message?.toString() ?: "null")
    }

    fun printerr(message: Any?) {
        System.err.println(message?.toString() ?: "null")
    }

    fun getProperty(key: String): String? = System.getProperty(key)

    fun getProperty(key: String, default: String): String = System.getProperty(key, default)

    fun sleep(millis: Long) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            throw IllegalStateException("Sys.sleep() must not be called on the main server thread")
        }
        Thread.sleep(millis)
    }

    fun newDate(): java.util.Date = java.util.Date()

    fun newDate(millis: Long): java.util.Date = java.util.Date(millis)
}

val Sys = SafeSystemUtil()

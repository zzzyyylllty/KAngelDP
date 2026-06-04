package io.github.zzzyyylllty.kangeldungeon.function.javascript


object ThreadUtil {
    fun sleep(time: Long) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            throw IllegalStateException("ThreadUtil.sleep() must not be called on the main server thread")
        }
        Thread.sleep(time)
    }
}
package io.github.zzzyyylllty.kangeldungeon.function.javascript

import io.github.zzzyyylllty.kangeldungeon.logger.severeL
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event

object EventUtil {
    fun cancel(event: Cancellable,cancel: Boolean = true) {
        try {
            event.isCancelled = cancel
        } catch (e: Exception) {
            severeL("EventUtilCancelFailed", event.toString())
            e.printStackTrace()
        }
    }
    fun call(event: Event) {
        try {
            event.callEvent()
        } catch (e: Exception) {
            severeL("EventUtilCallFailed", event.toString())
            e.printStackTrace()
        }
    }
}
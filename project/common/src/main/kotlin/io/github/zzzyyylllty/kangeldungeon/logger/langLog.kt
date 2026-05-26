package io.github.zzzyyylllty.kangeldungeon.logger

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.consoleSender
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import taboolib.module.lang.asLangText

val prefix = "[<gradient:#ffaa66:#8884ff>KAngelDP</gradient>]"


fun infoL(node: String,vararg args: Any) {
    infoS(console.asLangText(node,*args))
}
fun severeL(node: String,vararg args: Any) {
    severeS(console.asLangText(node,*args))
}
fun warningL(node: String,vararg args: Any) {
    warningS(console.asLangText(node,*args))
}

fun fineS(message: String) {
    consoleSender.sendStringAsComponent("<gray>$prefix [<#66ffcc>FINE</#66ffcc>]</gray> <reset>$message")
}

fun debugS(message: String) {
    consoleSender.sendStringAsComponent("<gray>$prefix [<#ddaa77>DEBUG</#ddaa77>]</gray> <#aaaaaa>$message")
}

fun infoS(message: String) {
    consoleSender.sendStringAsComponent("<gray>$prefix [<#66ccff>INFO</#66ccff>]</gray> <reset>$message")
}

fun warningS(message: String) {
    consoleSender.sendStringAsComponent("<gray>$prefix [<#ffee66>WARN</#ffee66>]</gray> <#eeeeaa>$message")
}

fun severeS(message: String) {
    consoleSender.sendStringAsComponent("<gray>$prefix [<#ff6600>ERROR</#ff6600>]</gray> <#ffccbb>$message")
}

fun CommandSender.sendStringAsComponent(message: String) {
    val mm = MiniMessage.miniMessage()
    val component = try {
        // 优先使用 MiniMessage 解析（支持 <gradient>, <red> 等标签）
        mm.deserialize(message.replace('§', '&'))
    } catch (_: Exception) {
        // MiniMessage 解析失败时回退到传统 & 颜色代码
        LegacyComponentSerializer.legacyAmpersand().deserialize(message.replace('§', '&'))
    }
    // Adventure 环境下 Bukkit CommandSender 应实现 Audience，使用安全转换避免 ClassCastException
    if (this is Audience) {
        (this as Audience).sendMessage(component)
    } else {
        // 非 Audience 的 CommandSender（如某些平台的控制台代理），回退到传统字符串消息
        sendMessage(LegacyComponentSerializer.legacySection().serialize(component))
    }
}

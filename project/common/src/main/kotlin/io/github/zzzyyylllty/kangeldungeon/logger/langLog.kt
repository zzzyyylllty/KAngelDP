package io.github.zzzyyylllty.kangeldungeon.logger

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.consoleSender
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import taboolib.module.lang.asLangText

val prefix = "[<gradient:#ffaa66:#8884ff>KAngelDungeon</gradient>]"


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
    val legacy = LegacyComponentSerializer.legacyAmpersand()
    (this as Audience).sendMessage(mm.deserialize(legacy.serialize(legacy.deserialize(message.replace("§", "&")))))
}

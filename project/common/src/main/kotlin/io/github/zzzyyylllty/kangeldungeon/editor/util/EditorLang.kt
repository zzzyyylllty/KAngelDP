package io.github.zzzyyylllty.kangeldungeon.editor.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import taboolib.platform.util.asLangText

/**
 * Extension functions on [Player] for editor localization.
 *
 * All lang keys use the "editor." prefix automatically.
 * In lang files, entries use MiniMessage format.
 */

private val mm = MiniMessage.miniMessage()

/** Get a localized [Component] for the given lang key. */
fun Player.lang(key: String, vararg args: Any): Component {
    return mm.deserialize(this.asLangText("editor.$key", *args))
}

/** Send a localized message to this player. */
fun Player.langMsg(key: String, vararg args: Any) {
    this.sendMessage(this.lang(key, *args))
}

/** Get a localized legacy §-formatted string (for GUI titles). */
fun Player.langStr(key: String, vararg args: Any): String {
    return this.asLangText("editor.$key", *args)
}

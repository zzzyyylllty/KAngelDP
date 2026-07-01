package io.github.zzzyyylllty.kangeldungeon.editor

import io.github.zzzyyylllty.kangeldungeon.editor.screens.MainMenu
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.subCommand

/**
 * /kangeldungeon editor — opens the visual configuration editor.
 */
@CommandHeader(
    name = "kangeldungeon",
    aliases = ["dg", "dungeon"],
    permission = "kangeldungeon.command.editor",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object EditorCommand {

    @CommandBody
    val editor = subCommand {
        execute<Player> { sender, _, _ ->
            EditorSession.get(sender)  // init session
            MainMenu.open(sender)
        }
    }
}

package io.github.zzzyyylllty.kangeldungeon.editor

import io.github.zzzyyylllty.kangeldungeon.editor.screens.MainMenu
import io.github.zzzyyylllty.kangeldungeon.editor.util.SelectionTool
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
        // /kangeldungeon editor wand — gives selection wand
        literal("wand") {
            execute<Player> { sender, _, _ ->
                SelectionTool.giveWand(sender)
            }
        }
        // /kangeldungeon editor pos1 — set pos1 to current location
        literal("pos1") {
            execute<Player> { sender, _, _ ->
                val session = EditorSession.get(sender)
                session.setPosition1(sender.location)
                sender.sendMessage("§aPos1 set to current location!")
            }
        }
        // /kangeldungeon editor pos2 — set pos2 to current location
        literal("pos2") {
            execute<Player> { sender, _, _ ->
                val session = EditorSession.get(sender)
                session.setPosition2(sender.location)
                sender.sendMessage("§aPos2 set to current location!")
            }
        }
    }
}

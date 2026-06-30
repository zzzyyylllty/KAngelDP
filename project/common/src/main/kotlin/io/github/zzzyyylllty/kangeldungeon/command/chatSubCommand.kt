package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.platform.util.asLangText

/**
 * 地牢聊天命令 — /dchat <message>
 * 将消息只发送给同地牢的玩家
 * 另有自动路由功能：在 AsyncPlayerChatEvent 中将普通聊天转发给同地牢玩家
 */
@CommandHeader(
    name = "kangeldungeonchat",
    aliases = ["dchat", "dc"],
    permission = "kangeldungeon.command.dchat",
    permissionDefault = taboolib.common.platform.command.PermissionDefault.TRUE
)
object DungeonChatCommand {

    private val mm = MiniMessage.miniMessage()

    @CommandBody
    val main = mainCommand {
        execute<org.bukkit.command.CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            player.sendMessage(mm.deserialize("<red>用法: /dchat <消息></red>"))
        }
    }

    @CommandBody
    val message = subCommand {
        dynamic("message") {
            // 只用第一个词，更完整的消息由自动路由处理
            execute<org.bukkit.command.CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@execute
                }
                val msg = context["message"]
                sendDungeonChat(player, msg)
            }
        }
    }

    @CommandBody
    val toggle = subCommand {
        execute<org.bukkit.command.CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            if (player.uniqueId in KAngelDungeon.dungeonChatOptOut) {
                KAngelDungeon.dungeonChatOptOut.remove(player.uniqueId)
                player.sendMessage(mm.deserialize("<green>已开启地牢自动聊天路由</green>"))
            } else {
                KAngelDungeon.dungeonChatOptOut.add(player.uniqueId)
                player.sendMessage(mm.deserialize("<red>已关闭地牢自动聊天路由</red>"))
            }
        }
    }

    /**
     * 将消息发送给同地牢的所有玩家
     */
    fun sendDungeonChat(player: Player, message: String) {
        val instanceUuid = KAngelDungeon.playerToInstanceIndex[player.uniqueId] ?: run {
            player.sendMessage(mm.deserialize("<red>你不在任何地牢中</red>"))
            return
        }
        val instance = KAngelDungeon.dungeonInstances[instanceUuid] ?: return
        val config = instance.getTemplate()?.dungeonChat ?: return
        if (!config.enabled) {
            player.sendMessage(mm.deserialize("<red>此地牢未启用聊天功能</red>"))
            return
        }

        val formatted = config.format
            .replace("%player%", player.name)
            .replace("%message%", message)
            .replace("%dungeon%", instance.templateName)

        val component = mm.deserialize(formatted)
        for (uuid in instance.players) {
            val target = Bukkit.getPlayer(uuid) ?: continue
            target.sendMessage(component)
        }
    }
}

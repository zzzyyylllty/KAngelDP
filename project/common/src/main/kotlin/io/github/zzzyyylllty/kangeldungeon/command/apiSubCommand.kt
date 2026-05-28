package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.function.kether.runKether
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync
import taboolib.platform.util.asLangText

@CommandHeader(
    name = "kangeldungeonapi",
    aliases = ["dga", "dungeonapi"],
    permission = "kangeldungeon.command.api",
    description = "api Command of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object ApiCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    /** 解析 Minimessage */
    @CommandBody
    val minimessage = subCommand {
        dynamic("content") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val content = context["content"]
                    sender.sendStringAsComponent(content) }
            }
        }
    }

    /** Kether */
    @CommandBody
    val eval = subCommand {
        dynamic("script") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    // 获取参数的值
                    val content = context["script"]
                    sender.sendStringAsComponent(sender.asLangText("ApiKether", content))
                    val ret = runKether(listOf(content), sender)
                    sender.sendStringAsComponent(sender.asLangText("ApiReturn", ret.get().toString())) }
            }
        }
    }



    /** Kether */
    @CommandBody
    val evalJs = subCommand {
        dynamic("script") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val content = context["script"].toString()
                    sender.sendStringAsComponent(sender.asLangText("ApiJs", content))
                    val ret = GraalJsUtil.directEval(content, mapOf("player" to sender))
                    sender.sendStringAsComponent(sender.asLangText("ApiReturn", ret.toString())) }
            }
        }
    }

    /** Kether */
    @CommandBody
    val evalByPlayer = subCommand {
        player("player") {
            dynamic("script") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        val bukkitPlayer = tabooPlayer.castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("ApiPlayerInvalid"))
                            return@submitAsync
                        }
                        // 获取参数的值
                        val content = context["script"].toString()
                        sender.sendStringAsComponent(sender.asLangText("ApiKether", content))
                        val ret = runKether(listOf(content), bukkitPlayer)
                        sender.sendStringAsComponent(sender.asLangText("ApiReturn", ret.get().toString())) }
                }
            }
        }
    }
    /** Kether */
    @CommandBody
    val evalSilent = subCommand {
        dynamic("script") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val content = context["script"].toString()
                    runKether(listOf(content), sender) }
            }
        }
    }

    /** Kether */
    @CommandBody
    val evalByPlayerSilent = subCommand {
        player("player") {
            dynamic("script") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        val bukkitPlayer = tabooPlayer.castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("ApiPlayerInvalid"))
                            return@submitAsync
                        }
                        val content = context["script"].toString()
                        runKether(listOf(content), bukkitPlayer)
                    }
                }
            }
        }
    }


}

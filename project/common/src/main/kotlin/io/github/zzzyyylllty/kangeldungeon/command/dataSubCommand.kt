package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.embiancomponent.EmbianComponent.SafetyComponentSetter
import io.github.zzzyyylllty.embiancomponent.tools.getComponentsNMSFiltered
import io.github.zzzyyylllty.kangeldungeon.data.DataUtil
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.util.minimessage.mmUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.bool
import taboolib.common.platform.command.component.CommandComponent
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.asList
import taboolib.module.nms.NMSItemTag.Companion.asNMSCopy
import taboolib.module.nms.getItemTag
import taboolib.platform.util.asLangText
import taboolib.platform.util.giveItem
import kotlin.run
import kotlin.text.replace
import kotlin.text.toDouble


@CommandHeader(
    name = "kangeldungeondata",
    aliases = ["dgd", "dungeond"],
    permission = "kangeldungeon.command.data",
    description = "DATA Command of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = true,
)
object DataCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }


    @CommandBody
    val get = subCommand {
        dynamic("id") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val player = sender as? Player? ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    val data = DataUtil.getDataRaw(player, key)
                    var message = sender.asLangText("PlayerData_Fetch", player.name, key, data ?: "<i>null")
                    sender.sendStringAsComponent(message)
                }
            }
            player("player") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        // 转化为Bukkit的Player
                        val player = tabooPlayer.castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                            return@submitAsync
                        }
                        val key = context["id"]
                        val data = DataUtil.getDataRaw(player, key)
                        var message = sender.asLangText("PlayerData_Fetch", player.name, key, data ?: "<i>null")
                        sender.sendStringAsComponent(message)
                    }
                }
            }
        }
    }


    @CommandBody
    val remove = subCommand {
        dynamic("id") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val player = sender as? Player? ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    DataUtil.removeData(player, key)
                    var message = sender.asLangText("PlayerData_Remove", player.name, key)
                    sender.sendStringAsComponent(message)
                }
            }
            player("player") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        // 转化为Bukkit的Player
                        val player = tabooPlayer.castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                            return@submitAsync
                        }
                        val key = context["id"]
                        DataUtil.removeData(player, key)
                        var message = sender.asLangText("PlayerData_Remove", player.name, key)
                        sender.sendStringAsComponent(message)
                    }
                }
            }
        }
    }


    @CommandBody
    val set = subCommand {
        player("player") {
            dynamic("id") {
                dynamic("value") {
                    execute<CommandSender> { sender, context, argument ->
                        submitAsync {
                            val dvalue = context["value"]
                            val tabooPlayer = context.player("player")
                            // 转化为Bukkit的Player
                            val player = tabooPlayer.castSafely<Player>() ?: run {
                                sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                                return@submitAsync
                            }
                            val key = context["id"]
                            DataUtil.setData(player, key, dvalue)
                            var message = sender.asLangText("PlayerData_Modify", player.name, key, dvalue)
                            sender.sendStringAsComponent(message)
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val clear = subCommand {
        execute<CommandSender> { sender, context, argument ->
            submitAsync {
                val player = sender as? Player? ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@submitAsync
                }
                DataUtil.resetAllData(player)
                var message = sender.asLangText("PlayerData_Clear", player.name)
                sender.sendStringAsComponent(message)
            }

        }
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val tabooPlayer = context.player("player")
                    // 转化为Bukkit的Player
                    val player = tabooPlayer.castSafely<Player>() ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                        return@submitAsync
                    }
                    DataUtil.resetAllData(player)
                    var message = sender.asLangText("PlayerData_Clear", player.name)
                    sender.sendStringAsComponent(message)
                }

            }
        }
    }

    @CommandBody
    val getCooldown = subCommand {
        dynamic("id") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val player = sender as? Player? ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    val data = DataUtil.getCooldownLeftLong(player, key)?.toDouble()?.div(1000)
                    var message = sender.asLangText("PlayerCooldown_Fetch", player.name, key, data ?: "<i>null")
                    sender.sendStringAsComponent(message)
                }
            }
            player("player") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        // 转化为Bukkit的Player
                        val player = tabooPlayer.castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                            return@submitAsync
                        }
                        val key = context["id"]
                        val data = DataUtil.getCooldownLeftLong(player, key)?.toDouble()?.div(1000)
                        var message = sender.asLangText("PlayerCooldown_Fetch", player.name, key, data ?: "<i>null")
                        sender.sendStringAsComponent(message)
                    }
                }
            }
        }
    }


    @CommandBody
    val removeCooldown = subCommand {
        dynamic("id") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val player = sender as? Player? ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    DataUtil.resetCooldown(player, key)
                    var message = sender.asLangText("PlayerCooldown_Remove", player.name, key)
                    sender.sendStringAsComponent(message)
                }
            }
            player("player") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val tabooPlayer = context.player("player")
                        // 转化为Bukkit的Player
                        val player = tabooPlayer.castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                            return@submitAsync
                        }
                        val key = context["id"]
                        DataUtil.resetCooldown(player, key)
                        var message = sender.asLangText("PlayerCooldown_Remove", player.name, key)
                        sender.sendStringAsComponent(message)
                    }
                }
            }
        }
    }


    @CommandBody
    val setCooldown = subCommand {
        player("player") {
            dynamic("id") {
                dynamic("value") {
                    execute<CommandSender> { sender, context, argument ->
                        submitAsync {
                            val dvalue = context["value"]
                            val tabooPlayer = context.player("player")
                            // 转化为Bukkit的Player
                            val player = tabooPlayer.castSafely<Player>() ?: run {
                                sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                                return@submitAsync
                            }
                            val key = context["id"]
                            DataUtil.setCooldown(player, key, dvalue.toDouble())
                            var message = sender.asLangText("PlayerCooldown_Modify", player.name, key, dvalue)
                            sender.sendStringAsComponent(message)
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val clearCooldown = subCommand {
        execute<CommandSender> { sender, context, argument ->
            submitAsync {
                val player = sender as? Player? ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@submitAsync
                }
                DataUtil.resetAllCooldown(player)
                var message = sender.asLangText("PlayerCooldown_Clear", player.name)
                sender.sendStringAsComponent(message)
            }

        }
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val tabooPlayer = context.player("player")
                    // 转化为Bukkit的Player
                    val player = tabooPlayer.castSafely<Player>() ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                        return@submitAsync
                    }
                    DataUtil.resetAllCooldown(player)
                    var message = sender.asLangText("PlayerCooldown_Clear", player.name)
                    sender.sendStringAsComponent(message)
                }

            }
        }
    }

    @CommandBody
    val browse = subCommand {
        execute<CommandSender> { sender, context, argument ->
            submitAsync {
                val player = sender as? Player? ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@submitAsync
                }

                sender.sendStringAsComponent(browseDataMap(sender, player.name, DataUtil.getAllDataRaw(player)))
            }

        }
        player("player") {
            execute<CommandSender> { sender, context, argument ->
                submitAsync {
                    val tabooPlayer = context.player("player")
                    // 转化为Bukkit的Player
                    val player = tabooPlayer.castSafely<Player>() ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                        return@submitAsync
                    }
                    sender.sendStringAsComponent(browseDataMap(sender, player.name, DataUtil.getAllDataRaw(player)))
                }

            }
        }
    }



    fun browseDataMap(sender: CommandSender, name: String, map: Map<String, String>): String {
        var str = sender.asLangText("PlayerData_Browse_Title", name)
        for (entry in map) {
            str += "<br>${sender.asLangText("PlayerData_Browse_Section", entry.key, entry.value)}"
        }
        if (map.isEmpty()) sender.sendStringAsComponent(sender.asLangText("PlayerData_Browse_Empty"))
        return str
            .replace("cooldown.", "<#ffcc66><u>cooldown.</u></#ffcc66>")
            .replace("level.", "<#ff9966><u>level.</u></#ff9966>")
            .replace("exp.", "<#66ffcc><u>exp.</u></#66ffcc>")
    }
}
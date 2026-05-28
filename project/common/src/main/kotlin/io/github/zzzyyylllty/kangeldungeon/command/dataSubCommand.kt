package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.data.DataUtil
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
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
    name = "kangeldungeondata",
    aliases = ["dd", "dungeondata"],
    permission = "kangeldungeon.command.data",
    description = "DATA Command of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
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
                    val player = sender as? Player ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    val data = DataUtil.getDataRaw(player, key)
                    val message = sender.asLangText("PlayerDataFetch", player.name, key, data ?: "<i>null")
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
                        val message = sender.asLangText("PlayerDataFetch", player.name, key, data ?: "<i>null")
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
                    val player = sender as? Player ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    DataUtil.removeData(player, key)
                    val message = sender.asLangText("PlayerDataRemove", player.name, key)
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
                        val message = sender.asLangText("PlayerDataRemove", player.name, key)
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
                            val message = sender.asLangText("PlayerDataModify", player.name, key, dvalue)
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
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@submitAsync
                }
                DataUtil.resetAllData(player)
                val message = sender.asLangText("PlayerDataClear", player.name)
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
                    val message = sender.asLangText("PlayerDataClear", player.name)
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
                    val player = sender as? Player ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    val data = DataUtil.getCooldownLeftLong(player, key)?.toDouble()?.div(1000)
                    val message = sender.asLangText("PlayerCooldownFetch", player.name, key, data ?: "<i>null")
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
                        val message = sender.asLangText("PlayerCooldownFetch", player.name, key, data ?: "<i>null")
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
                    val player = sender as? Player ?: run {
                        sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                        return@submitAsync
                    }
                    val key = context["id"]
                    DataUtil.resetCooldown(player, key)
                    val message = sender.asLangText("PlayerCooldownRemove", player.name, key)
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
                        val message = sender.asLangText("PlayerCooldownRemove", player.name, key)
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
                            val cooldownValue = try {
                                dvalue.toDouble()
                            } catch (e: NumberFormatException) {
                                sender.sendStringAsComponent(sender.asLangText("NumberExpected"))
                                return@submitAsync
                            }
                            DataUtil.setCooldown(player, key, cooldownValue)
                            val message = sender.asLangText("PlayerCooldownModify", player.name, key, dvalue)
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
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@submitAsync
                }
                DataUtil.resetAllCooldown(player)
                val message = sender.asLangText("PlayerCooldownClear", player.name)
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
                    val message = sender.asLangText("PlayerCooldownClear", player.name)
                    sender.sendStringAsComponent(message)
                }

            }
        }
    }

    @CommandBody
    val browse = subCommand {
        execute<CommandSender> { sender, context, argument ->
            submitAsync {
                val player = sender as? Player ?: run {
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
        if (map.isEmpty()) return sender.asLangText("PlayerDataBrowseEmpty")
        var str = sender.asLangText("PlayerDataBrowseTitle", name)
        for (entry in map) {
            str += "<br>${sender.asLangText("PlayerDataBrowseSection", entry.key, entry.value)}"
        }
        return str
            .replace("cooldown.", "<#ffcc66><u>cooldown.</u></#ffcc66>")
            .replace("level.", "<#ff9966><u>level.</u></#ff9966>")
            .replace("exp.", "<#66ffcc><u>exp.</u></#66ffcc>")
    }
}
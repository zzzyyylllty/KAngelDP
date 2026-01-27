package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.embiancomponent.EmbianComponent.SafetyComponentSetter
import io.github.zzzyyylllty.embiancomponent.tools.getComponentsNMSFiltered
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.levels
import io.github.zzzyyylllty.kangeldungeon.data.DataUtil
import io.github.zzzyyylllty.kangeldungeon.data.PlayerDataManager
import io.github.zzzyyylllty.kangeldungeon.logger.infoS
import io.github.zzzyyylllty.kangeldungeon.logger.infoS
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.logger.severeS
import io.github.zzzyyylllty.kangeldungeon.util.devLog
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
import taboolib.expansion.getDataContainer
import taboolib.module.nms.NMSItemTag.Companion.asNMSCopy
import taboolib.module.nms.getItemTag
import taboolib.platform.util.asLangText
import taboolib.platform.util.giveItem
import kotlin.run
import kotlin.text.replace
import kotlin.text.toDouble


@CommandHeader(
    name = "kangeldungeonlevel",
    aliases = ["rpglevel"],
    permission = "kangeldungeon.command.level",
    description = "level Command of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = true,
)
object LevelCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    @CommandBody
    val setLevel = subCommand {
        dynamic("key") {
            dynamic("value") {
                execute<CommandSender> { sender, context, argument ->

                    val key = context["key"]
                    val value = context["value"]
                    val player = context.option("player", "p")?.let {
                        devLog("Finding player: $it")
                        Bukkit.getPlayer(it)
                    } ?: sender as? Player? ?: run {
                        sender.severeS(sender.asLangText("Player_Only_Command"))
                        return@execute
                    }
                    val dataContainer = player.getDataContainer()
                    val isSilent = context.option("silent", "s") == ""
                    val isConsoleSilent = context.option("consolesilent", "cs", "csilent") == ""

                    submitAsync {
                        val final = PlayerDataManager(dataContainer, player).setLevel(key, value.toLong())
                        if (final == null) {
                            if (!isConsoleSilent) sender.infoS(sender.asLangText("Operation_Cancelled"), false)
                        } else {
                            if (!isConsoleSilent) sender.infoS(
                                sender.asLangText(
                                    "PlayerLevel_Reset_Console",
                                    player.name,
                                    key,
                                    final
                                ), false
                            )
                            if (!isSilent) player.sendStringAsComponent(
                                player.asLangText(
                                    "PlayerLevel_Reset_Player",
                                    key,
                                    final
                                )
                            )
                        }
                    }
                }
            }
            suggestion<CommandSender>(uncheck = true) { sender, context ->
                levels.keys.asList()
            }
        }
    }

    @CommandBody
    val addLevel = subCommand {
        dynamic("key") {
            dynamic("value") {
                execute<CommandSender> { sender, context, argument ->

                    val key = context["key"]
                    val value = context["value"]
                    val player = context.option("player", "p")?.let { Bukkit.getPlayer(it) } ?: sender as? Player? ?: run {
                        sender.severeS(sender.asLangText("Player_Only_Command"))
                        return@execute
                    }
                    val dataContainer = player.getDataContainer()
                    val isSilent = context.option("silent", "s") == ""
                    val isConsoleSilent = context.option("consolesilent", "cs", "csilent") == ""

                    submitAsync {
                        val final = PlayerDataManager(dataContainer, player).addLevel(key, value.toLong())
                        if (final == null) {
                            if (!isConsoleSilent) sender.infoS(sender.asLangText("Operation_Cancelled"), false)
                        } else {
                            if (!isConsoleSilent) sender.infoS(
                                sender.asLangText(
                                    "PlayerLevel_Modify_Console",
                                    player.name,
                                    key,
                                    final
                                ), false
                            )
                            if (!isSilent) player.sendStringAsComponent(
                                player.asLangText(
                                    "PlayerLevel_Modify_Player",
                                    key,
                                    final
                                )
                            )
                        }
                    }
                }
            }
            suggestion<CommandSender>(uncheck = true) { sender, context ->
                levels.keys.asList()
            }
        }
    }

    @CommandBody
    val removeLevel = subCommand {
        dynamic("key") {
            dynamic("value") {
                execute<CommandSender> { sender, context, argument ->

                    val key = context["key"]
                    val value = context["value"]
                    val player = context.option("player", "p")?.let { Bukkit.getPlayer(it) } ?: sender as? Player? ?: run {
                        sender.severeS(sender.asLangText("Player_Only_Command"))
                        return@execute
                    }
                    val dataContainer = player.getDataContainer()
                    val isSilent = context.option("silent", "s") == ""
                    val isConsoleSilent = context.option("consolesilent", "cs", "csilent") == ""

                    submitAsync {
                        val final =
                            PlayerDataManager(dataContainer, player).removeLevel(key, value.toLong())
                        if (final == null) {
                            if (!isConsoleSilent) sender.infoS(
                                sender.asLangText("Operation_Cancelled"),
                                false
                            )
                        } else {
                            if (!isConsoleSilent) sender.infoS(
                                sender.asLangText(
                                    "PlayerLevel_Modify_Console",
                                    player.name,
                                    key,
                                    final
                                ), false
                            )
                            if (!isSilent) player.sendStringAsComponent(
                                player.asLangText(
                                    "PlayerLevel_Modify_Player",
                                    key,
                                    final
                                )
                            )
                        }
                    }
                }
            }
            suggestion<CommandSender>(uncheck = true) { sender, context ->
                levels.keys.asList()
            }
        }
    }

    @CommandBody
    val resetLevel = subCommand {
        dynamic("key") {
            execute<CommandSender> { sender, context, argument ->

                val key = context["key"]
                val player = context.option("player", "p")?.let { Bukkit.getPlayer(it) } ?: sender as? Player? ?: run {
                    sender.severeS(sender.asLangText("Player_Only_Command"))
                    return@execute
                }
                val dataContainer = player.getDataContainer()
                val isSilent = context.option("silent", "s") == ""
                val isConsoleSilent = context.option("consolesilent", "cs", "csilent") == ""

                submitAsync {
                    val final = PlayerDataManager(dataContainer, player).resetLevel(key)
                    if (final == null) {
                        if (!isConsoleSilent) sender.infoS(sender.asLangText("Operation_Cancelled"), false)
                    } else {
                        if (!isConsoleSilent) sender.infoS(
                            sender.asLangText(
                                "PlayerLevel_Reset_Console",
                                player.name,
                                key,
                                final
                            ), false
                        )
                        if (!isSilent) player.sendStringAsComponent(
                            player.asLangText(
                                "PlayerLevel_Reset_Player",
                                key,
                                final
                            )
                        )
                    }
                }
            }
            suggestion<CommandSender>(uncheck = true) { sender, context ->
                levels.keys.asList()
            }
        }
    }


    @CommandBody
    val setExp = subCommand {
        dynamic("key") {
            dynamic("value") {
                execute<CommandSender> { sender, context, argument ->

                    val key = context["key"]
                    val value = context["value"]
                    val player = context.option("player", "p")?.let { Bukkit.getPlayer(it) } ?: sender as? Player? ?: run {
                        sender.severeS(sender.asLangText("Player_Only_Command"))
                        return@execute
                    }
                    val dataContainer = player.getDataContainer()
                    val isSilent = context.option("silent", "s") == ""
                    val isConsoleSilent = context.option("consolesilent", "cs", "csilent") == ""

                    submitAsync {
                        val final = PlayerDataManager(dataContainer, player).setExp(key, value.toDouble())
                        if (final == null) {
                            if (!isConsoleSilent) sender.infoS(sender.asLangText("Operation_Cancelled"), false)
                        } else {
                            if (!isConsoleSilent) sender.infoS(
                                sender.asLangText(
                                    "PlayerExp_Modify_Console",
                                    player.name,
                                    key,
                                    final
                                ), false
                            )
                            if (!isSilent) player.sendStringAsComponent(
                                player.asLangText(
                                    "PlayerExp_Modify_Player",
                                    key,
                                    final
                                )
                            )
                        }
                    }
                }
            }
            suggestion<CommandSender>(uncheck = true) { sender, context ->
                levels.keys.asList()
            }
        }
    }

    @CommandBody
    val addExp = subCommand {
        dynamic("key") {
            dynamic("value") {
                execute<CommandSender> { sender, context, argument ->

                    val key = context["key"]
                    val value = context["value"]
                    val player = context.option("player", "p")?.let { Bukkit.getPlayer(it) } ?: sender as? Player? ?: run {
                        sender.severeS(sender.asLangText("Player_Only_Command"))
                        return@execute
                    }
                    val dataContainer = player.getDataContainer()
                    val isSilent = context.option("silent", "s") == ""
                    val isConsoleSilent = context.option("consolesilent", "cs", "csilent") == ""

                    submitAsync {
                        val final = PlayerDataManager(dataContainer, player).addExp(key, value.toDouble())
                        if (final == null) {
                            if (!isConsoleSilent) sender.infoS(sender.asLangText("Operation_Cancelled"), false)
                        } else {
                            if (!isConsoleSilent) sender.infoS(
                                sender.asLangText(
                                    "PlayerExp_Add_Console",
                                    player.name,
                                    key,
                                    value.toDouble()
                                ), false
                            )
                            if (!isSilent) player.sendStringAsComponent(
                                player.asLangText(
                                    "PlayerExp_Add_Player",
                                    key,
                                    value.toDouble()
                                )
                            )
                        }
                    }
                }
            }
            suggestion<CommandSender>(uncheck = true) { sender, context ->
                levels.keys.asList()
            }
        }
    }

    @CommandBody
    val removeExp = subCommand {
        dynamic("key") {
            dynamic("value") {
                execute<CommandSender> { sender, context, argument ->

                    val key = context["key"]
                    val value = context["value"]
                    val player = context.option("player", "p")?.let { Bukkit.getPlayer(it) } ?: sender as? Player? ?: run {
                        sender.severeS(sender.asLangText("Player_Only_Command"))
                        return@execute
                    }
                    val dataContainer = player.getDataContainer()
                    val isSilent = context.option("silent", "s") == ""
                    val isConsoleSilent = context.option("consolesilent", "cs", "csilent") == ""

                    submitAsync {
                        val final = PlayerDataManager(dataContainer, player).removeExp(key, value.toDouble())
                        if (final == null) {
                            if (!isConsoleSilent) sender.infoS(sender.asLangText("Operation_Cancelled"), false)
                        } else {
                            if (!isConsoleSilent) sender.infoS(
                                sender.asLangText(
                                    "PlayerExp_Remove_Console",
                                    player.name,
                                    key,
                                    value.toDouble()
                                ), false
                            )
                            if (!isSilent) player.sendStringAsComponent(
                                player.asLangText(
                                    "PlayerExp_Remove_Player",
                                    key,
                                    value.toDouble()
                                )
                            )
                        }
                    }
                }
            }

            suggestion<CommandSender>(uncheck = true) { sender, context ->
                levels.keys.asList()
            }
        }
    }

}
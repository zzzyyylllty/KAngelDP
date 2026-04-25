package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import org.bukkit.Bukkit
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
    name = "kangeldungeonmanage",
    aliases = ["dgm", "dungeonm"],
    permission = "kangeldungeon.command.dungeon",
    description = "Dungeon management commands.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = true,
)
object DungeonCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    /**
     * 列出所有可用地牢模板
     */
    @CommandBody
    val templates = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val templates = KAngelDungeon.dungeonTemplates
            if (templates.isEmpty()) {
                sender.sendStringAsComponent("<yellow>没有可用的地牢模板")
                return@execute
            }
            sender.sendStringAsComponent("<gradient:gold:yellow>===== 地牢模板列表 =====</gradient>")
            for ((name, template) in templates) {
                sender.sendStringAsComponent(
                    " <gray>- <gold>${template.displayName}</gold> <dark_gray>($name)</dark_gray>" +
                    " | <gray>玩家人数限制</gray>" +
                    " | <gray>时间限制: ${template.timeLimit?.toInt()}s</gray>"
                )
            }
        }
    }

    /**
     * 创建地牢
     * /kangeldungeon dungeon create <template> [players ...]
     */
    @CommandBody
    val create = subCommand {
        dynamic("template") {
            execute<CommandSender> { sender, context, argument ->
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
            }
            player("player") {
                execute<CommandSender> { sender, context, argument ->
                    submitAsync {
                        val leader = context.player("player").castSafely<Player>() ?: run {
                            sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                            return@submitAsync
                        }
                        val templateName = context["template"]
                        val template = KAngelDungeon.dungeonTemplates[templateName]
                        if (template == null) {
                            sender.sendStringAsComponent("<red>地牢模板 $templateName 不存在!</red>")
                            return@submitAsync
                        }

                        // 解析额外玩家
                        val players = mutableListOf(leader)
                        val playersStr = context["players"]
                        if (playersStr.isNotBlank()) {
                            for (name in playersStr.split(" ")) {
                                val p = Bukkit.getPlayerExact(name)
                                if (p != null && p !in players) players.add(p)
                            }
                        }

                        if (players.isEmpty()) {
                            sender.sendStringAsComponent("<red>没有可加入地牢的玩家!</red>")
                            return@submitAsync
                        }

                        DungeonHelper.createDungeon(templateName, players, leader, emptyMap())
                        sender.sendStringAsComponent("<green>地牢 <gold>$templateName</gold> 已创建!</green>")
                    }
                }
                dynamic("players") {
                    execute<CommandSender> { sender, context, argument ->
                        submitAsync {
                            val leader = context.player("player").castSafely<Player>() ?: run {
                                sender.sendStringAsComponent(sender.asLangText("PlayerNotExist"))
                                return@submitAsync
                            }
                            val templateName = context["template"]
                            val template = KAngelDungeon.dungeonTemplates[templateName]
                            if (template == null) {
                                sender.sendStringAsComponent("<red>地牢模板 $templateName 不存在!</red>")
                                return@submitAsync
                            }

                            // 解析额外玩家
                            val players = mutableListOf(leader)
                            val playersStr = context["players"]
                            if (playersStr.isNotBlank()) {
                                for (name in playersStr.split(" ")) {
                                    val p = Bukkit.getPlayerExact(name)
                                    if (p != null && p !in players) players.add(p)
                                }
                            }

                            if (players.isEmpty()) {
                                sender.sendStringAsComponent("<red>没有可加入地牢的玩家!</red>")
                                return@submitAsync
                            }

                            DungeonHelper.createDungeon(templateName, players, leader, emptyMap())
                            sender.sendStringAsComponent("<green>地牢 <gold>$templateName</gold> 已创建!</green>")
                        }
                    }
                }
            }
        }
    }

    /**
     * 开始地牢
     * /kangeldungeon dungeon start <uuid>
     */
    @CommandBody
    val start = subCommand {
        dynamic("uuid") {
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent("<red>无效的UUID: $uuidStr</red>")
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent("<red>地牢实例不存在: $uuidStr</red>")
                    return@execute
                }
                val success = instance.start()
                if (success) {
                    sender.sendStringAsComponent("<green>地牢 ${instance.templateName} 已开始!</green>")
                } else {
                    sender.sendStringAsComponent("<yellow>地牢 ${instance.templateName} 无法开始，请检查状态</yellow>")
                }
            }
        }
    }

    /**
     * 停止/结束地牢
     * /kangeldungeon dungeon stop <uuid>
     */
    @CommandBody
    val stop = subCommand {
        dynamic("uuid") {
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent("<red>无效的UUID: $uuidStr</red>")
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent("<red>地牢实例不存在: $uuidStr</red>")
                    return@execute
                }
                val success = instance.fail()
                if (success) {
                    sender.sendStringAsComponent("<green>地牢 ${instance.templateName} 已强制结束!</green>")
                } else {
                    sender.sendStringAsComponent("<yellow>地牢 ${instance.templateName} 无法结束</yellow>")
                }
            }
        }
    }

    /**
     * 列出活跃地牢实例
     * /kangeldungeon dungeon list
     */
    @CommandBody
    val list = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val instances = KAngelDungeon.dungeonInstances
            if (instances.isEmpty()) {
                sender.sendStringAsComponent("<yellow>当前没有活跃的地牢实例</yellow>")
                return@execute
            }
            sender.sendStringAsComponent("<gradient:gold:yellow>===== 活跃地牢列表 (${instances.size}) =====</gradient>")
            for ((uuid, instance) in instances) {
                val stateColor = when (instance.state) {
                    DungeonState.PREPARING -> "<yellow>准备中"
                    DungeonState.ACTIVE -> "<green>进行中"
                    DungeonState.COMPLETED -> "<gray>已完成"
                    DungeonState.FAILED -> "<red>已失败"
                }
                val elapsed = instance.getElapsedTime().toInt()
                sender.sendStringAsComponent(
                    " <gray>-</gray> <gold>${instance.templateName}</gold>" +
                    " | $stateColor</reset>" +
                    " | <gray>玩家: ${instance.getAlivePlayerCount()}/${instance.getPlayerCount()}</gray>" +
                    " | <gray>已用: ${elapsed}s</gray>" +
                    " | <dark_gray>$uuid</dark_gray>"
                )
            }
        }
    }

    /**
     * 查看地牢详情
     * /kangeldungeon dungeon info <uuid>
     */
    @CommandBody
    val info = subCommand {
        dynamic("uuid") {
            execute<CommandSender> { sender, context, argument ->
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent("<red>无效的UUID: $uuidStr</red>")
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent("<red>地牢实例不存在: $uuidStr</red>")
                    return@execute
                }
                sender.sendStringAsComponent("<gradient:gold:yellow>===== 地牢详情 =====</gradient>")
                sender.sendStringAsComponent(" <gray>模板: <gold>${instance.templateName}</gold></gray>")
                sender.sendStringAsComponent(" <gray>UUID: <dark_gray>$uuid</dark_gray></gray>")
                sender.sendStringAsComponent(" <gray>状态: <gold>${instance.state.name}</gold></gray>")
                sender.sendStringAsComponent(" <gray>玩家: ${instance.getAlivePlayerCount()}/${instance.getPlayerCount()}</gray>")
                sender.sendStringAsComponent(" <gray>在线玩家: ${instance.getOnlinePlayerNames().joinToString(", ") { "<green>$it</green>" }}</gray>")
                sender.sendStringAsComponent(" <gray>已用时间: ${instance.getElapsedTime().toInt()}s</gray>")
                val template = instance.getTemplate()
                if (template != null) {
                    sender.sendStringAsComponent(" <gray>时间限制: ${template.timeLimit?.toInt() ?: "无限制"}s</gray>")
                }
            }
        }
    }

    /**
     * 加入地牢
     * /kangeldungeon dungeon join <uuid>
     */
    @CommandBody
    val join = subCommand {
        dynamic("uuid") {
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@execute
                }
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent("<red>无效的UUID: $uuidStr</red>")
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent("<red>地牢实例不存在: $uuidStr</red>")
                    return@execute
                }
                val success = instance.addPlayer(player)
                if (success) {
                    sender.sendStringAsComponent("<green>已加入地牢 ${instance.templateName}!</green>")
                } else {
                    sender.sendStringAsComponent("<yellow>无法加入地牢</yellow>")
                }
            }
        }
    }

    /**
     * 离开地牢
     * /kangeldungeon dungeon leave
     */
    @CommandBody
    val leave = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val player = sender as? Player ?: run {
                sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                return@execute
            }
            // 查找玩家所在的地牢
            val instance = KAngelDungeon.dungeonInstances.values.firstOrNull { it.players.contains(player.uniqueId) }
            if (instance == null) {
                sender.sendStringAsComponent("<yellow>你当前不在任何地牢中</yellow>")
                return@execute
            }
            val success = instance.removePlayer(player)
            if (success) {
                sender.sendStringAsComponent("<green>已离开地牢 ${instance.templateName}!</green>")
            } else {
                sender.sendStringAsComponent("<yellow>无法离开地牢</yellow>")
            }
        }
    }

    /**
     * 传送至地牢世界
     * /kangeldungeon dungeon tp <uuid>
     */
    @CommandBody
    val tp = subCommand {
        dynamic("uuid") {
            execute<CommandSender> { sender, context, argument ->
                val player = sender as? Player ?: run {
                    sender.sendStringAsComponent(sender.asLangText("PlayerOnlyCommand"))
                    return@execute
                }
                val uuidStr = context["uuid"]
                val uuid = try { java.util.UUID.fromString(uuidStr) } catch (e: Exception) { null }
                if (uuid == null) {
                    sender.sendStringAsComponent("<red>无效的UUID: $uuidStr</red>")
                    return@execute
                }
                val instance = KAngelDungeon.dungeonInstances[uuid]
                if (instance == null) {
                    sender.sendStringAsComponent("<red>地牢实例不存在: $uuidStr</red>")
                    return@execute
                }
                player.teleport(instance.spawnLocation)
                sender.sendStringAsComponent("<green>已传送至地牢 ${instance.templateName}</green>")
            }
        }
    }
}

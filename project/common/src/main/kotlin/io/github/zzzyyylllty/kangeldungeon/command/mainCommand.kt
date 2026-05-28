package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.reloadCustomConfig
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import taboolib.common.platform.command.component.CommandComponentDynamic
import taboolib.common.platform.command.component.CommandComponentLiteral
import taboolib.module.lang.asLangText
import org.bukkit.command.CommandSender
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.component.CommandComponent
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.runningPlatform
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.platform.util.asLangText

/**
 * Usage: /kangeldungeon
 *          ├── about
 *          ├── status
 *          ├── reload
 *          ├── api         &lt;!---&gt; /dga
 *          │   ├── minimessage &lt;content&gt;
 *          │   ├── eval &lt;script&gt;
 *          │   ├── evalJs &lt;script&gt;
 *          │   ├── evalByPlayer &lt;player&gt; &lt;script&gt;
 *          │   ├── evalSilent &lt;script&gt;
 *          │   └── evalByPlayerSilent &lt;player&gt; &lt;script&gt;
 *          ├── debug       &lt;!---&gt; /dgd
 *          │   ├── getBlockRegenMap
 *          │   ├── getTemplates
 *          │   ├── getInstances
 *          │   ├── getMonsterConfigs &lt;dungeon&gt;
 *          │   ├── getObstacleConfigs &lt;dungeon&gt;
 *          │   ├── getInteractConfigs &lt;dungeon&gt;
 *          │   ├── getPlans &lt;dungeon&gt;
 *          │   ├── getScripts &lt;dungeon&gt;
 *          │   ├── getDevMode / setDevMode &lt;mode&gt;
 *          │   ├── getMemoryInfo
 *          │   └── getConfig &lt;key&gt;
 *          ├── data        &lt;!---&gt; /dd
 *          │   ├── get &lt;id&gt; [player]
 *          │   ├── remove &lt;id&gt; [player]
 *          │   ├── set &lt;player&gt; &lt;id&gt; &lt;value&gt;
 *          │   ├── clear [player]
 *          │   ├── getCooldown &lt;id&gt; [player]
 *          │   ├── removeCooldown &lt;id&gt; [player]
 *          │   ├── setCooldown &lt;player&gt; &lt;id&gt; &lt;value&gt;
 *          │   ├── clearCooldown [player]
 *          │   └── browse [player]
 *          └── dungeon     &lt;!---&gt; /dgm
 *              ├── templates
 *              ├── create &lt;template&gt; [player] [extra...]
 *              ├── start &lt;uuid&gt;
 *              ├── stop &lt;uuid&gt;
 *              ├── complete &lt;uuid&gt;
 *              ├── list
 *              ├── info &lt;uuid&gt;
 *              ├── join &lt;uuid&gt;
 *              ├── leave
 *              ├── tp &lt;uuid&gt;
 *              ├── kick &lt;player&gt;
 *              ├── addplayer &lt;uuid&gt; &lt;player&gt;
 *              └── listplayers &lt;uuid&gt;
 *          └── party       &lt;!---&gt; /kdparty
 *              ├── create
 *              ├── invite &lt;player&gt;
 *              ├── join &lt;teamId&gt;
 *              ├── leave
 *              ├── kick &lt;player&gt;
 *              ├── transfer &lt;player&gt;
 *              ├── disband
 *              ├── info
 *              └── invites
 *          └── admin       &lt;!---&gt; /kda
 *              ├── info
 *              ├── stopall
 *              ├── purge
 *              ├── maintenance [on/off]
 *              ├── save
 *              ├── worlds
 *              ├── unloadworld &lt;world&gt;
 *              ├── playerinfo &lt;player&gt;
 *              ├── blacklist add/remove/list
 *              ├── meta &lt;uuid&gt; list/get/set/add/delete
 *              ├── instance &lt;uuid&gt;
 *              ├── broadcast &lt;uuid&gt; &lt;message&gt;
 *              ├── heal &lt;uuid&gt;
 *              └── kickall
 *
 * */

@CommandHeader(
    name = "kangeldungeon",
    aliases = ["dg", "dungeon"],
    permission = "kangeldungeon.command.main",
    description = "Main Command of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = false,
)
object KAngelDungeonMainCommand {

    @CommandBody
    val about = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendStringAsComponent(sender.asLangText("MainAbout", pluginVersion))
            sender.sendStringAsComponent(sender.asLangText("MainRunningOn", runningPlatform.name, versionId.toString()))
            sender.sendStringAsComponent(sender.asLangText("MainAuthor"))
            sender.sendStringAsComponent(sender.asLangText("MainHelp"))
        }
    }

    @CommandBody
    val status = subCommand {
        execute<CommandSender> { sender, context, argument ->
            val templates = KAngelDungeon.dungeonTemplates.size
            val active = KAngelDungeon.dungeonInstances.count { it.value.state == DungeonState.ACTIVE }
            val preparing = KAngelDungeon.dungeonInstances.count { it.value.state == DungeonState.PREPARING }
            val devMode = KAngelDungeon.devMode
            sender.sendStringAsComponent(sender.asLangText("MainStatusHeader"))
            sender.sendStringAsComponent(sender.asLangText("MainStatusVersion", pluginVersion))
            sender.sendStringAsComponent(sender.asLangText("MainStatusPlatform", runningPlatform.name))
            sender.sendStringAsComponent(sender.asLangText("MainStatusDevMode",
                if (devMode) sender.asLangText("StatusOn") else sender.asLangText("StatusOff")))
            sender.sendStringAsComponent(sender.asLangText("MainStatusTemplates", templates.toString()))
            sender.sendStringAsComponent(sender.asLangText("MainStatusActive", active.toString(), preparing.toString()))
        }
    }

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    /*
    @CommandBody
    val flatHelp = subCommand {
        createTabooLegacyStyleCommandHelper()
    }
    */

    @CommandBody
    val debug = DebugCommand

    @CommandBody
    val data = DataCommand
    @CommandBody
    val api = ApiCommand
    @CommandBody
    val dungeon = DungeonCommand

    @CommandBody
    val party = PartyCommand

    @CommandBody
    val admin = AdminCommand

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, context, argument ->
            sender.sendStringAsComponent(sender.asLangText("MainReloading"))
            try {
                reloadCustomConfig(async = true, onComplete = {
                    sender.sendStringAsComponent(sender.asLangText("MainReloaded"))
                })
            }
            catch (e: Exception) {
                sender.sendStringAsComponent(sender.asLangText("MainReloadFailed", e.message ?: "Unknown"))
                e.printStackTrace()
            }
        }
    }

}


fun CommandComponent.createModernHelper(checkPermissions: Boolean = true) {
    execute<ProxyCommandSender> { sender, context, _ ->
        val command = context.command
        val builder = StringBuilder("<gradient:yellow:aqua>Usage: /${command.name}<gradient>")
        var newline = false

        fun check(children: List<CommandComponent>): List<CommandComponent> {
            // 检查权限
            val filterChildren = if (checkPermissions) {
                children.filter { sender.hasPermission(it.permission) }
            } else {
                children
            }
            // 过滤隐藏
            return filterChildren.filter { it !is CommandComponentLiteral || !it.hidden }
        }

        fun space(space: Int): String {
            return (1..space).joinToString("") { " " }
        }

        fun print(compound: CommandComponent, index: Int, size: Int, offset: Int = 8, level: Int = 0, end: Boolean = false, optional: Boolean = false) {
            var option = optional
            var comment = 0
            when (compound) {
                is CommandComponentLiteral -> {
                    if (size == 1) {
                        builder.append(" ").append("<gradient:#66ccff:#ffffff>${compound.aliases[0]}<>")
                    } else {
                        newline = true
                        builder.appendLine()
                        builder.append(space(offset))
                        if (level > 1) {
                            builder.append(if (end) " " else "<#888888>│")
                        }
                        builder.append(space(level))
                        if (index + 1 < size) {
                            builder.append("<gradient:#888888:#cccccc>├── </gradient>")
                        } else {
                            builder.append("<gradient:#888888:#cccccc>└── </gradient>")
                        }
                        builder.append("<gradient:#66ccff:#ffffff>${compound.aliases[0]}</gradient>")
                    }
                    option = false
                    comment = compound.aliases[0].length
                }
                is CommandComponentDynamic -> {
                    val value = if (compound.comment.startsWith("@")) {
                        sender.asLangText(compound.comment.substring(1))
                    } else {
                        compound.comment
                    }
                    comment = if (compound.optional || option) {
                        option = true
                        builder.append(" ").append("<#66ffcc>[<$value>]")
                        compound.comment.length + 4
                    } else {
                        builder.append(" ").append("<#ffcc66><$value>")
                        compound.comment.length + 2
                    }
                }
            }
            if (level > 0) {
                comment += 1
            }
            val checkedChildren = check(compound.children)
            checkedChildren.forEachIndexed { i, children ->
                // 因 literal 产生新的行
                if (newline) {
                    print(children, i, checkedChildren.size, offset, level + comment, end, option)
                } else {
                    val length = if (offset == 8) command.name.length + 1 else comment + 1
                    print(children, i, checkedChildren.size, offset + length, level, end, option)
                }
            }
        }
        val checkedChildren = check(context.commandCompound.children)
        val size = checkedChildren.size
        checkedChildren.forEachIndexed { index, children ->
            print(children, index, size, end = index + 1 == size)
        }
        builder.lines().forEach {
            sender.castSafely<CommandSender>()?.sendStringAsComponent(it)
        }
    }
}

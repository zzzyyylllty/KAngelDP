package io.github.zzzyyylllty.kangeldungeon.command

import io.github.zzzyyylllty.embiancomponent.EmbianComponent.SafetyComponentSetter
import io.github.zzzyyylllty.embiancomponent.tools.getComponentsNMSFiltered
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.blockRegenMap
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.levels
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.regenTemplates
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.regenTemplatesByBlock
import io.github.zzzyyylllty.kangeldungeon.logger.infoS
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.bool
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.asList
import taboolib.expansion.setupDataContainer
import taboolib.module.nms.NMSItemTag.Companion.asNMSCopy
import taboolib.module.nms.getItemTag
import taboolib.platform.util.giveItem
import kotlin.text.get

@CommandHeader(
    name = "kangeldungeondebug",
    aliases = ["rpgdebug"],
    permission = "kangeldungeon.command.debug",
    description = "DEBUG Command of KAngelDungeon.",
    permissionMessage = "",
    permissionDefault = PermissionDefault.OP,
    newParser = true,
)
object DebugCommand {

    @CommandBody
    val main = mainCommand {
        createModernHelper()
    }

    @CommandBody
    val help = subCommand {
        createModernHelper()
    }

    @CommandBody
    val getBlockRegenMap = subCommand {
        execute<CommandSender> { sender, context, argument ->
            var message = blockRegenMap.toString()
            sender.infoS(message, false)
        }
    }

    @CommandBody
    val getRegenTemplatesByBlock = subCommand {
        execute<CommandSender> { sender, context, argument ->
            var message = regenTemplatesByBlock.toString()
            sender.infoS(message, false)
        }
    }

    @CommandBody
    val getRegenTemplates = subCommand {
        execute<CommandSender> { sender, context, argument ->
            var message = regenTemplates.toString()
            sender.infoS(message, false)
        }
    }

    @CommandBody
    val getLevels = subCommand {
        execute<CommandSender> { sender, context, argument ->
            var message = levels.toString()
            sender.infoS(message, false)
        }
    }
}
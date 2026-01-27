package io.github.zzzyyylllty.kangeldungeon.function

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.consoleSender
import io.github.zzzyyylllty.kangeldungeon.logger.sendStringAsComponent
import io.github.zzzyyylllty.kangeldungeon.util.VersionHelper
import org.bukkit.command.CommandSender
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.runningPlatform
import taboolib.module.lang.asLangText
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.platform.util.asLangText
import kotlin.collections.joinToString

@Awake(LifeCycle.ENABLE)
fun launchText() {

    val premiumDisplayName = if (VersionHelper().isKAngelDungeonPremium) {
        "<gradient:yellow:gold>" + console.asLangText("PremiumVersion")
    } else {
        "<gradient:green:aqua>" + console.asLangText("FreeVersion")
    }

    val specialThanks =
        listOf("MAORI", "NK_XingChen", "Jesuzi", "Blue_ruins(BlueIce)", "Zero", "TheAchu", "CedricHunsen")

    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringAsComponent("""<gradient:#eecc77:#88aacc>|________                    __   .__                 .__""")
    consoleSender.sendStringAsComponent("""<gradient:#eecc77:#88aacc>    \_____  \   __ __   ____  _/  |_ |__|_____   ______  |__|  ____    ____""")
    consoleSender.sendStringAsComponent("""<gradient:#eecc77:#88aacc>/  / \  \ |  |  \_/ __ \ \   __\|  |\__  \  \____ \ |  | /    \ _/ __ \""")
    consoleSender.sendStringAsComponent("""<gradient:#eecc77:#88aacc>/   \_/.  \|  |  /\  ___/  |  |  |  | / __ \_|  |_> >|  ||   |  \\  ___/""")
    consoleSender.sendStringAsComponent("""<gradient:#eecc77:#88aacc>\_____\ \_/|____/  \___  > |__|  |__|(____  /|   __/ |__||___|  / \___  >""")
    consoleSender.sendStringAsComponent("""<gradient:#eecc77:#88aacc>\__>            \/                 \/ |__|             \/      \/""")
    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringWithPrefix("<gold>",consoleSender.asLangText("WelcomeSeries"))
    consoleSender.sendStringWithPrefix("<gold>",consoleSender.asLangText("DesignBy", "<#ff66cc>AkaCandyKAngel</#ff66cc>"))
    consoleSender.sendStringWithPrefix("<gold>",consoleSender.asLangText("SpecialThanks","<aqua>[<dark_aqua>${specialThanks.joinToString("<dark_gray>, </dark_gray>")}<aqua>]"))
    consoleSender.sendStringWithPrefix("<gold>",consoleSender.asLangText("PoweredBy", "<#66ccff>TabooLib <gold>6.2"))
    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringWithPrefix("<#88ccff>", console.asLangText("Welcome1"))
    consoleSender.sendStringWithPrefix("<#88ccff>", console.asLangText("Welcome2", premiumDisplayName, "${pluginVersion}<reset>", "${runningPlatform.name} - $versionId"))
    consoleSender.sendStringAsComponent(" ")
    consoleSender.sendStringWithPrefix("<#66bbff>", console.asLangText("Welcome3", "https://github.com/zzzyyylllty"))
    consoleSender.sendStringWithPrefix("<#66bbff>", console.asLangText("Welcome4", "https://github.com/zzzyyylllty/KAngelDungeon"))
    consoleSender.sendStringWithPrefix("<#66bbff>", console.asLangText("Welcome5", "https://chotengroup.gitbook.io/kangeldungeon"))
    consoleSender.sendStringAsComponent(" ")
    if (VersionHelper().isKAngelDungeonPremium) consoleSender.sendStringWithPrefix("<gradient:red:yellow:green:aqua:light_purple>", console.asLangText("PremiumVersionWelcome", premiumDisplayName))
    consoleSender.sendStringAsComponent(" ")

}

private fun CommandSender.sendStringWithPrefix(prefix: String, message: String) {
    this.sendStringAsComponent(prefix + message)
}
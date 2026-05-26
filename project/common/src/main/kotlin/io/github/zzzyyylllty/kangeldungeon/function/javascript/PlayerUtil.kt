package io.github.zzzyyylllty.kangeldungeon.function.javascript

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times.times
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.function.submit
import taboolib.module.nms.ItemTag
import taboolib.module.nms.getItemTag
import taboolib.module.nms.setItemTag
import java.time.Duration

object PlayerUtil {
    @Suppress("DEPRECATION")
    private fun getPotionEffectType(type: String): PotionEffectType? {
        return PotionEffectType.getByName(type)
    }

    fun addPotionEffect(player: Player, type: String, duration: Int = 30, amplifier: Int = 0, ambient: Boolean = true, particles: Boolean = true, icon: Boolean = true) {
        val effectType = getPotionEffectType(type) ?: return
        submit {
            player.addPotionEffect(
                PotionEffect(
                    effectType,
                    duration,
                    amplifier,
                    ambient,
                    particles,
                    icon
                )
            )
        }
    }
    fun addPotionEffect(player: Player, type: String, duration: Int = 30, amplifier: Int = 0) {
        val effectType = getPotionEffectType(type) ?: return
        submit {
            player.addPotionEffect(
                PotionEffect(
                    effectType,
                    duration,
                    amplifier
                )
            )
        }
    }
    fun removePotionEffect(player: Player, type: String) {
        val effectType = getPotionEffectType(type) ?: return
        submit {
            player.removePotionEffect(effectType)
        }
    }
    fun showTitle(player: Player, title: Component, subTitle: Component, durationIn: Int = 30, duration: Int = 30, durationOut: Int = 30) {
        val fadeIn = Duration.ofMillis(durationIn.toLong() * 50) // Convert ticks to milliseconds
        val stay = Duration.ofMillis(duration.toLong() * 50)
        val fadeOut = Duration.ofMillis(durationOut.toLong() * 50)
        player.showTitle(Title.title(title, subTitle, times(fadeIn, stay, fadeOut)))
    }
}
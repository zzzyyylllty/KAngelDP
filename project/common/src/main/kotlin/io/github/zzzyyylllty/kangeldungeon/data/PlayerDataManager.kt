package io.github.zzzyyylllty.kangeldungeon.data

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.config
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.levels
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonAddExpEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonAddLevelEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonGetLevelEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonLevelUpgradeEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonRemoveExpEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonRemoveLevelEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonResetLevelEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonSetExpEvent
import io.github.zzzyyylllty.kangeldungeon.event.KAngelDungeonSetLevelEvent
import io.github.zzzyyylllty.kangeldungeon.function.kether.getBukkitPlayer
import io.github.zzzyyylllty.kangeldungeon.logger.warningS
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import org.bukkit.entity.Player
import taboolib.expansion.DataContainer
import taboolib.module.kether.ScriptFrame
import kotlin.math.max



class PlayerDataManager(val data: DataContainer, val player: Player) {
    fun getLevelWithoutEvent(levelName: String): Long? {
        return data["level.$levelName"]?.toLong()
    }
    fun getNextLevelExp(levelName: String): Double? {
        return LevelDataManager.getRequiredExp(levelName, player, (getLevel(levelName) ?: 0) + 1)
    }
    fun getLevelExp(levelName: String, level: Long): Double? {
        return LevelDataManager.getRequiredExp(levelName, player, level)
    }
    fun getLevelOrStartWithoutEvent(levelName: String): Long? {
        return data["level.$levelName"]?.toLong() ?: levels[levelName]?.level?.start ?: run {
            warningS("There are No Level levelNamed $levelName")
            null
        }
    }
    fun addLevel(levelName: String, level: Long): Long? {

        val template = levels[levelName]?: run {
            warningS("There are No Level levelNamed $levelName")
            return null
        }
        val event = KAngelDungeonRemoveLevelEvent(levelName, level, player)
        event.call()
        if (!event.isCancelled) {
            val final = limit(template.level.min, (getLevelOrStart(levelName) ?: 0) + event.removeAmount, template.level.getMaxLevel(player))
            data["level.$levelName"] = final
            return final
        }
        return null
    }
    fun removeLevel(levelName: String, level: Long): Long? {
        val template = levels[levelName]?: run {
            warningS("There are No Level levelNamed $levelName")
            return null
        }
        val event = KAngelDungeonRemoveLevelEvent(levelName, level, player)
        event.call()
        if (!event.isCancelled) {
            val final = limit(template.level.min, (getLevelOrStart(levelName) ?: 0) - event.removeAmount, template.level.getMaxLevel(player))
            data["level.$levelName"] = final
            return final
        }
        return null
    }
    fun setLevel(levelName: String, level: Long): Long? {
        val template = levels[levelName]?: run {
            warningS("There are No Level levelNamed $levelName")
            return null
        }
        val event = KAngelDungeonSetLevelEvent(levelName, level, player)
        event.call()
        if (!event.isCancelled) {
            val final = limit(template.level.min, event.level, template.level.getMaxLevel(player))
            data["level.$levelName"] = final
            return final
        }
        return null
    }
    fun resetLevel(levelName: String): Long? {
        val template = levels[levelName]?: run {
            warningS("There are No Level levelNamed $levelName")
            return null
        }
        val event = KAngelDungeonResetLevelEvent(levelName, player)
        event.call()
        if (!event.isCancelled) {
            val final = limit(template.level.min, template.level.start, template.level.getMaxLevel(player))
            data["level.$levelName"] = final
            return final
        }
        return null
    }
    fun getLevel(levelName: String): Long? {
        val event = KAngelDungeonGetLevelEvent(levelName, getLevelWithoutEvent(levelName) ?: return null, player)
        event.call()
        return event.level
    }
    fun checkUpdateLevel(levelName: String) {
        devLog("Checking update for $levelName")
        var required = getNextLevelExp(levelName) ?: return
        var current = getExp(levelName)
        val sourceLevel = getLevel(levelName)
        var cLevel = sourceLevel ?: return
        devLog("required $required current $current CLevel $cLevel")
        if (required <= current) {
            val limit = config.getInt("options.level.update-limit", 10)
            for (i in 1..limit) {
                if (required <= current) {
                    current -= required
                    cLevel++
                    getLevelExp(levelName, cLevel)?.let { required = it }
                } else {
                    break
                }
            }
            val event = KAngelDungeonLevelUpgradeEvent(levelName, sourceLevel, cLevel, current, player)
            event.call()
            setExp(levelName, event.finalExp)
            setLevel(levelName, event.targetLevel)
        }
    }
    fun getLevelDisplay(levelName: String, display: String): String? {
        val level = levels[levelName] ?: run {
            warningS("There are No Level levelNamed $levelName")
            return null
        }
        return level.displayNames[display] ?: level.displayNames["default"]
    }
    fun getLevelRaw(levelName: String): String? {
        val level = levels[levelName] ?: run {
            warningS("There are No Level levelNamed $levelName")
            return null
        }
        return level.displayNames["raw"] ?: level.displayNames["default"]
    }
    fun getLevelOrStart(levelName: String): Long? {
        val event = KAngelDungeonGetLevelEvent(levelName, getLevelOrStartWithoutEvent(levelName) ?: return null, player)
        event.call()
        return event.level
    }
    fun getExp(levelName: String): Double {
        return data["exp.$levelName"]?.toDouble() ?: 0.0
    }
    fun addExp(levelName: String, exp: Double): Double? {
        val event = KAngelDungeonAddExpEvent(levelName, exp, player)
        event.call()
        if (!event.isCancelled) {
            val final = (data["exp.$levelName"]?.toDoubleOrNull() ?: 0.0) + event.exp
            data["exp.$levelName"] = final
            return final
        }
        return null
    }
    fun removeExp(levelName: String, exp: Double): Double? {
        val event = KAngelDungeonRemoveExpEvent(levelName, exp, player)
        event.call()
        if (!event.isCancelled) {
            val final = max((data["exp.$levelName"]?.toDoubleOrNull() ?: 0.0) - event.exp, 0.0)
            data["exp.$levelName"] = final
            return final
        }
        return null
    }
    fun setExp(levelName: String, exp: Double): Double? {
        val event = KAngelDungeonSetExpEvent(levelName, exp, player)
        event.call()
        if (!event.isCancelled) data["exp.$levelName"] = event.exp
        if (!event.isCancelled) {
            val final = event.exp
            data["exp.$levelName"] = final
            return final
        }
        return null
    }
}

object LevelDataManager {

    fun getMinLevel(levelName: String): Long? {
        return levels[levelName]?.level?.min ?: run {
            warningS("There are No Level levelNamed $levelName")
            null
        }
    }
    fun getMaxLevel(levelName: String, player: Player): Long? {
        return levels[levelName]?.level?.getMaxLevel(player) ?: run {
            warningS("There are No Level levelNamed $levelName")
            null
        }
    }
    fun getRequiredExp(levelName: String, player: Player, level: Long): Double? {
        return levels[levelName]?.level?.getRequiredExp(player, level) ?: run {
            warningS("There are No Level levelNamed $levelName")
            null
        }
    }
}


fun limit(min: Long, value: Long, max: Long): Long {
    return if (min > value) min
    else if (max < value) max
    else value
}
package io.github.zzzyyylllty.kangeldungeon.data

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.config
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



fun limit(min: Long, value: Long, max: Long): Long {
    return if (min > value) min
    else if (max < value) max
    else value
}
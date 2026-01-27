package io.github.zzzyyylllty.kangeldungeon.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent


class KAngelDungeonGetLevelEvent(
    val levelName: String,
    var level: Long,
    val player: Player,
) : BukkitProxyEvent()


class KAngelDungeonAddExpEvent(
    val levelName: String,
    var exp: Double,
    val player: Player,
) : BukkitProxyEvent()


class KAngelDungeonRemoveExpEvent(
    val levelName: String,
    var exp: Double,
    val player: Player,
) : BukkitProxyEvent()


class KAngelDungeonSetExpEvent(
    val levelName: String,
    var exp: Double,
    val player: Player,
) : BukkitProxyEvent()


class KAngelDungeonAddLevelEvent(
    val levelName: String,
    var addAmount: Long,
    val player: Player,
) : BukkitProxyEvent()


class KAngelDungeonRemoveLevelEvent(
    val levelName: String,
    var removeAmount: Long,
    val player: Player,
) : BukkitProxyEvent()


class KAngelDungeonSetLevelEvent(
    val levelName: String,
    var level: Long,
    val player: Player,
) : BukkitProxyEvent()

class KAngelDungeonResetLevelEvent(
    val levelName: String,
    val player: Player,
) : BukkitProxyEvent()

class KAngelDungeonLevelUpgradeEvent(
    val levelName: String,
    var sourceLevel: Long,
    var targetLevel: Long,
    var finalExp: Double,
    val player: Player,
) : BukkitProxyEvent()


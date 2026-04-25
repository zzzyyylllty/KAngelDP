package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/** 地牢开始前，可取消 */
class DungeonStartPreEvent(val instance: DungeonInstance, override val allowCancelled: Boolean = true) : BukkitProxyEvent()
/** 地牢开始后，不可取消 */
class DungeonStartPostEvent(val instance: DungeonInstance, override val allowCancelled: Boolean = false) : BukkitProxyEvent()

/** 玩家加入地牢前，可取消 */
class DungeonPlayerJoinPreEvent(val instance: DungeonInstance, val player: Player, override val allowCancelled: Boolean = true) : BukkitProxyEvent()
/** 玩家加入地牢后，不可取消 */
class DungeonPlayerJoinPostEvent(val instance: DungeonInstance, val player: Player, override val allowCancelled: Boolean = false) : BukkitProxyEvent()

/** 玩家退出地牢前，可取消 */
class DungeonPlayerQuitPreEvent(val instance: DungeonInstance, val player: Player, override val allowCancelled: Boolean = true) : BukkitProxyEvent()
/** 玩家退出地牢后，不可取消 */
class DungeonPlayerQuitPostEvent(val instance: DungeonInstance, val player: Player, override val allowCancelled: Boolean = false) : BukkitProxyEvent()

/** 地牢通关成功前，可取消 */
class DungeonCompletePreEvent(val instance: DungeonInstance, override val allowCancelled: Boolean = true) : BukkitProxyEvent()
/** 地牢通关成功后，不可取消 */
class DungeonCompletePostEvent(val instance: DungeonInstance, override val allowCancelled: Boolean = false) : BukkitProxyEvent()

/** 地牢失败前，可取消 */
class DungeonFailPreEvent(val instance: DungeonInstance, override val allowCancelled: Boolean = true) : BukkitProxyEvent()
/** 地牢失败后，不可取消 */
class DungeonFailPostEvent(val instance: DungeonInstance, override val allowCancelled: Boolean = false) : BukkitProxyEvent()

/** 地牢玩家死亡时，不可取消 */
class DungeonPlayerDeathEvent(val instance: DungeonInstance, val player: Player, override val allowCancelled: Boolean = false) : BukkitProxyEvent()

/** 地牢准备倒计时每秒触发一次 */
class DungeonTickEvent(val instance: DungeonInstance, override val allowCancelled: Boolean = false) : BukkitProxyEvent()
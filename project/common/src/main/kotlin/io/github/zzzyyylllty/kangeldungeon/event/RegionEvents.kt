package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.RegionConfig
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/** 玩家进入区域后触发，不可取消 */
class RegionEnterEvent(
    val instance: DungeonInstance,
    val region: RegionConfig,
    val player: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

/** 玩家离开区域后触发，不可取消 */
class RegionLeaveEvent(
    val instance: DungeonInstance,
    val region: RegionConfig,
    val player: Player,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

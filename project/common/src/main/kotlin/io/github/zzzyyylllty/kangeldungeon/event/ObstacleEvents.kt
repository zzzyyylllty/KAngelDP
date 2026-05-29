package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.ObstacleConfig
import taboolib.platform.type.BukkitProxyEvent

/** 障碍物准备前，可取消 */
class ObstaclePreparePreEvent(
    val instance: DungeonInstance,
    val config: ObstacleConfig,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/** 障碍物准备后，不可取消 */
class ObstaclePreparePostEvent(
    val instance: DungeonInstance,
    val config: ObstacleConfig,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

/** 障碍物激活前（栅栏门关闭），可取消 */
class ObstacleActivatePreEvent(
    val instance: DungeonInstance,
    val config: ObstacleConfig,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/** 障碍物激活后，不可取消 */
class ObstacleActivatePostEvent(
    val instance: DungeonInstance,
    val config: ObstacleConfig,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

/** 障碍物打开前（栅栏门打开），可取消 */
class ObstacleOpenPreEvent(
    val instance: DungeonInstance,
    val config: ObstacleConfig,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/** 障碍物打开后，不可取消 */
class ObstacleOpenPostEvent(
    val instance: DungeonInstance,
    val config: ObstacleConfig,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

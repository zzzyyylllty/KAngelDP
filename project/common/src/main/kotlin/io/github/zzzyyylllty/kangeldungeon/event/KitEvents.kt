package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.KitConfig
import io.github.zzzyyylllty.kangeldungeon.data.KitReward
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/** Kit打开前，可取消 */
class KitOpenPreEvent(
    val instance: DungeonInstance,
    val kitName: String,
    val player: Player,
    val config: KitConfig,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/** Kit打开后，不可取消 */
class KitOpenPostEvent(
    val instance: DungeonInstance,
    val kitName: String,
    val player: Player,
    val config: KitConfig,
    val rewards: List<KitReward>,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

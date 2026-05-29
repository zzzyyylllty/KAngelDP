package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.MonsterConfig
import org.bukkit.entity.LivingEntity
import taboolib.platform.type.BukkitProxyEvent

/** 怪物生成前，可取消 */
class MonsterSpawnPreEvent(
    val instance: DungeonInstance,
    val config: MonsterConfig,
    override val allowCancelled: Boolean = true
) : BukkitProxyEvent()

/** 怪物生成后，不可取消 */
class MonsterSpawnPostEvent(
    val instance: DungeonInstance,
    val config: MonsterConfig,
    val entities: List<LivingEntity>,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

/** 怪物组全部清除时，不可取消 */
class MonsterGroupClearEvent(
    val instance: DungeonInstance,
    val config: MonsterConfig,
    override val allowCancelled: Boolean = false
) : BukkitProxyEvent()

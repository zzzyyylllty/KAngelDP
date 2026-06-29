package io.github.zzzyyylllty.kangeldungeon.util.region

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.event.RegionEnterEvent
import io.github.zzzyyylllty.kangeldungeon.event.RegionLeaveEvent
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 区域管理器 - 追踪玩家在区域中的进出状态
 * 通过 PlayerMoveEvent 监听玩家移动，检测进入/离开区域边界时触发对应事件和代理脚本
 */
object RegionManager {

    // worldName -> (playerUUID -> set of regionIds)
    val playerActiveRegions = ConcurrentHashMap<String, ConcurrentHashMap<UUID, MutableSet<String>>>()

    @SubscribeEvent
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val from = event.from
        val to = event.to ?: return

        // 仅当跨越整块时检测（忽略转头/小范围移动）
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) return

        checkPlayerRegions(player)
    }

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        removePlayerFromAll(event.player)
    }

    @SubscribeEvent
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        val player = event.player
        // 触发旧世界的区域离开事件
        fireRegionLeaveForWorld(player, event.from.name)
        // 检查新世界的区域进入
        checkPlayerRegions(player)
    }

    /**
     * 检查玩家当前所在区域，触发进出事件
     */
    private fun checkPlayerRegions(player: Player) {
        val worldName = player.world.name
        val playerUUID = player.uniqueId

        val instance = findDungeonInstanceByWorld(worldName) ?: return
        val regionConfigs = KAngelDungeon.dungeonRegionConfigs[instance.templateName] ?: return
        if (regionConfigs.isEmpty()) return

        val pos = RegionPos(player.location.blockX, player.location.blockY, player.location.blockZ)

        val worldTracking = playerActiveRegions.getOrPut(worldName) { ConcurrentHashMap() }
        val currentRegions = worldTracking.getOrPut(playerUUID) { ConcurrentHashMap.newKeySet() }

        val newRegions: MutableSet<String> = ConcurrentHashMap.newKeySet()

        for ((regionId, config) in regionConfigs) {
            if (isInside(pos, config.from, config.to)) {
                newRegions.add(regionId)
                if (regionId !in currentRegions) {
                    fireRegionEnter(instance, config, player)
                }
            }
        }

        for (regionId in currentRegions) {
            if (regionId !in newRegions) {
                regionConfigs[regionId]?.let { config ->
                    fireRegionLeave(instance, config, player)
                }
            }
        }

        // 复用 currentRegions 集合而非替换引用，避免并发读取时看到空集合
        currentRegions.clear()
        currentRegions.addAll(newRegions)
    }

    private fun fireRegionEnter(instance: DungeonInstance, config: RegionConfig, player: Player) {
        try {
            RegionEnterEvent(instance, config, player).call()
            instance.meta.add("region.enter", 1)
            instance.meta.add("region.enter.${config.id}", 1)
            // 触发 REGION_ENTER 任务
            TaskManager.triggerTasks(instance, "REGION_ENTER", mapOf(
                "playerName" to player.name,
                "player" to player,
                "regionId" to config.id,
                "region" to config
            ))
            config.agent?.onEnter?.let { script ->
                val data = defaultData + mapOf(
                    "instance" to instance,
                    "template" to instance.getTemplate(),
                    "region" to config,
                    "player" to player
                )
                GraalJsUtil.cachedEval(script, data)
            }
        } catch (e: Exception) {
            warningL("WarningRegionAgentFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    private fun fireRegionLeave(instance: DungeonInstance, config: RegionConfig, player: Player) {
        try {
            RegionLeaveEvent(instance, config, player).call()
            instance.meta.add("region.leave", 1)
            instance.meta.add("region.leave.${config.id}", 1)
            // 触发 REGION_LEAVE 任务
            TaskManager.triggerTasks(instance, "REGION_LEAVE", mapOf(
                "playerName" to player.name,
                "player" to player,
                "regionId" to config.id,
                "region" to config
            ))
            config.agent?.onLeave?.let { script ->
                val data = defaultData + mapOf(
                    "instance" to instance,
                    "template" to instance.getTemplate(),
                    "region" to config,
                    "player" to player
                )
                GraalJsUtil.cachedEval(script, data)
            }
        } catch (e: Exception) {
            warningL("WarningRegionAgentFailed", config.id, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 判断坐标点是否在长方体区域内
     */
    fun isInside(pos: RegionPos, from: RegionPos, to: RegionPos): Boolean {
        val minX = minOf(from.x, to.x)
        val maxX = maxOf(from.x, to.x)
        val minY = minOf(from.y, to.y)
        val maxY = maxOf(from.y, to.y)
        val minZ = minOf(from.z, to.z)
        val maxZ = maxOf(from.z, to.z)

        return pos.x in minX..maxX && pos.y in minY..maxY && pos.z in minZ..maxZ
    }

    /**
     * 根据地牢世界名查找对应的地牢实例
     */
    private fun findDungeonInstanceByWorld(worldName: String): DungeonInstance? {
        val instanceUuid = KAngelDungeon.worldInstanceIndex[worldName] ?: return null
        return KAngelDungeon.dungeonInstances[instanceUuid]
    }

    /**
     * 从所有世界区域追踪中移除玩家（触发离开事件）
     */
    fun removePlayerFromAll(player: Player) {
        val uuid = player.uniqueId
        for ((worldName, worldMap) in playerActiveRegions) {
            val regions = worldMap.remove(uuid) ?: continue
            val instance = findDungeonInstanceByWorld(worldName) ?: continue
            val regionConfigs = KAngelDungeon.dungeonRegionConfigs[instance.templateName] ?: continue
            for (regionId in regions) {
                regionConfigs[regionId]?.let { config ->
                    fireRegionLeave(instance, config, player)
                }
            }
        }
    }

    /**
     * 从指定世界的区域追踪中移除玩家
     */
    private fun removePlayerFromWorld(player: Player, worldName: String) {
        playerActiveRegions[worldName]?.remove(player.uniqueId)
    }

    /**
     * 玩家离开世界时，触发所有区域 onLeave
     */
    private fun fireRegionLeaveForWorld(player: Player, worldName: String) {
        val instance = findDungeonInstanceByWorld(worldName) ?: return
        val regionConfigs = KAngelDungeon.dungeonRegionConfigs[instance.templateName] ?: return
        val trackedRegions = playerActiveRegions[worldName]?.remove(player.uniqueId) ?: return

        for (regionId in trackedRegions) {
            regionConfigs[regionId]?.let { config ->
                fireRegionLeave(instance, config, player)
            }
        }
    }

    /**
     * 清理指定世界的区域追踪数据（触发所有玩家离开事件）
     */
    fun clearWorld(worldName: String) {
        val worldMap = playerActiveRegions.remove(worldName) ?: return
        val instance = findDungeonInstanceByWorld(worldName) ?: return
        val regionConfigs = KAngelDungeon.dungeonRegionConfigs[instance.templateName] ?: return
        for ((playerUUID, regions) in worldMap) {
            val player = Bukkit.getPlayer(playerUUID) ?: continue
            for (regionId in regions) {
                regionConfigs[regionId]?.let { config ->
                    fireRegionLeave(instance, config, player)
                }
            }
        }
    }

    /**
     * 获取玩家当前所在的所有区域 ID
     */
    fun getPlayerRegions(worldName: String, playerUUID: UUID): Set<String> {
        return playerActiveRegions[worldName]?.get(playerUUID)?.toSet() ?: emptySet()
    }

    /**
     * 获取区域内的所有在线玩家
     */
    fun getPlayersInRegion(worldName: String, regionId: String): List<Player> {
        val worldMap = playerActiveRegions[worldName] ?: return emptyList()
        return worldMap.filter { (_, regions) -> regionId in regions }
            .mapNotNull { (uuid, _) -> Bukkit.getPlayer(uuid) }
    }

    /**
     * 检查玩家是否在指定区域内
     */
    fun isPlayerInRegion(worldName: String, playerUUID: UUID, regionId: String): Boolean {
        return playerActiveRegions[worldName]?.get(playerUUID)?.contains(regionId) ?: false
    }
}

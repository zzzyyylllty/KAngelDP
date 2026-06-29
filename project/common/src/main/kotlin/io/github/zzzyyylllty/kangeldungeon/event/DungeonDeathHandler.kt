package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DeathConfig
import io.github.zzzyyylllty.kangeldungeon.data.DeathMode
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.data.MaxDeathAction
import io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家死亡事件处理器
 * 支持四种死亡模式：RESPAWN / SPECTATE / POSSESS / LEAVE
 */
object DungeonDeathHandler {

    // 待处理的自动复活定时任务，用于玩家离开或地牢结束时取消
    private val pendingRespawnTasks = ConcurrentHashMap<UUID, PlatformExecutor.PlatformTask>()

    /**
     * 取消指定玩家的待处理复活任务（玩家离开地牢时调用）
     */
    fun cancelRespawnTask(playerUniqueId: UUID) {
        pendingRespawnTasks.remove(playerUniqueId)?.cancel()
    }

    /**
     * 取消地牢实例所有玩家的待处理复活任务（地牢卸载时调用）
     */
    fun cancelAllRespawnTasks(instance: DungeonInstance) {
        val toCancel = pendingRespawnTasks.keys.filter { uuid ->
            uuid in instance.players
        }
        toCancel.forEach { pendingRespawnTasks.remove(it)?.cancel() }
    }

    @SubscribeEvent
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity as? Player ?: return

        val instanceUuid = KAngelDungeon.playerToInstanceIndex[player.uniqueId]
        val instance = if (instanceUuid != null) KAngelDungeon.dungeonInstances[instanceUuid] else null
        if (instance == null || instance.state != DungeonState.ACTIVE) return

        // 标记为死亡
        instance.markPlayerDead(player)

        // 触发 PLAYER_DEATH 任务
        TaskManager.triggerTasks(instance, "PLAYER_DEATH", mapOf(
            "playerName" to player.name,
            "player" to player
        ))

        val template = instance.getTemplate() ?: return
        val deathConfig = template.gameplayGeneral.death
        val miscConfig = template.miscConfig

        // 检查最大死亡次数（按玩家独立计数）
        if (miscConfig.maxDeaths > 0) {
            val deathCount = instance.getMetaAsInt("player.dead.${player.name}") ?: 0
            if (deathCount >= miscConfig.maxDeaths) {
                when (miscConfig.kickOnMaxDeaths) {
                    MaxDeathAction.SPECTATE -> {
                        instance.setSpectateMode(player.name)
                        return
                    }
                    MaxDeathAction.LOBBY -> {
                        instance.forceRemovePlayer(player)
                        return
                    }
                    MaxDeathAction.KICK -> {
                        player.kickPlayer("<red>You have reached the maximum death count for this dungeon.</red>")
                        return
                    }
                }
            }
        }

        when (deathConfig.mode) {
            DeathMode.LEAVE -> handleLeaveMode(instance, player)
            DeathMode.SPECTATE -> handleSpectateMode(instance, player, deathConfig)
            DeathMode.POSSESS -> handlePossessMode(instance, player, deathConfig)
            DeathMode.RESPAWN -> handleRespawnMode(instance, player, deathConfig)
        }
    }

    /**
     * LEAVE 模式：直接退出地牢（死亡强制退出，跳过 onLeave 条件检查）
     */
    private fun handleLeaveMode(instance: DungeonInstance, player: Player) {
        if (!instance.forceRemovePlayer(player)) {
            // forceRemovePlayer 失败时执行兜底清理（例如 PreEvent 被取消）
            cancelRespawnTask(player.uniqueId)
            instance.players.remove(player.uniqueId)
            instance.deadPlayers.remove(player.uniqueId)
            instance.clearPlayerMeta(player)
            DungeonPlayerQuitPostEvent(instance, player).call()
            KAngelDungeon.playerToInstanceIndex.remove(player.uniqueId, instance.uuid)
            val prev = io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper.playerPreviousLocations.remove(player.uniqueId)
            player.teleport(prev ?: Bukkit.getWorlds().first().spawnLocation)
            TaskManager.triggerTasks(instance, "PLAYER_LEAVE", mapOf(
                "playerName" to player.name,
                "player" to player
            ))
        }
    }

    /**
     * SPECTATE 模式：死后进入旁观者模式
     */
    private fun handleSpectateMode(instance: DungeonInstance, player: Player, config: DeathConfig) {
        instance.setSpectateMode(player.name)
    }

    /**
     * POSSESS 模式：死后附身到随机存活队友
     */
    private fun handlePossessMode(instance: DungeonInstance, player: Player, config: DeathConfig) {
        instance.possessPlayer(player.name)
    }

    /**
     * RESPAWN 模式：等待复活（可选自动复活定时器）
     */
    private fun handleRespawnMode(instance: DungeonInstance, player: Player, config: DeathConfig) {
        // 检查是否超出复活次数
        if (config.maxRespawns > 0) {
            val used = instance.getPlayerRespawnCount(player.name)
            if (used >= config.maxRespawns) {
                // 超出复活次数，自动转为 LEVE 模式
                handleLeaveMode(instance, player)
                return
            }
        }

        // 自动复活定时器（可追踪取消，使用重新查找避免捕获 instance 引用）
        if (config.autoRespawnDelay > 0) {
            val playerName = player.name
            val playerUuid = player.uniqueId
            val instanceUuid = instance.uuid
            // 取消该玩家已有的待处理任务（防止重复）
            pendingRespawnTasks.remove(playerUuid)?.cancel()
            val task = submit(delay = config.autoRespawnDelay * 20L) {
                pendingRespawnTasks.remove(playerUuid)
                // 从索引重新查找 instance，避免闭包捕获过期引用导致内存泄漏
                val currentUuid = KAngelDungeon.playerToInstanceIndex[playerUuid]
                val currentInstance = if (currentUuid != null) KAngelDungeon.dungeonInstances[currentUuid] else null
                if (currentInstance != null && playerUuid in currentInstance.deadPlayers && currentInstance.state == DungeonState.ACTIVE) {
                    currentInstance.respawnPlayer(playerName)
                }
            }
            pendingRespawnTasks[playerUuid] = task
        }
    }
}

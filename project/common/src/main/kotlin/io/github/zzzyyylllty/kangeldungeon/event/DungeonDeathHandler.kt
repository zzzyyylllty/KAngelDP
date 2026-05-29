package io.github.zzzyyylllty.kangeldungeon.event

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DeathConfig
import io.github.zzzyyylllty.kangeldungeon.data.DeathMode
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit

/**
 * 玩家死亡事件处理器
 * 支持四种死亡模式：RESPAWN / SPECTATE / POSSESS / LEAVE
 */
object DungeonDeathHandler {

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
            instance.players.remove(player.uniqueId)
            instance.deadPlayers.remove(player.uniqueId)
            DungeonPlayerQuitPostEvent(instance, player).call()
            KAngelDungeon.playerToInstanceIndex.remove(player.uniqueId, instance.uuid)
            val prev = io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper.playerPreviousLocations.remove(player.uniqueId)
            player.teleport(prev ?: Bukkit.getWorlds().first().spawnLocation)
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

        // 自动复活定时器
        if (config.autoRespawnDelay > 0) {
            val playerName = player.name
            submit(delay = config.autoRespawnDelay * 20L) {
                if (player.uniqueId in instance.deadPlayers && instance.state == DungeonState.ACTIVE) {
                    instance.respawnPlayer(playerName)
                }
            }
        }
    }
}

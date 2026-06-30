package io.github.zzzyyylllty.kangeldungeon.util.scoreboard

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.data.ScoreboardLine
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.ScoreboardManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 计分板管理器 — 当玩家进入地牢时自动设置侧边栏计分板，离开时恢复。
 * 支持自定义文本行、PAPI 变量、动态更新。
 */
object ScoreboardManager {

    private val mm = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    /** playerUUID -> 进入地牢前备份的计分板 */
    private val savedScoreboards = ConcurrentHashMap<UUID, Scoreboard>()

    /** playerUUID -> 当前地牢的目标名称（用于快速清除） */
    private val playerObjectives = ConcurrentHashMap<UUID, String>()

    /** instanceUUID -> 上次更新 tick 计数器 */
    private val lastUpdateTick = ConcurrentHashMap<UUID, Int>()

    /**
     * 玩家进入地牢时应用计分板
     */
    fun applyDungeonScoreboard(player: Player, instance: DungeonInstance) {
        val config = instance.getTemplate()?.scoreboard ?: return
        if (!config.enabled) return

        // 备份旧计分板
        savedScoreboards.putIfAbsent(player.uniqueId, player.scoreboard)

        val sbManager: ScoreboardManager = Bukkit.getScoreboardManager()
        val board = sbManager.newScoreboard
        val objName = "kd_${instance.uuid.toString().take(8)}"
        val obj = board.registerNewObjective(objName, "dummy", mm.deserialize(config.title))
        obj.displaySlot = DisplaySlot.SIDEBAR

        playerObjectives[player.uniqueId] = objName
        player.scoreboard = board
        devLog("Applied dungeon scoreboard for ${player.name} in ${instance.templateName}")
    }

    /**
     * 更新玩家计分板（由 tick 或定时任务调用）
     */
    fun updatePlayerScoreboard(player: Player, instance: DungeonInstance) {
        val config = instance.getTemplate()?.scoreboard ?: return
        if (!config.enabled) return

        val board = player.scoreboard
        val objName = playerObjectives[player.uniqueId] ?: return
        val obj = board.getObjective(objName) ?: return

        // 清除旧的计分板条目
        for (entry in board.entries) {
            board.resetScores(entry)
        }

        // 解析变量
        val elapsed = instance.getElapsedTime().toInt()
        val timeStr = "%02d:%02d".format(elapsed / 60, elapsed % 60)
        val template = instance.getTemplate()
        val remaining = template?.let { instance.getRemainingTime(it) }?.toInt()
        val remainingStr = if (remaining != null) "%02d:%02d".format(remaining / 60, remaining % 60) else "∞"
        val stateLabel = when (instance.state) {
            DungeonState.PREPARING -> "准备中"
            DungeonState.ACTIVE -> "进行中"
            DungeonState.COMPLETED -> "通关!"
            DungeonState.FAILED -> "失败"
        }
        val kills = instance.getMetaAsInt("mob.kill") ?: 0
        val deaths = instance.getMetaAsInt("player.dead") ?: 0

        // 从右到左填充行（计分板分数 = 行数 - 索引，从上到下显示）
        val lines = config.lines
        val scoreStart = lines.size
        for ((index, line) in lines.withIndex()) {
            var text = line.text
                .replace("%dungeon_name%", instance.templateName)
                .replace("%state%", stateLabel)
                .replace("%time%", timeStr)
                .replace("%remaining%", remainingStr)
                .replace("%elapsed%", timeStr)
                .replace("%alive%", instance.getAlivePlayerCount().toString())
                .replace("%total%", instance.players.size.toString())
                .replace("%dead%", instance.getDeadPlayerCount().toString())
                .replace("%kills%", kills.toString())
                .replace("%deaths%", deaths.toString())
                .replace("%difficulty%", instance.getDifficulty() ?: "N/A")

            // PAPI 变量支持
            if (line.usePapi) {
                text = setPlaceholders(player, text)
            }

            val displayName = legacySerializer.serialize(mm.deserialize(text))
            // 确保字符串不超 40 字符（MC 计分板限制）
            val display = if (displayName.length > 40) displayName.take(40) else displayName
            val score = scoreStart - index
            obj.getScore(display).score = score
        }
    }

    /**
     * 恢复玩家计分板（离开地牢时）
     */
    fun restorePlayerScoreboard(player: Player) {
        val saved = savedScoreboards.remove(player.uniqueId)
        if (saved != null) {
            player.scoreboard = saved
        } else {
            // 没有备份，设为空计分板
            player.scoreboard = Bukkit.getScoreboardManager().newScoreboard
        }
        playerObjectives.remove(player.uniqueId)
    }

    /**
     * 恢复地牢所有玩家的计分板
     */
    fun restoreAllForInstance(instance: DungeonInstance) {
        for (uuid in instance.players) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            restorePlayerScoreboard(player)
        }
    }

    /**
     * 每 tick 更新所有在线玩家的计分板（由 DungeonHelper tick 调用）
     * 尊重 updateInterval 配置，避免不必要的高频刷新
     */
    fun tick(instance: DungeonInstance) {
        val config = instance.getTemplate()?.scoreboard ?: return
        if (!config.enabled) return

        // 检查更新间隔
        val currentTick = Bukkit.getCurrentTick()
        val lastTick = lastUpdateTick[instance.uuid] ?: 0
        if (currentTick - lastTick < config.updateInterval) return
        lastUpdateTick[instance.uuid] = currentTick

        for (uuid in instance.players) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            updatePlayerScoreboard(player, instance)
        }
    }

    // ===== PAPI 占位符支持 =====

    /**
     * 尝试使用 PAPI 解析变量，无 PAPI 时回退
     */
    private fun setPlaceholders(player: Player, text: String): String {
        if (!text.contains('%')) return text
        return try {
            val papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
            val method = papiClass.getMethod("setPlaceholders", Player::class.java, String::class.java)
            method.invoke(null, player, text) as? String ?: text
        } catch (_: Exception) {
            text
        }
    }
}

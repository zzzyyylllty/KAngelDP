package io.github.zzzyyylllty.kangeldungeon.util.bossbar

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.BossBarColorOption
import io.github.zzzyyylllty.kangeldungeon.data.BossBarConfig
import io.github.zzzyyylllty.kangeldungeon.data.BossBarStyleOption
import io.github.zzzyyylllty.kangeldungeon.data.BossHealthBarConfig
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import io.github.zzzyyylllty.kangeldungeon.util.devLog
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * BossBar 管理器 — 管理每个地牢实例的计时器 BossBar 和 Boss 血量 BossBar。
 * 每个活跃地牢在 tick 中自动更新：计时器 → instanceBars，Boss血量 → bossBars。
 */
object BossBarManager {

    private val mm = MiniMessage.miniMessage()

    /** instanceUuid -> (BossBar) 计时器/信息 BossBar */
    private val instanceBars = ConcurrentHashMap<UUID, BossBar>()

    /** 活跃 Boss 血量 BossBar：无需持久化，随怪物生成/死亡自动创建销毁 */
    private val bossBars = ConcurrentHashMap<String, BossBar>()  // monsterInstanceId -> BossBar

    /**
     * 为地牢实例创建或重置计时器 BossBar
     */
    fun createTimerBar(instance: DungeonInstance) {
        val config = instance.getTemplate()?.bossBar?.timer ?: return
        if (!config.enabled) return

        val bar = instanceBars.computeIfAbsent(instance.uuid) {
            BossBar.bossBar(Component.empty(), 1f, toAdventureColor(config.color), toAdventureStyle(config.style))
        }
        // 为地牢中所有在线玩家显示
        for (uuid in instance.players) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            if (player.isOnline) bar.addViewer(player)
        }
    }

    /**
     * 更新计时器 BossBar（由 tick 调用）
     */
    fun updateTimerBar(instance: DungeonInstance) {
        val config = instance.getTemplate()?.bossBar?.timer ?: return
        if (!config.enabled) return

        val bar = instanceBars[instance.uuid] ?: return
        val elapsed = instance.getElapsedTime().toInt()
        val mins = elapsed / 60
        val secs = elapsed % 60
        val timeStr = "%02d:%02d".format(mins, secs)

        val template = instance.getTemplate()
        val remaining = template?.let { instance.getRemainingTime(it) }?.toInt()
        val remainingStr = if (remaining != null) "%02d:%02d".format(remaining / 60, remaining % 60) else timeStr

        val alive = instance.getAlivePlayerCount()
        val total = instance.players.size

        val stateLabel = when (instance.state) {
            DungeonState.PREPARING -> "准备中"
            DungeonState.ACTIVE -> "进行中"
            DungeonState.COMPLETED -> "通关!"
            DungeonState.FAILED -> "失败"
        }

        val (color, style, titleTemplate) = when (instance.state) {
            DungeonState.PREPARING -> Triple(config.prepColor, config.prepStyle, config.prepTitle)
            DungeonState.COMPLETED -> Triple(config.completeColor, config.completeStyle, config.completeTitle)
            DungeonState.FAILED -> Triple(config.failColor, config.failStyle, config.failTitle)
            else -> Triple(config.color, config.style, config.title)
        }

        val title = titleTemplate
            .replace("%time%", if (instance.state == DungeonState.PREPARING) remainingStr else timeStr)
            .replace("%remaining%", remainingStr)
            .replace("%alive%", alive.toString())
            .replace("%total%", total.toString())
            .replace("%state%", stateLabel)
            .replace("%elapsed%", timeStr)
            .replace("%dungeon_name%", instance.templateName)

        bar.name(mm.deserialize(title))
        bar.color(toAdventureColor(color))
        bar.overlay(toAdventureStyle(style))
        bar.progress((alive.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f))

        // 确保所有在线玩家都能看到
        ensureViewers(bar, instance)
    }

    /**
     * 为 Boss 怪物创建血量 BossBar
     * @param monsterInstanceId 怪物实例 ID（唯一标识）
     * @param displayName 显示名称
     * @param health 当前生命
     * @param maxHealth 最大生命
     * @param color 颜色
     */
    fun createBossHealthBar(
        monsterInstanceId: String,
        displayName: String,
        health: Double,
        maxHealth: Double,
        color: BossBarColorOption = BossBarColorOption.RED,
        style: BossBarStyleOption = BossBarStyleOption.SOLID
    ): BossBar {
        val bar = BossBar.bossBar(
            mm.deserialize("<red>$displayName $health/$maxHealth</red>"),
            (health / maxHealth).coerceIn(0.0, 1.0).toFloat(),
            toAdventureColor(color),
            toAdventureStyle(style)
        )
        bossBars[monsterInstanceId] = bar
        return bar
    }

    /**
     * 更新 Boss 血量 BossBar
     */
    fun updateBossHealthBar(instance: DungeonInstance, monsterInstanceId: String, displayName: String, health: Double, maxHealth: Double) {
        val bar = bossBars[monsterInstanceId] ?: return
        val config = getBossHealthConfig(instance)
        if (config != null && !config.enabled) return
        val title = config?.title
            ?.replace("%boss_name%", displayName)
            ?.replace("%hp%", health.toInt().toString())
            ?.replace("%max_hp%", maxHealth.toInt().toString()) ?: "<red>$displayName ${health.toInt()}/${maxHealth.toInt()}</red>"
        bar.name(mm.deserialize(title))
        bar.progress((health / maxHealth).coerceIn(0.0, 1.0).toFloat())
    }

    /**
     * 为 BossBar 添加观众
     */
    fun addViewer(bar: BossBar, player: Player) {
        bar.addViewer(player)
    }

    /**
     * 为 BossBar 移除观众
     */
    fun removeViewer(bar: BossBar, player: Player) {
        bar.removeViewer(player)
    }

    /**
     * 移除 Boss 血量 BossBar（怪物死亡时调用）
     */
    fun removeBossHealthBar(monsterInstanceId: String) {
        bossBars.remove(monsterInstanceId)?.let { bar -> bar.viewers().toList().forEach { bar.removeViewer(it as Audience) } }
    }

    /**
     * 销毁地牢实例的所有 BossBar（地牢卸载时调用）
     */
    fun destroyInstanceBars(instance: DungeonInstance) {
        instanceBars.remove(instance.uuid)?.let { bar -> bar.viewers().toList().forEach { bar.removeViewer(it as Audience) } }
    }

    /**
     * 确保地牢实例的 BossBar 对所有活跃玩家可见
     */
    private fun ensureViewers(bar: BossBar, instance: DungeonInstance) {
        val viewers = bar.viewers().map { (it as? Player)?.uniqueId }.toSet()
        for (uuid in instance.players) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            if (uuid !in viewers && player.isOnline) {
                bar.addViewer(player)
            }
        }
        // 移除不在该地牢中的观众（使用快照避免在遍历时修改 live 集合）
        val currentViewers = bar.viewers().toList()
        for (v in currentViewers) {
            val p = v as? Player ?: continue
            if (p.uniqueId !in instance.players) {
                bar.removeViewer(p)
            }
        }
    }

    /**
     * 在地牢 tick 中更新所有活跃 BossBar（计时器 + Boss 血量）
     */
    fun tick(instance: DungeonInstance) {
        updateTimerBar(instance)
    }

    private fun toAdventureColor(color: BossBarColorOption): BossBar.Color = when (color) {
        BossBarColorOption.BLUE -> BossBar.Color.BLUE
        BossBarColorOption.GREEN -> BossBar.Color.GREEN
        BossBarColorOption.PINK -> BossBar.Color.PINK
        BossBarColorOption.PURPLE -> BossBar.Color.PURPLE
        BossBarColorOption.RED -> BossBar.Color.RED
        BossBarColorOption.WHITE -> BossBar.Color.WHITE
        BossBarColorOption.YELLOW -> BossBar.Color.YELLOW
    }

    private fun toAdventureStyle(style: BossBarStyleOption): BossBar.Overlay = when (style) {
        BossBarStyleOption.SOLID -> BossBar.Overlay.PROGRESS
        BossBarStyleOption.SEGMENTED_6 -> BossBar.Overlay.NOTCHED_6
        BossBarStyleOption.SEGMENTED_10 -> BossBar.Overlay.NOTCHED_10
        BossBarStyleOption.SEGMENTED_12 -> BossBar.Overlay.NOTCHED_12
        BossBarStyleOption.SEGMENTED_20 -> BossBar.Overlay.NOTCHED_20
    }

    private fun getBossHealthConfig(instance: DungeonInstance): BossHealthBarConfig? {
        return instance.getTemplate()?.bossBar?.bossHealth
    }
}

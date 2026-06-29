package io.github.zzzyyylllty.kangeldungeon.util.papi

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonState
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

/**
 * PAPI 注册入口（软依赖反射调用）
 */
object PapiHook {
    @JvmStatic
    fun register() {
        KAngelDungeonExpansion().register()
    }
}

/**
 * PlaceholderAPI 扩展 —— 暴露 %kangeldungeon_xxx% 占位符
 *
 * 占位符前缀: kangeldungeon
 * 使用示例:
 *   %kangeldungeon_state%             地牢状态 (PREPARING/ACTIVE/COMPLETED/FAILED)
 *   %kangeldungeon_template%          地牢模板名称
 *   %kangeldungeon_players%           玩家数
 *   %kangeldungeon_alive%             存活玩家数
 *   %kangeldungeon_elapsed_formatted% 已用时间 (MM:SS)
 *   %kangeldungeon_remaining%         剩余时间（秒）
 *   %kangeldungeon_mob_kills%         怪物击杀数
 *   %kangeldungeon_player_status%     玩家状态
 *   %kangeldungeon_player_deaths%     玩家死亡次数
 *   %kangeldungeon_meta_<key>%        任意元数据键（点号保留）
 */
class KAngelDungeonExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String = "kangeldungeon"

    override fun getAuthor(): String = "KAngelDungeon"

    override fun getVersion(): String = "1.0.0"

    /** 保留扩展不被 /papi reload 卸载 */
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (params.isBlank()) return null
        return parsePlaceholder(player, params.lowercase())
    }
}

private fun parsePlaceholder(player: Player?, params: String): String? {
    // 快速判断：是否在地牢中
    if (params == "in_dungeon") {
        val p = player ?: return "no"
        return if (KAngelDungeon.playerToInstanceIndex.containsKey(p.uniqueId)) "yes" else "no"
    }

    val instance = findInstance(player) ?: return ""
    val template = instance.getTemplate()

    return when {
        // ===== 实例状态 =====
        params == "state" -> instance.state.name
        params == "template" -> instance.templateName
        params == "display" -> template?.displayName ?: instance.templateName
        params == "players" -> instance.getPlayerCount().toString()
        params == "alive" -> instance.getAlivePlayerCount().toString()
        params == "dead" -> instance.getDeadPlayerCount().toString()
        params == "leader" -> instance.getLeaderName() ?: ""
        params == "difficulty" -> instance.getDifficulty() ?: ""
        params == "world" -> instance.worldName
        params == "online_names" -> instance.getOnlinePlayerNames().joinToString(", ")
        params == "dead_names" -> instance.getDeadPlayerNames().joinToString(", ")

        // ===== 时间相关 =====
        params == "elapsed" -> instance.getElapsedTime().toInt().toString()
        params == "elapsed_formatted" -> instance.getElapsedTimeFormatted()
        params == "remaining" -> formatRemaining(instance)
        params == "remaining_formatted" -> formatRemainingFormatted(instance)

        // ===== 状态判断 =====
        params == "is_active" -> (instance.state == DungeonState.ACTIVE).toYesNo()
        params == "is_preparing" -> (instance.state == DungeonState.PREPARING).toYesNo()
        params == "is_completed" -> (instance.state == DungeonState.COMPLETED).toYesNo()
        params == "is_failed" -> (instance.state == DungeonState.FAILED).toYesNo()
        params == "is_finished" -> instance.isFinished().toYesNo()

        // ===== 模板信息 =====
        params == "time_limit" -> template?.timeLimit?.toInt()?.toString() ?: ""
        params == "prep_time" -> template?.preparationTime?.toInt()?.toString() ?: ""
        params == "min_players" -> template?.gameplayGeneral?.minPlayers?.toString() ?: ""
        params == "max_players" -> template?.gameplayGeneral?.maxPlayers?.toString() ?: ""
        params == "pvp" -> (template?.pvpEnabled == true).toYesNo()
        params == "min_level" -> template?.joinRequirements?.minLevel?.toString() ?: ""

        // ===== 统计信息 =====
        params == "mob_kills" -> instance.meta.getAsInt("mob.kill")?.toString() ?: "0"
        params == "boss_kills" -> instance.meta.getAsInt("boss.kill")?.toString() ?: "0"
        params == "total_deaths" -> instance.meta.getAsInt("player.dead")?.toString() ?: "0"
        params == "dungeon_starts" -> instance.meta.getAsInt("dungeon.start")?.toString() ?: "0"
        params == "dungeon_completes" -> instance.meta.getAsInt("dungeon.complete")?.toString() ?: "0"
        params == "dungeon_fails" -> instance.meta.getAsInt("dungeon.fail")?.toString() ?: "0"
        params == "monster_spawns" -> instance.meta.getAsInt("monster.spawn")?.toString() ?: "0"
        params == "kit_opens" -> instance.meta.getAsInt("kit.open")?.toString() ?: "0"
        params == "region_enters" -> instance.meta.getAsInt("region.enter")?.toString() ?: "0"
        params == "obstacle_activations" -> instance.meta.getAsInt("obstacle.activate")?.toString() ?: "0"

        // ===== 玩家相关 =====
        params == "player_status" -> {
            val name = player?.name ?: return ""
            instance.getPlayerStatus(name)
        }
        params == "player_deaths" -> {
            val name = player?.name ?: return ""
            instance.meta.getAsInt("player.dead.$name")?.toString() ?: "0"
        }

        // ===== 通用元数据 =====
        params.startsWith("meta_") -> {
            val metaKey = params.removePrefix("meta_")
            instance.meta.getAsString(metaKey)
                ?: instance.meta.getAsInt(metaKey)?.toString()
                ?: instance.meta.getAsDouble(metaKey)?.toString()
                ?: ""
        }

        else -> null
    }
}

// ==================== 工具方法 ====================

/**
 * 根据玩家查找其所在的地牢实例
 */
private fun findInstance(player: Player?): DungeonInstance? {
    val p = player ?: return null
    val instanceUuid = KAngelDungeon.playerToInstanceIndex[p.uniqueId] ?: return null
    return KAngelDungeon.dungeonInstances[instanceUuid]
}

private fun Boolean.toYesNo(): String = if (this) "yes" else "no"

private fun formatRemaining(instance: DungeonInstance): String {
    val template = instance.getTemplate() ?: return "0"
    val remaining = instance.getRemainingTime(template) ?: return ""
    return remaining.toInt().toString()
}

private fun formatRemainingFormatted(instance: DungeonInstance): String {
    val template = instance.getTemplate() ?: return "0:00"
    val remaining = instance.getRemainingTime(template) ?: return ""
    val total = remaining.toInt().coerceAtLeast(0)
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}

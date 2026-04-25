package io.github.zzzyyylllty.kangeldungeon.util

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.DependencyHelper
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper.compareNumeric
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper.compareNumericOrString
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper.compareString
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper.resolvePapi
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Location
import org.bukkit.entity.Player

// ==================== 数据模型 ====================

enum class SelectorType {
    ALL,  // @all - 返回所有匹配玩家
    P,    // @p   - 返回距队长最近的一位
    R     // @r   - 返回随机一位
}

enum class ComparisonOperator {
    GE,   // >=
    LE,   // <=
    GT,   // >
    LT,   // <
    EQ,   // = 或 ==
    RANGE // = 且值含 ..（如 1..6，主要用于 distance）
}

data class TargetSelector(
    val type: SelectorType,
    val conditions: List<TargetCondition>
)

sealed class TargetCondition {

    abstract fun matches(player: Player): Boolean

    /** PAPI 占位符条件: papi:player_health>=30 */
    data class Papi(
        val placeholder: String,
        val operator: ComparisonOperator,
        val value: String
    ) : TargetCondition() {
        override fun matches(player: Player): Boolean {
            if (!DependencyHelper.papi) return true // PAPI 未安装时跳过
            val resolved = resolvePapi(player, placeholder) ?: return true
            return compareNumericOrString(resolved, value, operator)
        }
    }

    /** 生命值: health>=30 */
    data class Health(
        val operator: ComparisonOperator,
        val value: Double
    ) : TargetCondition() {
        override fun matches(player: Player): Boolean {
            return compareNumeric(player.health, value, operator)
        }
    }

    /** 距离: distance:1,2,3=1..6 */
    data class Distance(
        val x: Double, val y: Double, val z: Double,
        val min: Double, val max: Double
    ) : TargetCondition() {
        override fun matches(player: Player): Boolean {
            val dist = player.location.distance(Location(player.world, x, y, z))
            return dist in min..max
        }
    }

    /** 经验等级: level>=5 */
    data class Level(
        val operator: ComparisonOperator,
        val value: Int
    ) : TargetCondition() {
        override fun matches(player: Player): Boolean {
            return compareNumeric(player.level.toDouble(), value.toDouble(), operator)
        }
    }

    /** 游戏模式: gamemode==creative */
    data class GameMode(
        val operator: ComparisonOperator,
        val value: String
    ) : TargetCondition() {
        override fun matches(player: Player): Boolean {
            return compareString(player.gameMode.name, value, operator)
        }
    }

    /** 玩家名: name==Steve */
    data class Name(
        val operator: ComparisonOperator,
        val value: String
    ) : TargetCondition() {
        override fun matches(player: Player): Boolean {
            return compareString(player.name, value, operator)
        }
    }

    /** 世界名: world==world_nether */
    data class World(
        val operator: ComparisonOperator,
        val value: String
    ) : TargetCondition() {
        override fun matches(player: Player): Boolean {
            return compareString(player.world.name, value, operator)
        }
    }
}

// ==================== 解析器 ====================

object TargetSelectorHelper {

    private val SELECTOR_REGEX = Regex("^@(all|p|r)\\{(.*)\$")
    private val OPERATOR_REGEX = Regex("\\s*(>=|<=|>|<|==|=)\\s*")

    /**
     * 解析并求值目标选择器
     * @param instance 当前地牢实例
     * @param line 选择器字符串，如 "@all{papi:player_health>=30&&distance:1,2,3=1..6}"
     * @return 匹配的玩家列表
     */
    fun parseLine(instance: DungeonInstance, line: String): List<Player> {
        val selector = parse(line) ?: return emptyList()
        return evaluate(instance, selector)
    }

    /**
     * 解析选择器字符串为结构化对象
     */
    fun parse(selectorStr: String): TargetSelector? {
        val match = SELECTOR_REGEX.matchEntire(selectorStr.trim()) ?: run {
            warningL("WarningTargetSelectorInvalid", selectorStr)
            return null
        }

        val type = when (match.groupValues[1].lowercase()) {
            "all" -> SelectorType.ALL
            "p" -> SelectorType.P
            "r" -> SelectorType.R
            else -> return null
        }

        val conditionsRaw = match.groupValues[2].trim()
        if (conditionsRaw.isEmpty()) {
            return TargetSelector(type, emptyList())
        }

        val conditionStrings = conditionsRaw.split("&&")
        val conditions = mutableListOf<TargetCondition>()

        for (raw in conditionStrings) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue
            val condition = parseCondition(trimmed)
            if (condition != null) {
                conditions.add(condition)
            } else {
                warningL("WarningTargetSelectorConditionInvalid", trimmed)
            }
        }

        return TargetSelector(type, conditions)
    }

    /**
     * 对预解析的 TargetSelector 求值
     */
    fun evaluate(instance: DungeonInstance, selector: TargetSelector): List<Player> {
        val candidates = instance.getOnlinePlayers().filter { player ->
            selector.conditions.all { it.matches(player) }
        }

        return when (selector.type) {
            SelectorType.ALL -> candidates
            SelectorType.P -> {
                if (candidates.isEmpty()) return emptyList()
                val center = instance.getLeader()?.location ?: instance.spawnLocation
                listOfNotNull(candidates.minByOrNull { it.location.distance(center) })
            }
            SelectorType.R -> {
                if (candidates.isEmpty()) return emptyList()
                listOf(candidates.random())
            }
        }
    }

    // ==================== 条件解析 ====================

    private fun parseCondition(raw: String): TargetCondition? {
        return try {
            when {
                raw.startsWith("papi:") -> parsePapi(raw.removePrefix("papi:"))
                raw.startsWith("distance:") -> parseDistance(raw.removePrefix("distance:"))
                else -> parseTypedCondition(raw)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 解析 papi:placeholder>=value */
    private fun parsePapi(rest: String): TargetCondition? {
        val (key, op, value) = splitOperator(rest) ?: return null
        return TargetCondition.Papi(key, op, value)
    }

    /** 解析 distance:x,y,z=min..max */
    private fun parseDistance(rest: String): TargetCondition? {
        val eqIndex = rest.indexOf('=')
        if (eqIndex == -1) return null

        val coordsStr = rest.substring(0, eqIndex)
        val rangeStr = rest.substring(eqIndex + 1)

        val coords = coordsStr.split(",").map { it.trim().toDouble() }
        if (coords.size != 3) return null

        val (min, max) = parseRange(rangeStr) ?: return null
        return TargetCondition.Distance(coords[0], coords[1], coords[2], min, max)
    }

    /** 解析类型条件: health>10, name==Steve 等 */
    private fun parseTypedCondition(raw: String): TargetCondition? {
        val typeEntries = listOf(
            "health" to { rest: String, op: ComparisonOperator ->
                TargetCondition.Health(op, rest.trim().toDouble())
            },
            "level" to { rest: String, op: ComparisonOperator ->
                TargetCondition.Level(op, rest.trim().toInt())
            },
            "gamemode" to { rest: String, op: ComparisonOperator ->
                TargetCondition.GameMode(op, rest.trim())
            },
            "name" to { rest: String, op: ComparisonOperator ->
                TargetCondition.Name(op, rest.trim())
            },
            "world" to { rest: String, op: ComparisonOperator ->
                TargetCondition.World(op, rest.trim())
            }
        )

        for ((typeName, factory) in typeEntries) {
            if (!raw.lowercase().startsWith(typeName)) continue
            val remainder = raw.removePrefix(typeName).trim()
            if (remainder.isEmpty()) continue
            // 验证剩余部分以操作符开头
            if (!remainder.startsWith(">") && !remainder.startsWith("<") && !remainder.startsWith("=") && !remainder.startsWith("!")) continue

            val (_, op, value) = splitOperator(remainder) ?: continue
            return factory(value, op)
        }

        return null
    }

    // ==================== 操作符 & 值解析 ====================

    /**
     * 从字符串中分割出 key、操作符、value
     * 返回 Triple(key, operator, value)
     * 对于 "player_health>=30" → ("player_health", GE, "30")
     * 对于 ">10" → ("", GT, "10")
     */
    private fun splitOperator(input: String): Triple<String, ComparisonOperator, String>? {
        val match = OPERATOR_REGEX.find(input) ?: return null
        val key = input.substring(0, match.range.first)
        val opStr = match.groupValues[1]
        val value = input.substring(match.range.last + 1)

        val op = when (opStr) {
            ">=" -> ComparisonOperator.GE
            "<=" -> ComparisonOperator.LE
            ">" -> ComparisonOperator.GT
            "<" -> ComparisonOperator.LT
            "==" -> ComparisonOperator.EQ
            "=" -> {
                // = 可能是 EQ 也可能是 RANGE（如 distance 的 1..6）
                if (value.contains("..")) ComparisonOperator.RANGE else ComparisonOperator.EQ
            }
            else -> return null
        }

        return Triple(key, op, value)
    }

    /**
     * 解析范围字符串 "1..6" → Pair(1.0, 6.0)
     */
    private fun parseRange(rangeStr: String): Pair<Double, Double>? {
        val parts = rangeStr.split("..")
        if (parts.size != 2) return null
        val min = parts[0].trim().toDouble()
        val max = parts[1].trim().toDouble()
        return Pair(min, max)
    }

    // ==================== 比较工具 ====================

    fun compareNumeric(playerValue: Double, conditionValue: Double, op: ComparisonOperator): Boolean {
        return when (op) {
            ComparisonOperator.GE -> playerValue >= conditionValue
            ComparisonOperator.LE -> playerValue <= conditionValue
            ComparisonOperator.GT -> playerValue > conditionValue
            ComparisonOperator.LT -> playerValue < conditionValue
            ComparisonOperator.EQ -> playerValue == conditionValue
            ComparisonOperator.RANGE -> false // 数值类型不支持 RANGE
        }
    }

    fun compareNumericOrString(playerValue: String, conditionValue: String, op: ComparisonOperator): Boolean {
        val pNum = playerValue.toDoubleOrNull()
        val cNum = conditionValue.toDoubleOrNull()
        return if (pNum != null && cNum != null) {
            compareNumeric(pNum, cNum, op)
        } else {
            compareString(playerValue, conditionValue, op)
        }
    }

    fun compareString(playerValue: String, conditionValue: String, op: ComparisonOperator): Boolean {
        return when (op) {
            ComparisonOperator.EQ -> playerValue.equals(conditionValue, ignoreCase = true)
            // 对于字符串，非 EQ 操作符使用词法比较（不常见但兜底）
            ComparisonOperator.GE -> playerValue.compareTo(conditionValue, ignoreCase = true) >= 0
            ComparisonOperator.LE -> playerValue.compareTo(conditionValue, ignoreCase = true) <= 0
            ComparisonOperator.GT -> playerValue.compareTo(conditionValue, ignoreCase = true) > 0
            ComparisonOperator.LT -> playerValue.compareTo(conditionValue, ignoreCase = true) < 0
            ComparisonOperator.RANGE -> false
        }
    }

    // ==================== PAPI 直接调用 ====================

    fun resolvePapi(player: Player, placeholder: String): String? {
        return try {
            PlaceholderAPI.setPlaceholders(player, "%$placeholder%")
        } catch (e: Exception) {
            null
        }
    }
}

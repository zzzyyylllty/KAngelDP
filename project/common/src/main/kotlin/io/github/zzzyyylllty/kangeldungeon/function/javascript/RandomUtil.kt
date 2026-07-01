package io.github.zzzyyylllty.kangeldungeon.function.javascript

import java.util.Random

/**
 * 随机工具 - 提供 JS 脚本中的随机数/随机选择操作
 */
object RandomUtil {

    private val random = Random()

    /**
     * 生成 [0, bound) 的随机整数
     * JS: RandomUtil.nextInt(10)
     */
    fun nextInt(bound: Int): Int {
        return random.nextInt(bound.coerceAtLeast(1))
    }

    /**
     * 生成 [min, max] 的随机整数（包含两端）
     * JS: RandomUtil.nextIntRange(5, 10)
     */
    fun nextIntRange(min: Int, max: Int): Int {
        return random.nextInt(min, max + 1)
    }

    /**
     * 生成 [0.0, 1.0) 的随机小数
     * JS: RandomUtil.nextDouble()
     */
    fun nextDouble(): Double {
        return random.nextDouble()
    }

    /**
     * 生成 [min, max) 的随机小数
     * JS: RandomUtil.nextDoubleRange(1.5, 5.5)
     */
    fun nextDoubleRange(min: Double, max: Double): Double {
        return random.nextDouble(min, max)
    }

    /**
     * 随机布尔值
     * JS: RandomUtil.nextBoolean()
     */
    fun nextBoolean(): Boolean {
        return random.nextBoolean()
    }

    /**
     * 从列表中随机选一个元素
     * JS: RandomUtil.pick(["a", "b", "c"])
     */
    fun pick(list: List<Any?>): Any? {
        if (list.isEmpty()) return null
        return list[random.nextInt(list.size)]
    }

    /**
     * 随机打乱列表（返回新列表，不修改原列表）
     * JS: RandomUtil.shuffle(list)
     */
    fun shuffle(list: List<Any?>): List<Any?> {
        return list.shuffled(random)
    }

    /**
     * 从列表中随机选 N 个不重复元素
     * JS: RandomUtil.sample(list, 3)
     */
    fun sample(list: List<Any?>, count: Int): List<Any?> {
        return list.shuffled(random).take(count.coerceAtLeast(0))
    }

    /**
     * 加权随机选择：从 Map<元素, 权重> 中按权重随机选一个
     * JS: RandomUtil.weightedPick({diamond: 10, stone: 50, dirt: 100})
     */
    fun weightedPick(weightMap: Map<String, Any?>): String? {
        if (weightMap.isEmpty()) return null
        val entries = weightMap.entries.mapNotNull { (key, value) ->
            val weight = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }?.takeIf { it > 0 } ?: return@mapNotNull null
            key to weight
        }
        if (entries.isEmpty()) return null
        val totalWeight = entries.sumOf { it.second }
        if (totalWeight <= 0.0) return null
        var r = random.nextDouble() * totalWeight
        for ((key, weight) in entries) {
            r -= weight
            if (r <= 0.0) return key
        }
        return entries.last().first
    }

    /**
     * 高斯分布随机数（正态分布）
     * JS: RandomUtil.nextGaussian(0, 1)  — 平均值为0，标准差为1的正态分布
     */
    fun nextGaussian(mean: Double = 0.0, stdDev: Double = 1.0): Double {
        return random.nextGaussian() * stdDev + mean
    }

    /**
     * 从加权 Map 中随机选 N 个不重复元素
     * JS: RandomUtil.weightedPickList({a: 10, b: 30, c: 60}, 2)
     */
    fun weightedPickList(weightMap: Map<String, Any?>, count: Int): List<String> {
        if (weightMap.isEmpty() || count <= 0) return emptyList()
        val pool = weightMap.entries.mapNotNull { (key, value) ->
            val weight = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }?.takeIf { it > 0 } ?: return@mapNotNull null
            key to weight
        }.toMutableList()
        if (pool.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        val n = count.coerceAtMost(pool.size)
        repeat(n) {
            val total = pool.sumOf { it.second }
            if (total <= 0.0) return@repeat
            var r = random.nextDouble() * total
            for (i in pool.indices) {
                r -= pool[i].second
                if (r <= 0.0) {
                    result.add(pool[i].first)
                    pool.removeAt(i)
                    break
                }
            }
        }
        return result
    }
}

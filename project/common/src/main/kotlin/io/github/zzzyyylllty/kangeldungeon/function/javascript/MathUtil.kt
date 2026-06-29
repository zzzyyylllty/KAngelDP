package io.github.zzzyyylllty.kangeldungeon.function.javascript

/**
 * 数学工具 - 提供 JS 脚本中常用的数学运算
 */
object MathUtil {

    /**
     * 将值限制在 [min, max] 范围内
     * JS: MathUtil.clamp(15, 0, 10)  → 10
     */
    fun clamp(value: Double, min: Double, max: Double): Double {
        return value.coerceIn(min, max)
    }

    /**
     * 整数版 clamp
     * JS: MathUtil.clampInt(15, 0, 10)  → 10
     */
    fun clampInt(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }

    /**
     * 线性插值: a + (b - a) * t
     * JS: MathUtil.lerp(0, 100, 0.5)  → 50.0
     */
    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t.coerceIn(0.0, 1.0)
    }

    /**
     * 判断值是否在 [min, max] 范围内
     * JS: MathUtil.inRange(5, 0, 10)  → true
     */
    fun inRange(value: Double, min: Double, max: Double): Boolean {
        return value >= min && value <= max
    }

    /**
     * 向下取整
     * JS: MathUtil.floor(3.7)  → 3
     */
    fun floor(value: Double): Int {
        return kotlin.math.floor(value).toInt()
    }

    /**
     * 向上取整
     * JS: MathUtil.ceil(3.2)  → 4
     */
    fun ceil(value: Double): Int {
        return kotlin.math.ceil(value).toInt()
    }

    /**
     * 四舍五入
     * JS: MathUtil.round(3.5)  → 4
     */
    fun round(value: Double): Long {
        return kotlin.math.round(value).toLong()
    }

    /**
     * 绝对值
     * JS: MathUtil.abs(-5)  → 5
     */
    fun abs(value: Double): Double {
        return kotlin.math.abs(value)
    }

    /**
     * 平方根
     * JS: MathUtil.sqrt(16)  → 4.0
     */
    fun sqrt(value: Double): Double {
        return kotlin.math.sqrt(value)
    }

    /**
     * 将角度转为弧度
     * JS: MathUtil.toRadians(180)  → 3.14159...
     */
    fun toRadians(degrees: Double): Double {
        return Math.toRadians(degrees)
    }

    /**
     * 将弧度转为角度
     * JS: MathUtil.toDegrees(radians)  → 180.0
     */
    fun toDegrees(radians: Double): Double {
        return Math.toDegrees(radians)
    }

    /**
     * 两点间的欧几里得距离
     * JS: MathUtil.distance(x1, y1, z1, x2, y2, z2)
     */
    fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * 两点间的二维距离
     * JS: MathUtil.distance2D(x1, z1, x2, z2)
     */
    fun distance2D(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        val dx = x2 - x1
        val dz = z2 - z1
        return kotlin.math.sqrt(dx * dx + dz * dz)
    }

    /**
     * 百分比计算 (value / max * 100)
     * JS: MathUtil.percent(5, 20)  → 25.0
     */
    fun percent(value: Double, max: Double): Double {
        if (max == 0.0) return 0.0
        return (value / max * 100.0).coerceIn(0.0, 100.0)
    }
}

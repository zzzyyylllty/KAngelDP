# MathUtil 数学工具

在 JS 脚本中通过 `MathUtil` 访问。

---

## 方法列表

### `MathUtil.clamp(value, min, max)`

将浮点值限制在 `[min, max]` 范围内。

```js
MathUtil.clamp(15, 0, 10);   // → 10.0
MathUtil.clamp(-5, 0, 10);   // → 0.0
MathUtil.clamp(7, 0, 10);    // → 7.0
```

### `MathUtil.clampInt(value, min, max)`

整数版 clamp。

```js
MathUtil.clampInt(15, 0, 10);  // → 10
MathUtil.clampInt(-5, 0, 10);  // → 0
```

---

### `MathUtil.lerp(a, b, t)`

线性插值：`a + (b - a) * t`。`t` 自动限制在 0 ~ 1 之间。

```js
MathUtil.lerp(0, 100, 0.5);    // → 50.0
MathUtil.lerp(10, 20, 0.3);    // → 13.0
MathUtil.lerp(0, 100, 1.5);    // → 100.0 (t 被裁剪到 1.0)
```

---

### `MathUtil.inRange(value, min, max)`

判断值是否在 `[min, max]` 范围内。

```js
MathUtil.inRange(5, 0, 10);    // → true
MathUtil.inRange(15, 0, 10);   // → false
```

---

### `MathUtil.floor(value)`

向下取整。

```js
MathUtil.floor(3.7);    // → 3
MathUtil.floor(-3.7);   // → -4
```

### `MathUtil.ceil(value)`

向上取整。

```js
MathUtil.ceil(3.2);     // → 4
MathUtil.ceil(-3.2);    // → -3
```

### `MathUtil.round(value)`

四舍五入。

```js
MathUtil.round(3.5);    // → 4
MathUtil.round(3.4);    // → 3
```

### `MathUtil.abs(value)`

绝对值。

```js
MathUtil.abs(-5);       // → 5.0
MathUtil.abs(5);        // → 5.0
```

### `MathUtil.sqrt(value)`

平方根。

```js
MathUtil.sqrt(16);      // → 4.0
MathUtil.sqrt(2);       // → 1.414...
```

### `MathUtil.toRadians(degrees)`

角度 → 弧度。

```js
MathUtil.toRadians(180);  // → 3.14159...
```

### `MathUtil.toDegrees(radians)`

弧度 → 角度。

```js
MathUtil.toDegrees(Math.PI);  // → 180.0
```

---

### `MathUtil.distance(x1, y1, z1, x2, y2, z2)`

两点间的三维欧几里得距离。

```js
MathUtil.distance(0, 0, 0, 10, 0, 10);   // → ~14.14
```

### `MathUtil.distance2D(x1, z1, x2, z2)`

两点间的二维平面距离。

```js
MathUtil.distance2D(0, 0, 10, 10);  // → ~14.14
```

---

### `MathUtil.percent(value, max)`

百分比计算 `(value / max * 100)`。max 为 0 时返回 0。

```js
MathUtil.percent(5, 20);    // → 25.0
MathUtil.percent(1, 3);     // → 33.33...
MathUtil.percent(0, 100);   // → 0.0
```

---

### `MathUtil.min(...values)`

取最小值（支持任意数量参数）。

```js
MathUtil.min(3, 7, 1, 9);    // → 1.0
MathUtil.min(1.5, 2.3, 0.8); // → 0.8
```

### `MathUtil.max(...values)`

取最大值（支持任意数量参数）。

```js
MathUtil.max(3, 7, 1, 9);    // → 9.0
MathUtil.max(1.5, 2.3, 0.8); // → 2.3
```

### `MathUtil.minInt(...values)`

取整数最小值。

```js
MathUtil.minInt(3, 7, 1, 9);  // → 1
```

### `MathUtil.maxInt(...values)`

取整数最大值。

```js
MathUtil.maxInt(3, 7, 1, 9);  // → 9
```

---

### `MathUtil.map(value, fromMin, fromMax, toMin, toMax)`

将一个范围的值映射到另一个范围。

```js
MathUtil.map(0.5, 0, 1, 0, 100);    // → 50.0
MathUtil.map(50, 0, 100, 0, 1);     // → 0.5
MathUtil.map(0, 0, 100, -50, 50);   // → -50.0
```

---

## 常见用法（实用组合）

**伤害随距离衰减**:
```js
var maxDamage = 50;
var maxRange = 20;
var dist = MathUtil.distance(x1, y1, z1, x2, y2, z2);
var factor = 1 - MathUtil.clamp(dist / maxRange, 0, 1);
var damage = maxDamage * factor;
```

**Boss 血量百分比提示**:
```js
var maxHealth = boss.getMaxHealth();
var currentHealth = boss.getHealth();
var pct = MathUtil.percent(currentHealth, maxHealth);
var message = "Boss 血量: " + MathUtil.round(pct) + "%";
PlayerUtil.sendMessage(player, pct < 30 ? "<red>" + message : "<green>" + message);
```

**根据玩家数量动态调整**:
```js
var baseMobs = 5;
var players = instance.getPlayerCount();
var mobCount = MathUtil.clampInt(MathUtil.floor(baseMobs + players * 1.5), 3, 30);
```

**平滑移动/过渡**:
```js
var progress = 0;
var task = taboolib.common.platform.function.submit(delay = 1, period = 1, Runnable(function() {
    progress += 0.05;
    if (progress >= 1) {
        // 完成
    }
    var value = MathUtil.lerp(start, end, progress);
}));
```

**Boss 阶段判定**:
```js
var hpPct = MathUtil.percent(boss.getHealth(), boss.getMaxHealth());
if (MathUtil.inRange(hpPct, 60, 100)) {
    // 阶段 1
} else if (MathUtil.inRange(hpPct, 30, 59)) {
    // 阶段 2 - 狂暴
} else {
    // 阶段 3 - 濒死
}
```

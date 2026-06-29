# MathUtil 数学工具

在 JS 脚本中通过 `MathUtil` 访问。

---

## 方法列表

### `MathUtil.clamp(value, min, max)`

将浮点值限制在 `[min, max]` 范围内。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 输入值 |
| `min` | `Double` | 最小值 |
| `max` | `Double` | 最大值 |

**返回值**: `Double`

**示例**:
```js
MathUtil.clamp(15, 0, 10);  // → 10.0
MathUtil.clamp(-5, 0, 10);  // → 0.0
```

---

### `MathUtil.clampInt(value, min, max)`

将整数值限制在 `[min, max]` 范围内。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Int` | 输入值 |
| `min` | `Int` | 最小值 |
| `max` | `Int` | 最大值 |

**返回值**: `Int`

---

### `MathUtil.lerp(a, b, t)`

线性插值：`a + (b - a) * t`。

| 参数 | 类型 | 说明 |
|------|------|------|
| `a` | `Double` | 起始值 |
| `b` | `Double` | 结束值 |
| `t` | `Double` | 插值因子（0.0 ~ 1.0） |

**返回值**: `Double`

**示例**:
```js
MathUtil.lerp(0, 100, 0.5);  // → 50.0
```

---

### `MathUtil.inRange(value, min, max)`

判断值是否在 `[min, max]` 范围内。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 输入值 |
| `min` | `Double` | 下界 |
| `max` | `Double` | 上界 |

**返回值**: `Boolean`

---

### `MathUtil.floor(value)`

向下取整。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 输入值 |

**返回值**: `Int`

**示例**: `MathUtil.floor(3.7)` → `3`，`MathUtil.floor(-3.7)` → `-4`

---

### `MathUtil.ceil(value)`

向上取整。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 输入值 |

**返回值**: `Int`

**示例**: `MathUtil.ceil(3.2)` → `4`

---

### `MathUtil.round(value)`

四舍五入。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 输入值 |

**返回值**: `Long`

**示例**: `MathUtil.round(3.5)` → `4`

---

### `MathUtil.abs(value)`

绝对值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 输入值 |

**返回值**: `Double`

---

### `MathUtil.sqrt(value)`

平方根。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 输入值（>= 0） |

**返回值**: `Double`

---

### `MathUtil.toRadians(degrees)`

将角度转为弧度。

| 参数 | 类型 | 说明 |
|------|------|------|
| `degrees` | `Double` | 角度 |

**返回值**: `Double`

---

### `MathUtil.toDegrees(radians)`

将弧度转为角度。

| 参数 | 类型 | 说明 |
|------|------|------|
| `radians` | `Double` | 弧度 |

**返回值**: `Double`

---

### `MathUtil.distance(x1, y1, z1, x2, y2, z2)`

两点间的三维欧几里得距离。

| 参数 | 类型 | 说明 |
|------|------|------|
| `x1/y1/z1` | `Double` | 点1坐标 |
| `x2/y2/z2` | `Double` | 点2坐标 |

**返回值**: `Double`

---

### `MathUtil.distance2D(x1, z1, x2, z2)`

两点间的二维平面距离。

| 参数 | 类型 | 说明 |
|------|------|------|
| `x1/z1` | `Double` | 点1坐标 |
| `x2/z2` | `Double` | 点2坐标 |

**返回值**: `Double`

---

### `MathUtil.percent(value, max)`

百分比计算 `(value / max * 100)`。

| 参数 | 类型 | 说明 |
|------|------|------|
| `value` | `Double` | 当前值 |
| `max` | `Double` | 最大值 |

**返回值**: `Double` — 0.0 ~ 100.0

**示例**:
```js
MathUtil.percent(5, 20);  // → 25.0
```

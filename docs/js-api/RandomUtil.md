# RandomUtil 随机工具

在 JS 脚本中通过 `RandomUtil` 访问。

---

## 方法列表

### `RandomUtil.nextInt(bound)`

生成 `[0, bound)` 的随机整数。

| 参数 | 类型 | 说明 |
|------|------|------|
| `bound` | `Int` | 上界（不含） |

**返回值**: `Int`

---

### `RandomUtil.nextIntRange(min, max)`

生成 `[min, max]` 的随机整数（包含两端）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `min` | `Int` | 最小值 |
| `max` | `Int` | 最大值 |

**返回值**: `Int`

**示例**:
```js
RandomUtil.nextIntRange(5, 10);  // 可能返回 5, 6, 7, 8, 9, 10
```

---

### `RandomUtil.nextDouble()`

生成 `[0.0, 1.0)` 的随机小数。

**返回值**: `Double`

---

### `RandomUtil.nextDoubleRange(min, max)`

生成 `[min, max)` 的随机小数。

| 参数 | 类型 | 说明 |
|------|------|------|
| `min` | `Double` | 最小值 |
| `max` | `Double` | 最大值 |

**返回值**: `Double`

---

### `RandomUtil.nextBoolean()`

随机布尔值。

**返回值**: `Boolean`

---

### `RandomUtil.pick(list)`

从列表中随机选一个元素。

| 参数 | 类型 | 说明 |
|------|------|------|
| `list` | `List<Any?>` | 列表 |

**返回值**: `Any?` — 空列表返回 null

---

### `RandomUtil.shuffle(list)`

随机打乱列表（返回新列表）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `list` | `List<Any?>` | 原列表 |

**返回值**: `List<Any?>` — 打乱后的新列表

---

### `RandomUtil.sample(list, count)`

从列表中随机选 N 个不重复元素。

| 参数 | 类型 | 说明 |
|------|------|------|
| `list` | `List<Any?>` | 列表 |
| `count` | `Int` | 选取数量 |

**返回值**: `List<Any?>`

**示例**:
```js
RandomUtil.sample(["a", "b", "c", "d", "e"], 3);  // 可能返回 ["c", "a", "e"]
```

---

### `RandomUtil.weightedPick(weightMap)`

加权随机选择：从 Map<元素, 权重> 中按权重随机选一个。

| 参数 | 类型 | 说明 |
|------|------|------|
| `weightMap` | `Map<String, Number>` | 元素 -> 权重 映射 |

**返回值**: `String?` — 被选中的元素键名

**示例**:
```js
RandomUtil.weightedPick({diamond: 10, stone: 50, dirt: 100});
// dirt 被选中的概率最高
```

# EntityUtil 实体工具

在 JS 脚本中通过 `EntityUtil` 访问。

---

## 方法列表

### `EntityUtil.isAlive(entity)`

判断实体是否存活。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `LivingEntity` | 生物实体 |

**返回值**: `Boolean`

---

### `EntityUtil.damage(entity, amount)`

对实体造成伤害。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `LivingEntity` | 目标实体 |
| `amount` | `Double` | 伤害值 |

**返回值**: 无

---

### `EntityUtil.remove(entity)`

移除实体。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |

**返回值**: 无

---

### `EntityUtil.getEntitiesNear(worldName, x, y, z, radius)`

获取位置附近的实体列表。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Double` | X 坐标 |
| `y` | `Double` | Y 坐标 |
| `z` | `Double` | Z 坐标 |
| `radius` | `Double` | 搜索半径 |

**返回值**: `List<Entity>`

---

### `EntityUtil.getPlayersNear(worldName, x, y, z, radius)`

获取位置附近的玩家列表。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Double` | X 坐标 |
| `y` | `Double` | Y 坐标 |
| `z` | `Double` | Z 坐标 |
| `radius` | `Double` | 搜索半径 |

**返回值**: `List<Player>`

---

### `EntityUtil.getLivingEntitiesNear(worldName, x, y, z, radius)`

获取位置附近的存活生物列表。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Double` | X 坐标 |
| `y` | `Double` | Y 坐标 |
| `z` | `Double` | Z 坐标 |
| `radius` | `Double` | 搜索半径 |

**返回值**: `List<LivingEntity>`

---

### `EntityUtil.setGlowing(entity, glowing)`

设置实体发光效果。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |
| `glowing` | `Boolean` | 是否发光 |

**返回值**: 无

---

### `EntityUtil.setCustomName(entity, name)`

设置实体自定义名称（MiniMessage）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |
| `name` | `String` | MiniMessage 名称 |

**返回值**: 无

---

### `EntityUtil.getCustomName(entity)`

获取实体自定义名称。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |

**返回值**: `String?` — 无名称时返回 null

---

### `EntityUtil.isMonster(entity)`

判断实体是否为敌对怪物。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |

**返回值**: `Boolean`

---

### `EntityUtil.setPickupDelay(entity, ticks)`

设置掉落物拾取延迟（仅对掉落物实体有效）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体（必须是 Item） |
| `ticks` | `Int` | 延迟 tick |

**返回值**: 无

---

### `EntityUtil.setInvulnerable(entity, invulnerable)`

设置实体是否无敌。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |
| `invulnerable` | `Boolean` | 是否无敌 |

**返回值**: 无

---

### `EntityUtil.setSilent(entity, silent)`

设置实体是否静音（禁用 AI 声音）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |
| `silent` | `Boolean` | 是否静音 |

**返回值**: 无

---

### `EntityUtil.setFireTicks(entity, ticks)`

让实体着火。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |
| `ticks` | `Int` | 着火 tick（>= 0） |

**返回值**: 无

---

### `EntityUtil.getType(entity)`

获取实体类型名。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |

**返回值**: `String` — 如 `"ZOMBIE"`、`"PLAYER"`

---

### `EntityUtil.teleport(entity, x, y, z, worldName?)`

传送实体到坐标。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `entity` | `Entity` | — | 目标实体 |
| `x` | `Double` | — | X 坐标 |
| `y` | `Double` | — | Y 坐标 |
| `z` | `Double` | — | Z 坐标 |
| `worldName` | `String`? | `null` | 世界名，留空则为当前世界 |

**返回值**: 无

---

### `EntityUtil.getUniqueId(entity)`

获取实体的 UUID 字符串。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `Entity` | 目标实体 |

**返回值**: `String` — UUID 字符串

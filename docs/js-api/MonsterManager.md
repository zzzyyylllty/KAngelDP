# MonsterManager 怪物管理器

在 JS 脚本中通过 `MonsterManager` 访问。

---

## 方法列表

### `MonsterManager.spawnMonsters(instance, config)`

生成配置中所有的怪物。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `MonsterConfig` | 怪物配置对象 |

**返回值**: `List<LivingEntity>?` — 生成的实体列表，失败返回 null

---

### `MonsterManager.setMonsterActive(instance, configId, active)`

设置怪物组激活状态。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `configId` | `String` | 怪物配置 ID |
| `active` | `Boolean` | 是否激活 |

**返回值**: 无

---

### `MonsterManager.setMonsterCooldown(instance, configId, cooldownTicks)`

设置怪物组重生冷却。`-1` 恢复为 config 默认值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `configId` | `String` | 怪物配置 ID |
| `cooldownTicks` | `Long` | 冷却 tick 数 |

**返回值**: 无

---

### `MonsterManager.setMonsterActivationRangeMin(instance, configId, value)`

设置怪物组最小激活距离。`null` 恢复为 config 默认值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `configId` | `String` | 怪物配置 ID |
| `value` | `Double?` | 最小距离（方块），null=恢复默认 |

**返回值**: 无

---

### `MonsterManager.setMonsterActivationRangeMax(instance, configId, value)`

设置怪物组最大激活距离。`null` 恢复为 config 默认值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `configId` | `String` | 怪物配置 ID |
| `value` | `Double?` | 最大距离（方块），null=恢复默认 |

**返回值**: 无

---

### `MonsterManager.resetMonsterActivationRange(instance, configId)`

重置怪物组激活距离为 config 默认值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `configId` | `String` | 怪物配置 ID |

**返回值**: 无

---

### `MonsterManager.getMonsterInstances(instance)`

获取地牢所有活跃的怪物组实例。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |

**返回值**: `Map<String, MonsterInstance>` — (configId -> MonsterInstance)

---

### `MonsterManager.removeEntityTracking(entityIds)`

移除指定实体的击杀追踪（用于非正常移除场景）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entityIds` | `Set<UUID>` | 要移除追踪的实体 UUID 集合 |

**返回值**: 无

# ObstacleManager 障碍物管理器

在 JS 脚本中通过 `ObstacleManager` 访问。

---

## 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `ObstacleManager.activeObstacles` | `Map<String, MutableSet<ObstacleInstance>>` | 所有活跃的障碍物实例，按 worldName 索引 |

---

## 方法列表

### `ObstacleManager.prepareObstacle(instance, config)`

准备障碍物（预先保存方块状态、预留位置）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `ObstacleConfig` | 障碍物配置 |

**返回值**: `Boolean` — 是否成功准备

---

### `ObstacleManager.activateObstacle(instance, config)`

激活障碍物（关闭栅栏门，放置方块阻挡玩家）。
支持 `openDelaySeconds` 延迟激活和 `activeDurationSeconds` 自动关闭。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `ObstacleConfig` | 障碍物配置 |

**返回值**: `Boolean` — 是否成功激活

---

### `ObstacleManager.openObstacle(instance, config)`

打开障碍物（移除方块恢复通行）。已开启过的障碍物不再重复执行。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `ObstacleConfig` | 障碍物配置 |

**返回值**: `Boolean` — 是否成功打开

---

### `ObstacleManager.openObstacleForce(instance, config)`

强制打开障碍物（跳过已开启检查）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `ObstacleConfig` | 障碍物配置 |

**返回值**: `Boolean` — 是否成功打开

---

### `ObstacleManager.restoreBlocks(instance)`

恢复已保存的方块状态（彻底清理障碍物）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |

**返回值**: 无

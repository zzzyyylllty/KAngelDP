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

准备障碍物（预先保存方块状态、预留位置）。触发 `onPrepare` JS 代理。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `ObstacleConfig` | 障碍物配置 |

**返回值**: `Boolean` — 是否成功准备

```js
// 在 PREPARE 阶段的 plan 中预加载障碍物
ObstacleManager.prepareObstacle(instance, config);
```

---

### `ObstacleManager.activateObstacle(instance, config)`

激活障碍物（关闭栅栏门，放置方块阻挡玩家）。触发 `onStart` JS 代理。
支持 `openDelaySeconds` 延迟激活和 `activeDurationSeconds` 自动关闭。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `ObstacleConfig` | 障碍物配置 |

**返回值**: `Boolean` — 是否成功激活

```js
ObstacleManager.activateObstacle(instance, config);
```

---

### `ObstacleManager.openObstacle(instance, config)`

打开障碍物（移除方块恢复通行）。已开启过的障碍物不再重复执行。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `ObstacleConfig` | 障碍物配置 |

**返回值**: `Boolean` — 是否成功打开

```js
// 清除所有怪物后开门
ObstacleManager.openObstacle(instance, config);
```

### `ObstacleManager.openObstacleForce(instance, config)`

强制打开障碍物（跳过已开启检查）。

```js
ObstacleManager.openObstacleForce(instance, config);
```

---

### `ObstacleManager.restoreBlocks(instance)`

恢复已保存的方块状态（彻底清理障碍物）。地牢结束时自动调用。

```js
ObstacleManager.restoreBlocks(instance);
```

---

## 常见用法

**Boss 战锁门**:
```js
// onStart 阶段 — 锁门
ObstacleManager.activateObstacle(instance, bossRoomConfig);

// onAllKilled — 开门
ObstacleManager.openObstacle(instance, bossRoomConfig);
```

**分段关卡**:
```js
var wave = instance.getMetaAsInt("wave");
switch (wave) {
    case 1:
        ObstacleManager.activateObstacle(instance, gate1);
        break;
    case 2:
        ObstacleManager.openObstacle(instance, gate1);
        ObstacleManager.activateObstacle(instance, gate2);
        break;
    case 3:
        ObstacleManager.openObstacle(instance, gate2);
        break;
}
```

**强制重置**:
```js
// 用于管理指令或故障恢复
ObstacleManager.openObstacleForce(instance, stuckGate);
```

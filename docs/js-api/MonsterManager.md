# MonsterManager 怪物管理器

在 JS 脚本中通过 `MonsterManager` 访问。

---

## 方法列表

### `MonsterManager.spawnMonsters(instance, config)`

生成配置中所有的怪物。执行 spawnCondition JS 检查，触发 onSpawn 代理脚本。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `config` | `MonsterConfig` | 怪物配置对象 |

**返回值**: `List<LivingEntity>?` — 生成的实体列表，失败或条件不满足返回 null

```js
var template = instance.getTemplate();
var configs = MonsterManager.getMonsterConfigs ? instance.getMonsterConfigs() : null;
// 通常通过 instance 的方法获取 config
// 这里示例直接 spawn
```

---

### `MonsterManager.setMonsterActive(instance, configId, active)`

设置怪物组激活状态。设为 false 后该组不再参与 tick 检测和重生。

```js
MonsterManager.setMonsterActive(instance, "zombies", false);  // 暂停
MonsterManager.setMonsterActive(instance, "zombies", true);   // 恢复
```

---

### `MonsterManager.setMonsterCooldown(instance, configId, cooldownTicks)`

设置怪物组重生冷却。`-1` 恢复为 config 默认值。

```js
MonsterManager.setMonsterCooldown(instance, "zombies", 600);  // 30 秒冷却
MonsterManager.setMonsterCooldown(instance, "zombies", -1);   // 恢复默认
```

---

### `MonsterManager.setMonsterActivationRangeMin(instance, configId, value)`

设置怪物组最小激活距离。玩家必须超过此距离怪物才会激活。`null` 恢复默认。

```js
MonsterManager.setMonsterActivationRangeMin(instance, "zombies", 3.0);   // 至少 3 格远
MonsterManager.setMonsterActivationRangeMin(instance, "zombies", null);  // 恢复默认
```

---

### `MonsterManager.setMonsterActivationRangeMax(instance, configId, value)`

设置怪物组最大激活距离。玩家超出此距离怪物进入休眠。`null` 恢复默认。

```js
MonsterManager.setMonsterActivationRangeMax(instance, "zombies", 30.0);  // 最远 30 格
MonsterManager.setMonsterActivationRangeMax(instance, "zombies", null);  // 恢复默认
```

---

### `MonsterManager.resetMonsterActivationRange(instance, configId)`

重置怪物组激活距离为 config 默认值（同时重置 min 和 max）。

```js
MonsterManager.resetMonsterActivationRange(instance, "zombies");
```

---

### `MonsterManager.getMonsterInstances(instance)`

获取地牢所有活跃的怪物组实例。

```js
var instances = MonsterManager.getMonsterInstances(instance);
// 返回: { "boss_group": MonsterInstance, "zombies": MonsterInstance }
```

---

### `MonsterManager.removeEntityTracking(entityIds)`

移除指定实体的击杀追踪（用于非正常移除场景，如 `clearAllMobs`）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entityIds` | `Set<UUID>` | 要移除追踪的实体 UUID 集合 |

```js
// 清除所有活跃怪物的追踪
var instances = MonsterManager.getMonsterInstances(instance);
for (var id in instances) {
    var mobInst = instances[id];
    // 不能直接访问内部 spawnedMobs，一般由插件自动处理
}
```

---

## 常见用法

**动态调整怪物难度**:
```js
// 根据玩家数量动态调整激活范围
var playerCount = instance.getPlayerCount();
if (playerCount >= 4) {
    MonsterManager.setMonsterActivationRangeMax(instance, "zombies", 40.0);
    MonsterManager.setMonsterActivationRangeMax(instance, "skeletons", 40.0);
} else {
    MonsterManager.setMonsterActivationRangeMax(instance, "zombies", 20.0);
}
```

**Boss 战阶段切换**:
```js
// 在 onEachKill 或 onAllKilled 中调用
function onBossPhaseChange(instance) {
    MonsterManager.setMonsterActive(instance, "adds", true);  // 激活小怪
    MonsterManager.setMonsterCooldown(instance, "adds", 200); // 10 秒重生
}
```

**暂停所有怪物**:
```js
var instances = MonsterManager.getMonsterInstances(instance);
for (var id in instances) {
    MonsterManager.setMonsterActive(instance, id, false);
}
```

**查看 `MonsterInstance` 属性**:

`MonsterInstance` 对象可用的公共属性：`active`（是否激活）、`config`（MonsterConfig 配置对象）、`allKilled`（是否全灭）、`respawnCount`（已重生次数）。

# TaskManager 任务管理器

在 JS 脚本中通过 `TaskManager` 访问。任务系统用于监听玩家行为（破坏方块、击杀、受伤等）并触发 JS 脚本。

---

## 方法列表

### `TaskManager.getTaskConfigs(instance)`

获取地牢的所有任务配置。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |

**返回值**: `Map<String, TaskConfig>` — 任务配置映射 (taskId -> TaskConfig)

```js
var tasks = TaskManager.getTaskConfigs(instance);
for (var id in tasks) {
    Sys.println("任务: " + id + " 触发器: " + tasks[id].trigger);
}
```

---

### `TaskManager.triggerTasks(instance, trigger, context?)`

根据 trigger 触发地牢中所有匹配的任务。自动检查条件过滤、冷却和最大执行次数。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `trigger` | `String` | 触发器类型 |
| `context` | `Map<String, Any?>`? | 传递给 JS 脚本的额外变量 |

可用的触发器类型：
- `BLOCK_BREAK` — 破坏方块时
- `BLOCK_PLACE` — 放置方块时
- `DAMAGE_TAKEN` — 受到伤害时
- `DAMAGE_DEALT` — 造成伤害时
- `MOB_KILL` — 击杀怪物时
- `MONSTER_SPAWN` — 怪物生成时
- `MONSTER_GROUP_CLEAR` — 怪物组全灭时
- `REGION_ENTER` — 进入区域时
- `REGION_LEAVE` — 离开区域时
- `PLAYER_JOIN` — 玩家加入地牢时
- `PLAYER_LEAVE` — 玩家离开地牢时
- `CUSTOM` — 仅通过 `triggerTask` 手动触发

```js
// 手动触发 CUSTOM 任务
TaskManager.triggerTasks(instance, "CUSTOM", {
    playerName: player.getName(),
    player: player,
    customValue: 42
});
```

---

### `TaskManager.triggerTask(instance, taskId)`

手动触发指定任务，跳过冷却和次数限制。

```js
var ok = TaskManager.triggerTask(instance, "secret_bonus");
if (ok) {
    PlayerUtil.sendMessage(player, "<gold>隐藏任务触发！");
}
```

---

### `TaskManager.getExecutionCount(instance, taskId)`

获取任务的已执行次数。

```js
var count = TaskManager.getExecutionCount(instance, "boss_kill");
if (count >= 3) {
    PlayerUtil.sendMessage(player, "<red>Boss 已被击杀 " + count + " 次");
}
```

---

### `TaskManager.resetExecutionCount(instance, taskId)`

重置任务的已执行次数（需要重新触发的场景）。

```js
TaskManager.resetExecutionCount(instance, "daily_bonus");
```

---

## 任务过滤器说明

在配置任务时可以通过 `filters` 字段精确控制触发条件：

```yaml
tasks:
  kill_zombie:
    trigger: MOB_KILL
    filters:
      mobType: ZOMBIE   # 只在击杀僵尸时触发
    agent:
      onTrigger: |
        player.sendMessage("你杀了一只僵尸");
```

可用的过滤器字段取决于触发器类型：
- `BLOCK_BREAK` / `BLOCK_PLACE`: `blockType`（方块类型名）
- `MOB_KILL`: `mobType`（实体类型名）、`mobName`（自定义名称）
- `DAMAGE_TAKEN`: `damageCause`（伤害类型）
- `DAMAGE_DEALT`: `damageCause`、`targetType`（目标类型）

---

## 常见用法

**获取任务执行统计**:
```js
var tasks = TaskManager.getTaskConfigs(instance);
for (var id in tasks) {
    var count = TaskManager.getExecutionCount(instance, id);
    Sys.println("[Task] " + id + " 执行了 " + count + " 次");
}
```

**重置所有任务计数（新阶段）**:
```js
var tasks = TaskManager.getTaskConfigs(instance);
for (var id in tasks) {
    TaskManager.resetExecutionCount(instance, id);
}
```

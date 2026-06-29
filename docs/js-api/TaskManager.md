# TaskManager 任务管理器

在 JS 脚本中通过 `TaskManager` 访问。

---

## 方法列表

### `TaskManager.getTaskConfigs(instance)`

获取地牢的所有任务配置。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |

**返回值**: `Map<String, TaskConfig>` — 任务配置映射 (taskId -> TaskConfig)

---

### `TaskManager.triggerTasks(instance, trigger, context?)`

根据 trigger 触发地牢中所有匹配的任务。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `trigger` | `String` | 触发器类型（如 `"MOB_KILL"`、`"PLAYER_JOIN"`） |
| `context` | `Map<String, Any?>`? | 传递给 JS 脚本的额外变量 |

**返回值**: 无

---

### `TaskManager.triggerTask(instance, taskId)`

手动触发指定任务，跳过冷却和次数限制。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `taskId` | `String` | 任务 ID |

**返回值**: `Boolean` — 是否找到并执行了任务

---

### `TaskManager.getExecutionCount(instance, taskId)`

获取任务的已执行次数。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `taskId` | `String` | 任务 ID |

**返回值**: `Int` — 已执行次数

---

### `TaskManager.resetExecutionCount(instance, taskId)`

重置任务的已执行次数。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `taskId` | `String` | 任务 ID |

**返回值**: 无

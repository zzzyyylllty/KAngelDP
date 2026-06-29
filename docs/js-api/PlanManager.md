# PlanManager 计划管理器

在 JS 脚本中通过 `PlanManager` 访问。

---

## 方法列表

### `PlanManager.startPlansForTrigger(instance, trigger)`

根据触发器名称启动匹配的计划。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `trigger` | `String` | 触发器名称（如 `"BEGIN"`、`"END"`、`"FAIL"`） |

**返回值**: 无

---

### `PlanManager.stopAllPlans(instance)`

停止地牢所有进行中的计划任务。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |

**返回值**: 无

---

### `PlanManager.isPlanActive(instance, planName)`

检查指定计划是否活跃。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |
| `planName` | `String` | 计划名称 |

**返回值**: `Boolean` — 是否活跃

---

### `PlanManager.getActivePlanNames(instance)`

获取活跃计划名称列表。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 地牢实例 |

**返回值**: `List<String>` — 活跃计划名称列表

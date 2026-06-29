# KitManager 奖励包管理器

在 JS 脚本中通过 `KitManager` 访问。

---

## 方法列表

### `KitManager.rollRewards(rewards, count)`

根据权重随机抽取指定数量的奖励（weight 模式）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `rewards` | `List<KitReward>` | 候选奖励列表 |
| `count` | `Int` | 抽取数量（不超过 rewards.size） |

**返回值**: `List<KitReward>` — 抽取到的奖励列表（不重复）

---

### `KitManager.rewardsByChance(rewards)`

按独立概率判定奖励（chance 模式）。每个奖励独立按 chance% 概率判定。

| 参数 | 类型 | 说明 |
|------|------|------|
| `rewards` | `List<KitReward>` | 候选奖励列表 |

**返回值**: `List<KitReward>` — 判定通过的奖励列表

---

### `KitManager.executeReward(reward, player, instance?)`

执行单条奖励。

| 参数 | 类型 | 说明 |
|------|------|------|
| `reward` | `KitReward` | 奖励配置 |
| `player` | `Player` | 目标玩家 |
| `instance` | `Any?` | 地牢实例（SCRIPT/AGENT 类型需要） |

**返回值**: `Boolean` — 是否成功执行

---

### `KitManager.checkCooldown(player, dungeonName, kitName)`

检查玩家是否在 Kit 冷却中。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 玩家 |
| `dungeonName` | `String` | 地牢模板名称 |
| `kitName` | `String` | Kit 名称 |

**返回值**: `Long?` — 剩余冷却毫秒数，null 表示不在冷却中

---

### `KitManager.applyCooldown(player, dungeonName, kitName, seconds)`

为玩家应用 Kit 冷却。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 玩家 |
| `dungeonName` | `String` | 地牢模板名称 |
| `kitName` | `String` | Kit 名称 |
| `seconds` | `Int` | 冷却秒数 |

**返回值**: 无

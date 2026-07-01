# JS Agent 钩子参考

本页记录了可视化编辑器中所有可编辑的 JS 脚本字段，按组件分类。

所有 JS 脚本中均可使用 `instance`（[DungeonInstance](js-api.md)）和 Bukkit 全局对象。

---

## 地牢生命周期（Dungeon Lifecycle Agents）

配置文件：`dungeon/<name>/option.yml → agent`

通过编辑器「Page 3 → Lifecycle Agents (JS)」管理。

| 钩子 | 触发时机 | JS 上下文 |
|------|----------|-----------|
| `onStart` | 地牢开始（PREPARING → ACTIVE） | `instance`, `player`（队长） |
| `onComplete` | 地牢通关 | `instance`, `player`（队长） |
| `onFail` | 地牢失败 | `instance`, `player`（队长） |
| `onLeave` | 玩家主动离开地牢 | `instance`, `player`（离开者） |
| `onLeaveFail` | 地牢失败时玩家离开 | `instance`, `player`（离开者） |

数据结构：
```yaml
agent:
  onStart:
    trigger: ONCE
    gjs: |
      // your JS code here
      instance.sendMessageToAllPlayers("地牢开始了！");
```

---

## 难度配置（Difficulty Agents）

配置文件：`dungeon/<name>/difficulty.yml → difficulties.<id>`

通过编辑器「Difficulty → Click agents」管理。

| 钩子 | 触发时机 | JS 上下文 |
|------|----------|-----------|
| `onStart` | 难度被应用 | `instance`, `player`（队长） |
| `onComplete` | 在该难度下通关 | `instance`, `player`（队长） |
| `onFail` | 在该难度下失败 | `instance`, `player`（队长） |

示例 JS：
```javascript
// onStart — 根据难度调整参数
instance.setAllPlayersMaxHealth(20.0);
instance.sendMessageToAllPlayers("难度已应用！");
```

---

## 怪物组（Monster Group）

配置文件：`dungeon/<name>/monster/<id>.yml`

通过编辑器编辑怪物组的所有字段。

### 条件字段（直接 JS）

| 字段 | 类型 | 说明 |
|------|------|------|
| `spawnCondition` | JS Boolean | 是否可以生成。返回 `true` 才生成 |
| `respawnCondition` | JS Boolean | 是否可以重生。返回 `true` 才重生 |

示例：
```javascript
// 仅当活跃玩家 >= 2 时才生成
instance.getAlivePlayerCount() >= 2
```

### Agent 钩子（agent.*）

| 钩子 | 触发时机 | JS 上下文 |
|------|----------|-----------|
| `onSpawn` | 怪物组生成时 | `instance` |
| `onAllKilled` | 该组所有怪物被击杀 | `instance` |
| `onRespawn` | 怪物组重生时 | `instance` |
| `onEachKill` | 该组每个怪物被击杀时 | `instance`, `mob`（被击杀的实体） |

示例：
```javascript
// onAllKilled — 解锁下一区域
instance.setMeta("wave1_cleared", true);
instance.sendMessageToAllPlayers("<green>第一波已清除！");

// onEachKill — 动态掉落
if (mob instanceof org.bukkit.entity.Monster) {
    var loc = mob.getLocation();
    loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 2));
}
```

---

## 障碍物（Obstacle）

配置文件：`dungeon/<name>/obstacle/<id>.yml`

| 钩子 | 触发时机 | JS 上下文 |
|------|----------|-----------|
| `agent.onPrepare` | 障碍物准备时（方块状态被保存） | `instance` |
| `agent.onStart` | 障碍物激活时 | `instance` |

示例：
```javascript
// onStart — 播放音效 + 消息
instance.broadcastSound(org.bukkit.Sound.ENTITY_IRON_DOOR_OPEN, 1.0, 1.0);
instance.sendMessageToAllPlayers("<yellow>门已打开！");
```

---

## 区域（Region）

配置文件：`dungeon/<name>/region/<id>.yml`

| 钩子 | 触发时机 | JS 上下文 |
|------|----------|-----------|
| `agent.onEnter` | 玩家进入区域时 | `instance`, `player` |
| `agent.onLeave` | 玩家离开区域时 | `instance`, `player` |

示例：
```javascript
// onEnter — 触发陷阱
instance.setPlayerHealth(player.getName(), player.getHealth() - 5);
instance.sendMessageToPlayer(player.getName(), "<red>你踩到了陷阱！");

// onLeave — 清理状态
instance.removePotionEffectFromPlayer(player.getName(), "POISON");
```

---

## 交互点（Interact）

配置文件：`dungeon/<name>/interact/<id>.yml`

| 钩子 | 触发时机 | JS 上下文 |
|------|----------|-----------|
| `agent.onActive` | 交互触发时 | `instance`, `player` |
| `agent.onPost` | 交互处理完成后 | `instance`, `player` |

示例：
```javascript
// onActive — 检查条件
var wave = instance.getMetaAsInt("wave");
if (wave < 3) {
    instance.sendMessageToPlayer(player.getName(), "<red>你还需要清除更多怪物！");
    false; // 返回 false 阻止交互
}
```

---

## 任务（Task）

配置文件：`dungeon/<name>/task/<id>.yml`

| 钩子 | 触发时机 | JS 上下文 |
|------|----------|-----------|
| `agent.onTrigger` | 任务被触发时 | `instance`, `player` |

示例：
```javascript
// onTrigger — 奖励额外物品
var item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1);
player.getInventory().addItem(item);
player.sendMessage("§a任务完成奖励：钻石");
```

---

## 计划（Plan）

配置文件：`dungeon/<name>/plan/<id>.yml`

| 字段 | 类型 | 说明 |
|------|------|------|
| `onRun` | JS | 计划执行时运行的代码 |

示例：
```javascript
// onRun — 定点空投
instance.dropItem(10, 5, 10, "DIAMOND", 3);
instance.dropItem(12, 5, 10, "GOLDEN_APPLE", 1);
instance.sendMessageToAllPlayers("<gold>空投已到达！");
```

---

## Kit 奖励（Kit Reward）

配置文件：`dungeon/<name>/kit/<id>.yml` 或 `kits/<id>.yml`

通过 Kit 编辑器的「Rewards → 编辑奖励条目」配置。用于 `SCRIPT` 和 `AGENT` 类型的奖励。

### SCRIPT 类型

| 字段 | 说明 |
|------|------|
| `script` | 要执行的脚本文件名（不含路径，对应 `dungeon/<name>/script/` 中的文件） |

### AGENT 类型

| 字段 | 类型 | 说明 |
|------|------|------|
| `agentTrigger` | String | 触发器类型：`ONCE` / `ON_KILL` / `ON_COMPLETE` |
| `agentScript` | JS | 要执行的 JS 代码 |

示例 AGENT 奖励：
```yaml
- type: AGENT
  agentTrigger: ONCE
  agentScript: |
    player.sendMessage("§a恭喜获得特殊奖励！");
    instance.giveItemToAllPlayers(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_SCRAP, 1));
  weight: 10
```

---

## 可用 JS 对象

| 变量 | 类型 | 说明 |
|------|------|------|
| `instance` | [DungeonInstance](js-api.md) | 当前地牢实例（所有 JS 钩子中可用） |
| `player` | `Player` | 触发事件的玩家（部分钩子中有，详见钩子说明） |
| `mob` | `Entity` | 被击杀的怪物（仅 `onEachKill` 中可用） |
| `Bukkit` | `org.bukkit.Bukkit` | Bukkit 服务端 API |

所有 JS 脚本支持完整的 Java/Bukkit API。示例常见用法：

```javascript
// 玩家操作
player.sendMessage("消息");
player.getInventory().addItem(item);
player.teleport(location);
player.getWorld().playSound(player.getLocation(), sound, 1.0, 1.0);

// 物品操作
var item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD, 1);
var meta = item.getItemMeta();
meta.setDisplayName("§b神剑");
item.setItemMeta(meta);

// 实体操作
var loc = player.getLocation();
var tnt = player.getWorld().spawn(loc, org.bukkit.entity.TNTPrimed.class);
tnt.setFuseTicks(40);

// 随机数
var rand = new java.util.Random();
var amount = rand.nextInt(5) + 1;
```

# 任务系统（Task System）

Task 系统允许在地牢生命周期的特定时刻触发自定义 JS 脚本，支持条件过滤和执行限制。

---

## 配置

任务定义在 `dungeon/<name>/task/*.yml`：

```yaml
任务ID:
  trigger: MOB_KILL                # 触发器类型（必需）
  filters:                         # 可选，过滤条件
    mob_type: SKELETON
  maxExecutions: 3                 # 最大执行次数，-1=无限（默认 -1）
  cooldown: 0                      # 执行冷却（tick），0=无冷却
  agent:
    onTrigger: |-                  # 触发时执行的 JS
      instance.setMeta("skeleton_kills",
        instance.getMetaAsInt("skeleton_kills") + 1);
      instance.sendMessageToAllPlayers(
        "<gray>骷髅击杀: " + instance.getMetaAsInt("skeleton_kills") + " / 10</gray>"
      );
```

---

## 触发器类型

### DUNGEON_START
地牢开始时触发。

**上下文变量**：`instance`

```yaml
welcome_task:
  trigger: DUNGEON_START
  agent:
    onTrigger: |-
      instance.sendMessageToAllPlayers("<green>欢迎来到地牢！</green>");
```

---

### DUNGEON_COMPLETE
地牢通关时触发。

**上下文变量**：`instance`

```yaml
reward_task:
  trigger: DUNGEON_COMPLETE
  agent:
    onTrigger: |-
      instance.openKitToAll("completion_chest");
```

---

### DUNGEON_FAIL
地牢失败时触发。

**上下文变量**：`instance`

---

### PLAYER_JOIN
有玩家加入地牢时触发。

**上下文变量**：`instance`, `player`, `playerName`

```yaml
join_notify:
  trigger: PLAYER_JOIN
  agent:
    onTrigger: |-
      instance.sendMessageToAllPlayers("<yellow>" + playerName + " 加入了地牢！</yellow>");
```

---

### PLAYER_LEAVE
有玩家离开地牢时触发。

**上下文变量**：`instance`, `player`, `playerName`

---

### PLAYER_DEATH
有玩家死亡时触发。

**上下文变量**：`instance`, `player`, `playerName`

```yaml
death_counter:
  trigger: PLAYER_DEATH
  agent:
    onTrigger: |-
      instance.addMeta("total_deaths", 1);
      var deaths = instance.getMetaAsInt("total_deaths");
      if (deaths >= 5) {
        instance.sendMessageToAllPlayers("<red>队伍已累计死亡 " + deaths + " 次！</red>");
      }
```

---

### MOB_KILL
有生物被击杀时触发。

**上下文变量**：`instance`, `player`, `playerName`, `mobType`, `mobName`

**可用过滤**：`mob_type`, `mob_name`

```yaml
skeleton_quest:
  trigger: MOB_KILL
  filters:
    mob_type: SKELETON
  maxExecutions: 10
  agent:
    onTrigger: |-
      var count = instance.getTaskExecutionCount("skeleton_quest");
      instance.setPlayerMeta(player, "skeletons_killed",
        instance.getPlayerMetaAsInt(player, "skeletons_killed") + 1);
      if (count >= 10) {
        instance.sendMessageToAllPlayers("<gold>所有骷髅已被消灭！</gold>");
      }
```

---

### MONSTER_GROUP_CLEAR
怪物组全清时触发。

**上下文变量**：`instance`, `configId`

```yaml
wave_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters:
    configId: wave_1
  agent:
    onTrigger: |-
      instance.setMeta("wave", 2);
      instance.spawnMonsters("wave_2");
      instance.sendTitleToAllPlayers("<red>第二波！</red>", "");
```

---

### MONSTER_SPAWN
怪物组生成时触发。

**上下文变量**：`instance`, `configId`

---

### REGION_ENTER / REGION_LEAVE
玩家进入/离开区域时触发。

**上下文变量**：`instance`, `player`, `playerName`, `regionId`

**可用过滤**：`regionId`

```yaml
boss_room_enter:
  trigger: REGION_ENTER
  filters:
    regionId: boss_room
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.sendTitleToAllPlayers("<red>⚠ Boss 房间</red>", "<yellow>" + playerName + " 进入了！</yellow>");
      instance.spawnMonsters("boss");
```

---

### CUSTOM
自定义触发器，仅可通过 JS 手动触发。

**上下文变量**：自定义（由 `triggerTask` 的 `context` 参数提供）

```yaml
secret_event:
  trigger: CUSTOM
  agent:
    onTrigger: |-
      instance.sendMessageToAllPlayers("<light_purple>秘密事件触发！</light_purple>");
      instance.giveExperienceToAllPlayers(100);
```

通过 JS 手动触发：
```js
TaskManager.triggerTask(instance, "secret_event", {});
```

---

## 过滤系统

`filters` 中的每个 key-value 对都必须与触发时的上下文变量完全匹配，任务才会执行。

**大小写不敏感**：`mob_type: SKELETON` 匹配 `skeleton` 和 `SKELETON`。

**示例**：

```yaml
# 仅在特定怪物组的全清事件触发
specific_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters:
    configId: boss_group

# 仅特定生物类型
zombie_only:
  trigger: MOB_KILL
  filters:
    mob_type: ZOMBIE

# 仅特定区域
checkpoint:
  trigger: REGION_ENTER
  filters:
    regionId: checkpoint_1
```

---

## 执行限制

### maxExecutions

限制任务在单个地牢实例生命周期内的最大执行次数。

- `0`：不会自动触发（只能手动 `triggerTask`）
- `-1`：无限次
- `N`：最多执行 N 次

```yaml
three_waves:
  trigger: MONSTER_GROUP_CLEAR
  maxExecutions: 3       # 最多触发 3 次
```

### cooldown

任务执行后需要等待的 tick 数才能再次触发。

```yaml
throttled_task:
  trigger: PLAYER_DEATH
  cooldown: 100           # 至少间隔 5 秒（100 tick / 20 = 5 秒）
```

---

## JS 控制

### 手动触发任务

```js
// 触发指定任务（绕过 maxExecutions 和 cooldown 限制）
instance.triggerTask("任务ID");
```

### 查询执行次数

```js
var count = instance.getTaskExecutionCount("任务ID");
```

### 重置执行计数

```js
instance.resetTaskExecutionCount("任务ID");
```

---

## 自动触发的上下文变量参考

| 触发器 | 可用变量 |
|--------|----------|
| `DUNGEON_START` | `instance` |
| `DUNGEON_COMPLETE` | `instance` |
| `DUNGEON_FAIL` | `instance` |
| `PLAYER_JOIN` | `instance`, `player`, `playerName` |
| `PLAYER_LEAVE` | `instance`, `player`, `playerName` |
| `PLAYER_DEATH` | `instance`, `player`, `playerName` |
| `MOB_KILL` | `instance`, `player`, `playerName`, `mobType`, `mobName` |
| `MONSTER_GROUP_CLEAR` | `instance`, `configId` |
| `MONSTER_SPAWN` | `instance`, `configId` |
| `REGION_ENTER` | `instance`, `player`, `playerName`, `regionId` |
| `REGION_LEAVE` | `instance`, `player`, `playerName`, `regionId` |
| `KIT_OPEN` | `instance`, `player`, `playerName`, `kitName` |
| `CUSTOM` | 自定义（由 `triggerTask` 提供） |

---

## 完整示例：波次系统

```yaml
# task/wave_system.yml

wave_start:
  trigger: DUNGEON_START
  agent:
    onTrigger: |-
      instance.setMeta("wave", 1);
      instance.spawnMonsters("wave_1");
      instance.sendTitleToAllPlayers("<red>第一波！</red>", "<yellow>消灭所有怪物</yellow>");

wave_1_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters:
    configId: wave_1
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.setMeta("wave", 2);
      instance.spawnMonsters("wave_2");
      instance.sendTitleToAllPlayers("<red>第二波！</red>", "<yellow>更多怪物来了</yellow>");
      instance.openKitToAll("wave_1_reward");

wave_2_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters:
    configId: wave_2
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.setMeta("wave", 3);
      instance.spawnMonsters("boss_wave");
      instance.sendTitleToAllPlayers("<dark_red>⚠ Boss 波！</dark_red>", "");

boss_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters:
    configId: boss_wave
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.openKitToAll("boss_reward");
      instance.complete();
```

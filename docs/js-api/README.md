# JS API 参考索引

此目录包含每个 JS 可访问对象的完整 API 文档。

## 核心对象 (通过 `defaultData` 注入)

### 地牢实例方法 (`instance`)
- [DungeonInstance](./DungeonInstance.md) — 地牢实例的核心方法（`instance.start()`、`instance.complete()` 等）

### 管理器对象
- [TaskManager](./TaskManager.md) — 任务管理：`getTaskConfigs`、`triggerTasks`、`triggerTask`
- [MonsterManager](./MonsterManager.md) — 怪物管理：`spawnMonsters`、`setMonsterActive`、`setMonsterCooldown`
- [ObstacleManager](./ObstacleManager.md) — 障碍物管理：`prepareObstacle`、`activateObstacle`、`openObstacle`
- [RegionManager](./RegionManager.md) — 区域管理：`isPlayerInRegion`、`getPlayersInRegion`
- [PlanManager](./PlanManager.md) — 计划管理：`startPlansForTrigger`、`stopAllPlans`
- [KitManager](./KitManager.md) — 奖励包管理：`rollRewards`、`executeReward`
- [DungeonHelper](./DungeonHelper.md) — 地牢帮助：`createDungeon`、`getWorldName`、`unloadDungeonWorld`

### 工具对象
- [ItemStackUtil](./ItemStackUtil.md) — 物品操作：创建、NBT、背包、装备
- [EventUtil](./EventUtil.md) — 事件取消与触发
- [ThreadUtil](./ThreadUtil.md) — 线程睡眠（仅异步）
- [PlayerUtil](./PlayerUtil.md) — 玩家操作：药水、消息、背包、传送、伤害
- [EntityUtil](./EntityUtil.md) — 实体操作：附近搜索、发光、命名、伤害
- [BlockUtil](./BlockUtil.md) — 方块操作：设置、查询、破坏
- [RandomUtil](./RandomUtil.md) — 随机操作：随机数、加权选择、洗牌
- [MathUtil](./MathUtil.md) — 数学运算：clamp、lerp、距离、百分比
- [Sys](./Sys.md) — 安全 System 包装（`currentTimeMillis`、`sleep`、`println`）
- [Bukkit](./Bukkit.md) — 安全 Bukkit 包装（`broadcast`）
- [TargetSelectorHelper](./TargetSelectorHelper.md) — 目标选择器解析

### 标准库
| JS 名 | 说明 |
|-------|------|
| `DungeonAPI` | `KAngelDungeonAPI` 接口的 Java Class 对象 |
| `Math` | `java.lang.Math` |
| `System` | `java.lang.System` |
| `Gson` | `com.google.gson.Gson` |
| `mmUtil` | MiniMessage 格式化工具 |
| `mmJsonUtil` | MiniMessage JSON 工具 |
| `mmLegacySectionUtil` | 旧版 § 格式转换 |
| `mmLegacyAmpersandUtil` | 旧版 & 格式转换 |

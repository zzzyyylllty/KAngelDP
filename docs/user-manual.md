# KAngelDungeon 用户使用手册

> 版本：0.1.1 | 适用平台：Paper 1.21.4+ | 语言：Kotlin + JavaScript

---

## 目录

1. [简介](#1-简介)
2. [安装与首次启动](#2-安装与首次启动)
3. [快速入门：五分钟创建第一个地牢](#3-快速入门五分钟创建第一个地牢)
4. [核心概念](#4-核心概念)
5. [地牢配置详解](#5-地牢配置详解)
6. [怪物系统](#6-怪物系统)
7. [障碍物系统](#7-障碍物系统)
8. [奖励系统 (Kit)](#8-奖励系统-kit)
9. [任务触发器系统 (Task)](#9-任务触发器系统-task)
10. [区域检测](#10-区域检测)
11. [交互点](#11-交互点)
12. [定时计划 (Plan)](#12-定时计划-plan)
13. [难度系统](#13-难度系统)
14. [队伍系统](#14-队伍系统)
15. [JavaScript 脚本编写](#15-javascript-脚本编写)
16. [Kether 表达式](#16-kether-表达式)
17. [命令参考](#17-命令参考)
18. [事件 API（供开发者）](#18-事件-api供开发者)
19. [实战案例](#19-实战案例)
20. [与其他插件集成](#20-与其他插件集成)
21. [常见问题 (FAQ)](#21-常见问题-faq)

---

## 1. 简介

KAngelDungeon 是 Minecraft **地牢副本**插件。你可以用它创建独立的副本世界，让玩家组队挑战怪物、破解机关、击败 Boss，最终获得奖励。

**核心能力**：

| 能力 | 说明 |
|------|------|
| 副本世界 | 每个地牢拥有独立的世界，互不干扰 |
| 完整生命周期 | 准备 → 战斗中 → 通关/失败 → 自动清理 |
| JS 脚本驱动 | 几乎所有行为都可通过 JavaScript 自定义 |
| 怪物系统 | 支持原版怪物 + MythicMobs，自动重生、等级缩放、激活距离 |
| 障碍物系统 | 动态栅栏门，支持序列动画、延迟激活、自动关闭 |
| 奖励系统 | 权重/概率两种抽奖模式，支持物品、命令、脚本奖励 |
| 难度分级 | 每个地牢模板可配多套难度 |
| 队伍系统 | 内置组队，API 接口支持对接外部队伍插件 |
| 数据持久化 | SQLite / MySQL 玩家数据存储 |

**运行依赖**：

| 依赖 | 必需 | 说明 |
|------|:--:|------|
| TabooLib 6.3+ | ✅ | 框架依赖 |
| MythicMobs | ❌ | 提供自定义怪物（MM4/MM5） |
| Chemdah | ❌ | 任务进度追踪 |
| ItemsAdder / Oraxen / Nexo / CraftEngine | ❌ | 自定义物品/方块 |
| PlaceholderAPI | ❌ | 变量支持 |

---

## 2. 安装与首次启动

### 2.1 安装

1. 确保服务器已安装 **TabooLib 6.3+** 和 **Kotlin 运行时**
2. 将 `KAngelDungeon-0.1.1.jar` 放入 `plugins/` 目录
3. 重启服务器

### 2.2 首次启动生成的文件

```
plugins/KAngelDungeon/
├── config.yml              # 主配置文件
├── lang/                   # 语言文件（zh_CN.yml / en_US.yml）
├── data.db                 # SQLite 数据库（默认）
├── schematics/             # Schematic 文件存放目录
├── sources/                # 世界模板存放目录
├── kits/                   # 全局 Kit 配置
│   └── sample.yml          # 示例 Kit
└── dungeon/                # 地牢模版
    └── sample/             # 示例地牢（完整演示所有功能）
        ├── option.yml      # 地牢主配置
        ├── difficulty.yml  # 难度配置
        ├── kit/            # 地牢专属 Kit
        ├── monster/        # 怪物配置
        ├── obstacle/       # 障碍物配置
        ├── region/         # 区域配置
        ├── interact/       # 交互点配置
        ├── plan/           # 定时计划
        ├── script/         # 独立脚本
        └── task/           # 任务触发器
```

### 2.3 关键 config.yml 设置

```yaml
# 调试模式 — 开发时开启，正式服关闭
debug: false

# JS 安全沙箱（强烈建议保持 false）
allow-danger-js: false

# JS 类白名单 — 脚本中 Java.type() 可加载的类前缀
allowed-js-classes:
  - "org.bukkit"
  - "net.kyori.adventure"

# 语言
lang: zh_CN

# 地牢结束后多少秒自动踢出玩家
auto-exit-delay: 60

# 启动时自动清理残留的地牢世界
cleanup-residual-worlds: true

# 准备阶段倒计时通知
preparation:
  notify:
    enabled: true
    mode: "title"        # title / actionbar / chat
    interval: 5           # 每隔多少秒通知一次
    countdown-last-seconds: 10  # 最后10秒每秒通知

# 队伍系统
party:
  mode: kangeldp           # kangeldp(内置) / none
  max-size: 5
  invite-timeout: 60
```

---

## 3. 快速入门：五分钟创建第一个地牢

### 3.1 准备地图

**方式一：世界模板（推荐）**

1. 在服务器创建一个世界，建造好地牢场景
2. 将世界文件夹复制到 `plugins/KAngelDungeon/sources/你的地图名/`
3. 只需保留 `region/`、`level.dat` 等必要文件

**方式二：Schematic**

1. 用 WorldEdit 选择地牢区域
2. `//copy` → `//schem save 你的地图名`
3. 将 `.schem` 文件放入 `plugins/KAngelDungeon/schematics/`

### 3.2 编写地牢配置

在 `plugins/KAngelDungeon/dungeon/我的地牢/` 下创建 `option.yml`：

```yaml
display:
  name: "新手试炼"

map:
  type: MAP                     # MAP（世界模板）或 SCHEMATIC
  source: 你的地图名             # 对应 sources/ 下的文件夹名
  spawn: {x: 0, y: 64, z: 0}   # 玩家出生坐标

gameplay:
  general:
    timeLimit: 300              # 5分钟时间限制
    preparationTime: 10         # 10秒准备时间
    minPlayers: 1
    maxPlayers: 4
    death:
      mode: RESPAWN
      maxRespawns: 3
      autoRespawnDelay: 5

agent:
  onStart: |-
    instance.sendMessageToAllPlayers("<green>地牢开始！消灭所有怪物！</green>");
    instance.spawnMonsters("wave_1");

  onComplete: |-
    instance.sendTitleToAllPlayers("<gold>通关！</gold>", "<yellow>恭喜！</yellow>");
    instance.openKitToAll("completion_reward");
```

### 3.3 创建怪物配置

在 `dungeon/我的地牢/monster/` 下创建 `monsters.yml`：

```yaml
wave_1:
  monsters:
    - mob: minecraft:zombie
      location: 0,64,5
      amount: 5
      scattered: 3
```

### 3.4 创建奖励配置

在 `dungeon/我的地牢/kit/` 下创建 `rewards.yml`：

```yaml
completion_reward:
  display:
    name: "<gold>通关奖励</gold>"
  min_rewards: 1
  max_rewards: 2
  rewards:
    - type: item
      source: minecraft
      item: diamond
      amount: 3
      weight: 50
    - type: command
      command: "give %player% minecraft:iron_ingot 10"
      weight: 30
    - type: command
      command: "give %player% minecraft:gold_ingot 5"
      weight: 20
```

### 3.5 重载并启动

```
/dg reload                                    # 重载所有配置
/dg create 我的地牢                            # 创建地牢（你自己作为队长）
```

等待准备倒计时结束，地牢即开始！

---

## 4. 核心概念

### 4.1 模板 vs 实例

| 概念 | 说明 | 类比 |
|------|------|------|
| **模板 (Template)** | `option.yml` 中定义的地牢配置 | 菜谱 |
| **实例 (Instance)** | 每次创建的实际运行地牢 | 做出来的一盘菜 |

同一个模板可以同时创建多个实例（多组玩家同时攻略同一地图）。

### 4.2 地牢生命周期

```
创建 → PREPARING（准备阶段）
         │
         ├── 倒计时结束 → ACTIVE（战斗阶段）
         │                    │
         │                    ├── 通关 → COMPLETED → 60秒后清理
         │                    │
         │                    └── 超时/全灭 → FAILED → 60秒后清理
         │
         └── 强制结束 → FAILED
```

### 4.3 配置查找优先级

地牢专属配置优先于全局配置：

```
dungeon/<地牢名>/kit/*.yml  >  kits/*.yml
dungeon/<地牢名>/monster/*.yml  >  （无全局回退）
```

### 4.4 代理脚本 (Agent)

地牢的各个生命周期节点可以绑定 JS 脚本，在特定时机自动执行：

| 触发点 | 配置位置 | 说明 |
|--------|----------|------|
| `onCheck` | `option.yml → agent` | 检查玩家能否开始地牢 |
| `onStart` | `option.yml → agent` | 地牢开始时 |
| `onComplete` | `option.yml → agent` | 通关时 |
| `onFail` | `option.yml → agent` | 失败时 |
| `onLeave` | `option.yml → agent` | 玩家尝试离开时 |
| `onLeaveFail` | `option.yml → agent` | 离开被阻止时的提示 |

---

## 5. 地牢配置详解

### 5.1 option.yml 完整参考

```yaml
# ==================== 显示信息 ====================
display:
  name: "地牢显示名"                  # 必填
  icon:
    material: DIAMOND_SWORD          # GUI 图标材质
    parameters:                      # 可选，物品模型参数
      item_model: "minecraft:iron_sword"
  description: "描述文本"

# ==================== 地图设置 ====================
map:
  type: MAP                          # MAP（世界模板）/ SCHEMATIC
  source: world_template_name        # 模板文件夹名 / .schem 文件名
  spawn:
    x: 0                            # 出生点 X
    y: 64                           # 出生点 Y
    z: 0                            # 出生点 Z

# ==================== 初始元数据 ====================
meta:
  global:                            # 实例级元数据
    my_key: my_value
  player:                            # 玩家级元数据（每位玩家加入时初始化）
    score: 0

# ==================== 玩法设置 ====================
gameplay:
  general:
    timeLimit: 600                   # 时间限制（秒），null = 无限制
    preparationTime: 30              # 准备时间（秒），null = 无准备阶段，立即开始

    adventureMode: true              # 强制冒险模式

    minPlayers: 1                    # 最少玩家数
    maxPlayers: 5                    # 最多玩家数
    allowParty: true                 # 允许整个队伍加入

    # ---- 死亡系统 ----
    death:
      mode: RESPAWN                  # RESPAWN / SPECTATE / POSSESS / LEAVE
      maxRespawns: 3                 # -1=无限, 0=一命通关, N=最多N次
      autoRespawnDelay: 5            # 自动复活延迟（秒），0=手动
      keepInventoryOnRespawn: false
      respawnAtSpawn: true           # 复活在出生点

    # ---- 死亡不掉落 ----
    keepInventory:
      enabled: true
      requiredLives: false           # true=仅不掉落通过死亡不掉落机制保护的物品

    # ---- 禁止物品 ----
    bannedItems:
      - ENDER_PEARL
      - CHORUS_FRUIT
      - TOTEM_OF_UNDYING

    # ---- 方块放置控制 ----
    blockPlace:
      mode: BLACKLIST                # BLACKLIST（禁止列表中的）/ WHITELIST（只允许列表中的）
      list: [TNT, LAVA_BUCKET]

    # ---- 方块破坏控制 ----
    blockBreak:
      mode: BLACKLIST
      list: [TNT]

  # ---- 命令控制 ----
  commands:
    mode: BLACKLIST
    list:
      - tpa
      - home
      - spawn
      - warp

# ==================== 原版控制 ====================
vanillaOptions:
  hungry: true                       # 是否消耗饱食度
  healthRegain:                      # 自然回血控制
    food: false
    saturation: false
    potions: false
    other: false
  durability: true                   # 是否消耗耐久
  itemsDrop: true                    # 是否允许丢弃物品
  itemsPickup: true                  # 是否允许捡起物品
  spawnpoint: "0 64 0"             # 出生点（覆盖 map.spawn）
  gameRules:                        # 地牢开始后自动设置的游戏规则
    doTileDrops: false
    doDaylightCycle: false
    doWeatherCycle: false
    keepInventory: true
    naturalRegeneration: false

# ==================== 权限 ====================
requiredPermission: null             # 进入地牢需要的权限节点，null = 无限制

# ==================== PVP ====================
pvpEnabled: false
allowRespawn: true

# ==================== 代理脚本 ====================
agent:
  onCheck: |-                        # 检查是否可以开始 → return false 阻止
  onStart: |-                        # 开始时
  onComplete: |-                     # 通关时
  onFail: |-                         # 失败时
  onLeave: |-                        # 玩家离开 → return false 阻止
  onLeaveFail: |-                    # 阻止离开时的消息
```

### 5.2 死亡模式详解

| 模式 | 玩家死亡后 | 适用场景 |
|------|-----------|----------|
| `RESPAWN` | 等待复活（手动/自动），受 maxRespawns 限制 | 标准副本 |
| `SPECTATE` | 进入旁观模式，可自由飞行观看 | 团队副本，队友可继续 |
| `POSSESS` | 旁观并附身到随机存活队友 | 合作副本 |
| `LEAVE` | 直接退出地牢 | 硬核模式 |

---

## 6. 怪物系统

### 6.1 基本配置

```yaml
怪物组ID:
  # ---- 激活控制 ----
  active: true                       # 是否初始激活
  priority: 0                        # 优先级，数字越大越早自动检查生成

  # ---- 生成时机 ----
  spawnDelay: 0                      # 地牢开始后延迟多少 tick 自动生成（0 = 不自动生成）
  spawnInterval: 0                   # 同组内每只怪之间的生成间隔（tick），0 = 同时生成
  spawnCondition: |-                 # JS 条件，return true 才生成

  # ---- 激活距离 ----
  activationRangeMin: 0              # 玩家必须离生成点多远才会激活（0=不限）
  activationRangeMax: -1             # 玩家必须在多近才会激活（-1=不限）

  # ---- 重生 ----
  respawnCooldown: 0                 # 全清后多少 tick 重生（0=不重生）
  maxRespawns: -1                    # 最大重生次数（-1=无限, 0=不重生, N=N次）
  respawnCondition: |-               # JS 条件，return true 才允许重生

  # ---- 数值 ----
  healthMultiplier: 1.0              # 生命倍率
  damageMultiplier: 1.0              # 伤害倍率
  leashRange: 0                      # 怪物离生成点最大距离（超出则拉回），0=不限

  # ---- 怪物列表 ----
  monsters:
    - mob: minecraft:zombie          # 原版格式：minecraft:<类型>
      location: 10,64,10             # 生成坐标
      amount: 5                      # 数量
      scattered: 3                   # 散布半径（方块）
      level: 0                       # 等级缩放（0=无）
    - mob: mythicmobs:BossKnight     # MythicMobs 格式
      location: 15,64,10
      amount: 1
      level: 5

  # ---- 脚本 ----
  agent:
    onSpawn: |-                      # 生成时执行
    onAllKilled: |-                  # 全组被击杀时执行
    onRespawn: |-                    # 重生时执行
    onEachKill: |-                   # 每击杀一个怪物时执行
```

### 6.2 激活距离工作原理

```
              activationRangeMax
          ┌─────────────────────────┐
          │                         │
  ┌───────┼─────┐                   │
  │ 太近  │激活 │     太远/不激活    │
  │不激活 │区域 │                   │
  └───────┼─────┘                   │
          │                         │
          └─────────────────────────┘
     activationRangeMin
```

当 `spawnDelay > 0` 或 `spawnCondition` 不为空时，怪物组不会在 `onStart` 时自动生成，而是在每个 tick 检查条件。

### 6.3 JS 控制怪物

```js
// 手动生成
instance.spawnMonsters("monster_id");

// 查看配置
var configs = instance.getMonsterConfigs();

// 查看实例
var instances = instance.getActiveMonsters();

// 动态控制激活距离
instance.setMonsterActivationRangeMin("monster_id", 5.0);
instance.setMonsterActivationRangeMax("monster_id", 30.0);

// 动态开关
instance.setMonsterActive("monster_id", true);

// 动态设置重生冷却
instance.setMonsterCooldown("monster_id", 200);  // 200 tick = 10 秒
```

---

## 7. 障碍物系统

### 7.1 概述

障碍物用于控制玩家的通行。最典型的用法是铁栅栏门：激活时关闭栅栏阻挡玩家，打开后移除栅栏恢复通行。

### 7.2 基本配置

```yaml
障碍物ID:
  # ---- 时间控制 ----
  openDelaySeconds: 3                # 打开延迟（秒）
  activeDurationSeconds: 10          # 激活后自动关闭时间（秒）

  # ---- 动画 ----
  openingAnimation:                  # 激活时动画
    enabled: true
    particle: END_ROD
    particleCount: 20
    sound: BLOCK_LEVER_CLICK
    volume: 1.0
    pitch: 1.0

  closingAnimation:                  # 打开时动画
    enabled: true
    particle: SMOKE_NORMAL
    particleCount: 15

  # ---- 代理脚本 ----
  agent:
    onPrepare: |-                    # 准备时（保存方块）
    onStart: |-                      # 激活时

  # ---- 栅栏定义 ----
  obstacles:
    门ID:
      mode: RESTORE_BLOCKS           # 方块恢复模式
      cuboid:                        # 长方体区域
        pos1: {x: 10, y: 64, z: 10}
        pos2: {x: 10, y: 66, z: 12}
      sequentialConfig:              # 序列动画（逐块开门/关门）
        enabled: true
        openDirection: LEFT_TO_RIGHT # 方向：LEFT_TO_RIGHT / RIGHT_TO_LEFT
                                    #       TOP_TO_BOTTOM / BOTTOM_TO_TOP
                                    #       FRONT_TO_BACK / BACK_TO_FRONT
        reverseOnClose: true         # 关门时反向
        blocksPerStep: 1             # 每步处理多少方块
        stepDelayTicks: 2            # 步间延迟
        openEffect:                  # 开门时每块的效果
          enabled: true
          particle: WHITE_SMOKE
          count: 8
        closeEffect:                 # 关门时每块的效果
          enabled: true
          particle: WHITE_SMOKE
          count: 6
```

### 7.3 生命周期

```
INACTIVE → prepare() → PREPARING → activate() → ACTIVE → open() → OPEN
                                                  ↓
                                              超时自动 open()
```

### 7.4 JS 控制

```js
instance.prepareObstacle("gate_id");   // 保存方块状态
instance.activateObstacle("gate_id");  // 关闭栅栏（延迟 + 持续时间遵照配置）
instance.openObstacle("gate_id");      // 打开栅栏（已开过的不会重复开）
instance.openObstacleForce("gate_id"); // 强制打开（无视重复检查）
instance.restoreObstacleBlocks();      // 恢复所有方块到初始状态
```

---

## 8. 奖励系统 (Kit)

### 8.1 位置

- **全局 Kit**：`kits/*.yml` — 所有地牢通用
- **地牢 Kit**：`dungeon/<name>/kit/*.yml` — 仅该地牢可用（同名时覆盖全局）

### 8.2 配置格式

```yaml
Kit名称:
  display:
    name: "<gold>通关宝箱</gold>"     # 显示名
    material: CHEST                   # 图标

  cooldown: 86400                     # 冷却（秒），0=无冷却

  conditions:                         # Kether 条件（全部通过才能开启）
    - "check player level > 5"

  messages:                           # 自定义消息
    open: "<gold>你获得了奖励！</gold>"
    cooldown: "<red>冷却中，剩余 %remaining% 秒</red>"
    condition_fail: "<red>你不满足开启条件</red>"

  broadcast_message: "<yellow>%player%</yellow> <gold>获得了 %kit%！</gold>"

  # ---- 抽取配置（仅 weight 模式） ----
  min_rewards: 1
  max_rewards: 3

  # ---- 奖励列表 ----
  rewards:
    # 物品奖励
    - type: item
      source: minecraft               # minecraft / craftengine / itemsadder / oraxen / nexo
      item: diamond
      amount: 3
      weight: 50                      # 权重
      chance: 30                      # 概率%（设置后自动切换为概率模式）
      parameters:                     # 物品自定义（MiniMessage 格式）
        display-name: "<gradient:#55ff55:#aaffaa>通关钻石</gradient>"
        lore:
          - "<gray>品质：传说</gray>"
          - "<gray>来自地牢试炼</gray>"
      message: "<gold>获得钻石 x3！</gold>"

    # 命令奖励
    - type: command
      command: "give %player% minecraft:experience_bottle 16"
      weight: 30

    # JS 脚本奖励
    - type: script
      weight: 15
      script: |-
        player.sendMessage("<light_purple>你触发了稀有奖励！</light_purple>");
        player.giveExp(500);

    # Agent 脚本奖励
    - type: agent
      weight: 5
      agent-trigger: onKitReward
      agent-script: |-
        player.sendMessage("<gold>传说级奖励！</gold>");
```

### 8.3 两种模式

| 模式 | 触发方式 | 行为 |
|------|----------|------|
| **权重模式** | 所有奖励只有 `weight` | 按权重不放回抽取 `min_rewards`~`max_rewards` 个 |
| **概率模式** | 任意奖励设置了 `chance` | 每个奖励独立按 `chance%` 判定，全部通过全部发 |

### 8.4 JS 调用

```js
// 给单个玩家
instance.openKit("kit_name", player);

// 给所有在线玩家
instance.openKitToAll("kit_name");
```

---

## 9. 任务触发器系统 (Task)

### 9.1 概述

Task 系统在特定地牢事件发生时自动执行 JS 脚本，支持条件过滤和执行次数/冷却限制。不需要在 `option.yml` 的 agent 中编写大量逻辑，而是拆分为独立的小任务。

### 9.2 配置

```yaml
任务ID:
  trigger: MOB_KILL                   # 触发器类型
  filters:                            # 可选，过滤条件（全部匹配才触发）
    mob_type: SKELETON
  maxExecutions: 10                   # 最大执行次数（-1=无限）
  cooldown: 100                       # 执行冷却（tick），0=无
  agent:
    onTrigger: |-                     # 触发时执行的 JS
```

### 9.3 全部触发器

| 触发器 | 触发时机 | 可用上下文变量 |
|--------|----------|---------------|
| `DUNGEON_START` | 地牢开始 | `instance` |
| `DUNGEON_COMPLETE` | 通关 | `instance` |
| `DUNGEON_FAIL` | 失败 | `instance` |
| `PLAYER_JOIN` | 玩家加入 | `instance`, `player`, `playerName` |
| `PLAYER_LEAVE` | 玩家离开 | `instance`, `player`, `playerName` |
| `PLAYER_DEATH` | 玩家死亡 | `instance`, `player`, `playerName` |
| `MOB_KILL` | 击杀怪物 | `instance`, `mobType`, `mobName`, `killer` |
| `MONSTER_SPAWN` | 怪物生成 | `instance`, `configId` |
| `MONSTER_GROUP_CLEAR` | 怪组全清 | `instance`, `configId` |
| `REGION_ENTER` | 进入区域 | `instance`, `player`, `regionId` |
| `REGION_LEAVE` | 离开区域 | `instance`, `player`, `regionId` |
| `KIT_OPEN` | 打开 Kit | `instance`, `player`, `kitName` |

### 9.4 典型用例

**波次系统**：

```yaml
# task/waves.yml

wave_1_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters:
    configId: wave_1
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.sendTitleToAllPlayers("<red>第二波！</red>", "");
      instance.spawnMonsters("wave_2");
      instance.openKitToAll("wave_1_reward");

boss_defeated:
  trigger: MONSTER_GROUP_CLEAR
  filters:
    configId: boss_wave
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.openKitToAll("boss_reward");
      instance.complete();  // 通关！
```

**杀怪计数**：

```yaml
count_skeletons:
  trigger: MOB_KILL
  filters:
    mob_type: SKELETON
  maxExecutions: 10
  agent:
    onTrigger: |-
      var killed = instance.getTaskExecutionCount("count_skeletons");
      instance.sendActionBarToAllPlayers("<yellow>骷髅击杀：" + killed + "/10</yellow>");
      if (killed >= 10) {
        instance.sendMessageToAllPlayers("<green>所有骷髅已消灭！</green>");
      }
```

---

## 10. 区域检测

### 10.1 配置

在 `dungeon/<name>/region/*.yml`：

```yaml
区域ID:
  from: "x1 y1 z1"
  to: "x2 y2 z2"
  agent:
    onEnter: |-
      instance.sendMessageToAllPlayers("<yellow>" + player.name + " 进入了 " + region.id + "</yellow>");
    onLeave: |-
      instance.sendMessageToAllPlayers("<gray>" + player.name + " 离开了 " + region.id + "</gray>");
```

### 10.2 JS 查询

```js
// 玩家是否在区域内
instance.isPlayerInRegion("playerName", "regionId");

// 区域内所有玩家
var players = instance.getPlayersInRegion("regionId");

// 玩家所在的所有区域
var regions = instance.getPlayerRegions("playerName");
```

---

## 11. 交互点

### 11.1 配置

在 `dungeon/<name>/interact/*.yml`：

```yaml
交互点ID:
  pos: "130 -57 178"            # 坐标
  agent:
    onActive: |-                 # 交互时（取消原版交互）
      player.sendMessage("你点击了按钮！");
      instance.spawnMonsters("ambush");
    onPost: |-                   # 交互后（不取消原版交互）
```

### 11.2 JS 调用

```js
instance.getInteractConfigs();
instance.triggerInteractBtn("button_id");  // 手动触发交互
```

---

## 12. 定时计划 (Plan)

Plan 用于在特定阶段执行定时/重复任务。

### 12.1 配置

```yaml
计划名:
  trigger: BEGIN                  # 触发器：PREPARE / BEGIN / END / FAIL
  delay: 200                      # 首次延迟（tick），0 = 立即
  period: 200                     # 重复间隔（tick），不填 = 单次执行
  async: true                     # 异步执行（推荐 JS 放 true）
  agent:
    onRun: |-
      instance.sendActionBarToAllPlayers("剩余时间：" + instance.getElapsedTimeFormatted());
```

### 12.2 触发时机

| 触发器 | 对应阶段 |
|--------|----------|
| `PREPARE` | 地牢进入准备阶段时（创建后立即触发） |
| `BEGIN` | 地牢正式开始（ACTIVE）时 |
| `END` | 地牢通关（COMPLETED）时 |
| `FAIL` | 地牢失败（FAILED）时 |

### 12.3 JS 控制

```js
instance.startPlansForTrigger("BEGIN");  // 手动启动
instance.stopAllPlans();                  // 停止所有
instance.isPlanActive("planName");        // 检查是否活跃
instance.getActivePlanNames();            // 列出活跃计划
```

---

## 13. 难度系统

### 13.1 配置

在 `dungeon/<name>/difficulty.yml`：

```yaml
difficulties:
  easy:
    display: "简单"
    description: "适合新手"
    meta:
      global:                      # 覆盖 option.yml 中同名的 meta
        difficulty_mult: 0.8
      player:
        bonus_score: 0
    agents:                        # 覆盖 option.yml 中同名的 agent
      onStart: |-
        instance.sendMessageToAllPlayers("<green>难度：简单</green>");

  hard:
    display: "困难"
    description: "挑战模式"
    meta:
      global:
        difficulty_mult: 2.0
        death.maxRespawns: 1       # 一命通关！

  nightmare:
    display: "噩梦"
    description: "隐藏难度 —— 无法复活"
    meta:
      global:
        difficulty_mult: 3.0
        death.maxRespawns: 0
        death.autoRespawnDelay: 0
```

### 13.2 创建时选择难度

```
/dg create 我的地牢 easy           # 不指定则创建 UI 让玩家选择
/dg create 我的地牢 hard Notch     # 指定难度 + 队长
```

### 13.3 JS 查询

```js
var difficulty = instance.getDifficulty();          // "easy" 或 null
var config = instance.getDifficultyConfig();        // DifficultyConfig 对象
if (config) {
    instance.sendMessageToAllPlayers("难度：" + config.display);
}
```

---

## 14. 队伍系统

### 14.1 命令

```
/kdparty create          # 创建队伍
/kdparty invite Notch    # 邀请玩家
/kdparty join <teamId>   # 接受邀请
/kdparty leave           # 离开队伍
/kdparty kick Notch      # 踢出队员
/kdparty transfer Notch  # 转让队长
/kdparty disband         # 解散队伍
/kdparty info            # 查看信息
```

### 14.2 与地牢创建联动

当 `option.yml` 中 `allowParty: true` 时：

```
/dg create 我的地牢            # 自动邀请全队
/dg join <uuid>               # 队长的加入自动带入全队
```

---

## 15. JavaScript 脚本编写

### 15.1 全局可用变量

在 JS 脚本中，以下变量始终可用：

| 变量 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 当前地牢实例（`onCheck` / 全局脚本中可能为 null） |
| `template` | `DungeonTemplate` | 地牢模版 |
| `player` | `Player?` | 当前玩家（区域/交互/任务上下文中可用） |
| `Bukkit` | `Bukkit` | Bukkit API 入口 |
| `Math` | `Math` | JS Math 对象 |
| `console` | `ConsoleSender` | 控制台 |

### 15.2 常用脚本模式

#### 波次刷怪

```js
var wave = instance.getMetaAsInt("wave") || 1;

if (wave == 1) {
    instance.spawnMonsters("wave_1");
} else if (wave == 2) {
    instance.spawnMonsters("wave_2");
} else if (wave == 3) {
    instance.spawnMonsters("boss");
}

instance.sendTitleToAllPlayers("<red>第 " + wave + " 波！</red>", "");
instance.addMeta("wave", 1);  // 累加
```

#### 条件判断

```js
// 检查玩家是否满足条件
var alivePlayers = instance.getAlivePlayerCount();
if (alivePlayers <= 1) {
    instance.sendMessageToAllPlayers("<red>只剩最后一人！</red>");
}

// 根据难度调整
var diff = instance.getDifficulty();
if (diff == "hard") {
    instance.spawnMonsters("hard_bonus");
}
```

#### 倒计时广播

```js
var remaining = instance.getRemainingTimeSimple();
if (remaining && remaining <= 60) {
    instance.sendActionBarToAllPlayers("<red>剩余时间：</red><gold>" + Math.floor(remaining) + "秒</gold>");
}
```

#### 遍历玩家

```js
var players = instance.getOnlinePlayers();
for (var i = 0; i < players.length; i++) {
    var p = players[i];
    var score = instance.getPlayerMetaAsInt(p, "score") || 0;
    if (score >= 100) {
        p.sendMessage("你的分数达标了！");
    }
}
```

#### 使用目标选择器

```js
// 选择生命值低于10的存活玩家
var targets = instance.selectTargets("@alive{health<10}");
for (var i = 0; i < targets.length; i++) {
    targets[i].sendMessage("你的生命值危险！");
}

// 随机选一个玩家传送
var random = instance.selectRandomTarget("@all{level>=5}");
if (random) {
    instance.teleportPlayerTo(random.name, 100, 64, 100);
}
```

### 15.3 JS API 完整参考

`instance` 对象提供 **150+ 个方法**，完整列表见 [JS API 参考](js-api.md)。常用分类速查：

| 分类 | 方法数 | 常用方法 |
|------|:------:|----------|
| 生命周期 | 9 | `start()`, `complete()`, `fail()`, `isActive()` |
| 玩家管理 | 20+ | `getOnlinePlayers()`, `addPlayer()`, `removePlayer()`, `respawnPlayer()` |
| 全体操作 | 15+ | `setAllPlayersHealth()`, `healAllPlayers()`, `giveItemToAllPlayers()` |
| 单玩家 | 20+ | `setPlayerHealth()`, `healPlayer()`, `teleportPlayerTo()` |
| 消息 | 4 | `sendMessageToAllPlayers()`, `sendTitleToAllPlayers()`, `sendActionBarToAllPlayers()` |
| 音效 | 3 | `broadcastSound()`, `playSoundAt()`, `playSoundToPlayer()` |
| 世界 | 10+ | `setWorldTime()`, `setWorldBorder()`, `setGameRule()` |
| 视觉 | 7 | `spawnParticle()`, `firework()`, `strikeLightning()`, `explosionEffect()` |
| 实体 | 7 | `dropItem()`, `clearHostileMobs()`, `clearAllMobs()` |
| 怪物 | 10+ | `spawnMonsters()`, `setMonsterActive()`, `setMonsterActivationRangeMax()` |
| 障碍物 | 6 | `prepareObstacle()`, `activateObstacle()`, `openObstacle()` |
| 区域 | 3 | `isPlayerInRegion()`, `getPlayersInRegion()` |
| Kit | 2 | `openKit()`, `openKitToAll()` |
| 元数据 | 15+ | `setMeta()`, `getMeta()`, `getMetaAsInt()`, `setPlayerMeta()` |
| 时间 | 5 | `getElapsedTime()`, `getRemainingTimeSimple()` |
| 脚本 | 2 | `runScript()`, `runAllScripts()` |
| 目标选择器 | 5 | `selectTargets()`, `selectRandomTarget()` |

---

## 16. Kether 表达式

Kether 是 TabooLib 的脚本 DSL，可用于条件判断和快捷操作。KAngelDungeon 注册了 `kangeldp` (别名 `kdp`) 前缀动作。

### 16.1 常用动作

```
# 查询
kangeldp inside                          # 是否在地牢中
kangeldp inside named "template_name"    # 是否在特定地牢
kangeldp state                           # 地牢状态字符串
kangeldp template                        # 模板名称

# 操作
kangeldp complete                        # 通关地牢
kangeldp fail                            # 失败地牢

# 元数据
kangeldp meta get <key>
kangeldp meta set <key> <value>
kangeldp meta add <key> <value>

# 怪物
kangeldp monster spawn <configId>
kangeldp monster clear

# 障碍物
kangeldp obstacle open <id>
kangeldp obstacle activate <id>

# 消息
kangeldp tellall <message>
kangeldp titleall <title> <subtitle>
kangeldp actionbarall <message>

# JS 执行
kangeldp eval "<JS代码>"
```

### 16.2 配合 Chemdah 使用

```yaml
# 条件：玩家必须在特定地牢中
condition: "kangeldp inside named 'test_dungeon'"

# 条件：地牢必须进行中
condition: "kangeldp state == 'ACTIVE'"

# 动作：通过地牢
action: "kangeldp complete"

# 动作：增加元数据
action: "kangeldp meta add 'quests_done' 1"
```

---

## 17. 命令参考

### 17.1 命令结构

```
/kangeldungeon (dg, dungeon)        主命令
  ├── about / status / help / reload
  ├── dungeon (dgm, dungeonm)       地牢管理
  ├── admin (kda, dungeonadmin)     管理员工具
  ├── debug (dgd, dungeondebug)     调试命令
  ├── data (dd, dungeondata)        玩家数据
  ├── api (dga, dungeonapi)         脚本测试
  └── party (kdparty)              队伍系统
```

### 17.2 常用命令速查

#### 日常使用

| 命令 | 说明 |
|------|------|
| `/dg create <模板> [难度] [队长]` | 创建并开始地牢 |
| `/dg join <uuid>` | 加入等待中的地牢 |
| `/dg leave` | 离开当前地牢 |
| `/dg list` | 查看活跃地牢 |
| `/dg templates` | 列出所有可用模板 |

#### 管理员

| 命令 | 说明 |
|------|------|
| `/kda stopall` | 强制停止所有地牢 |
| `/kda purge` | 清理已结束的地牢 |
| `/kda maintenance [on/off]` | 维护模式开关 |
| `/dg reload` | 热重载所有配置 |

#### 调试

| 命令 | 说明 |
|------|------|
| `/dgd getInstances` | 查看所有活跃实例 |
| `/dgd getInstance <uuid>` | 查看实例详情 |
| `/dgd getMonsterConfigs <模板名>` | 查看怪物配置 |
| `/dgd getMemoryInfo` | 查看缓存使用量 |
| `/dgd evalInstance <uuid> <js>` | 在实例上下文中执行 JS |

> 完整命令列表见 [命令参考](commands.md)

---

## 18. 事件 API（供开发者）

KAngelDungeon 提供了 **40+ 个自定义 Bukkit 事件**，采用 Pre/Post 模式。

### 18.1 监听示例（Kotlin）

```kotlin
import io.github.zzzyyylllty.kangeldungeon.event.DungeonCompletePostEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object MyListener : Listener {

    @EventHandler
    fun onDungeonComplete(event: DungeonCompletePostEvent) {
        val instance = event.instance
        // 给通关玩家额外奖励
        instance.openKitToAll("external_bonus")
    }
}
```

### 18.2 全部事件一览

| 事件 | 可取消 | 用途 |
|------|:------:|------|
| `DungeonStartPreEvent` | 是 | 阻止地牢开始 |
| `DungeonStartPostEvent` | 否 | 地牢开始后通知 |
| `DungeonCompletePreEvent` | 是 | 阻止通关 |
| `DungeonCompletePostEvent` | 否 | 通关后奖励 |
| `DungeonFailPreEvent` | 是 | 阻止失败 |
| `DungeonFailPostEvent` | 否 | 失败后处理 |
| `DungeonPlayerJoinPreEvent` | 是 | 限制玩家加入 |
| `DungeonPlayerJoinPostEvent` | 否 | 玩家加入通知 |
| `DungeonPlayerQuitPreEvent` | 是 | 战斗状态禁止退出 |
| `DungeonPlayerQuitPostEvent` | 否 | 退出后清理 |
| `DungeonPlayerDeathEvent` | 否 | 死亡统计 |
| `DungeonPlayerRespawnEvent` | 否 | 复活处理 |
| `MonsterSpawnPreEvent` | 是 | 控制怪物生成 |
| `MonsterSpawnPostEvent` | 否 | 生成后修改属性 |
| `MonsterGroupClearEvent` | 否 | 清怪触发下一波 |
| `ObstaclePreparePreEvent` | 是 | 控制障碍物准备 |
| `ObstacleActivatePreEvent` | 是 | 控制障碍物激活 |
| `ObstacleOpenPreEvent` | 是 | 控制障碍物开启 |
| `RegionEnterEvent` | 否 | 区域进入检测 |
| `RegionLeaveEvent` | 否 | 区域离开检测 |
| `KitOpenPreEvent` | 是 | 控制 Kit 开启 |
| `KitOpenPostEvent` | 否 | Kit 开启后统计 |
| `DungeonMobKillEvent` | 否 | Chemdah 任务计数 |
| `DungeonPlayerCompleteEvent` | 否 | Chemdah 个人通关 |
| `DungeonPlayerFailEvent` | 否 | Chemdah 个人失败 |

---

## 19. 实战案例

### 19.1 经典波次挑战

**效果**：3 波怪物 + Boss，每清完一波自动出下一波，全清后通关。

**option.yml agent.onStart**：
```js
instance.setMeta("wave", 0);
// 60秒后开始第一波
instance.sendTitleToAllPlayers("<green>准备迎战！</green>", "<yellow>60秒后开始</yellow>");
```

**task/waves.yml**：

```yaml
start_wave_1:
  trigger: DUNGEON_START
  agent:
    onTrigger: |-
      instance.setMeta("wave", 1);
      instance.spawnMonsters("wave_1_mobs");
      instance.sendTitleToAllPlayers("<red>第一波</red>", "<yellow>3只僵尸 + 2只骷髅</yellow>");

wave_1_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters: {configId: wave_1_mobs}
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.spawnMonsters("wave_2_mobs");
      instance.sendTitleToAllPlayers("<red>第二波</red>", "<yellow>5只蜘蛛 + 1只女巫</yellow>");

wave_2_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters: {configId: wave_2_mobs}
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.spawnMonsters("wave_3_mobs");
      instance.sendTitleToAllPlayers("<red>最终波</red>", "<yellow>1只 Boss</yellow>");

boss_clear:
  trigger: MONSTER_GROUP_CLEAR
  filters: {configId: boss_mobs}
  maxExecutions: 1
  agent:
    onTrigger: |-
      instance.openKitToAll("completion_reward");
      instance.complete();
```

### 19.2 闸门机关房间

**效果**：玩家进入房间 → 3秒后闸门关闭 → 刷怪 → 击杀全部怪后闸门开启。

**monster/mobs.yml**：
```yaml
trap_room_mobs:
  monsters:
    - mob: minecraft:zombie
      location: 50,64,50
      amount: 8
      scattered: 5
  agent:
    onAllKilled: |-
      instance.openObstacle("room_gate");
      instance.sendMessageToAllPlayers("<green>闸门已开启，继续前进！</green>");
```

**obstacle/gate.yml**：
```yaml
room_gate:
  openDelaySeconds: 3
  obstacles:
    gate:
      cuboid:
        pos1: {x: 45, y: 64, z: 50}
        pos2: {x: 45, y: 67, z: 53}
      sequentialConfig:
        enabled: true
        openDirection: BOTTOM_TO_TOP
        blocksPerStep: 1
        stepDelayTicks: 3
```

**region/trigger.yml**：
```yaml
trap_trigger:
  from: "40 60 45"
  to: "55 70 55"
  agent:
    onEnter: |-
      // 防止重复触发
      if (instance.hasMeta("trap_triggered")) return;
      instance.setMeta("trap_triggered", true);
      instance.activateObstacle("room_gate");
      // 延迟刷怪（给玩家反应时间）
      instance.spawnMonsters("trap_room_mobs");
```

### 19.3 多人合作解谜

**效果**：3个玩家需要分别站在3个压力板上才能打开大门。

```js
// 在 option.yml agent 中管理
var plate1 = instance.getPlayerMetaAsBoolean(player, "on_plate_1") || false;
var plate2 = instance.getPlayerMetaAsBoolean(player, "on_plate_2") || false;
var plate3 = instance.getPlayerMetaAsBoolean(player, "on_plate_3") || false;

if (plate1 && plate2 && plate3) {
    instance.openObstacle("puzzle_door");
    instance.sendMessageToAllPlayers("<gold>大门已打开！</gold>");
    instance.broadcastSound("block_iron_door_open", 1.0, 1.0);
}
```

### 19.4 计时挑战

**效果**：越早通关奖励越好。

```js
// onStart
instance.setMeta("start_time", Date.now());

// onComplete
var elapsed = (Date.now() - instance.getMetaAsLong("start_time")) / 1000;
if (elapsed < 120) {
    instance.openKitToAll("speed_reward_gold");     // 2分钟以内 → 金奖励
} else if (elapsed < 300) {
    instance.openKitToAll("speed_reward_silver");   // 5分钟以内 → 银奖励
} else {
    instance.openKitToAll("speed_reward_bronze");   // 其他 → 铜奖励
}
```

### 19.5 动态难度调整

**效果**：根据存活玩家数量动态调整怪物强度。

```js
// 在 option.yml 或怪物 agent.onSpawn 中
var aliveCount = instance.getAlivePlayerCount();
if (aliveCount >= 4) {
    // 4人或以上 → 增强怪物
    instance.setMonsterActivationRangeMax("mobs", null);
} else if (aliveCount <= 1) {
    // 只剩1人 → 降低难度
    // 通过全局变量通知怪物生成逻辑降级
    instance.setMeta("handicap_mode", true);
}
```

---

## 20. 与其他插件集成

### 20.1 MythicMobs

怪物配置中直接使用 `mythicmobs:<mob_id>`：

```yaml
monsters:
  - mob: mythicmobs:SkeletalKnight
    location: 100,64,100
    amount: 1
    level: 5
```

### 20.2 Chemdah（任务系统）

Chemdah 可以监听 KAngelDungeon 事件来追踪任务进度：

```yaml
# Chemdah 任务配置
objectives:
  - type: 'kangeldungeon mobkill'
    amount: 10
    mob_type: 'SKELETON'
```

通过 Kether 条件检查：

```yaml
condition: "kangeldp inside named 'test_dungeon'"
action: "kangeldp complete"
```

### 20.3 ItemsAdder / Oraxen / Nexo / CraftEngine

Kit 物品奖励的 `source` 字段支持：

```yaml
rewards:
  - type: item
    source: itemsadder       # 使用 ItemsAdder 物品
    item: "my_namespace:custom_sword"
    amount: 1

  - type: item
    source: craftengine      # 使用 CraftEngine 物品
    item: "another_plugin:custom_item"
    amount: 1
```

### 20.4 PlaceholderAPI

如果安装了 PlaceholderAPI，KAngelDungeon 注册的变量可通过 `%kangeldungeon_xxx%` 访问（需要在目标选择器中使用时自动解析）。

---

## 21. 常见问题 (FAQ)

### 地牢相关

**Q: 地牢创建后没有反应？**
A: 检查：1) 世界模板文件夹是否在 `sources/` 下；2) `/dg reload` 是否执行成功；3) 控制台是否有报错。

**Q: 玩家无法加入地牢？**
A: 检查：1) 是否被 `bannedPlayers` 封禁；2) `minPlayers` / `maxPlayers` 设置；3) 玩家是否已在其他地牢中；4) `onLeave` 代理是否阻止了离开。

**Q: 地牢世界文件夹没有自动清理？**
A: 检查 `config.yml` 中 `cleanup-residual-worlds: true`。手动清理：`/kda purge`。

**Q: 准备阶段时间到了但地牢没开始？**
A: 如果是 SCHEMATIC 模式，可能 Schematic 粘贴尚未完成。等待粘贴完成后 `worldReady` 变为 true 才会开始。

### JS 脚本相关

**Q: JS 脚本不执行或有报错？**
A: 1) 检查 `.yml` 中脚本使用了 `|-` 多行文字格式；2) 查看控制台完整错误栈；3) 使用 `/dga evalJs "你的脚本"` 单独测试。

**Q: 如何调试 JS 脚本？**
A: 1) 开启 `config.yml` 中 `debug: true`；2) 在 JS 中用 `print("调试信息")` 输出到控制台；3) 使用 `/dgd evalInstance <uuid> <js>` 在运行中的地牢测试。

**Q: JS 脚本中能调用 Java 类吗？**
A: 安全模式下只能调用 `allowed-js-classes` 白名单中的包。如果需要完整 Java 访问，开启 `allow-danger-js: true`（**强烈不推荐**）。

### 性能相关

**Q: 插件占用多少内存？**
A: 空载约 20-50MB。每个活跃地牢实例增加约 5-20MB（取决于怪物数量、脚本复杂度）。建议限制同时运行的地牢数量。

**Q: 地牢世界文件很大怎么办？**
A: 1) 使用 SCHEMATIC 模式（只在需要时粘贴）；2) 清理世界模板中的非必要文件；3) 确保 `isAutoSave = false`（插件已默认设置）。

### 其他

**Q: 如何备份地牢配置？**
A: 直接复制 `plugins/KAngelDungeon/dungeon/` 和 `plugins/KAngelDungeon/kits/` 目录。

**Q: 如何迁移到另一台服务器？**
A: 复制整个 `plugins/KAngelDungeon/` 目录，包括 `sources/` 中的世界模板。注意数据库配置可能需要修改。

**Q: 玩家进入地牢后原来的位置丢失了？**
A: 插件会缓存玩家进入地牢前的位置，离开时自动传送回去。如果传送失败（世界被删除等），玩家会被送到主世界出生点。

---

> 更多技术细节参见：[命令参考](commands.md) | [配置参考](configuration.md) | [JS API](js-api.md) | [事件系统](events.md) | [任务系统](task-system.md) | [Kether 动作](kether-actions.md)

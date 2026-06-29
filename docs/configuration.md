# 配置文件参考

## 目录结构

```
plugins/KAngelDungeon/
├── config.yml                      # 主配置
├── kits/                           # 全局 Kit（所有地牢通用）
│   └── *.yml
└── dungeon/
    └── <地牢ID>/
        ├── option.yml              # 地牢模板定义（必需）
        ├── difficulty.yml          # 难度配置
        ├── kit/                    # 地牢专属 Kit
        │   └── *.yml
        ├── monster/                # 怪物组配置
        │   └── *.yml
        ├── obstacle/               # 障碍物配置
        │   └── *.yml
        ├── region/                 # 区域配置
        │   └── *.yml
        ├── interact/               # 交互点配置
        │   └── *.yml
        ├── plan/                   # 定时计划配置
        │   └── *.yml
        ├── script/                 # 独立脚本
        │   └── *.yml
        └── task/                   # 任务触发器配置
            └── *.yml
```

---

## config.yml（主配置）

```yaml
# 调试模式（启用后输出开发日志）
debug: false

# 允许 JS 访问完整 Java 运行时（危险！仅开发时使用）
allow-danger-js: false

# 安全模式下的 JS 类白名单
allowed-js-classes:
  - "org.bukkit"
  - "net.minecraft"

# 语言（zh_CN / en_US）
lang: zh_CN

# 数据库配置
database:
  enable: false              # true=MySQL, false=SQLite
  filename: data             # SQLite 文件名
  host: localhost
  port: 3306
  user: root
  password: ""
  database: kangeldungeon
  table: kangeldungeon_data

# 文件加载正则过滤
file-load:
  kit: ".*"
  obstacle: ".*"
  monster: ".*"
  plan: ".*"
  script: ".*"
  region: ".*"
  interact: ".*"
  task: ".*"
  difficulty: ".*"

# 队伍系统
party:
  mode: kangeldp             # kangeldp(内置) / none / 外部插件名
  max-size: 5
  invite-timeout: 60         # 邀请超时（秒）

# 准备阶段通知
preparation:
  notify:
    enabled: true
    mode: "title"            # title / actionbar / chat
    interval: 5              # 通知间隔（秒）
    countdown-last-seconds: 10  # 最后 N 秒每秒通知

# 启动时清理残留地牢世界
cleanup-residual-worlds: true

# 地牢结束后自动退出延迟（秒）
auto-exit-delay: 60
auto-exit-notify-interval: 5
auto-exit-countdown-last-seconds: 10

# 通知开关
notify:
  on-timeout: true
  on-all-dead: true
  on-complete: true
  on-fail: true

# 性能选项
performance:
  adventure:
    use-split-replace-list-serialize: false  # 特定 Paper 版本兼容
```

---

## dungeon/<name>/option.yml（地牢模板）

```yaml
# ===== 显示信息 =====
display:
  name: "示例地牢"
  icon:
    material: DIAMOND_SWORD       # 或 craftengine:default:topaz
    parameters:                    # 可选，物品参数
      item_model: "minecraft:iron_sword"
  description: "一个示例地牢"

# ===== 地图 =====
map:
  type: MAP                        # MAP（世界模板）或 SCHEMATIC
  source: testmap                  # 世界模板名 或 schematic 文件名
  spawn:
    x: 77
    y: -58
    z: 191

# ===== 初始元数据 =====
meta:
  global:                          # 实例级别
    sample_meta: 1
  player:                          # 玩家级别（玩家加入时初始化）
    sample_meta: 1

# ===== 游戏设置 =====
gameplay:
  general:
    timeLimit: 600                 # 时间限制（秒），0 或不填 = 无限制
    preparationTime: 10            # 准备时间（秒）

    # 死亡系统（新）
    death:
      mode: RESPAWN                # RESPAWN / SPECTATE / POSSESS / LEAVE
      maxRespawns: 3               # -1=无限, 0=不可复活, N=最大次数
      autoRespawnDelay: 5          # 自动重生延迟（秒），0=手动
      keepInventoryOnRespawn: false
      respawnAtSpawn: true

    adventureMode: true            # 冒险模式
    minPlayers: 1
    maxPlayers: 5
    allowParty: true               # 允许队伍加入

    keepInventory:
      enabled: true
      requiredLives: false         # true=仅保留死亡不掉落的生命数

    # 禁止物品
    bannedItems:
      - ENDER_PEARL

    # 方块放置控制
    blockPlace:
      mode: BLACKLIST              # BLACKLIST / WHITELIST
      list:
        - TNT
        - LAVA_BUCKET

    # 方块破坏控制
    blockBreak:
      mode: BLACKLIST
      list:
        - TNT

  # 命令控制
  commands:
    mode: BLACKLIST
    list:
      - tpa

  # 计时器
  sequence:
    timer:
      mode: COUNTDOWN              # COUNTDOWN / STOPWATCH
      start: 600

# ===== 原版选项 =====
vanillaOptions:
  hungry: true
  healthRegain:
    food: false
    saturation: false
    potions: false
    other: false
  durability: true
  itemsDrop: true
  itemsPickup: true
  spawnpoint: "77 -58 191"         # 玩家出生偏移
  gameRules:                       # 地牢开始时应用
    doTileDrops: false
    doDaylightCycle: false
    doWeatherCycle: false
    keepInventory: true
    naturalRegeneration: false

# ===== 权限 =====
requiredPermission: null           # 进入地牢需要的权限节点

# ===== PVP =====
pvpEnabled: false
allowRespawn: true

# ===== 生命周期代理脚本 =====
agent:
  onCheck: |-                      # 检查玩家是否可以开始，return false 阻止
  onCheckFail: |-                  # 检查失败时的提示
  onLeave: |-                      # return false 阻止离开
  onLeaveFail: |-                  # 阻止离开时的提示
  onStart: |-                      # 地牢开始时
  onComplete: |-                   # 通关时
  onFail: |-                       # 失败时
  # 自定义触发器：onWaveStart 等任意名称均可

# ===== 加入要求 =====
# 可选，玩家进入地牢前的条件检查，不满足则阻止创建地牢
join-requirements:
  min-level: 0                         # 最低等级，0=不检查
  required-permissions: []             # 所需权限节点列表
  required-items: []                   # 所需物品列表
    # - material: DIAMOND              # 物品材质
    #   amount: 1                      # 数量
    #   take: false                    # 是否消耗
  required-money: 0.0                  # 所需金币（需要 Vault 经济插件）

# ===== 视觉效果 =====
# 可选，覆盖地牢生命周期阶段的默认标题/音效
# 留空则使用默认行为
visual:
  start-title: ""                      # 开始标题（MiniMessage）
  start-subtitle: ""                   # 开始副标题
  start-sound:                         # 开始音效
    sound: ""                          # 音效名称
    volume: 1.0
    pitch: 1.0
  complete-title: ""                   # 通关标题
  complete-subtitle: ""
  complete-sound:                      # 通关音效
    sound: ""
    volume: 1.0
    pitch: 1.0
  fail-title: ""                       # 失败标题
  fail-subtitle: ""
  fail-sound:                          # 失败音效
    sound: ""
    volume: 1.0
    pitch: 1.0

# ===== 环境控制 =====
# 可选，地牢开始时自动应用的世界环境设置
environment:
  allow-fly: false                     # 是否允许飞行
  game-mode: ADVENTURE                 # 游戏模式
  fly-speed: 0.1                       # 飞行速度（0~1）
  walk-speed: 0.2                      # 行走速度（0~1）
  potion-effects: []                   # 药水效果列表
    # - type: SPEED                    # 药水效果类型
    #   amplifier: 0                   # 等级（0=I）
    #   duration: 30                   # 持续时间（秒）
  world-border:                        # 世界边界
    size: 256.0                        # 边长
    center-x: 0.0
    center-z: 0.0
  time-lock: 6000                      # 锁定世界时间（tick），null=不锁定
  weather-lock: CLEAR                  # 锁定天气（CLEAR/RAIN/THUNDER），null=不锁定

# ===== 通关奖励 =====
# 可选，通关/失败时的奖励和命令执行
rewards:
  complete-commands: []                # 通关时执行的命令（%player% 占位符）
  fail-commands: []                    # 失败时执行的命令
  complete-items: []                   # 通关时给予的物品
    # - material: DIAMOND
    #   amount: 1
  complete-money: 0.0                  # 通关金币（需要 Vault）
  complete-experience: 0               # 通关经验值
  per-player: true                     # true=每人执行，false=仅执行一次

# ===== 杂项 =====
misc:
  join-while-running: false            # 是否允许地牢进行中时加入
  max-deaths: -1                       # 最大死亡次数（按玩家），-1=无限
  kick-on-max-deaths: SPECTATE         # 超过最大死亡时的操作（SPECTATE/LOBBY/KICK）
  title-join: ""                       # 玩家中途加入时的标题
  title-leave: ""                      # 玩家离开时的标题

# ===== Breakable Blocks / 可破坏方块 =====
# 白名单模式：只有在此列表中的方块可以被破坏
# 留空则使用 gameplay.general.blockBreak 的原有规则
# 非空时，方块必须同时满足 blockBreak 规则和此列表才能被破坏
breakable-blocks:
  - OAK_PLANKS
  - GLASS

# ===== Player Blocks / 玩家放置的方块 =====
# 控制玩家在 dungeon 世界中的方块放置行为
player-blocks:
  track-placed: false       # 是否追踪玩家放置的方块（追踪后可在地牢结束时恢复）
  clear-on-end: false       # 地牢结束时是否恢复被替换的方块
  max-blocks-per-player: -1 # 每名玩家最大放置数量，-1=无限
```

---

## dungeon/<name>/difficulty.yml（难度配置）

```yaml
difficulties:
  easy:
    display: "简单"
    description: "适合新手"
    meta:                          # 覆盖 option.yml 的 meta
      global:
        difficulty_mult: 0.8
        death.maxRespawns: 5
        death.autoRespawnDelay: 3
    agents:                        # 独立的代理脚本（覆盖 option.yml）
      onStart: |-
        instance.sendMessageToAllPlayers("<green>难度：简单</green>");
      onComplete: |-
      onFail: |-
  normal:
    display: "普通"
    description: "标准难度"
  hard:
    display: "困难"
    description: "挑战模式"
```

选择难度时，配置会与 `option.yml` 合并：
- 难度 `meta.global` 覆盖 `option.yml` 中的同名 key
- 难度 `agents` 中存在的触发器覆盖，不存在的回退到 `option.yml`

---

## dungeon/<name>/monster/*.yml（怪物组配置）

```yaml
怪物组ID:
  # 基本设置
  active: true                     # 初始激活状态
  priority: 0                      # 优先级，越高的组优先生成
  spawnDelay: 0                    # 开始后延迟（tick）首次生成
  spawnInterval: 0                 # 每个怪物之间的生成间隔（tick）

  # 激活距离
  activationRangeMin: 0            # 最小激活距离（玩家必须在此距离之外），0=无限制
  activationRangeMax: -1           # 最大激活距离（玩家必须在此距离之内），-1=无限制

  # 重生设置
  maxRespawns: -1                  # 最大重生次数，-1=无限, 0=不重生
  respawnCooldown: 0               # 重生冷却（tick）
  respawnCondition: |-             # JS 表达式，return true 才允许重生

  # 生成条件
  spawnCondition: |-               # JS 表达式，return true 才允许生成

  # 数值倍率
  healthMultiplier: 1.0
  damageMultiplier: 1.0

  # 牵制范围
  leashRange: 0                    # 怪物离生成点最大距离，0=不限

  # 怪物列表
  monsters:
    - mob: minecraft:zombie        # 原版：minecraft:<type>
      location: 115,-56,185        # 生成坐标
      amount: 3                    # 数量
      scattered: 5                 # 散布半径
      level: 0                     # 0=无等级缩放
    - mob: mythicmobs:SkeletalKnight  # MythicMobs：mythicmobs:<id>
      location: 146,-58,157
      amount: 2
      scattered: 3
      level: 3

  # 事件代理
  agent:
    onSpawn: |-                    # 生成时
    onAllKilled: |-                # 全组击杀时
    onRespawn: |-                  # 重生时
    onEachKill: |-                 # 单个怪物击杀时
```

### 激活距离说明

```
玩家离生成点太近 → 不激活（activationRangeMin）
玩家在范围内     → 激活生成（activationRangeMin < 距离 < activationRangeMax）
玩家离生成点太远 → 不激活（activationRangeMax）
```

两个值均可通过 JS 运行时修改：
```js
instance.setMonsterActivationRangeMin("monsterId", 5.0);
instance.setMonsterActivationRangeMax("monsterId", 30.0);
instance.resetMonsterActivationRange("monsterId");  // 恢复默认
```

---

## Kit 配置（kits/*.yml 和 dungeon/<name>/kit/*.yml）

### 全局 Kit vs 地牢 Kit

- `kits/*.yml`：全局 Kit，所有地牢通用
- `dungeon/<name>/kit/*.yml`：地牢专属 Kit
- 同名 Kit 地牢版本覆盖全局版本

### 配置格式

```yaml
kit名称:
  # 显示
  display:
    name: "<gold>通关奖励</gold>"
    material: CHEST

  # 冷却（秒），0=无冷却
  cooldown: 86400

  # Kether 条件表达式（全部通过才能打开）
  conditions:
    - "check player level > 5"

  # 全服广播（支持 %player% %kit% 占位符）
  broadcast_message: "<yellow>%player%</yellow> <gold>获得了 %kit%！</gold>"

  # 自定义消息
  messages:
    open: "<gold>你获得了奖励！</gold>"
    cooldown: "<red>冷却中，剩余 %remaining% 秒</red>"
    condition_fail: "<red>不满足条件</red>"

  # 抽取范围（仅 weight 模式）
  min_rewards: 1
  max_rewards: 2

  # 奖励列表
  rewards:
    # ---- 物品奖励 ----
    - type: item
      source: minecraft             # minecraft / craftengine / itemsadder / oraxen
      item: diamond
      amount: 1
      weight: 50                    # 权重模式
      chance: 10                    # 概率模式（0-100%），设置了 chance 则切换为概率模式
      parameters:                   # 物品参数（MiniMessage）
        display-name: "<gradient:#55ff55:#aaffaa>通关钻石</gradient>"
        lore:
          - "<gray>恭喜通关！</gray>"
      components:                   # 1.20.5+ 数据组件
        "minecraft:enchantment_glint_override": true
      message: "<gold>获得钻石 x1！</gold>"  # 单条奖励消息

    # ---- 命令奖励 ----
    - type: command
      command: "give %player% minecraft:iron_ingot 5"
      weight: 40

    # ---- JS 脚本奖励 ----
    - type: script
      weight: 10
      script: |-
        player.sendMessage("<light_purple>稀有脚本奖励！</light_purple>");

    # ---- Agent 脚本奖励 ----
    - type: agent
      weight: 5
      agent-trigger: onKitReward
      agent-script: |-
        player.sendMessage("<gold>特殊奖励！</gold>");
```

### 两种抽取模式

| 模式 | 触发条件 | 行为 |
|------|----------|------|
| **权重模式** | 所有奖励都只有 `weight` | 从池中按权重抽取 `min_rewards`~`max_rewards` 个 |
| **概率模式** | 任意一个奖励设置了 `chance` | 每个奖励独立按 `chance%` 概率判定 |

### JS 调用

```js
// 给单个玩家
instance.openKit("kit名称", player);

// 给所有在线玩家
instance.openKitToAll("kit名称");
```

---

## dungeon/<name>/obstacle/*.yml（障碍物配置）

```yaml
障碍物ID:
  agent:
    onPrepare: |-                  # 准备时（保存方块状态）
    onStart: |-                    # 激活时

  openDelaySeconds: 3              # 打开延迟
  activeDurationSeconds: 10        # 激活持续时间

  # 打开动画
  openingAnimation:
    enabled: true
    particle: END_ROD
    particleCount: 20
    sound: BLOCK_LEVER_CLICK
    volume: 1.0
    pitch: 1.0
    durationTicks: 20
    intervalTicks: 2

  # 关闭动画
  closingAnimation:
    enabled: true
    particle: SMOKE_NORMAL
    particleCount: 15

  # 障碍物门定义
  obstacles:
    门ID:
      mode: RESTORE_BLOCKS         # 恢复方块模式
      cuboid:
        pos1:
          x: 137
          y: -56
          z: 184
        pos2:
          x: 137
          y: -58
          z: 181
      sequentialConfig:            # 序列动画（逐块打开/关闭）
        enabled: true
        openDirection: LEFT_TO_RIGHT  # LEFT_TO_RIGHT / RIGHT_TO_LEFT / TOP_TO_BOTTOM / BOTTOM_TO_TOP
        reverseOnClose: true
        blocksPerStep: 1
        stepDelayTicks: 2
        openEffect:
          enabled: true
          particle: WHITE_SMOKE
          count: 8
        closeEffect:
          enabled: true
          particle: WHITE_SMOKE
          count: 6
```

### 生命周期

```
INACTIVE →  prepare() → PREPARING →  activate() → ACTIVE →  open() / openForce() → OPEN
                                                                              → CLOSED（超时）
```

### JS 调用

```js
instance.prepareObstacle("id");
instance.activateObstacle("id");
instance.openObstacle("id");
instance.openObstacleForce("id");  // 跳过检查
```

---

## dungeon/<name>/region/*.yml（区域配置）

```yaml
区域名:
  from: "x1 y1 z1"
  to: "x2 y2 z2"
  agent:
    onEnter: |-
      player.sendMessage("你进入了区域！");
    onLeave: |-
      player.sendMessage("你离开了区域！");
```

### JS 查询

```js
instance.isPlayerInRegion("playerName", "regionId");  // boolean
instance.getPlayersInRegion("regionId");               // List<Player>
instance.getPlayerRegions("playerName");               // List<String>
```

---

## dungeon/<name>/interact/*.yml（交互点配置）

```yaml
交互点ID:
  pos: "130 -57 178"              # 坐标
  agent:
    onActive: |-                   # 交互时（替代原版交互）
    onPost: |-                     # 交互后（不取消原版交互）
```

---

## dungeon/<name>/plan/*.yml（定时计划）

```yaml
计划名:
  trigger: BEGIN                   # BEGIN（开始后）/ PREPARE（准备时）/ END（结束时）/ FAIL（失败时）
  delay: 200                       # 首次执行延迟（tick）
  period: 200                      # 重复间隔（tick），不填 = 单次执行
  async: true                      # 是否异步执行 JS
  agent:
    onRun: |-
      instance.sendMessageToAllPlayers("10秒过去了");
```

### 触发器值

| 值 | 触发时机 |
|----|----------|
| `PREPARE` | 地牢进入 PREPARING 状态 |
| `BEGIN` | 地牢进入 ACTIVE 状态 |
| `END` | 地牢通关（COMPLETED） |
| `FAIL` | 地牢失败（FAILED） |

### JS 控制

```js
instance.startPlansForTrigger("BEGIN");  // 手动启动指定触发器的所有计划
instance.stopAllPlans();                 // 停止所有活跃计划
```

### Kether 控制

```
kangeldp plan start BEGIN
kangeldp plan stop
```

---

## dungeon/<name>/script/*.yml（独立脚本）

```yaml
脚本名:
  onRun: |-                        # 主执行代码
    instance.sendMessageToAllPlayers("执行脚本！");
  onPost: |-                       # 后执行代码
    instance.setMeta("script_done", true);
```

### 自建 .js 文件脚本（推荐）

除了 YAML 格式外，你也可以直接放置 `*.js` 文件到 `dungeon/<name>/script/` 目录下：

```
dungeon/<name>/
└── script/
    ├── sample.yml                 # YAML 格式（旧方式）
    └── my_script.js               # JS 文件（新方式，文件名即脚本名）
```

`.js` 文件的全部内容即为脚本的 `onRun` 代码，文件名（不含扩展名）作为脚本名称。

示例 `my_script.js`：
```javascript
// 直接编写 JS 代码，无需 YAML 包装
instance.sendMessageToAllPlayers("<yellow>自定义 JS 脚本执行！</yellow>");
var count = instance.getMetaAsInt("my_script_count") || 0;
instance.setMeta("my_script_count", count + 1);
```

### 全局 JS 脚本

放置在 `plugins/KAngelDungeon/scripts/` 目录下的 `.js` 文件将作为全局脚本加载，
所有地牢都可以通过 `instance.runScript("脚本名")` 调用：

```
plugins/KAngelDungeon/
└── scripts/
    ├── utils.js                   # 全局工具函数
    └── common.js                  # 通用逻辑
```

全局脚本的优先级低于地牢专属脚本（`dungeon/<name>/script/`），
当两者同名时，地牢专属脚本优先。

### JS 调用

```js
instance.runScript("脚本名");
instance.runAllScripts();
```

### Kether 调用

```
kangeldp script <脚本名>
```

---

## PlaceholderAPI 变量

当服务器安装 PlaceholderAPI 后，KAngelDungeon 会注册 `%kangeldungeon_xxx%` 占位符。
所有占位符需要玩家位于地牢中才会返回值，否则返回空字符串。

### 实例状态

| 占位符 | 说明 | 示例值 |
|--------|------|--------|
| `%kangeldungeon_in_dungeon%` | 是否在地牢中 | yes / no |
| `%kangeldungeon_state%` | 地牢状态 | PREPARING / ACTIVE / COMPLETED / FAILED |
| `%kangeldungeon_template%` | 地牢模板名称 | sample |
| `%kangeldungeon_display%` | 地牢显示名称 | 示例地牢 |
| `%kangeldungeon_world%` | 地牢世界名称 | KDP_sample_xxxx |
| `%kangeldungeon_leader%` | 队长名称 | Notch |
| `%kangeldungeon_difficulty%` | 难度 ID（未选择则为空） | hard |

### 玩家统计

| 占位符 | 说明 |
|--------|------|
| `%kangeldungeon_players%` | 地牢中玩家总数 |
| `%kangeldungeon_alive%` | 存活玩家数 |
| `%kangeldungeon_dead%` | 死亡玩家数 |
| `%kangeldungeon_online_names%` | 在线玩家名称列表（逗号分隔） |
| `%kangeldungeon_dead_names%` | 死亡玩家名称列表（逗号分隔） |
| `%kangeldungeon_player_status%` | 当前玩家状态：alive / dead / offline / not_in_dungeon |
| `%kangeldungeon_player_deaths%` | 当前玩家的死亡次数 |

### 时间相关

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `%kangeldungeon_elapsed%` | 已用时间（秒） | 125 |
| `%kangeldungeon_elapsed_formatted%` | 已用时间（MM:SS） | 02:05 |
| `%kangeldungeon_remaining%` | 剩余时间（秒，无限制则为空） | 475 |
| `%kangeldungeon_remaining_formatted%` | 剩余时间（MM:SS） | 07:55 |

### 状态判断（返回 yes / no）

| 占位符 | 判断条件 |
|--------|----------|
| `%kangeldungeon_is_active%` | 地牢是否进行中 |
| `%kangeldungeon_is_preparing%` | 地牢是否准备中 |
| `%kangeldungeon_is_completed%` | 地牢是否已通关 |
| `%kangeldungeon_is_failed%` | 地牢是否已失败 |
| `%kangeldungeon_is_finished%` | 地牢是否已结束（完成或失败） |
| `%kangeldungeon_pvp%` | 地牢是否允许 PVP |

### 模板配置

| 占位符 | 说明 |
|--------|------|
| `%kangeldungeon_time_limit%` | 时间限制（秒） |
| `%kangeldungeon_prep_time%` | 准备时间（秒） |
| `%kangeldungeon_min_players%` | 最小玩家数 |
| `%kangeldungeon_max_players%` | 最大玩家数 |
| `%kangeldungeon_min_level%` | 最低等级要求 |

### 统计数据

| 占位符 | 说明 |
|--------|------|
| `%kangeldungeon_mob_kills%` | 总怪物击杀数 |
| `%kangeldungeon_boss_kills%` | 总 Boss 击杀数 |
| `%kangeldungeon_total_deaths%` | 总死亡次数 |
| `%kangeldungeon_dungeon_starts%` | 地牢开始次数 |
| `%kangeldungeon_dungeon_completes%` | 地牢通关次数 |
| `%kangeldungeon_dungeon_fails%` | 地牢失败次数 |
| `%kangeldungeon_monster_spawns%` | 怪物生成次数 |
| `%kangeldungeon_kit_opens%` | Kit 开启次数 |
| `%kangeldungeon_region_enters%` | 区域进入次数 |
| `%kangeldungeon_obstacle_activations%` | 障碍物激活次数 |

### 通用元数据

通过 `%kangeldungeon_meta_<key>%` 可访问地牢实例中的任意元数据值。
key 中的点号 `.` 保留原样：

```
%kangeldungeon_meta_wave%                → instance.meta["wave"]
%kangeldungeon_meta_player.dead.Notch%   → instance.meta["player.dead.Notch"]
%kangeldungeon_meta_mob.kill.zombie%     → instance.meta["mob.kill.zombie"]
```

---

## dungeon/<name>/task/*.yml（任务触发器）

```yaml
任务ID:
  trigger: MOB_KILL                # 触发器类型
  filters:                         # 可选，过滤值必须完全匹配
    mob_type: SKELETON
  maxExecutions: 3                 # 最大执行次数，-1=无限
  cooldown: 0                      # 执行冷却（tick）
  agent:
    onTrigger: |-
      instance.setMeta("skeleton_kills", instance.getMetaAsInt("skeleton_kills") + 1);
```

详见 [任务系统文档](task-system.md)。

---

## MetaConfig 内置统计 Key

以下 key 由插件自动维护：

| Key | 说明 | 类型 |
|-----|------|------|
| `dungeon.start` | 地牢开始次数 | Int |
| `dungeon.complete` | 通关次数 | Int |
| `dungeon.fail` | 失败次数 | Int |
| `player.join` | 玩家加入次数 | Int |
| `player.leave` | 玩家离开次数 | Int |
| `player.dead` | 玩家死亡总数 | Int |
| `player.dead.<玩家名>` | 特定玩家死亡次数 | Int |
| `mob.kill` | 生物击杀总数 | Int |
| `mob.kill.<生物类型>` | 特定生物击杀数 | Int |
| `boss.kill` | Boss 击杀数 | Int |
| `boss.kill.<Boss名>` | 特定 Boss 击杀数 | Int |
| `monster.spawn.<configId>` | 怪物组生成次数 | Int |
| `monster.group.clear.<configId>` | 怪物组全清次数 | Int |
| `obstacle.*.<configId>` | 障碍物相关统计 | Int |
| `region.enter.<configId>` | 区域进入次数 | Int |
| `region.leave.<configId>` | 区域离开次数 | Int |
| `interact.trigger.<configId>` | 交互触发次数 | Int |

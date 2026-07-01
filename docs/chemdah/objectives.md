# Chemdah 任务目标 (Objectives) 文档

本文档列出了 KAngelDungeon 注册的所有 Chemdah 自定义任务目标，包含条件参数、变量和配置示例。

---

## 目录

1. [地牢目标](#1-地牢目标)
2. [实体目标](#2-实体目标)
3. [方块目标](#3-方块目标)
4. [物品目标](#4-物品目标)
5. [玩家目标](#5-玩家目标)
6. [位置目标](#6-位置目标)

---

## 1. 地牢目标

### 1.1 `kangeldp enter` — 进入地牢

- **事件**: `DungeonPlayerJoinPostEvent`
- **计数**: 每次进入 +1
- **目标类型**: `goal.amount` (int)

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple, dark_forest]` |
| `difficulty` | List | 匹配难度 ID | `difficulty: [hard, nightmare]` |
| `party` | Boolean | 需要组队进入 | `party: true` |

```yaml
tasks:
  enter:
    type: kangeldp enter
    condition:
      dungeon: [ancient_temple]
    goal:
      amount: 1
```

### 1.2 `kangeldp complete` — 通关地牢

- **事件**: `DungeonPlayerCompleteEvent`
- **计数**: 每次通关 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |
| `difficulty` | List | 匹配难度 ID | `difficulty: [hard]` |
| `party` | Boolean | 需要组队通关 | `party: true` |
| `min-players` | Int | 最少玩家数 | `min-players: 2` |
| `max-deaths` | Int | 全队最大死亡数 | `max-deaths: 5` |

```yaml
tasks:
  complete_hard:
    type: kangeldp complete
    condition:
      dungeon: [ancient_temple]
      difficulty: [hard]
      max-deaths: 3
    goal:
      amount: 1
```

### 1.3 `kangeldp fail` — 地牢失败

- **事件**: `DungeonPlayerFailEvent`
- **计数**: 每次失败 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |
| `difficulty` | List | 匹配难度 ID | `difficulty: [nightmare]` |

```yaml
tasks:
  fail_once:
    type: kangeldp fail
    condition:
      dungeon: [ancient_temple]
    goal:
      amount: 3
```

### 1.4 `kangeldp kill` — 地牢内击杀怪物

- **事件**: `DungeonMobKillEvent`
- **计数**: 每击杀一只怪物 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |
| `mob-type` | List | 匹配怪物类型 (Bukkit EntityType) | `mob-type: [ZOMBIE, SKELETON]` |
| `mob-name` | List | 匹配怪物自定义名 (含模糊匹配) | `mob-name: [§cBoss Zombie]` |
| `mob-id` | List | 匹配怪物 ID (MythicMobs ID) | `mob-id: [SkeletonKing]` |
| `min-level` | Double | 最低怪物等级 | `min-level: 10` |
| `max-level` | Double | 最高怪物等级 | `max-level: 50` |

**条件变量**: `mob-type`, `mob-name`, `mob-id`

```yaml
tasks:
  kill_zombies:
    type: kangeldp kill
    condition:
      dungeon: [ancient_temple]
      mob-type: [ZOMBIE]
    goal:
      amount: 10
```

### 1.5 `kangeldp death` — 地牢内死亡

- **事件**: `DungeonPlayerDeathEvent`
- **计数**: 每次死亡 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |

```yaml
tasks:
  die_in_dungeon:
    type: kangeldp death
    condition:
      dungeon: [ancient_temple]
    goal:
      amount: 1
```

### 1.6 `kangeldp respawn` — 地牢内复活

- **事件**: `DungeonPlayerRespawnEvent`
- **计数**: 每次复活 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |

```yaml
tasks:
  respawn_in_dungeon:
    type: kangeldp respawn
    condition:
      dungeon: [ancient_temple]
    goal:
      amount: 3
```

### 1.7 `kangeldp boss` — 击杀地牢 Boss

- **事件**: `DungeonBossKillEvent`
- **计数**: 每次击杀 Boss +1
- **前置要求**: 怪物 YAML 配置需设置 `boss: true`

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |
| `boss-name` | List | 匹配 Boss 自定义名 | `boss-name: [Dragon King]` |
| `boss-id` | List | 匹配 Boss ID | `boss-id: [SkeletonKing]` |

```yaml
# monsters/dragon_king.yml
DragonKing:
  boss: true
  monsters:
    - mob: SkeletonKing
      location: "10,64,10"
```

```yaml
tasks:
  kill_boss:
    type: kangeldp boss
    condition:
      dungeon: [ancient_temple]
      boss-name: [Dragon King]
    goal:
      amount: 1
```

### 1.8 `kangeldp survive` — 地牢中存活时间

- **类型**: Tickable（每 20 tick 检查一次）+ 无事件监听
- **计数**: 目标 `goal.amount` 为存活秒数
- **完成条件**: 玩家在地牢内的存活时间达到目标秒数自动完成

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |

> **注意**: 该目标 `isListener = false`，不依赖事件触发。计时从玩家加入地牢（`playerJoinTimes`）开始计算。离开地牢再进入会重新计时。

```yaml
tasks:
  survive_5min:
    type: kangeldp survive
    condition:
      dungeon: [ancient_temple]
    goal:
      amount: 300
```

### 1.9 `kangeldp perfect` — 无死亡通关

- **事件**: `DungeonPlayerCompleteEvent`
- **计数**: 通关时死亡数为 0 则 +1，否则 +0

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |

```yaml
tasks:
  perfect_run:
    type: kangeldp perfect
    condition:
      dungeon: [ancient_temple]
    goal:
      amount: 1
```

### 1.10 `kangeldp rating` — 评价通关

- **事件**: `DungeonPlayerCompleteEvent`
- **计数**: 通关时计算评分，每次通关 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |
| `min-rating` | Int | 最低评分 (0-100) | `min-rating: 80` |

**变量**:
| 变量名 | 说明 |
|--------|------|
| `rating` | 该次通关的评分 (0-100) |

**评分算法**: `基础分100 - 死亡数×10 + 击杀数×2 - 耗时分钟数`

```yaml
tasks:
  high_rating:
    type: kangeldp rating
    condition:
      dungeon: [ancient_temple]
      min-rating: 80
    goal:
      amount: 1
```

### 1.11 `kangeldp chest` — 打开地牢战利品箱

- **事件**: `InventoryOpenEvent` (Bukkit)
- **计数**: 每次打开箱子或木桶 +1
- **说明**: 监听原版 `InventoryOpenEvent`，在玩家打开箱子/木桶时触发。自动检测玩家是否在地牢中。

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `dungeon` | List | 匹配地牢模板名 | `dungeon: [ancient_temple]` |

```yaml
tasks:
  open_chests:
    type: kangeldp chest
    condition:
      dungeon: [ancient_temple]
    goal:
      amount: 5
```

---

## 2. 实体目标

### 2.1 `entity kill` — 击杀实体

- **事件**: `EntityDeathEvent`
- **计数**: 每次击杀 +1
- **说明**: 监听原版 Bukkit 事件，不限地牢内外

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` | List | 匹配实体类型 | `type: [ZOMBIE, SKELETON]` |
| `name` | List | 匹配实体自定义名（模糊匹配） | `name: [Boss]` |
| `world` | List | 匹配世界名 | `world: [world, world_nether]` |
| `spawn-reason` | List | 匹配生成原因 | `spawn-reason: [SPAWNER, NATURAL]` |

**变量**:
| 变量名 | 说明 |
|--------|------|
| `killed-type` | 被击杀实体的类型名 |
| `killed-name` | 被击杀实体的名称 |

```yaml
tasks:
  kill_10_zombies:
    type: entity kill
    condition:
      type: [ZOMBIE]
    goal:
      amount: 10
```

### 2.2 `entity tame` — 驯服动物

- **事件**: `EntityTameEvent`
- **计数**: 每次驯服 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` | List | 匹配实体类型 | `type: [WOLF, CAT]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `tamed-type`

```yaml
tasks:
  tame_wolves:
    type: entity tame
    condition:
      type: [WOLF]
    goal:
      amount: 2
```

### 2.3 `entity breed` — 繁殖动物

- **事件**: `EntityBreedEvent`
- **计数**: 每次繁殖 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` | List | 匹配实体类型 | `type: [SHEEP, COW]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `bred-type`

```yaml
tasks:
  breed_sheep:
    type: entity breed
    condition:
      type: [SHEEP]
    goal:
      amount: 5
```

### 2.4 `entity shear` — 剪羊毛/实体

- **事件**: `PlayerShearEntityEvent`
- **计数**: 每次剪切 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` | List | 匹配实体类型 | `type: [SHEEP]` |
| `world` | List | 匹配世界名 | `world: [world]` |

```yaml
tasks:
  shear_sheep:
    type: entity shear
    condition:
      type: [SHEEP]
    goal:
      amount: 3
```

---

## 3. 方块目标

### 3.1 `block break` — 破坏方块

- **事件**: `BlockBreakEvent`
- **计数**: 每次破坏 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配方块类型 | `type: [STONE, DIAMOND_ORE]` |
| `world` | List | 匹配世界名 | `world: [world]` |
| `biome` | List | 匹配群系 | `biome: [PLAINS]` |
| `position` | Position | 匹配位置区域 | `position: "10,60,10,20,70,20"` |
| `held-item` | List | 匹配手持物品 | `held-item: [DIAMOND_PICKAXE]` |
| `drop-items` | Boolean | 是否掉落物品 (设为 false 则阻止掉落) | `drop-items: false` |

**变量**: `broken-type`, `broken-world`

```yaml
tasks:
  mine_stone:
    type: block break
    condition:
      type: [STONE]
    goal:
      amount: 64
```

### 3.2 `block place` — 放置方块

- **事件**: `BlockPlaceEvent`
- **计数**: 每次放置 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配方块类型 | `type: [OAK_PLANKS]` |
| `world` | List | 匹配世界名 | `world: [world]` |
| `biome` | List | 匹配群系 | `biome: [FOREST]` |
| `position` | Position | 匹配位置区域 | `position: "100,64,100,110,70,110"` |
| `against` | List | 匹配放置目标方块 | `against: [STONE]` |

**变量**: `placed-type`

```yaml
tasks:
  build_house:
    type: block place
    condition:
      type: [OAK_PLANKS]
    goal:
      amount: 100
```

### 3.3 `block interact` — 交互方块

- **事件**: `PlayerInteractEvent`
- **计数**: 每次交互 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配被交互方块 | `type: [CHEST, FURNACE]` |
| `action` | List | 匹配交互动作 | `action: [RIGHT_CLICK_BLOCK]` |
| `world` | List | 匹配世界名 | `world: [world]` |
| `position` | Position | 匹配位置区域 | `position: "0,0,0,100,100,100"` |
| `held-item` | List | 匹配手持物品 | `held-item: [STICK]` |

```yaml
tasks:
  open_chests:
    type: block interact
    condition:
      type: [CHEST]
    goal:
      amount: 10
```

---

## 4. 物品目标

### 4.1 `item craft` — 合成物品

- **事件**: `CraftItemEvent`
- **计数**: 每次合成的物品数量

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配合成产物 | `type: [DIAMOND_SWORD]` |
| `recipe` | List | 匹配配方名 (NamespacedKey) | `recipe: [minecraft:iron_ingot]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `crafted-type`

```yaml
tasks:
  craft_swords:
    type: item craft
    condition:
      type: [DIAMOND_SWORD]
    goal:
      amount: 3
```

### 4.2 `item smelt` — 熔炼物品

- **事件**: `FurnaceExtractEvent`
- **计数**: 取出的熔炼产物数量

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配熔炼产物 | `type: [IRON_INGOT]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `smelted-type`

```yaml
tasks:
  smelt_iron:
    type: item smelt
    condition:
      type: [IRON_INGOT]
    goal:
      amount: 32
```

### 4.3 `item enchant` — 附魔物品

- **事件**: `EnchantItemEvent`
- **计数**: 每次附魔 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配被附魔物品 | `type: [DIAMOND_SWORD]` |
| `enchantment` | List | 匹配附魔 ID (NamespacedKey) | `enchantment: [sharpness]` |
| `min-level` | Int | 最低附魔等级 | `min-level: 3` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `enchanted-type`

```yaml
tasks:
  enchant_sword:
    type: item enchant
    condition:
      type: [DIAMOND_SWORD]
      enchantment: [sharpness]
      min-level: 3
    goal:
      amount: 1
```

### 4.4 `item pickup` — 拾取物品

- **事件**: `PlayerAttemptPickupItemEvent` (Paper)
- **计数**: 拾取的物品数量

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配拾取物品 | `type: [DIAMOND]` |
| `world` | List | 匹配世界名 | `world: [world]` |
| `position` | Position | 匹配位置区域 | `position: "0,0,0,100,100,100"` |

**变量**: `picked-type`

```yaml
tasks:
  collect_diamonds:
    type: item pickup
    condition:
      type: [DIAMOND]
    goal:
      amount: 10
```

### 4.5 `item consume` — 食用/饮用

- **事件**: `PlayerItemConsumeEvent`
- **计数**: 每次消耗 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `type` / `material` | List | 匹配消耗品 | `type: [GOLDEN_APPLE, COOKED_BEEF]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `consumed-type`

```yaml
tasks:
  eat_apples:
    type: item consume
    condition:
      type: [GOLDEN_APPLE]
    goal:
      amount: 5
```

### 4.6 `item fish` — 钓鱼

- **事件**: `PlayerFishEvent`
- **计数**: 仅 `CAUGHT_FISH` 状态 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `state` | List | 匹配钓鱼状态 | `state: [CAUGHT_FISH]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `fish-state`

```yaml
tasks:
  fish_cod:
    type: item fish
    condition:
      state: [CAUGHT_FISH]
    goal:
      amount: 10
```

---

## 5. 玩家目标

### 5.1 `player damage dealt` — 造成伤害

- **事件**: `EntityDamageByEntityEvent`
- **计数**: 造成的伤害值 (取整，至少 1)

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `target-type` | List | 匹配目标实体类型 | `target-type: [ZOMBIE]` |
| `damage-cause` | List | 匹配伤害原因 | `damage-cause: [ENTITY_ATTACK, PROJECTILE]` |
| `min-damage` | Double | 最低单次伤害 | `min-damage: 5.0` |
| `world` | List | 匹配世界名 | `world: [world]` |
| `held-item` | List | 匹配手持物品 | `held-item: [DIAMOND_SWORD]` |

**变量**: `target-type`

```yaml
tasks:
  deal_damage:
    type: player damage dealt
    condition:
      target-type: [ZOMBIE]
    goal:
      amount: 100
```

### 5.2 `player damage taken` — 受到伤害

- **事件**: `EntityDamageEvent`
- **计数**: 受到的伤害值 (取整，至少 1)

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `damage-cause` | List | 匹配伤害原因 | `damage-cause: [FALL, LAVA]` |
| `min-damage` | Double | 最低单次伤害 | `min-damage: 10.0` |
| `world` | List | 匹配世界名 | `world: [world_nether]` |
| `attacker-type` | List | 匹配攻击者类型 | `attacker-type: [SKELETON]` |

**变量**: `damage-cause`

```yaml
tasks:
  survive_fall:
    type: player damage taken
    condition:
      damage-cause: [FALL]
    goal:
      amount: 50
```

### 5.3 `player level` — 达到等级

- **事件**: `PlayerLevelChangeEvent`
- **计数**: 每次升级 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `min-level` | Int | 最低等级 | `min-level: 30` |
| `max-level` | Int | 最高等级 | `max-level: 50` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `new-level`, `old-level`

```yaml
tasks:
  reach_level_30:
    type: player level
    condition:
      min-level: 30
    goal:
      amount: 1
```

### 5.4 `player command` — 执行指令

- **事件**: `PlayerCommandPreprocessEvent`
- **计数**: 每次执行指令 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `command` | List | 匹配指令前缀 | `command: [/warp, /spawn]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `command-used`, `full-command`

```yaml
tasks:
  use_warp:
    type: player command
    condition:
      command: [/warp]
    goal:
      amount: 5
```

### 5.5 `player statistic` — 统计里程碑

- **事件**: `PlayerStatisticIncrementEvent` (Paper)
- **计数**: 增加的统计值

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `statistic` | List | 匹配统计名 (key 或 name) | `statistic: [jump, walk_one_cm]` |
| `entity-type` | List | 匹配实体类型统计子项 | `entity-type: [ZOMBIE]` (如 `kill_entity`) |
| `material` | List | 匹配物品/方块统计子项 | `material: [DIAMOND]` (如 `mine_block`) |
| `min-value` | Int | 最低总值 | `min-value: 1000` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `statistic-name`

```yaml
tasks:
  jump_100:
    type: player statistic
    condition:
      statistic: [jump]
      min-value: 100
    goal:
      amount: 100
```

---

## 6. 位置目标

### 6.1 `position` — 到达位置

- **事件**: `PlayerMoveEvent`
- **计数**: 0 (到达即完成)
- **说明**: 当玩家移动到目标位置区域时任务完成。

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `position` | Position | 🟢 **必填** 目标区域 | `position: "100,64,100,110,70,110"` |
| `world` | List | 匹配世界名 | `world: [world]` |
| `biome` | List | 匹配群系 | `biome: [PLAINS]` |
| `min-y` | Int | 最低 Y 高度 | `min-y: 60` |
| `max-y` | Int | 最高 Y 高度 | `max-y: 80` |

**变量**:
| 变量名 | 说明 |
|--------|------|
| `world-name` | 当前世界名 |
| `biome-name` | 当前群系名 |
| `x`, `y`, `z` | 当前坐标 |

```yaml
tasks:
  reach_tower:
    type: position
    condition:
      position: "100,64,100,120,80,120"
      world: [world]
    goal:
      amount: 1  # 到达一次即可
```

### 6.2 `biome` — 进入群系

- **事件**: `PlayerMoveEvent`
- **计数**: 0 (进入即完成)
- **说明**: 只在生物群系变化时检测。跨群系边界时触发。

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `biome` | List | 🟢 **必填** 目标群系 | `biome: [DESERT, BADLANDS]` |
| `from-biome` | List | 来自群系 | `from-biome: [PLAINS]` |
| `world` | List | 匹配世界名 | `world: [world]` |

**变量**: `biome-name`

```yaml
tasks:
  explore_desert:
    type: biome
    condition:
      biome: [DESERT]
    goal:
      amount: 1
```

### 6.3 `world` — 切换世界

- **事件**: `PlayerChangedWorldEvent`
- **计数**: 每次切换 +1

| 条件 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `world` | List | 🟢 **必填** 目标世界 | `world: [world_nether]` |
| `from-world` | List | 来自世界 | `from-world: [world]` |

**变量**: `world-name`, `from-world-name`

```yaml
tasks:
  enter_nether:
    type: world
    condition:
      world: [world_nether]
    goal:
      amount: 1
```

---

## Position 格式说明

`position` 条件使用与 Chemdah 原生相同的格式：

```
"minX,minY,minZ,maxX,maxY,maxZ"
```

示例: `"10,60,10,20,70,20"` 表示从 `(10,60,10)` 到 `(20,70,20)` 的立方体区域。

---

## 注册说明

所有目标在插件启动时通过 `KAngelDungeon` 主类的 `onEnable` 方法以反射方式自动注册：

```kotlin
val chemdahClass = Class.forName("io.github.zzzyyylllty.kangeldungeon.util.chemdah.ChemdahObjectives")
chemdahClass.getMethod("register").invoke(null)
```

**前置条件**: 需要服务器安装 Chemdah 插件。如果未安装，目标注册将被静默跳过，不影响插件正常功能。

---

## 扩展：添加自定义目标

参考以下模式创建新的 Chemdah 任务目标：

```kotlin
@Dependency("YourPlugin")  // 可选：仅在依赖存在时注册
object YourCustomObjective : ObjectiveCountableI<YourEvent>() {
    override val name = "your type name"
    override val event = YourEvent::class.java

    init {
        handler { it.player }  // 从事件中提取 Player
        addSimpleCondition("condition-name") { data, e ->
            data.asList().any { it.equals(e.someField, true) }
        }
        addConditionVariable("variable-name") { _, _, e -> e.someField.toString() }
    }

    override fun getCount(event: YourEvent): Int = 1
}
```

然后在 `ChemdahObjectives.register()` 中添加 `YourCustomObjective.register()`。

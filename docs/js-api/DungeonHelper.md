# DungeonHelper 地牢帮助工具

在 JS 脚本中通过 `DungeonHelper` 访问。

---

## 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `DungeonHelper.playerPreviousLocations` | `Map<UUID, Location>` | 玩家进入地牢前的位置缓存，用于离开时传送回去 |

---

## 方法列表

### `DungeonHelper.getWorldName(dungeonName, dungeonUUID)`

获取地牢的 Bukkit 世界名称。格式: `KDP_{dungeonName}_{uuid}`

| 参数 | 类型 | 说明 |
|------|------|------|
| `dungeonName` | `String` | 地牢模板名称 |
| `dungeonUUID` | `UUID` | 地牢实例 UUID |

**返回值**: `String` — 世界名称

---

### `DungeonHelper.dungeonWorldExists(dungeonInstance)`

检查地牢世界是否已加载。

| 参数 | 类型 | 说明 |
|------|------|------|
| `dungeonInstance` | `DungeonInstance` | 地牢实例 |

**返回值**: `Boolean` — 世界是否存在

---

### `DungeonHelper.createDungeonWorld(template, dungeonUUID)`

创建/加载地牢世界（自动检测使用 region 文件或 schematic）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `template` | `DungeonTemplate` | 地牢模板 |
| `dungeonUUID` | `UUID` | 地牢实例 UUID |

**返回值**: `World?` — 创建的世界对象，失败返回 null

---

### `DungeonHelper.createDungeonWorldByRegionFile(template, dungeonUUID)`

从模板 region 文件创建地牢世界。

| 参数 | 类型 | 说明 |
|------|------|------|
| `template` | `DungeonTemplate` | 地牢模板 |
| `dungeonUUID` | `UUID` | 地牢实例 UUID |

**返回值**: `World?`

---

### `DungeonHelper.createDungeonWorldBySchematic(template, dungeonUUID)`

通过 WorldEdit Schematic 创建地牢世界。

| 参数 | 类型 | 说明 |
|------|------|------|
| `template` | `DungeonTemplate` | 地牢模板 |
| `dungeonUUID` | `UUID` | 地牢实例 UUID |

**返回值**: `World?`

---

### `DungeonHelper.unloadDungeonWorld(dungeonInstance, syncDelete?)`

卸载地牢世界。

| 参数 | 类型 | 说明 |
|------|------|------|
| `dungeonInstance` | `DungeonInstance` | 地牢实例 |
| `syncDelete` | `Boolean` | 是否同步删除，默认 false |

**返回值**: 无

---

### `DungeonHelper.createDungeon(templateName, players, leader, meta, difficultyId?)`

创建一个完整的地牢实例（包含世界创建、玩家加入等）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `templateName` | `String` | 地牢模板名称 |
| `players` | `Collection<Player>` | 加入地牢的玩家 |
| `leader` | `Player` | 队长 |
| `meta` | `Map<String, Any>` | 地牢元数据 |
| `difficultyId` | `String?` | 难度 ID，默认 null |

**返回值**: `DungeonInstance?`

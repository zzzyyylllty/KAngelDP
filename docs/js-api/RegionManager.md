# RegionManager 区域管理器

在 JS 脚本中通过 `RegionManager` 访问。

---

## 方法列表

### `RegionManager.isPlayerInRegion(worldName, playerUUID, regionId)`

检查玩家是否在指定区域内。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 地牢世界名称 |
| `playerUUID` | `UUID` | 玩家 UUID |
| `regionId` | `String` | 区域配置 ID |

**返回值**: `Boolean` — 是否在区域内

---

### `RegionManager.getPlayersInRegion(worldName, regionId)`

获取区域内的所有在线玩家。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 地牢世界名称 |
| `regionId` | `String` | 区域配置 ID |

**返回值**: `List<Player>` — 区域内的在线玩家列表

---

### `RegionManager.getPlayerRegions(worldName, playerUUID)`

获取玩家当前所在的所有区域 ID。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 地牢世界名称 |
| `playerUUID` | `UUID` | 玩家 UUID |

**返回值**: `Set<String>` — 区域 ID 集合

---

### `RegionManager.clearWorld(worldName)`

清理指定世界的区域追踪数据（触发所有玩家离开事件）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 地牢世界名称 |

**返回值**: 无

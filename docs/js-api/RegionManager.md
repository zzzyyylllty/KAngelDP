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

```js
var inZone = RegionManager.isPlayerInRegion(instance.worldName, player.getUniqueId(), "danger_zone");
if (inZone) {
    PlayerUtil.sendMessage(player, "<red>你处于危险区域！");
}
```

---

### `RegionManager.getPlayersInRegion(worldName, regionId)`

获取区域内的所有在线玩家。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 地牢世界名称 |
| `regionId` | `String` | 区域配置 ID |

```js
var inRoom = RegionManager.getPlayersInRegion(instance.worldName, "boss_room");
inRoom.forEach(function(p) {
    PlayerUtil.sendTitle(p, "<red>⚔ Boss 战</red>", "", 10, 30, 10);
});
```

---

### `RegionManager.getPlayerRegions(worldName, playerUUID)`

获取玩家当前所在的所有区域 ID。

```js
var regions = RegionManager.getPlayerRegions(instance.worldName, player.getUniqueId());
regions.forEach(function(rid) {
    Sys.println("玩家在区域: " + rid);
});
```

---

### `RegionManager.clearWorld(worldName)`

清理指定世界的区域追踪数据（触发所有玩家离开事件）。

```js
RegionManager.clearWorld(instance.worldName);
```

---

## 常见用法

**检查玩家是否在安全区**:
```js
var inSafe = RegionManager.isPlayerInRegion(instance.worldName, player.getUniqueId(), "safe_zone");
if (!inSafe) {
    PlayerUtil.damage(player, 2.0);  // 在非安全区持续掉血
}
```

**区域计数**:
```js
var count = RegionManager.getPlayersInRegion(instance.worldName, "puzzle_room").length;
if (count >= 2) {
    // 至少 2 人在解谜室时才触发
}
```

**离开区域触发事件**:
```js
// 在 onLeave 代理脚本中:
PlayerUtil.sendMessage(player, "<yellow>你离开了安全区！");
PlayerUtil.addPotionEffect(player, "POISON", 100, 1);
```

# PlayerUtil 玩家工具

在 JS 脚本中通过 `PlayerUtil` 访问。

---

## 方法列表

### `PlayerUtil.addPotionEffect(player, type, duration?, amplifier?, ambient?, particles?, icon?)`

给玩家添加药水效果。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `type` | `String` | — | 效果类型（如 `"SPEED"`、`"INVISIBILITY"`） |
| `duration` | `Int` | 30 | 持续时间（tick） |
| `amplifier` | `Int` | 0 | 等级（0=I, 1=II） |
| `ambient` | `Boolean` | true | 是否减少粒子可见度 |
| `particles` | `Boolean` | true | 是否显示粒子 |
| `icon` | `Boolean` | true | 是否显示图标 |

**返回值**: 无

---

### `PlayerUtil.addPotionEffect(player, type, duration?, amplifier?)`

给玩家添加药水效果（简化版）。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `type` | `String` | — | 效果类型 |
| `duration` | `Int` | 30 | 持续时间（tick） |
| `amplifier` | `Int` | 0 | 等级 |

**返回值**: 无

---

### `PlayerUtil.removePotionEffect(player, type)`

移除玩家身上的指定药水效果。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `type` | `String` | 效果类型 |

**返回值**: 无

---

### `PlayerUtil.showTitle(player, title, subTitle, durationIn?, duration?, durationOut?)`

给玩家发送标题（Component 版）。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `title` | `Component` | — | 标题组件 |
| `subTitle` | `Component` | — | 副标题组件 |
| `durationIn` | `Int` | 30 | 淡入 tick |
| `duration` | `Int` | 30 | 停留 tick |
| `durationOut` | `Int` | 30 | 淡出 tick |

**返回值**: 无

---

### `PlayerUtil.sendTitle(player, title, subtitle?, fadeIn?, stay?, fadeOut?)`

给玩家发送标题（MiniMessage 字符串版）。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `title` | `String` | — | MiniMessage 标题 |
| `subtitle` | `String` | `""` | MiniMessage 副标题 |
| `fadeIn` | `Int` | 10 | 淡入 tick |
| `stay` | `Int` | 70 | 停留 tick |
| `fadeOut` | `Int` | 20 | 淡出 tick |

**返回值**: 无

**示例**:
```js
PlayerUtil.sendTitle(player, "<gold>胜利!</gold>", "<green>恭喜通关</green>", 10, 70, 20);
```

---

### `PlayerUtil.sendMessage(player, message)`

使用 MiniMessage 发送聊天消息。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `message` | `String` | MiniMessage 格式消息 |

**返回值**: 无

**示例**:
```js
PlayerUtil.sendMessage(player, "<red>你死了!</red>");
```

---

### `PlayerUtil.sendActionBar(player, message)`

使用 MiniMessage 发送 ActionBar 消息。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `message` | `String` | MiniMessage 格式消息 |

**返回值**: 无

---

### `PlayerUtil.heal(player)`

完全治愈玩家（满血、满饱食、灭火）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |

**返回值**: 无

---

### `PlayerUtil.setHealth(player, health)`

设置玩家生命值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `health` | `Double` | 生命值（0 ~ 最大生命） |

**返回值**: 无

---

### `PlayerUtil.setFood(player, food)`

设置玩家食物值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `food` | `Int` | 食物值（0 ~ 20） |

**返回值**: 无

---

### `PlayerUtil.setSaturation(player, saturation)`

设置玩家饱和度。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `saturation` | `Float` | 饱和度（0 ~ 20） |

**返回值**: 无

---

### `PlayerUtil.setLevel(player, level)`

设置玩家经验等级。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `level` | `Int` | 经验等级（>= 0） |

**返回值**: 无

---

### `PlayerUtil.giveItem(player, itemStack)`

给予玩家物品（背包满则掉落在地上）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: 无

---

### `PlayerUtil.giveItemStack(player, material, amount?)`

按材料名和数量给予玩家物品。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `material` | `String` | — | 材料名（如 `"DIAMOND"`） |
| `amount` | `Int` | 1 | 数量 |

**返回值**: 无

**示例**:
```js
PlayerUtil.giveItemStack(player, "DIAMOND", 5);
```

---

### `PlayerUtil.takeItem(player, material, amount)`

从玩家背包移除物品。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `material` | `String` | 材料名 |
| `amount` | `Int` | 移除数量 |

**返回值**: `Boolean` — 是否成功移除（数量不足时返回 false）

---

### `PlayerUtil.countItem(player, material)`

统计玩家背包中某材料的数量。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `material` | `String` | 材料名 |

**返回值**: `Int` — 物品总数

---

### `PlayerUtil.hasItem(player, material, amount?)`

检查玩家是否有足够数量的物品。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `material` | `String` | — | 材料名 |
| `amount` | `Int` | 1 | 需要数量 |

**返回值**: `Boolean`

---

### `PlayerUtil.clearInventory(player)`

清空玩家背包。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |

**返回值**: 无

---

### `PlayerUtil.setGameMode(player, gamemode)`

设置玩家游戏模式。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `gamemode` | `String` | 游戏模式（`"SURVIVAL"`、`"CREATIVE"`、`"ADVENTURE"`、`"SPECTATOR"`） |

**返回值**: 无

---

### `PlayerUtil.teleport(player, x, y, z, worldName?)`

传送玩家到坐标。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `x` | `Double` | — | X 坐标 |
| `y` | `Double` | — | Y 坐标 |
| `z` | `Double` | — | Z 坐标 |
| `worldName` | `String`? | `null` | 世界名，留空则为当前世界 |

**返回值**: 无

---

### `PlayerUtil.damage(player, amount)`

对玩家造成伤害。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `amount` | `Double` | 伤害值（>= 0） |

**返回值**: 无

---

### `PlayerUtil.kick(player, reason?)`

踢出玩家。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `reason` | `String` | `""` | 踢出原因（支持 MiniMessage） |

**返回值**: 无

---

### `PlayerUtil.getPlayer(name)`

通过名字获取在线玩家。

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 玩家名 |

**返回值**: `Player?` — 在线玩家对象，未找到返回 null

---

### `PlayerUtil.isOnline(playerName)`

检查玩家是否在线。

| 参数 | 类型 | 说明 |
|------|------|------|
| `playerName` | `String` | 玩家名 |

**返回值**: `Boolean`

---

### `PlayerUtil.getOnlinePlayers()`

获取所有在线玩家列表。

**返回值**: `List<Player>`

---

### `PlayerUtil.setAllowFlight(player, allow)`

设置玩家飞行状态。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `allow` | `Boolean` | 是否允许飞行 |

**返回值**: 无

---

### `PlayerUtil.setFlySpeed(player, speed)`

设置玩家飞行速度。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `speed` | `Float` | 速度（-1 ~ 1，默认 0.1） |

**返回值**: 无

---

### `PlayerUtil.setWalkSpeed(player, speed)`

设置玩家行走速度。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `speed` | `Float` | 速度（-1 ~ 1，默认 0.2） |

**返回值**: 无

---

### `PlayerUtil.hasPermission(player, permission)`

检查玩家权限。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `permission` | `String` | 权限节点 |

**返回值**: `Boolean`

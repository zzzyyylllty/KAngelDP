# PlayerUtil 玩家工具

在 JS 脚本中通过 `PlayerUtil` 访问。

---

## 方法列表

### `PlayerUtil.addPotionEffect(player, type, duration?, amplifier?, ambient?, particles?, icon?)`

给玩家添加药水效果。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `type` | `String` | — | 效果类型（如 `"SPEED"`、`"INVISIBILITY"`、`"POISON"`） |
| `duration` | `Int` | 30 | 持续时间（tick，20 tick = 1 秒） |
| `amplifier` | `Int` | 0 | 等级（0=I 级, 1=II 级） |
| `ambient` | `Boolean` | true | 是否减少粒子可见度 |
| `particles` | `Boolean` | true | 是否显示粒子 |
| `icon` | `Boolean` | true | 是否显示图标 |

**示例**:
```js
// 速度 II，30 秒
PlayerUtil.addPotionEffect(player, "SPEED", 600, 1);

// 隐身，60 秒，无粒子（隐藏效果）
PlayerUtil.addPotionEffect(player, "INVISIBILITY", 1200, 0, true, false, false);
```

---

### `PlayerUtil.removePotionEffect(player, type)`

移除玩家身上的指定药水效果。

```js
PlayerUtil.removePotionEffect(player, "POISON");
PlayerUtil.removePotionEffect(player, "WEAKNESS");
```

---

### `PlayerUtil.sendTitle(player, title, subtitle?, fadeIn?, stay?, fadeOut?)`

使用 MiniMessage 格式发送标题。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `title` | `String` | — | MiniMessage 格式大标题 |
| `subtitle` | `String` | `""` | MiniMessage 格式副标题 |
| `fadeIn` | `Int` | 10 | 淡入 tick |
| `stay` | `Int` | 70 | 停留 tick |
| `fadeOut` | `Int` | 20 | 淡出 tick |

```js
PlayerUtil.sendTitle(player, "<gold>胜利!</gold>", "<green>恭喜通关</green>", 10, 70, 20);
PlayerUtil.sendTitle(player, "<red>失败...</red>", "<gray>下次加油</gray>");
```

---

### `PlayerUtil.sendMessage(player, message)`

使用 MiniMessage 发送聊天消息。

```js
PlayerUtil.sendMessage(player, "<red>你死了!</red>");
PlayerUtil.sendMessage(player, "<gradient:gold:yellow>恭喜获得奖励！</gradient>");
```

---

### `PlayerUtil.sendActionBar(player, message)`

使用 MiniMessage 发送 ActionBar 消息（物品栏上方）。

```js
PlayerUtil.sendActionBar(player, "<yellow>⏳ 正在加载地牢...</yellow>");
PlayerUtil.sendActionBar(player, "<light_purple>❤ 你已进入危险区域</light_purple>");
```

---

### `PlayerUtil.showTitle(player, title, subTitle, fadeIn?, stay?, fadeOut?)`

给玩家发送标题（Component 对象版，用于预构建的组件）。

```js
var title = mmUtil.deserialize("<gold>Boss 战!</gold>");
var sub = mmUtil.deserialize("<red>准备战斗</red>");
PlayerUtil.showTitle(player, title, sub, 10, 50, 20);
```

---

### `PlayerUtil.heal(player)`

完全治愈玩家（满血 + 满饱食 + 灭火）。

```js
PlayerUtil.heal(player);
```

---

### `PlayerUtil.setHealth(player, health)`

设置玩家生命值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `health` | `Double` | 生命值（0 ~ 最大生命，自动裁剪） |

```js
PlayerUtil.setHealth(player, 20.0);     // 满血
PlayerUtil.setHealth(player, 1.0);      // 残血（剩半颗心）
```

---

### `PlayerUtil.setFood(player, food)`

设置玩家食物值。

```js
PlayerUtil.setFood(player, 20);   // 满饱食
PlayerUtil.setFood(player, 6);    // 半饥饿
```

---

### `PlayerUtil.setSaturation(player, saturation)`

设置玩家饱和度（食物消化前的快速恢复阶段）。

```js
PlayerUtil.setSaturation(player, 10.0);  // 高饱和度
```

---

### `PlayerUtil.setLevel(player, level)`

设置玩家经验等级。

```js
PlayerUtil.setLevel(player, 0);   // 清空等级
PlayerUtil.setLevel(player, 30);  // 设置 30 级
```

---

### `PlayerUtil.giveItem(player, itemStack)`

给予玩家物品（背包满则掉落在地上）。

```js
var sword = ItemStackUtil.createItem("DIAMOND_SWORD", 1, "<gold>Excalibur</gold>", ["<gray>传说之剑</gray>"]);
PlayerUtil.giveItem(player, sword);
```

---

### `PlayerUtil.giveItemStack(player, material, amount?)`

按材料名和数量给予玩家物品。

```js
PlayerUtil.giveItemStack(player, "DIAMOND", 5);
PlayerUtil.giveItemStack(player, "COOKED_BEEF", 16);
```

---

### `PlayerUtil.takeItem(player, material, amount)`

从玩家背包移除物品。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `material` | `String` | 材料名 |
| `amount` | `Int` | 移除数量 |

**返回值**: `Boolean` — 数量不足时返回 false

```js
if (PlayerUtil.takeItem(player, "DIAMOND", 3)) {
    PlayerUtil.sendMessage(player, "<green>消耗了 3 个钻石!");
} else {
    PlayerUtil.sendMessage(player, "<red>钻石不足!");
}
```

---

### `PlayerUtil.countItem(player, material)`

统计玩家背包中某材料的数量。

```js
var diamonds = PlayerUtil.countItem(player, "DIAMOND");
if (diamonds >= 10) {
    PlayerUtil.sendMessage(player, "<green>你有足够的钻石!");
}
```

---

### `PlayerUtil.hasItem(player, material, amount?)`

检查玩家是否有足够数量的物品。

```js
if (PlayerUtil.hasItem(player, "EMERALD", 5)) {
    PlayerUtil.takeItem(player, "EMERALD", 5);
    PlayerUtil.giveItemStack(player, "DIAMOND", 1);
    PlayerUtil.sendMessage(player, "<gold>兑换成功！5 绿宝石 → 1 钻石");
}
```

---

### `PlayerUtil.clearInventory(player)`

清空玩家背包。

```js
PlayerUtil.clearInventory(player);
```

---

### `PlayerUtil.setGameMode(player, gamemode)`

设置玩家游戏模式。支持: `"SURVIVAL"` / `"CREATIVE"` / `"ADVENTURE"` / `"SPECTATOR"`

```js
PlayerUtil.setGameMode(player, "SPECTATOR");
PlayerUtil.setGameMode(player, "SURVIVAL");
```

---

### `PlayerUtil.teleport(player, x, y, z, worldName?)`

传送玩家到坐标。不指定 worldName 则保持当前世界。

```js
// 当前世界传送
PlayerUtil.teleport(player, 100, 64, 100);

// 跨世界传送
PlayerUtil.teleport(player, 0, 100, 0, "world_nether");
```

---

### `PlayerUtil.damage(player, amount)`

对玩家造成伤害。

```js
PlayerUtil.damage(player, 10.0);  // 5 颗心
PlayerUtil.damage(player, 40.0);  // 20 颗心（通常足够击杀）
```

---

### `PlayerUtil.kick(player, reason?)`

踢出玩家（支持 MiniMessage 原因）。

```js
PlayerUtil.kick(player, "<red>地牢已关闭</red>");
```

---

### `PlayerUtil.getPlayer(name)`

通过名字获取在线玩家。

```js
var target = PlayerUtil.getPlayer("Notch");
if (target != null) {
    PlayerUtil.teleport(target, 0, 100, 0);
}
```

---

### `PlayerUtil.isOnline(playerName)`

检查玩家是否在线。

```js
if (PlayerUtil.isOnline("player123")) {
    var p = PlayerUtil.getPlayer("player123");
    PlayerUtil.sendMessage(p, "<aqua>欢迎回来！</aqua>");
}
```

---

### `PlayerUtil.getOnlinePlayers()`

获取所有在线玩家列表。

```js
var all = PlayerUtil.getOnlinePlayers();
all.forEach(function(p) {
    PlayerUtil.sendMessage(p, "<yellow>全体公告：地牢维护中</yellow>");
});
```

---

### `PlayerUtil.setAllowFlight(player, allow)`

设置玩家飞行状态。

```js
PlayerUtil.setAllowFlight(player, true);   // 允许飞行
PlayerUtil.setAllowFlight(player, false);  // 禁止飞行
```

---

### `PlayerUtil.setFlySpeed(player, speed)`

设置玩家飞行速度（-1 ~ 1，默认 0.1）。

```js
PlayerUtil.setFlySpeed(player, 0.5);   // 快速飞行
PlayerUtil.setFlySpeed(player, 0.0);   // 悬停不动
```

---

### `PlayerUtil.setWalkSpeed(player, speed)`

设置玩家行走速度（-1 ~ 1，默认 0.2）。

```js
PlayerUtil.setWalkSpeed(player, 0.5);  // 加速
PlayerUtil.setWalkSpeed(player, 0.1);  // 减速
```

---

### `PlayerUtil.hasPermission(player, permission)`

检查玩家权限。

```js
if (PlayerUtil.hasPermission(player, "kangeldungeon.admin")) {
    PlayerUtil.sendMessage(player, "<red>管理员已确认</red>");
}
```

---

### `PlayerUtil.getHealth(player)`

获取玩家当前生命值。

```js
var hp = PlayerUtil.getHealth(player);
if (hp < 10) {
    PlayerUtil.sendMessage(player, "<red>你血量很低！</red>");
}
```

### `PlayerUtil.getMaxHealth(player)`

获取玩家最大生命值。

```js
var maxHp = PlayerUtil.getMaxHealth(player);
```

### `PlayerUtil.getFoodLevel(player)`

获取玩家食物值（0~20）。

```js
if (PlayerUtil.getFoodLevel(player) < 6) {
    PlayerUtil.sendMessage(player, "<yellow>你需要吃东西！</yellow>");
}
```

### `PlayerUtil.getSaturation(player)`

获取玩家饱和度（0~20）。

```js
var sat = PlayerUtil.getSaturation(player);
```

### `PlayerUtil.getLevel(player)`

获取玩家经验等级。

```js
var level = PlayerUtil.getLevel(player);
PlayerUtil.sendMessage(player, "当前等级: " + level);
```

### `PlayerUtil.getExp(player)`

获取玩家经验进度（0.0 ~ 1.0）。

```js
var exp = PlayerUtil.getExp(player);
if (exp >= 1.0) {
    PlayerUtil.sendMessage(player, "<green>可以升级了！</green>");
}
```

---

### `PlayerUtil.getGameMode(player)`

获取玩家游戏模式。

```js
var mode = PlayerUtil.getGameMode(player);  // "SURVIVAL", "CREATIVE" 等
```

---

### `PlayerUtil.getLocation(player)`

获取玩家位置（Bukkit Location 对象）。

```js
var loc = PlayerUtil.getLocation(player);
var x = loc.getX();
var y = loc.getY();
var z = loc.getZ();
```

### `PlayerUtil.getWorld(player)`

获取玩家所在世界名。

```js
var worldName = PlayerUtil.getWorld(player);
```

---

### `PlayerUtil.getPing(player)`

获取玩家延迟（ping）。

```js
var ping = PlayerUtil.getPing(player);
if (ping > 200) {
    PlayerUtil.sendMessage(player, "<yellow>你的网络延迟较高：" + ping + "ms</yellow>");
}
```

---

### `PlayerUtil.hasPotionEffect(player, type)`

检查玩家是否有指定药水效果。

```js
if (PlayerUtil.hasPotionEffect(player, "SPEED")) {
    PlayerUtil.sendMessage(player, "<green>你已经有速度效果了</green>");
}
```

---

### `PlayerUtil.playSound(player, sound, volume?, pitch?)`

向玩家播放音效。

```js
PlayerUtil.playSound(player, "entity_experience_orb_pickup", 1.0, 1.0);
PlayerUtil.playSound(player, "entity_generic_explode", 1.0, 0.5);
```

### `PlayerUtil.playSoundAt(worldName, x, y, z, sound, volume?, pitch?)`

在世界坐标播放音效（所有玩家可听到）。

```js
PlayerUtil.playSoundAt("world", 100, 64, 100, "entity_generic_explode", 1.0, 1.0);
```

---

## 常见用法（实用组合）

**Boss 战开场效果**:
```js
PlayerUtil.setAllowFlight(player, false);
PlayerUtil.setGameMode(player, "SURVIVAL");
PlayerUtil.heal(player);
PlayerUtil.sendTitle(player, "<red>⚔ 巫妖王</red>", "<dark_red>最终决战</dark_red>", 20, 60, 20);
PlayerUtil.addPotionEffect(player, "DAMAGE_RESISTANCE", 100, 0);
```

**玩家阵亡处理**:
```js
PlayerUtil.setGameMode(player, "SPECTATOR");
PlayerUtil.sendMessage(player, "<gray>你已阵亡，等待队友通关...</gray>");
PlayerUtil.clearInventory(player);
```

**退出地牢清理状态**:
```js
PlayerUtil.clearInventory(player);
PlayerUtil.setLevel(player, 0);
PlayerUtil.heal(player);
PlayerUtil.removePotionEffect(player, "SPEED");
PlayerUtil.removePotionEffect(player, "INCREASE_DAMAGE");
PlayerUtil.setGameMode(player, "SURVIVAL");
PlayerUtil.setAllowFlight(player, false);
```

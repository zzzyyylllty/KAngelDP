# EntityUtil 实体工具

在 JS 脚本中通过 `EntityUtil` 访问。

---

## 方法列表

### `EntityUtil.isAlive(entity)`

判断实体是否存活。

```js
if (EntityUtil.isAlive(entity)) {
    EntityUtil.damage(entity, 5.0);
}
```

---

### `EntityUtil.damage(entity, amount)`

对实体造成伤害。

| 参数 | 类型 | 说明 |
|------|------|------|
| `entity` | `LivingEntity` | 目标实体（必须是 LivingEntity） |
| `amount` | `Double` | 伤害值（>= 0） |

```js
EntityUtil.damage(entity, 10.0);
EntityUtil.damage(entity, 100.0);  // 一击必杀
```

---

### `EntityUtil.remove(entity)`

移除实体。

```js
EntityUtil.remove(entity);
```

---

### `EntityUtil.getEntitiesNear(worldName, x, y, z, radius)`

获取位置附近的实体列表。

```js
var entities = EntityUtil.getEntitiesNear("world", 100, 64, 100, 10);
entities.forEach(function(e) {
    EntityUtil.setGlowing(e, true);
});
```

---

### `EntityUtil.getPlayersNear(worldName, x, y, z, radius)`

获取位置附近的玩家列表。

```js
var nearby = EntityUtil.getPlayersNear(worldName, x, y, z, 5);
nearby.forEach(function(p) {
    PlayerUtil.sendMessage(p, "<yellow>你感受到了强大的能量...</yellow>");
});
```

---

### `EntityUtil.getLivingEntitiesNear(worldName, x, y, z, radius)`

获取位置附近的存活生物列表。

```js
var mobs = EntityUtil.getLivingEntitiesNear(worldName, x, y, z, 15);
mobs.forEach(function(m) {
    if (EntityUtil.isMonster(m)) {
        EntityUtil.setGlowing(m, true);
    }
});
```

---

### `EntityUtil.setGlowing(entity, glowing)`

设置实体发光效果（显示轮廓）。

```js
EntityUtil.setGlowing(entity, true);   // 开启发光
EntityUtil.setGlowing(entity, false);  // 关闭发光
```

---

### `EntityUtil.setCustomName(entity, name)`

设置实体自定义名称（MiniMessage 格式）。

```js
EntityUtil.setCustomName(entity, "<red>Lv.50 Boss</red>");
EntityUtil.setCustomName(entity, "<gradient:gold:yellow>⭐ 传说级⭐</gradient>");
```

---

### `EntityUtil.getCustomName(entity)`

获取实体自定义名称（MiniMessage 格式，无名称返回 null）。

```js
var name = EntityUtil.getCustomName(entity);
if (name != null) {
    PlayerUtil.sendMessage(player, "实体名称: " + name);
}
```

---

### `EntityUtil.isMonster(entity)`

判断实体是否为敌对怪物（Monster 类型，包括僵尸、骷髅等）。

```js
if (EntityUtil.isMonster(entity)) {
    EntityUtil.damage(entity, 50.0);
}
```

---

### `EntityUtil.setPickupDelay(entity, ticks)`

设置掉落物拾取延迟（仅对 Item 掉落物实体有效）。

```js
EntityUtil.setPickupDelay(dropEntity, 100);  // 5 秒内不可拾取
EntityUtil.setPickupDelay(dropEntity, 0);    // 立即可以拾取
```

---

### `EntityUtil.setInvulnerable(entity, invulnerable)`

设置实体是否无敌。

```js
EntityUtil.setInvulnerable(entity, true);   // 无敌
EntityUtil.setInvulnerable(entity, false);  // 可被伤害
```

---

### `EntityUtil.setSilent(entity, silent)`

设置实体是否静音（禁用 AI 声音）。

```js
EntityUtil.setSilent(entity, true);  // 静音
```

---

### `EntityUtil.setFireTicks(entity, ticks)`

让实体着火（20 tick = 1 秒）。

```js
EntityUtil.setFireTicks(entity, 100);   // 着火 5 秒
EntityUtil.setFireTicks(entity, 0);     // 灭火
```

---

### `EntityUtil.getType(entity)`

获取实体类型名。

```js
var type = EntityUtil.getType(entity);  // "ZOMBIE", "SKELETON", "PLAYER" 等
```

---

### `EntityUtil.teleport(entity, x, y, z, worldName?)`

传送实体到坐标。不指定 worldName 则保持当前世界。

```js
EntityUtil.teleport(entity, 0, 64, 0);
EntityUtil.teleport(entity, 100, 64, 100, "world_nether");
```

---

### `EntityUtil.getUniqueId(entity)`

获取实体的 UUID 字符串。

```js
var uuid = EntityUtil.getUniqueId(entity);
```

---

### `EntityUtil.getHealth(entity)`

获取实体当前生命值。

```js
var hp = EntityUtil.getHealth(boss);
PlayerUtil.sendMessage(player, "Boss 血量: " + MathUtil.round(hp));
```

### `EntityUtil.getMaxHealth(entity)`

获取实体最大生命值。

```js
var maxHp = EntityUtil.getMaxHealth(entity);
```

### `EntityUtil.setMaxHealth(entity, health)`

设置实体最大生命值（保持当前血量比例）。

```js
EntityUtil.setMaxHealth(boss, 500.0);  // Boss 血量提升到 500
```

---

### `EntityUtil.getLocation(entity)`

获取实体位置（Location 对象）。

```js
var loc = EntityUtil.getLocation(entity);
```

### `EntityUtil.getWorldName(entity)`

获取实体所在世界名。

```js
var wn = EntityUtil.getWorldName(entity);
```

---

### `EntityUtil.isDead(entity)`

判断实体是否已死亡。

```js
if (EntityUtil.isDead(boss)) {
    instance.broadcast("<gold>Boss 已被击败！</gold>");
}
```

### `EntityUtil.getName(entity)`

获取实体名称（优先自定义名，其次类型名）。

```js
var name = EntityUtil.getName(entity);  // "Lv.50 Boss" 或 "ZOMBIE"
```

---

### `EntityUtil.setAI(entity, hasAI)`

设置实体是否启用 AI（false 使实体不动）。

```js
EntityUtil.setAI(entity, false);  // 冻结实体
EntityUtil.setAI(entity, true);   // 恢复 AI
```

### `EntityUtil.setGravity(entity, gravity)`

设置实体是否受重力影响。

```js
EntityUtil.setGravity(entity, false);  // 悬浮
```

---

### `EntityUtil.isGlowing(entity)`

检查实体是否发光。

```js
if (EntityUtil.isGlowing(entity)) {
    // 处于发光状态
}
```

---

### `EntityUtil.getPassengers(entity)`

获取实体的所有乘客列表。

```js
var passengers = EntityUtil.getPassengers(vehicle);
```

### `EntityUtil.addPassenger(vehicle, passenger)`

添加乘客（让 passenger 骑乘 vehicle）。

```js
EntityUtil.addPassenger(vehicle, passenger);
```

### `EntityUtil.removePassengers(entity)`

移除所有乘客。

```js
EntityUtil.removePassengers(entity);
```

---

## 常见用法（实用组合）

**范围 AOE 伤害**:
```js
var mobs = EntityUtil.getLivingEntitiesNear(worldName, x, y, z, 8);
mobs.forEach(function(m) {
    if (EntityUtil.isMonster(m)) {
        EntityUtil.damage(m, 20.0);
        EntityUtil.setFireTicks(m, 60);
    }
});
```

**标记附近怪物**:
```js
var mobs = EntityUtil.getLivingEntitiesNear(worldName, x, y, z, 20);
mobs.forEach(function(m) {
    if (EntityUtil.isMonster(m)) {
        EntityUtil.setGlowing(m, true);
        EntityUtil.setCustomName(m, "<red>⚔ 目标</red>");
    }
});
```

**Boss 召唤闪电**:
```js
var players = EntityUtil.getPlayersNear(worldName, x, y, z, 10);
players.forEach(function(p) {
    var loc = p.getLocation();
    loc.getWorld().strikeLightning(loc);
    EntityUtil.damage(p, 8.0);
});
```

**掉落物品保护（防止立即拾取）**:
```js
var drops = EntityUtil.getEntitiesNear(worldName, x, y, z, 5);
drops.forEach(function(d) {
    if (d instanceof org.bukkit.entity.Item) {
        EntityUtil.setPickupDelay(d, 40);  // 2 秒保护
    }
});
```

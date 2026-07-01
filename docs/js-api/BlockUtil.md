# BlockUtil 方块工具

在 JS 脚本中通过 `BlockUtil` 访问。

---

## 方法列表

### `BlockUtil.setBlock(worldName, x, y, z, material)`

设置方块。

```js
BlockUtil.setBlock("world", 100, 64, 100, "DIAMOND_BLOCK");
BlockUtil.setBlock("world", 101, 64, 100, "REDSTONE_BLOCK");
```

---

### `BlockUtil.getBlockType(worldName, x, y, z)`

获取方块类型名。世界不存在返回 `"UNKNOWN"`。

```js
var type = BlockUtil.getBlockType("world", 100, 64, 100);
if (type == "CHEST") {
    // 发现箱子
}
```

---

### `BlockUtil.isSolid(worldName, x, y, z)`

判断方块是否为实心。

```js
if (BlockUtil.isSolid("world", x, y, z)) {
    // 脚下是实心方块
}
```

---

### `BlockUtil.isAir(worldName, x, y, z)`

判断方块是否为空气。

```js
if (BlockUtil.isAir("world", x, y, z)) {
    BlockUtil.setBlock("world", x, y, z, "STONE");
}
```

---

### `BlockUtil.breakBlock(worldName, x, y, z)`

破坏方块（模拟自然破坏，掉落物品）。

```js
BlockUtil.breakBlock("world", 100, 64, 100);
```

---

### `BlockUtil.getBlockData(worldName, x, y, z)`

获取方块数据对象。

```js
var data = BlockUtil.getBlockData("world", x, y, z);
if (data != null) {
    // 操作 data
}
```

### `BlockUtil.setBlockData(worldName, x, y, z, data)`

设置方块数据（保留方块类型但改变状态）。

```js
var data = BlockUtil.getBlockData("world", x, y, z);
// 修改 data 属性
BlockUtil.setBlockData("world", x, y, z, data);
```

---

### `BlockUtil.getHardness(worldName, x, y, z)`

获取方块硬度（-1 为基岩类不可破坏）。
- 0 = 瞬间破坏（如花、草）
- ~0.6 = 木头
- ~1.5 = 石头
- ~3 = 铁块
- -1 = 基岩/命令方块（不可破坏）

```js
var hardness = BlockUtil.getHardness("world", x, y, z);
if (hardness < 0) {
    PlayerUtil.sendMessage(player, "<red>此方块无法破坏");
} else if (hardness > 2.0) {
    PlayerUtil.sendMessage(player, "<yellow>方块很硬");
}
```

---

### `BlockUtil.getLightLevel(worldName, x, y, z)`

获取方块光照等级（0 ~ 15）。

```js
var light = BlockUtil.getLightLevel("world", x, y, z);
if (light < 7) {
    // 足够暗，可以生成怪物
}
```

---

### `BlockUtil.isReplaceable(worldName, x, y, z)`

判断方块是否可被替代（如草、水、藤蔓）。

```js
if (BlockUtil.isReplaceable("world", x, y, z)) {
    BlockUtil.setBlock("world", x, y, z, "STONE");  // 直接替换
}
```

### `BlockUtil.isPassable(worldName, x, y, z)`

判断方块是否可通行（如门、活板门、台阶）。

```js
if (!BlockUtil.isPassable("world", x, y, z)) {
    PlayerUtil.sendMessage(player, "<red>前方不可通行");
}
```

### `BlockUtil.getFace(fromWorld, fromX, fromY, fromZ, toWorld, toX, toY, toZ)`

获取相邻方块的方向名。

```js
var face = BlockUtil.getFace("world", 0, 64, 0, "world", 1, 64, 0);
// 返回 "EAST" (如果两个方块在同一个世界且相邻)
```

---

### `BlockUtil.setBlocks(worldName, x1, y1, z1, x2, y2, z2, material)`

批量填充方块（填充长方体区域）。

```js
// 在 (90,60,90) 到 (110,70,110) 范围内填充石头
BlockUtil.setBlocks("world", 90, 60, 90, 110, 70, 110, "STONE");
```

---

### `BlockUtil.getBiome(worldName, x, y, z)`

获取生态群系（Biome）名称。

```js
var biome = BlockUtil.getBiome("world", 100, 64, 100);
// 可能返回 "PLAINS", "DESERT", "FOREST" 等
```

---

### `BlockUtil.isLiquid(worldName, x, y, z)`

判断方块是否为液体（水、岩浆等）。

```js
if (BlockUtil.isLiquid("world", x, y, z)) {
    PlayerUtil.sendMessage(player, "<red>脚下是液体！");
}
```

---

### `BlockUtil.getLocation(worldName, x, y, z)`

获取方块的 Location 对象（Bukkit Location），可用于生成实体等。

```js
var loc = BlockUtil.getLocation("world", 100, 64, 100);
if (loc != null) {
    player.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.ZOMBIE);
}
```

---

## 常见用法（实用组合）

**建造平台**:
```js
var cx = 100, cy = 64, cz = 100, size = 5;
for (var dx = -size; dx <= size; dx++) {
    for (var dz = -size; dz <= size; dz++) {
        BlockUtil.setBlock("world", cx + dx, cy, cz + dz, "STONE");
    }
}
```

**挖出房间**:
```js
var x1 = 90, x2 = 110, y1 = 60, y2 = 70, z1 = 90, z2 = 110;
for (var x = x1; x <= x2; x++) {
    for (var y = y1; y <= y2; y++) {
        for (var z = z1; z <= z2; z++) {
            BlockUtil.setBlock("world", x, y, z, "AIR");
        }
    }
}
```

**检测玩家脚下方块**:
```js
var loc = player.getLocation();
var below = BlockUtil.getBlockType("world", loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
if (below == "LAVA" || below == "WATER") {
    PlayerUtil.damage(player, 2.0);
}
```

**建造传送门框架**:
```js
var wx = 100, wy = 64, wz = 100;
for (var dy = 0; dy < 5; dy++) {
    BlockUtil.setBlock("world", wx, wy + dy, wz, "OBSIDIAN");
    BlockUtil.setBlock("world", wx + 3, wy + dy, wz, "OBSIDIAN");
}
for (var dx = 1; dx <= 2; dx++) {
    BlockUtil.setBlock("world", wx + dx, wy, wz, "OBSIDIAN");
    BlockUtil.setBlock("world", wx + dx, wy + 4, wz, "OBSIDIAN");
}
```

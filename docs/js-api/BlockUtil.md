# BlockUtil 方块工具

在 JS 脚本中通过 `BlockUtil` 访问。

---

## 方法列表

### `BlockUtil.setBlock(worldName, x, y, z, material)`

设置方块。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |
| `material` | `String` | 材料名（如 `"DIAMOND_BLOCK"`） |

**返回值**: 无

---

### `BlockUtil.getBlockType(worldName, x, y, z)`

获取方块类型名。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `String` — 材料名，如 `"STONE"`，世界不存在返回 `"UNKNOWN"`

---

### `BlockUtil.isSolid(worldName, x, y, z)`

判断方块是否为实心。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `Boolean`

---

### `BlockUtil.isAir(worldName, x, y, z)`

判断方块是否为空气。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `Boolean`

---

### `BlockUtil.breakBlock(worldName, x, y, z)`

破坏方块（模拟自然破坏，掉落物品）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: 无

---

### `BlockUtil.getBlockData(worldName, x, y, z)`

获取方块数据对象。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `BlockData?` — 方块数据对象

---

### `BlockUtil.setBlockData(worldName, x, y, z, data)`

设置方块数据。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |
| `data` | `BlockData` | 方块数据对象 |

**返回值**: 无

---

### `BlockUtil.getHardness(worldName, x, y, z)`

获取方块硬度（-1 为基岩类不可破坏）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `Float`

---

### `BlockUtil.getLightLevel(worldName, x, y, z)`

获取方块光照等级。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `Int` — 0 ~ 15

---

### `BlockUtil.isReplaceable(worldName, x, y, z)`

判断方块是否可被替代（如草、水）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `Boolean`

---

### `BlockUtil.isPassable(worldName, x, y, z)`

判断方块是否可通行（如门、活板门）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `worldName` | `String` | 世界名 |
| `x` | `Int` | X 坐标 |
| `y` | `Int` | Y 坐标 |
| `z` | `Int` | Z 坐标 |

**返回值**: `Boolean`

---

### `BlockUtil.getFace(fromWorld, fromX, fromY, fromZ, toWorld, toX, toY, toZ)`

获取相邻方块的方向名。

| 参数 | 类型 | 说明 |
|------|------|------|
| `fromWorld` | `String` | 源方块世界名 |
| `fromX` | `Int` | 源 X |
| `fromY` | `Int` | 源 Y |
| `fromZ` | `Int` | 源 Z |
| `toWorld` | `String` | 目标方块世界名 |
| `toX` | `Int` | 目标 X |
| `toY` | `Int` | 目标 Y |
| `toZ` | `Int` | 目标 Z |

**返回值**: `String?` — 方向名如 `"NORTH"`、`"UP"`，非同世界或不相邻返回 null

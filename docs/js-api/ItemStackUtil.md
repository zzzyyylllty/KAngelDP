# ItemStackUtil 物品工具

在 JS 脚本中通过 `ItemStackUtil` 访问。

---

## 方法列表

### `ItemStackUtil.getItemTag(itemStack)`

获取物品的 NBT 标签。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `ItemTag` — NBT 标签对象

---

### `ItemStackUtil.setItemTag(itemStack, tag)`

设置物品的 NBT 标签。**不会改变原本物品**，返回新的物品副本。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |
| `tag` | `ItemTag` | NBT 标签 |

**返回值**: `ItemStack` — 设置标签后的新物品

---

### `ItemStackUtil.setItemTagDirect(itemStack, tag)`

直接修改物品的 NBT 标签。**会改变原本物品**。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |
| `tag` | `ItemTag` | NBT 标签 |

**返回值**: `ItemStack` — 修改后的物品

---

### `ItemStackUtil.createItem(material, amount?, name?, lore?)`

创建物品（支持 MiniMessage 名称和 Lore）。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `material` | `String` | — | 材料名（如 `"DIAMOND_SWORD"`） |
| `amount` | `Int` | 1 | 数量 |
| `name` | `String`? | `null` | MiniMessage 显示名称 |
| `lore` | `List<String>`? | `null` | MiniMessage Lore 描述列表 |

**返回值**: `ItemStack?` — 创建失败（材料无效）返回 null

**示例**:
```js
var sword = ItemStackUtil.createItem("DIAMOND_SWORD", 1, "<gold>Excalibur</gold>", ["<gray>传说之剑</gray>"]);
```

---

### `ItemStackUtil.createItemStack(material, amount?)`

创建简易物品。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `material` | `String` | — | 材料名 |
| `amount` | `Int` | 1 | 数量 |

**返回值**: `ItemStack?`

---

### `ItemStackUtil.addItemToPlayer(player, itemStack)`

给予玩家物品（背包满则掉落）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `Boolean` — 始终返回 true

---

### `ItemStackUtil.removeItemFromPlayer(player, material, amount)`

从玩家背包移除指定材料。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `material` | `String` | 材料名 |
| `amount` | `Int` | 移除数量 |

**返回值**: `Boolean` — 数量不足时返回 false

---

### `ItemStackUtil.countItem(player, material)`

统计玩家背包中某材料的数量。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `material` | `String` | 材料名 |

**返回值**: `Int`

---

### `ItemStackUtil.hasItem(player, material, amount?)`

检查玩家是否有足够数量的物品。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `player` | `Player` | — | 目标玩家 |
| `material` | `String` | — | 材料名 |
| `amount` | `Int` | 1 | 需要数量 |

**返回值**: `Boolean`

---

### `ItemStackUtil.getItemInMainHand(player)`

获取玩家主手的物品。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |

**返回值**: `ItemStack?` — 空手时返回 null

---

### `ItemStackUtil.getItemInOffHand(player)`

获取玩家副手的物品。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |

**返回值**: `ItemStack?`

---

### `ItemStackUtil.setItemInMainHand(player, itemStack)`

设置玩家主手物品。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: 无

---

### `ItemStackUtil.setItemInOffHand(player, itemStack)`

设置玩家副手物品。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: 无

---

### `ItemStackUtil.setHelmet(player, itemStack)`

设置玩家头盔装备。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: 无

---

### `ItemStackUtil.setChestplate(player, itemStack)`

设置玩家胸甲装备。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: 无

---

### `ItemStackUtil.setLeggings(player, itemStack)`

设置玩家护腿装备。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: 无

---

### `ItemStackUtil.setBoots(player, itemStack)`

设置玩家靴子装备。

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `Player` | 目标玩家 |
| `itemStack` | `ItemStack` | 物品 |

**返回值**: 无

---

### `ItemStackUtil.getItemName(itemStack)`

获取物品的 MiniMessage 显示名称。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `String` — 无自定义名称时返回材料名

---

### `ItemStackUtil.setItemName(itemStack, name)`

设置物品显示名称（返回新物品，不修改原物品）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |
| `name` | `String` | MiniMessage 名称 |

**返回值**: `ItemStack`

---

### `ItemStackUtil.getItemType(itemStack)`

获取物品的材料名。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `String` — 如 `"DIAMOND_SWORD"`

---

### `ItemStackUtil.getItemAmount(itemStack)`

获取物品数量。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `Int`

---

### `ItemStackUtil.isAir(itemStack)`

判断物品是否为空。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `Boolean`

---

### `ItemStackUtil.isSimilar(item1, item2)`

判断两个物品是否相似（类型和 meta 相同）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `item1` | `ItemStack` | 物品1 |
| `item2` | `ItemStack` | 物品2 |

**返回值**: `Boolean`

---

### `ItemStackUtil.cloneItem(itemStack)`

克隆物品。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `ItemStack`

---

### `ItemStackUtil.setLore(itemStack, lore)`

设置物品 Lore（返回新物品）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |
| `lore` | `List<String>` | MiniMessage 描述列表 |

**返回值**: `ItemStack`

---

### `ItemStackUtil.getLore(itemStack)`

获取物品的 MiniMessage Lore。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |

**返回值**: `List<String>?` — 无 lore 时返回 null

---

### `ItemStackUtil.setUnbreakable(itemStack, unbreakable)`

设置物品是否不可破坏（返回新物品）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `itemStack` | `ItemStack` | 物品 |
| `unbreakable` | `Boolean` | 是否不可破坏 |

**返回值**: `ItemStack`

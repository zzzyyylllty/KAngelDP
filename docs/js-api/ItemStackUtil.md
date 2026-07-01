# ItemStackUtil 物品工具

在 JS 脚本中通过 `ItemStackUtil` 访问。

---

## NBT 操作

### `ItemStackUtil.getItemTag(itemStack)`

获取物品的 NBT 标签。

```js
var tag = ItemStackUtil.getItemTag(itemStack);
```

---

### `ItemStackUtil.setItemTag(itemStack, tag)`

设置物品的 NBT 标签。**不会改变原本物品**，返回新的物品副本。

```js
var tag = ItemStackUtil.getItemTag(itemStack);
tag.put("dungeonId", "my_dungeon");
var newItem = ItemStackUtil.setItemTag(itemStack, tag);
```

---

### `ItemStackUtil.setItemTagDirect(itemStack, tag)`

直接修改物品的 NBT 标签。**会改变原本物品**。

```js
var tag = ItemStackUtil.getItemTag(itemStack);
tag.put("level", 5);
ItemStackUtil.setItemTagDirect(itemStack, tag);
```

---

## 创建物品

### `ItemStackUtil.createItem(material, amount?, name?, lore?)`

创建物品（支持 MiniMessage 名称和 Lore）。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `material` | `String` | — | 材料名（如 `"DIAMOND_SWORD"`） |
| `amount` | `Int` | 1 | 数量 |
| `name` | `String`? | `null` | MiniMessage 显示名称 |
| `lore` | `List<String>`? | `null` | MiniMessage 描述列表 |

**返回值**: `ItemStack?` — 材料无效返回 null

```js
// 基础物品
var diamond = ItemStackUtil.createItem("DIAMOND", 5);

// 自定义名称和 lore
var sword = ItemStackUtil.createItem("DIAMOND_SWORD", 1,
    "<gold>Excalibur</gold>",
    ["<gray>传说之剑</gray>", "<yellow>⚔ 攻击力 +10</yellow>"]
);

// 给玩家
PlayerUtil.giveItem(player, sword);
```

---

### `ItemStackUtil.createItemStack(material, amount?)`

创建简易物品（无 meta）。

```js
var stack = ItemStackUtil.createItemStack("STONE", 64);
```

---

## 物品操作

### `ItemStackUtil.addItemToPlayer(player, itemStack)`

给予玩家物品（背包满则掉落）。

```js
var item = ItemStackUtil.createItem("GOLDEN_APPLE", 16);
ItemStackUtil.addItemToPlayer(player, item);
```

---

### `ItemStackUtil.removeItemFromPlayer(player, material, amount)`

从玩家背包移除指定材料。

```js
if (ItemStackUtil.removeItemFromPlayer(player, "DIAMOND", 5)) {
    PlayerUtil.sendMessage(player, "<green>扣除了 5 个钻石");
} else {
    PlayerUtil.sendMessage(player, "<red>钻石不足！");
}
```

---

### `ItemStackUtil.countItem(player, material)`

统计玩家背包中某材料的数量。

```js
var logs = ItemStackUtil.countItem(player, "OAK_LOG");
PlayerUtil.sendMessage(player, "你有 " + logs + " 个橡木原木");
```

---

### `ItemStackUtil.hasItem(player, material, amount?)`

检查玩家是否有足够数量的物品。

```js
if (ItemStackUtil.hasItem(player, "EMERALD", 3)) {
    ItemStackUtil.removeItemFromPlayer(player, "EMERALD", 3);
    PlayerUtil.giveItemStack(player, "DIAMOND", 1);
    PlayerUtil.sendMessage(player, "<gold>兑换成功！");
}
```

---

## 手持物品

### `ItemStackUtil.getItemInMainHand(player)`

获取玩家主手的物品。空手返回 null。

```js
var held = ItemStackUtil.getItemInMainHand(player);
if (held != null) {
    var type = ItemStackUtil.getItemType(held);
    PlayerUtil.sendMessage(player, "你拿着: " + type);
}
```

### `ItemStackUtil.getItemInOffHand(player)`

获取玩家副手的物品。

```js
var offHand = ItemStackUtil.getItemInOffHand(player);
```

### `ItemStackUtil.setItemInMainHand(player, itemStack)`

设置玩家主手物品。

```js
var sword = ItemStackUtil.createItem("DIAMOND_SWORD", 1, "<gold>神剑</gold>");
ItemStackUtil.setItemInMainHand(player, sword);
```

### `ItemStackUtil.setItemInOffHand(player, itemStack)`

设置玩家副手物品。

```js
var torch = ItemStackUtil.createItem("TORCH", 1);
ItemStackUtil.setItemInOffHand(player, torch);
```

---

## 装备

### `ItemStackUtil.setHelmet(player, itemStack)`

```js
var helm = ItemStackUtil.createItem("DIAMOND_HELMET", 1, "<aqua>水龙头盔</aqua>");
ItemStackUtil.setHelmet(player, helm);
```

### `ItemStackUtil.setChestplate(player, itemStack)`

```js
var chest = ItemStackUtil.createItem("DIAMOND_CHESTPLATE", 1, "<aqua>水龙胸甲</aqua>");
ItemStackUtil.setChestplate(player, chest);
```

### `ItemStackUtil.setLeggings(player, itemStack)`

```js
var legs = ItemStackUtil.createItem("DIAMOND_LEGGINGS", 1, "<aqua>水龙护腿</aqua>");
ItemStackUtil.setLeggings(player, legs);
```

### `ItemStackUtil.setBoots(player, itemStack)`

```js
var boots = ItemStackUtil.createItem("DIAMOND_BOOTS", 1, "<aqua>水龙靴</aqua>");
ItemStackUtil.setBoots(player, boots);
```

**一键全套装备**:
```js
function equipFull(player, material, nameColor) {
    ItemStackUtil.setHelmet(player, ItemStackUtil.createItem(material + "_HELMET", 1, nameColor + material + "头盔"));
    ItemStackUtil.setChestplate(player, ItemStackUtil.createItem(material + "_CHESTPLATE", 1, nameColor + material + "胸甲"));
    ItemStackUtil.setLeggings(player, ItemStackUtil.createItem(material + "_LEGGINGS", 1, nameColor + material + "护腿"));
    ItemStackUtil.setBoots(player, ItemStackUtil.createItem(material + "_BOOTS", 1, nameColor + material + "靴子"));
}
equipFull(player, "DIAMOND", "<aqua>");
```

---

## 查询

### `ItemStackUtil.getItemName(itemStack)`

获取物品的 MiniMessage 显示名称（无自定义名称时返回材料名）。

```js
var name = ItemStackUtil.getItemName(itemStack);
```

### `ItemStackUtil.setItemName(itemStack, name)`

设置物品显示名称（返回新物品，不修改原物品）。

```js
var renamed = ItemStackUtil.setItemName(itemStack, "<red>改名</red>");
```

### `ItemStackUtil.getItemType(itemStack)`

获取物品的材料名。

```js
var type = ItemStackUtil.getItemType(itemStack);
PlayerUtil.sendMessage(player, "材料: " + type);
```

### `ItemStackUtil.getItemAmount(itemStack)`

```js
var amount = ItemStackUtil.getItemAmount(itemStack);
```

### `ItemStackUtil.isAir(itemStack)`

```js
if (ItemStackUtil.isAir(itemStack)) {
    // 空手或空气
}
```

### `ItemStackUtil.isSimilar(item1, item2)`

判断两个物品是否相似（类型和 meta 相同）。

```js
if (ItemStackUtil.isSimilar(item1, item2)) {
    PlayerUtil.sendMessage(player, "物品相同");
}
```

### `ItemStackUtil.cloneItem(itemStack)`

```js
var cloned = ItemStackUtil.cloneItem(itemStack);
```

---

## Lore 操作

### `ItemStackUtil.setLore(itemStack, lore)`

设置物品 Lore（返回新物品）。

```js
var withLore = ItemStackUtil.setLore(itemStack, [
    "<gray>第一行描述</gray>",
    "<yellow>⚡ 特殊属性</yellow>"
]);
```

### `ItemStackUtil.getLore(itemStack)`

获取物品的 MiniMessage Lore（无 lore 返回 null）。

```js
var lore = ItemStackUtil.getLore(itemStack);
if (lore != null) {
    lore.forEach(function(line) { player.sendMessage(line); });
}
```

---

### `ItemStackUtil.setUnbreakable(itemStack, unbreakable)`

设置物品是否不可破坏（返回新物品）。

```js
var unbreaking = ItemStackUtil.setUnbreakable(itemStack, true);
```

---

### `ItemStackUtil.setAmount(itemStack, amount)`

设置物品数量（返回新物品，不修改原物品）。

```js
var stack = ItemStackUtil.createItemStack("DIAMOND", 1);
var stack64 = ItemStackUtil.setAmount(stack, 64);
```

---

### `ItemStackUtil.addEnchantment(itemStack, enchantment, level)`

给物品添加附魔（返回新物品）。附魔不存在时返回原物品。

```js
var sword = ItemStackUtil.createItemStack("DIAMOND_SWORD", 1);
var enchanted = ItemStackUtil.addEnchantment(sword, "sharpness", 5);
```

### `ItemStackUtil.removeEnchantment(itemStack, enchantment)`

移除物品的指定附魔（返回新物品）。

```js
var cleaned = ItemStackUtil.removeEnchantment(enchanted, "sharpness");
```

### `ItemStackUtil.hasEnchantment(itemStack, enchantment)`

判断物品是否有指定附魔。

```js
if (ItemStackUtil.hasEnchantment(itemStack, "sharpness")) {
    PlayerUtil.sendMessage(player, "<green>此剑有锋利附魔");
}
```

### `ItemStackUtil.getEnchantments(itemStack)`

获取物品的所有附魔（返回 Map<附魔名, 等级>）。

```js
var enchants = ItemStackUtil.getEnchantments(itemStack);
// 返回如 {sharpness: 5, unbreaking: 3}
```

---

### `ItemStackUtil.addLore(itemStack, line)`

在物品 Lore 末尾添加一行（返回新物品，MiniMessage 格式）。

```js
var item = ItemStackUtil.addLore(itemStack, "<yellow>✨ 稀有物品</yellow>");
```

### `ItemStackUtil.removeLore(itemStack, index)`

移除指定索引的 Lore 行（返回新物品）。

```js
var item = ItemStackUtil.removeLore(itemStack, 0);  // 移除第一行
```

### `ItemStackUtil.clearLore(itemStack)`

清空物品的所有 Lore（返回新物品）。

```js
var item = ItemStackUtil.clearLore(itemStack);
```

---

## 常见用法（实用组合）

**创建完整的传说武器**:
```js
var weapon = ItemStackUtil.createItem("NETHERITE_SWORD", 1,
    "<gradient:red:gold>🔥 烈焰之刃</gradient>",
    [
        "<gray>远古锻造师的杰作</gray>",
        "",
        "<red>⚔ 攻击伤害 +15</red>",
        "<gold>✨ 火焰附加 II</gold>",
        "",
        "<dark_gray><italic>\"烈焰永不熄灭\"</italic></dark_gray>"
    ]
);
weapon = ItemStackUtil.setUnbreakable(weapon, true);
var tag = ItemStackUtil.getItemTag(weapon);
tag.put("weapon_level", 5);
weapon = ItemStackUtil.setItemTag(weapon, tag);
ItemStackUtil.setItemInMainHand(player, weapon);
```

**检查并消耗材料**:
```js
var cost = 32;
if (ItemStackUtil.hasItem(player, "IRON_INGOT", cost)) {
    ItemStackUtil.removeItemFromPlayer(player, "IRON_INGOT", cost);
    var reward = ItemStackUtil.createItem("DIAMOND", 1);
    ItemStackUtil.addItemToPlayer(player, reward);
    PlayerUtil.sendMessage(player, "<green>锻造成功！消耗了 " + cost + " 个铁锭");
} else {
    PlayerUtil.sendMessage(player, "<red>需要 " + cost + " 个铁锭");
}
```

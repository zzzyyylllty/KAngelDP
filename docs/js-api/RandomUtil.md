# RandomUtil 随机工具

在 JS 脚本中通过 `RandomUtil` 访问。

---

## 方法列表

### `RandomUtil.nextInt(bound)`

生成 `[0, bound)` 的随机整数（不含 bound）。

```js
RandomUtil.nextInt(10);      // 0 ~ 9
RandomUtil.nextInt(100);     // 0 ~ 99
RandomUtil.nextInt(1);       // 始终返回 0
```

---

### `RandomUtil.nextIntRange(min, max)`

生成 `[min, max]` 的随机整数（包含两端）。

```js
RandomUtil.nextIntRange(5, 10);  // 可能返回 5, 6, 7, 8, 9, 10
RandomUtil.nextIntRange(1, 6);   // 模拟骰子
```

---

### `RandomUtil.nextDouble()`

生成 `[0.0, 1.0)` 的随机小数。

```js
var r = RandomUtil.nextDouble();  // 0.0 ~ 0.999...
```

---

### `RandomUtil.nextDoubleRange(min, max)`

生成 `[min, max)` 的随机小数。

```js
RandomUtil.nextDoubleRange(1.5, 5.5);  // 1.5 ~ 5.499...
```

---

### `RandomUtil.nextBoolean()`

随机布尔值。

```js
if (RandomUtil.nextBoolean()) {
    // 50% 概率
}
```

---

### `RandomUtil.pick(list)`

从列表中随机选一个元素。空列表返回 null。

```js
var mobs = ["ZOMBIE", "SKELETON", "SPIDER"];
var chosen = RandomUtil.pick(mobs);  // 随机一个

var messages = ["<red>轰！", "<yellow>砰！", "<gold>铛！"];
PlayerUtil.sendMessage(player, RandomUtil.pick(messages));
```

---

### `RandomUtil.shuffle(list)`

随机打乱列表（返回新列表，不修改原列表）。

```js
var cards = ["A", "B", "C", "D", "E"];
var shuffled = RandomUtil.shuffle(cards);
// shuffled 顺序随机，原列表不变
```

---

### `RandomUtil.sample(list, count)`

从列表中随机选 N 个不重复元素。

```js
var pool = ["奖励A", "奖励B", "奖励C", "奖励D", "奖励E"];
var selected = RandomUtil.sample(pool, 3);  // 随机选 3 个不重复
```

---

### `RandomUtil.weightedPick(weightMap)`

加权随机选择：从 Map<元素, 权重> 中按权重随机选一个。权重越高被选中的概率越大。

```js
var result = RandomUtil.weightedPick({diamond: 10, stone: 50, dirt: 100});
// dirt 约 62.5%   (100/160)
// stone 约 31.25%  (50/160)
// diamond 约 6.25% (10/160)
```

---

### `RandomUtil.weightedPickList(weightMap, count)`

从加权 Map 中随机选 N 个不重复元素。（无放回抽样）

```js
var rewards = RandomUtil.weightedPickList(
    {diamond: 10, iron: 30, stone: 60},
    2
);
// 可能返回 ["stone", "iron"]，不含重复
```

---

### `RandomUtil.nextGaussian(mean, stdDev)`

生成高斯分布（正态分布）随机数。

```js
// 平均值为 0，标准差为 1 的正态分布
var g = RandomUtil.nextGaussian(0, 1);  // -3.0 ~ 3.0 之间，接近 0 更常见

// 用于生成更自然的随机分布
var naturalHP = 100 + RandomUtil.nextGaussian(0, 10);  // 约 80 ~ 120
```

---

## 常见用法（实用组合）

**随机掉落**:
```js
var loot = RandomUtil.weightedPick({
    "minecraft:diamond": 5,
    "minecraft:iron_ingot": 30,
    "minecraft:gold_ingot": 20,
    "minecraft:stone": 45
});
var world = player.getWorld();
var loc = player.getLocation();
world.dropItemNaturally(loc, ItemStackUtil.createItemStack(loot, RandomUtil.nextIntRange(1, 3)));
```

**随机点数生成**:
```js
var damage = RandomUtil.nextIntRange(8, 15) + RandomUtil.nextDouble();
EntityUtil.damage(target, damage);
```

**随机小怪群**:
```js
var mobTypes = ["ZOMBIE", "SKELETON", "SPIDER", "CREEPER"];
var count = RandomUtil.nextIntRange(3, 8);
for (var i = 0; i < count; i++) {
    var type = RandomUtil.pick(mobTypes);
    var loc = player.getLocation().clone().add(RandomUtil.nextIntRange(-5, 5), 0, RandomUtil.nextIntRange(-5, 5));
    player.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.valueOf(type));
}
```

**加权奖池系统**:
```js
function rollLootTable(player) {
    var rarity = RandomUtil.weightedPick({
        "common": 600,
        "uncommon": 300,
        "rare": 80,
        "epic": 18,
        "legendary": 2
    });
    
    switch (rarity) {
        case "legendary":
            PlayerUtil.giveItemStack(player, "NETHERITE_INGOT", 1);
            PlayerUtil.sendMessage(player, "<gold>✨ 传说级！下界合金锭！");
            break;
        case "epic":
            PlayerUtil.giveItemStack(player, "DIAMOND", RandomUtil.nextIntRange(3, 8));
            break;
        default:
            PlayerUtil.giveItemStack(player, "IRON_INGOT", RandomUtil.nextIntRange(1, 5));
    }
}
```

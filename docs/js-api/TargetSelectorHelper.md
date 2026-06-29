# TargetSelectorHelper 目标选择器

在 JS 脚本中通过 `TargetSelectorHelper` 访问。

---

## 方法列表

### `TargetSelectorHelper.parseLine(instance, line)`

解析并求值目标选择器字符串。

| 参数 | 类型 | 说明 |
|------|------|------|
| `instance` | `DungeonInstance` | 当前地牢实例 |
| `line` | `String` | 选择器字符串，如 `"@all{health>10}"` |

**返回值**: `List<Player>` — 匹配的玩家列表

---

## 选择器语法

基础格式: `@selector{条件}`

### 选择器类型

| 类型 | 说明 |
|------|------|
| `@all` | 所有在线玩家 |
| `@p` | 最近/第一个匹配的玩家 |
| `@r` | 随机匹配的玩家 |
| `@alive` | 存活的玩家 |
| `@dead` | 死亡的玩家 |

### 条件语法

条件支持布尔表达式连接：

```
@all{health>10}
@all{level>=5&&name=PlayerName}
@all{papi:player_health>=30&&distance:1,2,3=1..6}
```

| 运算符 | 说明 |
|--------|------|
| `>` | 大于 |
| `<` | 小于 |
| `>=` | 大于等于 |
| `<=` | 小于等于 |
| `=` / `==` | 等于 |
| `&&` | 与 |
| `\|\|` | 或 |

### 变量类型

| 变量 | 说明 |
|------|------|
| `health` | 生命值 |
| `level` | 等级 |
| `name` | 玩家名称 |
| `papi:...` | PlaceholderAPI 变量 |
| `distance:x,y,z=min..max` | 距离范围 |

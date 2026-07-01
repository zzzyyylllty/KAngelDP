# Bukkit (ScriptBukkit)

在 JS 脚本中通过 `Bukkit` 访问。提供安全包装的 Bukkit 功能。

---

## 方法列表

### `Bukkit.broadcast(component)`

广播消息给所有在线玩家。

| 参数 | 类型 | 说明 |
|------|------|------|
| `component` | `Component` 或 `Any` | Adventure Component 或任意对象（toString 后广播） |

**简单字符串广播**:
```js
Bukkit.broadcast("<gold>📢 全体公告</gold>");
Bukkit.broadcast("<red>⚡ 服务器即将重启</red>");
```

**使用 MiniMessage 工具**:
```js
var component = mmUtil.deserialize("<gradient:gold:yellow>重要通知</gradient>");
Bukkit.broadcast(component);
```

**广播给所有玩家 + 控制台日志**:
```js
Bukkit.broadcast("<green>地牢活动已开始！使用 /dg join 参加</green>");
Sys.println("[公告] 地牢活动广播已发送");
```

**实际用途示例 — BOSS 死亡全服通知**:
```js
var bossName = EntityUtil.getCustomName(boss) || "未知 Boss";
Bukkit.broadcast("<red>⚔ " + bossName + " 已被击败！</red>");
Bukkit.broadcast("<gold>🎉 击败者: " + player.getName() + "</gold>");
```

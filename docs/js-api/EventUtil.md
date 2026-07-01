# EventUtil 事件工具

在 JS 脚本中通过 `EventUtil` 访问。

---

## 方法列表

### `EventUtil.cancel(event, cancel?)`

取消/恢复一个可取消的事件。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `event` | `Cancellable` | — | 可取消的事件对象 |
| `cancel` | `Boolean` | true | 是否取消 |

```js
// 取消事件
EventUtil.cancel(event);
EventUtil.cancel(event, true);   // 同上

// 恢复事件
EventUtil.cancel(event, false);
```

---

### `EventUtil.call(event)`

手动触发一个 Bukkit 事件。

| 参数 | 类型 | 说明 |
|------|------|------|
| `event` | `Event` | 要触发的事件对象 |

```js
// 触发玩家伤害事件
var damageEvent = new org.bukkit.event.entity.EntityDamageEvent(
    player,
    org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM,
    10.0
);
EventUtil.call(damageEvent);
```

---

## 常见用法

**阻止玩家破坏方块**:
```js
// 在 BLOCK_BREAK 任务或 onPlayerInteract 等上下文中
if (event instanceof org.bukkit.event.block.BlockBreakEvent) {
    var block = event.getBlock();
    if (block.getType() == org.bukkit.Material.CHEST) {
        EventUtil.cancel(event);
        PlayerUtil.sendMessage(player, "<red>你不能破坏箱子！");
    }
}
```

**手动触发自定义事件**:
```js
var evt = new org.bukkit.event.player.PlayerCommandPreprocessEvent(player, "/say hello");
EventUtil.call(evt);
if (!evt.isCancelled()) {
    // 命令未被取消
}
```

# EventUtil 事件工具

在 JS 脚本中通过 `EventUtil` 访问。

---

## 方法列表

### `EventUtil.cancel(event, cancel?)`

取消/恢复一个可取消的事件。

| 参数 | 类型 | 说明 |
|------|------|------|
| `event` | `Cancellable` | 可取消的事件对象 |
| `cancel` | `Boolean` | 是否取消，默认 true |

**返回值**: 无

---

### `EventUtil.call(event)`

手动触发一个 Bukkit 事件。

| 参数 | 类型 | 说明 |
|------|------|------|
| `event` | `Event` | 要触发的事件对象 |

**返回值**: 无

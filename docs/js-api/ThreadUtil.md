# ThreadUtil 线程工具

在 JS 脚本中通过 `ThreadUtil` 访问。

**注意**: 不得在主服务器线程上调用，否则会抛出 `IllegalStateException`。

---

## 方法列表

### `ThreadUtil.sleep(time)`

使当前线程休眠指定毫秒。

| 参数 | 类型 | 说明 |
|------|------|------|
| `time` | `Long` | 休眠毫秒数 |

**返回值**: 无

**抛出**: `IllegalStateException` — 在主线程调用时

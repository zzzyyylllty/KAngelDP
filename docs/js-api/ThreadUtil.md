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

**抛出**: `IllegalStateException` — 在主线程调用时

```js
// 仅限异步上下文！
ThreadUtil.sleep(1000);  // 等待 1 秒
ThreadUtil.sleep(5000);  // 等待 5 秒
```

---

## 对比 `Sys.sleep()` 和 `ThreadUtil.sleep()`

两者功能相同，区别在于抛出的异常信息不同。都只能在异步线程中使用。

**异步 Plan 中的使用示例**:
```js
// 放在 async: true 的 Plan 的 onRun 中
Sys.println("开始处理...");
ThreadUtil.sleep(3000);
Sys.println("3 秒后继续...");
```

---

## 注意事项

- 不要在 `onSpawn`、`onEnter`、`onClick` 等同步钩子中调用
- 仅在 `async: true` 的 Plan 的 onRun 中使用
- 长时间 sleep（> 5 秒）可能被服务器 watchdog 警告

# Sys (SafeSystemUtil)

在 JS 脚本中通过 `Sys` 访问。提供 Java `System` 功能的安全包装，解决 GraalJS 安全模式下无法访问 `java.lang.System` 静态方法的问题。

---

## 方法列表

### `Sys.currentTimeMillis()`

获取当前毫秒时间戳。

**返回值**: `Long` — 当前时间毫秒数

---

### `Sys.nanoTime()`

获取纳秒时间戳。

**返回值**: `Long` — 纳秒时间戳

---

### `Sys.println(message)`

输出到控制台（stdout）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `message` | `Any?` | 要输出的信息 |

**返回值**: 无

---

### `Sys.printerr(message)`

输出到控制台（stderr）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `message` | `Any?` | 要输出的错误信息 |

**返回值**: 无

---

### `Sys.getProperty(key)`

读取系统属性。

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | `String` | 属性键（如 `"os.name"`） |

**返回值**: `String?` — 属性值

---

### `Sys.getProperty(key, default)`

读取系统属性（带默认值）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | `String` | 属性键 |
| `default` | `String` | 默认值 |

**返回值**: `String`

---

### `Sys.sleep(millis)`

睡眠指定毫秒。**不得在主线程调用**。

| 参数 | 类型 | 说明 |
|------|------|------|
| `millis` | `Long` | 睡眠毫秒数 |

**返回值**: 无

**抛出**: `IllegalStateException` — 在主线程调用时

---

### `Sys.newDate()`

创建当前时间的 `java.util.Date` 对象。

**返回值**: `java.util.Date`

---

### `Sys.newDate(millis)`

从毫秒时间戳创建 `java.util.Date` 对象。

| 参数 | 类型 | 说明 |
|------|------|------|
| `millis` | `Long` | 时间戳毫秒数 |

**返回值**: `java.util.Date`

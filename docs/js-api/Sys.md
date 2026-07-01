# Sys (SafeSystemUtil)

在 JS 脚本中通过 `Sys` 访问。提供 Java `System` 功能的安全包装，解决 GraalJS 安全模式下无法访问 `java.lang.System` 静态方法的问题。

---

## 方法列表

### `Sys.currentTimeMillis()`

获取当前毫秒时间戳。

```js
var now = Sys.currentTimeMillis();
```

常用于测量耗时:
```js
var start = Sys.currentTimeMillis();
// ... 执行操作 ...
var elapsed = Sys.currentTimeMillis() - start;
Sys.println("操作耗时: " + elapsed + "ms");
```

### `Sys.nanoTime()`

获取纳秒时间戳（用于高精度测量，不宜用于显示时间）。

```js
var start = Sys.nanoTime();
// ... 短操作 ...
var elapsed = Sys.nanoTime() - start;
Sys.println("耗时: " + elapsed + "ns");
```

---

### `Sys.println(message)`

输出到控制台（stdout）。

```js
Sys.println("JS 脚本执行成功");
Sys.println("当前玩家: " + player.getName());
```

---

### `Sys.printerr(message)`

输出到控制台（stderr）。

```js
try {
    // 可能出错的操作
} catch (e) {
    Sys.printerr("错误: " + e.message);
}
```

---

### `Sys.getProperty(key)`

读取系统属性。

```js
var os = Sys.getProperty("os.name");
Sys.println("服务器系统: " + os);
```

### `Sys.getProperty(key, default)`

```js
var javaVer = Sys.getProperty("java.version", "unknown");
```

---

### `Sys.sleep(millis)`

睡眠指定毫秒。**不得在主线程调用**（仅在异步脚本中使用）。

```js
// 仅限异步上下文！
Sys.sleep(2000);  // 等待 2 秒
Sys.println("2 秒后执行");
```

---

### `Sys.newDate()`

创建当前时间的 `java.util.Date` 对象。

```js
var now = Sys.newDate();
Sys.println("当前时间: " + now.toString());
```

### `Sys.newDate(millis)`

从毫秒时间戳创建 Date。

```js
var date = Sys.newDate(Sys.currentTimeMillis());
```

---

## 常见用法

**性能测量**:
```js
var t1 = Sys.nanoTime();
// ... 大量运算 ...
var t2 = Sys.nanoTime();
Sys.println("耗时: " + ((t2 - t1) / 1000000) + "ms");
```

**调试日志**:
```js
Sys.println("[DEBUG] 玩家 " + player.getName() + " 触发了 onSpawn");
Sys.println("[DEBUG] 位置: " + player.getLocation().toString());
```

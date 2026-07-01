# Kether 动作参考

KAngelDungeon 注册了 `kangeldp`（别名 `kdp`）前缀的 Kether 脚本动作。

所有动作的上下文玩家是脚本的 `sender`（脚本执行者）。

**不推荐使用**

---

## 查询

### inst — 获取地牢实例

```
kangeldp inst
```

返回玩家所在的 `DungeonInstance` 对象。不在任何地牢返回 `null`。

---

### inside — 检查是否在地牢中

```
kangeldp inside
kangeldp inside named <地牢模板名>
```

- 无参数：返回 `Boolean`，玩家是否在任何地牢中
- `named <模板名>`：返回 `Boolean`，玩家是否在指定模板的地牢中

---

### state — 获取地牢状态

```
kangeldp state
```

返回 `String`：`"PREPARING"` / `"ACTIVE"` / `"COMPLETED"` / `"FAILED"` / `"NONE"`

---

### template — 获取地牢模板名

```
kangeldp template
```

返回 `String`：地牢模板名，不在任何地牢返回 `"NONE"`

---

### uuid — 获取地牢 UUID

```
kangeldp uuid
```

返回 `String`：地牢实例 UUID，不在任何地牢返回 `"NONE"`

---

## 流程控制

### complete — 通关地牢

```
kangeldp complete
```

返回 `Boolean`：是否成功触发通关流程

---

### fail — 失败地牢

```
kangeldp fail
```

返回 `Boolean`：是否成功触发失败流程

---

### eval — 执行 JS

```
kangeldp eval <JS代码>
```

在地牢实例上下文中执行 JS 代码。`<JS代码>` 支持 Kether 子表达式。

示例：
```
kangeldp eval "instance.setMeta('wave', 3); instance.sendMessageToAllPlayers('新一波！');"
```

---

### script — 执行命名脚本

```
kangeldp script <脚本名>
```

执行 `dungeon/<name>/script/` 中的命名脚本（先 onRun 再 onPost）。

---

## 元数据

### meta get — 获取元数据

```
kangeldp meta get <key>
```

返回元数据值，不存在返回 `nil`。

---

### meta set — 设置元数据

```
kangeldp meta set <key> <value>
```

---

### meta add — 增加元数据

```
kangeldp meta add <key> <value>
```

数值累加。`key` 存在且为数字时进行数学加法。

---

## 障碍物

### obstacle open — 打开障碍物

```
kangeldp obstacle open <障碍物ID>
```

返回 `Boolean`：是否成功

---

### obstacle openforce — 强制打开障碍物

```
kangeldp obstacle openforce <障碍物ID>
```

跳过条件检查直接打开。

---

### obstacle prepare — 准备障碍物

```
kangeldp obstacle prepare <障碍物ID>
```

保存方块状态，准备激活。

---

### obstacle activate — 激活障碍物

```
kangeldp obstacle activate <障碍物ID>
```

开始障碍物动画。

---

## 怪物

### monster spawn — 生成怪物组

```
kangeldp monster spawn <怪物组ID>
```

返回 `Boolean`：是否成功生成

---

### monster clear — 清除所有怪物

```
kangeldp monster clear
```

清除地牢世界中的所有敌对生物。

---

### monster range — 设置激活距离

```
kangeldp monster range <怪物组ID> <最小距离> <最大距离>
```

运行时覆盖怪物组的激活距离。不影响配置文件。

---

### monster range_reset — 重置激活距离

```
kangeldp monster range_reset <怪物组ID>
```

恢复为配置文件中的默认激活距离。

---

## 消息

### tellall — 发送消息

```
kangeldp tellall <消息>
```

向地牢所有玩家发送 MiniMessage 消息。

---

### titleall — 发送标题

```
kangeldp titleall <标题>
kangeldp titleall <标题> <副标题>
```

向地牢所有玩家发送标题。

---

### actionbarall — 发送动作栏

```
kangeldp actionbarall <消息>
```

向地牢所有玩家发送 ActionBar 消息。

---

## 计划

### plan start — 启动计划

```
kangeldp plan start <触发器>
```

启动指定触发器的所有计划。触发器值：`PREPARE` / `BEGIN` / `END` / `FAIL`

---

### plan stop — 停止计划

```
kangeldp plan stop
```

停止地牢实例中所有活跃的计划。

---

## 集成示例

### Chemdah 任务中的使用

```yaml
# 检查玩家是否在特定地牢中
condition: "kangeldp inside named 'test_dungeon'"

# 检查地牢状态
condition2: "check kangeldp state == 'ACTIVE'"

# 完成任务后通关地牢
action: "kangeldp complete"
```

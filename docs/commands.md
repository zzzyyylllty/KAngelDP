# 命令参考

## 命令树总览

```
/kangeldungeon (dg, dungeon)
├── about
├── status
├── help
├── reload
├── dungeon  ── 地牢管理
├── admin    ── 管理员工具
├── debug    ── 调试命令
├── data     ── 玩家数据
├── api      ── 脚本测试
└── party    ── 队伍系统
```

每个子命令组都有独立别名，可单独使用。

---

## 主命令 `/kangeldungeon`

权限：`kangeldungeon.command.main`（默认 OP）
别名：`/dg`、`/dungeon`

| 子命令 | 参数 | 说明 |
|--------|------|------|
| `about` | — | 显示插件版本、平台、作者信息 |
| `status` | — | 显示模板数、活跃实例数、准备中实例数、开发模式状态 |
| `help` | — | 交互式命令树帮助 |
| `reload` | — | 异步重载所有配置，完成后显示诊断信息 |

---

## 地牢管理 `/kangeldungeonmanage`

权限：`kangeldungeon.command.dungeon`（默认 OP）
别名：`/dgm`、`/dungeonm`

### 创建与管理

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `templates` | — | 列出所有已加载的地牢模板（名称、人数限制、时间限制） |
| `create` | `<模板名> [难度] [队长] [额外玩家...]` | 创建并启动地牢。不指定队长则使用命令发送者。支持 `allowParty` 自动组队 |
| `start` | `<uuid>` | 手动开始 PREPARING 状态的地牢 |
| `stop` | `<uuid>` | 失败/停止 ACTIVE 或 PREPARING 状态的地牢 |
| `complete` | `<uuid>` | 手动完成 ACTIVE 状态的地牢 |

### 查看

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `list` | — | 列出所有活跃实例（模板、状态、难度、人数、已用时间、UUID） |
| `info` | `<uuid>` | 查看实例详情（模板、UUID、状态、难度、玩家、时间限制） |
| `listplayers` | `<uuid>` | 列出地牢中所有玩家及其状态（在线/死亡/离线） |

### 玩家操作

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `join` | `<uuid>` | 加入地牢。若启用了 `allowParty`，自动邀请全队成员 |
| `leave` | — | 离开当前地牢（遵循 `onLeave` 代理检查） |
| `forceleave` | — | 强制离开，跳过 `onLeave` 检查（需 `kangeldungeon.command.forceleave` 权限） |
| `tp` | `<uuid>` | 传送至地牢世界（自动加入地牢） |
| `kick` | `<玩家>` | 将玩家踢出地牢 |
| `forcekick` | `<玩家>` | 强制踢出，跳过 `onLeave` 检查 |
| `addplayer` | `<uuid> <玩家>` | 将玩家添加到指定地牢实例 |

---

## 管理员命令 `/kangeldungeonadmin`

权限：`kangeldungeon.command.admin`（默认 OP）
别名：`/kda`、`/dungeonadmin`

### 全局控制

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `info` | — | 插件全局状态：各状态实例数、模板数、脚本数、障碍物数、怪物数、黑名单数、维护/开发模式 |
| `stopall` | — | 强制停止所有活跃/准备中的地牢实例 |
| `purge` | — | 清理所有已完成/失败的实例（删除世界、卸载） |
| `maintenance` | `[on/off]` | 查看或切换维护模式（阻止非管理员加入地牢） |
| `save` | — | 输出当前所有实例的快照日志 |
| `kickall` | — | 将所有玩家踢出所有地牢（含离线玩家清理） |

### 世界管理

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `worlds` | — | 列出所有地牢世界及加载状态 |
| `unloadworld` | `<世界名>` | 卸载地牢世界，传送玩家离开，清理区域方块 |

### 玩家管理

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `playerinfo` | `<玩家>` | 显示玩家的地牢状态、黑名单状态 |
| `blacklist add` | `<玩家> [原因]` | 封禁玩家（踢出地牢并禁止进入） |
| `blacklist remove` | `<玩家>` | 解除封禁 |
| `blacklist list` | — | 列出所有被封禁玩家 |

### 元数据操作

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `meta <uuid> list` | — | 列出实例所有元数据 |
| `meta <uuid> get` | `<key>` | 获取指定元数据值 |
| `meta <uuid> set` | `<key> <value>` | 设置元数据（自动解析 int/double） |
| `meta <uuid> add` | `<key> <value>` | 数值累加 |
| `meta <uuid> delete` | `<key>` | 删除元数据 |

### 实例操作

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `instance` | `<uuid>` | 显示完整实例详情（玩家、元数据、怪物击杀） |
| `broadcast` | `<uuid> <消息>` | 向地牢所有玩家发送 MiniMessage 消息 |
| `heal` | `<uuid>` | 治疗地牢所有玩家（满血+满饱食度+灭火） |
| `forceleave` | `<uuid> <玩家>` | 强制指定玩家离开指定地牢（管理员版） |

---

## 调试命令 `/kangeldungeondebug`

权限：`kangeldungeon.command.debug`（默认 OP）
别名：`/dgd`、`/dungeondebug`

### 查看配置

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `getTemplates` | — | 列出所有模板详情（显示名、人数、时间、PVP、重生设置） |
| `getMonsterConfigs` | `<地牢名>` | 显示怪物配置（`*` 查看所有） |
| `getObstacleConfigs` | `<地牢名>` | 显示障碍物配置（含全局） |
| `getInteractConfigs` | `<地牢名>` | 显示交互配置 |
| `getPlans` | `<地牢名>` | 显示计划配置 |
| `getRegionConfigs` | `<地牢名>` | 显示区域配置 |
| `getScripts` | `<地牢名>` | 显示脚本配置 |
| `getDungeonOptions` | `<地牢名>` | 显示地牢选项完整详情 |
| `getKits` | `<地牢名>` | 显示 Kit 配置（全局+每个地牢） |
| `getConfig` | `<key>` | 获取主配置值 |

### 查看实例

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `getInstances` | — | 列出所有活跃实例 |
| `getInstance` | `<uuid>` | 显示实例完整详情（玩家、元数据、活跃计划/怪物） |
| `checkWorld` | `<uuid>` | 检查地牢世界状态（环境、难度、实体数、时间） |
| `getActivePlans` | `<uuid>` | 显示实例中活跃的计划 |
| `getActiveMonsters` | `<uuid>` | 显示实例中活跃的怪物组 |

### 脚本测试

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `evalInstance` | `<uuid> <js>` | 在指定地牢实例上下文中执行 JS |
| `runScript` | `<uuid> <脚本名>` | 手动运行命名脚本 |
| `spawnTestMobs` | `<uuid> <配置ID>` | 在活跃地牢中生成测试怪物 |

### 其他

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `getDevMode` | — | 查看开发模式状态 |
| `setDevMode` | `<true/false>` | 切换开发模式 |
| `getMemoryInfo` | — | 显示缓存映射大小（模板、实例、脚本、障碍物、怪物、Kit 等） |
| `getBlockRegenMap` | — | 查看方块恢复追踪数据 |
| `checkBlockRegen` | — | 同上 |

---

## 数据命令 `/kangeldungeondata`

权限：`kangeldungeon.command.data`（默认 OP）
别名：`/dd`、`/dungeondata`

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `get` | `<id> [player]` | 获取玩家持久化数据 |
| `remove` | `<id> [player]` | 删除玩家数据 |
| `set` | `<player> <id> <value>` | 设置玩家数据 |
| `clear` | `[player]` | 清空玩家所有数据 |
| `getCooldown` | `<id> [player]` | 获取冷却剩余秒数 |
| `removeCooldown` | `<id> [player]` | 删除冷却 |
| `setCooldown` | `<player> <id> <value>` | 设置冷却（秒） |
| `clearCooldown` | `[player]` | 清空玩家所有冷却 |
| `browse` | `[player]` | 浏览所有玩家数据 |

---

## API 测试 `/kangeldungeonapi`

权限：`kangeldungeon.command.api`（默认 OP）
别名：`/dga`、`/dungeonapi`

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `minimessage` | `<content>` | 解析并显示 MiniMessage 字符串 |
| `eval` | `<script>` | 执行 Kether 脚本并显示返回值 |
| `evalJs` | `<script>` | 执行 JS 脚本并显示返回值 |
| `evalByPlayer` | `<player> <script>` | 以指定玩家身份执行 Kether |
| `evalSilent` | `<script>` | 静默执行 Kether（无输出） |
| `evalByPlayerSilent` | `<player> <script>` | 以指定玩家身份静默执行 Kether |

---

## 队伍命令 `/kangeldungeonparty`

权限：`kangeldungeon.command.party`（默认所有玩家）
别名：`/kdparty`、`/party`、`/kdp`

| 子命令 | 语法 | 说明 |
|--------|------|------|
| `create` | — | 创建队伍 |
| `invite` | `<玩家>` | 邀请玩家加入 |
| `join` | `<teamId>` | 接受邀请 |
| `leave` | — | 离开队伍（队长不能离开） |
| `kick` | `<玩家>` | 踢出队员（仅队长） |
| `transfer` | `<玩家>` | 转让队长（仅队长） |
| `disband` | — | 解散队伍（仅队长） |
| `info` | — | 查看队伍信息 |
| `invites` | — | 查看待处理邀请 |

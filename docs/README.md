# KAngelDungeon 插件文档

## 简介

KAngelDungeon 是一个基于 **TabooLib 6.3+** 的 Minecraft 地牢副本插件，支持：

- 多模板地牢，基于 `MAP`（世界模板）或 `SCHEMATIC` 创建
- 完整的副本生命周期：准备 → 进行 → 通关/失败 → 自动销毁
- JavaScript (GraalJS) 脚本驱动的事件、障碍物、怪物、任务系统
- Kether 表达式集成（TabooLib 脚本 DSL）
- 队伍/组队系统
- 多难度支持（每个地牢模板独立）
- 奖励包（Kit）系统（权重/概率两种模式，支持全局 + 每个地牢）
- 怪物组管理（激活距离、自动重生、等级缩放）
- 障碍物系统（序列动画、粒子效果）
- 区域检测（进入/离开触发）
- 交互点系统
- Plan 定时任务系统
- Task 条件触发器系统
- 玩家元数据（实例级别 + 玩家级别）
- Bukkit 事件 API（Pre/Post 模式）
- MySQL/SQLite 玩家数据存储

## 快速开始

### 1. 安装

将插件放入 `plugins/` 目录，重启服务器。

首次启动后会在 `plugins/KAngelDungeon/` 下生成：
```
KAngelDungeon/
├── config.yml          # 主配置
├── lang/               # 语言文件
├── dungeon/            # 地牢配置（示例）
│   └── sample/         # 示例地牢模板
│       ├── option.yml
│       ├── difficulty.yml
│       ├── kit/
│       ├── monster/
│       ├── obstacle/
│       ├── region/
│       ├── interact/
│       ├── plan/
│       ├── script/
│       └── task/
├── kits/               # 全局 Kit（所有地牢通用）
└── worlds/             # 世界模板存放目录
```

### 2. 创建第一个地牢

1. **准备世界模板**：在服务器世界目录下创建一个地图，或使用 Schematic 文件

2. **配置地牢选项**：编辑 `dungeon/<name>/option.yml`

3. **启动地牢**：
   ```
   /kangeldungeon dungeon create <模板名> [难度] [队长] [额外玩家...]
   ```

4. **或从 UI 加入**：玩家使用 `/kangeldungeon dungeon join <uuid>` 加入等待中的地牢

### 3. 基本命令

| 命令 | 说明 |
|------|------|
| `/dg create <template> [difficulty] [player] [players]` | 创建并开始地牢 |
| `/dg list` | 列出活跃地牢 |
| `/dg join <uuid>` | 加入地牢 |
| `/dg leave` | 离开地牢 |
| `/dg info <uuid>` | 查看地牢详情 |
| `/dg templates` | 列出可用模板 |
| `/dg reload` | 重载配置 |

### 4. JS 脚本快速示例

在 `dungeon/<name>/option.yml` 的 agent 部分：
```yaml
agent:
  onStart: |-
    instance.sendMessageToAllPlayers("<green>地牢开始！</green>");
    instance.setMeta("wave", 1);
  onComplete: |-
    instance.openKitToAll("completion_chest");
    instance.sendTitleToAllPlayers("<gold>通关！</gold>", "");
```

### 5. 依赖

- **必需**: TabooLib 6.3+
- **可选**: MythicMobs、Chemdah、ItemsAdder、Oraxen、CraftEngine

## 文档索引

| 文档 | 内容 |
|------|------|
| [命令参考](commands.md) | 全部 70+ 条命令的完整参考 |
| [配置文件](configuration.md) | 所有 YAML 配置文件格式 |
| [JS API 参考](js-api.md) | DungeonInstance 全部 150+ 个 JS 方法 |
| [JS Agent 钩子](js-agent-hooks.md) | 全部可编辑 JS 脚本字段（含示例） |
| [Kether 动作](kether-actions.md) | `kangeldp` 前缀的 Kether 脚本动作 |
| [事件系统](events.md) | 全部自定义事件（40+ 个） |
| [任务系统](task-system.md) | TaskConfig 触发器与过滤 |

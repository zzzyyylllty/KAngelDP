# 事件系统

KAngelDungeon 提供了 40+ 个自定义 Bukkit 事件，采用 Pre/Post 模式。所有事件均可被其他插件监听。

---

## 地牢生命周期事件

### 开始事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonStartPreEvent` | 是 | `instance` |
| `DungeonStartPostEvent` | 否 | `instance` |

在 `instance.start()` 调用时触发。Pre 事件可取消阻止开始。

---

### 通关事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonCompletePreEvent` | 是 | `instance` |
| `DungeonCompletePostEvent` | 否 | `instance` |

在 `instance.complete()` 调用时触发。

---

### 失败事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonFailPreEvent` | 是 | `instance` |
| `DungeonFailPostEvent` | 否 | `instance` |

在 `instance.fail()` 调用时触发。

---

### Tick 事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonTickEvent` | 否 | `instance` |

每秒触发一次，在所有状态（PREPARING/ACTIVE/COMPLETED/FAILED）下均触发。

---

## 玩家事件

### 加入

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonPlayerJoinPreEvent` | 是 | `instance`, `player` |
| `DungeonPlayerJoinPostEvent` | 否 | `instance`, `player` |

在 `instance.addPlayer()` 调用时触发。

---

### 离开

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonPlayerQuitPreEvent` | 是 | `instance`, `player` |
| `DungeonPlayerQuitPostEvent` | 否 | `instance`, `player` |

在 `instance.removePlayer()` 调用时触发。

---

### 死亡

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonPlayerDeathEvent` | 否 | `instance`, `player` |

在 `instance.markPlayerDead()` 调用时触发。

---

### 重生

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonPlayerRespawnEvent` | 否 | `instance`, `player` |

在玩家重生时触发。

---

## 怪物事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `MonsterSpawnPreEvent` | 是 | `instance`, `config` |
| `MonsterSpawnPostEvent` | 否 | `instance`, `config`, `entities` |
| `MonsterGroupClearEvent` | 否 | `instance`, `config` |

---

## 障碍物事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `ObstaclePreparePreEvent` | 是 | `instance`, `config` |
| `ObstaclePreparePostEvent` | 否 | `instance`, `config` |
| `ObstacleActivatePreEvent` | 是 | `instance`, `config` |
| `ObstacleActivatePostEvent` | 否 | `instance`, `config` |
| `ObstacleOpenPreEvent` | 是 | `instance`, `config` |
| `ObstacleOpenPostEvent` | 否 | `instance`, `config` |

---

## 区域事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `RegionEnterEvent` | 否 | `instance`, `region`, `player` |
| `RegionLeaveEvent` | 否 | `instance`, `region`, `player` |

---

## Kit 事件

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `KitOpenPreEvent` | 是 | `instance`, `kitName`, `player`, `config` |
| `KitOpenPostEvent` | 否 | `instance`, `kitName`, `player`, `config`, `rewards` |

---

## 队伍事件

| 事件 | 可取消 | 说明 |
|------|--------|------|
| `TeamCreatePreEvent` | 是 | 创建队伍前 |
| `TeamCreatePostEvent` | 否 | 创建队伍后 |
| `TeamDisbandPreEvent` | 是 | 解散队伍前 |
| `TeamDisbandPostEvent` | 否 | 解散队伍后 |
| `TeamInviteEvent` | 是 | 邀请玩家时 |
| `TeamJoinPreEvent` | 是 | 加入队伍前 |
| `TeamJoinPostEvent` | 否 | 加入队伍后 |
| `TeamLeavePreEvent` | 是 | 离开队伍前 |
| `TeamLeavePostEvent` | 否 | 离开队伍后 |
| `TeamKickPreEvent` | 是 | 踢出队员前 |
| `TeamKickPostEvent` | 否 | 踢出队员后 |
| `TeamTransferPreEvent` | 是 | 转让队长前 |
| `TeamTransferPostEvent` | 否 | 转让队长后 |

---

## 配置事件

| 事件 | 说明 |
|------|------|
| `kangeldungeonReloadEvent` | 配置重载时触发 |
| `KAngelDungeonCustomScriptDataLoadEvent` | 允许外部插件向 JS `defaultData` 注入变量 |

---

## Chemdah 集成事件

如果安装了 Chemdah 插件，以下事件用于任务进度追踪：

| 事件 | 可取消 | 字段 |
|------|--------|------|
| `DungeonMobKillEvent` | 否 | `instance`, `player`, `mobType`, `mobName`, `mobId`, `level`, `entity` |
| `DungeonPlayerCompleteEvent` | 否 | `instance`, `player` |
| `DungeonPlayerFailEvent` | 否 | `instance`, `player` |

---

## 监听示例（Kotlin）

```kotlin
import io.github.zzzyyylllty.kangeldungeon.event.DungeonStartPreEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object MyListener : Listener {

    @EventHandler
    fun onDungeonStart(event: DungeonStartPreEvent) {
        val instance = event.instance
        // 在地牢开始前做一些操作
        if (instance.templateName == "special_dungeon") {
            instance.setMeta("custom_flag", true)
        }
    }

    @EventHandler
    fun onKitOpen(event: KitOpenPreEvent) {
        // 阻止某些 Kit 在特定条件下打开
        if (event.kitName == "admin_only_kit" && !event.player.isOp) {
            event.isCancelled = true
        }
    }
}
```

## 监听示例（Java）

```java
import io.github.zzzyyylllty.kangeldungeon.event.DungeonCompletePostEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {

    @EventHandler
    public void onDungeonComplete(DungeonCompletePostEvent event) {
        var instance = event.getInstance();
        // 通关后的处理
        instance.getOnlinePlayers().forEach(player ->
            player.sendMessage("你完成了一个地牢！")
        );
    }
}
```

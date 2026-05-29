# JS API 参考（DungeonInstance）

在 JS 脚本中，`instance` 变量代表当前地牢实例。以下是所有可用方法。

---

## 生命周期与状态

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.start()` | `Boolean` | 从 PREPARING 状态开始地牢 |
| `instance.complete()` | `Boolean` | 通关地牢（进入 COMPLETED 状态） |
| `instance.fail()` | `Boolean` | 失败地牢（进入 FAILED 状态） |
| `instance.isPreparing()` | `Boolean` | 是否准备中 |
| `instance.isActive()` | `Boolean` | 是否进行中 |
| `instance.isCompleted()` | `Boolean` | 是否已通关 |
| `instance.isFailed()` | `Boolean` | 是否已失败 |
| `instance.isFinished()` | `Boolean` | 是否已结束（通关或失败） |

---

## 玩家管理

### 加入/离开

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.addPlayer(player)` | `Boolean` | 添加玩家（传送、触发事件） |
| `instance.removePlayer(player)` | `Boolean` | 移除玩家（遵循 onLeave） |
| `instance.forceRemovePlayer(player)` | `Boolean` | 强制移除（跳过 onLeave） |

### 死亡/复活

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.markPlayerDead(player)` | `Boolean` | 标记玩家死亡 |
| `instance.markPlayerAlive(player)` | `Boolean` | 取消死亡标记 |
| `instance.respawnPlayer(playerName)` | `Boolean` | 复活玩家（受 maxRespawns 限制） |
| `instance.respawnAllDeadPlayers()` | — | 复活所有死亡玩家 |
| `instance.setSpectateMode(playerName)` | `Boolean` | 设为旁观模式 |
| `instance.possessPlayer(playerName, targetName?)` | `Boolean` | 让玩家旁观另一个玩家 |

### 查询

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.getOnlinePlayers()` | `List<Player>` | 在线玩家列表 |
| `instance.getPlayerCount()` | `Int` | 玩家总数 |
| `instance.getAlivePlayerCount()` | `Int` | 存活玩家数 |
| `instance.getDeadPlayerCount()` | `Int` | 死亡玩家数 |
| `instance.getOnlinePlayerNames()` | `List<String>` | 在线玩家名称 |
| `instance.getLeader()` | `Player?` | 队长 |
| `instance.getLeaderName()` | `String?` | 队长名称 |
| `instance.getDeadPlayerNames()` | `List<String>` | 死亡玩家名称 |
| `instance.isPlayerInDungeon(playerName)` | `Boolean` | 玩家是否在地牢 |
| `instance.isPlayerDeadInDungeon(playerName)` | `Boolean` | 玩家是否已死亡 |
| `instance.isPlayerDead(playerName)` | `Boolean?` | 是否死亡（null=不在线） |
| `instance.getPlayerStatus(playerName)` | `String` | `"alive"` / `"dead"` / `"offline"` / `"not_in_dungeon"` |
| `instance.getPlayerRespawnCount(playerName)` | `Int` | 玩家重生次数 |

---

## 消息与标题

| 方法 | 说明 |
|------|------|
| `instance.sendMessageToAllPlayers(msg)` | 发送 MiniMessage 给所有玩家 |
| `instance.sendTitleToAllPlayers(title, subtitle?, fadeIn?, stay?, fadeOut?)` | 发送标题（默认 10/70/20 tick） |
| `instance.sendActionBarToAllPlayers(msg)` | 发送 ActionBar |
| `instance.sendMessageToPlayer(playerName, msg)` | 发送消息给指定玩家 |

---

## 全体玩家状态设置

| 方法 | 说明 |
|------|------|
| `instance.setAllPlayersGameMode(mode)` | 设置游戏模式（"creative"/"adventure"/"survival"/"spectator"） |
| `instance.setAllPlayersHealth(health)` | 设置生命值 |
| `instance.setAllPlayersFood(food)` | 设置饱食度 |
| `instance.setAllPlayersSaturation(saturation)` | 设置饱和度 |
| `instance.setAllPlayersLevel(level)` | 设置经验等级 |
| `instance.setAllPlayersMaxHealth(maxHealth)` | 设置最大生命值 |
| `instance.setAllPlayersWalkSpeed(speed)` | 设置步行速度 |
| `instance.setAllPlayersFlySpeed(speed)` | 设置飞行速度 |
| `instance.setAllPlayersAllowFlight(allow)` | 设置是否允许飞行 |
| `instance.healAllPlayers()` | 治疗所有玩家（满血+饱食+灭火） |
| `instance.clearAllPlayersInventory()` | 清空背包 |
| `instance.clearAllPlayersEffects()` | 清除药水效果 |
| `instance.giveExperienceToAllPlayers(amount)` | 给予经验 |
| `instance.giveItemToAllPlayers(itemStack)` | 给予物品 |

---

## 单个玩家状态设置

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.setPlayerGameMode(name, mode)` | `Boolean` | 设置游戏模式 |
| `instance.setPlayerHealth(name, health)` | `Boolean` | 设置生命值 |
| `instance.setPlayerFood(name, food)` | `Boolean` | 设置饱食度 |
| `instance.setPlayerMaxHealth(name, maxHealth)` | `Boolean` | 设置最大生命值 |
| `instance.setPlayerWalkSpeed(name, speed)` | `Boolean` | 设置步行速度 |
| `instance.setPlayerFlySpeed(name, speed)` | `Boolean` | 设置飞行速度 |
| `instance.setPlayerAllowFlight(name, allow)` | `Boolean` | 设置飞行 |
| `instance.setPlayerLevel(name, level)` | `Boolean` | 设置等级 |
| `instance.healPlayer(name)` | `Boolean` | 治疗 |
| `instance.clearPlayerEffects(name)` | `Boolean` | 清除药水效果 |

---

## 传送

| 方法 | 说明 |
|------|------|
| `instance.teleportAllToSpawn()` | 传送所有玩家到出生点 |
| `instance.teleportAllTo(x, y, z)` | 传送所有玩家到坐标 |
| `instance.teleportPlayerToSpawn(name)` | 传送单个玩家到出生点 |
| `instance.teleportPlayerTo(name, x, y, z)` | 传送单个玩家到坐标 |
| `instance.teleportPlayerTo(name, x, y, z, yaw, pitch)` | 传送（含朝向） |

---

## 药水效果

| 方法 | 说明 |
|------|------|
| `instance.applyPotionEffectToAllPlayers(type, duration, amplifier)` | 给所有玩家添加药水效果（type="SPEED", duration=ticks） |
| `instance.removePotionEffectFromAllPlayers(type)` | 移除所有玩家的指定药水效果 |
| `instance.applyPotionEffectToPlayer(name, type, duration, amplifier)` | 给单个玩家添加药水效果 |
| `instance.removePotionEffectFromPlayer(name, type)` | 移除单个玩家的指定药水效果 |

---

## Scoreboard Tags

| 方法 | 说明 |
|------|------|
| `instance.addScoreboardTag(name, tag)` | 添加标签 |
| `instance.removeScoreboardTag(name, tag)` | 移除标签 |
| `instance.hasScoreboardTag(name, tag)` | 是否有标签 |
| `instance.addScoreboardTagToAllPlayers(tag)` | 全体添加标签 |
| `instance.removeScoreboardTagFromAllPlayers(tag)` | 全体移除标签 |

---

## 世界操作

| 方法 | 说明 |
|------|------|
| `instance.setWorldTime(ticks)` | 设置时间（0-24000） |
| `instance.getWorldTime()` | 获取时间 |
| `instance.setWorldStorm(boolean)` | 设置下雨 |
| `instance.setWorldThundering(boolean)` | 设置雷暴 |
| `instance.hasStorm()` | 是否下雨 |
| `instance.isThundering()` | 是否雷暴 |
| `instance.setWorldDifficulty(str)` | 设置难度（"peaceful"/"easy"/"normal"/"hard"） |
| `instance.setGameRule(rule, value)` | 设置游戏规则 |
| `instance.setWorldBorder(centerX, centerZ, size)` | 设置世界边界 |

---

## 视觉效果

| 方法 | 说明 |
|------|------|
| `instance.strikeLightning(x, y, z)` | 闪电（仅视觉效果） |
| `instance.explosionEffect(x, y, z, power)` | 爆炸效果（不破坏方块） |
| `instance.firework(x, y, z)` | 简单烟花（红+黄） |
| `instance.fireworkCustom(x, y, z, type, colors, fadeColors, trail)` | 自定义烟花（type: BALL/BALL_LARGE/STAR/BURST/CREEPER） |
| `instance.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, speed)` | 生成粒子 |
| `instance.spawnParticleLine(particle, x1, y1, z1, x2, y2, z2, count)` | 线段粒子 |

---

## 音效

| 方法 | 说明 |
|------|------|
| `instance.broadcastSound(sound, volume, pitch)` | 全体播放音效 |
| `instance.playSoundAt(sound, x, y, z, volume, pitch)` | 指定位置播放音效 |
| `instance.playSoundToPlayer(name, sound, volume, pitch)` | 指定玩家播放音效 |

---

## 实体与物品生成

| 方法 | 说明 |
|------|------|
| `instance.dropItem(x, y, z, material, amount)` | 掉落物品（如 `"DIAMOND", 1`） |
| `instance.dropItemStack(x, y, z, itemStack)` | 掉落物品堆 |
| `instance.clearDropItems()` | 清除所有掉落物 |
| `instance.clearEntities(type)` | 清除指定类型实体（如 `"ARROW"`） |
| `instance.clearHostileMobs()` | 清除所有敌对生物 |
| `instance.clearAllMobs()` | 清除所有生物 |

---

## 命令执行

| 方法 | 说明 |
|------|------|
| `instance.executeCommand(cmd)` | 以控制台身份执行命令 |
| `instance.executeCommandAsPlayer(name, cmd)` | 以玩家身份执行命令 |

---

## 脚本执行

| 方法 | 说明 |
|------|------|
| `instance.runScript(name)` | 执行命名脚本（先 onRun 再 onPost） |
| `instance.runAllScripts()` | 执行模板的所有脚本 |

---

## 目标选择器

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.selectTargets(selector)` | `List<Player>` | 选择目标，如 `"@all{health>10}"` |
| `instance.selectFirstTarget(selector)` | `Player?` | 选择第一个目标 |
| `instance.selectRandomTarget(selector)` | `Player?` | 随机选择目标 |
| `instance.getTargetCount(selector)` | `Int` | 目标数量 |
| `instance.selectTargetsInRadius(selector, cx, cy, cz, radius)` | `List<Player>` | 范围内选择目标 |

**选择器语法**：`@all{条件}` / `@alive{条件}` / `@dead{条件}`

条件支持：`health>10`、`level>=5`、`name=PlayerName` 等

---

## Kit（奖励包）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.openKit(kitName, player)` | `Boolean` | 给单个玩家开启 Kit |
| `instance.openKitToAll(kitName)` | `Boolean` | 给所有在线玩家开启 Kit |

查找顺序：全局 Kit → 地牢专属 Kit（地牢版本覆盖全局版本）

---

## 障碍物

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.prepareObstacle(id)` | `Boolean` | 准备障碍物 |
| `instance.activateObstacle(id)` | `Boolean` | 激活障碍物 |
| `instance.openObstacle(id)` | `Boolean` | 打开障碍物 |
| `instance.openObstacleForce(id)` | `Boolean` | 强制打开（跳过检查） |
| `instance.restoreObstacleBlocks()` | — | 恢复所有障碍物方块 |
| `instance.getObstacleConfigs()` | `Map` | 获取所有障碍物配置 |

---

## 怪物

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.spawnMonsters(monsterId)` | `Boolean` | 生成怪物组 |
| `instance.getMonsterConfigs()` | `Map` | 获取怪物配置 |
| `instance.getMonsterInstances()` | `Map` | 获取怪物实例 |
| `instance.getActiveMonsters()` | `Map` | 获取活跃怪物组 |
| `instance.setMonsterActive(id, active)` | — | 设置怪物组激活状态 |
| `instance.setMonsterCooldown(id, ticks)` | — | 设置怪物组冷却 |
| `instance.setMonsterActivationRangeMin(id, value)` | — | 设置最小激活距离（null=恢复默认） |
| `instance.setMonsterActivationRangeMax(id, value)` | — | 设置最大激活距离（null=恢复默认） |
| `instance.resetMonsterActivationRange(id)` | — | 重置激活距离为配置默认值 |

---

## 任务

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.getTaskConfigs()` | `Map` | 获取任务配置 |
| `instance.triggerTask(taskId)` | `Boolean` | 手动触发任务（绕过冷却和次数限制） |
| `instance.getTaskExecutionCount(taskId)` | `Int` | 获取任务执行次数 |
| `instance.resetTaskExecutionCount(taskId)` | — | 重置任务执行计数 |

---

## 区域

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.getRegionConfigs()` | `Map` | 获取区域配置 |
| `instance.isPlayerInRegion(name, regionId)` | `Boolean` | 玩家是否在区域内 |
| `instance.getPlayersInRegion(regionId)` | `List<Player>` | 获取区域内的玩家 |
| `instance.getPlayerRegions(name)` | `List<String>` | 获取玩家所在的区域列表 |

---

## 交互点

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.getInteractConfigs()` | `Map` | 获取交互配置 |
| `instance.triggerInteractBtn(interactId)` | `Boolean` | 触发交互按钮 |

---

## 时间查询

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.getElapsedTime()` | `Double` | 已用时间（秒） |
| `instance.getRemainingTime(template)` | `Double?` | 剩余时间（秒） |
| `instance.getElapsedTimeFormatted()` | `String` | 已用时间格式化（"MM:SS"） |
| `instance.isTimedOut(template)` | `Boolean` | 是否超时 |
| `instance.areAllPlayersDead()` | `Boolean` | 所有玩家是否已死亡 |

---

## 难度

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `instance.getDifficulty()` | `String?` | 获取难度 ID |
| `instance.getDifficultyConfig()` | `DifficultyConfig?` | 获取难度配置对象 |

---

## 实例元数据（Instance Meta）

| 方法 | 说明 |
|------|------|
| `instance.setMeta(key, value)` | 设置元数据 |
| `instance.addMeta(key, value)` | 数值累加 |
| `instance.getMeta(key)` | 获取元数据（Any?） |
| `instance.getMetaAsInt(key)` | 获取为 Int |
| `instance.getMetaAsDouble(key)` | 获取为 Double |
| `instance.getMetaAsFloat(key)` | 获取为 Float |
| `instance.getMetaAsBoolean(key)` | 获取为 Boolean |
| `instance.getMetaAsString(key)` | 获取为 String |
| `instance.getMetaAsUUID(key)` | 获取为 UUID |
| `instance.getMetaAsList(key)` | 获取为 List |
| `instance.getMetaAsMap(key)` | 获取为 Map |
| `instance.hasMeta(key)` | 是否存在 |

---

## 玩家元数据（Player Meta）

每个地牢实例独立，不持久化。玩家离开地牢时自动清除。

| 方法 | 说明 |
|------|------|
| `instance.setPlayerMeta(player, key, value)` | 设置玩家元数据 |
| `instance.addPlayerMeta(player, key, value)` | 数值累加 |
| `instance.getPlayerMeta(player, key)` | 获取玩家元数据 |
| `instance.getPlayerMetaAsInt(player, key)` | 获取为 Int |
| `instance.getPlayerMetaAsDouble(player, key)` | 获取为 Double |
| `instance.getPlayerMetaAsString(player, key)` | 获取为 String |
| `instance.getPlayerMetaAsBoolean(player, key)` | 获取为 Boolean |
| `instance.hasPlayerMeta(player, key)` | 是否存在 |
| `instance.removePlayerMeta(player, key)` | 删除 |

---

## 击杀统计

| 方法 | 说明 |
|------|------|
| `instance.incrementMobKills()` | 增加生物击杀数 |
| `instance.incrementMobKills(mobName)` | 增加特定生物击杀数 |
| `instance.incrementBossKills()` | 增加 Boss 击杀数 |
| `instance.incrementBossKillsNamed(mobName)` | 增加特定 Boss 击杀数 |
| `instance.incrementBossAndMobKills()` | 同时增加 Boss 和生物击杀 |
| `instance.incrementBossAndMobKillsNamed(mobName)` | 同时增加（指定名称） |

---

## 公开属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `instance.uuid` | `UUID` | 实例 UUID |
| `instance.templateName` | `String` | 地牢模板名 |
| `instance.players` | `MutableSet<UUID>` | 所有玩家 UUID |
| `instance.deadPlayers` | `MutableSet<UUID>` | 死亡玩家 UUID |
| `instance.leaderUUID` | `UUID` | 队长 UUID |
| `instance.spawnLocation` | `Location` | 出生点 |
| `instance.worldName` | `String` | 世界名称 |
| `instance.world` | `World?` | Bukkit 世界对象 |
| `instance.state` | `DungeonState` | 状态（PREPARING/ACTIVE/COMPLETED/FAILED） |
| `instance.createdAt` | `Long` | 创建时间戳 |
| `instance.startedAt` | `Long?` | 开始时间戳 |
| `instance.completedAt` | `Long?` | 结束时间戳 |
| `instance.difficultyId` | `String?` | 难度 ID |

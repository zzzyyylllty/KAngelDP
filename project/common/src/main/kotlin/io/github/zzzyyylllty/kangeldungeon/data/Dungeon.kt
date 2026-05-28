package io.github.zzzyyylllty.kangeldungeon.data

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.event.*
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.CastHelper
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper
import io.github.zzzyyylllty.kangeldungeon.util.monster.MonsterManager
import io.github.zzzyyylllty.kangeldungeon.util.obstacle.ObstacleManager
import io.github.zzzyyylllty.kangeldungeon.util.plan.PlanManager
import io.github.zzzyyylllty.kangeldungeon.util.region.RegionManager
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import io.github.zzzyyylllty.kangeldungeon.function.kether.evalKetherBoolean
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import taboolib.module.lang.asLangText
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地牢脚本 - 定义从 script/ 目录加载的独立脚本
 * @param name 脚本名称
 * @param onRun 主执行阶段 JS 代码（先执行）
 * @param onPost 后执行阶段 JS 代码（onRun 完成后执行）
 */
data class DungeonScript(
    val name: String,
    val onRun: String? = null,
    val onPost: String? = null
)

/**
 * 地牢模板 - 定义地牢的静态配置信息
 */
data class DungeonTemplate(
    val name: String,
    val displayName: String = name,
    val description: List<String> = emptyList(),

    // 地图配置
    val schematicFile: String? = null, // Schematic文件
    val worldTemplate: String? = null, // 世界模板文件夹名
    val schematicPasteLocation: Vector, // Schematic粘贴位置（世界坐标）

    // 显示配置
    val icon: IconConfig? = null, // 图标配置（用于GUI展示）

    // 时间限制
    val timeLimit: Double? = 3600.0, // 秒
    val preparationTime: Double? = 30.0, // 准备时间（秒）

    // 其他设置
    val allowRespawn: Boolean = false,
    val keepInventory: Boolean = false, // 是否保留背包（旧字段，向后兼容）
    val pvpEnabled: Boolean = false,
    val naturalRegeneration: Boolean = true,

    // 权限要求
    val requiredPermission: String? = null,

    // 玩家出生点（相对坐标）
    val playerSpawnOffset: Vector,

    // === 以下字段来自 option.yml 的完整配置 ===

    // 通用游戏设置
    val gameplayGeneral: GameplayGeneralConfig = GameplayGeneralConfig(),

    // 命令过滤配置
    val commandConfig: CommandConfig = CommandConfig(),

    // 序列/计时器配置
    val timerConfig: TimerConfig = TimerConfig(),

    // 原版游戏选项
    val vanillaOptions: VanillaOptions = VanillaOptions(),

    // 游戏规则（启动时自动设置，如 doTileDrops, doDaylightCycle 等）
    val gameRules: Map<String, Any> = emptyMap(),

    // 地牢元数据模板
    val metaConfig: MetaConfig = MetaConfig(),

    // 地牢事件代理脚本（在特定生命周期执行）
    val agents: Agents? = null
) {
    /**
     * 获取解析后的出生点坐标（Vector），兼容 spawnpoint 配置
     */
    val effectiveSpawnpoint: Vector
        get() = vanillaOptions.spawnpoint ?: playerSpawnOffset

    /**
     * 获取 KeepInventory 的详细配置
     */
    val keepInventoryConfig: KeepInventoryConfig
        get() = gameplayGeneral.keepInventory

    /**
     * 执行指定触发器的代理脚本
     * @param trigger 触发器名称（如 "onStart", "onComplete", "onFail"）
     * @param extraVariables 额外传递给脚本的变量
     * @param player 关联的玩家（可为 null）
     * @return 脚本执行结果，默认返回 true
     */
    fun runAgent(trigger: String, extraVariables: Map<String, Any?>, player: Player?): Boolean {
        return agents?.runAgent(trigger, extraVariables, player) ?: true
    }
}

/**
 * 地牢实例 - 运行中的地牢副本
 * @param templateName 地牢模板名称
 * @param uuid 地牢的UUID
 * @param players 地牢玩家
 * @param deadPlayers 地牢死亡玩家
 * @param leaderUUID 地牢小队领导者
 * @param createdAt 创建时间
 * @param startedAt 开始时间
 * @param completedAt 完成时间
 * @param state 地牢状态
 * @param meta 地牢元数据
 * @param spawnLocation 地牢出生点
 */
class DungeonInstance(
    val templateName: String,
    val uuid: UUID = UUID.randomUUID(),

    // 玩家管理
    val players: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
    val deadPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
    val leaderUUID: UUID,

    // 时间管理（@Volatile 保证多线程可见性）
    @Volatile
    var createdAt: Long = System.currentTimeMillis(),
    @Volatile
    var startedAt: Long? = null,
    @Volatile
    var completedAt: Long? = null,

    // 状态管理（@Volatile 保证多线程可见性）
    @Volatile
    var state: DungeonState = DungeonState.PREPARING,

    // 统计信息
    var meta: DungeonMeta,

    // 玩家元数据（每个地牢实例独立，不持久化）
    val playerMeta: ConcurrentHashMap<UUID, DungeonMeta> = ConcurrentHashMap(),

    // 位置信息
    val spawnLocation: Location,

    // 世界是否已就绪（用于 schematic 模式，防止 tick 在粘贴完成前自动开始）
    @Volatile
    var worldReady: Boolean = true,

    // 准备阶段通知追踪：上次通知时剩余的秒数，避免每秒重复发送
    @Volatile
    var lastPrepNotifyRemaining: Int = -1,

    // 结束阶段倒计时追踪：上次通知时剩余的秒数
    @Volatile
    var lastEndCountdownRemaining: Int = -1,

    // 选择的难度 ID（null 表示默认/无难度）
    val difficultyId: String? = null
) {

    /**
     * 获取Bukkit世界名称
     */
    val worldName: String
        get() = DungeonHelper.getWorldName(templateName, uuid)

    /**
     * 获取Bukkit世界对象
     */
    val world: World?
        get() = Bukkit.getWorld(DungeonHelper.getWorldName(templateName, uuid))

    /**
     * 获取在线地牢玩家列表
     */
    fun getOnlinePlayers(): List<Player> {
        return players.mapNotNull { Bukkit.getPlayer(it) }
    }

    /**
     * 获取队长
     */
    fun getLeader(): Player? = Bukkit.getPlayer(leaderUUID)


    /**
     * 添加玩家到地牢
     * @param player 目标玩家
     * @return 是否成功加入。如果事件被取消或状态不允许则返回 false
     */
    fun addPlayer(player: Player): Boolean {
        // 1. 状态检查
        if (state != DungeonState.PREPARING && state != DungeonState.ACTIVE) {
            return false
        }

        // 2. 检查玩家是否已在其他活跃地牢中（O(1) 反向索引）
        val existingInstance = KAngelDungeon.playerToInstanceIndex[player.uniqueId]
        if (existingInstance != null && existingInstance != uuid) {
            return false
        }

        // 3. 触发 Pre 事件 (可取消)
        val event = DungeonPlayerJoinPreEvent(this, player)
        event.call()
        if (event.isCancelled) return false

        // 4. 执行核心逻辑
        val added = players.add(player.uniqueId)

        if (added) {
            // 5. 更新反向索引
            KAngelDungeon.playerToInstanceIndex[player.uniqueId] = uuid
            // 6. 缓存玩家进入地牢前的位置
            DungeonHelper.playerPreviousLocations[player.uniqueId] = player.location.clone()
            // 6. 传送玩家到地牢出生点
            player.teleport(spawnLocation)
            // 7. 触发 Post 事件 (不可取消)
            DungeonPlayerJoinPostEvent(this, player).call()
            meta.add("player.join", 1)
            // 触发 PLAYER_JOIN 任务
            TaskManager.triggerTasks(this, "PLAYER_JOIN", mapOf(
                "playerName" to player.name,
                "player" to player
            ))
        }

        return added
    }

    /**
     * 将玩家从地牢中移除
     * @param player 目标玩家
     * @return 是否成功移除
     */
    fun removePlayer(player: Player): Boolean {
        // 1. 执行 onLeave 条件检查脚本（返回 false 则阻止离开）
        val allowLeave = getTemplate()?.runAgent("onLeave", mapOf("instance" to this, "player" to player), player)
        if (allowLeave == false) {
            // 执行 onLeaveFail 发送提示消息
            getTemplate()?.runAgent("onLeaveFail", mapOf("instance" to this, "player" to player), player)
            return false
        }
        return removePlayerInternal(player)
    }

    /**
     * 强制移除玩家（跳过 onLeave 条件检查，但仍触发 PreEvent）
     * JS: instance.forceRemovePlayer(player)
     */
    fun forceRemovePlayer(player: Player): Boolean {
        return removePlayerInternal(player)
    }

    /**
     * 移除玩家的内部实现（跳过 onLeave，但执行 PreEvent → 移除 → PostEvent）
     */
    private fun removePlayerInternal(player: Player): Boolean {
        // 1. 触发 Pre 事件 (可取消，例如：战斗状态下不允许退出)
        val event = DungeonPlayerQuitPreEvent(this, player)
        event.call()
        if (event.isCancelled) return false

        // 2. 执行核心逻辑
        val removed = players.remove(player.uniqueId)

        if (removed) {
            // 3. 清理反向索引
            KAngelDungeon.playerToInstanceIndex.remove(player.uniqueId, uuid)
            // 3.5 清理玩家元数据
            clearPlayerMeta(player)
            // 4. 从死亡名单中清除
            deadPlayers.remove(player.uniqueId)
            // 5. 传送玩家回到进入地牢前的位置
            try {
                val prev = DungeonHelper.playerPreviousLocations.remove(player.uniqueId)
                if (prev != null) {
                    player.teleport(prev)
                }
            } catch (e: Exception) {
                // ignore teleport failure
            }
            // 6. 触发 Post 事件 (不可取消)
            DungeonPlayerQuitPostEvent(this, player).call()
            meta.add("player.leave", 1)
            // 触发 PLAYER_LEAVE 任务
            TaskManager.triggerTasks(this, "PLAYER_LEAVE", mapOf(
                "playerName" to player.name,
                "player" to player
            ))
        }

        return removed
    }

    /**
     * 将死亡玩家标记为已死亡，并从活跃玩家中移除
     * @return 是否成功标记（玩家不存在或已死亡返回 false）
     */
    fun markPlayerDead(player: Player): Boolean {
        if (player.uniqueId !in players) return false
        // 防止重复标记：如果已在死亡名单中，不再重复统计
        if (!deadPlayers.add(player.uniqueId)) return false
        meta.add("player.dead", 1)
        meta.add("player.dead.${player.name}", 1)
        DungeonPlayerDeathEvent(this, player).call()
        return true
    }

    /**
     * 将玩家从死亡名单中移除（复活）
     */
    fun markPlayerAlive(player: Player): Boolean {
        return deadPlayers.remove(player.uniqueId)
    }

    /**
     * 开始地牢
     * @return 地牢是否开始成功，如果地牢开始事件被取消或者地牢已经开始则会返回false
     */
    fun start(): Boolean {
        synchronized(this) {
            if (state != DungeonState.PREPARING) {
                warningL("WarningDungeonAlreadyStarted", templateName, uuid)
                return false
            }
        }

        // 2. 触发 Pre 事件 (可取消)
        val event = DungeonStartPreEvent(this)
        event.call()
        if (event.isCancelled) return false

        // 3. 执行状态变更
        synchronized(this) {
            if (state != DungeonState.PREPARING) return false
            state = DungeonState.ACTIVE
        }
        startedAt = System.currentTimeMillis()

        // 4. 传送所有玩家到出生点
        teleportAllToSpawn()

        // 5. 应用游戏设置
        val template = getTemplate()
        if (template != null) {
            if (template.gameplayGeneral.adventureMode) {
                setAllPlayersGameMode("adventure")
            }
            // 应用游戏规则（如 doTileDrops, doDaylightCycle 等）
            for ((rule, value) in template.gameRules) {
                setGameRule(rule, value.toString())
            }
        }

        // 6. 执行 onStart 代理脚本
        runAgentSafe("onStart", mapOf("instance" to this, "template" to getTemplate()), null)

        // 6b. 执行难度 onStart 代理脚本
        runDifficultyAgent("onStart", mapOf("instance" to this, "template" to getTemplate()))

        // 触发 DUNGEON_START 任务
        TaskManager.triggerTasks(this, "DUNGEON_START")

        // 7. 停止准备阶段计划，启动 BEGIN 触发器计划
        stopAllPlans()
        startPlansForTrigger("BEGIN")

        // 8. 发送开始通知
        sendTitleToAllPlayers(
            io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console.asLangText("DungeonStartTitle"),
            io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.console.asLangText("DungeonStartSubtitle"),
            10, 60, 20
        )

        // 9. 触发 Post 事件 (不可取消)
        DungeonStartPostEvent(this).call()
        meta.add("dungeon.start", 1)
        return true
    }

    /**
     * 完成地牢 (成功通关)
     * @return 是否成功触发完成逻辑
     */
    fun complete(): Boolean {
        synchronized(this) {
            if (state != DungeonState.ACTIVE) return false
        }

        // 1. 触发 Pre 事件 (可取消)
        val event = DungeonCompletePreEvent(this)
        event.call()
        if (event.isCancelled) return false

        // 2. 执行状态变更
        synchronized(this) {
            if (state != DungeonState.ACTIVE) return false
            state = DungeonState.COMPLETED
        }
        completedAt = System.currentTimeMillis()

        // 3. 执行 onComplete 代理脚本
        runAgentSafe("onComplete", mapOf("instance" to this, "template" to getTemplate()), null)

        // 3b. 执行难度 onComplete 代理脚本
        runDifficultyAgent("onComplete", mapOf("instance" to this, "template" to getTemplate()))

        // 触发 DUNGEON_COMPLETE 任务
        TaskManager.triggerTasks(this, "DUNGEON_COMPLETE")

        // 4. 停止所有进行中的计划，并启动 END 触发器计划
        stopAllPlans()
        startPlansForTrigger("END")

        // 5. 触发 Post 事件 (不可取消)
        DungeonCompletePostEvent(this).call()
        meta.add("dungeon.complete", 1)

        // 6. 为每个在线玩家触发单人完成事件（供 Chemdah 任务系统使用）
        for (uuid in players) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                try { DungeonPlayerCompleteEvent(this, player).call() } catch (_: Exception) { }
            }
        }
        return true
    }

    /**
     * 地牢失败 (例如玩家全灭或超时)
     * @return 是否成功触发失败逻辑
     */
    fun fail(): Boolean {
        synchronized(this) {
            if (state != DungeonState.ACTIVE && state != DungeonState.PREPARING) return false
        }

        // 1. 触发 Pre 事件 (可取消)
        val event = DungeonFailPreEvent(this)
        event.call()
        if (event.isCancelled) return false

        // 2. 执行状态变更
        synchronized(this) {
            if (state != DungeonState.ACTIVE && state != DungeonState.PREPARING) return false
            state = DungeonState.FAILED
        }
        completedAt = System.currentTimeMillis()

        // 3. 执行 onFail 代理脚本
        runAgentSafe("onFail", mapOf("instance" to this, "template" to getTemplate()), null)

        // 3b. 执行难度 onFail 代理脚本
        runDifficultyAgent("onFail", mapOf("instance" to this, "template" to getTemplate()))

        // 触发 DUNGEON_FAIL 任务
        TaskManager.triggerTasks(this, "DUNGEON_FAIL")

        // 4. 停止所有进行中的计划，并启动 FAIL 触发器计划
        stopAllPlans()
        startPlansForTrigger("FAIL")

        // 5. 触发 Post 事件 (不可取消)
        DungeonFailPostEvent(this).call()
        meta.add("dungeon.fail", 1)

        // 6. 为每个在线玩家触发单人失败事件（供 Chemdah 任务系统使用）
        for (uuid in players) {
            val player = Bukkit.getPlayer(uuid)
            if (player != null && player.isOnline) {
                try { DungeonPlayerFailEvent(this, player).call() } catch (_: Exception) { }
            }
        }
        return true
    }

    /**
     * 安全执行指定触发器的代理脚本（捕获异常，不中断主流程）
     */
    private fun runAgentSafe(trigger: String, extraVariables: Map<String, Any?>, player: Player?) {
        try {
            getTemplate()?.runAgent(trigger, extraVariables, player)
        } catch (e: Exception) {
            warningL("WarningAgentExecutionFailed", trigger, templateName, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 执行当前难度对应的代理脚本（与 option.yml agent 独立）
     */
    private fun runDifficultyAgent(trigger: String, extraVariables: Map<String, Any?> = emptyMap()) {
        val diffId = difficultyId ?: return
        val diffConfig = KAngelDungeon.dungeonDifficultyConfigs[templateName]?.get(diffId) ?: return
        val script = diffConfig.agents[trigger] ?: return
        try {
            val data = defaultData + mapOf(
                "instance" to this,
                "template" to getTemplate(),
                "difficulty" to diffConfig
            ) + extraVariables
            GraalJsUtil.cachedEval(script, data)
        } catch (e: Exception) {
            warningL("WarningDifficultyAgentFailed", trigger, templateName, diffId, e.message ?: "Unknown error")
            e.printStackTrace()
        }
    }

    /**
     * 根据触发器名称启动匹配的计划
     * 委托给 PlanManager 统一管理
     */
    fun startPlansForTrigger(trigger: String) {
        PlanManager.startPlansForTrigger(this, trigger)
    }

    /**
     * 停止所有进行中的计划任务
     */
    fun stopAllPlans() {
        PlanManager.stopAllPlans(this)
    }

    /**
     * 检查指定计划是否活跃
     */
    fun isPlanActive(planName: String): Boolean {
        return PlanManager.isPlanActive(this, planName)
    }

    /**
     * 获取活跃计划名称列表
     */
    fun getActivePlanNames(): List<String> {
        return PlanManager.getActivePlanNames(this)
    }

    /**
     * 获取已用时间（秒）
     */
    fun getElapsedTime(): Double {
        val start = startedAt ?: return 0.0
        val end = completedAt ?: System.currentTimeMillis()
        return (end - start).toDouble() / 1000.0
    }

    /**
     * 获取剩余时间（秒）
     * null代表没有时间限制
     */
    fun getRemainingTime(template: DungeonTemplate): Double? {
        val elapsed = getElapsedTime()
        return (template.timeLimit?.minus(elapsed))?.coerceAtLeast(0.0)
    }

    /**
     * 是否超时
     */
    fun isTimedOut(template: DungeonTemplate): Boolean {
        val limit = template.timeLimit ?: return false
        if (limit <= 0) return false
        return getElapsedTime() >= limit
    }

    /**
     * 所有玩家是否都死亡
     * 检查所有已注册玩家是否都已死亡或离线（离线玩家视为无法继续地牢）。
     * 防止掉线和死亡导致的死锁。
     */
    fun areAllPlayersDead(): Boolean {
        if (players.isEmpty()) return state == DungeonState.ACTIVE
        return players.all { it in deadPlayers || Bukkit.getPlayer(it) == null }
    }

    /**
     * 增加击杀数
     */
    fun incrementMobKills() {
        meta.add("mob.kill", 1)
    }

    /**
     * 增加特定怪击杀数
     */
    fun incrementMobKills(mobName: String) {
        meta.add("mob.kill", 1)
        meta.add("mob.kill.${mobName}", 1)
    }

    /**
     * 增加Boss击杀数
     * 不会同步增加怪物击杀数
     */
    fun incrementBossKills() {
        meta.add("boss.kill", 1)
    }

    /**
     * 增加特定Boss击杀数
     * 不会同步增加怪物击杀数
     */
    fun incrementBossKillsNamed(mobName: String) {
        meta.add("boss.kill", 1)
        meta.add("boss.kill.${mobName}", 1)
    }

    /**
     * 增加Boss击杀数
     * 同步增加怪物击杀数
     */
    fun incrementBossAndMobKills() {
        meta.add("boss.kill", 1)
        meta.add("mob.kill", 1)
    }

    /**
     * 增加特定Boss击杀数
     * 同步增加怪物击杀数
     */
    fun incrementBossAndMobKillsNamed(mobName: String) {
        meta.add("boss.kill", 1)
        meta.add("boss.kill.${mobName}", 1)
        meta.add("mob.kill", 1)
        meta.add("mob.kill.${mobName}", 1)
    }

    // ==================== JS脚本可调用方法 ====================

    /**
     * 发送消息给地牢中所有玩家（MiniMessage格式）
     * 可在JS脚本中调用: instance.sendMessageToAllPlayers("<yellow>Hello!</yellow>")
     */
    fun sendMessageToAllPlayers(message: String) {
        val component = MiniMessage.miniMessage().deserialize(message)
        getOnlinePlayers().forEach { it.sendMessage(component) }
    }

    /**
     * 发送Component消息给地牢中所有玩家
     */
    fun sendMessageToAllPlayers(component: Component) {
        getOnlinePlayers().forEach { it.sendMessage(component) }
    }

    /**
     * 发送标题给地牢中所有玩家（MiniMessage格式）
     * @param title 标题文本
     * @param subtitle 副标题文本
     * @param fadeIn 淡入时间（tick）
     * @param stay 停留时间（tick）
     * @param fadeOut 淡出时间（tick）
     */
    fun sendTitleToAllPlayers(title: String, subtitle: String = "", fadeIn: Int = 10, stay: Int = 70, fadeOut: Int = 20) {
        val titleComponent = MiniMessage.miniMessage().deserialize(title)
        val subtitleComponent = if (subtitle.isNotEmpty()) MiniMessage.miniMessage().deserialize(subtitle) else Component.empty()
        getOnlinePlayers().forEach { player ->
            player.showTitle(Title.title(titleComponent, subtitleComponent,
                Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))))
        }
    }

    /**
     * 播放声音给地牢中所有玩家
     * @param sound 声音名称（如 "entity_experience_orb_pickup"）
     * @param volume 音量
     * @param pitch 音调
     */
    fun broadcastSound(sound: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val soundObj = try { Sound.valueOf(sound.uppercase()) } catch (e: Exception) { return }
        getOnlinePlayers().forEach { it.playSound(it.location, soundObj, volume, pitch) }
    }

    /**
     * 传送所有在线地牢玩家到出生点
     */
    fun teleportAllToSpawn() {
        getOnlinePlayers().forEach { it.teleport(spawnLocation) }
    }

    /**
     * 踢出指定玩家（通过玩家名）
     * 管理员操作，跳过 onLeave 条件检查
     * @return 是否成功踢出
     */
    fun kickPlayer(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        return forceRemovePlayer(player)
    }

    /**
     * 获取地牢中玩家总数
     */
    fun getPlayerCount(): Int = players.size

    /**
     * 获取存活的玩家数量（在线且未死亡）
     */
    fun getAlivePlayerCount(): Int = players.count { it !in deadPlayers && Bukkit.getPlayer(it) != null }

    /**
     * 获取死亡的玩家数量
     */
    fun getDeadPlayerCount(): Int = deadPlayers.size

    /**
     * 获取所有在线玩家的名称列表
     */
    fun getOnlinePlayerNames(): List<String> = getOnlinePlayers().map { it.name }

    /**
     * 获取当前状态的名称（PREPARING / ACTIVE / COMPLETED / FAILED）
     */
    fun getStateName(): String = state.name

    /**
     * 获取地牢模板（通过模板名称查找）
     */
    fun getTemplate(): DungeonTemplate? = KAngelDungeon.dungeonTemplates[templateName]

    // ==================== JS脚本可调用：脚本系统 ====================

    /**
     * 执行指定名称的地牢脚本（依次执行 onRun → onPost）
     * JS: instance.runScript("sample")
     * @param scriptName 脚本名称
     * @return 是否找到并执行了脚本
     */
    fun runScript(scriptName: String): Boolean {
        val scripts = KAngelDungeon.dungeonScripts[templateName] ?: return false
        val script = scripts[scriptName] ?: return false
        val data = defaultData + mapOf("instance" to this, "template" to getTemplate(), "scriptName" to scriptName)

        script.onRun?.let { GraalJsUtil.cachedEval(it, data) }
        script.onPost?.let { GraalJsUtil.cachedEval(it, data) }
        return true
    }

    /**
     * 执行当前地牢模板的所有脚本
     * JS: instance.runAllScripts()
     */
    fun runAllScripts() {
        val scripts = KAngelDungeon.dungeonScripts[templateName] ?: return
        val data = defaultData + mapOf("instance" to this, "template" to getTemplate())
        for ((_, script) in scripts) {
            script.onRun?.let { GraalJsUtil.cachedEval(it, data) }
            script.onPost?.let { GraalJsUtil.cachedEval(it, data) }
        }
    }

    // ==================== JS脚本可调用：目标选择器 ====================

    /**
     * 使用目标选择器筛选玩家
     * JS: instance.selectTargets("@all{health>10}")
     * @param selector 目标选择器字符串，如 "@all{papi:player_health>=30&&distance:1,2,3=1..6}"
     * @return 匹配的玩家列表
     */
    fun selectTargets(selector: String): List<Player> {
        return TargetSelectorHelper.parseLine(this, selector)
    }

    /**
     * 使用目标选择器筛选后取第一个玩家
     * JS: var target = instance.selectFirstTarget("@all{health>10}")
     * @return 匹配的第一个玩家，无匹配返回 null
     */
    fun selectFirstTarget(selector: String): Player? {
        return selectTargets(selector).firstOrNull()
    }

    /**
     * 使用目标选择器筛选后随机取一个玩家
     * JS: var target = instance.selectRandomTarget("@all{level>=10}")
     * @return 随机匹配的玩家，无匹配返回 null
     */
    fun selectRandomTarget(selector: String): Player? {
        val targets = selectTargets(selector)
        return if (targets.isEmpty()) null else targets.random()
    }

    /**
     * 获取目标选择器匹配的玩家数量
     * JS: var count = instance.getTargetCount("@all{health>10}")
     */
    fun getTargetCount(selector: String): Int {
        return selectTargets(selector).size
    }

    /**
     * 在地牢中以某点为中心按半径过滤目标选择器结果
     * JS: var nearby = instance.selectTargetsInRadius("@all{health>10}", 100.0, 64.0, 200.0, 15.0)
     * @param selector 目标选择器
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param centerZ 中心Z坐标
     * @param radius 筛选半径
     * @return 指定半径内匹配的玩家列表
     */
    fun selectTargetsInRadius(selector: String, centerX: Double, centerY: Double, centerZ: Double, radius: Double): List<Player> {
        val w = world ?: return emptyList()
        val center = Location(w, centerX, centerY, centerZ)
        return selectTargets(selector).filter { it.location.distance(center) <= radius }
    }

    /**
     * 获取所有在线玩家（getOnlinePlayers 的简洁别名）
     * JS: var all = instance.getAllPlayers()
     */
    fun getAllPlayers(): List<Player> = getOnlinePlayers()

    // ==================== JS脚本可调用：玩家管理 ====================

    /**
     * 发送ActionBar给地牢中所有玩家（MiniMessage格式）
     * JS: instance.sendActionBarToAllPlayers("<yellow>Hello!</yellow>")
     */
    fun sendActionBarToAllPlayers(message: String) {
        val component = MiniMessage.miniMessage().deserialize(message)
        getOnlinePlayers().forEach { it.sendActionBar(component) }
    }

    /**
     * 清空所有在线玩家的背包
     * JS: instance.clearAllPlayersInventory()
     */
    fun clearAllPlayersInventory() {
        getOnlinePlayers().forEach { it.inventory.clear() }
    }

    /**
     * 设置所有在线玩家的生命值
     * JS: instance.setAllPlayersHealth(20.0)
     */
    fun setAllPlayersHealth(health: Double) {
        getOnlinePlayers().forEach { it.health = health.coerceAtMost(it.maxHealth) }
    }

    /**
     * 设置所有在线玩家的饱食度
     * JS: instance.setAllPlayersFood(20)
     */
    fun setAllPlayersFood(food: Int) {
        getOnlinePlayers().forEach { it.foodLevel = food }
    }

    /**
     * 设置所有在线玩家的经验等级
     * JS: instance.setAllPlayersLevel(10)
     */
    fun setAllPlayersLevel(level: Int) {
        getOnlinePlayers().forEach {
            it.level = level
            it.exp = 0.0f
        }
    }

    /**
     * 设置所有在线玩家的游戏模式
     * JS: instance.setAllPlayersGameMode("creative")
     */
    fun setAllPlayersGameMode(gamemode: String) {
        val gm = try { GameMode.valueOf(gamemode.uppercase()) } catch (e: Exception) { return }
        getOnlinePlayers().forEach { it.gameMode = gm }
    }

    /**
     * 治疗所有在线玩家（满血+满饱食+灭火）
     * JS: instance.healAllPlayers()
     */
    fun healAllPlayers() {
        getOnlinePlayers().forEach {
            it.health = it.maxHealth
            it.foodLevel = 20
            it.saturation = 5.0f
            it.fireTicks = 0
        }
    }

    /**
     * 给所有在线玩家物品
     * JS: var item = Bukkit.getItemFactory().createItemStack("DIAMOND_SWORD");
     *     instance.giveItemToAllPlayers(item, 1)
     */
    fun giveItemToAllPlayers(itemStack: org.bukkit.inventory.ItemStack) {
        getOnlinePlayers().forEach { it.inventory.addItem(itemStack.clone()) }
    }

    /**
     * 给指定玩家打开一个Kit（奖励包）
     *
     * 自动检测模式:
     * - weight模式: 从奖励池按权重抽取 min_rewards ~ max_rewards 个
     * - chance模式: 每个奖励独立按 chance% 概率判定
     *
     * JS: instance.openKit("reward_chest", player)
     *     instance.openKit("completion_rewards", player)
     *
     * @param kitName Kit的名称，对应 kit/ 目录下配置的ID
     * @param player 目标玩家
     * @return 是否成功找到并执行了Kit
     */
    fun openKit(kitName: String, player: Player): Boolean {
        val kit = KAngelDungeon.kitConfigs[kitName]
            ?: KAngelDungeon.dungeonKitConfigs[templateName]?.get(kitName)
            ?: return false

        val rewards = if (kit.isChanceMode) {
            io.github.zzzyyylllty.kangeldungeon.util.kit.KitManager.rewardsByChance(kit.rewards)
        } else {
            val safeMax = kit.maxRewards.coerceAtLeast(kit.minRewards)
            val count = kotlin.random.Random.nextInt(kit.minRewards, safeMax + 1)
            io.github.zzzyyylllty.kangeldungeon.util.kit.KitManager.rollRewards(kit.rewards, count)
        }

        return executeKitForPlayer(kit, kitName, rewards, player)
    }

    /**
     * 给所有在线玩家打开一个Kit
     * JS: instance.openKitToAll("reward_chest")
     *
     * @param kitName Kit的名称
     * @return 是否成功找到Kit
     */
    fun openKitToAll(kitName: String): Boolean {
        val kit = KAngelDungeon.kitConfigs[kitName]
            ?: KAngelDungeon.dungeonKitConfigs[templateName]?.get(kitName)
            ?: return false
        val players = getOnlinePlayers()

        if (kit.isChanceMode) {
            for (player in players) {
                openKit(kitName, player)
            }
        } else {
            val safeMax = kit.maxRewards.coerceAtLeast(kit.minRewards)
            val count = kotlin.random.Random.nextInt(kit.minRewards, safeMax + 1)
            val rewards = io.github.zzzyyylllty.kangeldungeon.util.kit.KitManager.rollRewards(kit.rewards, count)
            for (player in players) {
                executeKitForPlayer(kit, kitName, rewards, player)
            }
        }
        return true
    }

    /**
     * 对单个玩家执行Kit奖励（内部方法，包含冷却/条件/事件/消息/广播等完整流程）
     * @param kit Kit配置
     * @param kitName Kit名称
     * @param rewards 已抽取的奖励列表（由调用方提前计算好）
     * @param player 目标玩家
     * @return 是否实际发放了奖励
     */
    private fun executeKitForPlayer(kit: KitConfig, kitName: String, rewards: List<KitReward>, player: Player): Boolean {
        // 1. 冷却检查
        if (kit.cooldown > 0) {
            val remaining = io.github.zzzyyylllty.kangeldungeon.util.kit.KitManager.checkCooldown(player, templateName, kitName)
            if (remaining != null) {
                kit.messages?.get("cooldown")?.let { msg ->
                    val parsed = msg.replace("%remaining%", (remaining / 1000).toString())
                    player.sendMessage(MiniMessage.miniMessage().deserialize(parsed))
                }
                return false
            }
        }

        // 2. 条件检查
        if (!kit.conditions.isNullOrEmpty()) {
            val vars = mapOf<String, Any?>("player" to player, "instance" to this)
            if (!kit.conditions.evalKetherBoolean(player, vars, all = true)) {
                kit.messages?.get("condition_fail")?.let { msg ->
                    player.sendMessage(MiniMessage.miniMessage().deserialize(msg))
                }
                return false
            }
        }

        // 3. Pre事件
        val preEvent = io.github.zzzyyylllty.kangeldungeon.event.KitOpenPreEvent(this, kitName, player, kit)
        preEvent.call()
        if (preEvent.isCancelled) return false

        // 4. 执行奖励
        var anySuccess = false
        for (reward in rewards) {
            val ok = io.github.zzzyyylllty.kangeldungeon.util.kit.KitManager.executeReward(reward, player, this)
            if (ok) anySuccess = true
        }

        // 5. Post事件
        io.github.zzzyyylllty.kangeldungeon.event.KitOpenPostEvent(this, kitName, player, kit, rewards).call()

        // 6. 应用冷却（仅在至少一个奖励成功执行时才设置冷却）
        if (anySuccess && kit.cooldown > 0) {
            io.github.zzzyyylllty.kangeldungeon.util.kit.KitManager.applyCooldown(player, templateName, kitName, kit.cooldown)
        }

        // 7. Kit打开消息
        kit.messages?.get("open")?.let { msg ->
            player.sendMessage(MiniMessage.miniMessage().deserialize(msg))
        }

        // 8. 全服广播
        kit.broadcastMessage?.let { msg ->
            val parsed = msg.replace("%player%", player.name)
                .replace("%kit%", kit.displayName ?: kitName)
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(parsed))
        }

        // 9. Meta统计 & 任务触发
        meta.add("kit.open", 1)
        meta.add("kit.open.$kitName", 1)
        TaskManager.triggerTasks(this, "KIT_OPEN", mapOf(
            "kitName" to kitName,
            "player" to player,
            "rewards" to rewards
        ))

        return true
    }

    /**
     * 给所有在线玩家经验值
     * JS: instance.giveExperienceToAllPlayers(100)
     * @param amount 经验值数量
     */
    fun giveExperienceToAllPlayers(amount: Int) {
        getOnlinePlayers().forEach { it.giveExp(amount) }
    }

    /**
     * 设置所有在线玩家的移动速度
     * JS: instance.setAllPlayersWalkSpeed(0.2)
     * @param speed 速度值（默认 0.2）
     */
    fun setAllPlayersWalkSpeed(speed: Float) {
        getOnlinePlayers().forEach { it.walkSpeed = speed.coerceIn(0.0f, 1.0f) }
    }

    /**
     * 设置所有在线玩家的飞行速度
     * JS: instance.setAllPlayersFlySpeed(0.1)
     * @param speed 速度值（默认 0.1）
     */
    fun setAllPlayersFlySpeed(speed: Float) {
        getOnlinePlayers().forEach { it.flySpeed = speed.coerceIn(0.0f, 1.0f) }
    }

    /**
     * 设置所有在线玩家是否允许飞行
     * JS: instance.setAllPlayersAllowFlight(true)
     */
    fun setAllPlayersAllowFlight(allow: Boolean) {
        getOnlinePlayers().forEach { it.allowFlight = allow }
    }

    /**
     * 设置所有在线玩家的饱和值
     * JS: instance.setAllPlayersSaturation(20.0)
     */
    fun setAllPlayersSaturation(saturation: Float) {
        getOnlinePlayers().forEach { it.saturation = saturation.coerceIn(0.0f, 20.0f) }
    }

    /**
     * 设置所有在线玩家的最大生命值
     * JS: instance.setAllPlayersMaxHealth(40.0)
     */
    fun setAllPlayersMaxHealth(maxHealth: Double) {
        getOnlinePlayers().forEach { player ->
            val attrInstance = player.getAttribute(Attribute.MAX_HEALTH)
            if (attrInstance != null) {
                attrInstance.baseValue = maxHealth
            } else {
                player.maxHealth = maxHealth
            }
        }
    }

    // ==================== JS脚本可调用：药效管理 ====================

    /**
     * 给所有在线玩家添加药水效果
     * JS: instance.applyPotionEffectToAllPlayers("SPEED", 600, 1)
     * @param type 效果类型（如 SPEED, JUMP_BOOST, INVISIBILITY, REGENERATION）
     * @param duration 持续时间（秒）
     * @param amplifier 等级（0=I, 1=II）
     */
    fun applyPotionEffectToAllPlayers(type: String, duration: Int = 30, amplifier: Int = 0) {
        val effectType = getPotionEffectType(type) ?: return
        getOnlinePlayers().forEach { player ->
            player.addPotionEffect(PotionEffect(effectType, duration * 20, amplifier, true, true, true))
        }
    }

    /**
     * 移除所有在线玩家的指定药水效果
     * JS: instance.removePotionEffectFromAllPlayers("SPEED")
     * @param type 效果类型
     */
    fun removePotionEffectFromAllPlayers(type: String) {
        val effectType = getPotionEffectType(type) ?: return
        getOnlinePlayers().forEach { it.removePotionEffect(effectType) }
    }

    /**
     * 给指定玩家添加药水效果
     * JS: instance.applyPotionEffectToPlayer("Notch", "INVISIBILITY", 100, 0)
     * @param playerName 玩家名称
     * @param type 效果类型
     * @param duration 持续时间（秒）
     * @param amplifier 等级
     * @return 玩家是否在线
     */
    fun applyPotionEffectToPlayer(playerName: String, type: String, duration: Int = 30, amplifier: Int = 0): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        val effectType = getPotionEffectType(type) ?: return false
        player.addPotionEffect(PotionEffect(effectType, duration * 20, amplifier, true, true, true))
        return true
    }

    /**
     * 移除指定玩家的指定药水效果
     * JS: instance.removePotionEffectFromPlayer("Notch", "JUMP_BOOST")
     * @param playerName 玩家名称
     * @param type 效果类型
     * @return 玩家是否在线
     */
    fun removePotionEffectFromPlayer(playerName: String, type: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        val effectType = getPotionEffectType(type) ?: return false
        player.removePotionEffect(effectType)
        return true
    }

    // ==================== 工具方法 ====================

    @Suppress("DEPRECATION")
    private fun getPotionEffectType(type: String): PotionEffectType? {
        return PotionEffectType.getByName(type)
    }

    // ==================== JS脚本可调用：世界操作 ====================

    /**
     * 设置地牢世界时间（tick）
     * JS: instance.setWorldTime(1000)
     */
    fun setWorldTime(time: Long) {
        world?.time = time
    }

    /**
     * 设置地牢世界天气（是否下雨）
     * JS: instance.setWorldStorm(false)
     */
    fun setWorldStorm(storm: Boolean) {
        world?.setStorm(storm)
    }

    /**
     * 设置地牢世界雷暴
     * JS: instance.setWorldThundering(false)
     */
    fun setWorldThundering(thundering: Boolean) {
        world?.setThundering(thundering)
    }

    /**
     * 获取地牢世界当前时间（tick）
     * JS: var time = instance.getWorldTime()
     */
    fun getWorldTime(): Long {
        return world?.time ?: 0L
    }

    /**
     * 获取地牢世界是否正在下雨
     * JS: var storm = instance.hasStorm()
     */
    fun hasStorm(): Boolean {
        return world?.hasStorm() ?: false
    }

    /**
     * 获取地牢世界是否在雷暴
     * JS: var thunder = instance.isThundering()
     */
    fun isThundering(): Boolean {
        return world?.isThundering ?: false
    }

    /**
     * 设置地牢世界难度
     * JS: instance.setWorldDifficulty("hard")
     * @param difficulty 难度名称（peaceful / easy / normal / hard）
     */
    fun setWorldDifficulty(difficulty: String) {
        val diff = try { org.bukkit.Difficulty.valueOf(difficulty.uppercase()) } catch (e: Exception) { return }
        world?.difficulty = diff
    }

    /**
     * 设置地牢世界游戏规则
     * JS: instance.setGameRule("doDaylightCycle", "false")
     * @param rule 游戏规则名称
     * @param value 规则值
     */
    @Suppress("UNCHECKED_CAST")
    fun setGameRule(rule: String, value: String) {
        val gameRule = GameRule.getByName(rule) ?: return
        val type = gameRule.type
        val typedValue: Any = when {
            type == Boolean::class.java -> value.toBoolean()
            type == Int::class.java -> value.toInt()
            else -> return
        }
        @Suppress("UNCHECKED_CAST")
        world?.setGameRule(gameRule as GameRule<Any>, typedValue)
    }

    /**
     * 设置地牢世界边界
     * JS: instance.setWorldBorder(0.0, 0.0, 256.0)
     * @param centerX 边界中心X
     * @param centerZ 边界中心Z
     * @param size 边界大小（边长）
     */
    fun setWorldBorder(centerX: Double, centerZ: Double, size: Double) {
        val w = world ?: return
        val border = w.worldBorder
        border.center = Location(w, centerX, 0.0, centerZ)
        border.size = size
    }

    // ==================== JS脚本可调用：视觉效果 ====================

    /**
     * 在指定位置生成闪电效果（仅视觉，无伤害）
     * JS: instance.strikeLightning(100.0, 64.0, 200.0)
     */
    fun strikeLightning(x: Double, y: Double, z: Double) {
        val w = world ?: return
        w.strikeLightningEffect(Location(w, x, y, z))
    }

    /**
     * 在指定位置生成粒子效果
     * JS: instance.spawnParticle("flame", 100.0, 64.0, 200.0, 30)
     */
    fun spawnParticle(particle: String, x: Double, y: Double, z: Double, count: Int = 10, offsetX: Double = 0.5, offsetY: Double = 0.5, offsetZ: Double = 0.5, speed: Double = 0.1) {
        val p = try { Particle.valueOf(particle.uppercase()) } catch (e: Exception) { return }
        val w = world ?: return
        w.spawnParticle(p, Location(w, x, y, z), count, offsetX, offsetY, offsetZ, speed)
    }

    /**
     * 在两点之间生成粒子线
     * JS: instance.spawnParticleLine("flame", 0.0, 64.0, 0.0, 100.0, 64.0, 100.0, 50)
     * @param particle 粒子类型
     * @param x1,y1,z1 起点坐标
     * @param x2,y2,z2 终点坐标
     * @param count 粒子总数
     */
    fun spawnParticleLine(particle: String, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, count: Int = 30) {
        val p = try { Particle.valueOf(particle.uppercase()) } catch (e: Exception) { return }
        val w = world ?: return
        val start = Location(w, x1, y1, z1)
        val end = Location(w, x2, y2, z2)
        val step = 1.0 / count
        for (t in 0..count) {
            val ratio = t * step
            val loc = start.clone().add(end.clone().subtract(start).multiply(ratio))
            w.spawnParticle(p, loc, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }

    /**
     * 在指定坐标播放声音
     * JS: instance.playSoundAt("entity_experience_orb_pickup", 100.0, 64.0, 100.0, 1.0, 1.0)
     * @param sound 声音名称
     * @param x,y,z 坐标
     * @param volume 音量
     * @param pitch 音调
     */
    fun playSoundAt(sound: String, x: Double, y: Double, z: Double, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val soundObj = try { Sound.valueOf(sound.uppercase()) } catch (e: Exception) { return }
        val w = world ?: return
        w.playSound(Location(w, x, y, z), soundObj, volume, pitch)
    }

    /**
     * 给指定玩家播放声音
     * JS: instance.playSoundToPlayer("Notch", "entity_experience_orb_pickup", 1.0, 1.0)
     * @param playerName 玩家名称
     * @param sound 声音名称
     * @param volume 音量
     * @param pitch 音调
     * @return 玩家是否在线
     */
    fun playSoundToPlayer(playerName: String, sound: String, volume: Float = 1.0f, pitch: Float = 1.0f): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        val soundObj = try { Sound.valueOf(sound.uppercase()) } catch (e: Exception) { return false }
        player.playSound(player.location, soundObj, volume, pitch)
        return true
    }

    // ==================== JS脚本可调用：命令执行 ====================

    /**
     * 以控制台执行命令
     * JS: instance.executeCommand("give @a diamond 1")
     */
    fun executeCommand(command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }

    /**
     * 以指定玩家身份执行命令
     * JS: instance.executeCommandAsPlayer("Notch", "gamemode creative")
     * @return 玩家是否在线
     */
    fun executeCommandAsPlayer(playerName: String, command: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        return Bukkit.dispatchCommand(player, command)
    }

    // ==================== JS脚本可调用：单玩家操作 ====================

    /**
     * 发送消息给指定玩家（MiniMessage格式）
     * JS: instance.sendMessageToPlayer("Notch", "<red>You are the leader!</red>")
     * @return 玩家是否在线
     */
    fun sendMessageToPlayer(playerName: String, message: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.sendMessage(MiniMessage.miniMessage().deserialize(message))
        return true
    }

    /**
     * 检查指定玩家是否已死亡
     * JS: instance.isPlayerDead("Notch")
     * @return Boolean 或 null（玩家不在线）
     */
    fun isPlayerDead(playerName: String): Boolean? {
        val player = Bukkit.getPlayerExact(playerName) ?: return null
        return player.uniqueId in deadPlayers
    }

    /**
     * 复活指定玩家（传送回出生点、满血、移除死亡状态）
     * JS: instance.respawnPlayer("Notch")
     * @return 是否成功复活
     */
    fun respawnPlayer(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        if (player.uniqueId !in deadPlayers) return false

        // 检查复活次数限制
        val config = getTemplate()?.gameplayGeneral?.death ?: DeathConfig()
        if (config.maxRespawns > 0) {
            val count = getPlayerRespawnCount(playerName)
            if (count >= config.maxRespawns) return false
        }

        deadPlayers.remove(player.uniqueId)
        meta.add("player.respawn.${playerName}", 1)

        // 退出旁观模式
        if (player.gameMode == GameMode.SPECTATOR) {
            player.gameMode = GameMode.ADVENTURE
        }

        // 复活处理
        if (config.respawnAtSpawn) {
            player.teleport(spawnLocation)
        }
        if (!config.keepInventoryOnRespawn && getTemplate()?.keepInventoryConfig?.enabled != true) {
            // 默认清除：InventoryHandler 会处理 keepInventory，这里只处理特殊情况
        }
        player.health = player.maxHealth
        player.foodLevel = 20
        player.saturation = 5.0f
        player.fireTicks = 0
        player.activePotionEffects.toList().forEach { player.removePotionEffect(it.type) }

        DungeonPlayerRespawnEvent(this, player).call()
        return true
    }

    /**
     * 获取玩家已使用的复活次数（通过 meta 追踪）
     * JS: var count = instance.getPlayerRespawnCount("Notch")
     */
    fun getPlayerRespawnCount(playerName: String): Int {
        return meta.getAsInt("player.respawn.$playerName") ?: 0
    }

    /**
     * 复活所有已死亡的在线玩家（传送回出生点、满血、移除死亡状态）
     * JS: instance.respawnAllDeadPlayers()
     */
    fun respawnAllDeadPlayers() {
        val toRespawn = deadPlayers.mapNotNull { Bukkit.getPlayer(it)?.name }.toList()
        for (name in toRespawn) {
            respawnPlayer(name)
        }
    }

    /**
     * 设置玩家为旁观模式（死后自由飞行观看）
     * JS: instance.setSpectateMode("Notch")
     */
    fun setSpectateMode(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.gameMode = GameMode.SPECTATOR
        return true
    }

    /**
     * 强制玩家附身观看另一个玩家（旁观模式 + 传送并锁定视角）
     * JS: instance.possessPlayer("dead_player", "alive_target")
     * @param targetName 附身目标玩家名称，null 则随机选一个存活队友
     */
    fun possessPlayer(playerName: String, targetName: String? = null): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        val target = if (targetName != null) {
            Bukkit.getPlayerExact(targetName)
        } else {
            getOnlinePlayers().filter { it.uniqueId !in deadPlayers && it.uniqueId != player.uniqueId }.randomOrNull()
        } ?: return false

        player.gameMode = GameMode.SPECTATOR
        player.spectatorTarget = target
        return true
    }

    /**
     * 清除指定玩家身上的药水效果
     * JS: instance.clearPlayerEffects("Notch")
     * @return 玩家是否在线
     */
    fun clearPlayerEffects(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.activePotionEffects.toList().forEach { player.removePotionEffect(it.type) }
        return true
    }

    /**
     * 清除所有在线玩家的药水效果
     * JS: instance.clearAllPlayersEffects()
     */
    fun clearAllPlayersEffects() {
        getOnlinePlayers().forEach { player ->
            player.activePotionEffects.toList().forEach { player.removePotionEffect(it.type) }
        }
    }

    // ==================== JS脚本可调用：计分板标签 ====================

    /**
     * 给指定玩家添加计分板标签
     * JS: instance.addScoreboardTag("Notch", "boss_fight")
     * @param playerName 玩家名称
     * @param tag 标签名
     * @return 玩家是否在线
     */
    fun addScoreboardTag(playerName: String, tag: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.addScoreboardTag(tag)
        return true
    }

    /**
     * 移除指定玩家的计分板标签
     * JS: instance.removeScoreboardTag("Notch", "boss_fight")
     * @param playerName 玩家名称
     * @param tag 标签名
     * @return 玩家是否在线
     */
    fun removeScoreboardTag(playerName: String, tag: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.removeScoreboardTag(tag)
        return true
    }

    /**
     * 检查指定玩家是否有某计分板标签
     * JS: var has = instance.hasScoreboardTag("Notch", "boss_fight")
     * @return Boolean 或 null（玩家不在线）
     */
    fun hasScoreboardTag(playerName: String, tag: String): Boolean? {
        val player = Bukkit.getPlayerExact(playerName) ?: return null
        return player.scoreboardTags.contains(tag)
    }

    /**
     * 给所有在线玩家添加计分板标签
     * JS: instance.addScoreboardTagToAllPlayers("wave_1")
     */
    fun addScoreboardTagToAllPlayers(tag: String) {
        getOnlinePlayers().forEach { it.addScoreboardTag(tag) }
    }

    /**
     * 移除所有在线玩家的计分板标签
     * JS: instance.removeScoreboardTagFromAllPlayers("wave_1")
     */
    fun removeScoreboardTagFromAllPlayers(tag: String) {
        getOnlinePlayers().forEach { it.removeScoreboardTag(tag) }
    }

    // ==================== JS脚本可调用：单玩家状态管理 ====================

    /**
     * 设置指定玩家的移动速度
     * JS: instance.setPlayerWalkSpeed("Notch", 0.2)
     * @return 玩家是否在线
     */
    fun setPlayerWalkSpeed(playerName: String, speed: Float): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.walkSpeed = speed.coerceIn(0.0f, 1.0f)
        return true
    }

    /**
     * 设置指定玩家的飞行速度
     * JS: instance.setPlayerFlySpeed("Notch", 0.1)
     * @return 玩家是否在线
     */
    fun setPlayerFlySpeed(playerName: String, speed: Float): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.flySpeed = speed.coerceIn(0.0f, 1.0f)
        return true
    }

    /**
     * 设置指定玩家是否允许飞行
     * JS: instance.setPlayerAllowFlight("Notch", true)
     * @return 玩家是否在线
     */
    fun setPlayerAllowFlight(playerName: String, allow: Boolean): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.allowFlight = allow
        return true
    }

    /**
     * 设置指定玩家的游戏模式
     * JS: instance.setPlayerGameMode("Notch", "creative")
     * @return 玩家是否在线
     */
    fun setPlayerGameMode(playerName: String, gamemode: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        val gm = try { GameMode.valueOf(gamemode.uppercase()) } catch (e: Exception) { return false }
        player.gameMode = gm
        return true
    }

    /**
     * 设置指定玩家的经验等级
     * JS: instance.setPlayerLevel("Notch", 10)
     * @return 玩家是否在线
     */
    fun setPlayerLevel(playerName: String, level: Int): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.level = level
        player.exp = 0.0f
        return true
    }

    /**
     * 设置指定玩家的生命值
     * JS: instance.setPlayerHealth("Notch", 20.0)
     * @return 玩家是否在线
     */
    fun setPlayerHealth(playerName: String, health: Double): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.health = health
        return true
    }

    /**
     * 设置指定玩家的饱食度
     * JS: instance.setPlayerFood("Notch", 20)
     * @return 玩家是否在线
     */
    fun setPlayerFood(playerName: String, food: Int): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.foodLevel = food
        return true
    }

    /**
     * 设置指定玩家的最大生命值
     * JS: instance.setPlayerMaxHealth("Notch", 40.0)
     * @return 玩家是否在线
     */
    fun setPlayerMaxHealth(playerName: String, maxHealth: Double): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.maxHealth = maxHealth
        return true
    }

    /**
     * 治疗指定玩家（满血+满饱食+灭火）
     * JS: instance.healPlayer("Notch")
     * @return 玩家是否在线
     */
    fun healPlayer(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.health = player.maxHealth
        player.foodLevel = 20
        player.saturation = 5.0f
        player.fireTicks = 0
        return true
    }

    // ==================== JS脚本可调用：障碍物管理 ====================

    /**
     * 获取本 dungeons 的障碍物配置映射
     */
    private fun getDungeonObstacleConfigs(): Map<String, ObstacleConfig> {
        return KAngelDungeon.dungeonObstacleConfigs[templateName] ?: emptyMap()
    }

    /**
     * 按 ID 查找障碍物配置（优先查本 dungeons，dungeon 回退到全局）
     */
    private fun findObstacleConfig(obstacleId: String): ObstacleConfig? {
        return getDungeonObstacleConfigs()[obstacleId]
            ?: KAngelDungeon.obstacleConfigs[obstacleId]
    }

    /**
     * 准备障碍物（预先保存方块状态）
     * JS: instance.prepareObstacle("iron_bars_gate")
     * @return 是否成功准备
     */
    fun prepareObstacle(obstacleId: String): Boolean {
        val config = findObstacleConfig(obstacleId) ?: return false
        return ObstacleManager.prepareObstacle(this, config)
    }

    /**
     * 激活障碍物（关闭栅栏，阻挡玩家）
     * JS: instance.activateObstacle("iron_bars_gate")
     * @return 是否成功激活
     */
    fun activateObstacle(obstacleId: String): Boolean {
        val config = findObstacleConfig(obstacleId) ?: return false
        return ObstacleManager.activateObstacle(this, config)
    }

    /**
     * 打开障碍物（移除栅栏，恢复通行）
     * JS: instance.openObstacle("iron_bars_gate")
     * @return 是否成功打开
     */
    fun openObstacle(obstacleId: String): Boolean {
        val config = findObstacleConfig(obstacleId) ?: return false
        return ObstacleManager.openObstacle(this, config)
    }

    /**
     * 强制打开障碍物（跳过激活状态检查）
     * JS: instance.openObstacleForce("iron_bars_gate")
     */
    fun openObstacleForce(obstacleId: String): Boolean {
        val config = findObstacleConfig(obstacleId) ?: return false
        return ObstacleManager.openObstacleForce(this, config)
    }

    /**
     * 恢复已保存的方块状态（清理）
     * JS: instance.restoreObstacleBlocks()
     */
    fun restoreObstacleBlocks() {
        ObstacleManager.restoreBlocks(this)
    }

    /**
     * 获取本 dungeons 所有障碍物配置（地牢 + 全局合并）
     * JS: instance.getObstacleConfigs()
     * @return 本 dungeons 优先的障碍物配置映射
     */
    fun getObstacleConfigs(): Map<String, ObstacleConfig> {
        return getDungeonObstacleConfigs() + KAngelDungeon.obstacleConfigs
    }

    // ==================== JS脚本可调用：怪物管理 ====================

    /**
     * 获取本 dungeons 的怪物配置映射
     */
    private fun getDungeonMonsterConfigs(): Map<String, MonsterConfig> {
        return KAngelDungeon.dungeonMonsterConfigs[templateName] ?: emptyMap()
    }

    /**
     * 按 ID 查找怪物配置（优先查本 dungeons，再回退到全局）
     */
    private fun findMonsterConfig(monsterId: String): MonsterConfig? {
        return getDungeonMonsterConfigs()[monsterId]
            ?: KAngelDungeon.monsterConfigs[monsterId]
    }

    /**
     * 生成指定 ID 的怪物组
     * JS: instance.spawnMonsters("zombies")
     * @return 是否成功生成
     */
    fun spawnMonsters(monsterId: String): Boolean {
        val config = findMonsterConfig(monsterId) ?: return false
        return MonsterManager.spawnMonsters(this, config) != null
    }

    /**
     * 获取本 dungeons 所有怪物配置（地牢 + 全局合并）
     * JS: instance.getMonsterConfigs()
     * @return 本 dungeons 优先的怪物配置映射
     */
    fun getMonsterConfigs(): Map<String, MonsterConfig> {
        return getDungeonMonsterConfigs() + KAngelDungeon.monsterConfigs
    }

    /**
     * 获取所有活跃怪物组实例
     * JS: instance.getMonsterInstances()
     */
    fun getMonsterInstances(): Map<String, MonsterInstance> {
        return MonsterManager.getMonsterInstances(this)
    }

    /**
     * 设置怪物组激活状态
     * JS: instance.setMonsterActive("zombies", true)
     */
    fun setMonsterActive(monsterId: String, active: Boolean) {
        MonsterManager.setMonsterActive(this, monsterId, active)
    }

    /**
     * 设置怪物组重生冷却（tick），-1 恢复 config 默认
     * JS: instance.setMonsterCooldown("zombies", 200)
     */
    fun setMonsterCooldown(monsterId: String, cooldownTicks: Long) {
        MonsterManager.setMonsterCooldown(this, monsterId, cooldownTicks)
    }

    /**
     * 设置怪物组最小激活距离（方块），null 恢复 config 默认
     * JS: instance.setMonsterActivationRangeMin("zombies", 5.0)
     */
    fun setMonsterActivationRangeMin(monsterId: String, value: Double?) {
        MonsterManager.setMonsterActivationRangeMin(this, monsterId, value)
    }

    /**
     * 设置怪物组最大激活距离（方块），null 恢复 config 默认
     * JS: instance.setMonsterActivationRangeMax("zombies", 30.0)
     */
    fun setMonsterActivationRangeMax(monsterId: String, value: Double?) {
        MonsterManager.setMonsterActivationRangeMax(this, monsterId, value)
    }

    /**
     * 重置怪物组激活距离为 config 默认值
     * JS: instance.resetMonsterActivationRange("zombies")
     */
    fun resetMonsterActivationRange(monsterId: String) {
        MonsterManager.resetMonsterActivationRange(this, monsterId)
    }

    /**
     * 获取当前地牢的活跃怪物实例
     * JS: instance.getActiveMonsters()
     * @return 怪物实例映射 (configId -> MonsterInstance)
     */
    fun getActiveMonsters(): Map<String, MonsterInstance> {
        return MonsterManager.getMonsterInstances(this)
    }

    // ==================== JS脚本可调用：任务管理 ====================

    /**
     * 获取本 dungeons 的任务配置映射
     * JS: instance.getTaskConfigs()
     * @return 任务配置映射 (taskId -> TaskConfig)
     */
    fun getTaskConfigs(): Map<String, TaskConfig> {
        return KAngelDungeon.dungeonTaskConfigs[templateName] ?: emptyMap()
    }

    /**
     * 手动触发指定任务
     * JS: instance.triggerTask("taskId")
     * @param taskId 任务 ID
     * @return 是否找到并执行了任务
     */
    fun triggerTask(taskId: String): Boolean {
        return TaskManager.triggerTask(this, taskId)
    }

    /**
     * 获取任务的已执行次数
     * JS: var count = instance.getTaskExecutionCount("taskId")
     * @param taskId 任务 ID
     * @return 已执行次数
     */
    fun getTaskExecutionCount(taskId: String): Int {
        return TaskManager.getExecutionCount(this, taskId)
    }

    /**
     * 重置任务的已执行次数
     * JS: instance.resetTaskExecutionCount("taskId")
     * @param taskId 任务 ID
     */
    fun resetTaskExecutionCount(taskId: String) {
        TaskManager.resetExecutionCount(this, taskId)
    }

    // ==================== JS脚本可调用：地牢交互管理 ====================

    /**
     * 获取本 dungeons 的交互配置映射
     * JS: instance.getInteractConfigs()
     * @return 交互配置映射
     */
    fun getInteractConfigs(): Map<String, InteractConfig> {
        return KAngelDungeon.dungeonInteractConfigs[templateName] ?: emptyMap()
    }

    /**
     * 触发指定 ID 的交互（执行 onActive / onPost 代理脚本）
     * JS: instance.triggerInteractBtn("button")
     * @param interactId 交互配置 ID
     * @return 是否找到并触发了交互
     */
    fun triggerInteractBtn(interactId: String): Boolean {
        val config = getInteractConfigs()[interactId] ?: return false
        val data = defaultData + mapOf("instance" to this, "template" to getTemplate(), "interact" to config)
        config.agent?.onActive?.let { GraalJsUtil.cachedEval(it, data) }
        config.agent?.onPost?.let { GraalJsUtil.cachedEval(it, data) }
        return true
    }

    // ==================== JS脚本可调用：区域管理 ====================

    /**
     * 获取本 dungeons 的区域配置映射
     * JS: instance.getRegionConfigs()
     * @return 区域配置映射 (regionId -> RegionConfig)
     */
    fun getRegionConfigs(): Map<String, RegionConfig> {
        return KAngelDungeon.dungeonRegionConfigs[templateName] ?: emptyMap()
    }

    /**
     * 检查玩家是否在指定区域中
     * JS: var inside = instance.isPlayerInRegion("Notch", "spawn_zone")
     * @param playerName 玩家名称
     * @param regionId 区域 ID
     * @return 玩家是否在区域内
     */
    fun isPlayerInRegion(playerName: String, regionId: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        return RegionManager.isPlayerInRegion(worldName, player.uniqueId, regionId)
    }

    /**
     * 获取指定区域内的所有在线玩家
     * JS: var players = instance.getPlayersInRegion("boss_arena")
     * @param regionId 区域 ID
     * @return 区域内的玩家列表
     */
    fun getPlayersInRegion(regionId: String): List<Player> {
        return RegionManager.getPlayersInRegion(worldName, regionId)
    }

    /**
     * 获取指定玩家当前所在的所有区域
     * JS: var regions = instance.getPlayerRegions("Notch")
     * @param playerName 玩家名称
     * @return 区域 ID 列表
     */
    fun getPlayerRegions(playerName: String): List<String> {
        val player = Bukkit.getPlayerExact(playerName) ?: return emptyList()
        return RegionManager.getPlayerRegions(worldName, player.uniqueId).toList()
    }

    // ==================== JS脚本可调用：难度查询 ===================

    /**
     * 获取当前难度 ID
     * JS: instance.getDifficulty()
     * @return 难度ID字符串，无难度时返回 null
     */
    fun getDifficulty(): String? = difficultyId

    /**
     * 获取当前难度的配置对象
     * JS: var diff = instance.getDifficultyConfig()
     *     diff.display  -> "Hard"
     *     diff.meta     -> {global: {health_mult: 2.0}}
     * @return DifficultyConfig 或 null（无难度）
     */
    fun getDifficultyConfig(): DifficultyConfig? {
        val id = difficultyId ?: return null
        return KAngelDungeon.dungeonDifficultyConfigs[templateName]?.get(id)
    }

    // ==================== JS脚本可调用：状态快捷查询 ===================

    /**
     * 地牢是否处于准备阶段
     * JS: if (instance.isPreparing()) { ... }
     */
    fun isPreparing(): Boolean = state == DungeonState.PREPARING

    /**
     * 地牢是否处于激活状态（进行中）
     * JS: if (instance.isActive()) { ... }
     */
    fun isActive(): Boolean = state == DungeonState.ACTIVE

    /**
     * 地牢是否已成功完成
     * JS: if (instance.isCompleted()) { ... }
     */
    fun isCompleted(): Boolean = state == DungeonState.COMPLETED

    /**
     * 地牢是否已失败
     * JS: if (instance.isFailed()) { ... }
     */
    fun isFailed(): Boolean = state == DungeonState.FAILED

    /**
     * 地牢是否已结束（完成或失败）
     * JS: if (instance.isFinished()) { ... }
     */
    fun isFinished(): Boolean = state == DungeonState.COMPLETED || state == DungeonState.FAILED

    // ==================== JS脚本可调用：玩家信息查询 ===================

    /**
     * 获取所有玩家的UUID列表（包括离线玩家）
     * JS: var uuids = instance.getAllPlayerUUIDs()
     */
    fun getAllPlayerUUIDs(): List<UUID> = players.toList()

    /**
     * 获取所有死亡玩家的名称列表
     * JS: var dead = instance.getDeadPlayerNames()
     */
    fun getDeadPlayerNames(): List<String> {
        return deadPlayers.mapNotNull { Bukkit.getOfflinePlayer(it).name }
    }

    /**
     * 获取队长名称
     * JS: var leader = instance.getLeaderName()
     */
    fun getLeaderName(): String? {
        return Bukkit.getOfflinePlayer(leaderUUID).name
    }

    /**
     * 检查指定玩家名称是否在地牢中
     * JS: if (instance.isPlayerInDungeon("Notch")) { ... }
     */
    fun isPlayerInDungeon(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        return player.uniqueId in players
    }

    /**
     * 检查指定玩家名称是否已死亡（玩家需在线）
     * JS: if (instance.isPlayerDeadInDungeon("Notch")) { ... }
     */
    fun isPlayerDeadInDungeon(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        return player.uniqueId in deadPlayers
    }

    /**
     * 获取指定玩家的存活状态
     * JS: var status = instance.getPlayerStatus("Notch")
     * @return "alive" / "dead" / "offline" / "not_in_dungeon"
     */
    fun getPlayerStatus(playerName: String): String {
        val player = Bukkit.getPlayerExact(playerName)
        if (player == null) {
            return if (players.any { Bukkit.getOfflinePlayer(it).name == playerName }) "offline" else "not_in_dungeon"
        }
        if (player.uniqueId !in players) return "not_in_dungeon"
        return if (player.uniqueId in deadPlayers) "dead" else "alive"
    }

    // ==================== JS脚本可调用：玩家传送 ===================

    /**
     * 传送指定玩家到地牢出生点
     * JS: instance.teleportPlayerToSpawn("Notch")
     * @return 玩家是否在线
     */
    fun teleportPlayerToSpawn(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.teleport(spawnLocation)
        return true
    }

    /**
     * 传送指定玩家到地牢世界指定坐标
     * JS: instance.teleportPlayerTo("Notch", 100.0, 64.0, 200.0)
     * @return 玩家是否在线且地牢世界存在
     */
    fun teleportPlayerTo(playerName: String, x: Double, y: Double, z: Double): Boolean {
        val w = world ?: return false
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.teleport(Location(w, x, y, z))
        return true
    }

    /**
     * 传送指定玩家到地牢世界指定坐标（含朝向）
     * JS: instance.teleportPlayerTo("Notch", 100.0, 64.0, 200.0, 90.0, 0.0)
     * @return 玩家是否在线且地牢世界存在
     */
    fun teleportPlayerTo(playerName: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        val w = world ?: return false
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.teleport(Location(w, x, y, z, yaw, pitch))
        return true
    }

    /**
     * 传送所有在线玩家到地牢世界指定坐标
     * JS: instance.teleportAllTo(100.0, 64.0, 200.0)
     */
    fun teleportAllTo(x: Double, y: Double, z: Double) {
        val w = world ?: return
        val loc = Location(w, x, y, z)
        getOnlinePlayers().forEach { it.teleport(loc) }
    }

    // ==================== JS脚本可调用：视觉效果增强 ===================

    /**
     * 在指定位置生成爆炸效果（仅视觉，无伤害无破坏）
     * JS: instance.explosionEffect(100.0, 64.0, 200.0, 3.0)
     * @param power 爆炸强度
     */
    fun explosionEffect(x: Double, y: Double, z: Double, power: Float = 3.0f) {
        val w = world ?: return
        w.createExplosion(x, y, z, power, false, false)
    }

    /**
     * 在指定位置生成烟花效果
     * JS: instance.firework(100.0, 64.0, 200.0)
     */
    fun firework(x: Double, y: Double, z: Double) {
        val w = world ?: return
        val loc = Location(w, x, y, z)
        w.spawn(loc, org.bukkit.entity.Firework::class.java) { spawned ->
            val meta = spawned.fireworkMeta
            meta.addEffect(
                org.bukkit.FireworkEffect.builder()
                    .with(org.bukkit.FireworkEffect.Type.BALL)
                    .withColor(org.bukkit.Color.RED, org.bukkit.Color.YELLOW)
                    .build()
            )
            meta.power = 0
            spawned.fireworkMeta = meta
            spawned.detonate()
        }
    }

    /**
     * 在指定位置生成自定义烟花效果
     * JS: instance.fireworkCustom(100.0, 64.0, 200.0, "BALL", "RED,BLUE", "YELLOW", 1)
     * @param type 烟花类型：BALL / BALL_LARGE / STAR / BURST / CREEPER
     * @param colors 主颜色列表，逗号分隔（如 "RED,BLUE,YELLOW"）
     * @param fadeColors 渐褪色列表，逗号分隔
     * @param trail 是否有拖尾（0/1）
     */
    fun fireworkCustom(x: Double, y: Double, z: Double, type: String = "BALL", colors: String = "RED", fadeColors: String = "", trail: Int = 0) {
        val w = world ?: return
        val fwType = try { org.bukkit.FireworkEffect.Type.valueOf(type.uppercase()) } catch (e: Exception) { return }
        val loc = Location(w, x, y, z)

        val colorList = colors.split(",").mapNotNull { c ->
            try { parseColorName(c.trim().uppercase()) } catch (e: Exception) { null }
        }
        if (colorList.isEmpty()) return

        val fadeList = fadeColors.split(",").mapNotNull { c ->
            try { parseColorName(c.trim().uppercase()) } catch (e: Exception) { null }
        }

        w.spawn(loc, org.bukkit.entity.Firework::class.java) { spawned ->
            val meta = spawned.fireworkMeta
            meta.addEffect(
                org.bukkit.FireworkEffect.builder()
                    .with(fwType)
                    .withColor(colorList)
                    .withFade(fadeList)
                    .trail(trail != 0)
                    .build()
            )
            meta.power = 0
            spawned.fireworkMeta = meta
            spawned.detonate()
        }
    }

    // ==================== JS脚本可调用：实体/物品操作 ===================

    /**
     * 在地牢世界指定位置掉落物品
     * JS: instance.dropItem(100.0, 64.0, 200.0, "DIAMOND", 1)
     * @param material 物品材质名称（如 "DIAMOND", "IRON_INGOT"）
     * @param amount 数量
     * @return 是否成功生成
     */
    fun dropItem(x: Double, y: Double, z: Double, material: String, amount: Int = 1): Boolean {
        val w = world ?: return false
        val mat = try { org.bukkit.Material.valueOf(material.uppercase()) } catch (e: Exception) { return false }
        val itemStack = org.bukkit.inventory.ItemStack(mat, amount)
        w.dropItem(Location(w, x, y, z), itemStack)
        return true
    }

    /**
     * 在地牢世界指定位置掉落物品（含自定义 ItemStack）
     * JS: var item = Bukkit.getItemFactory().createItemStack("DIAMOND_SWORD");
     *     instance.dropItemStack(100.0, 64.0, 200.0, item)
     */
    fun dropItemStack(x: Double, y: Double, z: Double, itemStack: org.bukkit.inventory.ItemStack) {
        val w = world ?: return
        w.dropItem(Location(w, x, y, z), itemStack)
    }

    /**
     * 清除地牢世界中所有掉落物
     * JS: instance.clearDropItems()
     */
    fun clearDropItems() {
        world?.getEntitiesByClass(org.bukkit.entity.Item::class.java)?.forEach { it.remove() }
    }

    /**
     * 清除地牢世界中所有指定类型的实体
     * JS: instance.clearEntities("ARROW")
     * @param entityType 实体类型名称（如 "ARROW", "BOAT", "DROPPED_ITEM"）
     */
    fun clearEntities(entityType: String) {
        val type = try { org.bukkit.entity.EntityType.valueOf(entityType.uppercase()) } catch (e: Exception) { return }
        val toRemove = world?.entities?.filter { it.type == type } ?: return
        MonsterManager.removeEntityTracking(toRemove.map { it.uniqueId }.toSet())
        toRemove.forEach { it.remove() }
    }

    /**
     * 清除地牢世界中所有怪物（敌对生物）
     * JS: instance.clearHostileMobs()
     */
    fun clearHostileMobs() {
        val toRemove = world?.entities?.filter { it is org.bukkit.entity.Monster } ?: return
        MonsterManager.removeEntityTracking(toRemove.map { it.uniqueId }.toSet())
        toRemove.forEach { it.remove() }
    }

    /**
     * 清除地牢世界中所有生物（除玩家外）
     * JS: instance.clearAllMobs()
     */
    fun clearAllMobs() {
        val toRemove = world?.entities?.filter { it !is org.bukkit.entity.Player } ?: return
        MonsterManager.removeEntityTracking(toRemove.map { it.uniqueId }.toSet())
        toRemove.forEach { it.remove() }
    }

    // ==================== JS脚本可调用：剩余时间便捷方法 ===================

    /**
     * 获取剩余时间（秒），无需传递 template 参数
     * JS: var remain = instance.getRemainingTimeSimple()
     * @return 剩余秒数，无时间限制返回 null
     */
    fun getRemainingTimeSimple(): Double? {
        val template = getTemplate() ?: return null
        return getRemainingTime(template)
    }

    /**
     * 获取已用时间（格式化字符串）
     * JS: var time = instance.getElapsedTimeFormatted()
     * @return 格式如 "01:30"（分:秒）
     */
    fun getElapsedTimeFormatted(): String {
        val total = getElapsedTime().toInt()
        val minutes = total / 60
        val seconds = total % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    // ==================== JS脚本可调用：地牢元数据管理 ====================

    /**
     * 设置地牢元数据
     * JS: instance.setMeta("wave", 3)
     * @param key 元数据键名
     * @param value 值
     */
    fun setMeta(key: String, value: Any?) {
        meta.set(key, value)
    }

    /**
     * 增加地牢元数据（数值累加）
     * JS: instance.addMeta("wave", 1)
     * @param key 元数据键名
     * @param value 要累加的值
     */
    fun addMeta(key: String, value: Any?) {
        meta.add(key, value)
    }

    /**
     * 获取地牢元数据
     * JS: var wave = instance.getMeta("wave")
     * @param key 元数据键名
     * @return 元数据值（可能为 null）
     */
    fun getMeta(key: String): Any? {
        return meta.get(key)
    }

    /**
     * 获取地牢元数据（整型）
     * JS: var wave = instance.getMetaAsInt("wave")
     * @return 整型值，无法转换时返回 null
     */
    fun getMetaAsInt(key: String): Int? {
        return meta.getAsInt(key)
    }

    /**
     * JS: var time = instance.getMetaAsLong("start_time")
     */
    fun getMetaAsLong(key: String): Long? {
        return meta.getAsLong(key)
    }

    /**
     * 获取地牢元数据（浮点型）
     * JS: var value = instance.getMetaAsDouble("score")
     */
    fun getMetaAsDouble(key: String): Double? {
        return meta.getAsDouble(key)
    }

    /**
     * 获取地牢元数据（字符串）
     * JS: var value = instance.getMetaAsString("status")
     */
    fun getMetaAsString(key: String): String? {
        return meta.getAsString(key)
    }

    /**
     * JS: var v = instance.getMetaAsFloat("score")
     */
    fun getMetaAsFloat(key: String): Float? {
        return meta.getAsFloat(key)
    }

    /**
     * JS: var flag = instance.getMetaAsBoolean("flag")
     */
    fun getMetaAsBoolean(key: String): Boolean? {
        return meta.getAsBoolean(key)
    }

    /**
     * JS: var id = instance.getMetaAsUUID("playerId")
     */
    fun getMetaAsUUID(key: String): UUID? {
        return meta.getAsUUID(key)
    }

    /**
     * JS: var list = instance.getMetaAsList("items")
     */
    fun getMetaAsList(key: String): List<*>? {
        return meta.getAsList(key)
    }

    /**
     * JS: var map = instance.getMetaAsMap("data")
     */
    fun getMetaAsMap(key: String): Map<String, *>? {
        return meta.getAsMap(key)
    }

    /**
     * 检查地牢元数据是否存在
     * JS: var has = instance.hasMeta("wave")
     */
    fun hasMeta(key: String): Boolean {
        return meta.get(key) != null
    }

    // ==================== 玩家元数据 ====================

    private fun getOrCreatePlayerMeta(player: Player): DungeonMeta {
        return playerMeta.computeIfAbsent(player.uniqueId) { DungeonMeta(ConcurrentHashMap()) }
    }

    /**
     * 设置玩家元数据
     * JS: instance.setPlayerMeta(player, "score", 100)
     */
    fun setPlayerMeta(player: Player, key: String, value: Any?) {
        getOrCreatePlayerMeta(player).set(key, value)
    }

    /**
     * 增加玩家元数据（数值累加）
     * JS: instance.addPlayerMeta(player, "score", 10)
     */
    fun addPlayerMeta(player: Player, key: String, value: Any?) {
        getOrCreatePlayerMeta(player).add(key, value)
    }

    /**
     * 获取玩家元数据
     * JS: var score = instance.getPlayerMeta(player, "score")
     */
    fun getPlayerMeta(player: Player, key: String): Any? {
        return playerMeta[player.uniqueId]?.get(key)
    }

    /**
     * JS: var score = instance.getPlayerMetaAsInt(player, "score")
     */
    fun getPlayerMetaAsInt(player: Player, key: String): Int? {
        return playerMeta[player.uniqueId]?.getAsInt(key)
    }

    /**
     * JS: var score = instance.getPlayerMetaAsDouble(player, "score")
     */
    fun getPlayerMetaAsDouble(player: Player, key: String): Double? {
        return playerMeta[player.uniqueId]?.getAsDouble(key)
    }

    /**
     * JS: var name = instance.getPlayerMetaAsString(player, "title")
     */
    fun getPlayerMetaAsString(player: Player, key: String): String? {
        return playerMeta[player.uniqueId]?.getAsString(key)
    }

    /**
     * JS: var flag = instance.getPlayerMetaAsBoolean(player, "flag")
     */
    fun getPlayerMetaAsBoolean(player: Player, key: String): Boolean? {
        return playerMeta[player.uniqueId]?.getAsBoolean(key)
    }

    /**
     * 检查玩家元数据是否存在
     * JS: var has = instance.hasPlayerMeta(player, "score")
     */
    fun hasPlayerMeta(player: Player, key: String): Boolean {
        return playerMeta[player.uniqueId]?.get(key) != null
    }

    /**
     * 删除玩家元数据
     * JS: instance.removePlayerMeta(player, "score")
     */
    fun removePlayerMeta(player: Player, key: String) {
        playerMeta[player.uniqueId]?.remove(key)
    }

    /**
     * 清除玩家所有元数据（玩家离开地牢时调用）
     */
    fun clearPlayerMeta(player: Player) {
        playerMeta.remove(player.uniqueId)
    }

    /**
     * 清除所有玩家元数据（地牢实例销毁时调用）
     */
    fun clearAllPlayerMeta() {
        playerMeta.clear()
    }
}

enum class DungeonState{
    PREPARING,
    ACTIVE,
    FAILED,
    COMPLETED
}

/**
 * 地牢统计信息及元数据
 *
 * == 生命周期 ==
 * dungeon.start    地牢开始次数
 * dungeon.complete 地牢通关次数
 * dungeon.fail     地牢失败次数
 *
 * == 玩家 ==
 * player.join      玩家加入次数
 * player.leave     玩家离开次数
 * player.dead      所有玩家死亡次数
 * player.dead.<玩家名称> 特定玩家死亡次数
 *
 * == 战斗 ==
 * mob.kill         生物击杀总数
 * mob.kill.<生物类型> 特定类型生物击杀数
 * boss.kill        BOSS击杀数
 * boss.kill.<BOSS名称> 特定BOSS击杀数
 *
 * == 怪物系统 ==
 * monster.spawn             怪物组生成次数
 * monster.spawn.<configId>  特定怪物组生成次数
 * monster.group.clear       怪物组全清次数
 * monster.group.clear.<configId> 特定怪物组全清次数
 *
 * == 障碍物 ==
 * obstacle.prepare           障碍物准备次数
 * obstacle.prepare.<configId>
 * obstacle.activate          障碍物激活次数
 * obstacle.activate.<configId>
 * obstacle.open              障碍物打开次数
 * obstacle.open.<configId>
 *
 * == 区域 ==
 * region.enter           区域进入次数
 * region.enter.<configId>
 * region.leave           区域离开次数
 * region.leave.<configId>
 *
 * == 交互 ==
 * interact.trigger           交互触发次数
 * interact.trigger.<configId>
 */
data class DungeonMeta(
    val meta: ConcurrentHashMap<String, Any?>,
) {
    fun add(key: String, value: Any?) {
        if (value == null) {
            meta[key] = null
            return
        }
        meta.merge(key, value) { old, new -> CastHelper.increaseAny(old, new) }
    }
    fun set(key: String, value: Any?) {
        meta[key] = value
    }
    fun remove(key: String) {
        meta.remove(key)
    }
    fun keys(): Set<String> = meta.keys
    fun get(key: String): Any? {
        return meta[key]
    }
    fun getAsDouble(key: String): Double? {
        val v = meta[key] ?: return null
        return when (v) {
            is Number -> v.toDouble()
            else -> v.toString().toDoubleOrNull()
        }
    }
    fun getAsInt(key: String): Int? {
        val v = meta[key] ?: return null
        return when (v) {
            is Number -> v.toInt()
            else -> v.toString().toIntOrNull()
        }
    }
    fun getAsLong(key: String): Long? {
        val v = meta[key] ?: return null
        return when (v) {
            is Number -> v.toLong()
            else -> v.toString().toLongOrNull()
        }
    }
    fun getAsString(key: String): String? {
        return meta[key]?.toString()
    }
    fun getAsFloat(key: String): Float? {
        val v = meta[key] ?: return null
        return when (v) {
            is Number -> v.toFloat()
            else -> v.toString().toFloatOrNull()
        }
    }
    fun getAsBoolean(key: String): Boolean? {
        val v = meta[key] ?: return null
        return when (v) {
            is Boolean -> v
            else -> v.toString().toBooleanStrictOrNull()
        }
    }
    fun getAsByte(key: String): Byte? {
        val v = meta[key] ?: return null
        return when (v) {
            is Number -> v.toByte()
            else -> v.toString().toByteOrNull()
        }
    }
    fun getAsShort(key: String): Short? {
        val v = meta[key] ?: return null
        return when (v) {
            is Number -> v.toShort()
            else -> v.toString().toShortOrNull()
        }
    }
    fun getAsUUID(key: String): UUID? {
        val v = meta[key] ?: return null
        return when (v) {
            is UUID -> v
            else -> try { UUID.fromString(v.toString()) } catch (e: Exception) { null }
        }
    }
    @Suppress("UNCHECKED_CAST")
    fun getAsList(key: String): List<*>? {
        val v = meta[key] ?: return null
        return v as? List<*>
    }
    @Suppress("UNCHECKED_CAST")
    fun getAsMap(key: String): Map<String, *>? {
        val v = meta[key] ?: return null
        return (v as? Map<*, *>)?.filterKeys { it is String } as? Map<String, *>
    }
}

/** Parse Bukkit Color from string name (replaces removed Color.valueOf()) */
private val COLOR_MAP = mapOf(
    "WHITE" to org.bukkit.Color.WHITE, "SILVER" to org.bukkit.Color.SILVER,
    "GRAY" to org.bukkit.Color.GRAY, "BLACK" to org.bukkit.Color.BLACK,
    "RED" to org.bukkit.Color.RED, "MAROON" to org.bukkit.Color.MAROON,
    "YELLOW" to org.bukkit.Color.YELLOW, "OLIVE" to org.bukkit.Color.OLIVE,
    "LIME" to org.bukkit.Color.LIME, "GREEN" to org.bukkit.Color.GREEN,
    "AQUA" to org.bukkit.Color.AQUA, "TEAL" to org.bukkit.Color.TEAL,
    "BLUE" to org.bukkit.Color.BLUE, "NAVY" to org.bukkit.Color.NAVY,
    "FUCHSIA" to org.bukkit.Color.FUCHSIA, "PURPLE" to org.bukkit.Color.PURPLE,
    "ORANGE" to org.bukkit.Color.ORANGE
)
private fun parseColorName(name: String): org.bukkit.Color? = COLOR_MAP[name]
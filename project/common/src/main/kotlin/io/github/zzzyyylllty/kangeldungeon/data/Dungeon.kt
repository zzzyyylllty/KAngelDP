package io.github.zzzyyylllty.kangeldungeon.data

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.event.*
import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import io.github.zzzyyylllty.kangeldungeon.util.CastHelper
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.TargetSelectorHelper
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import io.github.zzzyyylllty.kangeldungeon.data.defaultData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.util.Vector
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

    // 时间限制
    val timeLimit: Double? = 3600.0, // 秒
    val preparationTime: Double? = 30.0, // 准备时间（秒）

    // 其他设置
    val allowRespawn: Boolean = false,
    val keepInventory: Boolean = false,
    val pvpEnabled: Boolean = false,
    val naturalRegeneration: Boolean = true,

    // 权限要求
    val requiredPermission: String? = null,

    // 玩家出生点（相对坐标）
    val playerSpawnOffset: Vector,

    // 地牢事件代理脚本（在特定生命周期执行）
    val agents: Agents? = null
) {
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
data class DungeonInstance(
    val templateName: String,
    val uuid: UUID = UUID.randomUUID(),

    // 玩家管理
    val players: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
    val deadPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
    val leaderUUID: UUID,

    // 时间管理
    val createdAt: Long = System.currentTimeMillis(),
    var startedAt: Long? = null,
    var completedAt: Long? = null,

    // 状态管理
    var state: DungeonState = DungeonState.PREPARING,

    // 统计信息
    var meta: DungeonMeta,

    // 位置信息
    val spawnLocation: Location
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

        // 2. 触发 Pre 事件 (可取消)
        val event = DungeonPlayerJoinPreEvent(this, player)
        event.call()
        if (event.isCancelled) return false

        // 3. 执行核心逻辑
        val added = players.add(player.uniqueId)

        if (added) {
            // 4. 传送玩家到地牢出生点
            player.teleport(spawnLocation)
            // 5. 触发 Post 事件 (不可取消)
            DungeonPlayerJoinPostEvent(this, player).call()
        }

        return added
    }

    /**
     * 将玩家从地牢中移除
     * @param player 目标玩家
     * @return 是否成功移除
     */
    fun removePlayer(player: Player): Boolean {
        // 1. 触发 Pre 事件 (可取消，例如：战斗状态下不允许退出)
        val event = DungeonPlayerQuitPreEvent(this, player)
        event.call()
        if (event.isCancelled) return false

        // 2. 执行核心逻辑
        val removed = players.remove(player.uniqueId)

        if (removed) {
            // 3. 传送玩家回到主世界
            try {
                player.teleport(Bukkit.getWorlds()[0].spawnLocation)
            } catch (e: Exception) {
                // ignore teleport failure
            }
            // 4. 触发 Post 事件 (不可取消)
            DungeonPlayerQuitPostEvent(this, player).call()
        }

        return removed
    }

    /**
     * 将死亡玩家标记为已死亡，并从活跃玩家中移除
     */
    fun markPlayerDead(player: Player): Boolean {
        if (player.uniqueId !in players) return false
        deadPlayers.add(player.uniqueId)
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
        // 1. 状态检查
        if (state != DungeonState.PREPARING) {
            warningL("WarningDungeonAlreadyStarted", templateName, uuid)
            return false
        }

        // 2. 触发 Pre 事件 (可取消)
        val event = DungeonStartPreEvent(this)
        event.call()
        if (event.isCancelled) return false

        // 3. 执行状态变更
        state = DungeonState.ACTIVE
        startedAt = System.currentTimeMillis()

        // 4. 传送所有玩家到出生点
        teleportAllToSpawn()

        // 5. 执行 onStart 代理脚本
        runAgentSafe("onStart", mapOf("instance" to this, "template" to getTemplate()), null)

        // 6. 触发 Post 事件 (不可取消)
        DungeonStartPostEvent(this).call()
        return true
    }

    /**
     * 完成地牢 (成功通关)
     * @return 是否成功触发完成逻辑
     */
    fun complete(): Boolean {
        if (state != DungeonState.ACTIVE) return false

        // 1. 触发 Pre 事件 (可取消)
        val event = DungeonCompletePreEvent(this)
        event.call()
        if (event.isCancelled) return false

        // 2. 执行状态变更
        state = DungeonState.COMPLETED
        completedAt = System.currentTimeMillis()

        // 3. 执行 onComplete 代理脚本
        runAgentSafe("onComplete", mapOf("instance" to this, "template" to getTemplate()), null)

        // 4. 触发 Post 事件 (不可取消)
        DungeonCompletePostEvent(this).call()
        return true
    }

    /**
     * 地牢失败 (例如玩家全灭或超时)
     * @return 是否成功触发失败逻辑
     */
    fun fail(): Boolean {
        // 准备中或进行中都可以触发失败
        if (state != DungeonState.ACTIVE && state != DungeonState.PREPARING) return false

        // 1. 触发 Pre 事件 (可取消)
        val event = DungeonFailPreEvent(this)
        event.call()
        if (event.isCancelled) return false

        // 2. 执行状态变更
        state = DungeonState.FAILED
        completedAt = System.currentTimeMillis()

        // 3. 执行 onFail 代理脚本
        runAgentSafe("onFail", mapOf("instance" to this, "template" to getTemplate()), null)

        // 4. 触发 Post 事件 (不可取消)
        DungeonFailPostEvent(this).call()
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
        return template.timeLimit?.let { getElapsedTime() >= it } ?: false
    }

    /**
     * 所有玩家是否都死亡
     */
    fun areAllPlayersDead(): Boolean {
        if (players.isEmpty()) return false
        val activePlayers = players.filter { Bukkit.getPlayer(it) != null }
        return activePlayers.isNotEmpty() && activePlayers.all { it in deadPlayers }
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
     * @return 是否成功踢出
     */
    fun kickPlayer(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        return removePlayer(player)
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
        val data = defaultData + mapOf("instance" to this, "template" to getTemplate())

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
        getOnlinePlayers().forEach { it.health = health }
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

    // ==================== JS脚本可调用：视觉效果 ====================

    /**
     * 在指定位置生成闪电效果（仅视觉，无伤害）
     * JS: instance.strikeLightning(100.0, 64.0, 200.0)
     */
    fun strikeLightning(x: Double, y: Double, z: Double) {
        world?.strikeLightningEffect(Location(world, x, y, z))
    }

    /**
     * 在指定位置生成粒子效果
     * JS: instance.spawnParticle("flame", 100.0, 64.0, 200.0, 30)
     */
    fun spawnParticle(particle: String, x: Double, y: Double, z: Double, count: Int = 10, offsetX: Double = 0.5, offsetY: Double = 0.5, offsetZ: Double = 0.5, speed: Double = 0.1) {
        val p = try { Particle.valueOf(particle.uppercase()) } catch (e: Exception) { return }
        world?.spawnParticle(p, Location(world, x, y, z), count, offsetX, offsetY, offsetZ, speed)
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
     * 复活所有已死亡的在线玩家
     * JS: instance.respawnAllDeadPlayers()
     */
    fun respawnAllDeadPlayers() {
        val toRespawn = deadPlayers.filter { Bukkit.getPlayer(it) != null }.toSet()
        deadPlayers.removeAll(toRespawn)
    }

    /**
     * 清除指定玩家身上的药水效果
     * JS: instance.clearPlayerEffects("Notch")
     * @return 玩家是否在线
     */
    fun clearPlayerEffects(playerName: String): Boolean {
        val player = Bukkit.getPlayerExact(playerName) ?: return false
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        return true
    }

    /**
     * 清除所有在线玩家的药水效果
     * JS: instance.clearAllPlayersEffects()
     */
    fun clearAllPlayersEffects() {
        getOnlinePlayers().forEach { player ->
            player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        }
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
 * player.dead 所有玩家死亡次数
 * player.dead.<玩家名称> 特定玩家死亡次数
 * boss.kill BOSS击杀数
 * boss.kill.<BOSS名称> BOSS击杀数
 * mob.kill 生物击杀
 * mob.kill.<mob> 生物击杀
 */
data class DungeonMeta(
    val meta: ConcurrentHashMap<String, Any?>,
) {
    fun add(key: String, value: Any?) {
        if (meta[key] == null) meta[key] = value
        else meta[key] = CastHelper.increaseAny(meta[key], value)
    }
    fun set(key: String, value: Any?) {
        meta[key] = value
    }
    fun get(key: String): Any? {
        return meta[key]
    }
    fun getAsDouble(key: String): Double? {
        return meta[key]?.toString()?.toDouble()
    }
    fun getAsInt(key: String): Int? {
        return meta[key]?.toString()?.toInt()
    }
    fun getAsLong(key: String): Long? {
        return meta[key]?.toString()?.toLong()
    }
    fun getAsString(key: String): String? {
        return meta[key]?.toString()
    }
}
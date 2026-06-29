package io.github.zzzyyylllty.kangeldungeon

import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.data.load.loadDungeonFiles
import io.github.zzzyyylllty.kangeldungeon.event.DungeonTickEvent
import io.github.zzzyyylllty.kangeldungeon.logger.*
import io.github.zzzyyylllty.kangeldungeon.team.TeamManager
import io.github.zzzyyylllty.kangeldungeon.team.TeamProvider
import io.github.zzzyyylllty.kangeldungeon.util.GraalJsUtil
import io.github.zzzyyylllty.kangeldungeon.util.KAngelDungeonLocalDependencyHelper
import io.github.zzzyyylllty.kangeldungeon.util.dependencies
import io.github.zzzyyylllty.kangeldungeon.util.deleteDirectory
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
import io.github.zzzyyylllty.kangeldungeon.util.monster.MonsterManager
import io.github.zzzyyylllty.kangeldungeon.util.obstacle.ObstacleManager
import io.github.zzzyyylllty.kangeldungeon.util.region.RegionManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.graalvm.polyglot.Source
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeEnv
import taboolib.common.platform.Awake
import taboolib.common.platform.Plugin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.common.platform.function.info
import taboolib.common.platform.function.severe
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.expansion.setupPlayerDatabase
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.database.getHost
import taboolib.module.kether.Kether
import taboolib.module.lang.asLangText
import taboolib.module.kether.KetherShell
import taboolib.module.lang.Language
import taboolib.module.lang.event.PlayerSelectLocaleEvent
import taboolib.module.lang.event.SystemSelectLocaleEvent
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.Volatile


object KAngelDungeon : Plugin(), KAngelDungeonAPI {


    @Config("config.yml")
    lateinit var config: Configuration

    val plugin by lazy { this }
    val dataFolder by lazy { nativeDataFolder() }
    val console by lazy { console() }
    val consoleSender by lazy { console.castSafely<CommandSender>() ?: let {
        severe("Failed to cast console to CommandSender, using fallback")
        Bukkit.getConsoleSender()
    } }
    val host by lazy { config.getHost("database") }
    val dataSource by lazy { host.createDataSource() }
    val gjsScriptCache by lazy { ConcurrentHashMap<String, Source?>() }

    val dungeonInstances = ConcurrentHashMap<UUID, DungeonInstance>()
    /** worldName → instanceUuid 反向索引，用于 O(1) 查找实例 */
    val worldInstanceIndex = ConcurrentHashMap<String, UUID>()
    /** playerUUID → instanceUuid 反向索引，用于 O(1) 检查玩家是否已在其他地牢 */
    val playerToInstanceIndex = ConcurrentHashMap<UUID, UUID>()
    val dungeonTemplates = ConcurrentHashMap<String, DungeonTemplate>()
    val dungeonScripts = ConcurrentHashMap<String, ConcurrentHashMap<String, DungeonScript>>()
    /** 全局 JS 脚本（scripts/ 目录下的 .js 文件），所有地牢通用 */
    val globalScripts = ConcurrentHashMap<String, DungeonScript>()
    // Per-dungeon configs: dungeonName -> (configId -> config)
    val dungeonObstacleConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, ObstacleConfig>>()
    val dungeonMonsterConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, MonsterConfig>>()
    val dungeonInteractConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, InteractConfig>>()
    val dungeonPlanConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, Plan>>()
    val dungeonRegionConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, RegionConfig>>()
    val dungeonKitConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, KitConfig>>()
    // Per-dungeon task configs
    val dungeonTaskConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, TaskConfig>>()
    // Per-dungeon difficulty configs
    val dungeonDifficultyConfigs = ConcurrentHashMap<String, ConcurrentHashMap<String, DifficultyConfig>>()
    // Global fallback (backward compat)
    val obstacleConfigs = ConcurrentHashMap<String, ObstacleConfig>()
    val monsterConfigs = ConcurrentHashMap<String, MonsterConfig>()
    val kitConfigs = ConcurrentHashMap<String, KitConfig>()
    val blockRegenMap = ConcurrentHashMap<String, MutableSet<UUID>>()
    /** 玩家放置方块追踪: worldName -> ("x,y,z" -> originalBlockData) */
    val playerPlacedBlocks = ConcurrentHashMap<String, MutableMap<String, String>>()
    /** 玩家放置方块计数: "world:player" -> count */
    val playerPlacedBlockCount = ConcurrentHashMap<String, Int>()

    val dateTimeFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }
    @Volatile
    var devMode = false
    @Volatile
    var maintenanceMode = false
    /** 重载配置中标志，防止 reload 期间 tick 读取不一致数据 */
    @Volatile
    var isReloading = false
    /** 被封禁的玩家 (lowercase name -> reason) */
    val blacklistedPlayers = ConcurrentHashMap<String, String>()
    val ketherScriptCache by lazy { ConcurrentHashMap<String, KetherShell.Cache>() }

    // Tick任务引用，用于生命周期管理和清理
    private var tickTask: PlatformExecutor.PlatformTask? = null

    // === KAngelDungeonAPI Implementation ===

    override val teamManager: TeamManager get() = TeamManager

    override fun registerTeamProvider(provider: TeamProvider) {
        TeamManager.registerProvider(provider)
    }

    override fun getTeamProvider(): TeamProvider? = TeamManager.getProvider()



    override fun onLoad() {
        if (config.getBoolean("database.enable", false)) {
            val dbSection = config.getConfigurationSection("database")
            if (dbSection != null) {
                setupPlayerDatabase(dbSection)
            } else {
                setupPlayerDatabase(File("${config.getString("database.filename", "data")}.db"))
            }
        } else {
            setupPlayerDatabase(File("${config.getString("database.filename", "data")}.db"))
        }
    }

    override fun onEnable() {

        infoL("Enable")

        val parser = Kether.scriptRegistry.getParser("kdp", "kether")
        println("kdp parser registered: ${parser.isPresent}")
        println("All registered actions: ${Kether.scriptRegistry.getRegisteredActions()}")
        Language.enableSimpleComponent = true
        reloadCustomConfig()

        // 初始化队伍系统
        TeamManager.initialize()

        // 启动地牢生命周期Tick（每秒）
        startDungeonTick()

        // 清理残余地牢世界（延迟执行确保配置已加载）
        if (config.getBoolean("cleanup-residual-worlds", true)) {
            submit(delay = 40L) {
                cleanupResidualWorlds()
            }
        }

        // 尝试加载 Chemdah 任务挂钩（软依赖，不存在时静默跳过）
        try {
            val chemdahClass = Class.forName("io.github.zzzyyylllty.kangeldungeon.util.chemdah.ChemdahObjectives")
            chemdahClass.getMethod("register").invoke(null)
        } catch (_: Throwable) {
            // Chemdah 未安装或挂载失败，静默跳过
        }

        // 尝试注册 PlaceholderAPI 扩展（软依赖）
        try {
            val papiHook = Class.forName("io.github.zzzyyylllty.kangeldungeon.util.papi.PapiHook")
            papiHook.getMethod("register").invoke(null)
        } catch (_: Throwable) {
            // PlaceholderAPI 未安装，静默跳过
        }
    }

    override fun onDisable() {
        infoL("Disable")

        // 取消地牢生命周期Tick
        tickTask?.cancel()
        tickTask = null

        // 清理所有活跃地牢实例（同步删除世界文件夹，确保关闭时清理）
        val instances = dungeonInstances.values.toList()
        for (instance in instances) {
            try {
                instance.stopAllPlans()
                RegionManager.clearWorld(instance.worldName)
                DungeonHelper.unloadDungeonWorld(instance, syncDelete = true)
            } catch (e: Exception) {
                severe(e.message ?: "Failed to cleanup dungeon instance: ${instance.uuid}")
                e.printStackTrace()
            }
        }
        dungeonInstances.clear()
        playerToInstanceIndex.clear()

        // 清理队伍系统
        TeamManager.unregisterProvider()

        // 关闭所有线程的 GraalJS Context，释放原生内存
        GraalJsUtil.closeCurrentContext()

        // 关闭数据库连接池
        try { (dataSource as? AutoCloseable)?.close() } catch (_: Exception) {}
    }

    /**
     * 启动地牢生命周期Tick任务（每秒运行一次）
     * - PREPARING 状态：倒计时结束后自动start()
     * - ACTIVE 状态：检查超时、检查全队死亡
     * - COMPLETED/FAILED 状态：60秒后自动清理
     */
    private fun startDungeonTick() {
        // 取消已存在的tick任务（防止重复创建）
        tickTask?.cancel()
        tickTask = submit(period = 20L, delay = 20L) {
            // 重载配置期间跳过 tick，避免读取不一致数据
            if (isReloading) return@submit

            val toRemove = mutableListOf<UUID>()
            val now = System.currentTimeMillis()

            for ((uuid, instance) in dungeonInstances) {
                when (instance.state) {
                    DungeonState.PREPARING -> {
                        // 触发Tick事件（供外部监听）
                        DungeonTickEvent(instance).call()

                        val template = dungeonTemplates[instance.templateName]
                        if (template != null) {
                            val prepTime = template.preparationTime ?: 0.0
                            if (prepTime > 0) {
                                val elapsed = (now - instance.createdAt) / 1000.0
                                val remaining = (prepTime - elapsed).coerceAtLeast(0.0)
                                val remainingInt = remaining.toInt()

                                // 准备阶段通知
                                if (prepTime > 0 && config.getBoolean("preparation.notify.enabled", true)) {
                                    val interval = config.getInt("preparation.notify.interval", 5).coerceAtLeast(1)
                                    val countdownLast = config.getInt("preparation.notify.countdown-last-seconds", 10).coerceAtLeast(1)
                                    val mode = config.getString("preparation.notify.mode", "title") ?: "title"

                                    val shouldNotify = remainingInt != instance.lastPrepNotifyRemaining &&
                                        (remainingInt <= countdownLast || remainingInt % interval == 0 || instance.lastPrepNotifyRemaining == -1)

                                    if (shouldNotify) {
                                        instance.lastPrepNotifyRemaining = remainingInt
                                        val message = console.asLangText("PrepNotifyCountdown", remainingInt.toString())
                                        when (mode.lowercase()) {
                                            "actionbar" -> instance.sendActionBarToAllPlayers(message)
                                            "chat" -> instance.sendMessageToAllPlayers(message)
                                            "title" -> instance.sendTitleToAllPlayers(message, "", 5, 30, 10)
                                            else -> instance.sendTitleToAllPlayers(message, "", 5, 30, 10)
                                        }
                                    }
                                }

                                // 准备时间到，自动开始
                                if (now - instance.createdAt >= prepTime * 1000 && instance.worldReady) {
                                    instance.start()
                                }
                            } else {
                                // 无准备时间，立即开始
                                if (instance.worldReady && instance.lastPrepNotifyRemaining == -1) {
                                    instance.start()
                                }
                            }
                        }
                    }
                    DungeonState.ACTIVE -> {
                        DungeonTickEvent(instance).call()

                        // 怪物组 tick（自动重生等）
                        MonsterManager.tickDungeonMonsters(instance)

                        // 环境时间锁定（每 tick 维持）
                        dungeonTemplates[instance.templateName]?.environment?.timeLock?.let { tl ->
                            instance.setWorldTime(tl)
                        }

                        val template = dungeonTemplates[instance.templateName]
                        if (template != null) {
                            // 超时检查：优先触发
                            if (instance.isTimedOut(template)) {
                                if (config.getBoolean("notify.on-timeout", true)) {
                                    instance.sendTitleToAllPlayers(
                                        console.asLangText("DungeonTimeoutTitle"),
                                        console.asLangText("DungeonTimeoutSubtitle", config.getString("auto-exit-delay", "60") ?: "60"),
                                        10, 80, 20
                                    )
                                    instance.broadcastSound("entity_wither_spawn", 2.0f, 0.5f)
                                }
                                instance.fail()
                            } else if (instance.areAllPlayersDead()) {
                                // 使用 else if 防止超时和全灭同时触发导致重复消息
                                if (config.getBoolean("notify.on-all-dead", true)) {
                                    instance.sendTitleToAllPlayers(
                                        console.asLangText("DungeonAllDeadTitle"),
                                        console.asLangText("DungeonAllDeadSubtitle", config.getString("auto-exit-delay", "60") ?: "60"),
                                        10, 80, 20
                                    )
                                    instance.broadcastSound("entity_wither_death", 2.0f, 0.5f)
                                }
                                instance.fail()
                            }
                        }
                    }
                    DungeonState.COMPLETED, DungeonState.FAILED -> {
                        val completedAt = instance.completedAt ?: now
                        val autoExitDelay = config.getLong("auto-exit-delay", 60) * 1000
                        val notifyInterval = config.getInt("auto-exit-notify-interval", 5).coerceAtLeast(1)
                        val countdownLast = config.getInt("auto-exit-countdown-last-seconds", 10).coerceAtLeast(1)

                        if (now - completedAt >= autoExitDelay) {
                            toRemove.add(uuid)
                        } else {
                            // 首次进入结束状态时显示完成/失败标题
                            if (instance.lastEndCountdownRemaining == -1) {
                                if (instance.state == DungeonState.COMPLETED && config.getBoolean("notify.on-complete", true)) {
                                    instance.sendTitleToAllPlayers(
                                        console.asLangText("DungeonCompletedTitle"),
                                        console.asLangText("DungeonCompletedSubtitle"),
                                        10, 80, 20
                                    )
                                    instance.broadcastSound("entity_firework_rocket_blast", 2.0f, 1.0f)
                                } else if (instance.state == DungeonState.FAILED && config.getBoolean("notify.on-fail", true)) {
                                    instance.sendTitleToAllPlayers(
                                        console.asLangText("DungeonFailedTitle"),
                                        console.asLangText("DungeonFailedSubtitle"),
                                        10, 80, 20
                                    )
                                    instance.broadcastSound("entity_wither_death", 2.0f, 0.5f)
                                }
                            }

                            val remaining = ((autoExitDelay - (now - completedAt)) / 1000).toInt()
                            if (remaining != instance.lastEndCountdownRemaining) {
                                instance.lastEndCountdownRemaining = remaining
                                // 按间隔或最后几秒显示倒计时
                                if (remaining <= countdownLast || remaining % notifyInterval == 0) {
                                    instance.sendActionBarToAllPlayers(
                                        console.asLangText("DungeonEndAutoExit", remaining.toString())
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 清理已结束的地牢世界
            toRemove.forEach { uuid ->
                try {
                    dungeonInstances[uuid]?.let { instance ->
                        instance.stopAllPlans()
                        RegionManager.clearWorld(instance.worldName)
                        DungeonHelper.unloadDungeonWorld(instance)
                    }
                } catch (e: Exception) {
                    severe("Error during dungeon cleanup: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun reloadCustomConfig(async: Boolean = true, onComplete: ((List<io.github.zzzyyylllty.kangeldungeon.data.load.ReloadDiagnostics.Issue>) -> Unit)? = null) {
        // 防止并发重载
        if (isReloading) {
            onComplete?.let { submit { it(emptyList()) } }
            return
        }
        isReloading = true
        submit(async) {
            // 清除上次的诊断数据
            io.github.zzzyyylllty.kangeldungeon.data.load.ReloadDiagnostics.clear()

            // 清理任务执行计数，防止 reload 后旧计数影响新配置的 maxExecutions
            io.github.zzzyyylllty.kangeldungeon.util.task.TaskManager.clearAll()

            try {
                config.reload()
                devMode = config.getBoolean("debug", false)

                ketherScriptCache.clear()
                gjsScriptCache.clear()

                // 在 async 块内检查活跃实例，避免调用线程和异步线程间的竞态
                val hasActiveInstances = dungeonInstances.values.any { it.state == DungeonState.PREPARING || it.state == DungeonState.ACTIVE }
                if (hasActiveInstances) {
                    warningL("DungeonActiveReloadConfigWarn")
                }

                // 没有活跃实例时才完全清理，否则只增量加载/覆盖
                if (!hasActiveInstances) {
                    dungeonTemplates.clear()
                    dungeonScripts.clear()
                    globalScripts.clear()
                    dungeonObstacleConfigs.clear()
                    dungeonMonsterConfigs.clear()
                    dungeonInteractConfigs.clear()
                    dungeonPlanConfigs.clear()
                    dungeonRegionConfigs.clear()
                    dungeonDifficultyConfigs.clear()
                    dungeonTaskConfigs.clear()
                    obstacleConfigs.clear()
                    monsterConfigs.clear()
                    dungeonKitConfigs.clear()
                    kitConfigs.clear()
                }

                loadDungeonFiles()
            } catch (e: Exception) {
                severe("Error during reload: ${e.message}")
                e.printStackTrace()
            } finally {
                isReloading = false
            }

            // 收集诊断
            val diagnostics = io.github.zzzyyylllty.kangeldungeon.data.load.ReloadDiagnostics.collect()

            // 重载完成回调（确保在主线程执行）
            if (onComplete != null) {
                submit { onComplete(diagnostics) }
            }
        }
    }

    @Awake(LifeCycle.INIT)
    fun initDependenciesInit() {
        solveDependencies(dependencies)
    }

    /**
     * 清理服务器启动时可能残留的 KDP_* 地牢世界文件夹
     * 通常在服务器意外关闭后，地牢世界文件夹未被正确删除
     */
    private fun cleanupResidualWorlds() {
        val worldContainer = Bukkit.getWorldContainer()
        val worldFolders = worldContainer.listFiles { file ->
            file.isDirectory && file.name.startsWith("KDP_")
        } ?: return

        if (worldFolders.isEmpty()) return

        infoL("ResidualWorldsCleanupStart")

        var cleaned = 0
        for (folder in worldFolders) {
            try {
                // 确保世界未加载
                val world = Bukkit.getWorld(folder.name)
                if (world != null) {
                    Bukkit.unloadWorld(world, false)
                }
                deleteDirectory(folder)
                cleaned++
            } catch (e: Exception) {
                severe("Failed to delete residual world folder: ${folder.name} - ${e.message}")
            }
        }

        infoL("ResidualWorldsCleanupDone", cleaned.toString())
    }

    fun solveDependencies(dependencies: List<String>, useTaboo: Boolean = true) {
        info("Starting loading dependencies...")
        for (name in dependencies) {
            try {
                info("Trying to load dependencies from file $name")
                val resource = KAngelDungeon::class.java.classLoader.getResource("META-INF/dependencies/$name.json")
                if (resource == null) {
                    severe("Resource META-INF/dependencies/$name.json not found!")
                    continue // 跳过这个依赖文件
                }

                if (useTaboo) RuntimeEnv.ENV_DEPENDENCY.loadFromLocalFile(resource) else KAngelDungeonLocalDependencyHelper().loadFromLocalFile(resource)

                info("Trying to load dependencies from file $name ... DONE.")
            } catch (e: Exception) {
                severe("Trying to load dependencies from file $name FAILED.")
                severe("Exception: $e")
                e.printStackTrace()
            }
        }
    }




    @SubscribeEvent
    fun lang(event: PlayerSelectLocaleEvent) {
        event.locale = config.getString("lang", "zh_CN") ?: "zh_CN"
    }

    @SubscribeEvent
    fun lang(event: SystemSelectLocaleEvent) {
        event.locale = config.getString("lang", "zh_CN") ?: "zh_CN"
    }


}

package io.github.zzzyyylllty.kangeldungeon

import io.github.zzzyyylllty.kangeldungeon.data.*
import io.github.zzzyyylllty.kangeldungeon.data.load.loadDungeonFiles
import io.github.zzzyyylllty.kangeldungeon.event.DungeonTickEvent
import io.github.zzzyyylllty.kangeldungeon.logger.*
import io.github.zzzyyylllty.kangeldungeon.util.KAngelDungeonLocalDependencyHelper
import io.github.zzzyyylllty.kangeldungeon.util.dependencies
import io.github.zzzyyylllty.kangeldungeon.util.dungeon.DungeonHelper
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
import taboolib.expansion.setupPlayerDatabase
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.database.getHost
import taboolib.module.kether.KetherShell
import taboolib.module.lang.Language
import taboolib.module.lang.event.PlayerSelectLocaleEvent
import taboolib.module.lang.event.SystemSelectLocaleEvent
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.script.CompiledScript
import kotlin.jvm.Volatile


object KAngelDungeon : Plugin() {


    @Config("config.yml")
    lateinit var config: Configuration

    val plugin by lazy { this }
    val dataFolder by lazy { nativeDataFolder() }
    val console by lazy { console() }
    val consoleSender by lazy { console() as? CommandSender ?: error("Console not available") }
    val host by lazy { config.getHost("database") }
    val dataSource by lazy { host.createDataSource() }
    val gjsScriptCache by lazy { ConcurrentHashMap<String, Source?>() }

    val dungeonInstances = ConcurrentHashMap<UUID, DungeonInstance>()
    val dungeonTemplates = ConcurrentHashMap<String, DungeonTemplate>()
    val dungeonScripts = ConcurrentHashMap<String, ConcurrentHashMap<String, DungeonScript>>()
    val blockRegenMap = ConcurrentHashMap<String, MutableSet<UUID>>()

    val dateTimeFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }
    @Volatile
    var devMode = true
    val ketherScriptCache by lazy { ConcurrentHashMap<String, KetherShell.Cache>() }
    val jsScriptCache by lazy { ConcurrentHashMap<String, CompiledScript>() }



    override fun onLoad() {
        if (config.getBoolean("database.enable", false)) {
            setupPlayerDatabase(config.getConfigurationSection("database")!!)
        } else {
            setupPlayerDatabase(File("${config.getString("database.filename", "data")}.db"))
        }
    }

    override fun onEnable() {

        infoL("Enable")
        Language.enableSimpleComponent = true
        reloadCustomConfig()

        // 启动地牢生命周期Tick（每秒）
        startDungeonTick()

    }

    override fun onDisable() {
        infoL("Disable")
    }
    /*
    fun compat() {
        if (Bukkit.getPluginManager().getPlugin("Chemdah") != null) {
            connectChemdah()
        }
    }*/

    /**
     * 启动地牢生命周期Tick任务（每秒运行一次）
     * - PREPARING 状态：倒计时结束后自动start()
     * - ACTIVE 状态：检查超时、检查全队死亡
     * - COMPLETED/FAILED 状态：60秒后自动清理
     */
    private fun startDungeonTick() {
        submit(period = 20L, delay = 20L) {
            val toRemove = mutableListOf<UUID>()
            val now = System.currentTimeMillis()

            for ((uuid, instance) in dungeonInstances) {
                when (instance.state) {
                    DungeonState.PREPARING -> {
                        // 触发Tick事件（供外部监听）
                        DungeonTickEvent(instance).call()

                        // 检查准备时间是否已到，到时自动开始
                        val template = dungeonTemplates[instance.templateName]
                        if (template != null) {
                            val prepTime = template.preparationTime ?: 0.0
                            if (prepTime > 0 && now - instance.createdAt >= prepTime * 1000) {
                                instance.start()
                            }
                        }
                    }
                    DungeonState.ACTIVE -> {
                        DungeonTickEvent(instance).call()

                        val template = dungeonTemplates[instance.templateName]
                        if (template != null) {
                            // 检查超时
                            if (instance.isTimedOut(template)) {
                                instance.fail()
                                instance.sendMessageToAllPlayers("<red>地牢超时！</red>")
                            }
                            // 检查是否所有活跃玩家都已死亡
                            if (instance.areAllPlayersDead()) {
                                instance.fail()
                                instance.sendMessageToAllPlayers("<red>全员阵亡，地牢失败！</red>")
                            }
                        }
                    }
                    DungeonState.COMPLETED, DungeonState.FAILED -> {
                        // 结束后60秒自动清理
                        val completedAt = instance.completedAt ?: now
                        if (now - completedAt >= 60_000) {
                            toRemove.add(uuid)
                        }
                    }
                }
            }

            // 清理已结束的地牢世界
            toRemove.forEach { uuid ->
                dungeonInstances[uuid]?.let { instance ->
                    // 传送剩余玩家回主世界
                    instance.getOnlinePlayers().forEach { player ->
                        try {
                            player.teleport(Bukkit.getWorlds()[0].spawnLocation)
                        } catch (_: Exception) {}
                    }
                    DungeonHelper.unloadDungeonWorld(instance)
                }
            }
        }
    }

    fun reloadCustomConfig(async: Boolean = true) {
        submit(async) {

            config.reload()
            devMode = config.getBoolean("debug",false)

            ketherScriptCache.clear()
            jsScriptCache.clear()

            dungeonTemplates.clear()
            dungeonScripts.clear()

            loadDungeonFiles()
        }
    }

    @Awake(LifeCycle.INIT)
    fun initDependenciesInit() {
        solveDependencies(dependencies)
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
        event.locale = config.getString("lang", "zh_CN")!!
    }

    @SubscribeEvent
    fun lang(event: SystemSelectLocaleEvent) {
        event.locale = config.getString("lang", "zh_CN")!!
    }


}

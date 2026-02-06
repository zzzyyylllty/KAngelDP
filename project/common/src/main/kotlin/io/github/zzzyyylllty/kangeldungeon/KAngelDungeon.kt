package io.github.zzzyyylllty.kangeldungeon

import io.github.zzzyyylllty.kangeldungeon.data.DungeonInstance
import io.github.zzzyyylllty.kangeldungeon.data.DungeonTemplate
import io.github.zzzyyylllty.kangeldungeon.data.load.loadLevelFiles
import io.github.zzzyyylllty.kangeldungeon.data.load.loadRegenFiles
import io.github.zzzyyylllty.kangeldungeon.logger.*
import io.github.zzzyyylllty.kangeldungeon.util.KAngelDungeonLocalDependencyHelper
import io.github.zzzyyylllty.kangeldungeon.util.dependencies
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.tabooproject.fluxon.runtime.FluxonRuntime
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeEnv
import taboolib.common.platform.Awake
import taboolib.common.platform.Plugin
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
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


object KAngelDungeon : Plugin() {


    @Config("config.yml")
    lateinit var config: Configuration

    val plugin by lazy { this }
    val dataFolder by lazy { nativeDataFolder() }
    val console by lazy { console() }
    val consoleSender by lazy { console.castSafely<CommandSender>()!! }
    val host by lazy { config.getHost("database") }
    val dataSource by lazy { host.createDataSource() }
    var fluxonInst: FluxonRuntime? = null

    val dungeonInstances = ConcurrentHashMap<UUID, DungeonInstance>()
    val dungeonTemplates = ConcurrentHashMap<String, DungeonTemplate>()

    val dateTimeFormatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }
    var devMode = true
    val ketherScriptCache by lazy { ConcurrentHashMap<String, KetherShell.Cache>() }
    val jsScriptCache by lazy { ConcurrentHashMap<String, CompiledScript>() }



    override fun onLoad() {
        fluxonInst = FluxonRuntime.getInstance()
        if (config.getBoolean("database.enable", false)) {
            setupPlayerDatabase(config.getConfigurationSection("database")!!)
        } else {
            setupPlayerDatabase(File("${config.getString("database.filename") ?: "data"}.db"))
        }
    }

    override fun onEnable() {

        infoL("Enable")
        Language.enableSimpleComponent = true
        reloadCustomConfig()

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

    fun reloadCustomConfig(async: Boolean = true) {
        submit(async) {

            config.reload()
            devMode = config.getBoolean("debug",false)

            ketherScriptCache.clear()
            jsScriptCache.clear()

            dungeonTemplates.clear()

            loadRegenFiles()
            loadLevelFiles()
        }
    }

    @Awake(LifeCycle.INIT)
    fun initDependenciesInit() {
        solveDependencies(dependencies)
    }


    fun solveDependencies(dependencies: List<String>, useTaboo: Boolean = true) {
        infoS("Starting loading dependencies...")
        for (name in dependencies) {
            try {
                infoS("Trying to load dependencies from file $name")
                val resource = KAngelDungeon::class.java.classLoader.getResource("META-INF/dependencies/$name.json")
                if (resource == null) {
                    severeS("Resource META-INF/dependencies/$name.json not found!")
                    continue // 跳过这个依赖文件
                }

                if (useTaboo) RuntimeEnv.ENV_DEPENDENCY.loadFromLocalFile(resource) else KAngelDungeonLocalDependencyHelper().loadFromLocalFile(resource)

                infoS("Trying to load dependencies from file $name ... DONE.")
            } catch (e: Exception) {
                severeS("Trying to load dependencies from file $name FAILED.")
                severeS("Exception: $e")
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

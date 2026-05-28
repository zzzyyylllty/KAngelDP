package io.github.zzzyyylllty.kangeldungeon.util

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.gjsScriptCache
import io.github.zzzyyylllty.kangeldungeon.logger.severeL
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.security.MessageDigest
import javax.script.*
import kotlin.String


val GJS_LANG_ID = "js"

val globalGJSEngine: Engine by lazy {
    Engine.newBuilder(GJS_LANG_ID)
        .allowExperimentalOptions(true)
        .option("js.ecmascript-version", "latest")
        .option("js.nashorn-compat", "true") // Nashorn 兼容模式
        .build()
}

val hostAccess: HostAccess? by lazy {
    HostAccess.newBuilder()
//允许不受限制地访问所有公共构造函数、公共类的方法或字段
        .allowPublicAccess(true)
//允许客户端语言实现任何 Java 接口
        .allowAllImplementations(true)
//允许客户端语言实现（扩展）任何 Java 类
        .allowAllClassImplementations(true)
//允许访问数组
        .allowArrayAccess(true)
//允许访问 List
        .allowListAccess(true)
//允许客户应用程序以缓冲区元素的形式访问 ByteBuffers
        .allowBufferAccess(false)
//允许客户应用程序使用迭代器将可迭代对象作为值进行访问
        .allowIterableAccess(false)
//允许客户应用程序将迭代器作为迭代器值进行访问。
        .allowIteratorAccess(true)
//允许客户应用程序以哈希值形式访问 Map 对象
        .allowMapAccess(true)
//允许客户应用程序继承对允许方法的访问权限
        .allowAccessInheritance(false)
        .build()
}

object GraalJsUtil {

    /**
     * 每个线程缓存一个 GraalVM Context，避免重复创建（创建 Context 开销大）。
     * GraalVM Context 不是线程安全的，因此使用 ThreadLocal 确保每个线程有自己的实例。
     */
    private val contextThreadLocal = ThreadLocal.withInitial { newGraalContext() }
    private val allContexts = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<Context, Boolean>())
    private val contextsLock = Any()

    fun compile(script: String): Source? {
        return try {
            Source.newBuilder(GJS_LANG_ID, script, "script.js").build()
        } catch (e: Exception) {
            e.printStackTrace()
            null // 编译失败时返回 null
        }
    }

    fun newGraalContext(): Context {
        val dangerMode = io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.config.getBoolean("allow-danger-js", false)
        val allowedPrefixes = io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.config.getStringList("allowed-js-classes")

        val builder = Context.newBuilder(GJS_LANG_ID)
            .engine(globalGJSEngine)

        if (dangerMode) {
            // 危险模式：允许完整的 Java 运行时访问（等同于启用前的旧行为）
            builder.allowAllAccess(true)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup { true }
        } else {
            // 安全模式：仅允许白名单包名中的类通过 Java.type() 加载
            builder.allowAllAccess(false)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup { className ->
                    allowedPrefixes.any { prefix -> className.startsWith(prefix) }
                }
        }

        synchronized(contextsLock) {
            val ctx = builder.build()
            allContexts.add(ctx)
            return ctx
        }
    }


    fun directEval(script: String, vars: Map<String, Any?>): Any? {

        return executeScript(script, vars)

    }
    fun cachedEval(script: String, vars: Map<String, Any?>): Any? {

        val hash = script.generateHash()
        val source = gjsScriptCache.getOrPut(hash) {
            compile(script)
        }

        if (source == null) {
            // 编译失败
            severeL("GraalJsCompileFailed", script)
            return null
        }

        return executeScript(source, vars)

    }

    // 重入检测：记录当前线程的重入深度，防止嵌套 cleanup 破坏外层变量
    private val reentrantDepth = ThreadLocal.withInitial { 0 }

    private fun executeScript(scriptOrSource: Any, vars: Map<String, Any?>): Any? {
        val context = contextThreadLocal.get()
        val bindings: Value = context.getBindings(GJS_LANG_ID)

        val depth = reentrantDepth.get()
        reentrantDepth.set(depth + 1)

        // 保存将被子脚本覆盖的旧变量，用于重入场景的恢复
        val savedVars = if (depth > 0) {
            vars.keys.filter { bindings.hasMember(it) }.associateWith { key ->
                try { bindings.getMember(key) } catch (_: Exception) { null }
            }
        } else {
            emptyMap()
        }

        // 设置本次执行的变量
        vars.forEach { (key, value) ->
            bindings.putMember(key, value)
        }

        try {
            val result: Value = when (scriptOrSource) {
                is String -> context.eval(GJS_LANG_ID, scriptOrSource)
                is Source -> context.eval(scriptOrSource)
                else -> throw IllegalArgumentException("Unsupported script type: ${scriptOrSource::class.java}")
            }

            return result.`as`(Any::class.java)
        } finally {
            // 重入场景：恢复被覆盖的外层变量而不是删除
            if (depth > 0) {
                savedVars.forEach { (key, value) ->
                    if (value != null) bindings.putMember(key, value) else bindings.removeMember(key)
                }
                vars.keys.forEach { key ->
                    if (key !in savedVars) bindings.removeMember(key)
                }
            } else {
                // 非重入场景：正常清理
                vars.keys.forEach { key ->
                    bindings.removeMember(key)
                }
            }
            reentrantDepth.set(depth)
        }
    }

    fun createContext(): Context {
        return newGraalContext()
    }

    /**
     * 关闭所有线程的 GraalVM Context，供插件卸载时调用
     */
    fun closeCurrentContext() {
        contextThreadLocal.get()?.close()
        contextThreadLocal.remove()
        // 清理其他线程创建的 Context（加锁防止并发创建）
        synchronized(contextsLock) {
            for (ctx in allContexts) {
                try { ctx.close() } catch (_: Exception) {}
            }
            allContexts.clear()
        }
    }

    private fun createScriptSource(script: String, cached: Boolean = true): Source {
        return Source.newBuilder(GJS_LANG_ID, script, "<eval>").cached(cached).build()
    }

}

fun String.generateHash(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
}
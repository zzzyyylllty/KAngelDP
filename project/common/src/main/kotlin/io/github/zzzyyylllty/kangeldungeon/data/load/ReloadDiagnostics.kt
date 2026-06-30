package io.github.zzzyyylllty.kangeldungeon.data.load

/**
 * 重载诊断收集器，在 reload 执行期间收集配置错误/警告，
 * 完成后一次性报告给命令发送者。
 */
object ReloadDiagnostics {

    enum class Severity { ERROR, WARNING }

    data class Issue(val severity: Severity, val key: String, val args: List<String> = emptyList())

    private val issues = mutableListOf<Issue>()
    private val lock = Any()

    fun error(key: String, vararg args: String) {
        synchronized(lock) { issues.add(Issue(Severity.ERROR, key, args.toList())) }
    }

    fun warn(key: String, vararg args: String) {
        synchronized(lock) { issues.add(Issue(Severity.WARNING, key, args.toList())) }
    }

    fun hasIssues(): Boolean = synchronized(lock) { issues.isNotEmpty() }

    fun collect(): List<Issue> = synchronized(lock) {
        val snapshot = issues.toList()
        issues.clear()
        snapshot
    }

    fun clear() { synchronized(lock) { issues.clear() } }
}

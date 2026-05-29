package io.github.zzzyyylllty.kangeldungeon.data.load

/**
 * 重载诊断收集器，在 reload 执行期间收集配置错误/警告，
 * 完成后一次性报告给命令发送者。
 */
object ReloadDiagnostics {

    enum class Severity { ERROR, WARNING }

    data class Issue(val severity: Severity, val key: String, val args: List<String> = emptyList())

    private val issues = mutableListOf<Issue>()

    fun error(key: String, vararg args: String) {
        issues.add(Issue(Severity.ERROR, key, args.toList()))
    }

    fun warn(key: String, vararg args: String) {
        issues.add(Issue(Severity.WARNING, key, args.toList()))
    }

    fun hasIssues(): Boolean = issues.isNotEmpty()

    fun collect(): List<Issue> {
        val snapshot = issues.toList()
        issues.clear()
        return snapshot
    }

    fun clear() { issues.clear() }
}

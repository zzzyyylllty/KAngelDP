package io.github.zzzyyylllty.kangeldungeon.util

import io.github.zzzyyylllty.kangeldungeon.logger.warningL
import java.io.File

fun deleteDirectory(directory: File, maxRetries: Int = 5): Boolean {
    if (!directory.exists()) return true

    if (directory.isDirectory) {
        directory.listFiles()?.forEach { file ->
            deleteDirectory(file, maxRetries)
        }
    }

    var retries = 0
    while (retries < maxRetries) {
        if (directory.delete()) return true
        retries++
        if (retries < maxRetries) {
            try {
                Thread.sleep(200)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    if (directory.exists()) {
        warningL("WarningDeleteDirectoryFailed", directory.name)
    }
    return !directory.exists()
}

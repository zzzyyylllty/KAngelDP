package io.github.zzzyyylllty.kangeldungeon.util

import io.github.zzzyyylllty.kangeldungeon.logger.debugS
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon.devMode
import taboolib.common.platform.function.submitAsync

fun devLog(input: String) {
    submitAsync { if (devMode) debugS(input) }
}

fun devMode(b: Boolean) {
    devMode = b
}
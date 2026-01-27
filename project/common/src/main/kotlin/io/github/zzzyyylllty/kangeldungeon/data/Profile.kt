package io.github.zzzyyylllty.kangeldungeon.data

import io.github.zzzyyylllty.kangeldungeon.function.kether.getBukkitPlayer
import taboolib.expansion.getDataContainer
import taboolib.module.kether.ScriptFrame


fun ScriptFrame.getProfile(): PlayerDataManager {
    val player = this.getBukkitPlayer()
    val data = player.getDataContainer()
    return PlayerDataManager(data, player)
}
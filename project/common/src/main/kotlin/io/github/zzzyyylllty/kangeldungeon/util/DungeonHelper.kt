package io.github.zzzyyylllty.kangeldungeon.util

import org.bukkit.entity.Player
import org.serverct.ersha.AttributePlus
import java.util.UUID

object DungeonHelper {
    fun getWorldName(dungeonName: String, dungeonUUID: UUID): String {
        return "KAngelD_${dungeonName}_$dungeonUUID"
    }
}
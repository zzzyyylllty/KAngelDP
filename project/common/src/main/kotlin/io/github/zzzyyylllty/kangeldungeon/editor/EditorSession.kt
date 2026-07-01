package io.github.zzzyyylllty.kangeldungeon.editor

import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import io.github.zzzyyylllty.kangeldungeon.editor.util.langMsg
import org.bukkit.Location
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-player editor session state.
 * Tracks what file is being edited and the current data map.
 */
class EditorSession(val player: Player) {

    /** The dungeon being edited */
    var dungeonName: String? = null
        private set

    /** The current YAML file being edited */
    var currentFile: File? = null
        private set

    /** The current YAML data (mutable map for in-place editing) */
    var currentData: MutableMap<String, Any?>? = null
        private set

    /** Whether the data has unsaved changes */
    var dirty: Boolean = false

    // ===== Point Selection =====

    /** First selection point */
    var pos1: Location? = null

    /** Second selection point */
    var pos2: Location? = null

    /** Whether the player is holding the selection wand */
    var wandActive: Boolean = false

    /** Currently selected wand target slot callback */
    var wandCallback: ((Location) -> Unit)? = null

    fun enterDungeon(name: String) {
        dungeonName = name
        currentFile = null
        currentData = null
        dirty = false
    }

    fun loadFile(file: File) {
        currentFile = file
        currentData = YamlIO.loadYaml(file)
        dirty = false
    }

    fun markDirty() {
        dirty = true
    }

    fun save() {
        val file = currentFile ?: return
        val data = currentData ?: return
        YamlIO.saveYaml(file, data)
        dirty = false
        player.langMsg("session.saved", file.name)
    }

    fun exit() {
        if (dirty) {
            player.langMsg("session.discarded")
        }
        dungeonName = null
        currentFile = null
        currentData = null
        dirty = false
        pos1 = null
        pos2 = null
        wandActive = false
        wandCallback = null
    }

    /** Format a location as "x y z" string for YAML storage */
    fun formatPos(loc: Location): String = "${loc.blockX} ${loc.blockY} ${loc.blockZ}"

    /** Format a location as "x y z world" string */
    fun formatPosWithWorld(loc: Location): String = "${loc.blockX} ${loc.blockY} ${loc.blockZ} ${loc.world.name}"

    /** Set pos1 and notify player */
    fun setPosition1(loc: Location) {
        pos1 = loc
        player.langMsg("wand.pos1Set", formatPos(loc))
    }

    /** Set pos2 and notify player */
    fun setPosition2(loc: Location) {
        pos2 = loc
        player.langMsg("wand.pos2Set", formatPos(loc))
    }

    companion object {
        private val sessions = ConcurrentHashMap<UUID, EditorSession>()

        fun get(player: Player): EditorSession {
            return sessions.getOrPut(player.uniqueId) { EditorSession(player) }
        }

        fun remove(player: Player) {
            sessions.remove(player.uniqueId)
        }

        fun getAll(): Collection<EditorSession> = sessions.values
    }
}

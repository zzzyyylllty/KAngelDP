package io.github.zzzyyylllty.kangeldungeon.editor

import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
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
        player.sendMessage("§aSaved to ${file.name}!")
    }

    fun exit() {
        if (dirty) {
            player.sendMessage("§eUnsaved changes discarded.")
        }
        dungeonName = null
        currentFile = null
        currentData = null
        dirty = false
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

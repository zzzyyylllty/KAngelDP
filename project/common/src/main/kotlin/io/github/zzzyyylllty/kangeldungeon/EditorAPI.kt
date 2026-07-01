package io.github.zzzyyylllty.kangeldungeon

import io.github.zzzyyylllty.kangeldungeon.editor.EditorCategory
import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import org.bukkit.entity.Player
import java.io.File

interface EditorAPI {

    // ===== Screen Navigation =====

    /** Open the main editor menu for a player. */
    fun openEditor(player: Player)

    /** Open the category menu for a specific dungeon. */
    fun openDungeon(player: Player, dungeonName: String)

    /** Open a specific config category editor for a dungeon. */
    fun openCategory(player: Player, dungeonName: String, category: EditorCategory)

    // ===== Data Access =====

    /** Load a YAML file into a mutable map. */
    fun loadYaml(file: File): MutableMap<String, Any?>

    /** Save a map to a YAML file with pretty printing. */
    fun saveYaml(file: File, data: Map<String, Any?>)

    /** Get the root folder for a dungeon's config files. */
    fun getDungeonFolder(name: String): File

    /** Get a config file path within a dungeon's category subdirectory. */
    fun getDungeonFile(dungeonName: String, category: String, fileName: String): File

    /** List all available dungeon names. */
    fun listDungeons(): List<String>

    /** List all YAML files in a dungeon's category directory. */
    fun listYamlFiles(dungeonName: String, category: String): List<File>

    // ===== Session Management =====

    /** Get or create an editor session for a player. */
    fun getSession(player: Player): EditorSession

    /** Remove a player's editor session. */
    fun removeSession(player: Player)

    /** Get all active editor sessions. */
    fun getAllSessions(): Collection<EditorSession>

    // ===== Dungeon Lifecycle =====

    /** Create a new dungeon with default directory structure. Returns false if already exists. */
    fun createDungeon(name: String): Boolean

    /** Delete a dungeon and all its files. Returns false if not found. */
    fun deleteDungeon(name: String): Boolean

    // ===== Convenience Config Access =====

    /** Load a dungeon's option.yml as a mutable map. */
    fun getDungeonOptions(name: String): MutableMap<String, Any?>?

    /** Save data to a dungeon's option.yml. */
    fun setDungeonOptions(name: String, data: Map<String, Any?>)
}

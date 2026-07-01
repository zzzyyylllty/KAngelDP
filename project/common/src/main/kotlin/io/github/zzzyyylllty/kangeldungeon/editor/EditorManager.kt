package io.github.zzzyyylllty.kangeldungeon.editor

import io.github.zzzyyylllty.kangeldungeon.EditorAPI
import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.editor.screens.CategoryMenu
import io.github.zzzyyylllty.kangeldungeon.editor.screens.DifficultyEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.DungeonSettings
import io.github.zzzyyylllty.kangeldungeon.editor.screens.InteractEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.KitEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.LootEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.MainMenu
import io.github.zzzyyylllty.kangeldungeon.editor.screens.MonsterEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.ObstacleEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.PlanEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.RegionEditor
import io.github.zzzyyylllty.kangeldungeon.editor.screens.TaskEditor
import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import org.bukkit.entity.Player
import java.io.File

object EditorManager : EditorAPI {

    // ===== Screen Navigation =====

    override fun openEditor(player: Player) {
        MainMenu.open(player)
    }

    override fun openDungeon(player: Player, dungeonName: String) {
        EditorSession.get(player).enterDungeon(dungeonName)
        CategoryMenu.open(player, dungeonName)
    }

    override fun openCategory(player: Player, dungeonName: String, category: EditorCategory) {
        EditorSession.get(player).enterDungeon(dungeonName)
        val opener: (Player, String) -> Unit = when (category) {
            EditorCategory.SETTINGS -> { p, d -> DungeonSettings.open(p, d) }
            EditorCategory.MONSTER -> { p, d -> MonsterEditor.openList(p, d) }
            EditorCategory.OBSTACLE -> { p, d -> ObstacleEditor.openList(p, d) }
            EditorCategory.REGION -> { p, d -> RegionEditor.openList(p, d) }
            EditorCategory.INTERACT -> { p, d -> InteractEditor.openList(p, d) }
            EditorCategory.PLAN -> { p, d -> PlanEditor.openList(p, d) }
            EditorCategory.KIT -> { p, d -> KitEditor.openList(p, d) }
            EditorCategory.TASK -> { p, d -> TaskEditor.openList(p, d) }
            EditorCategory.LOOT -> { p, d -> LootEditor.openList(p, d) }
            EditorCategory.DIFFICULTY -> { p, d -> DifficultyEditor.openList(p, d) }
        }
        opener(player, dungeonName)
    }

    // ===== Data Access =====

    override fun loadYaml(file: File): MutableMap<String, Any?> = YamlIO.loadYaml(file)

    override fun saveYaml(file: File, data: Map<String, Any?>) = YamlIO.saveYaml(file, data)

    override fun getDungeonFolder(name: String): File = YamlIO.dungeonFolder(name)

    override fun getDungeonFile(dungeonName: String, category: String, fileName: String): File =
        YamlIO.dungeonFile(dungeonName, category, fileName)

    override fun listDungeons(): List<String> = YamlIO.listDungeons()

    override fun listYamlFiles(dungeonName: String, category: String): List<File> =
        YamlIO.listYamlFiles(dungeonName, category)

    // ===== Session Management =====

    override fun getSession(player: Player): EditorSession = EditorSession.get(player)

    override fun removeSession(player: Player) = EditorSession.remove(player)

    override fun getAllSessions(): Collection<EditorSession> = EditorSession.getAll()

    // ===== Dungeon Lifecycle =====

    override fun createDungeon(name: String): Boolean {
        val dir = YamlIO.dungeonFolder(name)
        if (dir.exists()) return false
        dir.mkdirs()
        listOf("monster", "obstacle", "region", "interact", "plan", "kit", "task", "loot", "script", "difficulty").forEach {
            File(dir, it).mkdir()
        }
        val defaultOption = linkedMapOf<String, Any?>(
            "display" to linkedMapOf(
                "name" to name,
                "icon" to linkedMapOf("material" to "DIAMOND_SWORD"),
                "description" to emptyList<String>()
            ),
            "map" to linkedMapOf(
                "type" to "MAP",
                "source" to name,
                "spawn" to linkedMapOf("x" to 0, "y" to 100, "z" to 0)
            ),
            "gameplay" to linkedMapOf(
                "general" to linkedMapOf(
                    "timeLimit" to 3600,
                    "preparationTime" to 30,
                    "adventureMode" to true,
                    "minPlayers" to 1,
                    "maxPlayers" to 5,
                    "allowParty" to true,
                    "pvpEnabled" to false
                )
            )
        )
        YamlIO.saveYaml(File(dir, "option.yml"), defaultOption)
        KAngelDungeon.reloadCustomConfig(async = true)
        return true
    }

    override fun deleteDungeon(name: String): Boolean {
        val dir = YamlIO.dungeonFolder(name)
        if (!dir.exists()) return false
        dir.deleteRecursively()
        KAngelDungeon.reloadCustomConfig(async = true)
        return true
    }

    // ===== Convenience Config Access =====

    override fun getDungeonOptions(name: String): MutableMap<String, Any?>? {
        val file = YamlIO.dungeonFile(name, "", "option.yml")
        if (!file.exists()) return null
        return YamlIO.loadYaml(file)
    }

    override fun setDungeonOptions(name: String, data: Map<String, Any?>) {
        YamlIO.saveYaml(YamlIO.dungeonFile(name, "", "option.yml"), data)
        KAngelDungeon.reloadCustomConfig(async = true)
    }
}

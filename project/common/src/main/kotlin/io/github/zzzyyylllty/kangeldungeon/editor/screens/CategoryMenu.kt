package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Chest

/**
 * Category selection screen — shows all config categories for a dungeon.
 */
object CategoryMenu {

    data class Category(
        val id: String,
        val name: String,
        val material: Material,
        val screenOpener: (Player, String) -> Unit
    )

    private val categories = listOf(
        Category("settings", "Dungeon Settings", Material.COMMAND_BLOCK, { p, d -> DungeonSettings.open(p, d) }),
        Category("monster", "Monster Groups", Material.ZOMBIE_HEAD, { p, d -> MonsterEditor.openList(p, d) }),
        Category("obstacle", "Obstacles", Material.IRON_DOOR, { p, d -> ObstacleEditor.openList(p, d) }),
        Category("region", "Regions", Material.FILLED_MAP, { p, d -> RegionEditor.openList(p, d) }),
        Category("interact", "Interactions", Material.LEVER, { p, d -> InteractEditor.openList(p, d) }),
        Category("plan", "Plans", Material.CLOCK, { p, d -> PlanEditor.openList(p, d) }),
        Category("kit", "Kits", Material.CHEST, { p, d -> KitEditor.openList(p, d) }),
        Category("task", "Tasks", Material.TRIPWIRE_HOOK, { p, d -> TaskEditor.openList(p, d) }),
        Category("loot", "Loot Chests", Material.BARREL, { p, d -> LootEditor.openList(p, d) }),
        Category("difficulty", "Difficulty Presets", Material.NETHER_STAR, { p, d -> DifficultyEditor.openList(p, d) })
    )

    fun open(player: Player, dungeonName: String) {
        val dir = YamlIO.dungeonFolder(dungeonName)
        val hasOption = YamlIO.dungeonFile(dungeonName, "", "option.yml").exists()

        player.openMenu<Chest>("§8Edit: $dungeonName") {
            rows(6)
            GuiItems.BORDER_SLOTS_6ROW.forEach { set(it, GuiItems.border()) }

            // Row 0: border
            for (i in 0..8) set(i, GuiItems.border())

            // Place category items in rows 1-4 (slots 9-44)
            categories.forEachIndexed { index, cat ->
                val slot = 9 + index
                val count = countEntries(dungeonName, cat.id)
                set(slot, GuiItems.sectionItem(cat.material, cat.name, count))
            }

            // Back button (bottom-right)
            set(49, GuiItems.backButton())

            // Dungeon name display
            set(53, GuiItems.buildItem(Material.GRASS_BLOCK) {
                name = "<gold>$dungeonName</gold>"
                lore(
                    "<gray>Config: <${if (hasOption) "green" else "red"}>${if (hasOption) "OK" else "missing option.yml"}",
                    "",
                    "<gray><italic>Click to reload dungeons"
                )
            })

            onClick(lock = true) { event ->
                val slot = event.rawSlot
                when {
                    slot in 9 until (9 + categories.size) -> {
                        val cat = categories[slot - 9]
                        cat.screenOpener.invoke(player, dungeonName)
                    }
                    slot == 49 -> MainMenu.open(player)
                    slot == 53 -> MainMenu.open(player)
                }
            }

            handLocked(true)
        }
    }

    private fun countEntries(dungeonName: String, category: String): Int {
        if (category == "settings") return 1
        return YamlIO.listYamlFiles(dungeonName, category).size
    }
}

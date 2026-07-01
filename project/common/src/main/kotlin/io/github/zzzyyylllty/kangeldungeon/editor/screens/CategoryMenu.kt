package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import io.github.zzzyyylllty.kangeldungeon.editor.util.lang
import io.github.zzzyyylllty.kangeldungeon.editor.util.langStr
import io.github.zzzyyylllty.kangeldungeon.editor.util.langMsg
import net.kyori.adventure.text.Component
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
        val session = EditorSession.get(player)
        session.enterDungeon(dungeonName)

        player.openMenu<Chest>(player.langStr("title.category", dungeonName)) {
            rows(6)
            GuiItems.BORDER_SLOTS_6ROW.forEach { set(it, GuiItems.border()) }

            // Place category items in rows 1-4 (slots 9-44)
            categories.forEachIndexed { index, cat ->
                val slot = 9 + index
                val count = countEntries(dungeonName, cat.id)
                set(slot, GuiItems.compItem(cat.material, player.lang("category.${cat.id}"), listOf(
                    player.lang("common.entries", count.toString()),
                    Component.empty(),
                    player.lang("common.clickEdit")
                )))
            }

            // Back button
            set(49, GuiItems.backButton())

            // Dungeon name display
            set(53, GuiItems.compItem(Material.GRASS_BLOCK, player.lang("category.dungeonName", dungeonName), listOf(
                player.lang("category.configStatus", if (hasOption) player.langStr("category.configOK") else player.langStr("category.configMissing")),
                Component.empty(),
                player.lang("category.clickReload")
            )))

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

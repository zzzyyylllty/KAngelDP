package io.github.zzzyyylllty.kangeldungeon.editor.screens

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import io.github.zzzyyylllty.kangeldungeon.editor.util.GuiItems
import io.github.zzzyyylllty.kangeldungeon.editor.util.InputPrompts
import io.github.zzzyyylllty.kangeldungeon.editor.util.YamlIO
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import java.io.File

/**
 * Main menu — lists all dungeons with create/edit/delete.
 */
object MainMenu {

    fun open(player: Player) {
        val dungeons = YamlIO.listDungeons()

        player.openMenu<PageableChest<String>>("§8KAngelDungeon Editor") {
            rows(6)
            map(
                "#########",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "#@@@@@@@#",
                "L##G A###"
            )
            slotsBy('@')
            set('#', GuiItems.border())

            elements { dungeons }

            onGenerate { _, dungeonName, _, _ ->
                val optionFile = YamlIO.dungeonFile(dungeonName, "", "option.yml")
                GuiItems.dungeonItem(dungeonName, optionFile.exists())
            }

            onClick { event, dungeonName ->
                val session = EditorSession.get(player)
                if (event.clickEvent().isShiftClick) {
                    // Delete dungeon
                    InputPrompts.confirmDialog(player, "Delete Dungeon", "Delete '$dungeonName' and all files?", {
                        val dir = YamlIO.dungeonFolder(dungeonName)
                        if (dir.exists()) dir.deleteRecursively()
                        KAngelDungeon.reloadCustomConfig(async = true)
                        player.sendMessage("§cDungeon '$dungeonName' deleted!")
                        open(player)
                    })
                } else {
                    session.enterDungeon(dungeonName)
                    CategoryMenu.open(player, dungeonName)
                }
            }

            // "Create New Dungeon" button at slot 'A'
            onClick(getSlot('A')) {
                InputPrompts.textInput(player, "New Dungeon Name", null) { name ->
                    if (!name.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                        player.sendMessage("§cInvalid name! Use letters, numbers, hyphens, underscores only.")
                        return@textInput
                    }
                    val dir = YamlIO.dungeonFolder(name)
                    if (dir.exists()) {
                        player.sendMessage("§cDungeon '$name' already exists!")
                        open(player)
                        return@textInput
                    }

                    // Create directory structure
                    dir.mkdirs()
                    listOf("monster", "obstacle", "region", "interact", "plan", "kit", "task", "loot", "script", "difficulty").forEach {
                        File(dir, it).mkdir()
                    }

                    // Create default option.yml
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
                    player.sendMessage("§aDungeon '$name' created!")
                    open(player)
                }
            }

            // "Global Configs" button
            onClick(getSlot('G')) {
                GlobalConfigs.open(player)
            }

            // "Info" button at slot 'L'
            onClick(getSlot('L')) {
                player.sendMessage("")
                player.sendMessage("§6§l=== KAngelDungeon Editor ===")
                player.sendMessage("§eClick a dungeon to edit it.")
                player.sendMessage("§eShift+click to delete a dungeon.")
                player.sendMessage("§eClick '✚ Add New' to create a new dungeon.")
                player.sendMessage("§eJS script editing is not available in the editor.")
                player.sendMessage("")
            }

            set(getSlot('A'), GuiItems.addButton())
            set(getSlot('G'), GuiItems.buildItem(Material.COMPARATOR) {
                name = "<gold>Global Configs</gold>"
                lore(
                    "<gray>Edit global kits and loot chests",
                    "",
                    "<gray><italic>Click to open"
                )
            })
            set(getSlot('L'), GuiItems.buildItem(Material.COMPASS) {
                name = "<aqua>Info / Help</aqua>"
            })

            handLocked(true)
        }
    }

    private fun getSlot(c: Char): Int = when (c) {
        'L' -> 45
        'G' -> 48
        'A' -> 50
        else -> 0
    }
}

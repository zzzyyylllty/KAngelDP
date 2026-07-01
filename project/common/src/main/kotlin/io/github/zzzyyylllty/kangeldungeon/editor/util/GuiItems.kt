package io.github.zzzyyylllty.kangeldungeon.editor.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * Reusable GUI item builders and layout constants for the visual editor.
 */
object GuiItems {

    private val mm = MiniMessage.miniMessage()

    // ============ Navigation Items ============

    fun border(): ItemStack = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ")

    fun backButton(): ItemStack = namedItem(Material.ARROW, "<gray>← Back")

    fun saveButton(): ItemStack = namedItem(Material.LIME_DYE, "<green>✔ Save Changes")

    fun addButton(): ItemStack = namedItem(Material.EMERALD, "<green>✚ Add New")

    fun deleteButton(): ItemStack = namedItem(Material.BARRIER, "<red>✘ Delete")

    fun nextPage(): ItemStack = namedItem(Material.SPECTRAL_ARROW, "<white>Next Page →")

    fun prevPage(): ItemStack = namedItem(Material.SPECTRAL_ARROW, "<white>← Previous Page")

    fun infoItem(text: String): ItemStack = namedItem(Material.BOOK, "<aqua>$text")

    fun noEntries(): ItemStack = namedItem(Material.PAPER, "<gray><italic>No entries yet")

    fun warningItem(text: String): ItemStack = namedItem(Material.PAPER, "<yellow>$text")

    // ============ Field Display Items ============

    /**
     * Generic field display item showing label and current value.
     */
    fun fieldItem(material: Material, label: String, value: Any?): ItemStack {
        val displayName = "<yellow>$label</yellow>"
        val valueStr = when {
            value == null -> "<gray><italic>none"
            value is String && value.isEmpty() -> "<gray><italic>empty"
            value is Map<*, *> && value.isEmpty() -> "<gray><italic>empty map"
            value is List<*> && value.isEmpty() -> "<gray><italic>empty list"
            value is String -> truncate(value, 50)
            else -> value.toString()
        }
        return buildItem(material) {
            name = displayName
            lore(
                "<gray>Current: <white>$valueStr",
                "",
                "<gray><italic>Click to edit"
            )
        }
    }

    /**
     * Boolean toggle item (green/red dye).
     */
    fun toggleItem(label: String, value: Boolean): ItemStack {
        val mat = if (value) Material.LIME_DYE else Material.RED_DYE
        return buildItem(mat) {
            name = "<yellow>$label</yellow>"
            lore(
                "<gray>Current: <${if (value) "green" else "red"}>$value",
                "",
                "<gray><italic>Click to toggle"
            )
        }
    }

    /**
     * Enum selector item.
     */
    fun enumItem(label: String, current: String?, allValues: List<String>): ItemStack {
        val display = current ?: "default"
        return buildItem(Material.BOOK) {
            name = "<yellow>$label</yellow>"
            _lore = (mutableListOf(
                "<gray>Current: <white>$display",
                "",
                "<gray>Options:"
            ) + allValues.map { "  <dark_gray>- <gray>$it" } + listOf(
                "",
                "<gray><italic>Click to change"
            )).toMutableList()
        }
    }

    /**
     * Number field item with +/- quick adjust hint.
     */
    fun numberItem(label: String, value: Number?, suffix: String = ""): ItemStack {
        val display = value?.toString() ?: "0"
        return buildItem(Material.REPEATER) {
            name = "<yellow>$label</yellow>"
            lore(
                "<gray>Current: <white>$display$suffix",
                "",
                "<gray><italic>Left-click: edit",
                "<gray><italic>Right-click: reset",
                "<gray><italic>Shift+click: +10"
            )
        }
    }

    /**
     * Script placeholder — shows a non-interactive barrier item for JS script fields.
     */
    fun scriptPlaceholder(fieldName: String): ItemStack {
        return buildItem(Material.BARRIER) {
            name = "<red>$fieldName (JS Script)</red>"
            lore(
                "<red>Script editing not available</red>",
                "<red>in the in-game editor.</red>",
                "",
                "<dark_gray>Edit the YAML file directly",
                "<dark_gray>or use a text editor."
            )
        }
    }

    /**
     * Section entry item for category lists.
     */
    fun sectionItem(material: Material, name: String, count: Int): ItemStack {
        return buildItem(material) {
            this.name = "<gold>$name</gold>"
            lore(
                "<gray>Entries: <white>$count",
                "",
                "<gray><italic>Click to edit"
            )
        }
    }

    /**
     * Dungeon entry for the main menu.
     */
    fun dungeonItem(name: String, hasOption: Boolean): ItemStack {
        val mat = if (hasOption) Material.GRASS_BLOCK else Material.STRUCTURE_VOID
        return buildItem(mat) {
            this.name = "<gold>$name</gold>"
            lore(
                "<gray>Status: <${if (hasOption) "green" else "red"}>${if (hasOption) "configured" else "incomplete"}",
                "",
                "<gray><italic>Click to edit"
            )
        }
    }

    // ============ Layout Constants ============

    /** All border slots for a 6-row chest (slots 0-53) */
    val BORDER_SLOTS_6ROW: List<Int> = (0..53).filter { i ->
        i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8
    }

    // ============ Internal Helpers ============

    private fun truncate(s: String, max: Int): String {
        return if (s.length <= max) s else s.take(max) + "..."
    }

    fun namedItem(material: Material, displayName: String): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(mm.deserialize(displayName))
        }
        return item
    }

    fun buildItem(material: Material, block: ItemMetaBuilder.() -> Unit): ItemStack {
        val item = ItemStack(material)
        val builder = ItemMetaBuilder(item)
        builder.block()
        if (builder._name != null) {
            item.editMeta { meta ->
                meta.displayName(mm.deserialize(builder._name!!))
                if (builder._lore.isNotEmpty()) {
                    meta.lore(builder._lore.map { mm.deserialize(it) })
                }
            }
        }
        return item
    }

    class ItemMetaBuilder(private val item: ItemStack) {
        var _name: String? = null
        var _lore: MutableList<String> = mutableListOf()

        var name: String
            get() = _name ?: ""
            set(value) { _name = value }

        fun lore(vararg lines: String) {
            _lore.addAll(lines)
        }

        fun lore(lines: List<String>) {
            _lore.addAll(lines)
        }
    }
}

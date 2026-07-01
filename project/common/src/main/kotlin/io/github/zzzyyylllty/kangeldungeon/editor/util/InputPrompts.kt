package io.github.zzzyyylllty.kangeldungeon.editor.util

import io.github.zzzyyylllty.kangeldungeon.KAngelDungeon
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Anvil
import taboolib.module.ui.type.Chest
import taboolib.module.ui.type.PageableChest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Input prompt helpers — sign, chat, number, enum selection.
 */
object InputPrompts {

    private val chatListeners = ConcurrentHashMap<UUID, (String) -> Unit>()
    private val multilineListeners = ConcurrentHashMap<UUID, MultilineCapture>()

    private data class MultilineCapture(
        val lines: MutableList<String> = mutableListOf(),
        val callback: (String) -> Unit,
        val title: String
    )

    // ============ Anvil Text Input ============

    /**
     * Open an anvil GUI for single-line text input.
     */
    fun textInput(player: Player, title: String, current: String?, callback: (String) -> Unit) {
        player.openMenu<Anvil>(title) {
            onRename { _, text, _ ->
                val input = text?.trim() ?: return@onRename
                if (input.isNotEmpty()) {
                    callback(input)
                } else {
                    player.sendMessage("§cInput cannot be empty!")
                }
            }
        }
    }

    /**
     * Open an anvil GUI for numeric input.
     */
    fun numberInput(player: Player, title: String, current: Number?, callback: (Number) -> Unit) {
        player.openMenu<Anvil>(title) {
            onRename { _, text, _ ->
                val input = text?.trim() ?: return@onRename
                val intVal = input.toIntOrNull()
                if (intVal != null) {
                    callback(intVal)
                    return@onRename
                }
                val doubleVal = input.toDoubleOrNull()
                if (doubleVal != null) {
                    callback(doubleVal)
                    return@onRename
                }
                player.sendMessage("§cInvalid number: $input")
            }
        }
    }

    /**
     * Open an anvil GUI for integer input.
     */
    fun intInput(player: Player, title: String, current: Int?, callback: (Int) -> Unit) {
        player.openMenu<Anvil>(title) {
            onRename { _, text, _ ->
                val input = text?.trim() ?: return@onRename
                val value = input.toIntOrNull()
                if (value != null) {
                    callback(value)
                } else {
                    player.sendMessage("§cInvalid integer: $input")
                }
            }
        }
    }

    /**
     * Open an anvil GUI for decimal (double) input.
     */
    fun doubleInput(player: Player, title: String, current: Double?, callback: (Double) -> Unit) {
        player.openMenu<Anvil>(title) {
            onRename { _, text, _ ->
                val input = text?.trim() ?: return@onRename
                val value = input.toDoubleOrNull()
                if (value != null) {
                    callback(value)
                } else {
                    player.sendMessage("§cInvalid number: $input")
                }
            }
        }
    }

    // ============ Enum Selection ============

    /**
     * Open a selection menu for enum values.
     */
    fun enumSelect(player: Player, title: String, values: List<String>, current: String?, callback: (String) -> Unit) {
        player.openMenu<PageableChest<String>>(title) {
            rows(6)
            map("#########", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "#@@@@@@@#", "########")
            slotsBy('@')
            set('#', GuiItems.border())

            elements { values }

            onGenerate { _, value, _, _ ->
                val isSelected = value == current || (current == null && value == values.firstOrNull())
                GuiItems.buildItem(if (isSelected) Material.LIME_DYE else Material.GRAY_DYE) {
                    name = if (isSelected) "<green>$value ✓</green>" else "<white>$value</white>"
                }
            }

            onClick { _, value ->
                callback(value)
            }

            handLocked(true)
        }
    }

    // ============ Chat Input (single line + multiline) ============

    /**
     * Capture a single line chat message from the player.
     */
    fun chatInput(player: Player, title: String, callback: (String) -> Unit) {
        player.closeInventory()
        player.sendMessage("")
        player.sendMessage("§6§l--- $title ---")
        player.sendMessage("§7Type your input in chat.")
        player.sendMessage("")
        chatListeners[player.uniqueId] = callback
    }

    /**
     * Capture multiline text via chat. Player types lines, ends with "#done".
     */
    fun multilineInput(player: Player, title: String, current: String?, callback: (String) -> Unit) {
        player.closeInventory()
        player.sendMessage("")
        player.sendMessage("§6§l--- $title ---")
        player.sendMessage("§7Type your text line by line.")
        player.sendMessage("§7Type §e#done§7 to finish.")
        if (current != null && current.isNotEmpty()) {
            player.sendMessage("§7Current value:")
            current.lines().forEach { player.sendMessage("§8$it") }
            player.sendMessage("")
            player.sendMessage("§7Type §e#clear§7 to start fresh, or add more lines.")
        }
        player.sendMessage("")
        multilineListeners[player.uniqueId] = MultilineCapture(
            lines = if (current != null && current.isNotEmpty()) current.lines().toMutableList() else mutableListOf(),
            callback = callback,
            title = title
        )
    }

    // ============ List Input ============

    /**
     * Edit a list of strings via chat (one per line, #done to finish).
     */
    fun stringListInput(player: Player, title: String, current: List<String>?, callback: (List<String>) -> Unit) {
        player.closeInventory()
        player.sendMessage("")
        player.sendMessage("§6§l--- $title ---")
        player.sendMessage("§7Type one entry per line.")
        player.sendMessage("§7Type §e#done§7 to finish.")
        if (current != null && current.isNotEmpty()) {
            player.sendMessage("§7Current entries:")
            current.forEach { player.sendMessage("§8- $it") }
            player.sendMessage("")
            player.sendMessage("§7Type §e#clear§7 to start fresh.")
        }
        player.sendMessage("")
        multilineListeners[player.uniqueId] = MultilineCapture(
            lines = (current ?: emptyList()).toMutableList(),
            callback = { text -> callback(text.lines().filter { it.isNotBlank() }) },
            title = title
        )
    }

    /**
     * Confirm/cancel dialog.
     */
    fun confirmDialog(player: Player, title: String, message: String, onConfirm: () -> Unit) {
        player.openMenu<Chest>(title) {
            rows(3)
            set(11, GuiItems.buildItem(Material.LIME_DYE) {
                name = "<green>✔ Confirm"
                lore("<gray>$message")
            })
            set(15, GuiItems.buildItem(Material.RED_DYE) {
                name = "<red>✘ Cancel"
            })
            onClick(lock = true) { event ->
                when (event.rawSlot) {
                    11 -> {
                        player.closeInventory()
                        onConfirm()
                    }
                    15 -> player.closeInventory()
                }
            }
            handLocked(true)
        }
    }

    // ============ Event Listener ============

    @SubscribeEvent
    fun onChat(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
        val uuid = event.player.uniqueId
        val message = event.message.trim()

        // Check multiline listener first
        val mlCapture = multilineListeners[uuid]
        if (mlCapture != null) {
            event.isCancelled = true
            if (message.equals("#done", ignoreCase = true)) {
                multilineListeners.remove(uuid)
                val result = mlCapture.lines.joinToString("\n")
                event.player.sendMessage("§aInput captured! (${mlCapture.lines.size} lines)")
                mlCapture.callback(result)
                return
            }
            if (message.equals("#clear", ignoreCase = true)) {
                mlCapture.lines.clear()
                event.player.sendMessage("§eCleared! Type new lines or #done to finish.")
                return
            }
            if (message.equals("#cancel", ignoreCase = true) || message.equals("#exit", ignoreCase = true)) {
                multilineListeners.remove(uuid)
                event.player.sendMessage("§cCancelled.")
                return
            }
            mlCapture.lines.add(message)
            event.player.sendMessage("§7Line added (${mlCapture.lines.size} total). Type §e#done§7 to finish.")
            return
        }

        // Check single-line chat listener
        val listener = chatListeners[uuid]
        if (listener != null) {
            event.isCancelled = true
            chatListeners.remove(uuid)
            if (message.equals("#cancel", ignoreCase = true) || message.equals("#exit", ignoreCase = true)) {
                event.player.sendMessage("§cCancelled.")
                return
            }
            event.player.sendMessage("§aInput captured!")
            listener(message)
        }
    }
}

package io.github.zzzyyylllty.kangeldungeon.editor.util

import io.github.zzzyyylllty.kangeldungeon.editor.EditorSession
import io.github.zzzyyylllty.kangeldungeon.editor.util.langMsg
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common.platform.event.SubscribeEvent

/**
 * Selection wand tool — allows players to select positions by right-clicking blocks.
 *
 * Usage:
 *   /kangeldungeon editor wand  →  gives the player a selection wand (wooden axe)
 *   Right-click a block         →  sets pos1 (or pos2 if sneaking)
 *   Right-click air + sneak     →  clears selection
 *   Left-click air              →  shows current selection info
 *
 * When a wandCallback is set on the session, right-clicking a block will
 * trigger the callback with the clicked location (for targeted position setting).
 */
object SelectionTool {

    private const val WAND_IDENTIFIER = "§6§lSelection Wand"
    private const val WAND_LORE = "§7KAngelDungeon Selection Tool"

    /** Create a selection wand item */
    fun createWand(): ItemStack {
        val item = ItemStack(Material.WOODEN_AXE)
        item.editMeta { meta ->
            meta.displayName(net.kyori.adventure.text.Component.text(WAND_IDENTIFIER))
            meta.lore(listOf(
                net.kyori.adventure.text.Component.text(WAND_LORE),
                net.kyori.adventure.text.Component.text("§7Right-click block: set pos1"),
                net.kyori.adventure.text.Component.text("§7Shift+right-click block: set pos2"),
                net.kyori.adventure.text.Component.text("§7Left-click air: show selection"),
                net.kyori.adventure.text.Component.text("§7Shift+left-click air: clear selection")
            ))
        }
        return item
    }

    /** Check if an item is a selection wand */
    fun isWand(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.WOODEN_AXE) return false
        val meta = item.itemMeta
        if (meta == null || !meta.hasDisplayName()) return false
        val displayName = meta.displayName()
        return displayName != null && LegacyComponentSerializer.legacySection().serialize(displayName) == WAND_IDENTIFIER
    }

    /** Give a player a selection wand */
    fun giveWand(player: Player) {
        val inv = player.inventory
        // Remove existing wands
        inv.contents.filterNotNull().filter { isWand(it) }.forEach { inv.remove(it) }
        // Add new wand
        inv.addItem(createWand())
        EditorSession.get(player).wandActive = true
        player.langMsg("wand.given")
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val session = EditorSession.get(player)
        val item = event.item

        // Check wand usage
        if (isWand(item)) {
            event.isCancelled = true

            when (event.action) {
                Action.RIGHT_CLICK_BLOCK -> {
                    val clickedBlock = event.clickedBlock ?: return
                    val loc = clickedBlock.location

                    // If a wand callback is set, trigger it (for targeted pos setting from GUI)
                    val callback = session.wandCallback
                    if (callback != null) {
                        session.wandCallback = null
                        callback(loc)
                        player.langMsg("loc.captured", session.formatPos(loc))
                        return
                    }

                    if (player.isSneaking) {
                        session.setPosition2(loc)
                    } else {
                        session.setPosition1(loc)
                    }
                }
                Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                    if (player.isSneaking) {
                        session.pos1 = null
                        session.pos2 = null
                        session.wandCallback = null
                        player.langMsg("wand.selectionCleared")
                    } else {
                        val p1 = session.pos1
                        val p2 = session.pos2
                        player.langMsg("wand.selHeader")
                        if (p1 != null) player.langMsg("wand.pos1Label", session.formatPosWithWorld(p1))
                        else player.langMsg("wand.notSet", "1")
                        if (p2 != null) player.langMsg("wand.pos2Label", session.formatPosWithWorld(p2))
                        else player.langMsg("wand.notSet", "2")
                        if (p1 != null && p2 != null) {
                            val w = kotlin.math.abs(p1.blockX - p2.blockX) + 1
                            val h = kotlin.math.abs(p1.blockY - p2.blockY) + 1
                            val d = kotlin.math.abs(p1.blockZ - p2.blockZ) + 1
                            player.langMsg("wand.size", w.toString(), h.toString(), d.toString(), (w * h * d).toString())
                        }
                    }
                }
                else -> {}
            }
            return
        }

        // Handle wand callback even without the wand (used by GUI buttons)
        // If player right-clicks a block while a wand callback is pending and has no wand
        if (event.action == Action.RIGHT_CLICK_BLOCK && session.wandCallback != null) {
            val clickedBlock = event.clickedBlock ?: return
            val loc = clickedBlock.location
            val callback = session.wandCallback
            session.wandCallback = null
            callback?.invoke(loc)
            player.langMsg("loc.captured", session.formatPos(loc))
            event.isCancelled = true
        }
    }
}

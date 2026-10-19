package me.char321.sfadvancements.core.gui;

import me.char321.sfadvancements.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author char321
 */
public class AdvGUIManager implements Listener {
    private final Map<UUID, OpenGUI> guis = new HashMap<>();

    public void displayGUI(Player p) {
        OpenGUI gui = getByPlayer(p);
        p.openInventory(gui.getInventory());
    }

    /**
     * @implNote Only a player who opened the gui has one, so a click elsewhere never builds a gui, whose stats
     *           head would look the player's profile up.
     */
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        final Player player = (Player) e.getWhoClicked();
        OpenGUI openGUI = guis.get(player.getUniqueId());
        if (openGUI != null && e.getInventory().equals(openGUI.getInventory())) {
            e.setCancelled(true);
            Utils.runSync(() -> openGUI.click(player, e.getRawSlot()));
        }
    }

    public OpenGUI getByPlayer(Player p) {
        UUID uuid = p.getUniqueId();
        guis.computeIfAbsent(uuid, OpenGUI::new);
        return guis.get(uuid);
    }

    /**
     * finds out whether a player has the advancements gui open
     *
     * @param p the player
     * @return whether the player has the advancements gui open
     */
    public boolean isOpen(Player p) {
        OpenGUI openGUI = guis.get(p.getUniqueId());
        return openGUI != null && p.getOpenInventory().getTopInventory().equals(openGUI.getInventory());
    }
}

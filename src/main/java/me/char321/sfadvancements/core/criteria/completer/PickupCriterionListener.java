package me.char321.sfadvancements.core.criteria.completer;

import me.char321.sfadvancements.util.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Isolated pickup handler for {@link InventoryCriterionCompleter}. {@link EntityPickupItemEvent} only
 * exists on MC 1.12+, and Bukkit refuses to register an entire Listener whose handler references an
 * unloadable event type. Keeping this handler in its own class lets it be registered behind a
 * {@code Class.forName} guard while the completer's other handlers stay active on all versions.
 */
public class PickupCriterionListener implements Listener {

    private final InventoryCriterionCompleter completer;

    public PickupCriterionListener(InventoryCriterionCompleter completer) {
        this.completer = completer;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventory(EntityPickupItemEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) {
            Utils.runLater(() -> completer.onInventory1((Player) entity), 1L);
        }
    }
}

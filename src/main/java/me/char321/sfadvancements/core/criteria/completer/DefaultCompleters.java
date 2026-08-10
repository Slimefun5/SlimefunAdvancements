package me.char321.sfadvancements.core.criteria.completer;

import me.char321.sfadvancements.SFAdvancements;
import org.bukkit.Bukkit;

/**
 * @author char321
 */
public class DefaultCompleters {

    private DefaultCompleters() {

    }

    public static void registerDefaultCompleters() {
        new InteractCriterionCompleter().register();
        InventoryCriterionCompleter inventoryCompleter = new InventoryCriterionCompleter();
        inventoryCompleter.register();
        registerPickupListener(inventoryCompleter);
        new PlaceCriterionCompleter().register();
        new ResearchCriterionCompleter().register();
        new MultiBlockCriterionCompleter().register();
        new ConsumeCriterionCompleter().register();
        new MobKillCriterionCompleter().register();
        new SearchCriterionCompleter().register();
        new BlockBreakCriterionCompleter().register();

        if (SFAdvancements.instance().isMultiBlockCraftEvent()) {
            new MultiBlockCraftCriterionCompleter().register();
        }
    }

    /**
     * @implNote {@link PickupCriterionListener} is registered behind a {@code Class.forName} guard so the
     * 1.12+-only pickup event cannot fail the completer's event registration on older versions.
     */
    private static void registerPickupListener(InventoryCriterionCompleter completer) {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            Bukkit.getPluginManager().registerEvents(new PickupCriterionListener(completer), SFAdvancements.instance());
        } catch (ClassNotFoundException ignored) {
            // 1.8-1.11: no EntityPickupItemEvent, pickup-based advancement tracking is unavailable.
        }
    }
}

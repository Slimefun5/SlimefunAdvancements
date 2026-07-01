package me.char321.sfadvancements.vanilla;

import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/**
 * sync progress if someone uses /advancement grant ...
 *
 */
/**
 * @author char321
 */
public class AdvancementListener implements Listener {
    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        org.bukkit.NamespacedKey bukkitKey = e.getAdvancement().getKey();
        NamespacedKey key = new NamespacedKey(bukkitKey.getNamespace(), bukkitKey.getKey());
        if (!Utils.keyIsSFA(key)) return;

        Advancement advancement = Utils.fromKey(key);
        if (advancement == null) return;

        Player player = e.getPlayer();
        if (SFAdvancements.getAdvManager().isCompleted(player, advancement)) return;

        advancement.complete(player);
    }
}
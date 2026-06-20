package me.char321.sfadvancements.core;

import io.github.thebusybiscuit.slimefun5.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun5.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun5.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun5.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun5.libraries.xseries.XMaterial;
import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.util.MaterialCompat;
import org.bukkit.entity.Player;

/**
 * @author char321
 */
public class AdvancementsItemGroup extends FlexItemGroup {

    public static void init(SFAdvancements plugin) {
        if (SFAdvancements.getMainConfig().getConfiguration().getBoolean("add-advancements-to-guide")) {
            AdvancementsItemGroup itemGroup = new AdvancementsItemGroup();
            itemGroup.setTheme("misc");
            itemGroup.register(plugin);
        }
    }

    public AdvancementsItemGroup() {
        super(
                new io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey("sfadvancements", "advancements"),
                CustomItemStack.create(MaterialCompat.safe(XMaterial.FILLED_MAP), "&9Advancements"),
                -1);
    }

    @Override
    public boolean isVisible(Player p, PlayerProfile profile, SlimefunGuideMode layout) {
        return true;
    }

    @Override
    public void open(Player p, PlayerProfile profile, SlimefunGuideMode layout) {
        SFAdvancements.getGuiManager().displayGUI(p);
    }
}


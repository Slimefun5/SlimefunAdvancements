package me.char321.sfadvancements.core;

import io.github.thebusybiscuit.slimefun5.core.guide.widgets.GuideWidget;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.libraries.xseries.XMaterial;
import me.char321.sfadvancements.SFAdvancements;

/**
 * Registers the advancement screen as a guide widget button rather than a custom guide layout. Addon
 * custom guide screens are deprecated in this fork - addons contribute categories and items, and a
 * {@link GuideWidget} for genuinely functional screens like the advancement tree.
 *
 * @author char321
 */
public final class AdvancementsItemGroup {

    private AdvancementsItemGroup() {}

    public static void init(SFAdvancements plugin) {
        if (SFAdvancements.getMainConfig().getConfiguration().getBoolean("add-advancements-to-guide")) {
            Slimefun.getGuideWidgets().register(new GuideWidget(
                    "advancements",
                    "&9Advancements",
                    XMaterial.FILLED_MAP,
                    -1,
                    (p, profile) -> SFAdvancements.getGuiManager().displayGUI(p)));
        }
    }
}

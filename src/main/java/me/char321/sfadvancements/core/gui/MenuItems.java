package me.char321.sfadvancements.core.gui;

import io.github.thebusybiscuit.slimefun5.libraries.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.libraries.xseries.XMaterial;
import me.char321.sfadvancements.util.MaterialCompat;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author char321
 */
public class MenuItems {
    private MenuItems() {}

    public static final ItemStack BLACK = CustomItemStack.create(MaterialCompat.safe(XMaterial.BLACK_STAINED_GLASS_PANE), " ");
    public static final ItemStack GRAY = CustomItemStack.create(MaterialCompat.safe(XMaterial.GRAY_STAINED_GLASS_PANE), " ");
    public static final ItemStack YELLOW = CustomItemStack.create(MaterialCompat.safe(XMaterial.YELLOW_STAINED_GLASS_PANE), " ");

    /**
     * @implNote Built per viewer rather than held in a constant: a constant is resolved once at class
     *           load, which pins the label to whichever language the server happened to default to and
     *           shows it to every reader thereafter.
     */
    public static ItemStack backItem(@Nullable Player viewer) {
        ItemStack item = new ItemStack(MaterialCompat.safe(XMaterial.ENCHANTED_BOOK));
        ItemMeta meta = item.getItemMeta();
        final List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + ChatColors.color(coreMessage(viewer, "guide.back.guide")));
        meta.setDisplayName(ChatColors.color("&7⇦ " + coreMessage(viewer, "guide.back.title")));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * One of core's own guide labels for this viewer. The advancement screen borrows the guide's page
     * arrows and back button so they read identically to the guide a player just came from, instead of
     * this addon shipping forty translations of "Next page" that could drift from them.
     */
    public static String coreMessage(@Nullable Player viewer, String key) {
        return viewer == null ? Slimefun.getLocalization().getMessage(key)
                : Slimefun.getLocalization().getMessage(viewer, key);
    }
}


package me.char321.sfadvancements.core.gui;

import io.github.thebusybiscuit.slimefun5.libraries.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.libraries.xseries.XMaterial;
import me.char321.sfadvancements.util.MaterialCompat;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

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
    public static final ItemStack BACK_ITEM = createBackItem();

    private static ItemStack createBackItem() {
        ItemStack item = new ItemStack(MaterialCompat.safe(XMaterial.ENCHANTED_BOOK));
        ItemMeta meta = item.getItemMeta();
        final List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + ChatColors.color(Slimefun.getLocalization().getMessage("guide.back.guide")));
        meta.setDisplayName(ChatColors.color("&7⇦ " + Slimefun.getLocalization().getMessage("guide.back.title")));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}


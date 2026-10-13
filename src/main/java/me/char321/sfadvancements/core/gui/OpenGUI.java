package me.char321.sfadvancements.core.gui;

import io.github.thebusybiscuit.slimefun5.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun5.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun5.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun5.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun5.libraries.xseries.XMaterial;
import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.util.MaterialCompat;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.api.AdvancementGroup;
import me.char321.sfadvancements.api.criteria.Criterion;
import me.char321.sfadvancements.core.registry.AdvancementsRegistry;
import me.char321.sfadvancements.util.AdvancementContent;
import me.char321.sfadvancements.util.Messages;
import me.char321.sfadvancements.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author char321
 */
public class OpenGUI {
    private final Inventory inventory;
    private final AdvancementsRegistry registry = SFAdvancements.getRegistry();
    private final UUID playerUUID;
    private int page = 1;
    private int groupIndex = 0;
    private int scroll = 0;

    public OpenGUI(Player player) {
        this(player.getUniqueId());
    }

    public OpenGUI(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.inventory = Bukkit.createInventory(null, 54,
                ChatColor.BLUE + Messages.get(viewer(), "gui.title", "Advancements"));
        refresh();
    }

    /**
     * @implNote An {@link OpenGUI} outlives the session that created it (it is cached per uuid), so the
     *           viewer is looked up per refresh rather than held: a stale {@link Player} would resolve
     *           every label against a language the reader may since have changed.
     */
    @Nullable
    private Player viewer() {
        return Bukkit.getPlayer(playerUUID);
    }

    public Inventory getInventory() {
        refresh();
        return inventory;
    }

    public void click(Player player, int slot) {
        if (slot == 0) {
            PlayerProfile.find(player).ifPresent(profile -> SlimefunGuide.openMainMenu(profile, SlimefunGuideMode.SURVIVAL_MODE, profile.getGuideHistory().getMainMenuPage()));
        } else if (slot == 1 && page > 1) {
            page--;
        } else if (slot == 7) {
            int maxPage = (registry.getAdvancementGroups().size() - 1) / 5 + 1;
            if (page + 1 <= maxPage) {
                page++;
            }
        } else if (slot > 1 && slot < 7) {
            int possibleIndex = 5 * (page - 1) + (slot - 2);
            if (registry.getAdvancementGroups().size() > possibleIndex) {
                groupIndex = possibleIndex;
                scroll = 0;
            }
        } else if (slot == 17 && scroll > 0) {
            scroll--;
        } else if (slot == 53) {
            AdvancementGroup group = registry.getAdvancementGroups().get(groupIndex);
            int size = group.getVisibleAdvancements(playerUUID).size();
            int maxScroll = (size - 1) / 8 - 4;
            if (scroll + 1 <= maxScroll) {
                scroll++;
            }
        }
        refresh();
    }

    public void refresh() {
        refreshBackButton();
        refreshStats();
        refreshArrows();
        refreshGroups();
        refreshScroll();
        refreshAdvancements();
    }

    private void refreshBackButton() {
        inventory.setItem(0, MenuItems.backItem(viewer()));
    }

    private void refreshStats() {
        Player viewer = viewer();
        ItemStack head = MaterialCompat.stack(XMaterial.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        setOwningPlayer(meta, Bukkit.getOfflinePlayer(playerUUID));
        meta.setDisplayName(ChatColor.YELLOW + Messages.get(viewer, "gui.stats", "Stats"));
        StringBuilder completedadvancements = new StringBuilder();
        completedadvancements.append(ChatColor.GRAY)
                .append(Messages.get(viewer, "gui.completed", "Completed Advancements: "));
        int completed = SFAdvancements.getAdvManager().getProgress(playerUUID).getCompletedAdvancements().size();
        int total = SFAdvancements.getRegistry().getAdvancements().size();
        if (completed == total) {
            completedadvancements.append(ChatColor.YELLOW);
        } else {
            completedadvancements.append(ChatColor.WHITE);
        }
        completedadvancements.append(completed).append(ChatColor.GRAY).append("/").append(total);
        List<String> lore = new ArrayList<>();
        lore.add(completedadvancements.toString());
        meta.setLore(lore);
        head.setItemMeta(meta);
        inventory.setItem(8, head);
    }

    private void refreshArrows() {
        Player viewer = viewer();
        int maxPage = (registry.getAdvancementGroups().size() - 1) / 5 + 1;
        String pageLore = "&7(" + page + " / " + maxPage + ")";
        String previous = MenuItems.coreMessage(viewer, "guide.pages.previous");
        String next = MenuItems.coreMessage(viewer, "guide.pages.next");

        inventory.setItem(1, pageArrow(page == 1, previous, pageLore));
        inventory.setItem(7, pageArrow(page == maxPage, next, pageLore));
    }

    /** A page arrow, greyed out when the page it would turn to does not exist. */
    private static ItemStack pageArrow(boolean exhausted, String label, String pageLore) {
        XMaterial material = exhausted ? XMaterial.BLACK_STAINED_GLASS_PANE : XMaterial.LIME_STAINED_GLASS_PANE;
        String colour = exhausted ? "&7" : "&e";
        return CustomItemStack.create(MaterialCompat.safe(material), colour + label, pageLore);
    }

    private void refreshGroups() {
        for (int i=0;i<5;i++) {
            int slot = i+2;
            int dispIndex = 5 * (page - 1) + i;
            ItemStack display;
            if (registry.getAdvancementGroups().size() > dispIndex) {
                display = AdvancementContent.groupDisplay(viewer(), registry.getAdvancementGroups().get(dispIndex));
                if (dispIndex == groupIndex) {
                    display = Utils.makeShiny(display);
                }
            } else {
                display = MenuItems.GRAY;
            }
            inventory.setItem(slot, display);
        }
    }

    private void refreshScroll() {
        AdvancementGroup group = registry.getAdvancementGroups().get(groupIndex);
        inventory.setItem(26, MenuItems.YELLOW);
        inventory.setItem(35, MenuItems.YELLOW);
        inventory.setItem(44, MenuItems.YELLOW);

        Player viewer = viewer();

        ItemStack scrollUp;
        if (scroll == 0) {
            scrollUp = MenuItems.YELLOW;
        } else {
            scrollUp = CustomItemStack.create(MaterialCompat.safe(XMaterial.ARROW),
                    "&e" + Messages.get(viewer, "gui.scroll-up", "Scroll Up"));
        }
        inventory.setItem(17, scrollUp);

        ItemStack scrollDown;
        int size = group.getVisibleAdvancements(playerUUID).size();
        int maxScroll = (size - 1) / 8 - 4;
        if (scroll >= maxScroll) {
            scrollDown = MenuItems.YELLOW;
        } else {
            scrollDown = CustomItemStack.create(MaterialCompat.safe(XMaterial.ARROW),
                    "&e" + Messages.get(viewer, "gui.scroll-down", "Scroll Down"));
        }
        inventory.setItem(53, scrollDown);
    }

    private void refreshAdvancements() {
        AdvancementGroup group = registry.getAdvancementGroups().get(groupIndex);
        List<Advancement> advancements = group.getVisibleAdvancements(playerUUID);
        for (int i = 0; i < 40; i++) {
            int row = i / 8 + 1;
            int col = i % 8;
            int slot = row * 9 + col;

            int advindex = i + 8 * scroll;
            ItemStack display = null;
            if (advindex < advancements.size()) {
                Advancement adv = advancements.get(advindex);
                display = getDisplayfor (adv);
            }

            inventory.setItem(slot, display);
        }
    }

    private ItemStack getDisplayfor (Advancement adv) {
        ItemStack display = AdvancementContent.display(viewer(), adv);
        ItemMeta displayim = display.getItemMeta();
        if (displayim == null) {
            throw new IllegalArgumentException("display item meta is null");
        }

        displayim.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (SFAdvancements.getAdvManager().isCompleted(playerUUID, adv)) {
            Utils.makeShiny(displayim);
        }

        List<String> lore = displayim.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        boolean loreAdded = false;
        for (int i = lore.size() - 1; i >= 0; i--) {
            if ("%criteria%".equals(lore.get(i))) {
                lore.remove(i);
                lore.addAll(i, this.getCriteriaLore(adv));
                loreAdded = true;
            }
        }
        if (!loreAdded) {
            lore.addAll(this.getCriteriaLore(adv));
        }

        displayim.setLore(lore);
        display.setItemMeta(displayim);
        return display;
    }

    @SuppressWarnings("deprecation")
    private static void setOwningPlayer(SkullMeta meta, OfflinePlayer player) {
        try {
            SkullMeta.class.getMethod("setOwningPlayer", OfflinePlayer.class).invoke(meta, player);
        } catch (ReflectiveOperationException e) {
            String name = player.getName();
            if (name != null) {
                meta.setOwner(name);
            }
        }
    }

    private List<String> getCriteriaLore(Advancement adv) {
        List<String> res = new ArrayList<>();
        Player viewer = viewer();
        for (Criterion criterion : adv.getCriteria()) {
            String criterionName = AdvancementContent.criterionName(viewer, adv, criterion);
            int progress = SFAdvancements.getAdvManager().getCriterionProgress(playerUUID, criterion);
            int max = criterion.getCount();
            boolean cridone = progress >= max;
            res.add(ChatColor.GRAY + criterionName + ": " + (cridone ? ChatColor.YELLOW : ChatColor.WHITE) + progress + "/" + max);
        }
        return res;
    }
}


package me.char321.sfadvancements.vanilla;

import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.api.AdvancementGroup;
import me.char321.sfadvancements.api.criteria.Criterion;
import me.char321.sfadvancements.util.Utils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.roxeez.advancement.AdvancementManager;
import net.roxeez.advancement.display.FrameType;
import net.roxeez.advancement.display.Icon;
import net.roxeez.advancement.trigger.TriggerType;
import org.bukkit.Bukkit;
import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author char321
 */
public class VanillaHook {
    private AdvancementManager vanillaManager;
    private boolean initialized = false;

    private static final boolean HAS_ADVANCEMENT_API = hasAdvancementApi();

    private static boolean hasAdvancementApi() {
        try {
            Class.forName("org.bukkit.NamespacedKey");
            Class.forName("org.bukkit.advancement.Advancement");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Converts a relocated {@link NamespacedKey} shim into a real {@code org.bukkit.NamespacedKey}.
     * Only ever reached when {@link #HAS_ADVANCEMENT_API} is true, so the modern API is present.
     */
    private static Object toBukkitKey(NamespacedKey key) {
        try {
            Class<?> bukkitKey = Class.forName("org.bukkit.NamespacedKey");
            return bukkitKey.getConstructor(String.class, String.class).newInstance(key.getNamespace(), key.getKey());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create org.bukkit.NamespacedKey", e);
        }
    }

    public void init() {
        if (initialized) return;
        initialized = true;

        if (!HAS_ADVANCEMENT_API) {
            SFAdvancements.warn("Vanilla advancement API is unavailable on this server version. Disabling vanilla advancement integration.");
            return;
        }

        this.vanillaManager = new AdvancementManager(SFAdvancements.instance());

        Utils.listen(new PlayerJoinListener());
        Utils.listen(new AdvancementListener());
        reload();
    }

    public void reload() {
        if (!initialized) {
            init();
        }

        if (!HAS_ADVANCEMENT_API) {
            return;
        }

        vanillaManager.clearAdvancements();
        registerGroups(vanillaManager);
        registerAdvancements(vanillaManager);
        try {
            vanillaManager.createAll(true);
        } catch (Exception e) {
            SFAdvancements.warn("Failed to create vanilla advancements due to JSON format changes in 1.21.4+. Skipping vanilla advancement generation.");
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            syncProgress(p);
        }
    }

    private static void registerGroups(AdvancementManager manager) {
        for (AdvancementGroup group : SFAdvancements.getRegistry().getAdvancementGroups()) {
            manager.register(context -> {
                net.roxeez.advancement.Advancement vadvancement = new net.roxeez.advancement.Advancement(SFAdvancements.instance(), group.getId());

                vadvancement.setDisplay(display -> {
                    ItemStack item = group.getDisplayItem();
                    String background = group.getBackground();
                    ItemMeta meta = item.getItemMeta();
                    //noinspection DataFlowIssue
                    display.setTitle(meta.getDisplayName());
                    List<String> lore = meta.getLore();
                    if (lore == null) {
                        lore = new ArrayList<>();
                    }
                    display.setDescription(String.join("\n", lore));
                    display.setIcon(new Icon(item));
                    display.setFrame(FrameType.valueOf(group.getFrameType()));
                    setBackground(display, NamespacedKey.minecraft("textures/block/" + background.toLowerCase() + ".png"));
                    display.setAnnounce(false);
                });

                vadvancement.addCriteria("impossible", TriggerType.IMPOSSIBLE, a -> {});

                return vadvancement;
            });
        }
    }

    private static void registerAdvancements(AdvancementManager manager) {
        for (Map.Entry<NamespacedKey, Advancement> entry : SFAdvancements.getRegistry().getAdvancements().entrySet()) {
            registerAdvancement(manager, entry.getValue());
        }
    }

    private static void registerAdvancement(AdvancementManager manager, Advancement advancement) {
        if (manager == null) return;
        if (advancement == null) return;

        //TODO optimize
        String advKey = advancement.getKey().toString();
        String parentKey = advancement.getParent().toString();
        if (manager.getAdvancements().stream().anyMatch(vadv -> vadv.getKey().toString().equals(advKey))) return;
        //do i even need to do this?
        if (manager.getAdvancements().stream().noneMatch(vadv -> vadv.getKey().toString().equals(parentKey))) {
            Advancement parent = Utils.fromKey(advancement.getParent());
            if (parent != null) {
                registerAdvancement(manager, parent);
            }
        }

        manager.register(context -> {
            net.roxeez.advancement.Advancement vadvancement = newRoxeezAdvancement(advancement.getKey());

            vadvancement.setDisplay(display -> {
                ItemStack item = advancement.getDisplay();
                ItemMeta meta = item.getItemMeta();
                BaseComponent title;
                String description;
                //noinspection DataFlowIssue
                if (meta.hasDisplayName()) {
                    title = new TextComponent(meta.getDisplayName());
                } else {
                    title = Utils.getItemName(item);
                }
                description = getDescriptionfor (meta.getLore(), advancement);
                display.setTitle(title);
                display.setDescription(description);
                display.setIcon(new Icon(item));
                display.setFrame(FrameType.valueOf(advancement.getFrameType()));
                display.setHidden(advancement.isHidden());
                display.setAnnounce(false);
            });

            setParent(vadvancement, advancement.getParent());
            vadvancement.addCriteria("impossible", TriggerType.IMPOSSIBLE, a -> {});

            return vadvancement;
        });
    }

    private static net.roxeez.advancement.Advancement newRoxeezAdvancement(NamespacedKey key) {
        try {
            return net.roxeez.advancement.Advancement.class
                    .getConstructor(Class.forName("org.bukkit.NamespacedKey"))
                    .newInstance(toBukkitKey(key));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create vanilla advancement", e);
        }
    }

    private static void setParent(net.roxeez.advancement.Advancement advancement, NamespacedKey parent) {
        try {
            net.roxeez.advancement.Advancement.class
                    .getMethod("setParent", Class.forName("org.bukkit.NamespacedKey"))
                    .invoke(advancement, toBukkitKey(parent));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set vanilla advancement parent", e);
        }
    }

    private static void setBackground(net.roxeez.advancement.display.Display display, NamespacedKey background) {
        try {
            display.getClass()
                    .getMethod("setBackground", Class.forName("org.bukkit.NamespacedKey"))
                    .invoke(display, toBukkitKey(background));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set vanilla advancement background", e);
        }
    }

    private static String getDescriptionfor (List<String> lore, Advancement adv) {
        lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
        for (int i = lore.size() - 1; i >= 0; i--) {
            if ("%criteria%".equals(lore.get(i))) {
                lore.remove(i);
                lore.addAll(i, getCriteriaLore(adv));
                return String.join("\n", lore);
            }
        }
        return String.join("\n", lore);
    }

    private static List<String> getCriteriaLore(Advancement adv) {
        List<String> res = new ArrayList<>();
        for (Criterion criterion : adv.getCriteria()) {
            res.add("§7" + criterion.getName());
        }
        return res;
    }

    public void syncProgress(Player p) {
        if (!HAS_ADVANCEMENT_API) {
            return;
        }
        for (AdvancementGroup group : SFAdvancements.getRegistry().getAdvancementGroups()) {
            complete(p, Utils.keyOf(group.getId()));
        }
        for (Advancement adv : SFAdvancements.getRegistry().getAdvancements().values()) {
            if (SFAdvancements.getAdvManager().isCompleted(p, adv)) {
                complete(p, adv.getKey());
            } else {
                revoke(p, adv.getKey());
            }
        }
    }

    public void complete(Player p, NamespacedKey key) {
        if (!HAS_ADVANCEMENT_API) {
            return;
        }
        org.bukkit.advancement.Advancement advancement = getBukkitAdvancement(key);
        if (advancement == null) {
            SFAdvancements.warn("Tried to complete unregistered advancement " + key);
            return;
        }
        Utils.runSync(() -> p.getAdvancementProgress(advancement).awardCriteria("impossible"));
    }

    public void revoke(Player p, NamespacedKey key) {
        if (!HAS_ADVANCEMENT_API) {
            return;
        }
        org.bukkit.advancement.Advancement advancement = getBukkitAdvancement(key);
        if (advancement == null) {
            SFAdvancements.warn("Tried to revoke unregistered advancement " + key);
            return;
        }
        Utils.runSync(() -> p.getAdvancementProgress(advancement).revokeCriteria("impossible"));

    }

    private static org.bukkit.advancement.Advancement getBukkitAdvancement(NamespacedKey key) {
        try {
            return (org.bukkit.advancement.Advancement) Bukkit.class
                    .getMethod("getAdvancement", Class.forName("org.bukkit.NamespacedKey"))
                    .invoke(null, toBukkitKey(key));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}

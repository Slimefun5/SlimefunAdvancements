package me.char321.sfadvancements.vanilla;

import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.api.AdvancementGroup;
import me.char321.sfadvancements.api.criteria.Criterion;
import me.char321.sfadvancements.util.AdvancementContent;
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
    private AdvancementPacketTranslator translator;
    private boolean initialized = false;
    private boolean vanillaUnavailable = false;

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

        try {
            // Manager construction runs AdvancementAPI's ObjectSerializer static init, which can NoSuchMethodError on newer Paper; catch Throwable so enable survives.
            this.vanillaManager = new AdvancementManager(SFAdvancements.instance());
        } catch (Throwable t) {
            vanillaUnavailable = true;
            SFAdvancements.warn("Could not initialize vanilla advancement integration on this server version. Disabling it: " + t);
            return;
        }

        Utils.listen(new PlayerJoinListener());
        Utils.listen(new AdvancementListener());

        if (SFAdvancements.getMainConfig().getConfiguration().getBoolean("translate-vanilla-advancements", true)) {
            translator = AdvancementPacketTranslator.create();

            if (translator != null) {
                translator.start();
            }
        }

        reload();
    }

    /** Re-reads every online player's language, after a reload may have changed the loaded languages. */
    private void refreshLanguages() {
        if (translator == null) {
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            translator.refresh(p);
        }
    }

    public void reload() {
        if (!initialized) {
            init();
        }

        if (!HAS_ADVANCEMENT_API || vanillaUnavailable || vanillaManager == null) {
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

        refreshLanguages();

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
                    String language = AdvancementContent.defaultLanguage();
                    String title = AdvancementContent.groupName(language, group);
                    //noinspection DataFlowIssue
                    display.setTitle(title != null ? title : meta.getDisplayName());
                    display.setDescription(String.join("\n", AdvancementContent.groupLore(language, group)));
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
                String language = AdvancementContent.defaultLanguage();
                String name = AdvancementContent.displayName(language, advancement);
                BaseComponent title = name != null ? new TextComponent(name) : Utils.getItemName(item);
                String description = getDescriptionfor (
                        AdvancementContent.displayLore(language, advancement), advancement, language);
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

    private static String getDescriptionfor (List<String> lore, Advancement adv, String language) {
        lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
        for (int i = lore.size() - 1; i >= 0; i--) {
            if ("%criteria%".equals(lore.get(i))) {
                lore.remove(i);
                lore.addAll(i, getCriteriaLore(adv, language));
                return String.join("\n", lore);
            }
        }
        return String.join("\n", lore);
    }

    private static List<String> getCriteriaLore(Advancement adv, String language) {
        List<String> res = new ArrayList<>();
        for (Criterion criterion : adv.getCriteria()) {
            res.add("§7" + AdvancementContent.criterionName(language, adv, criterion));
        }
        return res;
    }

    public void syncProgress(Player p) {
        if (!HAS_ADVANCEMENT_API || vanillaUnavailable) {
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
        if (!HAS_ADVANCEMENT_API || vanillaUnavailable) {
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
        if (!HAS_ADVANCEMENT_API || vanillaUnavailable) {
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

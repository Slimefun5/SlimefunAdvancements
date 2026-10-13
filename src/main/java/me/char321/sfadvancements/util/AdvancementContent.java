package me.char321.sfadvancements.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.api.AdvancementGroup;
import me.char321.sfadvancements.api.criteria.Criterion;

/**
 * Resolves advancement and group text for one viewer, from {@code languages/<lang>/advancements.yml}.
 * <p>
 * Unlike {@link Messages}, the English side of this content is not a literal in the code: it lives in
 * {@code advancements.yml} and {@code groups.yml}, which the operator edits. So a line is only
 * translated while it still matches the copy this addon shipped - see {@link File#measure()}.
 *
 * @implNote Text is read from the live configuration rather than from the display {@link ItemStack}
 *           the loader already baked, because the baked copy has had its {@code &} codes converted.
 *           Translating uniformly from the raw config means one colourise step on the way out and no
 *           way to double-convert a line.
 */
public final class AdvancementContent {

    /**
     * One content file: where its shipped copy lives, which keys its translations use, and which of
     * its lines the operator has taken over.
     *
     * @implNote Advancements and groups differ only in these three things, so they are one type with
     *           two instances. Open-coding the pair instead invites a method that reads one file's
     *           configuration while resolving the other's path.
     */
    private static final class File {

        private final String resource;
        private final String keyPrefix;
        private final Supplier<ConfigurationSection> live;
        private final Set<String> customised = new HashSet<>();

        private File(String resource, String keyPrefix, Supplier<ConfigurationSection> live) {
            this.resource = resource;
            this.keyPrefix = keyPrefix;
            this.live = live;
        }

        /** Re-measures which lines no longer match the shipped default, so the operator owns them. */
        private void measure() {
            customised.clear();
            ConfigurationSection current = live.get();

            if (current == null) {
                return;
            }

            YamlConfiguration shipped = loadResource(resource);

            for (String path : shipped.getKeys(true)) {
                Object value = shipped.get(path);

                // A section is a container, not a line: ConfigurationSection has no equals, so
                // comparing two of them would mark every parent path as edited.
                if (value instanceof ConfigurationSection) {
                    continue;
                }

                if (!Objects.equals(value, current.get(path))) {
                    customised.add(path);
                }
            }
        }

        @Nullable
        private String string(@Nonnull String path) {
            ConfigurationSection current = live.get();
            return current == null ? null : current.getString(path);
        }

        @Nullable
        private List<String> list(@Nonnull String path) {
            ConfigurationSection current = live.get();
            return current == null || !current.isSet(path) ? null : current.getStringList(path);
        }

        @Nonnull
        private String line(@Nullable Player viewer, @Nonnull String id, @Nonnull String field,
                            @Nonnull String path, @Nonnull String authored) {
            String raw = string(path);
            String fallback = raw == null ? authored : raw;

            if (customised.contains(path)) {
                return ConfigUtils.translate(fallback);
            }

            return ConfigUtils.translate(message(viewer, key(id, field), fallback));
        }

        @Nullable
        private List<String> lines(@Nullable Player viewer, @Nonnull String id, @Nonnull String field,
                                   @Nonnull String path) {
            List<String> authored = list(path);

            if (authored == null) {
                return null;
            }

            return translate(customised.contains(path) ? authored
                    : messages(viewer, key(id, field), authored));
        }

        @Nonnull
        private String key(@Nonnull String id, @Nonnull String field) {
            return keyPrefix + "." + id + "." + field;
        }
    }

    private static final File ADVANCEMENTS =
            new File("advancements.yml", "advancements", () -> SFAdvancements.instance().getAdvancementConfig());
    private static final File GROUPS =
            new File("groups.yml", "groups", () -> SFAdvancements.instance().getGroupsConfig());

    private AdvancementContent() {}

    /**
     * Re-measures both content files. Must run after the live configurations are loaded and before
     * anything resolves text from them.
     */
    public static void reload() {
        ADVANCEMENTS.measure();
        GROUPS.measure();
    }

    /** The chat name of an advancement, as in {@code &a[Black Gold]}. */
    @Nonnull
    public static String name(@Nullable Player viewer, @Nonnull Advancement advancement) {
        String id = advancement.getId();
        return ADVANCEMENTS.line(viewer, id, "name", id + ".name", advancement.getName());
    }

    @Nonnull
    public static String criterionName(@Nullable Player viewer, @Nonnull Advancement advancement,
                                       @Nonnull Criterion criterion) {
        String id = advancement.getId();
        return ADVANCEMENTS.line(viewer, id, "criteria." + criterion.getId(),
                id + ".criteria." + criterion.getId() + ".name", criterion.getName());
    }

    @Nonnull
    public static List<String> displayLore(@Nullable Player viewer, @Nonnull Advancement advancement) {
        String id = advancement.getId();
        List<String> lore = ADVANCEMENTS.lines(viewer, id, "lore", id + ".display.lore");
        return lore == null ? Collections.emptyList() : lore;
    }

    /** The advancement's display item with its name and lore in the viewer's own language. */
    @Nonnull
    public static ItemStack display(@Nullable Player viewer, @Nonnull Advancement advancement) {
        return retexture(viewer, ADVANCEMENTS, advancement.getDisplay(), advancement.getId());
    }

    /** The group's display item with its name and lore in the viewer's own language. */
    @Nonnull
    public static ItemStack groupDisplay(@Nullable Player viewer, @Nonnull AdvancementGroup group) {
        return retexture(viewer, GROUPS, group.getDisplayItem(), group.getId());
    }

    /**
     * @implNote A {@code null} lore means the configuration authors none, which is NOT the same as
     *           authoring an empty one: {@code display: ELECTRIC_GOLD_PAN} takes the whole template
     *           stack, and overwriting its lore with an empty list would strip the item's own.
     */
    @Nonnull
    private static ItemStack retexture(@Nullable Player viewer, @Nonnull File file,
                                       @Nonnull ItemStack template, @Nonnull String id) {
        ItemStack item = template.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        String path = id + ".display";
        String authored = file.string(path + ".name");
        String name = authored != null ? file.line(viewer, id, "display-name", path + ".name", authored)
                : templateName(viewer, file, path);

        if (name != null) {
            meta.setDisplayName(name);
        }

        List<String> lore = file.lines(viewer, id, "lore", path + ".lore");

        if (lore != null) {
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * The per-viewer name of a Slimefun item used as a display without an authored name of its own.
     *
     * @implNote Without this the tile shows whatever the stack carries, and under the fork's
     *           no-baked-lore rule a Slimefun stack carries its raw id, so the screen would read
     *           ELECTRIC_GOLD_PAN.
     */
    @Nullable
    private static String templateName(@Nullable Player viewer, @Nonnull File file, @Nonnull String path) {
        String id = file.string(path);
        SlimefunItem item = id == null ? null : SlimefunItem.getById(id);

        if (item == null) {
            return null;
        }

        return viewer == null ? item.getItemName()
                : Slimefun.getItemTranslationService().getName(viewer, item);
    }

    @Nonnull
    private static YamlConfiguration loadResource(@Nonnull String name) {
        InputStream stream = SFAdvancements.instance().getResource(name);

        if (stream == null) {
            return new YamlConfiguration();
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            SFAdvancements.warn("Could not read bundled " + name + "; treating all of it as operator-edited");
            return new YamlConfiguration();
        }
    }

    @Nonnull
    private static String message(@Nullable Player viewer, @Nonnull String key, @Nonnull String fallback) {
        return Slimefun.getAddonMessageService().getMessage(viewer, SFAdvancements.instance(), key, fallback);
    }

    @Nonnull
    private static List<String> messages(@Nullable Player viewer, @Nonnull String key, @Nonnull List<String> fallback) {
        return Slimefun.getAddonMessageService().getMessages(viewer, SFAdvancements.instance(), key, fallback);
    }

    @Nonnull
    private static List<String> translate(@Nonnull List<String> lines) {
        List<String> translated = new ArrayList<>(lines.size());

        for (String line : lines) {
            translated.add(ConfigUtils.translate(line));
        }

        return translated;
    }
}

package me.char321.sfadvancements.vanilla;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import io.github.thebusybiscuit.slimefun5.utils.compatibility.packet.PacketReflect;
import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.util.AdvancementContent;
import me.char321.sfadvancements.util.Utils;

/**
 * Rewrites the outbound advancement-tree packet so each player reads the vanilla advancements screen in
 * their own language.
 * <p>
 * The tree itself is registered once, server-wide, so the text baked into it can only ever be right for
 * one reader; see {@link VanillaHook}, which registers it in the server's default language. This handler
 * is what makes the same screen per viewer.
 *
 * @implNote <strong>This covers the map-shaped packet only, which is roughly 1.12 to 1.20.1.</strong>
 *           From 1.20.2 the payload became a list of {@code AdvancementHolder} records, and a record's
 *           fields cannot be written reflectively at all, so translating those versions means
 *           constructing replacement records whose signatures differ per version. That path is not
 *           implemented here, and on those versions this handler resolves nothing and leaves the packet
 *           alone - the reader then sees the server's default language, which is the same thing they saw
 *           before this class existed.
 * @implNote Everything runs on a Netty thread, so the only state it touches is {@link #languages}, which
 *           the main thread fills on join. Resolving a viewer's language here would read their
 *           persistent data off-thread.
 */
public final class AdvancementPacketTranslator implements Listener {

    private static final String HANDLER_NAME = "sfadvancements-translate";

    private static final String[] PACKET_NAMES = {
        "net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket",
        "PacketPlayOutAdvancements",
    };

    private static final String[] COMPONENT_NAMES = {
        "net.minecraft.network.chat.Component",
        "net.minecraft.network.chat.IChatBaseComponent",
        "IChatBaseComponent",
    };

    private final Map<UUID, String> languages = new ConcurrentHashMap<>();

    private final Class<?> packetClass;
    private final Class<?> componentClass;
    private final ComponentFactory components;

    /** Set once a rewrite attempt fails, so an unsupported shape costs one failure and not one per packet. */
    private volatile boolean unsupported;

    private AdvancementPacketTranslator(Class<?> packetClass, Class<?> componentClass, ComponentFactory components) {
        this.packetClass = packetClass;
        this.componentClass = componentClass;
        this.components = components;
    }

    /**
     * @return a translator, or {@code null} when this server version exposes none of the classes it needs
     */
    @Nullable
    public static AdvancementPacketTranslator create() {
        Class<?> packet = resolve(PACKET_NAMES);
        Class<?> component = resolve(COMPONENT_NAMES);
        ComponentFactory factory = ComponentFactory.resolve();

        if (packet == null || component == null || factory == null || !carriesAMap(packet)) {
            SFAdvancements.warn("Per-viewer vanilla advancements are unavailable on this server version; "
                    + "the advancements screen will use the server's default language.");
            return null;
        }

        return new AdvancementPacketTranslator(packet, component, factory);
    }

    /**
     * Whether the packet still carries its advancements in a {@link Map}.
     *
     * @implNote Checked once here rather than per packet, because a packet that happens to carry no
     *           advancements is indistinguishable at runtime from a version whose payload this addon
     *           cannot read - and the list-shaped versions described on the class are exactly the ones
     *           that must be detected without guessing.
     */
    private static boolean carriesAMap(Class<?> packet) {
        for (Field field : packet.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                return true;
            }
        }

        return false;
    }

    public void start() {
        Utils.listen(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
            inject(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        refresh(e.getPlayer());
        inject(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        languages.remove(e.getPlayer().getUniqueId());
    }

    /** Re-reads one player's language on the main thread, for the Netty thread to use. */
    public void refresh(Player player) {
        String language = AdvancementContent.languageOf(player);

        if (language == null) {
            languages.remove(player.getUniqueId());
        } else {
            languages.put(player.getUniqueId(), language);
        }
    }

    private void inject(Player player) {
        Object channelObject = PacketReflect.channelOf(player);

        if (!(channelObject instanceof Channel)) {
            return;
        }

        Channel channel = (Channel) channelObject;

        try {
            if (channel.pipeline().get(HANDLER_NAME) != null) {
                return;
            }

            UUID id = player.getUniqueId();
            channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    super.write(ctx, rewrite(id, msg), promise);
                }
            });
        } catch (Throwable t) {
            SFAdvancements.warn("Could not translate vanilla advancements for " + player.getName()
                    + "; they will read the server's default language.");
        }
    }

    /** Never throws: on any failure the original packet goes out unchanged. */
    private Object rewrite(UUID viewer, Object packet) {
        if (unsupported || !packetClass.isInstance(packet)) {
            return packet;
        }

        String language = languages.get(viewer);

        if (language == null) {
            return packet;
        }

        try {
            rewriteAdded(packet, language);
        } catch (Throwable t) {
            unsupported = true;
        }

        return packet;
    }

    private void rewriteAdded(Object packet, String language) throws ReflectiveOperationException {
        for (Field field : packet.getClass().getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(packet);

            if (!(value instanceof Map)) {
                continue;
            }

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Advancement advancement = ours(entry.getKey());

                if (advancement == null) {
                    continue;
                }

                retitle(entry.getValue(), advancement, language);
            }
        }
    }

    /**
     * The addon's own advancement behind a packet key, or {@code null} for anyone else's.
     *
     * @implNote The key is matched by its {@code toString()}, which is {@code namespace:path} for every
     *           version of this type, rather than by calling an accessor whose name is obfuscated
     *           differently in each one.
     */
    @Nullable
    private Advancement ours(@Nullable Object key) {
        if (key == null) {
            return null;
        }

        String id = String.valueOf(key);
        int colon = id.indexOf(':');

        if (colon < 0 || !id.substring(0, colon).equals(SFAdvancements.instance().getName().toLowerCase(java.util.Locale.ROOT))) {
            return null;
        }

        return Utils.fromKey(id.substring(colon + 1));
    }

    /** Replaces the title and description components reachable from one advancement in the packet. */
    private void retitle(@Nullable Object holder, Advancement advancement, String language)
            throws ReflectiveOperationException {
        Object display = findDisplay(holder, 0);

        if (display == null) {
            return;
        }

        List<Field> fields = componentFields(display);

        if (fields.size() < 2) {
            return;
        }

        String name = AdvancementContent.displayName(language, advancement);
        String description = String.join("\n", AdvancementContent.displayLore(language, advancement));

        if (name != null) {
            fields.get(0).set(display, components.of(name));
        }

        fields.get(1).set(display, components.of(description));
    }

    /**
     * The nearest object holding at least two component fields, which is the display record of whichever
     * version this is. Searched rather than named, because the field holding it is called {@code display}
     * on one version and a single obfuscated letter on another.
     */
    @Nullable
    private Object findDisplay(@Nullable Object owner, int depth) throws ReflectiveOperationException {
        if (owner == null || depth > 3) {
            return null;
        }

        if (componentFields(owner).size() >= 2) {
            return owner;
        }

        for (Field field : owner.getClass().getDeclaredFields()) {
            Class<?> type = field.getType();

            if (type.isPrimitive() || type.isArray() || type.getName().startsWith("java.")) {
                continue;
            }

            field.setAccessible(true);
            Object nested = findDisplay(field.get(owner), depth + 1);

            if (nested != null) {
                return nested;
            }
        }

        return null;
    }

    private List<Field> componentFields(Object owner) {
        List<Field> out = new java.util.ArrayList<>();

        for (Field field : owner.getClass().getDeclaredFields()) {
            if (componentClass.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    out.add(field);
                } catch (Throwable t) {
                    // A record's field cannot be made writable; see the class note.
                    return java.util.Collections.emptyList();
                }
            }
        }

        return out;
    }

    @Nullable
    private static Class<?> resolve(String[] names) {
        String legacy = legacyPackage();

        for (String name : names) {
            Class<?> found = tryClass(name);

            if (found == null && legacy != null && name.indexOf('.') < 0) {
                found = tryClass(legacy + '.' + name);
            }

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    @Nullable
    private static String legacyPackage() {
        try {
            String craftbukkit = Bukkit.getServer().getClass().getPackage().getName();
            int dot = craftbukkit.lastIndexOf('.');
            String version = dot < 0 ? "" : craftbukkit.substring(dot + 1);
            return version.startsWith("v") ? "net.minecraft.server." + version : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static Class<?> tryClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Builds a native chat component from a legacy-coloured string, however this version spells it. */
    private static final class ComponentFactory {

        private final java.lang.reflect.Method method;

        private ComponentFactory(java.lang.reflect.Method method) {
            this.method = method;
        }

        @Nullable
        private static ComponentFactory resolve() {
            String[] holders = {
                "org.bukkit.craftbukkit.util.CraftChatMessage",
                "CraftChatMessage",
            };

            for (String holder : holders) {
                Class<?> found = tryClass(holder);

                if (found == null && holder.indexOf('.') < 0) {
                    found = tryClass(craftbukkitPackage() + ".util." + holder);
                }

                if (found == null) {
                    continue;
                }

                for (String name : new String[] { "fromStringOrNull", "fromString" }) {
                    try {
                        java.lang.reflect.Method m = found.getMethod(name, String.class);
                        return new ComponentFactory(m);
                    } catch (NoSuchMethodException ignored) {
                        // try the next spelling
                    }
                }
            }

            return null;
        }

        private static String craftbukkitPackage() {
            return Bukkit.getServer().getClass().getPackage().getName();
        }

        @Nullable
        private Object of(String text) throws ReflectiveOperationException {
            Object result = method.invoke(null, text);

            // fromString returns an array, fromStringOrNull a single component.
            if (result != null && result.getClass().isArray()) {
                return java.lang.reflect.Array.getLength(result) > 0
                        ? java.lang.reflect.Array.get(result, 0) : null;
            }

            return result;
        }
    }
}

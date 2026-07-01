package me.char321.sfadvancements.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Builds and sends the advancement broadcast message using only Bungee chat API
 * present on every supported version.
 * <p>
 * The original code used {@code net.md_5.bungee.api.chat.hover.content.Text} and the
 * {@code HoverEvent(Action, Content)} constructor, both 1.16+ only, which would prevent
 * {@code Advancement} from loading on 1.8. This routes through the array-based
 * {@link HoverEvent} constructor available since 1.8.
 *
 * @author char321
 */
public final class ComponentCompat {

    private ComponentCompat() {}

    @SuppressWarnings("deprecation")
    public static BaseComponent buildAdvancementMessage(String prefix, String name, String description) {
        BaseComponent component = new TextComponent();
        component.addExtra(prefix);

        TextComponent sub = new TextComponent(name);
        sub.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { new TextComponent(description) }));
        component.addExtra(sub);
        return component;
    }

    @SuppressWarnings("deprecation")
    public static void send(Player player, BaseComponent component) {
        player.spigot().sendMessage(component);
    }
}

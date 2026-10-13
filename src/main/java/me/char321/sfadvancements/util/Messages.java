package me.char321.sfadvancements.util;

import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import me.char321.sfadvancements.SFAdvancements;

/**
 * Resolves this addon's chat text for one viewer, from {@code languages/<lang>/messages.yml}.
 *
 * @implNote The {@link ChatColor} deliberately stays at the call site rather than moving into the
 *           translated value: a translator never edits a colour code they cannot see the effect of.
 *           A console recipient is handled by the service, which applies the same substitutions to
 *           the English literal and strips the colour.
 */
public final class Messages {

    private Messages() {}

    @Nonnull
    public static String get(@Nullable Player player, @Nonnull String key, @Nonnull String english) {
        return Slimefun.getAddonMessageService().getMessage(player, SFAdvancements.instance(), key, english);
    }

    public static void send(@Nonnull CommandSender recipient, @Nonnull String key, @Nonnull String english) {
        Slimefun.getAddonMessageService().sendMessage(recipient, SFAdvancements.instance(), key, english);
    }

    public static void send(@Nonnull CommandSender recipient, @Nonnull String key, @Nonnull String english,
                            @Nonnull UnaryOperator<String> placeholders) {
        Slimefun.getAddonMessageService().sendMessage(recipient, SFAdvancements.instance(), key, english, placeholders);
    }

    public static void send(@Nonnull CommandSender recipient, @Nonnull ChatColor colour, @Nonnull String key, @Nonnull String english) {
        send(recipient, colour, key, english, UnaryOperator.identity());
    }

    public static void send(@Nonnull CommandSender recipient, @Nonnull ChatColor colour, @Nonnull String key,
                            @Nonnull String english, @Nonnull UnaryOperator<String> placeholders) {
        Slimefun.getAddonMessageService().sendMessage(recipient, SFAdvancements.instance(), key, english,
                line -> colour + placeholders.apply(line));
    }
}

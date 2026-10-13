package me.char321.sfadvancements.core.command;

import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.api.Advancement;
import me.char321.sfadvancements.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import io.github.thebusybiscuit.slimefun5.libraries.keys.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author char321
 */
public class GrantCommand implements SubCommand {
    @Override
    public boolean onExecute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            Messages.send(sender, ChatColor.RED, "command.grant-usage", "Usage: /%label% grant <player> <advancement>",
                    line -> line.replace("%label%", label));
            return false;
        }

        Player p = Bukkit.getPlayer(args[1]);
        if (p == null) {
            Messages.send(sender, ChatColor.RED, "command.player-not-found", "Could not find player %player%",
                    line -> line.replace("%player%", args[1]));
            return false;
        }

        if (args[2].equals("*") || args[2].equals("all")) {
            for (Advancement adv : SFAdvancements.getRegistry().getAdvancements().values()) {
                adv.complete(p);
            }
            Messages.send(sender, "command.granted-all", "Successfully granted all advancements!");
            return true;
        }

        Advancement adv = SFAdvancements.getRegistry().getAdvancement(NamespacedKey.fromString(args[2]));
        if (adv == null) {
            Messages.send(sender, ChatColor.RED, "command.advancement-not-found", "Could not find advancement %advancement%",
                    line -> line.replace("%advancement%", args[2]));
            return false;
        }

        adv.complete(p);
        Messages.send(sender, "command.granted", "Successfully granted advancement %advancement% to player %player%",
                line -> line.replace("%advancement%", adv.getKey().toString()).replace("%player%", p.getName()));
        return true;
    }

    @Override
    public @Nonnull String getCommandName() {
        return "grant";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) { //if only brigadier existed in spigot api
            List<String> res = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().contains(args[1])) {
                    res.add(p.getName());
                }
            }
            return res;
        } else if (args.length == 3) {
            Player p = Bukkit.getPlayer(args[1]);
            if (p != null) {
                List<String> res = new ArrayList<>();
                res.add("*");
                res.add("all");
                for (Advancement adv : SFAdvancements.getRegistry().getAdvancements().values()) {
                    if (!SFAdvancements.getAdvManager().isCompleted(p, adv)) {
                        String s = adv.getKey().toString();
                        if (s.contains(args[2])) {
                            res.add(s);
                        }
                    }
                }
                return res;
            }
        }
        return Collections.emptyList();
    }
}

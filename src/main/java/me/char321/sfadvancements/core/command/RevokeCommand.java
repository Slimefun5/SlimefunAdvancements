package me.char321.sfadvancements.core.command;

import me.char321.sfadvancements.SFAdvancements;
import me.char321.sfadvancements.core.criteria.progress.PlayerProgress;
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
public class RevokeCommand implements SubCommand {
    @Override
    public boolean onExecute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            Messages.send(sender, ChatColor.RED, "command.revoke-usage", "Usage: /%label% revoke <player> <advancement>",
                    line -> line.replace("%label%", label));
            return false;
        }

        Player p = Bukkit.getPlayer(args[1]);
        if (p == null) {
            Messages.send(sender, ChatColor.RED, "command.player-not-found", "Could not find player %player%",
                    line -> line.replace("%player%", args[1]));
            return false;
        }

        PlayerProgress progress = SFAdvancements.getAdvManager().getProgress(p);
        if (args[2].equals("*") || args[2].equals("all")) {
            for (NamespacedKey adv : progress.getCompletedAdvancements()) {
                progress.revokeAdvancement(adv);
            }
            Messages.send(sender, "command.revoked-all", "Successfully revoked all advancements!");
            return true;
        }

        if (!progress.revokeAdvancement(NamespacedKey.fromString(args[2]))) {
            Messages.send(sender, ChatColor.RED, "command.revoke-failed", "Could not revoke advancement %advancement% from player %player%",
                    line -> line.replace("%advancement%", args[2]).replace("%player%", args[1]));
            return false;
        } else {
            Messages.send(sender, "command.revoked", "Successfully revoked!");
            return true;
        }
    }

    @Override
    public @Nonnull String getCommandName() {
        return "revoke";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
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
                for (NamespacedKey key : SFAdvancements.getAdvManager().getProgress(p).getCompletedAdvancements()) {
                    String s = key.toString();
                    if (s.contains(args[2])) {
                        res.add(key.toString());
                    }
                }
                return res;
            }
        }
        return Collections.emptyList();
    }

}

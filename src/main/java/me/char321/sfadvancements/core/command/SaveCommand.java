package me.char321.sfadvancements.core.command;

import me.char321.sfadvancements.SFAdvancements;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import me.char321.sfadvancements.util.Messages;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * @author char321
 */
public class SaveCommand implements SubCommand {

    @Override
    public boolean onExecute(CommandSender sender, Command command, String label, String[] args) {
        try {
            SFAdvancements.getAdvManager().save();
            Messages.send(sender, "command.saved", "Successfully saved advancements.");
            return true;
        } catch (IOException e) {
            Messages.send(sender, "command.save-failed", "An error occurred while saving advancements!");
            Messages.send(sender, "command.check-console", "Check the console for details.");
            SFAdvancements.logger().log(Level.SEVERE, e, () -> "Could not save advancements");
            return false;
        }
    }

    @Override
    public @Nonnull String getCommandName() {
        return "save";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

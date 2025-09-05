package de.g4memas0n.core.bukkit.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * A command class to extend for all registered bukkit commands.
 * @param <P> the main class of the plugin.
 */
@SuppressWarnings("unused")
public abstract class BaseCommand<P extends JavaPlugin> extends SubCommand<P> implements TabExecutor {

    /**
     * Constructs a new command with the specified name.
     * @param name the name of the command.
     */
    public BaseCommand(@NotNull String name) {
        super(name);
    }

    /**
     * Constructs a new command with the specified name and permission.
     * @param name the name of the command.
     * @param permission the permission for the command or null.
     */
    public BaseCommand(@NotNull String name, @Nullable String permission) {
        super(name, permission);
    }

    @Override
    public boolean register(@NotNull P plugin) {
        if (!super.register(plugin)) return false;
        // Configure logger to use the plugin logger if not already configured
        if (logger.getParent() == null) {
            logger.setParent(plugin.getLogger());
            logger.setUseParentHandlers(true);
        }

        PluginCommand parent = plugin.getCommand(getName());
        if (parent == null) {
            // The command may be available through a parent command, therefore log warning and succeed registration
            logger.warning("Could not register command '" + getName() + "'! Is it registered to bukkit?");
            return true;
        }
        parent.setExecutor(this);
        parent.setTabCompleter(this);
        return true;
    }

    @Override
    public boolean unregister(@NotNull P plugin) {
        if (!super.unregister(plugin)) return false;
        PluginCommand parent = plugin.getCommand(getName());
        if (parent != null) {
            parent.setExecutor(null);
            parent.setTabCompleter(null);
        }
        return true;
    }

    /**
     * Returns the description of the command.<p>
     * If the description does not match the description of the specified bukkit command, the description of it will
     * be updated.
     * @param command the matching bukkit command.
     * @return the command description or null.
     */
    public @Nullable String getDescription(@NotNull Command command) {
        String description = getDescription();
        if (description != null && !description.equals(command.getDescription())) {
            command.setDescription(description);
        }
        return description;
    }

    /**
     * Returns the usage of the command.<p>
     * If the usage does not match the usage of the specified bukkit command, the usage of it will be updated.
     * @param command the matching bukkit command.
     * @return the command usage or null.
     */
    public @Nullable String getUsage(@NotNull Command command) {
        String usage = getUsage();
        if (usage != null && !usage.equals(command.getUsage())) {
            command.setUsage(usage);
        }
        return usage;
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                   @NotNull String alias, @NotNull String[] arguments) {
        // Check if sender can perform the command.
        if (!testPermission(sender)) {
            String message = command.getPermissionMessage();
            if (message != null && !message.isEmpty()) {
                for (String line : message.split("\n")) {
                    sender.sendMessage(line);
                }
            }
            return true;
        }

        boolean success = execute(sender, alias, arguments);
        if (!success) {
            // Send description and usage message if command was not successful.
            String usage = getUsage(command);
            if (usage != null && !usage.isEmpty()) {
                String description = getDescription(command);
                if (description != null && !description.isEmpty()) {
                    for (String line : description.split("\n")) {
                        sender.sendMessage(line);
                    }
                }

                usage = usage.replace("<command>", alias);
                for (String line : usage.split("\n")) {
                    sender.sendMessage(line);
                }
                return true;
            }
        }
        return success;
    }

    @Override
    public final @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                     @NotNull String alias, @NotNull String[] arguments) {
        // Check if sender can perform the command and therefore can see tab-completions.
        if (!testPermission(sender)) {
            return Collections.emptyList();
        }

        List<String> completions = tabComplete(sender, alias, arguments);
        completions.sort(String.CASE_INSENSITIVE_ORDER);
        return completions;
    }
}

package de.g4memas0n.core.bukkit.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An abstract command class to extend for registering to bukkit.
 * @param <P> the main class of the plugin
 */
@SuppressWarnings("unused")
public abstract class BaseCommand<P extends JavaPlugin> extends SubCommand<P> implements TabExecutor {

    private Map<String, SubCommand<P>> commands;

    /**
     * Constructs a new command with the given name and permission.
     * @param name the name of the command
     * @param permission the permission for the command or null
     */
    public BaseCommand(@NotNull String name, @Nullable String permission) {
        super(name, permission);
    }

    @Override
    public boolean register(@NotNull P plugin) {
        if (isRegistered()) {
            return false;
        }

        // Command must be registered to bukkit, otherwise the command executor and tab completer can not be set.
        PluginCommand parent = plugin.getCommand(getName());
        if (parent == null) {
            plugin.getLogger().warning("Could not register command '" + getName() + "'! Is it registered to bukkit?");
            return false;
        }

        if (super.register(plugin)) {
            // Registration of all sub-commands.
            if (commands != null) {
                SubCommand<P> command;
                for (Iterator<SubCommand<P>> iterator = commands.values().iterator(); iterator.hasNext(); ) {
                    command = iterator.next();
                    if (!command.isRegistered() && !command.register(plugin)) {
                        // There may be a BaseCommand that could not be registered. In this case, all its sub-commands
                        // are also not registered, therefore the command cannot be used. To avoid potential errors,
                        // the command is removed from the list of sub-commands.
                        plugin.getLogger().severe("Failed to register sub-command '" + command.getName() + "' of command '" + getName() + "'!");
                        iterator.remove();
                    }
                }
            }

            parent.setExecutor(this);
            parent.setTabCompleter(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean unregister(@NotNull P plugin) {
        if (!isRegistered()) {
            return false;
        }

        if (super.unregister(plugin)) {
            PluginCommand parent = plugin.getCommand(getName());
            if (parent != null) {
                parent.setTabCompleter(null);
                parent.setExecutor(null);
            }

            if (commands != null) {
                for (SubCommand<P> command : commands.values()) {
                    if (command.isRegistered() && !command.unregister(plugin)) {
                        // Should not be the case, but if so a message will be logged.
                        plugin.getLogger().warning("Failed to unregister sub-command '" + command.getName() + "' of command '" + getName() + "'!");
                    }
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Registers a sub-command to the base command.
     *
     * @param command the sub-command to register
     * @return true if the registration was successful, false otherwise
     */
    public boolean register(@NotNull SubCommand<P> command) {
        if (isRegistered()) {
            return false;
        }

        if (commands == null) {
            commands = new HashMap<>();
        }
        return commands.put(command.getName(), command) == null;
    }

    /**
     * Unregisters a sub-command from the base command.
     *
     * @param command the sub-command to unregister
     * @return true if the unregistration was successful, false otherwise
     */
    public boolean unregister(@NotNull SubCommand<P> command) {
        if (isRegistered()) {
            return false;
        }

        if (commands != null && commands.remove(command.getName(), command)) {
            if (commands.isEmpty()) {
                commands = null;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the description of the command.
     * <p>
     * If the description does not match the description of the given bukkit command, the description of it will
     * be updated.
     *
     * @param command the matching bukkit command
     * @return the command description or null
     */
    public @Nullable String getDescription(@NotNull Command command) {
        String description = getDescription();
        if (description != null && !description.equals(command.getDescription())) {
            command.setDescription(description);
        }
        return description;
    }

    /**
     * Returns the usage of the command.
     * <p>
     * If the usage does not match the usage of the given bukkit command, the usage of it will be updated.
     *
     * @param command the matching bukkit command
     * @return the command usage or null
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

                for (String line : usage.replace("<command>", alias).split("\n")) {
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

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments) {
        // Search for registered sub-command
        if (commands != null) {
            SubCommand<P> command = commands.get(arguments[0].toLowerCase(Locale.ROOT));
            if (command != null && command.testPermission(sender)) {
                // Execute registered sub-command
                boolean success = command.execute(sender, arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
                if (!success) {
                    // Send description and usage message if command was not successful
                    String usage = command.getUsage();
                    if (usage != null && !usage.isEmpty()) {
                        String description = command.getDescription();
                        if (description != null && !description.isEmpty()) {
                            for (String line : description.split("\n")) {
                                sender.sendMessage(line);
                            }
                        }

                        for (String line : usage.replace("<command>", arguments[0]).replace("<parent>", alias).split("\n")) {
                            sender.sendMessage(line);
                        }
                        return true;
                    }
                }

                return success;
            }
        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments) {
        List<String> completions = new ArrayList<>();

        // Search for registered sub-command
        if (commands != null) {
            if (arguments.length > 1) {
                SubCommand<P> command = commands.get(arguments[0].toLowerCase(Locale.ROOT));
                if (command != null && command.testPermission(sender)) {
                    completions.addAll(command.tabComplete(sender, arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length)));
                }
            } else {
                for (SubCommand<P> command : commands.values()) {
                    if (command.testPermission(sender)) {
                        if (StringUtil.startsWithIgnoreCase(command.getName(), arguments[0])) {
                            completions.add(command.getName());
                        }
                    }
                }
            }
        }

        return completions;
    }
}

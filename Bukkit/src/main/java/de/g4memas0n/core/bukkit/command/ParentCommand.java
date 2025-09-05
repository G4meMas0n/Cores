package de.g4memas0n.core.bukkit.command;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A command class to extend for all parent commands.
 * @param <P> the main class of the plugin.
 */
@SuppressWarnings("unused")
public abstract class ParentCommand<P extends JavaPlugin> extends BaseCommand<P> {

    private final Map<String, SubCommand<P>> children;

    /**
     * Constructs a new parent command with the specified name.
     * @param name the name of the command.
     */
    public ParentCommand(@NotNull String name) {
        this(name, null);
    }

    /**
     * Constructs a new parent command with the specified name and permission.
     * @param name the name of the command.
     * @param permission the permission of the command or null.
     */
    public ParentCommand(@NotNull String name, @Nullable String permission) {
        super(name, permission);
        this.children = new HashMap<>();
    }

    @Override
    public boolean register(@NotNull P plugin) {
        if (!super.register(plugin)) return false;
        for (SubCommand<P> command : this.children.values()) {
            if (command.isRegistered()) {
                logger.warning("The sub-command '" + command.getName() + "' of command '" + getName() + "' is already registered.");
                continue;
            }
            if (!command.register(plugin)) {
                logger.warning("Failed to register sub-command '" + command.getName() + "' of command '" + getName() + "'!");
            }
        }
        return true;
    }

    /**
     * Registers a sub-command to this command.
     * @param command the sub-command to register
     * @return true if the registration was successful, false otherwise.
     */
    public boolean register(@NotNull SubCommand<P> command) {
        if (isRegistered()) return false;
        SubCommand<P> previous = this.children.put(command.getName(), command);
        return previous == null;
    }

    @Override
    public boolean unregister(@NotNull P plugin) {
        if (!super.unregister(plugin)) return false;
        for (SubCommand<P> command : this.children.values()) {
            if (command.isRegistered() && !command.unregister(plugin)) {
                // Should never be the case, but if so a message should be logged.
                logger.warning("Failed to unregister sub-command '" + command.getName() + "' of command '" + getName() + "'!");
            }
        }
        return true;
    }

    /**
     * Unregisters a sub-command from this command.
     * @param command the sub-command to unregister.
     * @return true if the unregistration was successful, false otherwise.
     */
    public boolean unregister(@NotNull SubCommand<P> command) {
        if (this.isRegistered()) return false;
        return this.children.remove(command.getName(), command);
    }

    @Override
    public final boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments) {
        SubCommand<P> command = this.children.get(arguments[0].toLowerCase(Locale.ROOT));
        if (command == null || !command.isRegistered() || !command.testPermission(sender)) return false;
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

                usage = usage.replace("<command>", arguments[0]).replace("<parent>", alias);
                for (String line : usage.split("\n")) {
                    sender.sendMessage(line);
                }
                return true;
            }
        }
        return success;
    }

    @Override
    public final @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments) {
        if (arguments.length > 1) {
            // Tab-complete registered sub-command
            SubCommand<P> command = this.children.get(arguments[0].toLowerCase(Locale.ROOT));
            if (command != null && command.isRegistered() && command.testPermission(sender)) {
                return command.tabComplete(sender, arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
            }
            return Collections.emptyList();
        }

        // Search for registered sub-command
        List<String> completions = new ArrayList<>();
        for (SubCommand<P> command : this.children.values()) {
            if (!command.isRegistered() || !command.testPermission(sender)) continue;
            if (StringUtil.startsWithIgnoreCase(command.getName(), arguments[0])) {
                completions.add(command.getName());
            }
        }
        return completions;
    }
}

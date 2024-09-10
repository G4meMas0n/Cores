package de.g4memas0n.core.bukkit.command;

import com.google.common.base.Preconditions;
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
import java.util.Objects;

/**
 * An abstract command class to extend for registering to bukkit.
 * @param <T> the main class of the plugin.
 */
@SuppressWarnings("unused")
public abstract class BaseCommand<T extends JavaPlugin> extends SubCommand<T> implements TabExecutor {

    private Map<String, SubCommand<T>> commands;

    /**
     * Constructs a new command with the given name and permission.
     * @param name the name of the command.
     * @param permission the permission for the command or null.
     */
    public BaseCommand(@NotNull String name, @Nullable String permission) {
        super(name, permission);
    }

    @Override
    public boolean register(@NotNull T plugin) {
        if (this.plugin == null) {
            final PluginCommand command = plugin.getCommand(name);
            if (command == null) {
                plugin.getLogger().warning("Could not register command '" + name + "'! Is it registered to bukkit/spigot?");
                return false;
            }

            if (commands != null) {
                SubCommand<T> subcommand;

                for (Iterator<SubCommand<T>> iterator = commands.values().iterator(); iterator.hasNext();) {
                    if (!(subcommand = iterator.next()).register(plugin) && subcommand.plugin == null) {
                        // There could be a BasicCommand that could not be registered. In this case, its subcommands
                        // are also not registered, so we cannot use this command. Therefore, the command is removed to
                        // reduce potential errors. Note that the absence of the plugin reference is checked, as the
                        // command may already be registered.
                        plugin.getLogger().warning("Failed to register subcommand '" + subcommand.name + "' for command '" + name + "'!");
                        iterator.remove();
                    }
                }
            }

            this.plugin = plugin;
            command.setExecutor(this);
            command.setTabCompleter(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean unregister(@NotNull T plugin) {
        if (this.plugin == plugin) {
            final PluginCommand command = plugin.getCommand(name);
            if (command != null) {
                command.setTabCompleter(null);
                command.setExecutor(null);
            }

            if (commands != null) {
                for (SubCommand<T> subcommand : commands.values()) {
                    if (!subcommand.unregister(plugin) && subcommand.plugin != null) {
                        // This should not be the case, but if so a warning message should be logged. Note that the
                        // presence of the plugin reference is checked, as the command may already be unregistered.
                        plugin.getLogger().warning("Failed to unregister subcommand '" + subcommand.name + "' for command '" + name + "'!");
                    }
                }
            }

            this.plugin = null;
            return true;
        }

        return false;
    }

    /**
     * Registers a subcommand to an implementing bukkit command.
     * @param command the subcommand to register.
     * @return true if it has been registered, false otherwise.
     */
    public boolean register(@NotNull SubCommand<T> command) {
        Preconditions.checkState(plugin == null);
        if (commands == null) {
            commands = new HashMap<>();
        }

        return commands.putIfAbsent(command.getName(), command) == null;
    }

    /**
     * Unregisters a subcommand from an implementing bukkit command.
     * @param command the subcommand to unregister.
     * @return true if it has been unregistered, false otherwise.
     */
    public boolean unregister(@NotNull SubCommand<T> command) {
        Preconditions.checkState(plugin == null);
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
     * If the set description does not match the description of the given bukkit command, the description of it will
     * be updated.
     * @param command the corresponding bukkit command or null
     * @return the command description or null.
     */
    public @Nullable String getDescription(@Nullable Command command) {
        String description = super.getDescription();
        if (command != null && description != null && !description.equals(command.getDescription())) {
            command.setDescription(description);
        }

        return description;
    }

    @Override
    public @Nullable String getDescription() {
        return plugin != null ? getDescription(plugin.getCommand(name)) : super.getDescription();
    }

    /**
     * Returns the usage of the command.
     * <p>
     * If the set usage does not match the usage of the given bukkit command, the usage of it will be updated.
     * @param command the corresponding bukkit command or null
     * @return the command usage or null.
     */
    public @Nullable String getUsage(@Nullable Command command) {
        String usage = super.getUsage();
        if (command != null && usage != null && !usage.equals(command.getUsage())) {
            command.setUsage(usage);
        }

        return usage;
    }

    @Override
    public @Nullable String getUsage() {
        return plugin != null ? getUsage(plugin.getCommand(name)) : super.getUsage();
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                   @NotNull String alias, @NotNull String[] arguments) {
        String permission = command.getPermission() != null ? command.getPermission() : this.permission;
        if (permission != null && !sender.hasPermission(permission)) {
            String message = command.getPermissionMessage();
            if (message != null) {
                sender.sendMessage(message);
            }

            return true;
        }

        SubCommand<T> subcommand = null;

        if (commands != null && arguments.length > 0) {
            subcommand = commands.get(arguments[0].toLowerCase(Locale.ROOT));

            if (subcommand != null && (subcommand.permission == null || sender.hasPermission(subcommand.permission))) {
                if (subcommand.execute(sender, arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length))) {
                    return true;
                }

                if (subcommand.hasUsage()) {
                    if (subcommand.hasDescription()) {
                        sender.sendMessage(Objects.requireNonNull(subcommand.getDescription()));
                    }

                    sender.sendMessage(Objects.requireNonNull(subcommand.getUsage())
                            .replace("<command>", arguments[0])
                            .replace("<parent>", alias));
                    return true;
                }
            }
        }

        if (subcommand == null && execute(sender, alias, arguments)) {
            return true;
        }

        if (hasUsage()) {
            if (hasDescription()) {
                sender.sendMessage(Objects.requireNonNull(getDescription(command)));
            }

            sender.sendMessage(Objects.requireNonNull(getUsage(command)).replace("<command>", alias));
            return true;
        }

        return false;
    }

    @Override
    public final @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                     @NotNull String alias, @NotNull String[] arguments) {
        String permission = command.getPermission() != null ? command.getPermission() : this.permission;
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }

        if (commands != null && arguments.length > 1) {
            SubCommand<T> subcommand = commands.get(arguments[0].toLowerCase(Locale.ROOT));

            if (subcommand != null && (subcommand.permission == null || sender.hasPermission(subcommand.permission))) {
                String[] args = Arrays.copyOfRange(arguments, 1, arguments.length);
                return subcommand.tabComplete(sender, arguments[0], args);
            }
        }

        List<String> completions = new ArrayList<>();
        if (arguments.length == 1 && commands != null) {
            for (SubCommand<T> subcommand : commands.values()) {
                if (subcommand.permission == null || sender.hasPermission(subcommand.permission)) {
                    if (StringUtil.startsWithIgnoreCase(subcommand.getName(), arguments[0])) {
                        completions.add(subcommand.getName());
                    }
                }
            }
        }

        completions.addAll(tabComplete(sender, alias, arguments));
        Collections.sort(completions);
        return completions;
    }
}

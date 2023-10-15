package de.g4memas0n.core.bukkit.command;

import com.google.common.base.Preconditions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
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
public abstract class BasicCommand<T extends JavaPlugin> extends SubCommand<T> implements TabExecutor {

    private Map<String, SubCommand<T>> commands;

    /**
     * Constructs a new command with the given name and permission.
     * @param name the name of the command.
     * @param permission the permission for the command or null.
     */
    public BasicCommand(@NotNull final String name, @Nullable final String permission) {
        super(name, permission);
    }

    @Override
    public boolean register(@NotNull final T plugin) {
        if (this.plugin == null) {
            final PluginCommand command = plugin.getCommand(this.name);
            if (command == null) {
                plugin.getLogger().warning("Failed to register command '" + this.name + "'! Is it registered to bukkit/spigot?");
                return false;
            }

            if (this.commands != null) {
                SubCommand<T> subcommand;

                for (Iterator<SubCommand<T>> iterator = this.commands.values().iterator(); iterator.hasNext();) {
                    if (!(subcommand = iterator.next()).register(plugin) && subcommand.plugin == null) {
                        // There could be a BasicCommand that could not be registered. In this case, its subcommands
                        // are also not registered, so we cannot use this command. Therefore, the command is removed to
                        // reduce potential errors. Note that the absence of the plugin reference is checked, as the
                        // command may already be registered.
                        plugin.getLogger().warning("Failed to register subcommand '" + subcommand.name + "' for command '" + this.name + "'!");
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
    public boolean unregister(@NotNull final T plugin) {
        if (this.plugin == plugin) {
            final PluginCommand command = plugin.getCommand(this.name);
            if (command != null) {
                command.setTabCompleter(null);
                command.setExecutor(null);
            }

            if (this.commands != null) {
                for (SubCommand<T> subcommand : this.commands.values()) {
                    if (!subcommand.unregister(plugin) && subcommand.plugin != null) {
                        // This should not be the case, but if so a warning message should be logged. Note that the
                        // presence of the plugin reference is checked, as the command may already be unregistered.
                        plugin.getLogger().warning("Failed to unregister subcommand '" + subcommand.name + "' for command '" + this.name + "'!");
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
    public boolean register(@NotNull final SubCommand<T> command) {
        Preconditions.checkState(this.plugin == null);
        if (this.commands == null) {
            this.commands = new HashMap<>();
        }

        return this.commands.putIfAbsent(command.getName(), command) == null;
    }

    /**
     * Unregisters a subcommand from an implementing bukkit command.
     * @param command the subcommand to unregister.
     * @return true if it has been unregistered, false otherwise.
     */
    public boolean unregister(@NotNull final SubCommand<T> command) {
        Preconditions.checkState(this.plugin == null);
        if (this.commands != null && this.commands.remove(command.getName(), command)) {
            if (this.commands.isEmpty()) {
                this.commands = null;
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
    public @Nullable String getDescription(@Nullable final Command command) {
        String description = super.getDescription();

        if (command != null && description != null && !description.equals(command.getDescription())) {
            command.setDescription(description);
        }

        return description;
    }

    @Override
    public @Nullable String getDescription() {
        return this.plugin != null ? getDescription(this.plugin.getCommand(this.name)) : super.getDescription();
    }

    /**
     * Returns the usage of the command.
     * <p>
     * If the set usage does not match the usage of the given bukkit command, the usage of it will be updated.
     * @param command the corresponding bukkit command or null
     * @return the command usage or null.
     */
    public @Nullable String getUsage(@Nullable final Command command) {
        String usage = super.getUsage();

        if (command != null && usage != null && !usage.equals(command.getUsage())) {
            command.setUsage(usage);
        }

        return usage;
    }

    @Override
    public @Nullable String getUsage() {
        return this.plugin != null ? getUsage(this.plugin.getCommand(this.name)) : super.getUsage();
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                                   @NotNull final String alias, @NotNull String[] arguments) {
        final String permission = command.getPermission() != null ? command.getPermission() : this.permission;

        if (permission != null && !sender.hasPermission(permission)) {
            final String message = command.getPermissionMessage();

            if (message != null) {
                sender.sendMessage(message);
            }

            return true;
        }

        SubCommand<T> subcommand = null;

        if (this.commands != null && arguments.length > 0) {
            subcommand = this.commands.get(arguments[0].toLowerCase(Locale.ROOT));

            if (subcommand != null && (subcommand.permission == null || sender.hasPermission(subcommand.permission))) {
                arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

                if (sender instanceof Player
                        ? subcommand.execute((Player) sender, arguments[0], arguments)
                        : subcommand.execute(sender, arguments[0], arguments)) {
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

        if (subcommand == null) {
            if (sender instanceof Player
                    ? execute((Player) sender, alias, arguments)
                    : execute(sender, alias, arguments)) {
                return true;
            }
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
    public final @NotNull List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command,
                                                     @NotNull final String alias, @NotNull String[] arguments) {
        final String permission = command.getPermission() != null ? command.getPermission() : this.permission;

        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }

        if (this.commands != null && arguments.length > 1) {
            final SubCommand<T> subcommand = this.commands.get(arguments[0].toLowerCase(Locale.ROOT));

            if (subcommand != null && (subcommand.permission == null || sender.hasPermission(subcommand.permission))) {
                arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

                return sender instanceof Player
                        ? subcommand.tabComplete((Player) sender, arguments[0], arguments)
                        : subcommand.tabComplete(sender, arguments[0], arguments);
            }
        }

        final List<String> completions = new ArrayList<>();

        if (arguments.length == 1 && this.commands != null) {
            for (SubCommand<T> subcommand : this.commands.values()) {
                if (subcommand.permission == null || sender.hasPermission(subcommand.permission)) {
                    if (StringUtil.startsWithIgnoreCase(subcommand.getName(), arguments[0])) {
                        completions.add(subcommand.getName());
                    }
                }
            }
        }

        completions.addAll(sender instanceof Player
                ? tabComplete((Player) sender, alias, arguments)
                : tabComplete(sender, alias, arguments));
        Collections.sort(completions);

        return completions;
    }
}

package com.github.g4memas0n.cores.bukkit.command;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class MainCommand<T extends JavaPlugin> extends BasicCommand<T> implements TabExecutor {

    private Map<String, BasicCommand<T>> commands;
    private PluginCommand parent;

    public MainCommand(@NotNull final String name, @Nullable final String permission, final int minArgs) {
        super(name, permission, minArgs);
    }

    public MainCommand(@NotNull final String name, @Nullable final String permission, final int minArgs, final int maxArgs) {
        super(name, permission, minArgs, maxArgs);
    }

    public boolean register(@NotNull final BasicCommand<T> command) {
        Preconditions.checkState(this.parent == null);
        if (this.commands == null) {
            this.commands = new HashMap<>();
        }

        return this.commands.putIfAbsent(command.getName(), command) == null;
    }

    public boolean unregister(@NotNull final BasicCommand<T> command) {
        Preconditions.checkState(this.parent == null);
        if (this.commands != null && this.commands.remove(command.getName(), command)) {
            if (this.commands.isEmpty()) {
                this.commands = null;
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean register(@NotNull final T plugin) {
        final PluginCommand parent = plugin.getCommand(this.name);

        if (parent == null) {
            plugin.getLogger().warning("Failed to register command '" + this.name + "'! Is it registered to bukkit/spigot?");
            return false;
        }

        if (super.register(plugin)) {
            if (this.commands != null) {
                this.commands.values().forEach(command -> command.register(plugin));
            }

            this.parent = parent;
            this.parent.setExecutor(this);
            this.parent.setTabCompleter(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean unregister() {
        if (this.parent != null) {
            this.parent.setTabCompleter(null);
            this.parent.setExecutor(null);
            this.parent = null;

            if (this.commands != null) {
                this.commands.values().forEach(BasicCommand::unregister);
            }

            return super.unregister();
        }

        return false;
    }

    @Override
    public @Nullable String getDescription() {
        final String description = super.getDescription();

        if (this.parent != null && !this.parent.getDescription().isEmpty() && description != null) {
            this.parent.setDescription(description);
        }

        return description;
    }

    @Override
    public @Nullable String getUsage() {
        final String usage = super.getUsage();

        if (this.parent != null && !this.parent.getUsage().isEmpty() && usage != null) {
            this.parent.setUsage(usage);
        }

        return usage;
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                                   @NotNull final String alias, @NotNull final String[] arguments) {
        final String permission = this.parent.getPermission() != null ? this.parent.getPermission() : this.permission;

        if (permission != null && !sender.hasPermission(permission)) {
            final String message = this.parent.getPermissionMessage();

            if (message != null) {
                sender.sendMessage(message);
            }

            return true;
        }

        boolean invalid = false;

        if (this.commands != null && arguments.length > 0) {
            final BasicCommand<T> subcommand = this.commands.get(arguments[0].toLowerCase());

            if (subcommand != null && (subcommand.permission == null || sender.hasPermission(subcommand.permission))) {
                final String[] args = Arrays.copyOfRange(arguments, 1, arguments.length);

                if (!subcommand.argsInRange(arguments.length) || !subcommand.execute(sender, subcommand.name, args)) {
                    if (subcommand.hasUsage()) {
                        if (subcommand.hasDescription()) {
                            sender.sendMessage(Objects.requireNonNull(subcommand.getDescription()));
                        }

                        sender.sendMessage(Objects.requireNonNull(subcommand.getUsage()).replace("<command>", subcommand.name));
                        return true;
                    }

                    invalid = true;
                }
            }
        }

        if (invalid || !this.argsInRange(arguments.length) || !this.execute(sender, alias, arguments)) {
            if (this.hasUsage()) {
                if (this.hasDescription()) {
                    sender.sendMessage(Objects.requireNonNull(this.getDescription()));
                }

                sender.sendMessage(Objects.requireNonNull(this.getUsage()).replace("<command>", alias));
            }
        }

        return true;
    }

    @Override
    public final @NotNull List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command,
                                                     @NotNull final String alias, @NotNull final String[] arguments) {
        final String permission = this.parent.getPermission() != null ? this.parent.getPermission() : this.permission;

        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }

        if (this.commands != null && arguments.length > 1) {
            final BasicCommand<T> subcommand = this.commands.get(arguments[0].toLowerCase());

            if (subcommand != null && (subcommand.permission == null || sender.hasPermission(subcommand.permission))) {
                final String[] args = Arrays.copyOfRange(arguments, 1, arguments.length);

                if (subcommand.argsInRange(arguments.length)) {
                    return subcommand.tabComplete(sender, subcommand.name, args);
                }

                return Collections.emptyList();
            }
        }

        final List<String> completions = new ArrayList<>();

        if (this.commands != null && arguments.length == 1) {
            for (final BasicCommand<T> subcommand : this.commands.values()) {
                if (subcommand.permission == null || sender.hasPermission(subcommand.permission)) {
                    if (StringUtil.startsWithIgnoreCase(subcommand.getName(), arguments[0])) {
                        completions.add(subcommand.getName());
                    }
                }
            }
        }

        if (this.argsInRange(arguments.length)) {
            completions.addAll(this.tabComplete(sender, alias, arguments));
        }

        Collections.sort(completions);

        return completions;
    }
}

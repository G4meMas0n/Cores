package com.github.g4memas0n.cores.bukkit.command;

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

public abstract class MainCommand<T extends JavaPlugin> extends BasicCommand<T> implements TabExecutor {

    private Map<String, BasicCommand<T>> commands;
    private PluginCommand parent;

    public MainCommand(@NotNull final String name, final int minArgs) {
        super(name, minArgs);
    }

    public MainCommand(@NotNull final String name, final int minArgs, final int maxArgs) {
        super(name, minArgs, maxArgs);
    }

    @Override
    public boolean register(@NotNull final T plugin) {
        final PluginCommand parent = plugin.getCommand(this.name);

        if (parent == null) {
            plugin.getLogger().warning("Failed to register command '" + this.name + "'! Is it registered to bukkit/spigot?");
            return false;
        }

        if (super.register(plugin)) {
            this.parent = parent;
            this.parent.setExecutor(this);
            this.parent.setTabCompleter(this);

            if (this.commands != null) {
                for (final BasicCommand<T> subcommand : this.commands.values()) {
                    if (subcommand.plugin == null && !subcommand.register(plugin)) {
                        subcommand.plugin = plugin;
                    }
                }
            }

            return true;
        }

        return false;
    }

    public boolean register(@NotNull final BasicCommand<T> command) {
        if (this.commands == null) {
            this.commands = new HashMap<>();
        }

        return this.commands.putIfAbsent(command.getName(), command) == null;
    }

    public boolean unregister(@NotNull final BasicCommand<T> command) {
        if (this.commands != null && this.commands.remove(command.getName(), command)) {
            if (this.commands.isEmpty()) {
                this.commands = null;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean unregister() {
        if (this.parent != null && super.unregister()) {
            if (this.commands != null) {
                for (final BasicCommand<T> subcommand : this.commands.values()) {
                    if (subcommand.plugin != null && !subcommand.unregister()) {
                        subcommand.plugin = null;
                    }
                }
            }

            this.parent.setTabCompleter(null);
            this.parent.setExecutor(null);
            this.parent = null;
            return true;
        }

        return false;
    }

    public @Nullable PluginCommand getParent() {
        return this.parent;
    }

    @Override
    public @NotNull String getDescription() {
        if (this.parent != null && !this.parent.getDescription().isEmpty()) {
            return this.parent.getDescription();
        }

        return super.getDescription();
    }

    @Override
    public boolean hasDescription() {
        return this.parent != null && !this.parent.getDescription().isEmpty() || super.hasDescription();
    }

    @Override
    public void setDescription(@NotNull final String description) {
        if (this.parent != null) {
            this.parent.setDescription(description);
        }

        super.setDescription(description);
    }

    @Override
    public @NotNull String getPermission() {
        if (this.parent != null && this.parent.getPermission() != null) {
            return this.parent.getPermission();
        }

        return super.getPermission();
    }

    @Override
    public boolean hasPermission() {
        return this.parent != null && this.parent.getPermission() != null || super.hasPermission();
    }

    @Override
    public void setPermission(@NotNull final String permission) {
        if (this.parent != null) {
            this.parent.setPermission(permission);
        }

        super.setPermission(permission);
    }

    @Override
    public @NotNull String getUsage() {
        if (this.parent != null && !this.parent.getUsage().isEmpty()) {
            return this.parent.getUsage();
        }

        return super.getUsage();
    }

    @Override
    public boolean hasUsage() {
        return this.parent != null && !this.parent.getUsage().isEmpty() || super.hasUsage();
    }

    @Override
    public void setUsage(@NotNull final String usage) {
        if (this.parent != null) {
            this.parent.setUsage(usage);
        }

        super.setUsage(usage);
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                                   @NotNull final String label, @NotNull String[] arguments) {
        if (this.hasPermission() && !sender.hasPermission(this.getPermission())) {
            if (this.parent.getPermissionMessage() != null) {
                sender.sendMessage(this.parent.getPermissionMessage());
            }

            return true;
        }

        if (this.commands != null && arguments.length > 0) {
            final BasicCommand<T> subcommand = this.commands.get(arguments[0].toLowerCase());

            if (subcommand != null && (!subcommand.hasPermission() || sender.hasPermission(subcommand.getPermission()))) {
                arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

                if (!subcommand.argsInRange(arguments.length) || !subcommand.execute(sender, arguments)) {
                    sender.sendMessage(subcommand.getDescription());
                    sender.sendMessage(subcommand.getUsage());
                }

                return true;
            }
        }

        if (!this.argsInRange(arguments.length) || !this.execute(sender, arguments)) {
            sender.sendMessage(this.getDescription());
            sender.sendMessage(this.getUsage());
        }

        return true;
    }

    @Override
    public final @NotNull List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command,
                                                     @NotNull final String label, @NotNull String[] arguments) {
        if (this.hasPermission() && !sender.hasPermission(this.getPermission())) {
            return Collections.emptyList();
        }

        if (this.commands != null && arguments.length > 1) {
            final BasicCommand<T> subcommand = this.commands.get(arguments[0].toLowerCase());

            if (subcommand != null && (!subcommand.hasPermission() || sender.hasPermission(subcommand.getPermission()))) {
                arguments = Arrays.copyOfRange(arguments, 1, arguments.length);

                if (subcommand.argsInRange(arguments.length)) {
                    return subcommand.tabComplete(sender, arguments);
                }

                return Collections.emptyList();
            }
        }

        final List<String> completions = new ArrayList<>();

        if (this.commands != null && arguments.length == 1) {
            for (final BasicCommand<T> subcommand : this.commands.values()) {
                if (!subcommand.hasPermission() || sender.hasPermission(subcommand.getPermission())) {
                    if (StringUtil.startsWithIgnoreCase(subcommand.getName(), arguments[0])) {
                        completions.add(subcommand.getName());
                    }
                }
            }
        }

        if (this.argsInRange(arguments.length)) {
            completions.addAll(this.tabComplete(sender, arguments));
        }

        Collections.sort(completions);

        return completions;
    }
}

package com.github.g4memas0n.cores.bukkit.command;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class BasicCommand<T extends JavaPlugin> {

    protected final String name;
    protected final int minArgs;
    protected final int maxArgs;

    protected T plugin;

    private String description;
    private String permission;
    private String usage;

    public BasicCommand(@NotNull final String name, final int minArgs) {
        this(name, minArgs, -1);
    }

    public BasicCommand(@NotNull final String name, final int minArgs, final int maxArgs) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.permission = "";
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean register(@NotNull final T plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            return true;
        }

        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean unregister() {
        if (this.plugin != null) {
            this.plugin = null;
            return true;
        }

        return false;
    }

    public final @NotNull String getName() {
        return this.name;
    }

    @SuppressWarnings("unused")
    public final int getMinArgs() {
        return this.minArgs;
    }

    @SuppressWarnings("unused")
    public final int getMaxArgs() {
        return this.maxArgs;
    }

    public final boolean argsInRange(final int arguments) {
        return this.maxArgs > 0
                ? arguments >= this.minArgs && arguments <= this.maxArgs
                : arguments >= this.minArgs;
    }

    public @NotNull String getDescription() {
        return this.description;
    }

    public void setDescription(@NotNull final String description) {
        this.description = description;
    }

    public @NotNull String getPermission() {
        return this.permission;
    }

    public void setPermission(@NotNull final String permission) {
        this.permission = permission;
    }

    public @NotNull String getUsage() {
        return this.usage;
    }

    public void setUsage(@NotNull final String usage) {
        this.usage = usage;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean execute(@NotNull final CommandSender sender,
                                    @NotNull final String[] arguments);

    public abstract @NotNull List<String> tabComplete(@NotNull final CommandSender sender,
                                                      @NotNull final String[] arguments);

    @Override
    public @NotNull String toString() {
        return this.getClass().getSimpleName() + "{name='" + this.name + "', minArgs=" + this.minArgs
                + ", maxArgs=" + this.maxArgs + ", permission='" + this.permission + "'}";
    }

    @Override
    public boolean equals(@Nullable final Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof BasicCommand)) {
            return false;
        }

        final BasicCommand<?> other = (BasicCommand<?>) object;
        return this.name.equals(other.name) && this.minArgs == other.minArgs && this.maxArgs == other.maxArgs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.minArgs, this.maxArgs);
    }
}

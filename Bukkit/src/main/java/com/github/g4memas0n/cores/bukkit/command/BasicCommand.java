package com.github.g4memas0n.cores.bukkit.command;

import com.github.g4memas0n.cores.bukkit.Registrable;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class BasicCommand<T extends JavaPlugin> implements Registrable<T> {

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
    }

    @Override
    public boolean register(@NotNull final T plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            return true;
        }

        return false;
    }

    @Override
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

    public @Nullable String getDescription() {
        return this.description;
    }

    public void setDescription(@NotNull final String description) {
        this.description = description;
    }

    public @Nullable String getPermission() {
        return this.permission != null ? this.permission : "";
    }

    public void setPermission(@NotNull final String permission) {
        this.permission = permission;
    }

    public @Nullable String getUsage() {
        return this.usage != null ? this.usage : "";
    }

    public void setUsage(@NotNull final String usage) {
        this.usage = usage;
    }

    /**
     * Executes this command for the given {@code sender} with the given {@code arguments}, returning its success.
     * <p>
     *     Note:<br>
     *     This method gets only called if the command source ({@code sender}) is permitted to perform this command.
     *     This means that the implementation of this method is not required to check the permission for the given
     *     {@code sender}.<br>
     *     If the implementation of this method returns {@code false}, the description and usage of this command
     *     will be send to the command source ({@code sender}).
     * </p>
     *
     * @param sender the source who executed the command.
     * @param arguments the passed arguments of the sender.
     * @return {@code true} if the execution was successful and valid.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean execute(@NotNull final CommandSender sender,
                                    @NotNull final String[] arguments);

    /**
     * Requests a list of possible completions for the last element in the given {@code arguments} if it gets executed
     * by the given {@code sender}.
     * <p>
     *     Note:<br>
     *     This method gets only called if the command source ({@code sender}) is permitted to perform this command.
     *     This means that the implementation of this method is not required to check the permission for the given
     *     {@code sender}.
     * </p>
     *
     * @param sender the source who tab-completed the command.
     * @param arguments the passed arguments of the sender, including the final partial argument to be completed.
     * @return a list of possible completions for the final arguments.
     */
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

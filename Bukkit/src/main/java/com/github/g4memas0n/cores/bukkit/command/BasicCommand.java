package com.github.g4memas0n.cores.bukkit.command;

import com.github.g4memas0n.cores.bukkit.util.I18n;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class BasicCommand<T extends JavaPlugin> {

    protected final String name;
    protected final String permission;
    protected final int minArgs;
    protected final int maxArgs;

    protected T plugin;

    private String description;
    private String usage;

    public BasicCommand(@NotNull final String name, @Nullable final String permission, final int minArgs) {
        this(name, permission, minArgs, -1);
    }

    public BasicCommand(@NotNull final String name, @Nullable final String permission, final int minArgs, final int maxArgs) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.permission = permission;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    public boolean register(@NotNull final T plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            return true;
        }

        return false;
    }

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
        if (this.description != null) {
            return I18n.has(this.description) ? I18n.tl(this.description) : this.description;
        }

        return null;
    }

    public boolean hasDescription() {
        return this.description != null;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    public @Nullable String getUsage() {
        if (this.usage != null) {
            return I18n.has(this.usage) ? I18n.tl(this.usage) : this.usage;
        }

        return null;
    }

    public boolean hasUsage() {
        return this.usage != null;
    }

    public void setUsage(@Nullable final String usage) {
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
     * @param alias the alias that was used for the command.
     * @param arguments the passed arguments of the sender.
     * @return {@code true} if the execution was successful and valid.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public abstract boolean execute(@NotNull final CommandSender sender, @NotNull final String alias,
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
     * @param alias the alias that was used for the command.
     * @param arguments the passed arguments of the sender, including the final partial argument to be completed.
     * @return a list of possible completions for the final arguments.
     */
    public abstract @NotNull List<String> tabComplete(@NotNull final CommandSender sender, @NotNull final String alias,
                                                      @NotNull final String[] arguments);

    @Override
    public @NotNull String toString() {
        return getClass().getSimpleName() + "{name='" + this.name + "', permission='" + this.permission
                + "', minArgs=" + this.minArgs + ", maxArgs=" + this.maxArgs +  "}";
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

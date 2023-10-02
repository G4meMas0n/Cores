package com.github.g4memas0n.cores.bukkit.command;

import com.github.g4memas0n.cores.bukkit.util.I18n;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class SubCommand<T extends JavaPlugin> {

    protected final String name;
    protected final String permission;

    protected T plugin;

    private String description;
    private String usage;

    public SubCommand(@NotNull final String name, @Nullable final String permission) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.permission = permission;
    }

    public boolean register(@NotNull final T plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            return true;
        }

        return false;
    }

    public boolean unregister(@NotNull final T plugin) {
        if (this.plugin == plugin) {
            this.plugin = null;
            return true;
        }

        return false;
    }

    public final @NotNull String getName() {
        return this.name;
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
    public abstract boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments);

    /**
     * Executes this command for the given {@code player} with the given {@code arguments}, returning its success.
     * <p>
     *     Note:<br>
     *     This methods gets called automatically if the command is executed by a player. The default implementation
     *     of this method, casts the player as sender and calls {@link #execute(CommandSender, String, String[])}.
     * </p>
     * @param player the player who executed the command.
     * @param alias the alias that was used for the command.
     * @param arguments the passed arguments of the player.
     * @return {@code true} if the execution was successful and valid.
     * @see #execute(CommandSender, String, String[])
     */
    public boolean execute(@NotNull Player player, @NotNull String alias, @NotNull String[] arguments) {
        return execute((CommandSender) player, alias, arguments);
    }

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
    public abstract @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments);

    /**
     * Requests a list of possible completions for the last element in the given {@code arguments} if it gets executed
     * by the given {@code player}.
     * <p>
     *     Note:<br>
     *     This methods gets called automatically if the command is executed by a player. The default implementation
     *     of this method, casts the player as sender and calls {@link #tabComplete(CommandSender, String, String[])}.
     * </p>
     * 
     * @param player the player who tab-completed the command.
     * @param alias the alias that was used for the command.
     * @param arguments the passed arguments of the player, including the final partial argument to be completed.
     * @return a list of possible completions for the final arguments.
     * @see #tabComplete(CommandSender, String, String[]) 
     */
    public @NotNull List<String> tabComplete(@NotNull Player player, @NotNull String alias, @NotNull String[] arguments) {
        return tabComplete((CommandSender) player, alias, arguments);
    }

    @Override
    public @NotNull String toString() {
        return getClass().getSimpleName() + "{name='" + this.name + "', permission='" + this.permission + "}";
    }

    @Override
    public boolean equals(@Nullable final Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof SubCommand<?>)) {
            return false;
        }

        SubCommand<?> other = (SubCommand<?>) object;
        return this.name.equals(other.name) && Objects.equals(this.permission, other.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.permission);
    }
}

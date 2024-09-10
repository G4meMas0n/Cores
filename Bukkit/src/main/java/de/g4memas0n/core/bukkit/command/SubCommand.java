package de.g4memas0n.core.bukkit.command;

import de.g4memas0n.core.bukkit.util.I18n;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * An abstract subcommand class to extend.
 * @param <T> the main class of the plugin.
 */
@SuppressWarnings("unused")
public abstract class SubCommand<T extends JavaPlugin> {

    protected final String name;
    protected final String permission;

    /**
     * The reference to the plugin main class instance.
     */
    protected T plugin;

    private String description;
    private String usage;

    /**
     * Constructs a new subcommand with the given name and permission.
     * @param name the name of the subcommand.
     * @param permission the permission for the subcommand or null.
     */
    public SubCommand(@NotNull String name, @Nullable String permission) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.permission = permission;
    }

    /**
     * Registers the implementing subcommand.
     * @param plugin the instance to the plugin main class.
     * @return true if it has been registered, false otherwise.
     */
    public boolean register(@NotNull T plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
            return true;
        }

        return false;
    }

    /**
     * Unregisters the implementing subcommand.
     * @param plugin the instance to the plugin main class.
     * @return true if it has been unregistered, false otherwise.
     */
    public boolean unregister(@NotNull T plugin) {
        if (this.plugin == plugin) {
            this.plugin = null;
            return true;
        }

        return false;
    }

    /**
     * Returns the name of the subcommand.
     * @return the command name.
     */
    public final @NotNull String getName() {
        return this.name;
    }

    /**
     * Returns the description of the subcommand.
     * <p>
     * If the set description matches a key in the {@link de.g4memas0n.core.bukkit.util.I18n translation class}, it will be translated. Otherwise, it
     * will be returned immediately.
     * @return the command description or null.
     */
    public @Nullable String getDescription() {
        if (this.description != null) {
            return I18n.has(this.description) ? I18n.tl(this.description) : this.description;
        }

        return null;
    }

    /**
     * Checks whether a description was set for the subcommand.
     * @return true if a description was set, false otherwise.
     */
    public boolean hasDescription() {
        return this.description != null;
    }

    /**
     * Sets the description for the subcommand.
     * @param description the new description or null.
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Returns the usage of the subcommand.
     * <p>
     * If the set usage matches a key in the {@link I18n translation class}, it will be translated. Otherwise, it
     * will be returned immediately.
     * @return the command usage or null.
     */
    public @Nullable String getUsage() {
        if (this.usage != null) {
            return I18n.has(this.usage) ? I18n.tl(this.usage) : this.usage;
        }

        return null;
    }

    /**
     * Checks whether a usage was set for the subcommand.
     * @return true if a usage was set, false otherwise.
     */
    public boolean hasUsage() {
        return this.usage != null;
    }

    /**
     * Sets the usage for the subcommand.
     * @param usage the new usage or null.
     */
    public void setUsage(@Nullable String usage) {
        this.usage = usage;
    }

    /**
     * Executes this command for the given {@code sender} with the given {@code arguments}, returning its success.
     * <p>
     * Note:<br>
     * This method gets only called if the command source ({@code sender}) is permitted to perform this command.
     * This means that the implementation of this method is not required to check the permission for the given
     * {@code sender}.<br>
     * If the implementation of this method returns {@code false}, the description and usage of this command
     * will be sent to the command source ({@code sender}).
     *
     * @param sender the source who executed the command.
     * @param alias the alias that was used for the command.
     * @param arguments the passed arguments of the sender.
     * @return {@code true} if the execution was successful and valid.
     */
    public abstract boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments);

    /**
     * Requests a list of possible completions for the last element in the given {@code arguments} if it gets executed
     * by the given {@code sender}.
     * <p>
     * Note:<br>
     * This method gets only called if the command source ({@code sender}) is permitted to perform this command.
     * This means that the implementation of this method is not required to check the permission for the given
     * {@code sender}.
     *
     * @param sender the source who tab-completed the command.
     * @param alias the alias that was used for the command.
     * @param arguments the passed arguments of the sender, including the final partial argument to be completed.
     * @return a list of possible completions for the final arguments.
     */
    public abstract @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments);

    @Override
    public @NotNull String toString() {
        return getClass().getSimpleName() + "{name='" + this.name + "', permission='" + this.permission + "}";
    }

    @Override
    public boolean equals(@Nullable Object object) {
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

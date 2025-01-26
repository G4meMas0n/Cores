package de.g4memas0n.core.bukkit.command;

import de.g4memas0n.core.bukkit.util.I18n;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * An abstract subcommand class to extend.
 *
 * @param <P> the main class of the plugin
 */
@SuppressWarnings("unused")
public abstract class SubCommand<P extends JavaPlugin> {

    private static final Map<String, SubCommand<?>> registeredCommands = new HashMap<>();

    private String name;
    private String permission;
    private String description;
    private String usage;

    /**
     * The reference to the plugin main class instance.
     */
    protected P plugin;

    /**
     * Constructs a new subcommand with the given name and permission.
     *
     * @param name the name of the subcommand.
     * @param permission the permission for the subcommand or null.
     */
    public SubCommand(@NotNull String name, @Nullable String permission) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.permission = permission;
    }

    /**
     * Checks whether the command is registered.
     *
     * @return true if the command is currently registered, false otherwise
     */
    public boolean isRegistered() {
        return registeredCommands.containsKey(name) && plugin != null;
    }

    /**
     * Registers the implementing command.
     *
     * @param plugin the plugin instance
     * @return true if the registration was successful, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean register(@NotNull P plugin) {
        if (!isRegistered()) {
            registeredCommands.put(name, this);
            this.plugin = plugin;
            return true;
        }
        return false;
    }

    /**
     * Unregisters the implementing command.
     *
     * @param plugin the plugin instance
     * @return true if the unregistration was successful, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean unregister(@NotNull P plugin) {
        if (isRegistered()) {
            registeredCommands.remove(name);
            this.plugin = null;
            return true;
        }
        return false;
    }

    /**
     * Returns the name of the command.
     *
     * @return the command name
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * Sets the name of the command.
     * <p>
     * May only be used before registering the command. Will return true if the new name is set, and false if the
     * command has already been registered.
     *
     * @param name the new command name
     * @return true if the name change happened, false if the command was already registered
     */
    public boolean setName(@NotNull String name) {
        if (!isRegistered()) {
            this.name = name;
            return true;
        }
        return false;
    }

    /**
     * Returns the permission required to be able to perform the command.
     *
     * @return the command permission or null if none
     */
    public @Nullable String getPermission() {
        return permission;
    }

    /**
     * Sets the permission required to be able to perform the command.
     *
     * @param permission the new permission or null
     */
    public void setPermission(@Nullable String permission) {
        this.permission = permission;
    }

    /**
     * Returns the description of the command.
     * <p>
     * If the description matches a key in the {@link I18n translation class}, it will be translated. Otherwise, it
     * will be returned immediately.
     *
     * @return the command description or null
     */
    public @Nullable String getDescription() {
        if (description != null) {
            return I18n.has(description) ? I18n.tl(description) : description;
        }
        return null;
    }

    /**
     * Sets the description for the command.
     *
     * @param description the new description or null
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    /**
     * Returns the usage of the command.
     * <p>
     * If the usage matches a key in the {@link I18n translation class}, it will be translated. Otherwise, it will be
     * returned immediately.
     *
     * @return the command usage or null
     */
    public @Nullable String getUsage() {
        if (usage != null) {
            return I18n.has(usage) ? I18n.tl(usage) : usage;
        }
        return null;
    }

    /**
     * Sets the usage for the command.
     *
     * @param usage the new usage or null
     */
    public void setUsage(@Nullable String usage) {
        this.usage = usage;
    }

    /**
     * Executes the command for the given {@code sender} with the given {@code arguments}, returning its success.
     * <p>
     * Note:<br>
     * This method will only be called if the {@link CommandSender} is permitted to perform this command.
     * The implementation is therefore not required to test the permission for the given {@code sender}.<br>
     * If the implementation of this method returns {@code false}, the description and usage of this command
     * will be sent to the {@code sender}.
     *
     * @param sender the source who executed the command
     * @param alias the alias used for the command
     * @param arguments the passed arguments to the command
     * @return true if the execution was successful, false otherwise
     */
    public abstract boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments);

    /**
     * Requests a list of tab-completions for the given command {@code arguments} if it gets executed by the given
     * {@code sender}.
     * <p>
     * Note:<br>
     * This method will only be called if the {@link CommandSender} is permitted to perform this command.
     * The implementation is therefore not required to test the permission for the given {@code sender}.
     *
     * @param sender the source who tab-completed the command
     * @param alias the alias used for the command
     * @param arguments the passed arguments to the command, including the last partial argument to be completed
     * @return a list of tab-completions for the given arguments
     */
    public abstract @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] arguments);

    /**
     * Tests whether the given {@link CommandSender} can perform the command.
     * <p>
     * No message is sent to the given sender.
     *
     * @param sender the sender to test
     * @return true if the sender can use it, false otherwise
     */
    public boolean testPermission(@NotNull CommandSender sender) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        for (String perm : permission.split(";")) {
            if (sender.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull String toString() {
        return getClass().getSimpleName() + "{name='" + name + '\''  + ", permission='" + permission + '\'' + '}';
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }

        if (!(object instanceof SubCommand<?> other)) {
            return false;
        }

        return name.equals(other.name) && Objects.equals(permission, other.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, permission);
    }
}

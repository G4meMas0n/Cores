package de.g4memas0n.core.bukkit.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * An interface indicating a targeted command.
 * @param <T> the class of the target object.
 */
@SuppressWarnings("unused")
public interface TargetCommand<T> {

    /**
     * Executes the command for the given {@code sender} with the given {@code target} and {@code arguments},
     * returning its success.
     * <p>
     * Note:<br>
     * This method gets only called if the {@link CommandSender} is permitted to perform this command.
     * The implementation is therefore not required to test the permission for the given {@code sender}.<br>
     * If the implementation of this method returns {@code false}, the description and usage of this command
     * will be sent to the {@code sender}.
     *
     * @param sender the source who executed the command
     * @param target the target object for the command
     * @param alias the alias used for the command
     * @param arguments the passed arguments to the command
     * @return true if the execution was successful, false otherwise
     */
    boolean execute(@NotNull CommandSender sender, @NotNull T target, @NotNull String alias, @NotNull String[] arguments);

    /**
     * Requests a list of tab-completions for the given {@code target} and the given command {@code arguments} if it
     * gets executed by the given {@code sender}.
     * <p>
     * Note:<br>
     * This method gets only called if the {@link CommandSender} is permitted to perform this command.
     * The implementation is therefore not required to test the permission for the given {@code sender}.
     *
     * @param sender the source who tab-completed the command
     * @param target the target object for the command
     * @param alias the alias used for the command
     * @param arguments the passed arguments to the command, including the last partial argument to be completed.
     * @return a list of tab-completions for the given arguments
     */
    @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull T target, @NotNull String alias, @NotNull String[] arguments);
}

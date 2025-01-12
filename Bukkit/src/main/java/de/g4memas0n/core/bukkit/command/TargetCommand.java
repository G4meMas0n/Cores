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
     * Executes this command for the given {@code sender} with the given {@code target} and {@code arguments},
     * returning its success.
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
     * @param target the target object for the command.
     * @param arguments the passed arguments of the sender.
     * @return {@code true} if the execution was successful and valid.
     */
    boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull T target, @NotNull String[] arguments);

    /**
     * Requests a list of possible completions for the given {@code target} and the last element in the given
     * {@code arguments} if it gets executed by the given {@code sender}.
     * <p>
     * Note:<br>
     * This method gets only called if the command source ({@code sender}) is permitted to perform this command.
     * This means that the implementation of this method is not required to check the permission for the given
     * {@code sender}.
     *
     * @param sender the source who tab-completed the command.
     * @param alias the alias that was used for the command.
     * @param target the target object for the command.
     * @param arguments the passed arguments of the sender, including the final partial argument to be completed.
     * @return a list of possible completions for the final arguments.
     */
    @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull T target, @NotNull String[] arguments);
}
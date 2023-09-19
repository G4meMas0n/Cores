package com.github.g4memas0n.cores.database;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown to indicate that a database error occurred.
 *
 * @since 1.0.0
 */
public class DatabaseException extends Exception {

    /**
     * Constructs an instance of DatabaseException with the specified message.
     * @param message the detail message of the exception.
     */
    public DatabaseException(@NotNull final String message) {
        super(message);
    }

    /**
     * Constructs an instance of EconomyException with the specified message and cause.
     * @param message the detail message of the exception.
     * @param cause the cause of the exception
     */
    public DatabaseException(@NotNull final String message, @NotNull final Throwable cause) {
        super(message, cause);
    }
}

package com.github.g4memas0n.cores.database;

import org.jetbrains.annotations.NotNull;

/**
 * An exception that provides information on any database error that occurs.<br>
 *
 * @since 1.0.0
 */
public class DatabaseException extends Exception {

    /**
     * Constructs a database exception with the given {@code reason}
     * @param reason a description of the exception.
     */
    public DatabaseException(@NotNull final String reason) {
        super(reason);
    }

    /**
     * Constructs a database exception with the given {@code reason} and {@code cause}.
     * @param reason a description of the exception.
     * @param cause the underlying reason for this {@code DatabaseException}.
     */
    public DatabaseException(@NotNull final String reason, @NotNull final Throwable cause) {
        super(reason, cause);
    }
}

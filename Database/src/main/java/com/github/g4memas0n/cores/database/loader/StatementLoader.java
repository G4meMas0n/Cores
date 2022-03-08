package com.github.g4memas0n.cores.database.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StatementLoader {

    private final Map<String, String> cache;
    protected String type;
    protected int version;

    protected StatementLoader() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Loads the statements file specified by the given input stream and tries to parse it according to the actual
     * statement loader.
     * @param file the {@link InputStream} to read the statements file.
     * @throws IOException Thrown when the specified input stream cannot be parsed.
     */
    public abstract void load(@NotNull final InputStream file) throws IOException;

    /**
     * Loads a statement from the parsed statements file with the specified {@code identifier}.
     * @param identifier the {@code identifier} that identifies the statement to get.
     * @return the loaded statement with the specified {@code identifier} or null if the statement could not be found.
     */
    protected abstract @Nullable String load(@NotNull final String identifier);

    /**
     * Gets the statement with the specified {@code identifier} or loads it from the actual statement loader, if it
     * has not been already loaded.
     * @param identifier the {@code identifier} that identifies the statement to get.
     * @return the statement with the specified {@code identifier}
     * @throws MissingResourceException Thrown when no Statement with the specified {@code identifier} could be found.
     */
    public final @NotNull String get(@NotNull final String identifier) throws MissingResourceException {
        String statement = this.cache.get(identifier);

        if (statement == null) {
            statement = this.load(identifier);

            if (statement == null) {
                throw new MissingResourceException("", this.getClass().getSimpleName(), identifier);
            }

            this.cache.put(identifier, statement);
        }

        return statement;
    }
}

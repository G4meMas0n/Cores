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

    public abstract void load(@NotNull final InputStream file) throws IOException;

    protected abstract @Nullable String load(@NotNull final String identifier);

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

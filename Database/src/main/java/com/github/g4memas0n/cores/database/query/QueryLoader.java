package com.github.g4memas0n.cores.database.query;

import com.github.g4memas0n.cores.database.driver.Driver;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * A loader for files, that contains mappings from keys to actual sql queries.<br>
 * The loading and parsing of the file and of the queries will be handled by the implementing class.
 * This base class provides a cache for sql queries, so that the queries get not loaded multiple times from the file.
 *
 * @since 1.0.0
 */
public abstract class QueryLoader {

    private Map<String, String> cache;

    /**
     * The parent query loader of this loader.
     */
    protected QueryLoader parent;

    /**
     * Protected default constructor for the implementing query loader classes.
     */
    protected QueryLoader() { }

    /**
     * Loads a query for the given key from this query loader.
     * Returns null if this query loader does not contain a query for the given key.
     *
     * @param key the key for the desired query.
     * @return the query for the given key, or null.
     */
    protected abstract @Nullable String load(@NotNull final String key);

    /**
     * Gets a query for the given key from this query loader or one of its parents.
     *
     * @param key the key for the desired query.
     * @return the query for the given key.
     * @throws MissingResourceException if no query for the given key can be found.
     */
    public final @NotNull String get(@NotNull final String key) throws MissingResourceException {
        String query = null;

        if (this.cache != null) {
            query = this.cache.get(key);

            if (query != null) {
                return query;
            }
        } else {
            this.cache = Maps.newConcurrentMap();
        }

        QueryLoader loader = this;

        while (loader != null) {
            query = loader.load(key);

            if (query != null) {
                this.cache.put(key, query);
                return query;
            }

            loader = loader.parent;
        }

        throw new MissingResourceException("missing query for key", getClass().getSimpleName(), key);
    }

    /**
     * Gets a query loader using the specified base name.
     * This method loads the property queries file that is visible to the class loader of this class.
     *
     * @param base the base name of the queries file.
     * @return a query loader for the given base name.
     * @throws IllegalArgumentException if the file is not visible to the class loader or contains a malformed Unicode
     *                                  escape sequence.
     * @throws IOException if an I/O error occurs.
     * @see #getLoader(String, Driver)
     */
    public static @NotNull QueryLoader getLoader(@NotNull final String base) throws IOException {
        final String path = base.toLowerCase(Locale.ROOT) + ".properties";

        try (InputStream stream = QueryLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("missing property file");
            }

            return new PropertyQueryLoader(stream);
        }
    }

    /**
     * Gets a query loader using the specified base name and driver.
     * This method loads the property queries file that is visible to the class loader of this class.
     *
     * @param base the base name of the queries file.
     * @param driver the driver for which a queries file is desired.
     * @return a query loader for the given base name.
     * @throws IllegalArgumentException if the file is not visible to the class loader or contains a malformed Unicode
     *                                  escape sequence.
     * @throws IOException if an I/O error occurs.
     * @see #getLoader(String)
     */
    public static @NotNull QueryLoader getLoader(@NotNull final String base, @NotNull final Driver driver) throws IOException {
        StringBuilder builder = new StringBuilder(base);
        QueryLoader temp, loader = null;
        String name;

        do {
            name = builder.toString();

            try {
                temp = getLoader(name.toLowerCase(Locale.ROOT));
                temp.parent = loader;
                loader = temp;
            } catch (IllegalArgumentException ignored) {

            }

            if (name.contains(driver.getType())) {
                if (driver.getVersion() != null && !name.contains(driver.getVersion())) {
                    builder.append("-").append(driver.getVersion());
                } else {
                    builder.setLength(0);
                }
            } else {
                builder.append("_").append(driver.getType());
            }
        } while (builder.length() > 0);

        if (loader == null) {
            throw new IllegalArgumentException("missing or unsupported files");
        }

        return loader;
    }
}

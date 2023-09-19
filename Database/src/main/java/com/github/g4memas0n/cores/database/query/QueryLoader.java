package com.github.g4memas0n.cores.database.query;

import com.github.g4memas0n.cores.database.driver.Driver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A loader for files, that contains mappings from keys to actual sql queries.<br>
 * The loading and parsing of the file and of the queries will be handled by the implementing class.
 * This base class provides a cache for sql queries, so that the queries get not loaded multiple times from the file.
 *
 * @since 1.0.0
 */
public abstract class QueryLoader {

    private final Map<String, String> cache;

    /**
     * The parent query loader of this loader.
     */
    protected QueryLoader parent;

    /**
     * Protected default constructor for the implementing query loader classes.
     */
    protected QueryLoader() {
        this.cache = new ConcurrentHashMap<>();
    }

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
        String query = this.cache.get(key);

        if (query == null) {
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

        return query;
    }

    /**
     * Gets a query loader using the specified loader class and the basename.
     * This method loads the queries file that is visible to the class loader of the given class.
     *
     * @param clazz the implementing loader class to use.
     * @param basename the basename of the queries file.
     * @return a query loader for the given basename.
     * @throws IllegalArgumentException if the file is not visible or contains a malformed Unicode escape sequence.
     * @throws IOException if an I/O error occurs.
     * @see #getLoader(Class, String, Driver)
     */
    public static @NotNull QueryLoader getLoader(@NotNull final Class<? extends QueryLoader> clazz,
                                                 @NotNull final String basename) throws IOException {
        try {
            return clazz.getConstructor(String.class).newInstance(basename.toLowerCase(Locale.ROOT));
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            }

            throw (IllegalArgumentException) ex.getCause();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("illegal constructor", ex);
        }
    }

    /**
     * Gets a query loader using the specified base name and driver.
     * This method loads the queries file that is visible to the class loader of the given class.
     *
     * @param clazz the implementing loader class to use.
     * @param basename the base name of the queries file, including the extension.
     * @param driver the driver for which a queries file is desired.
     * @return a query loader for the given base name.
     * @throws IllegalArgumentException if the file is not visible or contains a malformed Unicode escape sequence.
     * @throws IOException if an I/O error occurs.
     * @see #getLoader(Class, String)
     */
    public static @NotNull QueryLoader getLoader(@NotNull final Class<? extends QueryLoader> clazz,
                                                 @NotNull final String basename,
                                                 @NotNull final Driver driver) throws IOException {
        StringBuilder builder = new StringBuilder(basename);
        QueryLoader temp, loader = null;
        String name;

        do {
            name = builder.toString();

            try {
                temp = getLoader(clazz, name.toLowerCase(Locale.ROOT));
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

package com.github.g4memas0n.cores.database.query;

import com.github.g4memas0n.cores.database.driver.Driver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A loader for files, that contains mappings from identifiers to actual sql queries.<br>
 * The loading of the file and of the queries will be handled by the implementing class. This class provides a cache
 * for sql queries, so that the queries get not loaded multiple times from the file.
 *
 * @since 1.0.0
 */
public abstract class QueryLoader {

    private Map<String, String> cache;

    /**
     * The reference to a parent query loader for this query file.
     */
    protected QueryLoader parent;

    /**
     * The reference to the location of the file that is currently loaded.
     */
    protected String path;

    /**
     * Protected default constructor for the implementing query loader classes.
     */
    protected QueryLoader() { }

    /**
     * Loads the file located at the given {@code path} from the {@link ClassLoader} and parses it as queries file
     * according to the implementing query loader.
     *
     * @param path the path of the file containing the queries.
     * @throws IllegalArgumentException if the file could not be found or contains illegal elements.
     * @throws IOException if the file could not be read or parsed.
     * @see #load(InputStream)
     */
    public abstract void load(@NotNull final String path) throws IOException;

    /**
     * Reads the queries file from the given {@code stream} and parses it according to the implementing loader.
     *
     * @param stream the input stream to read from.
     * @throws IllegalArgumentException if the file contains illegal elements.
     * @throws IOException if the stream could not be read or parsed.
     * @see #load(String)
     */
    public abstract void load(@NotNull final InputStream stream) throws IOException;

    /**
     * Loads the query that is located at the given query {@code identifier} from the currently loaded queries file.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the loaded query for the given {@code identifier} or null if it not exists.
     */
    protected abstract @Nullable String loadQuery(@NotNull final String identifier);

    /**
     * Gets the query that for the given query {@code identifier} from the cache or loads it from the implementing
     * query loader, if it has not been already cached.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the query for the given {@code identifier}.
     * @throws MissingResourceException if no query entry with the given {@code identifier} exists.
     */
    public final @NotNull String getQuery(@NotNull final String identifier) throws MissingResourceException {
        String query;

        if (this.cache != null) {
            query = this.cache.get(identifier);

            if (query != null) {
                return query;
            }
        } else {
            this.cache = new ConcurrentHashMap<>();
        }

        query = loadQuery(identifier);

        if (query == null) {
            query = this.parent != null ? this.parent.loadQuery(identifier) : null;

            if (query == null) {
                throw new MissingResourceException("missing key in queries file located at " + this.path,
                        getClass().getSimpleName(), identifier);
            }
        }

        this.cache.put(identifier, query);
        return query;
    }

    /**
     * Creates a new query loader for the queries file with the given {@code name} from the {@link ClassLoader} and
     * loads and parses the file.
     * <p><i>Currently only property and json files are supported as queries files.</i></p>
     *
     * @param name the name of the queries file without an extension.
     * @return the query loader for the queries file.
     * @throws IllegalArgumentException if no file could be found or the file contains illegal elements.
     * @throws IOException if the file could not be read or parsed.
     * @see #getLoader(String, Driver)
     */
    public static @NotNull QueryLoader getLoader(@NotNull final String name) throws IOException {
        final ClassLoader clazz = QueryLoader.class.getClassLoader();
        QueryLoader loader = null;
        String path;

        try (InputStream stream = clazz.getResourceAsStream(path = name + ".properties")) {
            if (stream != null) {
                loader = new PropertyQueryLoader();
                loader.load(path);
                loader.path = path;
            }
        } catch (IllegalArgumentException ignored) {

        }

        try (InputStream stream = clazz.getResourceAsStream(path = name + ".json")) {
            if (stream != null) {
                loader = new JsonQueryLoader();
                loader.load(path);
                loader.path = path;
            }
        } catch (IllegalArgumentException ignored) {

        }

        if (loader == null) {
            throw new IllegalArgumentException("missing or unsupported file");
        }

        return loader;
    }

    /**
     * Creates a new query loader for the queries file for the given {@code driver} with the given {@code basename} from
     * the {@link ClassLoader} and loads and parses the file.
     * <p><i>Currently only json and property files are supported as queries files.</i></p>
     *
     * @param base the basename of the queries file without an extension.
     * @param driver the driver to load the queries file for.
     * @return the query loader for the queries file, including parent loaders.
     * @throws IllegalArgumentException if no file could be found or the file contains illegal elements.
     * @throws IOException if the file could not be read or parsed.
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

package com.github.g4memas0n.cores.database.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
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

    private final Map<String, String> cache;

    /**
     * The reference to the location of the file that is currently loaded.<br>
     * Must be set by the implementing class after the successful load of a file.
     */
    protected String path;

    /**
     * Protected default constructor for the implementing query loader classes.
     */
    protected QueryLoader() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Loads the file located at the given {@code path} from the {@link ClassLoader} and parses it as queries file
     * according to the implementing query loader.
     *
     * @param path the path of the file containing the queries.
     * @throws IllegalArgumentException if the file located at the given {@code path} could not be found.
     * @throws IOException if the file could not be read or parsed.
     */
    public abstract void load(@NotNull final String path) throws IOException;

    /**
     * Loads the query from the loaded queries file that matches with the given {@code identifier}.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the loaded query that matches the given {@code identifier} or null if no one matches.
     */
    public abstract @Nullable String loadQuery(@NotNull final String identifier);

    /**
     * Gets the query that matches with the given {@code identifier} from the cache or loads it from the implementing
     * query loader, if it has not been already cached.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the query that matches the given {@code identifier}.
     * @throws MissingResourceException if no query that matches the given {@code identifier} could be found.
     */
    public final @NotNull String getQuery(@NotNull final String identifier) throws MissingResourceException {
        String query = this.cache.get(identifier);

        if (query == null) {
            query = this.loadQuery(identifier);

            if (query == null) {
                throw new MissingResourceException("Missing key in queries file located at " + this.path,
                        this.getClass().getSimpleName(), identifier);
            }

            this.cache.put(identifier, query);
        }

        return query;
    }

    /**
     * Creates a new query loader and loads the file located at the given {@code path} from the {@link ClassLoader}.
     * This method will check the file extension and will create a query loader according to it.
     *
     * @param path the path of the file containing the queries.
     * @return the newly created query loader that has already loaded the file.
     * @throws IllegalArgumentException if the file located at the given {@code path} could not be found or could not
     * be loaded.
     */
    public static @NotNull QueryLoader loadFile(@NotNull final String path) {
        final String lowered = path.toLowerCase(Locale.ROOT);
        QueryLoader loader = null;

        if (lowered.endsWith(".json")) {
            loader = new JsonQueryLoader();
        } else if (lowered.endsWith(".xml")) {
            loader = new XmlQueryLoader();
        }

        if (loader != null) {
            try {
                loader.load(path);

                return loader;
            } catch (IOException ex) {
                throw new IllegalArgumentException("Queries file at given path cannot be parsed", ex);
            }
        } else {
            throw new IllegalArgumentException("Missing or unsupported queries file extension");
        }
    }
}

package com.github.g4memas0n.cores.database.query;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
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
     * Loads the batch file path that is located at the given batch {@code identifier} from the currently loaded
     * queries file.
     *
     * @param identifier the string that uniquely identifies a batch file path.
     * @return the loaded batch file path for the given {@code identifier} or null if it not exists.
     */
    protected abstract @Nullable String loadBatch(@NotNull final String identifier);

    /**
     * Loads the query that is located at the given query {@code identifier} from the currently loaded queries file.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the loaded query for the given {@code identifier} or null if it not exists.
     */
    protected abstract @Nullable String loadQuery(@NotNull final String identifier);

    /**
     * Gets the batch file path for the given batch {@code identifier} from the implementing query loader.
     *
      * @param identifier the string that uniquely identifies a batch file path.
     * @return the batch file path for the given {@code identifier}.
     * @throws MissingResourceException if no batch file path entry with the given {@code identifier} exists.
     */
    public final @NotNull String getBatch(@NotNull final String identifier) throws MissingResourceException {
        final String path = loadBatch(identifier);

        if (path == null) {
            throw new MissingResourceException("missing batch key in queries file located at " + this.path,
                    getClass().getSimpleName(), identifier);
        }

        return path;
    }

    /**
     * Gets the query that for the given query {@code identifier} from the cache or loads it from the implementing
     * query loader, if it has not been already cached.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the query for the given {@code identifier}.
     * @throws MissingResourceException if no query entry with the given {@code identifier} exists.
     */
    public final @NotNull String getQuery(@NotNull final String identifier) throws MissingResourceException {
        String query = this.cache.get(identifier);

        if (query == null) {
            query = this.loadQuery(identifier);

            if (query == null) {
                throw new MissingResourceException("missing key in queries file located at " + this.path,
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
     * @throws IllegalArgumentException if the file could not be found or could not be loaded.
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
                throw new IllegalArgumentException("queries file at given path cannot be parsed", ex);
            }
        } else {
            throw new IllegalArgumentException("missing or unsupported queries file extension");
        }
    }

    /**
     * Loads the sql batch file located at the given {@code path} from the {@link ClassLoader} and adds the individual
     * statements directly to the given {@link Statement}.
     *
     * @param path the path of the sql batch file.
     * @param statement the statement to add the loaded batch statements to.
     * @throws IOException if the file could not be read or parsed.
     * @throws SQLException if a sql exception occurs while adding batches to the statement.
     * @throws IllegalArgumentException if the batch file could not be found.
     */
    public static void loadBatch(@NotNull final String path, @NotNull final Statement statement) throws IOException, SQLException {
        final InputStream stream = QueryLoader.class.getClassLoader().getResourceAsStream(path);
        Preconditions.checkArgument(stream != null, "missing file at " + path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder query = new StringBuilder();
            String line;
            int index;

            while ((line = reader.readLine()) != null) {
                // Check for comments in the current line
                if ((index = line.indexOf("#")) >= 0) {
                    if (index == 0) {
                        continue;
                    }

                    line = line.substring(0, index).stripTrailing();
                }

                // Check for ending statements
                while ((index = line.indexOf(";")) >= 0) {
                    query.append(line.substring(0, index).stripLeading());
                    statement.addBatch(query.toString());
                    query.setLength(0);

                    line = line.substring(index + 1);
                }

                query.append(line.stripLeading());
            }
        } catch (RuntimeException ex) {
            throw new IOException("unable to parse batch file at " + path, ex);
        }
    }
}

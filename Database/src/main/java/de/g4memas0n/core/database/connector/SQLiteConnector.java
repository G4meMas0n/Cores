package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.Properties;

/**
 * A connector class for connecting to a flat-file sqlite database.
 */
public class SQLiteConnector extends FlatFileConnector {

    private final Path path;

    /**
     * The public default constructor for a sqlite connector.
     */
    public SQLiteConnector() {
        this(null);
    }

    /**
     * The public constructor for a sqlite connector with a given database path.
     * @param path the path to the database file.
     */
    public SQLiteConnector(@Nullable Path path) {
        this.path = path;
    }

    @Override
    public void configure(@NotNull Properties properties) {
        properties.setProperty("encoding", "UTF-8");

        super.configure(properties);
    }

    @Override
    public void initialize(@Nullable Properties properties) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("driver not available", ex);
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:sqlite://");

        if (properties != null && properties.containsKey("path")) {
            jdbcUrl.append(properties.getProperty("path"));
        } else {
            if (this.path == null) {
                throw new IllegalArgumentException("missing path property");
            }

            jdbcUrl.append(this.path);
        }

        initialize(jdbcUrl.toString(), properties);
    }
}

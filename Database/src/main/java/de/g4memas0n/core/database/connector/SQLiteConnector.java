package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.Properties;

/**
 * A sqlite database connector.
 * @see FlatFileConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class SQLiteConnector extends FlatFileConnector {

    /**
     * Constructs a sqlite database connector.
     * @param path the path to the database file.
     * @see FlatFileConnector
     */
    public SQLiteConnector(@NotNull Path path) {
        super(path);
    }

    @Override
    public @NotNull String getVendorName() {
        return "SQLite";
    }

    @Override
    public void configure(@NotNull Properties properties) {
        // Set default encoding to UTF-8
        if (properties.getProperty("encoding") == null) {
            properties.setProperty("encoding", "UTF-8");
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            logger.warning("Could not find sqlite driver");
            throw new RuntimeException("driver not available", ex);
        }

        super.configure(properties);
    }

    @Override
    public @NotNull String createUrl(@NotNull Path path) {
        return "jdbc:sqlite:" + path;
    }
}

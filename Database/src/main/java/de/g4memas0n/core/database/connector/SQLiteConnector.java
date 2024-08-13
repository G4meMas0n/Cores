package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A sqlite database connector.
 * @see FlatFileConnector
 * @see IConnector
 */
public class SQLiteConnector extends FlatFileConnector {

    private Properties properties;

    /**
     * Constructs a sqlite database connector.
     * @param path the path to the database file.
     * @see FlatFileConnector
     */
    public SQLiteConnector(@NotNull Path path) {
        super(path);
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
            logger.warning("Failed to find sqlite driver");
            throw new RuntimeException(ex);
        }

        this.properties = properties;
    }

    @Override
    public @NotNull Connection createConnection(@NotNull Path path) throws SQLException {
        String jdbcUrl = properties.getProperty("jdbcUrl");
        if (jdbcUrl == null) {
            jdbcUrl = "jdbc:sqlite:" + path;
        }
        return DriverManager.getConnection(jdbcUrl, properties);
    }
}

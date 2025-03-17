package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * An abstract database connector for connecting to a file based database.
 * @see Connector
 */
@SuppressWarnings("unused")
public abstract class FlatFileConnector implements Connector {

    /**
     * Logger instance used by the implementing flat-file connectors.
     */
    public static Logger logger = Logger.getLogger(FlatFileConnector.class.getName());

    private final Path path;
    private Properties properties;
    private String url;

    /**
     * Constructs a file based connector.
     * @param path the path to the database file.
     */
    public FlatFileConnector(@NotNull Path path) {
        this.path = path;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public boolean isShutdown() {
        return this.properties == null;
    }

    @Override
    public void configure(@NotNull Properties properties) {
        this.properties = properties;
        this.url = createUrl(path);
    }

    @Override
    public void shutdown() {
        this.properties = null;
    }

    /**
     * Creates a new jdbc-url for the given database path.
     * @param path the path to the flat-file database.
     * @return the created jdbc-url.
     */
    public abstract @NotNull String createUrl(@NotNull Path path);

    @Override
    public synchronized @NotNull Connection getConnection() throws SQLException {
        if (isShutdown()) {
            throw new SQLException("Connector not configured or shut down");
        }
        return DriverManager.getConnection(url, properties);
    }

    @Override
    public boolean isWrapperFor(@NotNull Class<?> type) {
        return Connector.class.equals(type) || Connection.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(@NotNull Class<T> type) throws SQLException {
        if (Connector.class.equals(type)) {
            return (T) this;
        } else if (Connection.class.isAssignableFrom(type)) {
            return (T) getConnection();
        }
        throw new SQLException("Cannot unwrap to " + type);
    }
}

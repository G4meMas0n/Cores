package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * An abstract database connector for connecting to a file based database.
 * @see IConnector
 */
@SuppressWarnings("unused")
public abstract class FlatFileConnector implements IConnector {

    /**
     * Logger instance used by the implementing flat-file connectors.
     */
    public static Logger logger = Logger.getLogger(FlatFileConnector.class.getName());
    private Connection connection;
    private final Path path;

    /**
     * Constructs a file based connector.
     * @param path the path to the database file.
     */
    public FlatFileConnector(@NotNull Path path) {
        this.path = path;
    }

    @Override
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public void closeConnection(@NotNull Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException ignored) {}
    }

    /**
     * Attempts to establish a new connection to the database.
     * @param path the path to the database file.
     * @return a connection to the database.
     * @throws SQLException if a database access error occurs.
     */
    public abstract @NotNull Connection createConnection(@NotNull Path path) throws SQLException;

    @Override
    public @NotNull Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = createConnection(path);
        }
        return connection;
    }

    @Override
    public boolean isWrapperFor(@NotNull Class<?> type) {
        return IConnector.class.equals(type) || Connection.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(@NotNull Class<T> type) throws SQLException {
        if (IConnector.class.equals(type)) {
            return (T) this;
        } else if (Connection.class.isAssignableFrom(type)) {
            return (T) getConnection();
        }
        throw new SQLException("Cannot unwrap to " + type);
    }
}

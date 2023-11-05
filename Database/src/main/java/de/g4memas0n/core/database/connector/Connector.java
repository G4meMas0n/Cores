package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * an interface for a class that connects to a database.
 */
public interface Connector {

    /**
     * A logger to use for logging for the implementing connector classes
     */
    @NotNull Logger LOGGER = Logger.getLogger(Connector.class.getName());

    /**
     * Initializes the connector with the given connection settings.
     * @param properties the connection properties.
     */
    void initialize(@Nullable Properties properties);

    /**
     * Shutdowns the connector.
     */
    void shutdown();

    /**
     * Returns a connection session to a database.
     * @return a valid connection session.
     * @throws SQLException if it fails to get a valid connection.
     */
    @NotNull Connection getConnection() throws SQLException;

    /**
     * Returns a connection session to a database.
     * <p>
     * The transaction argument determines whether a transaction will be started on the fetched or opened connection.
     * @param transaction whether a transaction should be started.
     * @return a valid connection session.
     * @throws SQLException if it fails to get a valid connection.
     */
    default @NotNull Connection getConnection(boolean transaction) throws SQLException {
        Connection connection = getConnection();

        if (transaction) {
            try {
                connection.setAutoCommit(false);
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to begin transaction on database connection.", ex);
                throw ex;
            }
        }

        return connection;
    }
}

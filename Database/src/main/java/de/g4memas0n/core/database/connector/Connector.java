package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Wrapper;
import java.util.Properties;

/**
 * An interface for connecting to a database.
 */
@SuppressWarnings("unused")
public interface Connector extends Wrapper {

    /**
     * Gets the vendor name of the connector implementation.
     * @return the database vendor name.
     */
    @NotNull String getVendorName();

    /**
     * Gets whether this connector connects to a remote database server.
     * @return true, if it is a remote database connector.
     */
    boolean isRemote();

    /**
     * Configures the database connector with the given properties.
     * @param properties the driver or data source properties.
     */
    void configure(@NotNull Properties properties);

    /**
     * Shutdowns the database connector.
     */
    void shutdown();

    /**
     * Attempts to close the given connection to the database.
     * @param connection the connection to close.
     */
    default void closeConnection(@NotNull Connection connection) {
        try (Connection closeable = connection) {
            if (!closeable.getAutoCommit()) {
                closeable.rollback();
            }
        } catch (SQLException ignored) {}
    }

    /**
     * Attempts to establish a connection to the database.
     * @return a connection to the database.
     * @throws SQLException if a database access error occurs.
     */
    @NotNull Connection getConnection() throws SQLException;

}

package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * an interface for a class that connects to a database.
 */
public interface Connector {

    /**
     * Initializes the connector with the given connection settings.
     * @param settings the connection settings.
     */
    void initialize(@Nullable Properties settings);

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

}

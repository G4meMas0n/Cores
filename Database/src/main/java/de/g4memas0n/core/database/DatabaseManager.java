package de.g4memas0n.core.database;

import de.g4memas0n.core.database.connector.Connector;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager class for database connections.
 */
public class DatabaseManager {

    private static Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private final Connector connector;

    /**
     * Constructs a new manager with the given database connector.
     * @param connector the database connector to use.
     */
    public DatabaseManager(@NotNull Connector connector) {
        this.connector = connector;
    }

    /**
     * Constructs a new manager with the given database connector and logger.
     * @param connector the database connector to use.
     * @param logger the logger to log all messages to.
     */
    public DatabaseManager(@NotNull Connector connector, @NotNull Logger logger) {
        this(connector);
        DatabaseManager.logger = logger;
    }

    /*
     *
     */

    /**
     * Connects to a database by initializing the manager's connector with the given properties.
     * <p>
     * The properties argument can be used to pass arbitrary string tag/value pairs as connection arguments. Normally
     * this includes at least "user" and "password" properties.
     * </p>
     * @param settings a list of arbitrary string tag/value pairs.
     * @throws RuntimeException if it fails to connect.
     */
    public void connect(@Nullable Properties settings) {
        Preconditions.checkState(this.connector != null, "connector not available");

        try {
            this.connector.initialize(settings);
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Failed to connect to database.", ex);
            throw ex;
        }
    }

    /**
     * Disconnects from a database by shutting down the manager's connector.
     * Calling this method on an unconnected manager has no effect.
     */
    public void disconnect() {
        if (this.connector != null) {
            try {
                this.connector.shutdown();
            } catch (RuntimeException ex) {
                getLogger().log(Level.SEVERE, "Failed to disconnect from database.", ex);
            }
        }
    }

    /*
     *
     */

    /**
     * Fetches or opens a connection to the database by calling the appropriate method on the manager's connector.
     * @return a valid connection session to the database.
     * @throws SQLException if it fails to fetch a connection.
     */
    public final @NotNull Connection getConnection() throws SQLException {
        if (this.connector == null) {
            throw new SQLException("connector is null");
        }

        try {
            return this.connector.getConnection();
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to fetch connection to database", ex);
            throw ex;
        }
    }

    /**
     * Fetches or opens a connection to the database by calling the appropriate method on the manager's connector.
     * <p>
     * The transaction argument determines whether a transaction will be started on the fetched or opened connection.
     * </p>
     * @param transaction whether a transaction should be started.
     * @return a valid connection session to the database.
     * @throws SQLException if it fails to fetch a connection.
     */
    public final @NotNull Connection getConnection(boolean transaction) throws SQLException {
        Connection connection = getConnection();

        try {
            if (transaction) {
                connection.setAutoCommit(false);
            }

            return connection;
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to begin transaction on fetched connection to database", ex);
            throw ex;
        }
    }

    /*
     *
     */

    /**
     * Returns the logger for this class, which will also be used for all classes of this module.
     * @return the logger instance for this class.
     */
    public static @NotNull Logger getLogger() {
        return logger;
    }

    /**
     * Closes the given {@code statements} silently, hiding any SQLException that occur.
     * @param statements the statements to close.
     */
    public static void close(@NotNull Statement... statements) {
        for (Statement statement : statements) {
            try {
                statement.close();
            } catch (SQLException ignored) { }
        }
    }

    /**
     * Closes the given {@code connection} silently, hiding any SQLException that occur.
     * @param connection the connection to close.
     */
    public static void close(@NotNull Connection connection) {
        try (Connection closeable = connection) {
            if (!closeable.getAutoCommit()) {
                closeable.rollback();
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Failed to close database connection", ex);
        }
    }
}

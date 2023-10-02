package com.github.g4memas0n.cores.database;

import com.github.g4memas0n.cores.database.driver.Driver;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager for database connections and transactions.
 * This class must be extended by an implementing class to initialize the database after the connection setup.
 *
 * @since 1.0.0
 */
public abstract class DatabaseManager {

    /**
     * The reference to the logger for this class.
     */
    protected static Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private volatile HikariDataSource source;
    private volatile Driver driver;

    /**
     * Public constructor for extending this class.
     */
    public DatabaseManager() { }

    /**
     * Connects to a data source using the given driver.
     * If the source class returned by the given driver implements the {@link java.sql.Driver} interface, the driver
     * must specify a valid jdbc url.
     * @param driver the database driver to use for the data source.
     * @throws DatabaseException if the connection to the data source fails.
     * @see #connect(Driver, Properties)
     */
    public final void connect(@NotNull final Driver driver) throws DatabaseException {
        Preconditions.checkState(this.source == null, "connection already established");
        HikariConfig config = driver.getProperties() != null ? new HikariConfig(driver.getProperties()) : new HikariConfig();

        if (java.sql.Driver.class.isAssignableFrom(driver.getSource())) {
            if (driver.getJdbcUrl() == null) {
                throw new IllegalArgumentException("driver is missing jdbc-url");
            }

            try {
                config.setDriverClassName(driver.getSource().getName());
                config.setJdbcUrl(driver.getJdbcUrl());
            } catch (RuntimeException ex) {
                throw new DatabaseException("failed to load driver", ex);
            }
        } else {
            config.setDataSourceClassName(driver.getSource().getName());
        }

        config.setAutoCommit(true);
        connect(driver, config);
    }

    /**
     * Connects to a data source using the given driver and properties.
     * This method will check for all occurrences of the property keys in the driver's data source properties and
     * replaces them with the corresponding values.
     * @param driver the database driver to use for the data source.
     * @param properties the properties for the data source, which will be used as placeholders.
     * @throws DatabaseException if the connection to the data source fails.
     * @see #connect(Driver)
     */
    public final void connect(@NotNull final Driver driver, @NotNull final Properties properties) throws DatabaseException {
        Preconditions.checkState(this.source == null, "connection already established");
        HikariConfig config = new HikariConfig();

        if (java.sql.Driver.class.isAssignableFrom(driver.getSource())) {
            String jdbc = driver.getJdbcUrl();
            String placeholder;

            if (jdbc == null) {
                throw new IllegalArgumentException("driver is missing jdbc-url");
            }

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (jdbc.contains(placeholder = "{" + entry.getKey() + "}")) {
                    jdbc = jdbc.replace(placeholder, entry.getValue().toString());
                }
            }

            try {
                config.setDriverClassName(driver.getSource().getName());
                config.setJdbcUrl(jdbc);
            } catch (RuntimeException ex) {
                throw new DatabaseException("failed to load driver", ex);
            }
        } else {
            if (driver.getProperties() != null) {
                String placeholder, value;

                for (Map.Entry<Object, Object> property : driver.getProperties().entrySet()) {
                    value = property.getValue().toString();

                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        if (value.contains(placeholder = "{" + entry.getKey() + "}")) {
                            value = value.replace(placeholder, entry.getValue().toString());
                        }
                    }

                    config.addDataSourceProperty(property.getKey().toString(), value);
                }
            }

            config.setDataSourceClassName(driver.getSource().getName());
        }

        config.setUsername(properties.getProperty("username"));
        config.setPassword(properties.getProperty("password"));
        config.setAutoCommit(true);
        connect(driver, config);
    }

    private void connect(@NotNull final Driver driver, @NotNull final HikariConfig config) throws DatabaseException {
        HikariDataSource source;

        try {
            getLogger().info("Setting up data source...");
            source = new HikariDataSource(config);
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Failed to setup data source", ex);
            throw new DatabaseException("data source setup failed", ex);
        }

        getLogger().info("Trying to establish connection...");
        try (Connection connection = source.getConnection()) {
            this.driver = driver;

            getLogger().info("Successfully established connection. Initializing database...");
            initialize(connection);
            getLogger().info("Successfully initialized database.");
        } catch (SQLException ex) {
            this.driver = null;

            source.close();
            getLogger().log(Level.SEVERE, "Failed to establish connection", ex);
            throw new DatabaseException("connection or initialization failed");
        }

        this.source = source;
    }

    /**
     * Disconnects from and shutdowns the connected data source.
     * Calling this method has no effect if no connection is established.
     */
    public final void disconnect() {
        if (this.source != null && !this.source.isClosed()) {
            getLogger().info("Shutting down data source...");
            this.source.close();
            getLogger().info("Successfully shut down data source.");
        }

        this.source = null;
        this.driver = null;
    }

    /**
     * Returns whether a connection to a data source is established.
     * @return true if a connection is established, false otherwise.
     */
    public final boolean isConnected() {
        return this.source != null && this.source.isRunning();
    }

    /**
     * Returns whether a connection to a data source is established.
     * @return true if no connection is established, false otherwise.
     */
    public final boolean isDisconnected() {
        return this.source == null || this.source.isClosed();
    }

    /*
     *
     */

    /**
     * Returns the data source driver that was used to connect to the data source.
     * @return the data source driver.
     */
    public final @NotNull Driver driver() {
        Preconditions.checkState(this.driver != null, "driver not available");
        return this.driver;
    }

    /**
     * Fetches or opens a connection to the database by calling the appropriate method on connected data source.
     * @return a valid connection session to the database, or null.
     * @throws IllegalStateException if no connection to a database is established.
     * @throws SQLException if it fails to fetch a connection.
     */
    public final @NotNull Connection fetch() throws SQLException {
        Preconditions.checkState(this.source != null, "no established database connection");

        try {
            return this.source.getConnection();
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to fetch connection to database", ex);
            throw ex;
        }
    }

    /**
     * Fetches or opens a connection to the database by calling the appropriate method on connected data source and
     * starts a new transaction on it.
     * @return a valid connection session to the database, or null.
     * @throws IllegalStateException if no connection to a database is established.
     * @throws SQLException if it fails to fetch a connection.
     */
    public final @NotNull Connection transaction() throws SQLException {
        Connection connection = fetch();

        try {
            connection.setAutoCommit(false);
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
     * Initializes the connected database.
     * This method will automatically be called after a connection to the data source has been established.
     * @param connection an active connection from the connected data source.
     * @throws SQLException if the initialization fails.
     */
    protected abstract void initialize(@NotNull final Connection connection) throws SQLException;

    /*
     *
     */

    /**
     * Returns the logger for this class, which will also be used from the driver and query loaders.
     * @return the logger instance for this class.
     * @throws IllegalStateException if the logger for this class is not set.
     */
    public static @NotNull Logger getLogger() {
        Preconditions.checkState(logger != null, "logger not available");
        return logger;
    }

    /**
     * Closes the given {@code statements} silently, hiding any SQLException that occur.
     * @param statements the statements to close.
     */
    public static void close(@NotNull final Statement... statements) {
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
    public static void close(@NotNull final Connection connection) {
        try (Connection closeable = connection) {
            if (!closeable.getAutoCommit()) {
                closeable.rollback();
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Failed to close database connection", ex);
        }
    }
}

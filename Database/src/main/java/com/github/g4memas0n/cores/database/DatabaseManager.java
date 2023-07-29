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
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager for database connections and transactions.<br>
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
    private volatile HikariConfig config;
    private volatile Driver driver;

    /**
     * Public constructor for extending this class.
     */
    public DatabaseManager() { }

    /*
     * Methods for establishing database connections:
     */

    /**
     * Loads the data source using the given driver.<br>
     * If the source class returned by the given driver implements the {@link java.sql.Driver} interface, the driver
     * must specify a valid jdbc url.
     *
     * @param driver a database driver to load.
     * @throws IllegalArgumentException if the given driver is missing a jdbc-url.
     * @throws IllegalStateException if a database connection is already established.
     * @throws DatabaseException if the data source failed to load.
     */
    public final void load(@NotNull final Driver driver) throws DatabaseException {
        Preconditions.checkState(this.source == null, "connection already established");
        HikariConfig config = new HikariConfig(driver.getProperties());

        try {
            if (java.sql.Driver.class.isAssignableFrom(driver.getSource())) {
                if (driver.getJdbcUrl() == null) {
                    throw new IllegalArgumentException("driver is missing jdbc-url");
                }

                config.setDriverClassName(driver.getSource().getName());
                config.setAutoCommit(true);
            } else {
                config.setDataSourceClassName(driver.getSource().getName());
                config.setAutoCommit(true);
            }

            // Driver have been loaded successfully.
            this.config = config;
            this.driver = driver;
        } catch (RuntimeException ex) {
            throw new DatabaseException("failed to load driver", ex);
        }
    }

    /**
     * Connects to a data source, expecting that all required settings are already set.
     * If the connection to the data source has established successfully, the {@link #initialize(Connection)} method
     * will be automatically called to initialize the data source.
     *
     * @throws DatabaseException if the connection or initialization fails.
     * @see #connect(Properties)
     */
    public final void connect() throws DatabaseException {
        Preconditions.checkState(this.source == null, "connection already established");
        Preconditions.checkState(this.config != null, "missing database driver");
        HikariDataSource source;

        try {
            getLogger().info("Setting up data source...");
            source = new HikariDataSource(this.config);
        } catch (RuntimeException ex) {
            getLogger().log(Level.WARNING, "Failed to setup data source", ex);
            throw new DatabaseException("data source setup failed");
        }

        getLogger().info("Trying to establish connection...");
        try (Connection connection = source.getConnection()) {
            getLogger().info("Successfully established connection. Initializing database...");
            initialize(connection);
            getLogger().info("Successfully initialized database.");

            this.source = source;
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to establish connection", ex);
            disconnect();
            throw new DatabaseException("connection failed");
        } catch (DatabaseException ex) {
            getLogger().log(Level.WARNING, "Failed to initialize database", ex);
            disconnect();
            throw ex;
        }
    }

    /**
     * Connects to a data source using the given connection properties.<br>
     * This method will check for all occurrences of the property keys in the data source properties and replaces them
     * with the corresponding values.
     *
     * @param properties properties for replacing placeholders.
     * @throws DatabaseException if the connection or initialization fails.
     * @see #connect()
     */
    public final void connect(@NotNull final Properties properties) throws DatabaseException {
        Preconditions.checkState(this.source == null, "connection already established");
        Preconditions.checkState(this.driver != null, "missing database driver");

        if (java.sql.Driver.class.isAssignableFrom(this.driver.getSource())) {
            String jdbc = Objects.requireNonNull(this.driver.getJdbcUrl());
            String placeholder;

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (jdbc.contains(placeholder = "{" + entry.getKey() + "}")) {
                    jdbc = jdbc.replace(placeholder, entry.getValue().toString());
                }
            }

            this.config.setJdbcUrl(jdbc);
        } else if (javax.sql.DataSource.class.isAssignableFrom(driver.getSource())) {
            String placeholder, value;
            boolean dirty = false;

            for (Map.Entry<Object, Object> property : this.config.getDataSourceProperties().entrySet()) {
                value = property.getValue().toString();

                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    if (value.contains(placeholder = "{" + entry.getKey() + "}")) {
                        value = value.replace(placeholder, entry.getValue().toString());
                        dirty = true;
                    }
                }

                if (dirty) {
                    property.setValue(value);
                    dirty = false;
                }
            }
        }

        this.config.setUsername(properties.getProperty("username"));
        this.config.setPassword(properties.getProperty("password"));
        this.connect();
    }

    /**
     * Disconnects from and shutdowns the connected data source.<br>
     * Calling this method has no effect if no connection is established.
     */
    public final void disconnect() {
        if (this.source != null && !this.source.isClosed()) {
            this.source.close();

            getLogger().info("Closed connection to database.");
        }

        this.source = null;
    }

    /*
     * Abstract methods:
     */

    /**
     * Initializes the connected database.
     * This method will automatically be called after a connection to the data source has been established.
     * @param connection an active connection from the connected data source.
     * @throws DatabaseException if the initialization fails.
     */
    protected abstract void initialize(@NotNull final Connection connection) throws DatabaseException;

    /*
     * Methods for handling database connections:
     */

    /**
     * Fetches or opens a connection to the database by calling the appropriate method on connected data source.<br>
     *
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

    /*
     * Static methods:
     */

    /**
     * Returns the logger for this class, which will also be used from the driver and query loaders.
     *
     * @return the logger instance for this class.
     * @throws IllegalStateException if the logger for this class is not set.
     */
    public static @NotNull Logger getLogger() {
        if (logger == null) {
            throw new IllegalStateException("logger is not available");
        }

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
            getLogger().log(Level.SEVERE, "Could not close database connection", ex);
        }
    }
}

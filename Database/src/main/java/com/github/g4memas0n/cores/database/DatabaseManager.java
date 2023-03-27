package com.github.g4memas0n.cores.database;

import com.github.g4memas0n.cores.database.driver.Driver;
import com.github.g4memas0n.cores.database.driver.DriverLoader;
import com.github.g4memas0n.cores.database.query.QueryLoader;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
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

    private final String basename;

    private volatile HikariDataSource source;
    private volatile HikariConfig config;
    private volatile QueryLoader queries;

    /**
     * Public constructor for extending this class.
     */
    public DatabaseManager() {
        this.basename = null;
    }

    /**
     * Public constructor for extending this class with a specified queries file basename.
     * @param basename the basename for the queries file.
     */
    public DatabaseManager(@NotNull final String basename) {
        this.basename = basename;
    }

    /*
     * Methods for establishing database connections:
     */

    /**
     * Tries to Load the given {@code driver}.<br>
     * The source class returned by the given {@code driver} must either implement the {@link java.sql.Driver} or the
     * {@link javax.sql.DataSource} interface, otherwise it will be treated as invalid and an exception will be
     * thrown.<br>
     * Note that when the source class returned by the given {@code driver} implements the {@link java.sql.Driver}
     * interface, the driver must specify a jdbc url. Otherwise, if it implements the {@link javax.sql.DataSource}
     * interface, the driver must specify the data-source properties.
     *
     * @param driver a driver object that represents a database driver to load.
     * @throws IllegalArgumentException if the given {@code driver} will be treated as invalid.
     * @throws IllegalStateException if a connection to a database is already established.
     * @throws DatabaseException if it fails to load the driver for unexpected reasons.
     * @see #load(String, String)
     */
    public final void load(@NotNull final Driver driver) throws DatabaseException {
        Preconditions.checkState(this.source == null, "connected to database already established");
        HikariConfig config = new HikariConfig(driver.getProperties());

        try {
            if (java.sql.Driver.class.isAssignableFrom(driver.getSource())) {
                Preconditions.checkArgument(driver.getJdbcUrl() != null, "driver is missing jdbc-url");

                config.setDriverClassName(driver.getSource().getName());
                config.setJdbcUrl(driver.getJdbcUrl());
                config.setAutoCommit(true);
            } else if (javax.sql.DataSource.class.isAssignableFrom(driver.getSource())) {
                config.setDataSourceClassName(driver.getSource().getName());
                config.setAutoCommit(true);
            } else {
                throw new IllegalArgumentException("driver contains illegal source class");
            }

            this.config = config; // Driver have been loaded successfully.
        } catch (RuntimeException ex) {
            throw new DatabaseException("failed to load driver", ex);
        }

        if (this.basename != null) {
            try {
                this.queries = QueryLoader.getLoader(this.basename, driver);
            } catch (IllegalArgumentException | IOException ex) {
                getLogger().log(Level.WARNING, "Failed to load queries file", ex);
            }
        }
    }

    /**
     * Loads a driver object from the drivers file located at the given {@code path} that matches the given database
     * {@code type}.
     *
     * @param path the path to the file containing the driver information.
     * @param type the database type (such as {@code MySQL}, {@code SQLite}, {@code etc...}), for which the driver to
     *             be loaded should be.
     * @throws IllegalArgumentException if the file at the given {@code path} cannot be loaded.
     * @throws IllegalStateException if a connection to a database is already established.
     * @throws DatabaseException if no driver can be found to load.
     * @see #load(Driver)
     * @see DriverLoader
     */
    public final void load(@NotNull final String path, @NotNull final String type) throws DatabaseException {
        Preconditions.checkState(this.source == null, "connected to database already established");
        DriverLoader loader;

        try {
            loader = DriverLoader.getLoader(path);
        } catch (IOException ex) {
            throw new DatabaseException("could not load any driver", ex);
        }

        for (Driver driver : loader.loadDrivers(type)) {
            try {
                load(driver);
            } catch (IllegalArgumentException ignored) {

            }
        }

        throw new DatabaseException("could not find any driver to load");
    }

    /**
     * Connects to a remote database, expecting that all required connection settings are already specified.
     * If a connection to the database has established successfully, the {@link #initialize()} method will be
     * automatically called to initialize the database.
     *
     * @throws DatabaseException if it fails to establish a connection or to initialize the database.
     * @see #connect(Properties)
     */
    public final void connect() throws DatabaseException {
        Preconditions.checkState(this.source == null, "connected to database already established");
        Preconditions.checkState(this.config != null, "no driver have been loaded yet");
        HikariDataSource source;

        try {
            getLogger().info("Trying to establish connection to database...");
            source = new HikariDataSource(this.config);
            getLogger().info("Successfully established connection to database.");

            getLogger().info("Initializing database...");
            initialize();
            getLogger().info("Successfully initialized database.");

            this.source = source;
        } catch (SQLException ex) {
            getLogger().warning("Failed to initialize database: " + ex.getMessage());
            disconnect();
            throw new DatabaseException("failed to initialize database", ex);
        } catch (RuntimeException ex) {
            getLogger().warning("Failed to establish connection to database: " + ex.getMessage());
            throw new DatabaseException("failed to establish connection", ex);
        }
    }

    /**
     * Connects to a remote database that ist accessible with the given {@code properties}.<br>
     * Before connecting, all occurrences of all the {@link Placeholder placeholders} will be replaced with their
     * according value specified in the given {@code properties}. If a placeholder is found in the connection settings
     * that is not found in the given {@code properties}, an exception will be thrown.<br>
     * The property keys that will be used are specified by the {@link Placeholder#getKey() getKey()} method in the
     * {@link Placeholder} enumeration.
     *
     * @param properties the connection properties to replace the placeholders.
     * @throws IllegalArgumentException if the given {@code properties} are missing a required entry.
     * @throws DatabaseException if it fails to establish a connection or to initialize the database.
     * @see #connect()
     */
    public final void connect(@NotNull final Properties properties) throws DatabaseException {
        Preconditions.checkState(this.source == null, "connected to database already established");
        Preconditions.checkState(this.config != null, "no driver have been loaded yet");
        final HikariConfig config = new HikariConfig();

        try {
            this.config.copyStateTo(config);
        } catch (RuntimeException ex) {
            throw new DatabaseException("failed to setup data-source config", ex);
        }

        if (config.getJdbcUrl() != null) {
            String jdbcUrl = config.getJdbcUrl();

            for (final Placeholder entry : Placeholder.values()) {
                if (jdbcUrl.contains(entry.getPlaceholder())) {
                    if (!properties.containsKey(entry.getKey())) {
                        throw new IllegalArgumentException("properties are missing " + entry.getKey() + " key");
                    }

                    jdbcUrl = jdbcUrl.replace(entry.getPlaceholder(), properties.getProperty(entry.getKey()));
                }
            }

            config.setJdbcUrl(jdbcUrl);
        } else {
            Properties srcProperties = config.getDataSourceProperties();
            String value;

            for (final Placeholder entry : Placeholder.values()) {
                for (final Map.Entry<Object, Object> property : srcProperties.entrySet()) {
                    value = property.getValue().toString();

                    if (value.contains(entry.getPlaceholder())) {
                        if (!properties.containsKey(entry.getKey())) {
                            throw new IllegalArgumentException("properties are missing " + entry.getKey() + " key");
                        }

                        property.setValue(value.replace(entry.getPlaceholder(), properties.getProperty(entry.getKey())));
                    }
                }

                if (entry.hasProperty() && properties.containsKey(entry.getKey())) {
                    config.addDataSourceProperty(entry.getProperty(), properties.getProperty(entry.getKey()));
                }
            }
        }

        config.setUsername(properties.getProperty(Placeholder.USERNAME.getKey()));
        config.setPassword(properties.getProperty(Placeholder.PASSWORD.getKey()));

        this.config = config;
        this.connect();
    }

    /**
     * Initializes the database by ensuring that all needed database tables, indices, etc... exists.
     *
     * @throws SQLException if any exception occurs during the initialization.
     */
    public abstract void initialize() throws SQLException;

    /**
     * Disconnects from the connected database and shutdowns the used data source.<br>
     * Note that calling this method has no effect if no connection to a database exists.
     */
    public final void disconnect() {
        if (this.source != null && !this.source.isClosed()) {
            this.source.close();
            this.source = null;

            getLogger().info("Closed connection to database.");
        }

        this.source = null;
    }

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
     * Methods for accessing the query loader:
     */

    /**
     * Prepares the sql statement on the given {@code connection} that has the given query {@code identifier}.<br>
     * Note that this method will only work if the loaded driver had specified a queries file, otherwise it will throw
     * an exception.
     *
     * @param connection the connection to prepare the statement on.
     * @param identifier the string that uniquely identifies a query.
     * @return the prepared statement for the query that has the given {@code identifier}.
     * @throws IllegalArgumentException if no query with the given {@code identifier} exists.
     * @throws IllegalStateException if no queries file is currently loaded.
     * @throws SQLException if it fails to prepare the statement.
     */
    public final @NotNull PreparedStatement prepare(@NotNull final Connection connection,
                                                    @NotNull final String identifier) throws SQLException {
        Preconditions.checkState(this.queries != null, "no loaded queries file");

        try {
            return connection.prepareStatement(this.queries.getQuery(identifier));
        } catch (MissingResourceException ex) {
            throw new IllegalArgumentException("query identifier " + identifier + " does not exist", ex);
        }
    }

    /**
     * Loads the sql query that has the given query {@code identifier} from the currently loaded queries file.<br>
     * Note that this method will only work if the loaded driver had specified a queries file, otherwise it will throw
     * an exception.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the query that has the given {@code identifier}.
     * @throws IllegalArgumentException if no query with the given {@code identifier} exists.
     * @throws IllegalStateException if no queries file is currently loaded.
     */
    public final @NotNull String query(@NotNull final String identifier) {
        Preconditions.checkState(this.queries != null, "no loaded queries file");

        try {
            return this.queries.getQuery(identifier);
        } catch (MissingResourceException ex) {
            throw new IllegalArgumentException("query with identifier " + identifier + " does not exist", ex);
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

    /*
     * Static classes:
     */

    /**
     * A placeholder enumeration for all the available placeholders in the database connection settings.
     *
     * @since 1.0.0
     */
    public enum Placeholder {

        /**
         * Represents the database name placeholder.
         */
        DATABASE("databaseName"),

        /**
         * Represents the server name/host placeholder.
         */
        HOST("serverName"),

        /**
         * Represents the server port placeholder.
         */
        PORT("portNumber"),

        /**
         * Represents the username placeholder.
         */
        USERNAME(),

        /**
         * Represents the password placeholder.
         */
        PASSWORD(),

        /**
         * Represents the path placeholder.
         */
        PATH();

        private final String property;

        Placeholder() {
            this.property = null;
        }

        Placeholder(@NotNull final String property) {
            this.property = property;
        }

        /**
         * Returns the property key for this {@code Placeholder}.
         * @return the property key representation.
         */
        public @NotNull String getKey() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * Returns the actual placeholder string for this {@code Placeholder} that will be replaced if found.
         * @return the placeholder string representation.
         */
        public @NotNull String getPlaceholder() {
            return "{" + getKey() + "}";
        }

        /**
         * Returns the property key for the data-source properties, if it has one. Otherwise, an exception will be thrown.<br>
         * It might be useful to check if this {@code Placeholder} has a data-source property via the {@link #hasProperty()}
         * method.
         * @return the property key for data-source properties.
         * @throws NoSuchElementException if this {@code Placeholder} has no data-source property.
         * @see #hasProperty()
         */
        public @NotNull String getProperty() {
            if (this.property == null) {
                throw new NoSuchElementException("no property field set");
            }

            return this.property;
        }

        /**
         * Checks whether this {@code Placeholder} has a data-source property.
         * @return {@code true}, if it has a data-source property. {@code false}, otherwise.
         */
        public boolean hasProperty() {
            return this.property != null;
        }
    }
}

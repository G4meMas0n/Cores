package com.github.g4memas0n.cores.database;

import com.github.g4memas0n.cores.database.driver.Driver;
import com.github.g4memas0n.cores.database.driver.DriverLoader;
import com.github.g4memas0n.cores.database.query.QueryLoader;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.MissingResourceException;
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
    private volatile QueryLoader queries;

    /**
     * Public default constructor for extending this class.
     */
    public DatabaseManager() { }

    /**
     * Loads the given {@code driver} by loading the drivers class.<br>
     * The class returned by the given {@code driver} must either implement the {@link java.sql.Driver} or the
     * {@link javax.sql.DataSource} interface, otherwise the given {@code driver} will be treated as invalid and an
     * exception will be thrown.<br>
     * Note that when the class returned by the given {@code driver} implements the {@link java.sql.Driver} interface,
     * the given driver must specify a jdbc url.
     *
     * @param driver a driver object that holds all required information to load a database driver.
     * @return {@code true} if the driver has been successfully loaded.
     * @throws IllegalArgumentException if the given {@code driver} will be treated as invalid.
     * @throws IllegalStateException if a connection to a database is already established.
     * @see DatabaseManager#load(String, String)
     */
    public final boolean load(@NotNull final Driver driver) {
        Preconditions.checkState(this.source == null, "Manager is already connected to a database");

        try {
            final Class<?> clazz = Class.forName(driver.getClassName());

            if (java.sql.Driver.class.isAssignableFrom(clazz)) {
                Preconditions.checkArgument(driver.getJdbcUrl() != null, "Required driver jdbc-url is null");

                this.config = new HikariConfig();
                this.config.setDriverClassName(clazz.getName());
                this.config.setJdbcUrl(driver.getJdbcUrl());
            }

            if (javax.sql.DataSource.class.isAssignableFrom(clazz)) {
                this.config = new HikariConfig();
                this.config.setDataSourceClassName(clazz.getName());
            }

            if (this.config != null) {
                if (driver.getProperties() != null) {
                    this.config.setDataSourceProperties(driver.getProperties());
                }

                if (driver.getQueries() != null) {
                    try {
                        this.queries = QueryLoader.loadFile(driver.getQueries());
                    } catch (IllegalArgumentException ex) {
                        getLogger().log(Level.WARNING, "Failed to load queries file " + driver.getQueries() + " for driver " + driver, ex);
                    }
                }

                this.config.setAutoCommit(true);
                return true;
            }
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.FINE, "Could not load driver class", ex);
            return false;
        }

        throw new IllegalArgumentException("Invalid class in the driver information");
    }

    /**
     * Loads a driver object from the drivers file located at the given {@code path} that matches the given database
     * {@code type}.
     *
     * @param path the path to the file containing the driver information for one or more drivers.
     * @param type the database type (such as {@code MySQL}, {@code SQLite}, {@code etc...}), for which the driver to
     *             be loaded should be.
     * @return {@code true} if a driver for the given database {@code type} has been successfully loaded.
     * @throws IllegalArgumentException if the file at the given {@code path} cannot be loaded.
     * @throws IllegalStateException if a connection to a database is already established.
     * @see DatabaseManager#load(Driver)
     * @see DriverLoader
     */
    public final boolean load(@NotNull final String path, @NotNull final String type) {
        Preconditions.checkState(this.source == null, "Manager is already connected to a database");

        try {
            final DriverLoader loader = DriverLoader.loadFile(path);

            try {
                for (final Driver driver : loader.loadDrivers(type)) {
                    if (this.load(driver)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException ignored) {

            }

            return false;
        } catch (IllegalArgumentException ex) {
            getLogger().log(Level.SEVERE, "Failed to load driver file at " + path, ex);

            throw ex;
        }
    }

    /**
     * Connects to a local database that is located at the given {@code path}.<br>
     * Before connecting to the database, all occurrences of the {@literal {Path}} placeholder in the drivers properties
     * and jdbc url will be replaced with the given {@code path}.
     *
     * @param path the path to the location of the local database.
     * @return {@code true} if a connection to the database has been successfully established.
     * @throws IllegalStateException if a connection to a database is already established or no driver has been loaded.
     * @see DatabaseManager#connect(String, String, int, String, String)
     */
    public final boolean connect(@NotNull final String path) {
        Preconditions.checkState(this.source == null, "Manager is already connected to a database");
        Preconditions.checkState(this.config != null, "No driver has been loaded yet");
        final HikariDataSource source = new HikariDataSource();
        this.config.copyStateTo(source);

        if (source.getJdbcUrl() != null) {
            source.setJdbcUrl(source.getJdbcUrl().replace("{Path}", path));
        } else {
            final Properties properties = source.getDataSourceProperties();

            for (final String property : properties.stringPropertyNames()) {
                if (properties.getProperty(property).contains("{Path}")) {
                    properties.setProperty(property, properties.getProperty(property).replace("{Path}", path));
                }
            }
        }

        return this.connect(source);
    }

    /**
     * Connects to a remote database that is reachable with the given {@code host}, {@code port} and {@code database}
     * name.<br>
     * Before connecting to the database, all occurrences of the {@literal {Host}}, {@literal {Port}} and
     * {@literal {Database}} placeholders in the drivers properties and jdbc url will be replaced with the given
     * {@code host}, {@code port} and {@code database} name. If not specified before, the given {@code host},
     * {@code port} and {@code database} name will be added to the drivers properties.
     *
     * @param database the name of the using database.
     * @param host the address that the remote database is hosted on.
     * @param port the port that the remote database is listen to.
     * @param user the name of the using database user.
     * @param password the password of the using database user.
     * @return {@code true} if a connection to the database has been successfully established.
     * @throws IllegalStateException if a connection to a database is already established or no driver has been loaded.
     * @see DatabaseManager#connect(String)
     */
    public final boolean connect(@NotNull final String database, @NotNull final String host, final int port,
                                 @NotNull final String user, @NotNull final String password) {
        Preconditions.checkState(this.source == null, "Manager is already connected to a database");
        Preconditions.checkState(this.config != null, "No driver has been loaded yet");
        final HikariDataSource source = new HikariDataSource();
        this.config.copyStateTo(source);

        if (source.getJdbcUrl() != null) {
            source.setJdbcUrl(source.getJdbcUrl().replace("{Host}", host)
                    .replace("{Port}", Integer.toString(port))
                    .replace("{Database}", database));
        } else {
            source.addDataSourceProperty("databaseName", database);
            source.addDataSourceProperty("portNumber", port);
            source.addDataSourceProperty("serverName", host);
        }

        source.setUsername(user);
        source.setPassword(password);

        return this.connect(source);
    }

    private boolean connect(@NotNull final HikariDataSource source) {
        try {
            final Connection connection = source.getConnection();

            getLogger().info("Successfully established connection to database");

            try {
                getLogger().info("Initializing database...");
                this.initialize(connection);
                this.source = source;
                getLogger().info("Successfully initialized database");

                return true;
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, "Failed to initialize database", ex);
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Failed to establish connection to database", ex);
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Unexpected exception during data source setup", ex);
        }

        return false;
    }

    /**
     * Initializes the database by ensuring that all needed database tables, indices, etc... exists.
     * If any database errors occurs during the initialization then an exception should be thrown by the implementing
     * class.
     *
     * @param connection an active connection session to the database.
     * @throws SQLException if any exception occurs during the database initialization.
     */
    public abstract void initialize(@NotNull final Connection connection) throws SQLException;

    /**
     * Disconnects from the connected database and shutdowns the used data source.<br>
     * Note that calling this method has no effect if no connection to a database exists.
     *
     * @return {@code true} if it has been disconnected from the database as a result of this call.
     */
    public final boolean disconnect() {
        if (this.source != null && !this.source.isClosed()) {
            this.source.close();
            this.source = null;
            return true;
        }

        this.source = null;
        return false;
    }

    /**
     * Fetches or opens a connection to the database by calling the appropriate method on connected data source.<br>
     * The result of this method may be null if a database error occurred during the connection opening.
     *
     * @return a valid connection session to the database, or null.
     * @throws IllegalStateException if no connection to a database exists.
     */
    public final @Nullable Connection open() {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");

        try {
            return this.source.getConnection();
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to fetch connection to database", ex);
            return null;
        }
    }

    /**
     * Fetches or opens a connection to the database and starts a transaction on the fetched connection session.
     * See {@link DatabaseManager#open()} for how the connection gets fetched.<br>
     * The result of this method may be null if a database error occurred during the connection opening or during the
     * transaction start.
     *
     * @return a valid connection session to the database with an active transaction, or null.
     * @throws IllegalStateException if no connection to a database exists.
     * @see DatabaseManager#open()
     */
    public final @Nullable Connection begin() {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");
        final Connection connection = this.open();

        try {
            if (connection != null) {
                connection.setAutoCommit(false);

                return connection;
            }
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to begin transaction on fetched connection to database", ex);
            this.close(connection);
        }

        return null;
    }

    /**
     * Loads the sql query that has the given {@code identifier} from the currently loaded queries file.<br>
     * Note that this method will only work if the loaded driver had specified a queries file, otherwise it will throw
     * an exception.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the query that has the given {@code identifier}.
     * @throws IllegalArgumentException if the given {@code identifier} is blank or no query with the given
     *                                  {@code identifier} exists.
     * @throws IllegalStateException if no queries file is currently loaded.
     */
    public final @NotNull String query(@NotNull final String identifier) {
        Preconditions.checkState(this.queries != null, "Manager has no loaded queries file");
        Preconditions.checkArgument(!identifier.isBlank(), "Query identifier cannot be blank");

        try {
            return this.queries.getQuery(identifier);
        } catch (MissingResourceException ex) {
            throw new IllegalArgumentException("Query with identifier " + identifier + " does not exist", ex);
        }
    }

    /**
     * Ends an active transaction on the given {@code connection} session by committing or rolling back the last
     * performed updates and closes the connection afterwards.<br>
     * The result of this method represents whether the last performed updates on the given {@code connection} has been
     * committed. This means that the result will only be {@code true} if the given {@code commit} parameter is set to
     * {@code true} and the performed commit was successful.
     *
     * @param connection the connection session with an active transaction.
     * @param commit {@code true} if the last performed updates should be committed to the database.
     * @return {@code true} if the last performed updates has been successfully committed.
     * @throws IllegalArgumentException if the given {@code connection} is already closed or is in auto-commit mode.
     * @see DatabaseManager#close(Connection)
     */
    public final boolean end(@NotNull final Connection connection, final boolean commit) {
        try {
            Preconditions.checkArgument(!connection.isClosed(), "Connection is already closed");
            Preconditions.checkArgument(!connection.getAutoCommit(), "Connection is in auto-commit mode");

            if (commit) {
                connection.commit();
            } else {
                connection.rollback();
            }

            connection.setAutoCommit(false);
            this.close(connection);
            return commit;
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to end transaction on fetched connection to database", ex);
            return false;
        }
    }

    /**
     * Closes the given {@code connection} session to the database if it is not already closed.<br>
     * Note that the given {@code connection} session will also be closed if it does not belong to the connected data
     * source.
     *
     * @param connection the connection session to be closed.
     * @throws IllegalArgumentException if the given {@code connection} is not in auto-commit mode.
     */
    public final void close(@NotNull final Connection connection) {
        try {
            if (connection.isClosed()) {
                return;
            }

            Preconditions.checkArgument(connection.getAutoCommit(), "Connection is not in auto-commit mode");
            connection.close();
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to close fetched connection to database", ex);
        }
    }

    /**
     * Returns the logger for this class, which will also be used from the driver and query loaders.
     *
     * @return the logger instance for this class.
     * @throws IllegalStateException if the logger for this class is not set.
     */
    public static @NotNull Logger getLogger() {
        if (logger == null) {
            throw new IllegalStateException("The logger is not available");
        }

        return logger;
    }
}

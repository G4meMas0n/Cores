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
import java.sql.Statement;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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
     * The maximum time in seconds to validate an active connection session.
     */
    protected static final int VALIDATION_TIMEOUT = 15;

    /**
     * The reference to the logger for this class.
     */
    protected static Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private final Map<Long, Connection> transactions;
    private HikariDataSource source;
    private HikariConfig config;
    private QueryLoader queries;

    /**
     * Public default constructor for extending this class.
     */
    public DatabaseManager() {
        this.transactions = new ConcurrentHashMap<>();
    }

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
     *
     * @see DatabaseManager#load(String, String)
     */
    public final boolean load(@NotNull final Driver driver) {
        Preconditions.checkState(this.source == null, "Manager is already connected to a database");

        try {
            final Class<?> clazz = Class.forName(driver.getClassName());

            if (java.sql.Driver.class.isAssignableFrom(clazz)) {
                Preconditions.checkArgument(driver.getJdbcUrl() != null, "");

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
     *
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
     *
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
     *
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
            getLogger().info("Initializing database...");

            if (this.initialize(connection)) {
                getLogger().info("Successfully initialized database");
                this.source = source;
                return true;
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
     *
     * @param connection an active connection session to the database.
     * @return {@code true} if the database has been successfully initialized.
     */
    public abstract boolean initialize(@NotNull final Connection connection);

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
     * Fetches or opens a connection to the connected database.<br>
     * If the thread calling this method is currently involved in a database transaction then the associated connection
     * of the transaction will be returned.<br>
     * This method does the same as calling {@link DatabaseManager#open(long) open(Thread.currentThread().getId())}.
     *
     * @return a connection session to the database.
     * @throws IllegalStateException if no connection to a database exists.
     *
     * @see DatabaseManager#open(long)
     */
    public final @Nullable Connection open() {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");
        return this.open(Thread.currentThread().getId());
    }

    /**
     * Fetches or opens a connection to the connected database.<br>
     * If the thread specified by the given {@code thread} id is currently involved in a database transaction then the
     * associated connection session of the transaction will be returned.
     *
     * @param thread the thread id of an existing thread.
     * @return a connection session to the database.
     * @throws IllegalArgumentException if the given {@code thread} id is negative.
     * @throws IllegalStateException if no connection to a database exists.
     *
     * @see DatabaseManager#open()
     */
    public final @Nullable Connection open(final long thread) {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");
        Preconditions.checkArgument(thread >= 0, "Thread id cannot be negative");

        try {
            Connection connection = this.transactions.get(thread);

            if (connection != null) {
                if (connection.isValid(VALIDATION_TIMEOUT)) {
                    return connection;
                }

                return null;
            }

            return this.source.getConnection();
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to fetch connection to database", ex);
            return null;
        }
    }

    /**
     * Gets the sql query that matches the given {@code identifier} from the currently loaded queries file.<br>
     * Note that this method will only work if the loaded driver had specified a queries file, otherwise it will throw
     * an exception.
     *
     * @param identifier the string that uniquely identifies a query.
     * @return the query that matches the given {@code identifier}.
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
     * Closes the given {@code connection} session to the database.<br>
     * If the given {@code connection} session relates to an active database transaction then the given
     * {@code connection} will not be closed.<br>
     * Note that the given {@code connection} session will also be closed if it does not belong to the active database
     * connection.
     *
     * @param connection the connection session that should be closed.
     *
     * @see DatabaseManager#close(Connection, Statement) 
     */
    public final void close(@NotNull final Connection connection) {
        this.close(connection, null);
    }

    /**
     * Closes the given {@code connection} session to the database and its active {@code statement} if it is not
     * null.<br>
     * If the given {@code connection} session relates to an active database transaction then the given
     * {@code connection} will not be closed.<br>
     * Note that the given {@code connection} session will also be closed if it does not belong to the active database
     * connection and the given {@code statement} will always be closed if it is not null.
     *
     * @param connection the connection session that should be closed.
     * @param statement an active statement to the given {@code connection} that should also be closed.
     *
     * @see DatabaseManager#close(Connection)
     */
    public final void close(@NotNull final Connection connection, @Nullable final Statement statement) {
        if (statement != null) {
            try {
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException ex) {
                getLogger().log(Level.WARNING, "Failed to close statement of fetched connection to database", ex);
            }
        }

        try {
            if (!connection.isClosed()) {
                for (final Connection transaction : this.transactions.values()) {
                    if (connection.equals(transaction)) {
                        return;
                    }
                }

                connection.close();
            }
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to close fetched connection to database", ex);
        }
    }

    /**
     * Begins a new database transaction that will be associated with the thread calling this method.<br>
     * This method does the same as calling {@link DatabaseManager#begin(long) begin(Thread.currentThread().getId())}.
     *
     * @throws IllegalStateException if no connection to a database exists.
     *
     * @see DatabaseManager#begin(long)
     */
    public final void begin() {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");
        this.begin(Thread.currentThread().getId());
    }

    /**
     * Begins a new database transaction that will be associated with the thread specified by the given {@code thread}
     * id.
     *
     * @param thread the thread id of an existing thread.
     * @throws IllegalArgumentException if the given {@code thread} id is negative.
     * @throws IllegalStateException if no connection to a database exists.
     *
     * @see DatabaseManager#begin()
     */
    public final void begin(final long thread) {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");
        Preconditions.checkArgument(thread >= 0, "Thread id cannot be negative");

        try {
            Connection connection = this.transactions.get(thread);

            if (connection != null) {
                if (!connection.isClosed()) {
                    getLogger().warning("Thread with id " + thread + " is already associated with an active database transaction");
                    return;
                }

                this.transactions.remove(thread);
            }

            connection = this.source.getConnection();
            connection.setAutoCommit(false);

            this.transactions.put(thread, connection);
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to begin transaction on fetched connection to database", ex);
        }
    }

    /**
     * Ends an active database transaction that is associated with the current thread by committing/aborting the last
     * performed updates.<br>
     * This method does the same as calling
     * {@link DatabaseManager#end(long, boolean) end(Thread.currentThread().getId(), commit)}.
     *
     * @param commit {@code true} if the last performed updates should be committed to the database.
     * @throws IllegalStateException if no connection to a database exists.
     *
     * @see DatabaseManager#end(long, boolean)
     */
    public final void end(final boolean commit) {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");
        this.end(Thread.currentThread().getId(), commit);
    }

    /**
     * Ends an active database transaction that is associated with the thread specified by the given {@code thread} id
     * by committing/aborting the last performed updates.
     *
     * @param thread the thread id of an existing thread.
     * @param commit {@code true} if the last performed updates should be committed to the database.
     * @throws IllegalArgumentException if the given {@code thread} id is negative.
     * @throws IllegalStateException if no connection to a database exists.
     *
     * @see DatabaseManager#end(boolean)
     */
    public final void end(final long thread, final boolean commit) {
        Preconditions.checkState(this.source != null, "Manager is not connected to a database");
        Preconditions.checkArgument(thread >= 0, "Thread id cannot be negative");

        try {
            final Connection connection = this.transactions.remove(thread);

            if (connection != null) {
                if (connection.isClosed()) {
                    getLogger().warning("Transaction connection associated with the thread specified by the id " + thread + " is already closed.");
                    return;
                }

                if (commit) {
                    connection.commit();
                } else {
                    connection.rollback();
                }

                connection.setAutoCommit(true);
                connection.close();
            } else {
                getLogger().warning("Thread with id " + thread + " is not associated with an active database transaction");
            }
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to end transaction on fetched connection to database", ex);
        }
    }

    /**
     * Checks whether the current thread is associated to an active database transaction.<br>
     * This method returns the same as calling
     * {@link DatabaseManager#transaction(long) transaction(Thread.currentThread().getId())}.
     *
     * @return {@code true} if there is an active database transaction that is associated with the current thread.
     */
    public final boolean transaction() {
        return this.transactions.containsKey(Thread.currentThread().getId());
    }

    /**
     * Checks whether the thread with the given {@code thread} id is associated to an active database transaction.
     *
     * @param thread the thread id of an existing thread.
     * @return {@code true} if there is an active database transaction that is associated with the thread specified
     *                      by the given {@code thread} id.
     */
    public final boolean transaction(final long thread) {
        return this.transactions.containsKey(thread);
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

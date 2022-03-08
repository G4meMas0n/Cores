package com.github.g4memas0n.cores.database;

import com.github.g4memas0n.cores.database.loader.DriverLoader;
import com.github.g4memas0n.cores.database.loader.DriverLoader.Driver;
import com.github.g4memas0n.cores.database.loader.JsonDriverLoader;
import com.github.g4memas0n.cores.database.loader.StatementLoader;
import com.github.g4memas0n.cores.database.loader.XmlStatementLoader;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DatabaseManager {

    protected static final int VALIDATION_TIMEOUT = 15;
    protected static Logger logger;

    private final Map<Long, Connection> transactions;
    private HikariDataSource source;
    private StatementLoader statements;
    private Driver driver;

    public DatabaseManager() {
        this.transactions = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the database using the specified connection to the database.
     * @param connection the {@link Connection} to initialize the database.
     * @return {@code true}, if the database has been successfully initialized, {@code false}, otherwise.
     */
    protected abstract boolean initialize(@NotNull final Connection connection);

    /**
     * Returns an input stream for the resource located at the specified path.
     * @param path the path to the resource.
     * @return the {@link InputStream} for reading the resource, or null if the resource could not be found.
     */
    protected abstract @Nullable InputStream getResource(@NotNull final String path);

    /**
     * Returns the statement of the specified {@code identifier} from the loaded statements file.
     * @param identifier the {@code identifier} that identifies the statement to get.
     * @return the statement with the specified {@code identifier}.
     * @throws IllegalStateException Thrown when no statements file for the current driver loaded.
     * @throws IllegalArgumentException Thrown when the specified identifier is blank.
     */
    public @NotNull String getStatement(@NotNull final String identifier) {
        Preconditions.checkState(this.statements != null, "The statement loader is not available");
        Preconditions.checkArgument(!identifier.isBlank(), "The statement identifier cannot be blank");

        return this.statements.get(identifier);
    }

    /**
     * Tries to load the specified {@link Driver Driver} and its associated statements file, if it exists.
     * This {@code DatabaseManager} will locate the path of the statements file using the method
     * {@link DatabaseManager#getResource(String)}.
     * @param driver the {@link Driver Driver} to load to.
     * @return {@code true}, if the specified driver has been successfully loaded.
     * @throws IllegalStateException Thrown when this {@code DatabaseManager} is already connected to a database.
     * @throws IllegalArgumentException Thrown when the specified {@link Driver Driver} has neither a driver class nor a data source class.
     * @see DatabaseManager#load(String, String)
     */
    public final boolean load(@NotNull final Driver driver) {
        Preconditions.checkState(this.source == null, "Already connected to a database");
        Preconditions.checkArgument(driver.driverClass() != null || driver.dataSourceClass() != null,
                "The driver must contain a driver class or a data source class");

        try {
            Class.forName(driver.dataSourceClass() != null ? driver.dataSourceClass() : driver.driverClass());

            if (driver.statements() != null && driver.statements().endsWith(".xml")) {
                final StatementLoader statements = new XmlStatementLoader();
                final InputStream stream = this.getResource(driver.statements());

                if (stream != null) {
                    try {
                        statements.load(stream);
                    } catch (IOException ex) {
                        getLogger().log(Level.WARNING, "Failed to load statements file for driver " + driver, ex);
                    }

                    this.statements = statements;
                }
            }

            this.driver = driver;
            return true;
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.FINE, "Could not load driver class", ex);
            return false;
        }
    }

    /**
     * Tries to load a driver for the specified database type from the drivers file specified by the given path.
     * This {@code DatabaseManager} will locate the path of the driver file using the method
     * {@link DatabaseManager#getResource(String)}.
     * @param path the path where the driver file is located.
     * @param type the database type that the loaded driver should support.
     * @return {@code true}, only if a driver for the specified database type has been successfully loaded.
     * @throws IllegalStateException Thrown when this {@code DatabaseManager} is already connected to a database.
     * @see DatabaseManager#load(Driver)
     */
    public final boolean load(@NotNull final String path, @NotNull final String type) {
        Preconditions.checkState(this.source == null, "Already connected to a database");
        final DriverLoader loader = new JsonDriverLoader();
        final InputStream stream = this.getResource(path);

        if (stream != null) {
            try {
                loader.load(stream);

                for (final Driver driver : loader.get(type)) {
                    if (this.load(driver)) {
                        return true;
                    }
                }
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Failed to load driver file", ex);
            }
        }

        return false;
    }

    /**
     * Connects this {@code DatabaseManager} to the database with the specified connection and login data.
     * @param database the name of the database to connect to.
     * @param host the address or the ip of the database to connect to.
     * @param port the port of the database to connect to.
     * @param user the username of the database user to log in.
     * @param password the password of the database user to log in.
     * @return {@code true}, if the connection to the specified database has been successfully established.
     * @throws IllegalStateException Thrown, when this {@code DatabaseManager} has no driver loaded.
     * @throws IllegalStateException Thrown, when this {@code DatabaseManager} is already connected to a database.
     */
    public final boolean connect(@NotNull final String database, @NotNull final String host, final int port,
                                 @NotNull final String user, @NotNull final String password) {
        Preconditions.checkState(this.source == null, "Already connected to a database");
        Preconditions.checkState(this.driver != null, "The driver has not been loaded yet");
        final HikariDataSource source = new HikariDataSource();

        if (this.driver.dataSourceClass() != null) {
            source.setDataSourceClassName(this.driver.dataSourceClass());
        }

        if (this.driver.driverClass() != null) {
            source.setDriverClassName(this.driver.driverClass());
        }

        source.addDataSourceProperty("characterEncoding", "UTF-8");
        source.addDataSourceProperty("cachePrepStmts", "true");
        source.addDataSourceProperty("prepStmtCacheSize" , "250");
        source.addDataSourceProperty("prepStmtCacheSqlLimit" , "2048");
        source.addDataSourceProperty("useServerPrepStmts", "true");
        source.setJdbcUrl(this.driver.url(database, host, port));
        source.setUsername(user);
        source.setPassword(password);
        source.setAutoCommit(true);

        try (Connection connection = source.getConnection()) {
            if (connection.isValid(VALIDATION_TIMEOUT)) {
                getLogger().info("Successfully established connection to database");
                this.source = source;

                return this.initialize(connection);
            }

            getLogger().severe("Failed to establish connection to database");
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Failed to connect to database", ex);
        }

        return false;
    }

    /**
     * Disconnects this {@code DatabaseManager} from the currently connected database.
     * @return {@code true}, if this {@code DatabaseManager} has successfully disconnected from the database.
     * @throws IllegalStateException Thrown when this {@code DatabaseManager} is not connected to any database.
     */
    public final boolean disconnect() {
        Preconditions.checkState(this.source != null, "Not connected to a database");
        if (!this.source.isClosed()) {
            this.source.close();
            this.source = null;
            return true;
        }

        return false;
    }

    /**
     * Opens a new {@link Connection} to the database or fetches the existing {@link Connection} when the current
     * thread is involved in a transaction.
     * @return the fetched {@link Connection} to the database.
     * @throws IllegalStateException Thrown when this {@code DatabaseManager} is not connected to any database.
     */
    public final @Nullable Connection open() {
        Preconditions.checkState(this.source != null, "Not connected to a database");
        final long id = Thread.currentThread().getId();

        try {
            Connection connection = this.transactions.get(id);

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
     * Closes the specified {@link Connection} to the database, when it is not currently involved in a transaction.
     * @param connection the {@link Connection} that should be closed.
     * @throws IllegalStateException Thrown when this {@code DatabaseManager} is not connected to any database.
     */
    public final void close(@NotNull final Connection connection) {
        Preconditions.checkState(this.source != null, "Not connected to a database");

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
     * Begins a new database transaction for the current thread.
     * @throws IllegalStateException Thrown when this {@code DatabaseManager} is not connected to any database.
     */
    public final void begin() {
        Preconditions.checkState(this.source != null, "Not connected to a database");
        final long id = Thread.currentThread().getId();

        try {
            Connection connection = this.transactions.get(id);

            if (connection != null) {
                if (!connection.isClosed()) {
                    getLogger().warning("Transaction had already began on fetched connection to database");
                    return;
                }

                this.transactions.remove(id);
            }

            connection = this.source.getConnection();
            connection.setAutoCommit(false);

            this.transactions.put(id, connection);
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to begin transaction on fetched connection to database", ex);
        }
    }

    /**
     * Ends the database transaction of the current thread by aborting or committing the last performed actions.
     * @param commit {@code true}, if the transaction should be committed, {@code false}, if aborted.
     * @throws IllegalStateException Thrown when this {@code DatabaseManager} is not connected to any database.
     */
    public final void end(final boolean commit) {
        Preconditions.checkState(this.source != null, "Not connected to a database");
        final long id = Thread.currentThread().getId();

        try {
            final Connection connection = this.transactions.remove(id);

            if (connection != null) {
                if (connection.isClosed()) {
                    getLogger().warning("Transaction cannot be ended on closed connection to database");
                    return;
                }

                if (commit) {
                    connection.commit();
                } else {
                    connection.rollback();
                }

                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to end transaction on fetched connection to database", ex);
        }
    }

    /**
     * Returns the registered logger of the {@code DatabaseManager}, which will also be used from the driver
     * and statements loaders.
     * @return the {@link Logger} of the {@code DatabaseManager}.
     */
    public static @NotNull Logger getLogger() {
        if (logger == null) {
            throw new IllegalStateException("The logger is not available");
        }

        return logger;
    }
}

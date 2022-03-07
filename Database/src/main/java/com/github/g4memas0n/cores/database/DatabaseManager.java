package com.github.g4memas0n.cores.database;

import com.github.g4memas0n.cores.database.loader.DriverLoader;
import com.github.g4memas0n.cores.database.loader.DriverLoader.Driver;
import com.github.g4memas0n.cores.database.loader.JsonDriverLoader;
import com.github.g4memas0n.cores.database.loader.StatementLoader;
import com.github.g4memas0n.cores.database.loader.XmlStatementLoader;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
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

    private static final int VALIDATION_TIMEOUT = 15;

    private static Logger logger;

    private final Map<Long, Connection> transactions;
    private HikariDataSource source;
    private StatementLoader statements;
    private Driver driver;

    public DatabaseManager() {
        this.transactions = new ConcurrentHashMap<>();
    }

    public @NotNull String getStatement(@NotNull final String identifier) {
        Preconditions.checkState(this.statements != null, "");
        Preconditions.checkArgument(!identifier.isBlank(), "The statement identifier cannot be blank");

        return this.statements.get(identifier);
    }

    public abstract @Nullable InputStream getResource(@NotNull final String path);

    public abstract boolean initialize(@NotNull final Connection connection);

    public void configure(@NotNull final HikariConfig config) {
        config.addDataSourceProperty("characterEncoding", "UTF-8");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize" , "250" );
        config.addDataSourceProperty("prepStmtCacheSqlLimit" , "2048" );
        config.addDataSourceProperty("useServerPrepStmts", "true");
    }

    public final boolean load(@NotNull final InputStream input, @NotNull final String type) {
        Preconditions.checkState(this.source == null, "");
        final DriverLoader loader = new JsonDriverLoader();

        try {
            loader.load(input);

            for (final Driver driver : loader.get(type)) {
                try {
                    Class.forName(driver.dataSourceClass() != null ? driver.dataSourceClass() : driver.driverClass());

                    if (driver.statements() != null && driver.statements().endsWith(".xml")) {
                        final StatementLoader statements = new XmlStatementLoader();
                        final InputStream stream = this.getResource(driver.statements());

                        if (stream != null) {
                            try {
                                loader.load(stream);
                            } catch (IOException ex) {
                                getLogger().log(Level.SEVERE, "Failed to load statements file for driver " + driver, ex);
                                continue;
                            }

                            this.statements = statements;
                        }
                    }

                    this.driver = driver;
                } catch (ClassNotFoundException ex) {
                    getLogger().log(Level.FINE, "Could not load driver class", ex);
                }
            }

            return this.driver != null;
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Failed to load driver file", ex);
        }

        return false;
    }

    public final boolean connect(@NotNull final String database, @NotNull final String host, final int port,
                           @NotNull final String user, @NotNull final String password) {
        Preconditions.checkState(this.source == null, "");
        Preconditions.checkState(this.driver != null, "");
        final HikariDataSource source = new HikariDataSource();

        if (this.driver.dataSourceClass() != null) {
            source.setDataSourceClassName(this.driver.dataSourceClass());
        }

        if (this.driver.driverClass() != null) {
            source.setDriverClassName(this.driver.driverClass());
        }

        source.setJdbcUrl(this.driver.url(database, host, port));
        source.setUsername(user);
        source.setPassword(password);
        source.setAutoCommit(true);

        this.configure(source);

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

    public final boolean disconnect() {
        Preconditions.checkState(this.source != null, "");
        if (!this.source.isClosed()) {
            this.source.close();
            this.source = null;
            return true;
        }

        return false;
    }

    public final @Nullable Connection open() {
        Preconditions.checkState(this.source != null, "");
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

    public final void close(@NotNull final Connection connection) {
        Preconditions.checkState(this.source != null, "");
        final long id = Thread.currentThread().getId();

        try {
            if (connection.isClosed() || connection.equals(this.transactions.get(id))) {
                return;
            }

            connection.close();
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Failed to close fetched connection to database", ex);
        }
    }

    public final void begin() {
        Preconditions.checkState(this.source != null, "");
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

    public final void end(final boolean commit) {
        Preconditions.checkState(this.source != null, "");
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

    public static @NotNull Logger getLogger() {
        if (logger == null) {
            throw new IllegalStateException("Cannot be invoked before it was set");
        }

        return logger;
    }
}

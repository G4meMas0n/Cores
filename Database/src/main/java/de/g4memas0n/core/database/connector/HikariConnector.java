package de.g4memas0n.core.database.connector;

import de.g4memas0n.core.database.DatabaseManager;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * An abstract connector class for connecting to a database using the hikari data source.
 */
public abstract class HikariConnector implements Connector {

    private final Consumer<HikariConfig> consumer;
    private HikariDataSource source;

    /**
     * A public constructor for the implementing hikari connector with a given custom configurator.
     * @param consumer a custom hikari configurator or null.
     */
    public HikariConnector(@Nullable Consumer<HikariConfig> consumer) {
        this.consumer = consumer;
    }

    /**
     * Initializes the hikari connector with the given connection settings.
     * <p>
     * The given class must either implement the {@link java.sql.Driver} or {@link javax.sql.DataSource} class. If the
     * class implements the {@link java.sql.Driver} class, a valid jdbc url must be given. Otherwise, if it does not
     * implement one of these classes the initialization will fail.
     * @param clazz the class to use.
     * @param jdbcUrl the jdbcUrl to use.
     * @param properties the properties to use.
     * @param user the user to user.
     * @param password the password to use.
     * @throws IllegalArgumentException if the class is unknown if the jdbc-url is missing.
     * @throws RuntimeException if the initialization of the hikari data source fails.
     */
    public void initialize(@NotNull Class<?> clazz, @Nullable String jdbcUrl, @Nullable Properties properties,
                           @Nullable String user, @Nullable String password) {
        Preconditions.checkState(this.source == null, "connector already initialized");
        HikariConfig config = new HikariConfig();

        DatabaseManager.getLogger().info("Configuring data source.");
        if (java.sql.Driver.class.isAssignableFrom(clazz)) {
            if (jdbcUrl == null) {
                throw new IllegalArgumentException("jdbc url required");
            }

            try {
                config.setDriverClassName(clazz.getName());
                config.setJdbcUrl(jdbcUrl);
            } catch (RuntimeException ex) {
                DatabaseManager.getLogger().log(Level.SEVERE, "Failed to configure data source.", ex);
                throw ex;
            }
        } else if (javax.sql.DataSource.class.isAssignableFrom(clazz)) {
            config.setDataSourceClassName(clazz.getName());
        } else {
            throw new IllegalArgumentException("unknown class");
        }

        if (properties != null) {
            config.setDataSourceProperties(properties);
        }

        config.setUsername(user);
        config.setPassword(password);
        config.setAutoCommit(true);

        if (this.consumer != null) {
            try {
                this.consumer.accept(config);
            } catch (RuntimeException ex) {
                DatabaseManager.getLogger().log(Level.WARNING, "Failed custom data source configuration.", ex);
            }
        }

        try {
            DatabaseManager.getLogger().info("Setting up data source.");
            this.source = new HikariDataSource(config);
        } catch (RuntimeException ex) {
            DatabaseManager.getLogger().log(Level.SEVERE, "Failed to set up data source.", ex);
            throw ex;
        }
    }

    @Override
    public void shutdown() {
        if (this.source != null && !this.source.isClosed()) {
            DatabaseManager.getLogger().info("Shutting down data source.");
            this.source.close();
        }

        this.source = null;
    }

    @Override
    public @NotNull Connection getConnection() throws SQLException {
        if (this.source == null) {
            throw new SQLException("hikari data source is null");
        }

        return this.source.getConnection();
    }
}

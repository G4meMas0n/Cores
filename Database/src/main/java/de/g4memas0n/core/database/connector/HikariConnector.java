package de.g4memas0n.core.database.connector;

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
     * The public default constructor for the implementing a hikari connector.
     */
    public HikariConnector() {
        this(null);
    }

    /**
     * The public constructor for the implementing a hikari connector with a given custom configurator.
     * @param consumer a custom hikari configurator or null.
     */
    public HikariConnector(@Nullable Consumer<HikariConfig> consumer) {
        this.consumer = consumer;
    }

    /**
     * Configures the given hikari config.
     * <p>
     * This method will be called during the {@link #initialize(Class, String, java.util.Properties, String, String)}
     * method and is intended to be overwritten by the implementing subclass to configure the hikari config.
     * @param config the hikari config to configure.
     */
    public void configure(@NotNull HikariConfig config) {
        /*
         * Setting this results in no initial connection validation, which is not required as the #getConnection method
         * should be called subsequently to set up the schema.
         * Setting the initialization timeout to don't perform any
         */
        config.setInitializationFailTimeout(-1);
        config.setAutoCommit(true);
    }

    /**
     * Initializes the hikari connector with the given connection properties.
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

        LOGGER.info("Configuring data source.");
        if (java.sql.Driver.class.isAssignableFrom(clazz)) {
            if (jdbcUrl == null) {
                throw new IllegalArgumentException("jdbc url required");
            }

            try {
                config.setDriverClassName(clazz.getName());
                config.setJdbcUrl(jdbcUrl);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "Failed to configure data source.", ex);
                throw ex;
            }
        } else if (javax.sql.DataSource.class.isAssignableFrom(clazz)) {
            config.setDataSourceClassName(clazz.getName());
        } else {
            throw new IllegalArgumentException("unknown driver class");
        }

        if (properties != null) {
            config.setDataSourceProperties(properties);
        }

        config.setUsername(user);
        config.setPassword(password);
        configure(config);

        if (this.consumer != null) {
            try {
                this.consumer.accept(config);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "Failed custom data source configuration.", ex);
            }
        }

        LOGGER.info("Setting up data source.");
        try {
            this.source = new HikariDataSource(config);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Failed to set up data source.", ex);
            throw ex;
        }
    }

    @Override
    public void shutdown() {
        if (this.source != null && !this.source.isClosed()) {
            LOGGER.info("Shutting down data source.");
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

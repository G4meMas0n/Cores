package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * An abstract connector class for connecting to a local flat-file database.
 */
public abstract class FlatFileConnector implements Connector {

    private String jdbcUrl;
    private Properties properties;

    /**
     * The default constructor for the implementing a flat-file connector.
     */
    public FlatFileConnector() { }

    /**
     * Configures the given properties.
     * <p>
     * This method will be called during the {@link #initialize(String, java.util.Properties)} method and is intended
     * to be overwritten by the implementing subclass to configure the driver.
     * @param properties the properties to configure.
     */
    public void configure(@NotNull Properties properties) {

    }

    /**
     * Initializes the flat-file connector with the given connection properties.
     * @param jdbcUrl the jdbcUrl to use.
     * @param properties the properties to use.
     * @throws RuntimeException if the driver for the given jdbc-url is not available.
     */
    public void initialize(@NotNull String jdbcUrl, @Nullable Properties properties) {
        try {
            DriverManager.getDriver(jdbcUrl);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to find available driver.", ex);
            throw new RuntimeException("driver not available", ex);
        }

        this.jdbcUrl = jdbcUrl;
        this.properties = new Properties();

        configure(this.properties);
        if (properties != null) {
            properties.forEach((key, value) -> this.properties.setProperty(key.toString(), value.toString()));
        }
    }

    @Override
    public void shutdown() {
        this.jdbcUrl = null;
        this.properties = null;
    }

    @Override
    public @NotNull Connection getConnection() throws SQLException {
        if (this.jdbcUrl == null) {
            throw new SQLException("jdbc-url is null");
        }

        return DriverManager.getConnection(this.jdbcUrl, this.properties);
    }
}

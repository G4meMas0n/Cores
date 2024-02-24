package de.g4memas0n.core.database.connector;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * A connector class for connecting to a mysql database using the hikari data source.
 */
public class MySQLConnector extends HikariConnector {

    /**
     * The public default constructor for a mysql connector.
     */
    public MySQLConnector() {
        super();
    }

    /**
     * The public constructor for a mysql connector with a given custom configurator.
     * @param consumer a custom hikari configurator or null.
     */
    public MySQLConnector(@Nullable Consumer<HikariConfig> consumer) {
        super(consumer);
    }

    @Override
    public void configure(@NotNull HikariConfig config) {
        /*
         * Setting the recommended data source configuration for mysql as noted in:
         * https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
         */
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("maintainTimeStats=", false);

        super.configure(config);
    }

    @Override
    public void initialize(@Nullable Properties properties) {
        Preconditions.checkArgument(properties != null);
        Class<?> clazz;
        String jdbcUrl;

        try {
            clazz = Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            try {
                clazz = Class.forName("com.mysql.jdbc.Driver");
                LOGGER.warning("Failed to find modern mysql driver. Falling back to legacy driver.");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("driver not available");
            }
        }

        try {
            // setting up jdbc url
            String host = (String) properties.getOrDefault("host", "127.0.0.1");
            String port = (String) properties.getOrDefault("port", "3306");

            if (!properties.containsKey("database")) {
                throw new IllegalArgumentException("missing database property");
            }

            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + properties.getProperty("database");
        } catch (ClassCastException ex) {
            LOGGER.log(Level.SEVERE, "Failed initialization due to compromised properties", ex);
            throw new IllegalArgumentException("compromised properties");
        }

        initialize(clazz, jdbcUrl, null, properties.getProperty("user"), properties.getProperty("password"));
    }
}

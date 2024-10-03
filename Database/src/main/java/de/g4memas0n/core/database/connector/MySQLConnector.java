package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.util.Properties;

/**
 * A mysql database connector.
 * @see HikariConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class MySQLConnector extends HikariConnector {

    @Override
    public void configure(@NotNull Properties properties) {
        /*
         * Setup MySQL driver instead of data source as noted in:
         * https://github.com/brettwooldridge/HikariCP/tree/dev
         */
        if (properties.getProperty("driverClassName") == null) {
            Class<?> driver;
            try {
                driver = Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ignored) {
                try {
                    driver = Class.forName("com.mysql.jdbc.Driver");
                    logger.warning("Could not find modern mysql driver, falling back to legacy driver");
                } catch (ClassNotFoundException ex) {
                    logger.warning("Could not find any mysql driver");
                    throw new RuntimeException("driver not available", ex);
                }
            }

            properties.setProperty("driverClassName", driver.getName());
            properties.remove("dataSourceClassName");
        }

        // Setup jdbcUrl by using the data source properties if not already set
        if (properties.getProperty("jdbcUrl") == null) {
            String databaseName = properties.getProperty("databaseName");
            String serverName = properties.getProperty("serverName");
            String portNumber = properties.getProperty("portNumber");

            if (databaseName == null || serverName == null || portNumber == null) {
                throw new IllegalArgumentException("databaseName or serverName or portNumber is null");
            }

            properties.setProperty("jdbcUrl", "jdbc:mysql://" + serverName + ":" + portNumber + "/" + databaseName);
        }

        /*
         * Setting the recommended data source configuration for mysql as noted in:
         * https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
         */
        properties.setProperty("dataSource.cachePrepStmts", "true");
        properties.setProperty("dataSource.prepStmtCacheSize", "250");
        properties.setProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        properties.setProperty("dataSource.useServerPrepStmts", "true");
        properties.setProperty("dataSource.useLocalSessionState", "true");
        properties.setProperty("dataSource.rewriteBatchedStatements", "true");
        properties.setProperty("dataSource.cacheResultSetMetadata", "true");
        properties.setProperty("dataSource.cacheServerConfiguration", "true");
        properties.setProperty("dataSource.elideSetAutoCommits", "true");
        properties.setProperty("dataSource.maintainTimeStats", "false");

        super.configure(properties);
    }
}

package de.g4memas0n.core.sql.connector;

import de.g4memas0n.core.sql.StatementProcessor;
import org.jetbrains.annotations.NotNull;
import java.util.Properties;
import java.util.logging.Level;

/**
 * A MySQL database connector.
 * @see HikariConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class MySQLConnector extends HikariConnector {

    private StatementProcessor processor;

    @Override
    public @NotNull String getVendorName() {
        return "MySQL";
    }

    @Override
    public @NotNull StatementProcessor getStatementProcessor() {
        if (processor != null) return processor;
        return StatementProcessor.BACKTICK_PROCESSOR;
    }

    @Override
    public void setStatementProcessor(@NotNull StatementProcessor processor) {
        this.processor = processor.equals(StatementProcessor.BACKTICK_PROCESSOR) ? null : processor;
    }

    @Override
    public void configure(@NotNull Properties properties) {
        /*
         * Setup MySQL driver instead of data source as noted in:
         * https://github.com/brettwooldridge/HikariCP/tree/dev
         */
        Class<?> driver;
        try {
            driver = Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            try {
                driver = Class.forName("com.mysql.jdbc.Driver");
                logger.log(Level.WARNING, "Could not find modern MySQL driver, falling back to legacy driver", ex);
            } catch (ClassNotFoundException ex2) {
                logger.log(Level.WARNING, "Could not find MySQL driver", ex2);
                throw new RuntimeException("driver not available", ex);
            }
        }

        properties.remove("dataSourceClassName");
        properties.put("driverClassName", driver.getName());
        if (properties.containsKey("jdbcUrl")) {
            // Setup jdbcUrl by using the properties if not already set
            properties.put("jdbcUrl", createUrl(properties));
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

    /**
     * Creates a new jdbc-url for the specified database properties.
     * @param properties the database properties.
     * @return the created jdbc-url.
     */
    protected @NotNull String createUrl(@NotNull Properties properties) {
        String serverName = properties.getProperty("serverName");
        if (serverName == null || serverName.isEmpty()) {
            throw new IllegalArgumentException("Property 'serverName' must ne provided");
        }

        String databaseName = properties.getProperty("databaseName");
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalArgumentException("Property 'databaseName' must be provided");
        }

        String portNumber = properties.getProperty("portNumber");
        if (portNumber == null || portNumber.equals("0")) portNumber = "3306";
        return "jdbc:" + getVendorName().toLowerCase() + "://" + serverName + ":" + portNumber + "/" + databaseName;
    }
}

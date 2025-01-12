package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.util.Properties;

/**
 * A mariadb database connector.
 * @see HikariConnector
 * @see MySQLConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class MariaDBConnector extends MySQLConnector {

    @Override
    public @NotNull String getVendorName() {
        return "MariaDB";
    }

    @Override
    public void configure(@NotNull Properties properties) {
        Class<?> driver;
        try {
            driver = Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            logger.warning("Could not find mariadb driver");
            throw new RuntimeException("driver not available", ex);
        }

        properties.setProperty("driverClassName", driver.getName());
        properties.remove("dataSourceClassName");

        // Setup jdbcUrl by using the data source properties if not already set
        if (properties.getProperty("jdbcUrl") == null) {
            properties.setProperty("jdbcUrl", createUrl(properties));
        }
        super.configure(properties);
    }
}

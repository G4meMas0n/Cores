package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.util.Properties;
import java.util.logging.Level;

/**
 * A MariaDB database connector.
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
            logger.log(Level.WARNING, "Could not load MariaDB driver", ex);
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

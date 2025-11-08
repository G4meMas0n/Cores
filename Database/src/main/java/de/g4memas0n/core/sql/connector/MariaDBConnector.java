package de.g4memas0n.core.sql.connector;

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

        properties.remove("dataSourceClassName");
        properties.put("driverClassName", driver.getName());
        if (properties.containsKey("jdbcUrl")) {
            // Setup jdbcUrl by using the properties if not already set
            properties.put("jdbcUrl", createUrl(properties));
        }

        super.configure(properties);
    }
}

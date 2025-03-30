package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.util.Properties;
import java.util.logging.Level;

/**
 * A PostgreSQL database connector.
 * @see HikariConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class PostgreSQLConnector extends HikariConnector {

    @Override
    public @NotNull String getVendorName() {
        return "PostgreSQL";
    }

    @Override
    public void configure(@NotNull Properties properties) {
        Class<?> dataSource;
        try {
            dataSource = Class.forName("org.postgresql.ds.PGSimpleDataSource");
        } catch (ClassNotFoundException ex) {
            logger.log(Level.WARNING, "Could not find PostgreSQL driver", ex);
            throw new RuntimeException("driver not available", ex);
        }

        properties.setProperty("dataSourceClassName", dataSource.getName());
        properties.remove("driverClassName");
        super.configure(properties);
    }
}

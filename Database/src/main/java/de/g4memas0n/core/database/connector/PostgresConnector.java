package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.util.Properties;

/**
 * A postgres database connector.
 * @see HikariConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class PostgresConnector extends HikariConnector {

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
            logger.warning("Could not find postgres driver");
            throw new RuntimeException("driver not available", ex);
        }

        properties.setProperty("dataSourceClassName", dataSource.getName());
        properties.remove("driverClassName");
        super.configure(properties);
    }
}

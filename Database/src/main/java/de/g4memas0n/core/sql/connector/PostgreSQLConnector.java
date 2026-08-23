package de.g4memas0n.core.sql.connector;

import de.g4memas0n.core.sql.StatementProcessor;
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

    /**
     * The official vendor name of the PostgreSQL database connector.
     */
    public static final String VENDOR_NAME = "PostgreSQL";
    private StatementProcessor processor;

    @Override
    public @NotNull String getVendorName() {
        return VENDOR_NAME;
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

        properties.remove("driverClassName");
        properties.put("dataSourceClassName", dataSource.getName());
        super.configure(properties);
    }

    @Override
    public @NotNull StatementProcessor getStatementProcessor() {
        if (processor != null) return processor;
        return StatementProcessor.QUOTE_PROCESSOR;
    }

    @Override
    public void setStatementProcessor(@NotNull StatementProcessor processor) {
        this.processor = processor.equals(StatementProcessor.QUOTE_PROCESSOR) ? null : processor;
    }
}

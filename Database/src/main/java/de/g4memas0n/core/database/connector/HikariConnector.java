package de.g4memas0n.core.database.connector;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.PropertyElf;
import org.jetbrains.annotations.NotNull;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A database connector that connects through HikariCP.
 * @see Connector
 */
@SuppressWarnings("unused")
public abstract class HikariConnector implements Connector {

    /**
     * The logger instance intended to be used by all implemented hikari connectors.
     */
    public static Logger logger = Logger.getLogger(HikariConnector.class.getName());
    private HikariDataSource dataSource;

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public boolean isShutdown() {
        return dataSource == null || dataSource.isClosed();
    }

    @Override
    public void configure(@NotNull Properties properties) {
        HikariConfig config = new HikariConfig();
        Properties configProperties = new Properties();

        Set<String> propertyNames = PropertyElf.getPropertyNames(config.getClass());
        properties.forEach((key, value) -> {
            String propertyName = key.toString();
            if (!propertyName.startsWith("dataSource.")) {
                if (propertyNames.contains(propertyName)) {
                    // The config has this property
                    configProperties.put(key, value);
                } else {
                    // The config does not have this property, assume it belongs to the data source
                    configProperties.put("dataSource." + key, value);
                }
            }
        });

        PropertyElf.setTargetFromProperties(config, configProperties);
        config.setAutoCommit(true);
        dataSource = new HikariDataSource(config);
    }

    @Override
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public @NotNull Connection getConnection() throws SQLException {
        if (isShutdown()) {
            throw new SQLException("Datasource not configured or shut down");
        }
        return dataSource.getConnection();
    }

    @Override
    public boolean isWrapperFor(@NotNull Class<?> type) {
        return Connector.class.equals(type) || DataSource.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(@NotNull Class<T> type) throws SQLException {
        if (Connector.class.equals(type)) {
            return (T) this;
        } else if (HikariDataSource.class.isAssignableFrom(type) || DataSource.class.isAssignableFrom(type)) {
            return (T) dataSource;
        }
        throw new SQLException("Cannot unwrap to " + type);
    }
}

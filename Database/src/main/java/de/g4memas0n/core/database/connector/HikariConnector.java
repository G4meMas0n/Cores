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
 * @see IConnector
 */
@SuppressWarnings("unused")
public class HikariConnector implements IConnector {

    public static Logger logger = Logger.getLogger(HikariConnector.class.getName());
    private HikariDataSource dataSource;

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
        if (dataSource == null) {
            throw new SQLException("Database connection unavailable");
        }
        return dataSource.getConnection();
    }

    @Override
    public boolean isWrapperFor(@NotNull Class<?> type) {
        return IConnector.class.equals(type) || DataSource.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(@NotNull Class<T> type) throws SQLException {
        if (IConnector.class.equals(type)) {
            return (T) this;
        } else if (HikariDataSource.class.isAssignableFrom(type) || DataSource.class.isAssignableFrom(type)) {
            return (T) dataSource;
        }
        throw new SQLException("Cannot unwrap to " + type);
    }
}

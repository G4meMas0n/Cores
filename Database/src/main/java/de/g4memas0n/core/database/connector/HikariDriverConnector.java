package de.g4memas0n.core.database.connector;

import com.zaxxer.hikari.HikariConfig;
import de.g4memas0n.core.database.DatabaseManager;
import de.g4memas0n.core.database.driver.Driver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * A hikari connector that uses the {@link Driver} class to connect to a database using the hikari connector.
 */
public class HikariDriverConnector extends HikariConnector {

    private Driver driver;

    /**
     * Constructs a new hikari connector for the given {@link Driver driver}.
     * @param driver the driver to use or null.
     */
    public HikariDriverConnector(@Nullable Driver driver) {
        this(driver, null);
    }

    /**
     * Constructs a new hikari connector for the given {@link Driver driver}.
     * @param driver the driver to use or null.
     * @param consumer a custom hikari configurator or null.
     */
    public HikariDriverConnector(@Nullable Driver driver, @Nullable Consumer<HikariConfig> consumer) {
        super(consumer);
        this.driver = driver;
    }

    @Override
    public void initialize(@Nullable Properties settings) {
        if (this.driver == null) {
            throw new IllegalStateException("driver not available");
        }

        initialize(settings, this.driver);
    }

    /**
     * Initializes the hikari connector for the given driver with the given connection settings.
     * @param settings the connection settings.
     * @param driver the driver to use.
     */
    public void initialize(@Nullable Properties settings, @NotNull Driver driver) {
        Class<?> clazz;

        try {
            clazz = driver.getDriverClass();
        } catch (RuntimeException ex) {
            DatabaseManager.getLogger().log(Level.SEVERE, "driver class not found", ex);
            throw ex;
        }

        if (settings != null) {
            String user = settings.getProperty("user");
            String password = settings.getProperty("password");

            initialize(clazz, driver.getJdbcUrl(settings), driver.getProperties(settings), user, password);
        } else {
            initialize(clazz, driver.getJdbcUrl(), driver.getProperties(), null, null);
        }

        setDriver(driver);
    }

    /**
     * Returns the used {@link Driver driver} object for the hikari connector.
     * @return the database driver.
     */
    public @NotNull Driver getDriver() {
        if (this.driver == null) {
            throw new IllegalStateException("driver not available");
        }

        return this.driver;
    }

    /**
     * Sets the used {@link Driver driver} object for the hikari connector.
     * @param driver the new database driver.
     */
    public void setDriver(@NotNull Driver driver) {
        if (!driver.equals(this.driver)) {
            this.driver = driver;
        }
    }
}

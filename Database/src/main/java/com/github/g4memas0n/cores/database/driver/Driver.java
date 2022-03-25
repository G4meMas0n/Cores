package com.github.g4memas0n.cores.database.driver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.Properties;

/**
 * A class containing all required driver information for a database driver.
 *
 * @since 1.0.0
 */
public class Driver {

    private final String type;
    private final String clazz;
    private final String url;
    private final String queries;

    private Properties properties;

    /**
     * Public constructor for creating a driver information.
     *
     * @param type the type of the database driver like 'MySQL' and/or 'SQLite', etc...
     * @param clazz the driver class implementing either {@link java.sql.Driver} or {@link javax.sql.DataSource}.
     * @param url the jdbc url for the driver class or null if the driver implements {@link javax.sql.DataSource}.
     * @param queries the path to the file containing sql queries or null.
     */
    public Driver(@NotNull final String type, @NotNull final String clazz,
                  @Nullable final String url, @Nullable final String queries) {
        this.type = type;
        this.clazz = clazz;
        this.url = url;
        this.queries = queries;
        this.properties = null;
    }

    /**
     * Returns the database type for that this driver information is.
     *
     * @return the database type of the driver information.
     */
    public @NotNull String getType() {
        return this.type;
    }

    /**
     * Returns the full class name of this driver information that either implements the {@link java.sql.Driver}
     * interface or the {@link javax.sql.DataSource} interface.
     *
     * @return the full name of the driver class.
     */
    public @NotNull String getClassName() {
        return this.clazz;
    }

    /**
     * Returns the jdbc url of this driver information.<br>
     * May be null if the class specified by this driver information implements {@link javax.sql.DataSource} instead
     * of {@link java.sql.Driver}.
     *
     * @return the jdbc url for the driver class.
     */
    public @Nullable String getJdbcUrl() {
        return this.url;
    }

    /**
     * Returns the path to a file containing sql queries or null if it is not specified.
     *
     * @return the path to a queries file.
     */
    public @Nullable String getQueries() {
        return this.queries;
    }

    /**
     * Returns the properties for the driver class if it implements {@link javax.sql.DataSource}.<br>
     * This means that this may be null if the class implements {@link java.sql.Driver} instead of
     * {@link javax.sql.DataSource}.
     *
     * @return the properties for the data source driver.
     */
    public @Nullable Properties getProperties() {
        return this.properties;
    }

    /**
     * Sets the properties for the driver class that will be used if the driver class implements
     * {@link javax.sql.DataSource}.
     *
     * @param properties the new properties for the data source driver.
     */
    public void setProperties(@Nullable final Properties properties) {
        this.properties = properties;
    }

    @Override
    public @NotNull String toString() {
        return "Driver{type='" + this.type + "', clazz='" + this.clazz + "', url='" + this.url
                + "', queries='" + this.queries + "', properties=" + this.properties + "}";
    }

    @Override
    public boolean equals(@Nullable final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || this.getClass()!= object.getClass()) {
            return false;
        }

        final Driver driver = (Driver) object;
        return this.type.equals(driver.type) && this.clazz.equals(driver.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.clazz);
    }
}

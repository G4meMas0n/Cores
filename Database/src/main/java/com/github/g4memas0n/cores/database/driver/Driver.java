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

    private final Class<?> source;
    private final String type;
    private final String url;

    private Properties properties;
    private String queries;

    /**
     * Public constructor for creating a driver representation.
     *
     * @param source the driver class implementing {@link java.sql.Driver} or {@link javax.sql.DataSource}.
     * @param type the type of the database driver like 'MySQL' and/or 'SQLite', etc...
     * @param url the jdbc url for the driver class or null if not required.
     */
    public Driver(@NotNull final Class<?> source, @NotNull final String type, @Nullable final String url) {
        this.source = source;
        this.type = type;
        this.url = url;
    }

    /**
     * Public constructor for creating a driver representation.
     *
     * @param clazz the name of a driver class implementing {@link java.sql.Driver} or {@link javax.sql.DataSource}.
     * @param type the type of the database driver like 'MySQL' and/or 'SQLite', etc...
     * @param url the jdbc url for the driver class or null if not required.
     */
    public Driver(@NotNull final String clazz, @NotNull final String type, @Nullable final String url) {
        try {
            this.source = Class.forName(clazz);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("class not found");
        }

        this.type = type;
        this.url = url;
    }

    /**
     * Returns the source class of this driver representation.<br>
     * This class normally implements the {@link java.sql.Driver} or {@link javax.sql.DataSource} interface.
     *
     * @return the source class of the driver.
     */
    public @NotNull Class<?> getSource() {
        return this.source;
    }

    /**
     * Returns the database type that this driver representation is for.
     *
     * @return the database type of the driver.
     */
    public @NotNull String getType() {
        return this.type;
    }

    /**
     * Returns the jdbc url of this driver representation.<br>
     * May be null if the source class specified by this driver implements {@link javax.sql.DataSource}.
     *
     * @return the jdbc url for the driver class.
     */
    public @Nullable String getJdbcUrl() {
        return this.url;
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
     * Sets or removes the properties for the driver class that will be used if the driver class implements
     * {@link javax.sql.DataSource}.
     *
     * @param properties the new properties for the data source driver or null.
     */
    public void setProperties(@Nullable final Properties properties) {
        this.properties = properties;
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
     * Sets or removes the path to a file containing sql queries.
     *
     * @param queries the new path to a queries file or null.
     */
    public void setQueries(@Nullable final String queries) {
        this.queries = queries;
    }

    @Override
    public @NotNull String toString() {
        return "Driver{source='" + this.source + "', type='" + this.type + "', url='" + this.url + "'}";
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
        return this.source.equals(driver.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.source);
    }
}

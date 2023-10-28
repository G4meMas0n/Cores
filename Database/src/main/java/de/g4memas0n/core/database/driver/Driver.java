package de.g4memas0n.core.database.driver;

import de.g4memas0n.core.database.DatabaseManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class containing all information for a database driver, like the driver class and the jdbc-url.
 */
public class Driver {

    private final String className;
    private final Vendor vendor;

    private Properties properties;
    private Class<?> clazz;
    private String jdbcUrl;

    /**
     * Constructs a new driver class with the given driver class name and vendor.
     * @param className the full name of the driver class.
     * @param vendor the vendor for which the driver is intended.
     */
    public Driver(@NotNull String className, @NotNull Vendor vendor) {
        this.className = className;
        this.vendor = vendor;
    }

    /**
     * Returns the drivers class if available to the class loader.
     * @return tne drivers class.
     * @throws ClassNotFoundException if the class is not available to the class loader.
     */
    public @NotNull Class<?> getDriverClass() throws ClassNotFoundException {
        if (this.clazz == null) {
            try {
                return this.clazz = Class.forName(this.className);
            } catch (ClassNotFoundException ex) {
                DatabaseManager.getLogger().log(Level.SEVERE, "driver class not found", ex);
                throw ex;
            }
        }

        return this.clazz;
    }

    /**
     * Returns the vendor for which the driver is intended.
     * @return the drivers vendor.
     */
    public @NotNull Vendor getVendor() {
        return this.vendor;
    }

    /*
     *
     */

    /**
     * Applies the given settings to the jdbc-url of the driver and returns it.
     * <p>
     * This method will search for placeholders like {@literal {<key>}} in the jdbc-url and replaces them with their
     * corresponding property values in the given settings.
     * <p>
     * Note: The returned value may be null if the jdbc-url was not set before.
     * @param settings the settings to use for the jdbc-url.
     * @return the updated jdbc-url for the given settings or null.
     */
    public @Nullable String getJdbcUrl(@NotNull Properties settings) {
        String url = this.jdbcUrl;

        if (url != null) {
            Matcher matcher = Pattern.compile("\\{(?<key>[a-zA-Z]+)\\}").matcher(url);
            String value;

            while (matcher.find()) {
                value = settings.getProperty(matcher.group("key"));
                url = url.replace(matcher.group(), Objects.requireNonNullElse(value, ""));
            }
        }

        return url;
    }

    /**
     * Returns the jdbc-url of the driver, if available.
     * <p>
     * Note: The returned value may be null if the jdbc-url was not set before.
     * @return the jdbc-url for the driver or null.
     */
    public @Nullable String getJdbcUrl() {
        return this.jdbcUrl;
    }

    /**
     * Sets or removes the jdbc-url of the driver.
     * @param jdbcUrl the new jdbc-url or null.
     */
    public void setJdbcUrl(@Nullable String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /*
     *
     */

    /**
     * Applies the given settings to the properties for the driver and returns the updated properties.
     * <p>
     * This method will search for placeholders like {@literal {<key>}} in the properties and replaces them with their
     * corresponding property values in the given settings.
     * <p>
     * Note: The returned value may be null if the properties have not been set before.
     * @param settings the settings to use for the properties.
     * @return the updated properties for the given settings or null.
     */
    public @Nullable Properties getProperties(@Nullable Properties settings) {
        if (this.properties == null) {
            return null;
        }

        // Copying properties so that the original properties are not changed.
        Properties result = new Properties();

        if (settings != null) {
            Pattern pattern = Pattern.compile("\\{(?<key>[a-zA-Z]+)\\}");
            String property, value;
            Matcher matcher;

            for (Map.Entry<Object, Object> entry : this.properties.entrySet()) {
                matcher = pattern.matcher(property = entry.getValue().toString());

                while (matcher.find()) {
                    value = settings.getProperty(matcher.group("key"));
                    property = property.replace(matcher.group(), Objects.requireNonNullElse(value, ""));
                }

                result.setProperty(entry.getKey().toString(), property);
            }
        }

        return result;
    }

    /**
     * Returns the properties for the driver, if available.
     * <p>
     * Note: The returned value may be null if the properties have not been set before.
     * @return the properties for the driver or null.
     */
    public @Nullable Properties getProperties() {
        return this.properties;
    }

    /**
     * Sets or removes the properties for the driver.
     * @param properties the new properties or null.
     */
    public void setProperties(@Nullable Properties properties) {
        this.properties = properties;
    }

    /*
     *
     */

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }

        if (object == null || object.getClass() != getClass()) {
            return false;
        }

        Driver other = (Driver) object;
        return Objects.equals(this.vendor, other.vendor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vendor);
    }

    /**
     * A class containing the information of a driver implementation vendor.
     */
    public static class Vendor {

        private final String name;
        private final int version;

        /**
         * Constructs a new driver implementation vendor with the given name.
         * @param name the name of the vendor.
         */
        public Vendor(@NotNull String name) {
            this(name, -1);
        }

        /**
         * Constructs a new driver implementation vendor with the given name and version.
         * @param name the name of the vendor.
         * @param version the version of the vendor.
         */
        public Vendor(@NotNull String name, int version) {
            this.name = name;
            this.version = version;
        }

        /**
         * Returns the name of the drivers implementation vendor.
         * @return the vendor name.
         */
        public @NotNull String getName() {
            return this.name;
        }

        /**
         * Returns whether the drivers implementation vendor has an associated version.
         * @return true if it has a version, false otherwise.
         */
        public boolean hasVersion() {
            return this.version > 0;
        }

        /**
         * Returns the version of the drivers implementation vendor.
         * @return the vendor version.
         */
        public int getVersion() {
            return this.version;
        }

        @Override
        public boolean equals(@Nullable Object object) {
            if (object == this) {
                return true;
            }

            if (object == null || object.getClass() != getClass()) {
                return false;
            }

            Vendor other = (Vendor) object;
            return Objects.equals(this.name, other.name) && this.version == other.version;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.version);
        }
    }
}

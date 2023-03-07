package com.github.g4memas0n.cores.database;

import org.jetbrains.annotations.NotNull;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * A placeholder enumeration for all the available placeholders in the database connection settings.
 *
 * @since 1.0.0
 */
public enum Placeholder {

    /**
     * Represents the database name placeholder.
     */
    DATABASE("databaseName"),

    /**
     * Represents the server name/host placeholder.
     */
    HOST("serverName"),

    /**
     * Represents the server port placeholder.
     */
    PORT("portNumber"),

    /**
     * Represents the username placeholder.
     */
    USERNAME(),

    /**
     * Represents the password placeholder.
     */
    PASSWORD(),

    /**
     * Represents the path placeholder.
     */
    PATH();

    private final String property;

    Placeholder() {
        this.property = null;
    }

    Placeholder(@NotNull final String property) {
        this.property = property;
    }

    /**
     * Returns the property key for this {@code Placeholder}.
     * @return the property key representation.
     */
    public @NotNull String getKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the actual placeholder string for this {@code Placeholder} that will be replaced if found.
     * @return the placeholder string representation.
     */
    public @NotNull String getPlaceholder() {
        return "{" + getKey() + "}";
    }

    /**
     * Returns the property key for the data-source properties, if it has one. Otherwise, an exception will be thrown.<br>
     * It might be useful to check if this {@code Placeholder} has a data-source property via the {@link #hasProperty()}
     * method.
     * @return the property key for data-source properties.
     * @throws NoSuchElementException if this {@code Placeholder} has no data-source property.
     * @see #hasProperty()
     */
    public @NotNull String getProperty() {
        if (this.property == null) {
            throw new NoSuchElementException("no property field set");
        }

        return this.property;
    }

    /**
     * Checks whether this {@code Placeholder} has a data-source property.
     * @return {@code true}, if it has a data-source property. {@code false}, otherwise.
     */
    public boolean hasProperty() {
        return this.property != null;
    }
}

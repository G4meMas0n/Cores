package com.github.g4memas0n.cores.database.driver;

import org.jetbrains.annotations.NotNull;

/**
 * A loader for files, that contains driver information for sql databases.<br>
 * The loading and parsing of the file and of the drivers will be handled by the implementing class.
 *
 * @since 1.0.0
 */
public abstract class DriverLoader {

    /**
     * Protected default constructor for the implementing driver loader classes.
     */
    protected DriverLoader() { }

    /**
     * Loads all drivers from the driver file regardless of the database type.<
     * The array returned by this method may be empty if no driver could be loaded successfully.
     * @return an array containing all loaded drivers.
     */
    public abstract Driver[] loadAll();

    /**
     * Loads all drivers from the driver file that matches the given database {@code type}.
     * The array returned by this method may be empty if no matching driver has been found or no found driver could be
     * loaded successfully.
     * @param type the driver type that the drivers must match.
     * @return an array containing the all loaded drivers for the given database {@code type}.
     */
    public abstract Driver[] load(@NotNull final String type);

}

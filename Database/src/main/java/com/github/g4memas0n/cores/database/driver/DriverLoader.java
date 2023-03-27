package com.github.g4memas0n.cores.database.driver;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
     * Loads the file located at the given {@code path} from the {@link ClassLoader} and parses it as driver file
     * according to the implementing driver loader.
     *
     * @param path the path of a file containing the drivers.
     * @throws IllegalArgumentException if the file could not be found or contains illegal elements.
     * @throws IOException if the file could not be read or parsed.
     * @see #load(InputStream) 
     */
    public abstract void load(@NotNull final String path) throws IOException;

    /**
     * Reads the driver file from the given {@code stream} and parses it according to the implementing loader.
     *
     * @param stream the input stream to read from.
     * @throws IllegalArgumentException if the file contains illegal elements.
     * @throws IOException if the stream could not be read or parsed.
     * @see #load(String) 
     */
    public abstract void load(@NotNull final InputStream stream) throws IOException;

    /**
     * Loads all drivers from the loaded driver file regardless of the database type.<br>
     * The list returned by this method may be empty if no driver could be loaded successfully.
     *
     * @return a list containing the all loaded drivers.
     */
    public abstract @NotNull List<Driver> loadDrivers();

    /**
     * Loads all drivers from the loaded driver file that matches the given database {@code type}.<br>
     * The list returned by this method may be empty if no matching driver has been found or no found driver could be
     * loaded successfully.
     *
     * @param type the driver type that the drivers must match.
     * @return a list containing the all loaded drivers for the given database {@code type}.
     */
    public abstract @NotNull List<Driver> loadDrivers(@NotNull final String type);

    /**
     * Creates a new driver loader for the driver file with the given {@code name} from the {@link ClassLoader} and
     * loads and parses the file.
     * <p><i>Currently only json files are supported as driver files.</i></p>
     *
     * @param name the name of the driver file with or without the extension.
     * @return the driver loader for the driver file.
     * @throws IllegalArgumentException if no file could be found or the file contains illegal elements.
     * @throws IOException if the file could not be read or parsed.
     */
    public static @NotNull DriverLoader getLoader(@NotNull final String name) throws IOException {
        DriverLoader loader;
        String path = name;

        if (!name.contains(".") || !name.substring(name.lastIndexOf(".")).equalsIgnoreCase(".json")) {
            path += ".json";
        }

        loader = new JsonDriverLoader();
        loader.load(path);
        return loader;
    }
}

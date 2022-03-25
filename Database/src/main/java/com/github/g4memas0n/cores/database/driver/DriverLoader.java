package com.github.g4memas0n.cores.database.driver;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A loader for files, that contains driver information for sql databases.<br>
 * The loading of the file and of the drivers will be handled by the implementing class.
 *
 * @since 1.0.0
 */
public abstract class DriverLoader {

    /**
     * The reference to the location of the file that is currently loaded.<br>
     * Must be set by the implementing class after the successful load of a file.
     */
    protected String path;

    /**
     * Protected default constructor for the implementing driver loader classes.
     */
    protected DriverLoader() { }

    /**
     * Loads the file located at the given {@code path} from the {@link ClassLoader} and parses it as driver file
     * according to the implementing driver loader.
     *
     * @param path the path of a file containing the driver information.
     * @throws IllegalArgumentException if the file located at the given {@code path} could not be found.
     * @throws IOException if the file could not be read or parsed.
     */
    public abstract void load(@NotNull final String path) throws IOException;

    /**
     * Loads all drivers from the loaded driver file regardless of the database type.<br>
     * The list returned by this method cannot be empty, as empty driver files are not allowed.
     *
     * @return a list containing the all loaded drivers.
     */
    public abstract @NotNull List<Driver> loadDrivers();

    /**
     * Loads all drivers from the loaded driver file that matches with the given database {@code type}.<br>
     * The list returned by this method may be empty if no matching driver has been found.
     *
     * @param type the driver type that the drivers must match.
     * @return a list containing the all loaded drivers for the given database {@code type}.
     */
    public abstract @NotNull List<Driver> loadDrivers(@NotNull final String type);

    /**
     * Creates a new driver loader and loads the file located at the given {@code path} from the {@link ClassLoader}.
     * This method will check the file extension and will create a query loader according to it.
     *
     * @param path the path of a file containing the driver information.
     * @return the newly created driver loader that has already loaded the file.
     * @throws IllegalArgumentException if the file located at the given {@code path} could not be found or could not
     * be loaded.
     */
    public static @NotNull DriverLoader loadFile(@NotNull final String path) {
        Preconditions.checkArgument(!path.isBlank(), "Path cannot be blank");
        final String lowered = path.toLowerCase(Locale.ROOT);

        if (lowered.endsWith(".json")) {
            final DriverLoader loader = new JsonDriverLoader();

            try {
                loader.load(path);

                return loader;
            } catch (IOException ex) {
                throw new IllegalArgumentException("Driver file at given path cannot be parsed", ex);
            }
        }

        throw new IllegalArgumentException("Missing or unsupported driver file extension");
    }
}

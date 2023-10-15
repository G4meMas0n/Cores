package de.g4memas0n.core.database.driver;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract reader class for reading driver files for database drivers.
 */
public abstract class DriverReader {

    /**
     * Reads all drivers from the given stream.
     * @param stream the stream to read from.
     * @return a list of all read drivers.
     * @throws IOException if an I/O error occurs.
     */
    public abstract @NotNull List<Driver> readAll(@NotNull InputStream stream) throws IOException;

    /**
     * Reads all drivers from the given reader.
     * @param reader the reader to read from.
     * @return a list of all read drivers.
     * @throws IOException if an I/O error occurs.
     */
    public abstract @NotNull List<Driver> readAll(@NotNull Reader reader) throws IOException;

    /**
     * Reads all drivers for the given vendor name from the given stream.
     * @param stream the stream to read from.
     * @param vendorName the vendor name to search drivers for.
     * @return a list of all read drivers for the given vendor name.
     * @throws IOException if an I/O error occurs.
     */
    public @NotNull List<Driver> read(@NotNull InputStream stream, @NotNull String vendorName) throws IOException {
        return filter(readAll(stream), vendorName);
    }

    /**
     * Reads all drivers for the given vendor name from the given reader.
     * @param reader the reader to read from.
     * @param vendorName the vendor name to search drivers for.
     * @return a list of all read drivers for the given vendor name.
     * @throws IOException if an I/O error occurs.
     */
    public @NotNull List<Driver> read(@NotNull Reader reader, @NotNull String vendorName) throws IOException {
        return filter(readAll(reader), vendorName);
    }

    private @NotNull List<Driver> filter(@NotNull List<Driver> drivers, @NotNull String vendorName) {
        List<Driver> result = new ArrayList<>();

        for (Driver driver : drivers) {
            if (vendorName.equals(driver.getVendor().getName())) {
                result.add(driver);
            }
        }

        return result;
    }
}

package de.g4memas0n.core.database.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/**
 * A loader class for queries files, containing query statements.
 */
public class QueryReader {

    /**
     * Reads the queries properties file for the given basename.
     * @param basename the basename of the queries file.
     * @return the properties containing all read query statements.
     * @throws IllegalArgumentException if the file is not visible to the class loader.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull Properties getQueries(@NotNull String basename) throws IOException {
        return getQueries(basename, (Properties) null);
    }

    /**
     * Reads the queries properties file for the given basename and with the given default properties.
     * @param basename the basename of the properties file.
     * @param defaults the default properties or null.
     * @return the properties with all read query statements.
     * @throws IllegalArgumentException if the file is not visible to the class loader.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull Properties getQueries(@NotNull String basename, @Nullable Properties defaults) throws IOException {
        String path = basename.toLowerCase(Locale.ROOT) + ".properties";

        try (InputStream stream = QueryReader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("file not found");
            }

            Properties properties = defaults != null ? new Properties(defaults) : new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return properties;
        }
    }

    /**
     * Reads the queries properties file for the given basename and vendor.
     * @param basename the basename of the queries file.
     * @param vendor the vendor for which the queries file is desired.
     * @return the properties containing read query statements.
     * @throws IllegalArgumentException if the file is not visible to the class loader.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull Properties getQueries(@NotNull String basename, @NotNull String vendor) throws IOException {
        StringBuilder builder = new StringBuilder(basename);
        Properties temp, properties = null;

        do {
            try {
                temp = getQueries(builder.toString(), properties);
                properties = temp;
            } catch (IllegalArgumentException ignored) {

            }

            if (builder.indexOf("_") < 0) {
                builder.append("_").append(vendor);
                continue;
            }

            builder.setLength(0);
        } while (builder.length() > 0);

        if (properties == null) {
            throw new IllegalArgumentException("files not found");
        }

        return properties;
    }
}

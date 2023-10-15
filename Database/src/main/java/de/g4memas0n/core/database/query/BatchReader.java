package de.g4memas0n.core.database.query;

import de.g4memas0n.core.database.driver.Driver.Vendor;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * A loader class for batch sql files, containing batch statements.
 */
public class BatchReader {

    /**
     * Reads the batch file from the given stream.
     * @param stream the stream to read from.
     * @return a list of all read batch statements.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull List<String> getBatch(@NotNull InputStream stream) throws IOException {
        List<String> statements = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder query = new StringBuilder();
            String line;
            int index;

            while ((line = reader.readLine()) != null) {
                // Check for comments in the current line
                if ((index = line.indexOf("--")) >= 0) {
                    if (index == 0) {
                        continue;
                    }

                    line = line.substring(0, index).trim();
                }

                // Check for ending statements
                while ((index = line.indexOf(";")) >= 0) {
                    query.append(line.substring(0, index).stripLeading());

                    if (query.length() > 0) {
                        statements.add(query.toString());
                        query.setLength(0);
                    }

                    line = line.substring(index + 1);
                }

                query.append(line.trim());
            }
        }

        return statements;
    }

    /**
     * Reads the batch file for the given basename.
     * @param basename the basename of the batch file.
     * @return a list of all read batch statements.
     * @throws IllegalArgumentException if the file is not visible to the class loader.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull List<String> getBatch(@NotNull String basename) throws IOException {
        String path = basename.toLowerCase(Locale.ROOT) + ".sql";

        try (InputStream stream = BatchReader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("file not found");
            }

            return getBatch(stream);
        }
    }

    /**
     * Reads the batch file for the given basename and vendor.
     * @param basename the basename of the batch file.
     * @param vendor the vendor for which the batch file is desired.
     * @return a list of all read batch statements.
     * @throws IllegalArgumentException if the file is not visible to the class loader.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull List<String> getBatch(@NotNull String basename, @NotNull Vendor vendor) throws IOException {
        StringBuilder builder = new StringBuilder(basename).append("_").append(vendor.getName());
        int index;

        if (vendor.hasVersion()) {
            builder.append("-").append(vendor.getVersion());
        }

        do {
            try {
                return getBatch(builder.toString());
            } catch (IllegalArgumentException ignored) {
                // search for parent batch files
                if (vendor.hasVersion()) {
                    index = builder.indexOf(Integer.toString(vendor.getVersion()));

                    if (index > 0) {
                        builder.delete(index - 1, builder.length());
                        continue;
                    }
                }

                index = builder.indexOf(vendor.getName());
                builder.delete(Math.max(index - 1, 0), builder.length());
            }
        } while (builder.length() > 0);

        // no batch could be found/loaded
        throw new IllegalArgumentException("missing or unsupported files");
    }
}

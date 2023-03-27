package com.github.g4memas0n.cores.database.query;

import com.github.g4memas0n.cores.database.driver.Driver;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * A loader class for sql files, containing batch statements.
 *
 * @since 1.0.0
 */
public class BatchLoader {

    /**
     * Loads and parses the sql batch file with the given {@code name} from the {@link ClassLoader} and adds the batch
     * statements to the given {@link Statement}.
     *
     * @param name the name of the batch file with or without the extension.
     * @param statement the statement to add the batch statements to.
     * @throws IllegalArgumentException if no batch file could not be found.
     * @throws IOException if the file could not be read or parsed.
     * @throws SQLException if a database error occurs while adding the batch statements.
     * @see #getBatch(String, Driver, Statement)
     */
    public static void getBatch(@NotNull final String name, @NotNull final Statement statement) throws IOException, SQLException {
        String path = name;

        if (!name.contains(".") || !name.substring(name.lastIndexOf(".")).equalsIgnoreCase(".sql")) {
            path += ".sql";
        }

        try (InputStream stream = BatchLoader.class.getClassLoader().getResourceAsStream(path)) {
            Preconditions.checkArgument(stream != null, "missing file at " + path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder query = new StringBuilder();
            String line;
            int index;

            while ((line = reader.readLine()) != null) {
                // Check for comments in the current line
                if ((index = line.indexOf("#")) >= 0) {
                    if (index == 0) {
                        continue;
                    }

                    line = line.substring(0, index).stripTrailing();
                }

                // Check for ending statements
                while ((index = line.indexOf(";")) >= 0) {
                    query.append(line.substring(0, index).stripLeading());
                    statement.addBatch(query.toString());
                    query.setLength(0);

                    line = line.substring(index + 1);
                }

                query.append(line.stripLeading());
            }
        }
    }

    /**
     * Loads and parses the sql batch file for the given {@code driver} with the given {@code basename} from the
     * {@link ClassLoader} and adds the batch statements to the given {@link Statement}.
     *
     * @param base the basename of the batch file without the extension.
     * @param driver the driver to load the batch file for.
     * @param statement the statement to add the batch statements to.
     * @throws IllegalArgumentException if no batch file could not be found.
     * @throws IOException if the file could not be read or parsed.
     * @throws SQLException if a database error occurs while adding the batch statements.
     * @see #getBatch(String, Statement)
     */
    public static void getBatch(@NotNull final String base, @NotNull final Driver driver,
                                @NotNull final Statement statement) throws IOException, SQLException {
        StringBuilder builder = new StringBuilder(base);
        builder.append("_").append(driver.getType());
        int index;

        if (driver.getVersion() != null) {
            builder.append("-").append(driver.getVersion());
        }

        do {
            try {
                getBatch(builder.toString().toLowerCase(Locale.ROOT), statement);
                return; // return immediately if batch loaded successfully
            } catch (IllegalArgumentException ignored) {

            }

            if (driver.getVersion() != null) {
                index = builder.indexOf(driver.getVersion());

                if (index > 0) {
                    builder.delete(index - 1, builder.length());
                    continue;
                }
            }

            index = builder.indexOf(driver.getType());
            builder.delete(Math.max(index - 1, 0), builder.length());
        } while (builder.length() > 0);

        // no batch could be found/loaded
        throw new IllegalArgumentException("missing batch files");
    }
}

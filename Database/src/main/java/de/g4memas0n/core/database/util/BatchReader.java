package de.g4memas0n.core.database.util;

import org.jetbrains.annotations.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

/**
 * A reader class for loading .sql batch files.
 */
@SuppressWarnings("unused")
public final class BatchReader {

    private BatchReader() {}

    /**
     * Reads the batch file for the given path and loads it to the given statement.
     * @param statement the statement to add the queries to.
     * @param path the path of the batch file.
     * @return the count of the batch statement.
     * @throws IllegalArgumentException if the file is not visible to the class loader.
     * @throws IOException if an I/O error occurs.
     * @throws SQLException if a database access error occurs.
     */
    public static int readBatch(@NotNull Statement statement, @NotNull String path) throws IOException, SQLException {
        String file = path + (path.matches("\\.sql$") ? "" : ".sql");
        try (InputStream stream = BatchReader.class.getClassLoader().getResourceAsStream(file)) {
            if (stream == null) {
                throw new IllegalArgumentException("file not found");
            }

            return readBatch(statement, stream);
        }
    }

    /**
     * Reads the batch file from the given stream and loads it to the given statement.
     * @param statement the statement to add the queries to.
     * @param stream the stream to read from.
     * @return the count of the batch statement.
     * @throws IOException if an I/O error occurs.
     * @throws SQLException if a database access error occurs.
     */
    public static int readBatch(@NotNull Statement statement, @NotNull InputStream stream) throws IOException, SQLException {
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder query = new StringBuilder();
            String line;
            int index;

            while ((line = reader.readLine()) != null) {
                // Check for comments in current line
                if ((index = line.indexOf("--")) >= 0) {
                    if (index == 0) {
                        continue;
                    }

                    line = line.substring(0, index).trim();
                }

                // Check for ending statements
                while ((index = line.indexOf(";")) >= 0) {
                    query.append(line.substring(0, index).stripLeading());
                    if (!query.isEmpty()) {
                        statement.addBatch(query.toString());
                        query.setLength(0);
                        count++;
                    }

                    line = line.substring(index + 1);
                }

                query.append(line.strip().replaceAll(" {2,}", " "));
            }
        } catch (SQLException ex) {
            if (count > 0) {
                statement.clearBatch();
            }

            throw ex;
        }

        return count;
    }

    /**
     * Reads the batch file for the given path.
     * @param path the path of the batch file.
     * @return a list of all batch statements.
     * @throws IllegalArgumentException if the file is not visible to the class loader.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull List<String> readBatch(@NotNull String path) throws IOException {
        String file = path + (path.matches("\\.sql$") ? "" : ".sql");
        try (InputStream stream = BatchReader.class.getClassLoader().getResourceAsStream(file)) {
            if (stream == null) {
                throw new IllegalArgumentException("file not found");
            }

            return readBatch(stream);
        }
    }

    /**
     * Reads the batch file from the given stream.
     * @param stream the stream to read from.
     * @return a list of all batch statements.
     * @throws IOException if an I/O error occurs.
     */
    public static @NotNull List<String> readBatch(@NotNull InputStream stream) throws IOException {
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
                    if (!query.isEmpty()) {
                        statements.add(query.toString());
                        query.setLength(0);
                    }

                    line = line.substring(index + 1);
                }

                query.append(line.strip().replaceAll(" {2,}", " "));
            }
        }

        return statements;
    }
}

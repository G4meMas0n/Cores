package com.github.g4memas0n.cores.database.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * An implementing query loader that loads the mapping from a property file.
 *
 * @since 1.0.0
 */
public class PropertyQueryLoader extends QueryLoader {

    private final Properties properties;

    /**
     * Creates a new property query loader from a {@link Reader}.
     * Unlike the constructor {@link #PropertyQueryLoader(InputStream)}, there is no limitation as to the encoding of
     * the input property file.
     * @param reader a reader that represents a property file to read from.
     * @throws IllegalArgumentException if a malformed Unicode escape sequence appears from reader.
     * @throws IOException if an I/O error occurs.
     */
    public PropertyQueryLoader(@NotNull final Reader reader) throws IOException {
        this.properties = new Properties();
        this.properties.load(reader);
    }

    /**
     * Creates a new property query loader from an {@link InputStream}.
     * This constructor reads the property file in UTF-8 by default. If any other charset is other, the constructor
     * {@link #PropertyQueryLoader(Reader)} may be used.
     *
     * @param stream an InputStream that represents a property file to read from.
     * @throws IllegalArgumentException if stream contains a malformed Unicode escape sequence.
     * @throws IOException if an I/O error occurs.
     */
    public PropertyQueryLoader(@NotNull final InputStream stream) throws IOException {
        this(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Override
    protected @Nullable String load(@NotNull final String key) {
        return this.properties.getProperty(key);
    }
}

package com.github.g4memas0n.cores.database.query;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * An implementing query loader that loads the mapping from a json file.<br>
 * This loader will only accept json files that are formed properly, like this example:
 * <pre><code>
 * {
 *     "identifier.one": "SQL Query",
 *     "identifier.two": "Another SQL Query"
 * }
 * </code></pre>
 *
 * @since 1.0.0
 */
public class JsonQueryLoader extends QueryLoader {

    private final JsonObject root;

    /**
     * Creates a new json query loader from a {@link Reader}.
     * Unlike the constructor {@link #JsonQueryLoader(InputStream)}, there is no limitation as to the encoding of
     * the input json file.
     * @param reader a reader that represents a json file to read from.
     * @throws IllegalArgumentException if a malformed Unicode escape sequence appears from reader.
     * @throws IOException if an I/O error occurs.
     */
    public JsonQueryLoader(@NotNull final Reader reader) throws IOException {
        try {
            this.root = new GsonBuilder().create().fromJson(reader, JsonObject.class);
        } catch (JsonSyntaxException ex) {
            throw new IllegalArgumentException("file must begin with an object", ex);
        } catch (JsonIOException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Creates a new json query loader from an {@link InputStream}.
     * This constructor reads the json file in UTF-8 by default. If any other charset is other, the constructor
     * {@link #JsonQueryLoader(Reader)} may be used.
     *
     * @param stream an InputStream that represents a json file to read from.
     * @throws IllegalArgumentException if stream contains a malformed Unicode escape sequence.
     * @throws IOException if an I/O error occurs.
     */
    public JsonQueryLoader(@NotNull final InputStream stream) throws IOException {
        this(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Override
    protected @Nullable String load(@NotNull String key) {
        JsonElement element = this.root.get(key);

        if (element == null) {
            JsonElement entry;
            JsonObject section = this.root;
            int index, prev = 0;

            while ((index = key.indexOf(".", prev)) >= 0) {
                entry = section.get(key.substring(0, index));

                if (entry != null && entry.isJsonObject()) {
                    section = entry.getAsJsonObject();
                    element = section.get(key = key.substring(index + 1));

                    if (element != null) {
                        break;
                    }

                    prev = 0;
                } else  {
                    prev = index + 1;
                }
            }
        }

        if (element != null && element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().getAsString();
        }

        return null;
    }
}

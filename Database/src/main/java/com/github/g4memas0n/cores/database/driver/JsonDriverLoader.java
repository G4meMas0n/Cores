package com.github.g4memas0n.cores.database.driver;

import com.google.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * An implementing driver loader that loads the driver information from a json file.
 * This loader will only accept json files that are formed properly, like this example:
 * <pre><code>
 * [
 *     {
 *         "class": "path.to.driver.or.datasource.Class",
 *         "url": "jdbc://url:for/driver-class",    # Only required if the class specified by 'class' implements the Driver interface.
 *         "type": "SQLType",                       # Database type like 'MySQL' and/or 'SQLite', etc...
 *         "version": "1",                          # Optional entry for specifying the type version.
 *         "properties": {                          # Optional entry for specifying data source properties.
 *             "key": "value"
 *         }
 *     }
 * ]
 * </code></pre>
 *
 * @since 1.0.0
 */
public class JsonDriverLoader extends DriverLoader {

    private final JsonArray root;

    /**
     * Creates a new json driver loader from a {@link Reader}.
     * Unlike the constructor {@link #JsonDriverLoader(InputStream)}, there is no limitation as to the encoding of
     * the input json file.
     * @param reader a reader that represents a json file to read from.
     * @throws IllegalArgumentException if a malformed Unicode escape sequence appears from reader.
     * @throws IOException if an I/O error occurs.
     */
    public JsonDriverLoader(@NotNull final Reader reader) throws IOException {
        try {
            this.root = new GsonBuilder().create().fromJson(reader, JsonArray.class);

            for (JsonElement element : this.root) {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("file contains illegal elements");
                }
            }
        } catch (JsonSyntaxException ex) {
            throw new IllegalArgumentException("file must begin with an array", ex);
        } catch (JsonIOException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Creates a new json driver loader from an {@link InputStream}.
     * This constructor reads the json file in UTF-8 by default. If any other charset is other, the constructor
     * {@link #JsonDriverLoader(Reader)} may be used.
     * @param stream an InputStream that represents a json file to read from.
     * @throws IllegalArgumentException if stream contains a malformed Unicode escape sequence.
     * @throws IOException if an I/O error occurs.
     */
    public JsonDriverLoader(@NotNull final InputStream stream) throws IOException {
        this(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Override
    public @NotNull Driver[] loadAll() {
        final Iterator<JsonElement> iterator = this.root.iterator();
        final List<Driver> drivers = new ArrayList<>();

        while (iterator.hasNext()) {
            try {
                drivers.add(load(iterator.next().getAsJsonObject()));
            } catch (IllegalArgumentException ignored) {

            }
        }

        return drivers.toArray(new Driver[0]);
    }

    @Override
    public @NotNull Driver[] load(@NotNull final String type) {
        final Iterator<JsonElement> iterator = this.root.iterator();
        final List<Driver> drivers = new ArrayList<>();
        JsonObject entry;

        while (iterator.hasNext()) {
            entry = iterator.next().getAsJsonObject();

            if (entry.has("type") && entry.get("type").isJsonPrimitive()) {
                try {
                    if (entry.get("type").getAsString().equalsIgnoreCase(type)) {
                        drivers.add(load(entry));
                    }
                } catch (IllegalArgumentException ignored) {

                }
            }
        }

        return drivers.toArray(new Driver[0]);
    }

    /**
     * Loads a driver from the given json object by reading the key/value pairs.
     * @param object the json object representing a driver.
     * @return a driver representation for the given object.
     * @throws IllegalArgumentException if the json object represents an illegal driver.
     */
    protected @NotNull Driver load(@NotNull final JsonObject object) {
        Preconditions.checkArgument(object.has("class"), "missing driver class");
        Driver driver;

        try {
            String url = object.has("url") ? object.get("url").getAsString() : null;
            String version = object.has("version") ? object.get("version").getAsString() : null;
            driver = new Driver(object.get("class").getAsString(), object.get("type").getAsString(), version, url);

            if (object.has("properties")) {
                Properties properties = new Properties();

                for (Entry<String, JsonElement> property : object.getAsJsonObject("properties").entrySet()) {
                    if (property.getValue().isJsonPrimitive()) {
                        properties.setProperty(property.getKey(), property.getValue().getAsString());
                    }
                }

                driver.setProperties(properties);
            }
        } catch (ClassCastException | IllegalStateException ex) {
            throw new IllegalArgumentException("illegal driver element", ex);
        }

        return driver;
    }
}

package com.github.g4memas0n.cores.database.driver;

import com.github.g4memas0n.cores.database.DatabaseManager;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

/**
 * An implementing driver loader that loads the driver information from a json file.<br>
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

    private final Gson gson;
    private JsonArray root;

    /**
     * Public constructor for creating a driver loader that reads json files.
     */
    public JsonDriverLoader() {
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void load(@NotNull final String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            Preconditions.checkArgument(stream != null, "missing file at " + path);
            load(stream);
        }
    }

    @Override
    public void load(@NotNull final InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            JsonArray root = this.gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : root) {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("file contains illegal elements");
                }
            }

            this.root = root;
        } catch (JsonSyntaxException ex) {
            throw new IllegalArgumentException("file must begin with an array");
        } catch (JsonIOException ex) {
            throw new IOException("file could not be parsed", ex);
        }
    }

    @Override
    public @NotNull List<Driver> loadDrivers() {
        return loadDrivers("*"); // Using wildcard driver type
    }

    @Override
    public @NotNull List<Driver> loadDrivers(@NotNull final String type) {
        Preconditions.checkState(this.root != null, "no file have been loaded yet");
        final Iterator<JsonElement> iterator = this.root.iterator();
        final List<Driver> drivers = new LinkedList<>(); // Using linked list because it will most likely be iterated
        JsonObject entry;
        String url, version;
        Driver driver;

        while (iterator.hasNext()) {
            entry = iterator.next().getAsJsonObject();

            if (!entry.has("class") || !entry.has("type")) {
                DatabaseManager.getLogger().severe("Skipping invalid driver entry with missing class/type");
                continue;
            }

            try {
                if (type.equals("*") || entry.get("type").getAsString().equalsIgnoreCase(type)) {
                    url = entry.has("url") ? entry.get("url").getAsString() : null;
                    version = entry.has("version") ? entry.get("version").getAsString() : null;
                    driver = new Driver(entry.get("class").getAsString(), entry.get("type").getAsString(), version, url);

                    if (entry.has("properties")) {
                        Properties properties = new Properties();

                        for (Entry<String, JsonElement> property : entry.getAsJsonObject("properties").entrySet()) {
                            if (property.getValue().isJsonPrimitive()) {
                                properties.setProperty(property.getKey(), property.getValue().getAsString());
                            }
                        }

                        driver.setProperties(properties);
                    }

                    drivers.add(driver);
                }
            } catch (UnsupportedOperationException ex) {
                DatabaseManager.getLogger().log(Level.SEVERE, "Skipping malformed driver entry for type " + type, ex);
            } catch (IllegalArgumentException ignored) {

            }
        }

        return drivers;
    }
}

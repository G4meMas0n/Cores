package com.github.g4memas0n.cores.database.driver;

import com.github.g4memas0n.cores.database.DatabaseManager;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
 * This loader will only accept json files that are formed like this:
 * <pre><code>
 * {
 *     "drivers": [
 *         {
 *             "class": "path.to.driver.or.datasource.Class",
 *             "url": "jdbc://url:for/driver-class",        # Only required if the class specified by 'class' implements the Driver interface.
 *             "type": "SQLType",                           # Database type like 'MySQL' and/or 'SQLite', etc...
 *             "queries": "path/to/queries/file.extension", # Optional entry for specifying a queries file.
 *             "properties": {                              # Optional entry for specifying data source properties.
 *                 "key": "value"
 *             }
 *         }
 *     ]
 * }
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
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        Preconditions.checkArgument(stream != null, "missing file at " + path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            JsonObject root = this.gson.fromJson(reader, JsonObject.class);
            JsonElement element = root.get("drivers");

            if (element != null) {
                if (!element.isJsonArray()) {
                    throw new JsonParseException("expected element of drivers to be a json array");
                }

                this.root = element.getAsJsonArray();
                this.path = path;
            } else {
                throw new JsonParseException("expected drivers key in the root JsonObject, but was missing");
            }
        } catch (JsonParseException ex) {
            throw new IOException("unable to parse driver file at " + path + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public @NotNull List<Driver> loadDrivers() {
        Preconditions.checkState(this.root != null, "no driver file have been loaded yet");
        return loadDrivers("*"); // Using wildcard driver type
    }

    @Override
    public @NotNull List<Driver> loadDrivers(@NotNull final String type) {
        Preconditions.checkState(this.root != null, "no driver file have been loaded yet");
        final Iterator<JsonElement> iterator = this.root.iterator();
        final List<Driver> drivers = new LinkedList<>(); // Using linked list because it will most likely be iterated
        Properties properties;
        JsonElement element;
        JsonObject entry;
        Driver driver;

        while (iterator.hasNext()) {
            if (!(element = iterator.next()).isJsonObject()) {
                DatabaseManager.getLogger().warning("Skipping illegal json element in driver file " + this.path);
                continue;
            }

            entry = element.getAsJsonObject();
            if (!entry.has("class") || !entry.has("type")) {
                DatabaseManager.getLogger().warning("Skipping invalid json object with missing class or type key in driver file " + this.path);
                continue;
            }

            try {
                if (type.equals("*") || entry.get("type").getAsString().equalsIgnoreCase(type)) {
                    driver = new Driver(Class.forName(entry.get("class").getAsString()), entry.get("type").getAsString(),
                            entry.has("url") ? entry.get("url").getAsString() : null);

                    if (entry.has("queries")) {
                        driver.setQueries(entry.get("queries").getAsString());
                    }

                    if (entry.has("properties")) {
                        entry = entry.get("properties").getAsJsonObject();
                        properties = new Properties();

                        for (Entry<String, JsonElement> property : entry.entrySet()) {
                            if (property.getValue().isJsonPrimitive()) {
                                properties.setProperty(property.getKey(), property.getValue().getAsString());
                            }
                        }

                        driver.setProperties(properties);
                    }

                    drivers.add(driver);
                }
            } catch (UnsupportedOperationException ex) {
                DatabaseManager.getLogger().log(Level.WARNING, "Skipping invalid json object in driver file " + this.path, ex);
            } catch (ClassNotFoundException ex) {
                DatabaseManager.getLogger().log(Level.WARNING, "Could not find class in driver file " + this.path, ex);
            }
        }

        return drivers;
    }
}

package com.github.g4memas0n.cores.database.driver;

import com.github.g4memas0n.cores.database.DatabaseManager;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(path);
        Preconditions.checkArgument(stream != null, "Missing file at path " + path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            JsonObject root = this.gson.fromJson(reader, JsonObject.class);
            JsonElement element = root.get("drivers");

            if (element == null) {
                throw new JsonSyntaxException("Expected drivers key in the root JsonObject, but was missing");
            }

            if (element.isJsonArray()) {
                this.root = element.getAsJsonArray();

                if (this.root.size() == 0) {
                    this.root = null;
                    throw new JsonSyntaxException("Expected at least one driver, but count was zero");
                }
            } else {
                throw new JsonSyntaxException("Expected driver key to be a JsonArray, but was " + element.getClass().getName());
            }
        } catch (JsonParseException ex) {
            throw new IOException("Unable to parse driver file at " + path, ex);
        }
    }

    @Override
    public @NotNull List<Driver> loadDrivers() {
        Preconditions.checkState(this.root != null, "The driver file has not been loaded yet");

        return this.loadDrivers("*"); // Using wildcard driver type
    }

    @Override
    public @NotNull List<Driver> loadDrivers(@NotNull final String type) {
        Preconditions.checkState(this.root != null, "The driver file has not been loaded yet");
        final Iterator<JsonElement> iterator = this.root.iterator();
        final List<Driver> drivers = new LinkedList<>(); // Using linked list because it will most likely be iterated

        while (iterator.hasNext()) {
            JsonObject entry = iterator.next().getAsJsonObject();

            if (!entry.has("type") || !entry.has("class")) {
                continue;
            }

            try {
                if (type.equals("*") || entry.get("type").getAsString().equalsIgnoreCase(type)) {
                    Driver driver = new Driver(entry.get("type").getAsString(), entry.get("class").getAsString(),
                            entry.has("url") ? entry.get("url").getAsString() : null,
                            entry.has("queries") ? entry.get("queries").getAsString() : null);

                    if (entry.has("properties")) {
                        JsonObject entryProperties = entry.get("properties").getAsJsonObject();
                        Properties properties = new Properties();

                        for (Entry<String, JsonElement> property : entryProperties.entrySet()) {
                            if (property.getValue().isJsonPrimitive()) {
                                properties.setProperty(property.getKey(), property.getValue().getAsString());
                            }
                        }

                        driver.setProperties(properties);
                    }

                    drivers.add(driver);
                }
            } catch (IllegalStateException ex) {
                DatabaseManager.getLogger().log(Level.WARNING, "Skipping invalid json object in driver file " + this.path, ex);
            }
        }

        return drivers;
    }
}

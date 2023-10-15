package de.g4memas0n.core.database.driver;

import de.g4memas0n.core.database.driver.Driver.Vendor;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * An implementing reader class for reading database drivers from json files.
 * <p>
 * The reader only accepts json files that are formed properly, like the following example. The jdbcUrl is only required
 * if the specified class implements {@link java.sql.Driver}. The properties are completely optional.
 * <pre><code>
 * [
 *   {
 *     "class": "com.mysql.cj.jdbc.Driver",
 *     "jdbcUrl": "jdbc:mysql://localhost:3306/db",
 *     "properties": {
 *         "user": "root",
 *         "password": "my password"
 *     }
 *     "vendor": {
 *       "name": "MySQL",
 *       "version": 8
 *     }
 *   }
 * ]
 * </code></pre>
 */
public class JsonDriverReader extends DriverReader {

    /**
     * Default constructor for constructing a json driver reader.
     */
    public JsonDriverReader() { }

    @Override
    public @NotNull List<Driver> readAll(@NotNull InputStream stream) throws IOException {
        return readAll(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @Override
    public @NotNull List<Driver> readAll(@NotNull Reader javaReader) throws IOException {
        JsonReader reader = new JsonReader(javaReader);
        Map<Vendor, Driver> drivers = new HashMap<>();
        Driver driver;

        reader.beginArray();
        while (reader.hasNext()) {
            try {
                driver = readDriver(reader);

                if (!drivers.containsKey(driver.getVendor())) {
                    drivers.put(driver.getVendor(), driver);
                }
            } catch (IOException ignored) {
                // simply ignore malformed driver's
            }
        }
        reader.endArray();

        return new ArrayList<>(drivers.values());
    }

    private @NotNull Driver readDriver(@NotNull JsonReader reader) throws IOException {
        Properties properties = null;
        String className = null;
        String jdbcUrl = null;
        Vendor vendor = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "class":
                    className = reader.nextString();
                    break;

                case "jdbcUrl":
                    jdbcUrl = reader.nextString();
                    break;

                case "properties":
                    properties = readProperties(reader);
                    break;

                case "vendor":
                    vendor = readVendor(reader);
                    break;

                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (className == null || vendor == null) {
            throw new IOException("missing class or vendor");
        } else {
            Driver driver = new Driver(className, vendor);

            if (jdbcUrl != null) {
                driver.setJdbcUrl(jdbcUrl);
            }

            if (properties != null) {
                driver.setProperties(properties);
            }

            return driver;
        }
    }

    private @NotNull Properties readProperties(@NotNull JsonReader reader) throws IOException {
        Properties properties = new Properties();
        String key;

        reader.beginObject();
        while (reader.hasNext()) {
            key = reader.nextName();

            if (reader.peek() == JsonToken.STRING) {
                properties.setProperty(key, reader.nextString());
                continue;
            }

            reader.skipValue();
        }
        reader.endObject();

        return properties;
    }

    private @NotNull Vendor readVendor(@NotNull JsonReader reader) throws IOException {
        String name = null;
        int version = 0;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "name":
                    name = reader.nextString();
                    break;

                case "version":
                    switch (reader.peek()) {
                        case STRING:
                            version = Integer.parseInt(reader.nextString());
                            break;

                        case NUMBER:
                            version = reader.nextInt();
                            break;

                        default:
                            throw new IOException("unexpected version token");
                    }
                    break;

                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (name == null) {
            throw new IOException("missing vendor name");
        }

        return version > 0 ? new Vendor(name, version) : new Vendor(name);
    }
}

package com.github.g4memas0n.cores.database.loader;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonDriverLoader extends DriverLoader {

    protected final Gson gson;
    protected JsonArray drivers;

    public JsonDriverLoader() {
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void load(@NotNull final InputStream file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file, StandardCharsets.UTF_8))) {
            JsonObject root = this.gson.fromJson(reader, JsonObject.class);
            JsonElement element = root.get("drivers");

            if (element.isJsonArray()) {
                this.drivers = element.getAsJsonArray();

                if (this.drivers.size() == 0) {
                    throw new JsonSyntaxException("Expected drivers json array size greater than zero");
                }
            } else {
                throw new JsonSyntaxException("Expected drivers to be a JsonArray, but was " + element.getClass().getName());
            }
        } catch (JsonParseException ex) {
            throw new IOException("Unable to parse drivers file", ex);
        }
    }

    @Override
    public @NotNull List<Driver> get(@NotNull final String type) {
        Preconditions.checkState(this.drivers != null, "The driver file has not been loaded yet");
        final Iterator<JsonElement> iterator = this.drivers.iterator();
        final List<Driver> results = new ArrayList<>();

        while (iterator.hasNext()) {
            JsonElement element = iterator.next();

            if (element.isJsonObject()) {
                JsonObject entry = element.getAsJsonObject();

                if (entry.has("type") && entry.get("type").getAsString().equalsIgnoreCase(type)) {
                    if ((!entry.has("driver") && !entry.has("source")) || !entry.has("url")) {
                        DatabaseManager.getLogger().warning("Encountered invalid driver file entry: "
                                + "Entry must contain a driver or source member and an url member");
                        continue;
                    }

                    Driver driver = new Driver(entry.get("type").getAsString(),
                            entry.has("version") ? entry.get("version").getAsInt() : 0,
                            entry.has("driver") ? entry.get("driver").getAsString() : null,
                            entry.has("source") ? entry.get("source").getAsString() : null,
                            entry.has("statements") ? entry.get("statements").getAsString() : null,
                            entry.get("url").getAsString());

                    if (!results.isEmpty()) {
                        int position = results.size();

                        for (int index = 0; index < results.size(); index++) {
                            if (driver.compareTo(results.get(index)) > 0) {
                                position = index;
                                break;
                            }
                        }

                        results.add(position, driver);
                    } else {
                        results.add(driver);
                    }
                }
            } else {
                DatabaseManager.getLogger().warning("Encountered illegal driver file entry: "
                        + "Expected entry to be a JsonObject, but was " + element.getClass().getName());
            }
        }

        return results;
    }
}

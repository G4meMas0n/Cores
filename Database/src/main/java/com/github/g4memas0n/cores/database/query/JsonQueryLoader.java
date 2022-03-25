package com.github.g4memas0n.cores.database.query;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * An implementing query loader that loads the mapping from a json file.<br>
 * This loader will only accept json files that are formed like this:
 * <pre><code>
 * {
 *     "queries": {
 *         "identifier": "SQL Query",
 *         "another.identifier": "Another SQL Query"
 *     }
 * }
 * </code></pre>
 *
 * @since 1.0.0
 */
public class JsonQueryLoader extends QueryLoader {

    private final Gson gson;
    private JsonObject root;

    /**
     * Public constructor for creating a query loader that reads json files.
     */
    public JsonQueryLoader() {
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void load(@NotNull final String path) throws IOException {
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(path);
        Preconditions.checkArgument(stream != null, "Missing file at path " + path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            JsonObject root = this.gson.fromJson(reader, JsonObject.class);
            JsonElement element = root.get("queries");

            if (element == null) {
                throw new JsonSyntaxException("Expected queries key in the root JsonObject, but was missing");
            }

            if (element.isJsonObject()) {
                this.root = element.getAsJsonObject();

                if (this.root.size() == 0) {
                    this.root = null;
                    throw new JsonSyntaxException("Expected at least one query, but count was zero");
                }
            } else {
                throw new JsonSyntaxException("Expected queries key to be a JsonObject, but was " + element.getClass().getName());
            }
        } catch (JsonParseException ex) {
            throw new IOException("Unable to parse queries file at " + path, ex);
        }
    }

    @Override
    public @Nullable String loadQuery(@NotNull final String identifier) {
        Preconditions.checkState(this.root != null, "The queries file has not been loaded yet");
        final JsonElement element = this.root.get(identifier);

        if (element != null && element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();

            return primitive.isString() ? primitive.getAsString() : null;
        }

        return null;
    }
}

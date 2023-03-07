package com.github.g4memas0n.cores.database.query;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
 *     "batches": {
 *         "identifier1": "path/to/batch/file",
 *         "identifier2": "path/to/another/batch/file"
 *     },
 *     "queries": {
 *         "identifier3": "SQL Query",
 *         "identifier4": "Another SQL Query"
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
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        Preconditions.checkArgument(stream != null, "missing file at " + path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            JsonObject root = this.gson.fromJson(reader, JsonObject.class);

            if (root.has("batches")) {
                if (!root.get("batches").isJsonObject()) {
                    throw new JsonParseException("expected element of batches to be a json object");
                }
            }

            if (root.has("queries")) {
                if (!root.get("queries").isJsonObject()) {
                    throw new JsonParseException("expected element of queries to be a json object");
                }
            }

            this.path = path;
            this.root = root;
        } catch (JsonParseException ex) {
            throw new IOException("unable to parse queries file at " + path + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected @Nullable String loadBatch(@NotNull final String identifier) {
        Preconditions.checkState(this.root != null, "no queries file has been loaded yet");
        final JsonObject batches = this.root.getAsJsonObject("batches");

        if (batches != null) {
            final JsonElement element = batches.get(identifier);

            if (element != null && element.isJsonPrimitive()) {
                return element.getAsJsonPrimitive().getAsString();
            }
        } else {
            throw new IllegalStateException("no batches have been loaded yet");
        }

        return null;
    }

    @Override
    protected @Nullable String loadQuery(@NotNull final String identifier) {
        Preconditions.checkState(this.root != null, "no queries file have been loaded yet");
        final JsonObject queries = this.root.getAsJsonObject("queries");

        if (queries != null) {
            final JsonElement element = queries.get(identifier);

            if (element != null && element.isJsonPrimitive()) {
                return element.getAsJsonPrimitive().getAsString();
            }
        } else {
            throw new IllegalStateException("no queries have been loaded yet");
        }

        return null;
    }
}

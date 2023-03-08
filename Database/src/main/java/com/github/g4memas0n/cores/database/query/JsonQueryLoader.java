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

import static com.github.g4memas0n.cores.database.DatabaseManager.getLogger;

/**
 * An implementing query loader that loads the mapping from a json file.<br>
 * This loader will only accept json files that are formed like this:
 * <pre><code>
 * {
 *     "options": {
 *         "parent": "path/to/parent/file"
 *     }
 *     "batches": {
 *         "identifier.one": "path/to/batch/file",
 *         "identifier.two": "path/to/another/batch/file"
 *     },
 *     "queries": {
 *         "identifier.one": "SQL Query",
 *         "identifier.two": "Another SQL Query"
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

            if (root.has("options")) {
                if (!root.get("options").isJsonObject()) {
                    throw new JsonParseException("expected element of options to be a json object");
                }

                JsonElement parent = root.getAsJsonObject("options").get("parent");

                if (parent != null && parent.isJsonPrimitive()) {
                    this.parent = QueryLoader.loadFile(parent.getAsString());
                }
            }

            this.path = path;
            this.root = root;
        } catch (JsonParseException ex) {
            throw new IOException("queries file " + path + " could not be parsed", ex);
        }
    }

    @Override
    protected @Nullable String loadBatch(@NotNull final String identifier) {
        Preconditions.checkState(this.root != null, "no queries file has been loaded yet");
        final JsonObject batches = this.root.getAsJsonObject("batches");

        if (batches != null) {
            final JsonElement element = batches.get(identifier);

            if (element != null) {
                if (element.isJsonPrimitive()) {
                    return element.getAsString();
                }

                getLogger().severe("Skipping illegal batch element " + identifier + " in queries file: " + this.path);
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

            if (element != null) {
                if (element.isJsonPrimitive()) {
                    return element.getAsString();
                }

                getLogger().severe("Skipping illegal query element " + identifier + " in queries file: " + this.path);
            }
        } else {
            throw new IllegalStateException("no queries have been loaded yet");
        }

        return null;
    }
}

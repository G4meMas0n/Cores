package com.github.g4memas0n.cores.database.query;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
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
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            Preconditions.checkArgument(stream != null, "missing file at " + path);
            load(stream);
        }
    }

    @Override
    public void load(@NotNull final InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            this.root = this.gson.fromJson(reader, JsonObject.class);
        } catch (JsonSyntaxException ex) {
            throw new IllegalArgumentException("file must begin with an object");
        } catch (JsonIOException ex) {
            throw new IOException("file could not be parsed", ex);
        }
    }

    @Override
    protected @Nullable String loadQuery(@NotNull String identifier) {
        Preconditions.checkState(this.root != null, "no file have been loaded yet");
        JsonElement element = this.root.get(identifier);

        if (element == null) {
            JsonElement entry;
            JsonObject section = this.root;
            int index, prev = 0;

            while ((index = identifier.indexOf(".", prev)) >= 0) {
                entry = section.get(identifier.substring(0, index));

                if (entry != null && entry.isJsonObject()) {
                    section = entry.getAsJsonObject();
                    element = section.get(identifier = identifier.substring(index + 1));

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

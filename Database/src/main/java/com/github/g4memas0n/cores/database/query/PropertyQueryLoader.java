package com.github.g4memas0n.cores.database.query;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * An implementing query loader that loads the mapping from a property file.
 *
 * @since 1.0.0
 */
public class PropertyQueryLoader extends QueryLoader {

    private Properties properties;

    /**
     * Public constructor for creating a query loader that reads properties files.
     */
    public PropertyQueryLoader() {
        this.properties = null;
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
        try {
            this.properties = new Properties();
            this.properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IOException ex) {
            this.properties = null;
            throw ex;
        }
    }

    @Override
    protected @Nullable String loadQuery(@NotNull final String identifier) {
        Preconditions.checkState(this.properties != null, "no file have been loaded yet");
        return this.properties.getProperty(identifier);
    }
}

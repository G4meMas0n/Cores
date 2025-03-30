package de.g4memas0n.core.database.connector;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;

/**
 * A H2 database connector.
 * @see FlatFileConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class H2Connector extends FlatFileConnector {

    /**
     * Constructs a H2 database connector.
     * @param path the path to the database file.
     * @see FlatFileConnector
     */
    public H2Connector(@NotNull Path path) {
        super(path);
    }

    @Override
    public @NotNull String getVendorName() {
        return "H2";
    }

    @Override
    public void configure(@NotNull Properties properties) {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ex) {
            logger.log(Level.WARNING, "Could not load H2 driver", ex);
            throw new RuntimeException("driver not available", ex);
        }

        super.configure(properties);
    }

    @Override
    public @NotNull String createUrl(@NotNull Path path) {
        return "jdbc:h2:file:" + path;
    }
}

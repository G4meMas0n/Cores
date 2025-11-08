package de.g4memas0n.core.sql.connector;

import de.g4memas0n.core.sql.StatementProcessor;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;

/**
 * A SQLite database connector.
 * @see FlatFileConnector
 * @see Connector
 */
@SuppressWarnings("unused")
public class SQLiteConnector extends FlatFileConnector {

    private StatementProcessor processor;

    /**
     * Constructs a SQLite database connector.
     * @param path the path to the database file.
     * @see FlatFileConnector
     */
    public SQLiteConnector(@NotNull Path path) {
        super(path);
    }

    @Override
    public @NotNull String getVendorName() {
        return "SQLite";
    }

    @Override
    public @NotNull StatementProcessor getStatementProcessor() {
        if (processor != null) return processor;
        return StatementProcessor.BACKTICK_PROCESSOR;
    }

    @Override
    public void setStatementProcessor(@NotNull StatementProcessor processor) {
        this.processor = processor.equals(StatementProcessor.BACKTICK_PROCESSOR) ? null : processor;
    }

    @Override
    public void configure(@NotNull Properties properties) {
        // Set default encoding to UTF-8
        if (!properties.containsKey("encoding")) {
            properties.put("encoding", "UTF-8");
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            logger.log(Level.WARNING, "Could not load SQLite driver", ex);
            throw new RuntimeException("driver not available", ex);
        }

        super.configure(properties);
    }

    @Override
    public @NotNull String createUrl(@NotNull Path path) {
        return "jdbc:sqlite:" + path;
    }
}

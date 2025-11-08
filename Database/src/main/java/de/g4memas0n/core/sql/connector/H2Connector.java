package de.g4memas0n.core.sql.connector;

import de.g4memas0n.core.sql.StatementProcessor;
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
     * A pre-defined statement processor that is based on the backtick processor and replaces the case-sensitive 'LIKE'
     * operator with the case-insensitive operator 'ILIKE'.
     */
    public static final StatementProcessor H2_PROCESSOR = StatementProcessor.BACKTICK_PROCESSOR.compose(
            statement -> statement.replaceAll("(?i)LIKE", "ILIKE")
    );

    private StatementProcessor processor;

    /**
     * Constructs an H2 database connector.
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
    public @NotNull StatementProcessor getStatementProcessor() {
        if (processor != null) return processor;
        return H2_PROCESSOR;
    }

    @Override
    public void setStatementProcessor(@NotNull StatementProcessor processor) {
        this.processor = processor.equals(H2_PROCESSOR) ? null : processor;
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

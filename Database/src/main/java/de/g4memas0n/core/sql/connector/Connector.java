package de.g4memas0n.core.sql.connector;

import de.g4memas0n.core.sql.StatementProcessor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Wrapper;
import java.util.Properties;

/**
 * An interface for connecting to a database.
 */
@SuppressWarnings("unused")
public interface Connector extends Wrapper {

    /**
     * Gets the vendor name of the connector implementation.
     * @return the database vendor name.
     */
    @NotNull String getVendorName();
    
    /**
     * Gets whether this connector has been shut down or not.
     * @return true, if the connector is shut down.
     */
    boolean isShutdown();

    /**
     * Configures the database connector with the specified properties.
     * @param properties the driver or data source properties.
     */
    void configure(@NotNull Properties properties);

    /**
     * Shutdowns the database connector.
     */
    void shutdown();

    /**
     * Attempts to establish a connection to the database.
     * @return a connection to the database.
     * @throws SQLException if a database error occurs.
     */
    @NotNull Connection getConnection() throws SQLException;
    
    /**
     * Attempts to close the specified connection to the database.
     * @param connection the connection to close.
     */
    default void closeConnection(@NotNull Connection connection) {
        try (Connection closeable = connection) {
            if (!closeable.getAutoCommit()) {
                closeable.rollback();
            }
        } catch (SQLException ignored) {}
    }

    /**
     * Gets the statement processor for the connector implementation.
     * @return the statement processor.
     */
    @NotNull StatementProcessor getStatementProcessor();

    /**
     * Sets the statement processor for the connector implementation.
     * @param processor the new statement processor.
     */
    void setStatementProcessor(@NotNull StatementProcessor processor);

    /**
     * Processes the specified sql statement based on the statement processor of the connector implementation.
     * @param statement the SQL statement to process.
     * @return the processed vendor-specific SQL statement.
     * @see StatementProcessor#process(String) 
     */
    default @NotNull @Language("SQL") String processSQL(@NotNull @Language("SQL") String statement) {
        return getStatementProcessor().process(statement);
    }
}

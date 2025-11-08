package de.g4memas0n.core.sql;

import org.jetbrains.annotations.NotNull;

/**
 * A functional interface for processing sql statement.
 */
@FunctionalInterface
@SuppressWarnings("unused")
public interface StatementProcessor {

    /**
     * A pre-defined statement processor that returns the statement without any modifications.
     */
    @NotNull StatementProcessor IDENTITY_PROCESSOR = statement -> statement;

    /**
     * A pre-defined statement processor that replaces literal enquotes with backticks.
     * Used in MySQL, MariaDB and SQLite.
     */
    @NotNull StatementProcessor BACKTICK_PROCESSOR = statement -> statement.replaceAll("\"", "`");

    /**
     * A pre-defined statement processor that replaces literal enquotes with quotes.
     * Used in PostgreSQL.
     */
    @NotNull StatementProcessor QUOTE_PROCESSOR = statement -> statement.replaceAll("`", "\"");

    /**
     * Processes the specified sql statement to replace the standard sql syntax with a vendor-specific SQL syntax.
     * @param statement the sql statement to process.
     * @return the processed vendor-specific sql statement.
     */
    @NotNull String process(@NotNull String statement);

    /**
     * Returns a new statement processor that prepends the specified statement processor to this statement processor.
     * @param processor the statement processor to prepend.
     * @return the new composed statement processor.
     */
    default @NotNull StatementProcessor compose(@NotNull StatementProcessor processor) {
        return statement -> process(processor.process(statement));
    }
}

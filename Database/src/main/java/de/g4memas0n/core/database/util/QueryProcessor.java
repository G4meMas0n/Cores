package de.g4memas0n.core.database.util;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

/**
 * a functional interface to process sql queries.
 */
@FunctionalInterface
public interface QueryProcessor {

    /**
     * Processes the given sql query and returns the resulting query.
     * @param query the sql query to process.
     * @return the processed sql query.
     */
    @NotNull @Language("SQL") String process(@NotNull @Language("SQL") String query);

}

package com.github.g4memas0n.cores.database.query;

import org.junit.Assert;
import org.junit.Test;
import java.util.MissingResourceException;
import java.util.Random;

public class QueryLoaderTest {

    private final Random random = new Random();

    @Test(expected = IllegalArgumentException.class)
    public void missingExtensionLoadTest() {
        QueryLoader.loadFile("database/queries");
    }

    @Test(expected = MissingResourceException.class)
    public void missingQueryGetTest() {
        QueryLoader loader;

        if (this.random.nextBoolean()) {
            loader = QueryLoader.loadFile("database/queries.json");
        } else {
            loader = QueryLoader.loadFile("database/queries.xml");
        }

        loader.getQuery("query.2");
    }

    @Test
    public void successfulQueryGetTest() {
        QueryLoader loader;

        if (this.random.nextBoolean()) {
            loader = QueryLoader.loadFile("database/queries.json");
        } else {
            loader = QueryLoader.loadFile("database/queries.xml");
        }

        Assert.assertEquals("FIRST SQL QUERY", loader.getQuery("query.0"));
        Assert.assertEquals("SECOND SQL QUERY", loader.getQuery("query.1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedExtensionLoadTest() {
        QueryLoader.loadFile("database/queries.txt");
    }
}

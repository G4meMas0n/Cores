package com.github.g4memas0n.cores.database.loader;

import com.google.gson.JsonSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;

public class JsonQueryLoaderTest {

    private JsonQueryLoader loader;

    @Before
    public void setup() {
        this.loader = new JsonQueryLoader();
    }

    @Test
    public void emptyFileLoadingTest() {
        try {
            this.loader.load("database/queries-empty.json");
            Assert.fail("Empty file loaded without exception");
        } catch (IOException ex) {
            final Throwable cause = ex.getCause();

            Assert.assertTrue("Cause is not an instance of JsonSyntaxException", cause instanceof JsonSyntaxException);
            Assert.assertEquals("Expected at least one query, but count was zero", cause.getMessage());
        }
    }

    @Test
    public void illegalFileLoadingTest() {
        try {
            this.loader.load("database/queries.xml");
            Assert.fail("Illegal file loaded without exception");
        } catch (IOException ignored) {

        }
    }

    @Test(expected = IllegalStateException.class)
    public void illegalQueryLoadingTest() {
        this.loader.loadQuery("query.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingFileLoadingTest() {
        try {
            this.loader.load("database/missing-file.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test
    public void missingQueryLoadingTest() {
        try {
            this.loader.load("database/queries.json");

            Assert.assertNull(this.loader.loadQuery("query.2"));
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test
    public void successfulFileQueryLoadingTest() {
        try {
            this.loader.load("database/queries.json");

            Assert.assertEquals("FIRST SQL QUERY", loader.getQuery("query.0"));
            Assert.assertEquals("SECOND SQL QUERY", loader.getQuery("query.1"));
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }
}

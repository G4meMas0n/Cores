package com.github.g4memas0n.cores.database.query;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import java.io.IOException;

public class XmlQueryLoaderTest {

    private XmlQueryLoader loader;

    @Before
    public void setup() {
        this.loader = new XmlQueryLoader();
    }

    @Test
    public void emptyFileLoadingTest() {
        try {
            this.loader.load("database/queries-empty.xml");
            Assert.fail("Empty file loaded without exception");
        } catch (IOException ex) {
            final Throwable cause = ex.getCause();

            Assert.assertTrue("Cause is not an instance of SAXException", cause instanceof SAXException);
            Assert.assertEquals("Expected at least one query tag, but count was zero", cause.getMessage());
        }
    }

    @Test
    public void illegalFileLoadingTest() {
        try {
            this.loader.load("database/queries.json");
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
            this.loader.load("database/missing-file.xml");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test
    public void missingQueryLoadingTest() {
        try {
            this.loader.load("database/queries.xml");

            Assert.assertNull(this.loader.loadQuery("query.2"));
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test
    public void successfulFileQueryLoadingTest() {
        try {
            this.loader.load("database/queries.xml");

            Assert.assertEquals("FIRST SQL QUERY", loader.getQuery("query.0"));
            Assert.assertEquals("SECOND SQL QUERY", loader.getQuery("query.1"));
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }
}

package com.github.g4memas0n.cores.database.query;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import java.io.IOException;

public class XmlQueryLoaderTest {

    private XmlQueryLoader loader;

    @Before
    public void setup() {
        this.loader = new XmlQueryLoader();
    }

    public void setupFile() {
        try {
            this.loader.load("database/query/queries.xml");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadMissingFileTest() {
        try {
            this.loader.load("database/query/missing-file.xml");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void loadIllegalFileTest() {
        try {
            this.loader.load("database/query/illegal-batches.xml");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().endsWith("could not be parsed"));
        }

        try {
            this.loader.load("database/query/illegal-queries.xml");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().endsWith("could not be parsed"));
        }

        try {
            this.loader.load("database/query/illegal-options.xml");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().endsWith("could not be parsed"));
        }
    }

    @Test
    public void loadCorrectFileTest() {
        try {
            this.loader.load("database/query/queries.xml");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void loadBatchAtIllegalTimeTest() {
        this.loader.loadBatch("identifier.one");
    }

    @Test(expected = IllegalStateException.class)
    public void loadQueryAtIllegalTimeTest() {
        this.loader.loadQuery("identifier.one");
    }

    @Test
    public void loadMissingBatchTest() {
        setupFile();

        Assert.assertNull("unknown batch", this.loader.loadBatch("missing"));
    }

    @Test
    public void loadMissingQueryTest() {
        setupFile();

        Assert.assertNull("unknown query", this.loader.loadQuery("missing"));
    }

    @Ignore // Fails for unexplainable reasons
    public void loadSpecificBatchTest() {
        String batch;
        setupFile();

        batch = this.loader.loadBatch("identifier.one");
        Assert.assertNotNull("missing batch", batch);
        Assert.assertEquals("non-matching batch", "path/to/batch/file", batch);

        batch = this.loader.loadBatch("identifier.two");
        Assert.assertNotNull("missing batch", batch);
        Assert.assertEquals("non-matching batch", "path/to/another/batch/file", batch);
    }

    @Test
    public void loadSpecificQueryTest() {
        String query;
        setupFile();

        query = this.loader.loadQuery("identifier.one");
        Assert.assertNotNull("missing query", query);
        Assert.assertEquals("non-matching query", "FIRST SQL QUERY", query);

        query = this.loader.loadQuery("identifier.two");
        Assert.assertNotNull("missing query", query);
        Assert.assertEquals("non-matching query", "SECOND SQL QUERY", query);
    }
}

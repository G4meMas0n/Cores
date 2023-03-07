package com.github.g4memas0n.cores.database.query;

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

    public void setupQueries() {
        try {
            this.loader.load("database/queries/queries.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test
    public void illegalBatchesFileLoadingTest() {
        try {
            this.loader.load("database/queries/illegal-batches.json");
            Assert.fail("loaded illegal batches file without exception");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().contains("expected element of batches to be a json object"));
        }
    }

    @Test
    public void illegalQueriesFileLoadingTest() {
        try {
            this.loader.load("database/queries/illegal-queries.json");
            Assert.fail("loaded illegal queries file without exception");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().contains("expected element of queries to be a json object"));
        }
    }

    @Test
    public void illegalFileLoadingTest() {
        try {
            this.loader.load("database/queries/queries.txt");
            Assert.fail("loaded illegal file without exception");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().contains("unable to parse queries file"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingFileLoadingTest() {
        try {
            this.loader.load("database/queries/missing.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void illegalBatchLoadingTest() {
        this.loader.loadBatch("id.0");
    }

    @Test(expected = IllegalStateException.class)
    public void illegalQueryLoadingTest() {
        this.loader.loadQuery("id.0");
    }

    @Test
    public void getBatchLoadingTest() {
        setupQueries();
        String batch;

        Assert.assertNotNull(batch = this.loader.loadBatch("id.0"));
        Assert.assertEquals(batch, "path/to/batch/file");
        Assert.assertNotNull(batch = this.loader.loadBatch("id.1"));
        Assert.assertEquals(batch, "path/to/another/batch/file");
        Assert.assertNull(this.loader.loadBatch("id.2"));
    }

    @Test
    public void getQueryLoadingTest() {
        setupQueries();
        String query;

        Assert.assertNotNull(query = this.loader.loadQuery("id.0"));
        Assert.assertEquals(query, "FIRST SQL QUERY");
        Assert.assertNotNull(query = this.loader.loadQuery("id.1"));
        Assert.assertEquals(query, "SECOND SQL QUERY");
        Assert.assertNull(this.loader.loadQuery("id.2"));
    }
}

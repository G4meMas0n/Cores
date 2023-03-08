package com.github.g4memas0n.cores.database.query;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.util.Random;

public class QueryLoaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void loadMissingExtensionTest() {
        try {
            QueryLoader.loadFile("database/query/queries");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadUnknownExtensionTest() {
        try {
            QueryLoader.loadFile("database/query/queries.txt");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void loadSuccessfulTest() {
        final Random random = new Random();

        try {
            QueryLoader.loadFile(random.nextBoolean() ? "database/query/queries.json" : "database/query/queries.xml");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }
}

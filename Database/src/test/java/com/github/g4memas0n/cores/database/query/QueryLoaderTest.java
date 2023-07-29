package com.github.g4memas0n.cores.database.query;

import com.github.g4memas0n.cores.database.driver.Driver;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import java.io.IOException;

public class QueryLoaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void loadMissingOrUnsupportedTest() {
        try {
            QueryLoader.getLoader("missing");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test
    public void loadForDriverTest() {
        QueryLoader loader = null;

        // Mock driver
        Driver driver = Mockito.mock(Driver.class);
        Mockito.when(driver.getType()).thenReturn("Driver");
        Mockito.when(driver.getVersion()).thenReturn("1");

        try {
            loader = QueryLoader.getLoader("database/queries", driver);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }

        Assert.assertNotNull("missing loader", loader);
        Assert.assertEquals("non-matching query", "Query (driver-version)", loader.load("key"));

        loader = loader.parent;
        Assert.assertNotNull("missing parent loader", loader);
        Assert.assertEquals("non-matching parent query", "Query (driver)", loader.load("key"));

        loader = loader.parent;
        Assert.assertNotNull("missing base loader", loader);
        Assert.assertEquals("non-matching base query", "Query", loader.load("key"));
    }
}

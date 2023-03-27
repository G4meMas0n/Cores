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
        Assert.assertNotNull("missing path", loader.path);
        Assert.assertEquals("non-matching path", "database/queries_driver-1.properties", loader.path);
        Assert.assertEquals("non-matching query", "Query (driver-version)", loader.loadQuery("identifier"));

        loader = loader.parent;
        Assert.assertNotNull("missing parent loader", loader);
        Assert.assertNotNull("missing parent path", loader.path);
        Assert.assertEquals("non-matching parent path", "database/queries_driver.properties", loader.path);
        Assert.assertEquals("non-matching parent query", "Query (driver)", loader.loadQuery("identifier"));

        loader = loader.parent;
        Assert.assertNotNull("missing base loader", loader);
        Assert.assertNotNull("missing base path", loader.path);
        Assert.assertEquals("non-matching base path", "database/queries.properties", loader.path);
        Assert.assertEquals("non-matching base query", "Query", loader.loadQuery("identifier"));
    }
}

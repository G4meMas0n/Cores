package de.g4memas0n.core.database.query;

import de.g4memas0n.core.database.driver.Driver.Vendor;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class QueryReaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void readFileMissingTest() {
        try {
            QueryReader.getQueries("queries/missing");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test
    public void readFileSuccessTest() {
        Properties queries;

        try {
            queries = QueryReader.getQueries("queries/test");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
            return;
        }

        String result = queries.getProperty("key1");

        Assert.assertNotNull("missing query key1", result);
        Assert.assertEquals("unexpected query key1", "Query1", result);
        Assert.assertNull("unexpected query", queries.getProperty("unexpected"));
    }

    @Test
    public void readFileVendorSuccessTest() {
        Vendor vendor = new Vendor("vendor", 1);
        Properties queries;

        try {
            queries = QueryReader.getQueries("queries/test", vendor);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
            return;
        }

        Map<String, String> tests = Map.of("key1", "Query1", "key2", "Query2", "key3", "Query3");
        String result;

        for (Map.Entry<String, String> entry : tests.entrySet()) {
            result = queries.getProperty(entry.getKey());

            Assert.assertNotNull("missing query " + entry.getKey(), result);
            Assert.assertEquals("unexpected query " + entry.getKey(), entry.getValue(), result);
        }

        Assert.assertNull("unexpected query", queries.getProperty("unexpected"));
    }
}

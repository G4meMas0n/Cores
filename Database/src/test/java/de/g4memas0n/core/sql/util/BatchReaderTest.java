package de.g4memas0n.core.sql.util;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.util.List;

public class BatchReaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void readMissingFileTest() {
        try {
            BatchReader.readBatch("missing.sql");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test
    public void readExistingFileTest() {
        String query, expected = "CREATE TABLE table_name (column_id INT NOT NULL,column_value VARCHAR NOT NULL,PRIMARY KEY (column_id))";
        List<String> batch;

        try {
            batch = BatchReader.readBatch("batches/test");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
            return;
        }

        Assert.assertEquals("unexpected batch size", 1, batch.size());
        Assert.assertNotNull("missing batch query", query = batch.get(0));
        Assert.assertEquals("unexpected batch query", expected, query);
    }
}

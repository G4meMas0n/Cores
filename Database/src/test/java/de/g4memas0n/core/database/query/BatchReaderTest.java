package de.g4memas0n.core.database.query;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.util.List;

public class BatchReaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void readFileMissingTest() {
        try {
            BatchReader.getBatch("batches/missing");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test
    public void readFileSuccessTest() {
        String query, expected = "CREATE TABLE table_name (column_id INT NOT NULL,column_value VARCHAR NOT NULL,PRIMARY KEY (column_id))";
        List<String> batch;

        try {
            batch = BatchReader.getBatch("batches/test");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
            return;
        }

        Assert.assertEquals("unexpected batch size", 1, batch.size());
        Assert.assertNotNull("missing batch query", query = batch.get(0));
        Assert.assertEquals("unexpected batch query", expected, query);
    }

    @Test
    public void readFileByVendorSuccessTest() {
        String query, expected = "CREATE TABLE IF NOT EXISTS table_name (column_id INT NOT NULL AUTO_INCREMENT,column_value VARCHAR(255) NOT NULL,PRIMARY KEY (column_id))";
        List<String> batch;

        try {
            batch = BatchReader.getBatch("batches/test", "MySQL");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
            return;
        }

        Assert.assertEquals("unexpected batch size", 1, batch.size());
        Assert.assertNotNull("missing batch query", query = batch.get(0));
        Assert.assertEquals("unexpected batch query", expected, query);
    }
}

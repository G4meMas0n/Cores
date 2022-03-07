package com.github.g4memas0n.cores.database.loader;

import com.github.g4memas0n.cores.database.loader.DriverLoader.Driver;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.io.InputStream;

public class JsonDriverLoaderTest {

    @Test
    public void loadFileTest() {
        final JsonDriverLoader loader = new JsonDriverLoader();
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("database/drivers.json");

        Assert.assertNotNull("Test drivers.json file cannot be found", stream);

        try {
            loader.load(stream);
        } catch (IOException ex) {
            Assert.fail("Exception during loading of test drivers.json file");
        }
    }

    @Test
    public void loadDriverTest() {
        final JsonDriverLoader loader = new JsonDriverLoader();
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("database/drivers.json");

        Assert.assertNotNull("Test drivers.json file cannot be found", stream);

        try {
            loader.load(stream);

            Assert.assertArrayEquals(new Driver[]{
                    new Driver("MySQL", 8, "com.mysql.cj.jdbc.Driver", null,
                            "jdbc:mysql://{HOST}:{PORT}/{DATABASE}", "database/database-mysql.xml"),
                    new Driver("MySQL", 5, "com.mysql.jdbc.Driver", null,
                            "jdbc:mysql://{HOST}:{PORT}/{DATABASE}", "database/database-mysql.xml")
            }, loader.get("MySQL").toArray(Driver[]::new));
        } catch (IOException ex) {
            Assert.fail("Exception during loading of test drivers.json file");
        }
    }

    @Test(expected = NullPointerException.class)
    public void loadDriverPreconditionTest() {
        final JsonDriverLoader loader = new JsonDriverLoader();

        loader.get("MySQL");
    }
}

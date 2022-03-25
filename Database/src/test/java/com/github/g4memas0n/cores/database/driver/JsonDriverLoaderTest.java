package com.github.g4memas0n.cores.database.driver;

import com.google.gson.JsonSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class JsonDriverLoaderTest {

    private JsonDriverLoader loader;

    @Before
    public void setup() {
        this.loader = new JsonDriverLoader();
    }

    @Test
    public void emptyFileLoadingTest() {
        try {
            this.loader.load("database/drivers-empty.json");
            Assert.fail("Empty file loaded without exception");
        } catch (IOException ex) {
            final Throwable cause = ex.getCause();

            Assert.assertTrue("Cause is not an instance of JsonSyntaxException", cause instanceof JsonSyntaxException);
            Assert.assertEquals("Expected at least one driver, but count was zero", cause.getMessage());
        }
    }

    @Test
    public void illegalFileLoadingTest() {
        try {
            this.loader.load("database/drivers-illegal.json");
            Assert.fail("Empty file loaded without exception");
        } catch (IOException ex) {
            final Throwable cause = ex.getCause();

            Assert.assertTrue("Cause is not an instance of JsonSyntaxException", cause instanceof JsonSyntaxException);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void illegalDriverLoadingTest() {
        this.loader.loadDrivers();
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingFileLoadingTest() {
        try {
            this.loader.load("database/missing-file.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test
    public void successfulFileDriverLoadingTest() {
        try {
            this.loader.load("database/drivers.json");
            final List<Driver> drivers = this.loader.loadDrivers("SQLite");

            Assert.assertEquals(1, drivers.size());

            Driver driver = drivers.get(0);
            Assert.assertEquals("SQLite", driver.getType());
            Assert.assertEquals("org.sqlite.SQLiteDataSource", driver.getClassName());
            Assert.assertNull(driver.getJdbcUrl());
            Assert.assertNull(driver.getQueries());
            Assert.assertNotNull(driver.getProperties());

            Properties properties = driver.getProperties();
            Assert.assertNotNull(properties.getProperty("encoding"));
            Assert.assertEquals("UTF-8", properties.getProperty("encoding"));
            Assert.assertNotNull(properties.getProperty("url"));
            Assert.assertEquals("Data Source={Path}/storage/database.db", properties.get("url"));
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }
}

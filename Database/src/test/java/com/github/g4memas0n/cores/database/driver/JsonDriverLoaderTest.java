package com.github.g4memas0n.cores.database.driver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;
import java.io.IOException;
import java.util.List;

public class JsonDriverLoaderTest {

    private JsonDriverLoader loader;

    @Before
    public void setup() {
        this.loader = new JsonDriverLoader();
    }

    public void setupFile() {
        try {
            this.loader.load("database/drivers.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadMissingFileTest() {
        try {
            this.loader.load("database/missing-file.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void loadIllegalFileTest() {
        try {
            this.loader.load("database/drivers-illegal.json");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().endsWith("could not be parsed"));
        }
    }

    @Test
    public void loadCorrectFileTest() {
        try {
            this.loader.load("database/drivers.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void loadDriverAtIllegalTimeTest() {
        this.loader.loadDrivers();
    }

    @Test
    public void loadAllDriverTest() {
        List<Driver> result;
        setupFile();

        result = this.loader.loadDrivers();
        Assert.assertEquals("non-matching result size", 2, result.size());
    }

    @Test
    public void loadSpecificDriverTest() {
        List<Driver> result;
        setupFile();

        result = this.loader.loadDrivers("MySQL");
        Assert.assertEquals("non-matching result size", 1, result.size());
        Assert.assertEquals("non-matching type", "MySQL", result.get(0).getType());
        Assert.assertEquals("non-matching class", com.mysql.cj.jdbc.Driver.class, result.get(0).getSource());
        Assert.assertNotNull("missing jdbc url", result.get(0).getJdbcUrl());
        Assert.assertNotNull("missing queries", result.get(0).getQueries());
        Assert.assertNull("unknown properties", result.get(0).getProperties());

        result = this.loader.loadDrivers("SQLite");
        Assert.assertEquals("non-matching result size", 1, result.size());
        Assert.assertEquals("non-matching type", "SQLite", result.get(0).getType());
        Assert.assertEquals("non-matching class", SQLiteDataSource.class, result.get(0).getSource());
        Assert.assertNotNull("missing properties", result.get(0).getProperties());
        Assert.assertNull("unknown jdbc url", result.get(0).getJdbcUrl());
        Assert.assertNull("unknown queries", result.get(0).getQueries());
    }
}

package com.github.g4memas0n.cores.database.driver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.util.List;

public class JsonDriverLoaderTest {

    private JsonDriverLoader loader;

    @Before
    public void setup() {
        this.loader = new JsonDriverLoader();
    }

    public void setupDrivers() {
        try {
            this.loader.load("database/drivers.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test
    public void illegalFileLoadingTest() {
        try {
            this.loader.load("database/drivers-illegal.json");
            Assert.fail("loaded illegal file without exception");
        } catch (IOException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(ex.getMessage().contains("expected element of drivers to be a json array"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingFileLoadingTest() {
        try {
            this.loader.load("database/missing-file.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception " + ex);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void illegalDriverLoadingTest() {
        this.loader.loadDrivers();
    }

    @Test
    public void getMySQLDriverLoadingTest() {
        this.setupDrivers();

        final List<Driver> drivers = this.loader.loadDrivers("MySQL");

        Assert.assertEquals(1, drivers.size());
        Assert.assertEquals("MySQL", drivers.get(0).getType());
        Assert.assertEquals(com.mysql.cj.jdbc.Driver.class, drivers.get(0).getSource());
        Assert.assertNotNull(drivers.get(0).getJdbcUrl());
        Assert.assertNotNull(drivers.get(0).getQueries());
        Assert.assertNull(drivers.get(0).getProperties());
    }
}

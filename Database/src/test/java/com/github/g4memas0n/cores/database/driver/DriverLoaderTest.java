package com.github.g4memas0n.cores.database.driver;

import org.junit.Assert;
import org.junit.Test;

public class DriverLoaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void missingExtensionLoadTest() {
        DriverLoader.loadFile("database/drivers");
    }

    @Test
    public void successfulDriverLoadTest() {
        final DriverLoader loader = DriverLoader.loadFile("database/drivers.json");

        Assert.assertTrue(loader instanceof JsonDriverLoader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedExtensionLoadTest() {
        DriverLoader.loadFile("database/drivers.txt");
    }
}

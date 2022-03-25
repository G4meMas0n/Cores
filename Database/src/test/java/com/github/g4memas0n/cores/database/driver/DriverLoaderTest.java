package com.github.g4memas0n.cores.database.driver;

import com.github.g4memas0n.cores.database.loader.QueryLoader;
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

        Assert.assertEquals(2, loader.loadDrivers().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedExtensionLoadTest() {
        QueryLoader.loadFile("database/drivers.txt");
    }
}

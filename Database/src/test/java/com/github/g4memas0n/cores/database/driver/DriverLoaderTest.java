package com.github.g4memas0n.cores.database.driver;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;

public class DriverLoaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void loadMissingExtensionTest() {
        try {
            DriverLoader.loadFile("database/drivers");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadUnknownExtensionTest() {
        try {
            DriverLoader.loadFile("database/drivers.txt");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }

    @Test
    public void loadSuccessfulTest() {
        try {
            DriverLoader.loadFile("database/drivers.json");
        } catch (IOException ex) {
            Assert.fail("Unexpected exception: " + ex.getMessage());
        }
    }
}

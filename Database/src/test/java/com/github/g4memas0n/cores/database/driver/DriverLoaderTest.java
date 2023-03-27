package com.github.g4memas0n.cores.database.driver;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;

public class DriverLoaderTest {

    @Test
    public void loadWithExtension() {
        try {
            DriverLoader.getLoader("database/drivers.json");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test
    public void loadWithoutExtension() {
        try {
            DriverLoader.getLoader("database/drivers");
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }
}

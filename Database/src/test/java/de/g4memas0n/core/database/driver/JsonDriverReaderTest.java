package de.g4memas0n.core.database.driver;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class JsonDriverReaderTest {

    @Test
    public void readDriverSuccessTest() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("drivers.json")) {
            Assert.assertNotNull("missing test file", stream);
            new JsonDriverReader().readAll(stream);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test
    public void readDriverMySQLSuccessTest() {
        Driver driver = readDriverByVendor("MySQL");
        Driver.Vendor vendor = driver.getVendor();
        String jdbcUrl;

        Assert.assertEquals("unexpected driver class", com.mysql.cj.jdbc.Driver.class, driver.getDriverClass());
        Assert.assertEquals("unexpected vendor name", "MySQL", vendor.getName());
        Assert.assertTrue("missing vendor version", vendor.hasVersion());
        Assert.assertEquals("unexpected vendor version", 8, vendor.getVersion());

        Assert.assertNotNull("missing jdbc-url", jdbcUrl = driver.getJdbcUrl());
        Assert.assertEquals("unexpected jdbc-url", "jdbc:mysql://{host}:{port}/{database}", jdbcUrl);
        Assert.assertNull("unexpected properties", driver.getProperties());
    }

    @Test
    public void readDriverSQLiteSuccessTest() {
        Driver driver = readDriverByVendor("SQLite");
        Driver.Vendor vendor = driver.getVendor();
        Properties properties;

        Assert.assertEquals("unexpected driver class", org.sqlite.SQLiteDataSource.class, driver.getDriverClass());
        Assert.assertEquals("unexpected vendor name", "SQLite", vendor.getName());
        Assert.assertFalse("unexpected vendor version", vendor.hasVersion());

        Assert.assertNull("unexpected jdbc-url", driver.getJdbcUrl());
        Assert.assertNotNull("missing properties", properties = driver.getProperties());
        Assert.assertEquals("unexpected property", "UTF-8", properties.getProperty("encoding"));
        Assert.assertEquals("unexpected property", "jdbc:sqlite:{path}/sample.db", properties.getProperty("url"));
    }

    private @NotNull Driver readDriverByVendor(@NotNull String vendor) {
        DriverReader reader = new JsonDriverReader();
        List<Driver> drivers = null;

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("drivers.json")) {
            Assert.assertNotNull("missing test file", stream);
            drivers = reader.read(stream, vendor);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }

        Assert.assertEquals("unexpected result size", 1, drivers.size());
        return drivers.get(0);
    }
}

package de.g4memas0n.core.database.driver;

import org.junit.Assert;
import org.junit.Test;
import java.util.Properties;

public class DriverTest {

    @Test
    public void updateJdbcUrlTest() {
        Driver driver = new Driver("com.mysql.cj.jdbc.Driver", new Driver.Vendor("MySQL"));
        driver.setJdbcUrl("jdbc:mysql://{host}:{port}/{database}");

        Properties settings = new Properties();
        settings.setProperty("host", "localhost");
        settings.setProperty("port", "3306");
        settings.setProperty("database", "db");

        Assert.assertEquals("unexpected jdbc-url", "jdbc:mysql://localhost:3306/db", driver.getJdbcUrl(settings));
    }

    @Test
    public void updatePropertiesTest() {
        Properties props, properties = new Properties();
        properties.setProperty("url", "jdbc:sqlite:{path}/sample.db");

        Driver driver = new Driver("org.sqlite.SQLiteDataSource", new Driver.Vendor("SQLite"));
        driver.setProperties(properties);

        Properties settings = new Properties();
        settings.setProperty("path", "//home");

        Assert.assertNotNull("missing properties", props = driver.getProperties(settings));
        Assert.assertEquals("unexpected property", "jdbc:sqlite://home/sample.db", props.getProperty("url"));
    }
}

package com.github.g4memas0n.cores.database.loader;

import org.junit.Assert;
import org.junit.Test;
import java.io.IOException;
import java.io.InputStream;

public class XmlStatementLoaderTest {

    @Test
    public void loadFileTest() {
        final XmlStatementLoader loader = new XmlStatementLoader();
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("database/statements.xml");

        Assert.assertNotNull("Test statements.xml file cannot be found", stream);

        try {
            loader.load(stream);
        } catch (IOException ex) {
            Assert.fail("Exception during loading of test statements.xml file");
        }
    }

    @Test
    public void loadStatementTest() {
        final XmlStatementLoader loader = new XmlStatementLoader();
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("database/statements.xml");

        Assert.assertNotNull("Test statements.xml file cannot be found", stream);

        try {
            loader.load(stream);

            Assert.assertEquals("SQL STATEMENT", loader.load("statement_id"));
            Assert.assertEquals("SQL STATEMENT 1", loader.load("table_name.statement_id_1"));
            Assert.assertEquals("SQL STATEMENT 2", loader.load("table_name.statement_id_2"));
        } catch (IOException ex) {
            Assert.fail("Exception during loading of test statements.xml file");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void loadStatementPreconditionTest() {
        final XmlStatementLoader loader = new XmlStatementLoader();

        loader.load("statement_id");
    }
}

package com.github.g4memas0n.cores.database.query;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JsonQueryLoaderTest {

    private JsonQueryLoader loader;

    @Before
    public void setup() {
        this.loader = new JsonQueryLoader();
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadIllegalFileTest() {
        final String illegal = "[ ]";

        try (InputStream stream = new ByteArrayInputStream(illegal.getBytes(StandardCharsets.UTF_8))) {
            this.loader.load(stream);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadMissingFileTest() {
        try {
            this.loader.load("database/missing.json");
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void loadDriverOnNotLoadedFileTest() {
        this.loader.loadQuery("identifier");
    }

    @Test
    public void loadQueryNoSectionTest() {
        final String file = "{\"first.second.third.fourth\": \"Query\"}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    @Test
    public void loadQueryFirstSectionTest() {
        final String file = "{\"first\": {\"second.third.fourth\": \"Query\"}}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    @Test
    public void loadQueryMiddleSectionTest() {
        final String file = "{\"first.second\": {\"third.fourth\": \"Query\"}}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    @Test
    public void loadQueryLastSectionTest() {
        final String file = "{\"first.second.third\": {\"fourth\": \"Query\"}}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    @Test
    public void loadQueryFirstMultipleSectionTest() {
        final String file = "{\"first\": {\"second\": {\"third.fourth\": \"Query\"}}}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    @Test
    public void loadQueryOuterMultipleSectionTest() {
        final String file = "{\"first\": {\"second.third\": {\"fourth\": \"Query\"}}}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    @Test
    public void loadQueryLastMultipleSectionTest() {
        final String file = "{\"first.second\": {\"third\": {\"fourth\": \"Query\"}}}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    @Test
    public void loadQueryAllSectionTest() {
        final String file = "{\"first\": {\"second\": {\"third\": {\"fourth\": \"Query\"}}}}";
        loadAndTest(file, "first.second.third.fourth", "Query");
    }

    public void loadAndTest(@NotNull final String file, @NotNull final String key, @NotNull final String value) {
        try (InputStream stream = new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8))) {
            this.loader.load(stream);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }

        String query = this.loader.loadQuery(key);
        Assert.assertNotNull("missing query", query);
        Assert.assertEquals("non-matching query", value, query);
    }
}

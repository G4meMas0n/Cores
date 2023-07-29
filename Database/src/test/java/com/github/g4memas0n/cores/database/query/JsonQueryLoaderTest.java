package com.github.g4memas0n.cores.database.query;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JsonQueryLoaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void loadIllegalFileTest() {
        final String illegal = "[ ]";

        try (InputStream stream = new ByteArrayInputStream(illegal.getBytes(StandardCharsets.UTF_8))) {
            new JsonQueryLoader(stream);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
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
            JsonQueryLoader loader = new JsonQueryLoader(stream);
            String query = loader.load(key);

            Assert.assertNotNull("missing query", query);
            Assert.assertEquals("non-matching query", value, query);
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }
}

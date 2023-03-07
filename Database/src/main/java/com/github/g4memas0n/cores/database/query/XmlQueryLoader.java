package com.github.g4memas0n.cores.database.query;

import com.github.g4memas0n.cores.database.DatabaseManager;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * An implementing query loader that loads the mapping from a xml file.<br>
 * This loader will only accept xml files that are formed like this:
 * <pre><code>
 * {@literal <batches>}
 *     {@literal <batch id="identifier1">path/to/batch/file</batch>}
 *     {@literal <batch id="identifier2">path/to/another/batch/file</batch>}
 * {@literal </batches>}
 * {@literal <queries>}
 *     {@literal <query id="identifier3">SQL Query</query>}
 *     {@literal <query id="identifier4">Another SQL Query</query>}
 * {@literal </queries>}
 * </code></pre>
 *
 * @since 1.0.0
 */
public class XmlQueryLoader extends QueryLoader {

    private final DocumentBuilderFactory factory;
    private Element batches;
    private Element queries;

    /**
     * Public constructor for creating a query loader that reads xml files.
     */
    public XmlQueryLoader() {
        this.factory = DocumentBuilderFactory.newInstance();

        try {
            this.factory.setIgnoringComments(true);
            this.factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ex) {
            DatabaseManager.getLogger().log(Level.WARNING, "Failed to enable secure xml processing", ex);
        }
    }

    @Override
    public void load(@NotNull final String path) throws IOException {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        Preconditions.checkArgument(stream != null, "missing file at " + path);

        try {
            final DocumentBuilder builder = this.factory.newDocumentBuilder();
            final Document document = builder.parse(stream);
            NodeList nodes;

            document.normalize();

            nodes = document.getElementsByTagName("batches");
            if (nodes.getLength() > 0) {
                if (nodes.getLength() > 1) {
                    throw new SAXException("expected maximal one branches tag");
                }

                this.batches = (Element) nodes.item(0);
            }

            nodes = document.getElementsByTagName("queries");
            if (nodes.getLength() > 0) {
                if (nodes.getLength() > 1) {
                    throw new SAXException("expected maximal one queries tag");
                }

                this.queries = (Element) nodes.item(0);
            }

            this.path = path;
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("unable to parse queries file at " + path + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected @Nullable String loadBatch(@NotNull final String identifier) {
        Preconditions.checkState(this.batches != null, "no batches have been loaded yet");
        final NodeList nodes = this.batches.getElementsByTagName("batch");
        Element batch;

        for (int index = 0; index < nodes.getLength(); index++) {
            batch = (Element) nodes.item(0);

            if (batch.getAttribute("id").equals(identifier)) {
                return batch.getTextContent();
            }
        }

        return null;
    }

    @Override
    protected @Nullable String loadQuery(@NotNull final String identifier) {
        Preconditions.checkState(this.queries != null, "no queries have been loaded yet");
        final NodeList nodes = this.queries.getElementsByTagName("query");
        Element query;

        for (int index = 0; index < nodes.getLength(); index++) {
            query = (Element) nodes.item(index);

            if (query.getAttribute("id").equals(identifier)) {
                return query.getTextContent();
            }
        }

        return null;
    }
}

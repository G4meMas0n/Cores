package com.github.g4memas0n.cores.database.loader;

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
 * {@literal <any_root_tag>}
 *     {@literal <queries>}
 *         {@literal <query id="identifier">SQL Query</query>}
 *         {@literal <query id="another.identifier">Another SQL Query</query>}
 *     {@literal </queries>}
 * {@literal </any_root_tag>}
 * </code></pre>
 */
public class XmlQueryLoader extends QueryLoader {

    private final DocumentBuilderFactory factory;
    private Element root;

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
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(path);
        Preconditions.checkArgument(stream != null, "Missing file at path " + path);

        try {
            final DocumentBuilder builder = this.factory.newDocumentBuilder();
            final Document document = builder.parse(stream);

            document.normalize();

            final NodeList nodes = document.getElementsByTagName("queries");

            if (nodes.getLength() > 0) {
                this.root = (Element) nodes.item(0);

                if (this.root.getElementsByTagName("query").getLength() == 0) {
                    this.root = null;
                    throw new SAXException("Expected at least one query tag, but count was zero");
                }
            } else {
                throw new SAXException("Expected queries tag in the xml document, but was missing");
            }
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("Unable to parse queries file at " + path, ex);
        }
    }

    @Override
    public @Nullable String loadQuery(@NotNull final String identifier) {
        Preconditions.checkState(this.root != null, "The queries file has not been loaded yet");
        final NodeList queries = this.root.getElementsByTagName("query");

        for (int index = 0; index < queries.getLength(); index++) {
            Element query = (Element) queries.item(index);

            if (query.getAttribute("id").equals(identifier)) {
                return query.getTextContent();
            }
        }

        return null;
    }
}

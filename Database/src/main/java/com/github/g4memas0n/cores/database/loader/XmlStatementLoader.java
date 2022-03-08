package com.github.g4memas0n.cores.database.loader;

import com.github.g4memas0n.cores.database.DatabaseManager;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class XmlStatementLoader extends StatementLoader {

    protected final DocumentBuilderFactory factory;
    protected Element root;

    public XmlStatementLoader() {
        this.factory = DocumentBuilderFactory.newInstance();

        try {
            this.factory.setIgnoringComments(true);
            this.factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ex) {
            DatabaseManager.getLogger().log(Level.WARNING, "Failed to enable secure xml processing", ex);
        }
    }

    @Override
    public void load(@NotNull final InputStream file) throws IOException {
        try {
            final DocumentBuilder builder = this.factory.newDocumentBuilder();
            final Document document = builder.parse(file);

            document.normalize();

            final Element root = document.getDocumentElement();

            if (root.getTagName().equals("sql")) {
                if (!root.hasAttribute("type")) {
                    throw new SAXException("Expected attribute type in sql tag, but was missing");
                }

                this.root = root;
                this.type = root.getAttribute("type");

                if (root.hasAttribute("version")) {
                    try {
                        this.version = Integer.parseInt(root.getAttribute("version"));
                    } catch (NumberFormatException ex) {
                        throw new SAXException("Expected attribute version to be an integer, but was not a number");
                    }
                } else {
                    this.version = 0;
                }
            } else {
                throw new SAXException("Expected root tag to be sql, but was " + root.getTagName());
            }
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("Unable to parse statements file", ex);
        }
    }

    protected @Nullable String load(@NotNull final Element parent, @NotNull final String identifier) {
        final NodeList children = parent.getChildNodes();

        for (int child = 0; child < children.getLength(); child++) {
            final Node node = children.item(child);

            if (node != null && node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("statements")) {
                final NodeList statements = ((Element) node).getElementsByTagName("statement");

                for (int index = 0; index < statements.getLength(); index++) {
                    if (statements.item(index).getNodeType() == Node.ELEMENT_NODE) {
                        Element statement = (Element) statements.item(index);

                        if (statement.hasAttribute("id")) {
                            if (statement.getAttribute("id").equalsIgnoreCase(identifier)) {
                                return statement.getTextContent();
                            }
                        } else {
                            DatabaseManager.getLogger().warning("Encountered invalid statement entry for type "
                                    + this.type + " version " + this.version + ": Entry must have an id attribute");
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    protected @Nullable String load(@NotNull final String identifier) {
        Preconditions.checkState(this.root != null, "The statements file has not been loaded yet");
        final String[] parts = identifier.split("\\.", 2);

        if (parts.length > 1) {
            final Node node = this.root.getElementsByTagName("tables").item(0);

            if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                final NodeList tables = ((Element) node).getElementsByTagName("table");

                for (int index = 0; index < tables.getLength(); index++) {
                    if (tables.item(index).getNodeType() == Node.ELEMENT_NODE) {
                        Element table = (Element) tables.item(index);

                        if (table.hasAttribute("name")) {
                            if (table.getAttribute("name").equalsIgnoreCase(parts[0])) {
                                return this.load(table, parts[1]);
                            }
                        } else {
                            DatabaseManager.getLogger().warning("Encountered invalid table entry for type "
                                    + this.type + " version " + this.version + ": Entry must have an name attribute");
                        }
                    }
                }
            }

            return null;
        }

        return this.load(this.root, identifier);
    }
}

package org.n52.wps.server.database;

import java.io.StringReader;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author abramhall
 */
public class XPathXMLParser {

    // FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF).
    private static final String UTF8_BOM = "\uFEFF";
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathXMLParser.class);

    private final String xml;
    private final NamespaceContext context;
    private final Document document;

    public XPathXMLParser(String xml) throws XPathExpressionException {
        this.xml = removeUT8BOM(xml);

        InputSource input = new InputSource(new StringReader(xml));
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        this.document = (Document) xpath.evaluate("/", input, XPathConstants.NODE);

        this.context = new WPSNamespaceContext(document);
    }

    public Document getDocument() {
        return document;
    }

    public NamespaceContext getContext() {
        return context;
    }

    public String getString(String xpathString) {
        return (String) parseXML(xpathString, XPathConstants.STRING);
    }

    public Node getNode(String xpathString) {
        return (Node) parseXML(xpathString, XPathConstants.NODE);
    }

    public NodeList getNodeList(String xpathString) {
        return (NodeList) parseXML(xpathString, XPathConstants.NODESET);
    }

    private Object parseXML(String xpathString, QName returnType) {
        Object result = null;
        InputSource input = new InputSource(new StringReader(xml));
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext(context);

        try {
            result = xpath.evaluate(xpathString, document, returnType);
        } catch (XPathExpressionException ex) {
            LOGGER.error("Error encountered parsing XML", ex);
        }

        return result;
    }

    private String removeUT8BOM(String string) {
        if (string.startsWith(UTF8_BOM)) {
            string = string.substring(1);
        }
        return string;
    }
}

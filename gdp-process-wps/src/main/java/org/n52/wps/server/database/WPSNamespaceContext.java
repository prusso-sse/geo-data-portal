package org.n52.wps.server.database;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author abramhall
 */
public class WPSNamespaceContext implements NamespaceContext {

    private Map<String, String> prefix2Uri = new HashMap<String, String>();
    private Map<String, String> uri2Prefix = new HashMap<String, String>();

    public WPSNamespaceContext(Document document) {
        examineNode(document.getFirstChild(), true);
    }

    private void examineNode(Node node, boolean attributesOnly) {
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            storeAttribute((Attr) attribute);
        }

        if (!attributesOnly) {
            NodeList chields = node.getChildNodes();
            for (int i = 0; i < chields.getLength(); i++) {
                Node chield = chields.item(i);
                if (chield.getNodeType() == Node.ELEMENT_NODE) {
                    examineNode(chield, false);
                }
            }
        }
    }

    private void storeAttribute(Attr attribute) {
        if (attribute.getNamespaceURI() != null && attribute.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
//                if (attribute.getNodeName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
//                    putInCache(DEFAULT_NS, attribute.getNodeValue());
//                } else {
            putInCache(attribute.getLocalName(), attribute.getNodeValue());
//                }
        }
    }

    private void putInCache(String prefix, String uri) {
        prefix2Uri.put(prefix, uri);
        uri2Prefix.put(uri, prefix);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return prefix2Uri.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return uri2Prefix.get(namespaceURI);
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException("This implementation only allows a one to one prefix to namespace ration.");
    }

}

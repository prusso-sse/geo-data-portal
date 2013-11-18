package gov.usgs.cida.gdp.wps.util.xml;


import javax.xml.namespace.QName;

import org.eclipse.xsd.XSDElementDeclaration;
import org.geotools.xml.impl.DocumentHandler;
import org.geotools.xml.impl.ElementHandler;
import org.geotools.xml.impl.Handler;


/**
 * Factory used to create element handler objects during the processing of an
 * instance document.
 *
 * @author Justin Deoliveira,Refractions Reserach Inc.,jdeolive@refractions.net
 *
 *
 *
 *
 * @source $URL$
 */
public interface GDPHandlerFactory {
    /**
     * Creates a handler for the root element of a document.
     */
    DocumentHandler createDocumentHandler(GDPParserHandler parser);

    /**
     * Creates an element hander for a global or top level element in a document.
     *
     * @param qName The qualified name identifying the element.
     * @param parent The parent handler.
     * @param parser The content handler driving the parser.
     *
     * @return A new element handler, or null if one could not be created.
     */
    ElementHandler createElementHandler(QName qName, Handler parent, GDPParserHandler parser);

    /**
     * Creates a handler for a particular element in a document.
     *
     * @param element The schema component which represents the declaration
     * of the element.
     * @param parent The parent handler.
     * @param parser The content handler driving the parser.
     *
     * @return A new element handler, or null if one could not be created.
     */
    ElementHandler createElementHandler(XSDElementDeclaration element, Handler parent,
        GDPParserHandler parser);

    /**
     * Creates a handler for a particular element in a document.
     *
     * @param attribute The schema component which represents the declaration
     * of the attribute.
     * @param parent The parent handler.
     * @param parser The content handler driving the parser.
     *
     * @return A new attribute handler, or null if one could not be created.
     */

    //AttributeHandler createAttributeHandler(XSDAttributeDeclaration attribute, Handler parent, GDPParserHandler parser );
}

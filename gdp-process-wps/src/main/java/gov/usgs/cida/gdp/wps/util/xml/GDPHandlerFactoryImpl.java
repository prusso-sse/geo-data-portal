package gov.usgs.cida.gdp.wps.util.xml;

import javax.xml.namespace.QName;

import org.eclipse.xsd.XSDElementDeclaration;
import org.geotools.xml.SchemaIndex;
import org.geotools.xml.impl.DocumentHandler;
import org.geotools.xml.impl.ElementHandler;
import org.geotools.xml.impl.Handler;


/**
 * 
 *
 * @source $URL$
 */
public class GDPHandlerFactoryImpl implements GDPHandlerFactory {
    public DocumentHandler createDocumentHandler(GDPParserHandler parser) {
        return new GDPDocumentHandlerImpl(this, parser);
    }

    public ElementHandler createElementHandler(QName qName, Handler parent, GDPParserHandler parser) {
        SchemaIndex index = parser.getSchemaIndex();

        //look up the element in the schema
        XSDElementDeclaration element = index.getElementDeclaration(qName);

        if (element != null) {
            return createElementHandler(element, parent, parser);
        }

        return null;
    }

    public ElementHandler createElementHandler(XSDElementDeclaration element, Handler parent,
        GDPParserHandler parser) {
        return new GDPElementHandlerImpl(element, parent, parser);
    }
}

package gov.usgs.cida.gdp.wps.util.xml;

import javax.xml.namespace.QName;

import org.eclipse.xsd.XSDSchemaContent;
import org.geotools.xml.InstanceComponent;
import org.geotools.xml.Node;
import org.geotools.xml.impl.DocumentHandler;
import org.geotools.xml.impl.Handler;
import org.geotools.xml.impl.HandlerImpl;


/**
 * 
 *
 * @source $URL$
 */
public class GDPDocumentHandlerImpl extends HandlerImpl implements DocumentHandler {
    /** factory used to create a handler for the root element **/
    GDPHandlerFactory factory;

    /** root node of the parse tree */
    Node tree;

    //ElementHandler handler;

    /** the parser */
    GDPParserHandler parser;

    public GDPDocumentHandlerImpl(GDPHandlerFactory factory, GDPParserHandler parser) {
        this.factory = factory;
        this.parser = parser;
    }

    public XSDSchemaContent getSchemaContent() {
        return null;
    }

    public InstanceComponent getComponent() {
        return null;
    }

    public Object getValue() {
        //jsut return the root of the parse tree's value
        if (tree != null) {
            return tree.getValue();
        }

        //    	//just return the root handler value
        //        if (handler != null) {
        //            return handler.getValue();
        //        }
        return null;
    }

    public Node getParseNode() {
        return tree;
    }

    public Handler createChildHandler(QName qName) {
        return factory.createElementHandler(qName, this, parser);
    }

    //    public List getChildHandlers() {
    //    	if ( handler == null ) {
    //    		return Collections.EMPTY_LIST;
    //    	}
    //    	
    //    	ArrayList list = new ArrayList();
    //    	list.add( handler );
    //    	
    //    	return list;
    //    }
    public void startChildHandler(Handler child) {
        this.tree = child.getParseNode();

        //this.handler = (ElementHandler) child;
    }

    public void endChildHandler(Handler child) {
        //this.handler = null;
    }

    public Handler getParentHandler() {
        //always null, this is the root handler
        return null;
    }

    //    public ElementHandler getDocumentElementHandler() {
    //        return handler;
    //    }
    
    public void startDocument() {
    }
    
    public void endDocument() {
    }
}

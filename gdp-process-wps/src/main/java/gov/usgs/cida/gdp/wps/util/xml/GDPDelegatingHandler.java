package gov.usgs.cida.gdp.wps.util.xml;


import javax.xml.namespace.QName;

import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDSchemaContent;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.InstanceComponent;
import org.geotools.xml.Node;
import org.geotools.xml.ParserDelegate;
import org.geotools.xml.impl.DocumentHandler;
import org.geotools.xml.impl.ElementHandler;
import org.geotools.xml.impl.ElementImpl;
import org.geotools.xml.impl.Handler;
import org.geotools.xml.impl.NodeImpl;
import org.picocontainer.MutablePicoContainer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 *
 * @source $URL$
 */
public class GDPDelegatingHandler implements DocumentHandler, ElementHandler {

    ParserDelegate delegate;
    Handler parent;
    QName elementName;
    NodeImpl parseTree;
    
    GDPDelegatingHandler( ParserDelegate delegate, QName elementName, Handler parent) {
        this.delegate = delegate;
        this.parent = parent;
        this.elementName = elementName;
        
        //create a parse tree
        XSDElementDeclaration e = XSDFactory.eINSTANCE.createXSDElementDeclaration();
        e.setTargetNamespace( elementName.getNamespaceURI() );
        e.setName( elementName.getLocalPart() );
        
        ElementImpl instance = new ElementImpl( e );
        instance.setName( elementName.getLocalPart() );
        instance.setNamespace( elementName.getNamespaceURI() );
        
        parseTree = new NodeImpl( instance );
    }
    
    public void setContext(MutablePicoContainer context) {
    }
    
    public MutablePicoContainer getContext() {
        return null;
    }
    
    
    public XSDElementDeclaration getElementDeclaration() {
        return ((ElementInstance)parseTree.getComponent()).getElementDeclaration();
    }
    
    public Handler getParentHandler() {
        return parent;
    }
    
    public Handler createChildHandler(QName name) {
        return new GDPDelegatingHandler( delegate, name, this );
    }

    public void startChildHandler(Handler child) {
    }
    
    public void endChildHandler(Handler child) {
    }

    public InstanceComponent getComponent() {
        return null;
    }

    public Node getParseNode() {
        return parseTree;
    }

    public XSDSchemaContent getSchemaContent() {
        return null;
    }
    
    public void startDocument() throws SAXException {
        delegate.startDocument();
    }
    
    public void endDocument() throws SAXException {
        delegate.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        delegate.startPrefixMapping(prefix, uri);
    }

    public void startElement(QName name, Attributes attributes)
        throws SAXException {
        
        if ( !( parent instanceof GDPDelegatingHandler ) ) {
            parent.startChildHandler( this );
        }
        
        delegate.startElement(name.getNamespaceURI(), name.getLocalPart(), 
            qname(name) , attributes);
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        delegate.characters( ch, start, length );
    }

    public void endElement(QName name) throws SAXException {
        delegate.endElement( name.getNamespaceURI(), name.getLocalPart(), qname( name ) );
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        delegate.endPrefixMapping(prefix);
    }

    String qname( QName name ) {
        return name.getNamespaceURI() != null ? name.getPrefix() + ":" + name.getLocalPart() : name.getLocalPart();
    }
}

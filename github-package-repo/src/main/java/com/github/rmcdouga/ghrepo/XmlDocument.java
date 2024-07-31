package com.github.rmcdouga.ghrepo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlDocument {
	private final XPath xpath = XPathFactory.newInstance().newXPath();
	
	private record Namespace(String nsPrefix, String nsUri) {};
	
	private final Document document;
	
    private XmlDocument(Document document, List<Namespace> namespaces) {
		this.document = document;
		if (namespaces.size() > 0) {
			xpath.setNamespaceContext(createNamespaceContext(namespaces));
		}
	}

	public static XmlDocument create(Path file) throws XmlDocumentException {
    	return builder(file).build();
    }

    public static XmlDocument create(byte[] bytes) throws XmlDocumentException {
    	return builder(bytes).build();
    }

    public static XmlDocument create(InputStream is) throws XmlDocumentException {
    	return new XmlDocumentBuilder(is).build();
    }

    public static XmlDocumentBuilder builder(InputStream is) throws XmlDocumentException {
    	return new XmlDocumentBuilder(is);
    }
    
    public static XmlDocumentBuilder builder(byte[] bytes) throws XmlDocumentException {
   		return new XmlDocumentBuilder(new ByteArrayInputStream(bytes));
    }
    
    public static XmlDocumentBuilder builder(Path file) throws XmlDocumentException {
    	try {
			return new XmlDocumentBuilder(Files.newInputStream(file));
		} catch (IOException e) {
			throw new XmlDocumentException("Error while reading XmlDocument from file '%s'.".formatted(file.toString()), e);
		}
    }
    
    public String getString(String xPathExpr) throws XmlDocumentException {
    	try {
			return xpath.evaluate(xPathExpr, document);
		} catch (XPathExpressionException e) {
			throw new XmlDocumentException("Error while processing xpath(" + xPathExpr + ").", e);
		}
    }

	public List<String> getStrings(String xPathExpr) throws XmlDocumentException {
		try {
			NodeList nodes = (NodeList) xpath.evaluate(xPathExpr, document, XPathConstants.NODESET);
			List<String> result = new ArrayList<>(nodes.getLength());
			for (int i = 0; i < nodes.getLength(); i++) {
				result.add(nodes.item(i).getTextContent());
			}
			return Collections.unmodifiableList(result);
		} catch (XPathExpressionException | DOMException e) {
			throw new XmlDocumentException("Error while processing xpath(" + xPathExpr + ").", e);
		}
	}

    public Boolean getBoolean(String xPathExpr) throws XmlDocumentException {
    	return Boolean.valueOf(getString(xPathExpr));
    }

	@Override
	public String toString() {
		return getStringFromDocument(document);
	}

	//method to convert Document to String
	public static String getStringFromDocument(Document doc)
	{
	    try
	    {
	       DOMSource domSource = new DOMSource(doc);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory tf = TransformerFactory.newInstance();
	       Transformer transformer = tf.newTransformer();
	       transformer.transform(domSource, result);
	       return writer.toString();
	    }
	    catch(TransformerException e)
	    {
			throw new XmlDocumentException("Error while converting XmlDocument to String.", e);
	    }
	} 

	private static NamespaceContext createNamespaceContext(List<Namespace> namespaces) {
		 return new NamespaceContext() {
		        public String getNamespaceURI(String prefix) {
		        	for (Namespace ns: namespaces) {
		        		if (ns.nsPrefix.equals(prefix)) {
		        			return ns.nsUri;
		        		}
		        	}
		        	return null;
		        }
		       
		        // Dummy implementation - not used!
		        public Iterator<String> getPrefixes(String val) {
		        	return namespaces.stream()
		        					 .filter(n->n.nsUri.equals(val))
		        					 .map(Namespace::nsPrefix)
		        					 .toList()
		        					 .iterator();
		        }
		       
		        // Dummy implementation - not used!
		        public String getPrefix(String uri) {
		        	for (Namespace ns: namespaces) {
		        		if (ns.nsUri.equals(uri)) {
		        			return ns.nsPrefix;
		        		}
		        	}
		        	return null;
		        }
		    };
	}
	
	public static class XmlDocumentBuilder {
		private final InputStream is;
		private final List<Namespace> namespaces = new ArrayList<>();
		private String defaultNamespacePrefix = null;

		public XmlDocumentBuilder(InputStream is) {
			this.is = is;
		}
		
		// "mvn", "http://maven.apache.org/SETTINGS/1.0.0"
		public XmlDocumentBuilder registerNs(String nsPrefix, String nsUrl) {
			namespaces.add(new Namespace(nsPrefix, nsUrl));
			return this;
		}
		
		public XmlDocumentBuilder defaultNsPrefix(String nsPrefix) {
			defaultNamespacePrefix = nsPrefix;
			return this;
		}

		public XmlDocument build() {
	    	try {
				DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
				xmlFactory.setNamespaceAware(true);
				Document document = xmlFactory.newDocumentBuilder().parse(is);
				if (defaultNamespacePrefix != null) {
					namespaces.add(constructDefaultNamespace(document, defaultNamespacePrefix));
				}
				return new XmlDocument(document, namespaces);
			} catch (SAXException | IOException | ParserConfigurationException e) {
				throw new XmlDocumentException("Error while parsing XmlDocument.", e);
			}
		}
		
		// Construct a Namespace from the default namespace declared in the document
		private static Namespace constructDefaultNamespace(Document document, String defaultNamespacePrefix) {
			String defaultNamespaceUrl = getDefaultNamespace(document).orElseThrow(()->new IllegalStateException("Can't set default namespace to '" + defaultNamespacePrefix + "', no default namespace found."));
			return new Namespace(defaultNamespacePrefix, defaultNamespaceUrl);
		}

		// Extracts the default namespace URL out of the document (if there is one)
		private static Optional<String> getDefaultNamespace(Document document) {
			return Optional.ofNullable(document.getDocumentElement().getAttributes())
						   .flatMap(XmlDocumentBuilder::getDefaultNamespaceValue);
		}

		// get the defaultNamespace URL value from the NamedNodeMap
		private static Optional<String> getDefaultNamespaceValue(NamedNodeMap attributes) {
			return IntStream.range(0, attributes.getLength())
	 			 	  		.<Node>mapToObj(i->attributes.item(i))		// Loop through all the attributes
	 			 	  		.filter(n->n.getNodeName().equals("xmlns"))	// Remove attributes that are not the default namespace 
	 			 	  		.map(Node::getNodeValue)					// Get the value
	 			 	  		.findFirst();
		}
	}
	
	@SuppressWarnings("serial")
	public static class XmlDocumentException extends RuntimeException {

		public XmlDocumentException() {
		}

		public XmlDocumentException(String message, Throwable cause) {
			super(message, cause);
		}

		public XmlDocumentException(String message) {
			super(message);
		}

		public XmlDocumentException(Throwable cause) {
			super(cause);
		}
    }
}

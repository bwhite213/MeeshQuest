/*
 * @(#)XmlUtility.java        2.0 2007/03/14
 *
 * Copyright Ben Zoller (University of Maryland, College Park), 2007
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */

package cmsc420.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import javax.naming.OperationNotSupportedException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Wrapper class for XML API validation and parsing methods. Shortcut methods
 * for reading, writing, parsing, validating, and transforming XML files.
 * 
 * @author Justin Manweiler (William and Mary)
 * @author Ben Zoller (added schema validation functionality)
 * @version 2.0, 14 Mar 2007
 */
public class XmlUtility {

	/**
	 * Can be used to create a new instance of a DOM Document to build a DOM
	 * tree.
	 */
	private static DocumentBuilder documentBuilder = null;

	/**
	 * Factory which generates new instances of DocumentBuilder.
	 */
	private static final DocumentBuilderFactory documentBuilderFactory;

	/**
	 * Used to check if a Document is valid XML.
	 */
	private static final DefaultHandler defaultHandler;

	/**
	 * Factory which generates a SAX based parser to parse XML documents.
	 */
	private static final SAXParserFactory saxFactory;

	/**
	 * Factory which generates a schema. Can load a schema from a file.
	 */
	private static SchemaFactory schemaFactory;

	/**
	 * Factory used to transform XML documents to output stream/file.
	 */
	private static final TransformerFactory transformerFactory;

	/**
	 * Transformer used to transform XML documents. Able to set encoding and
	 * indentation.
	 */
	private static Transformer transformer = null;

	/**
	 * Initializes XML processing and validation settings.
	 */
	static {
		defaultHandler = new DefaultHandler();

		documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true); // required for Java 6

		saxFactory = SAXParserFactory.newInstance();
		saxFactory.setValidating(true);

		transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number", 2);

		schemaFactory = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	}

	/**
	 * Private constructor. All class methods are static.
	 * 
	 */
	private XmlUtility() {
	}

	/**
	 * Gets a DocumentBuilder from the DocumentBuilderFactory
	 * 
	 * @return DocumentBuilder capable of generating a new XML Document.
	 * @throws ParserConfigurationException
	 *             a serious configuration error
	 */
	public static DocumentBuilder getDocumentBuilder()
			throws ParserConfigurationException {
		if (documentBuilder == null) {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		}

		return documentBuilder;
	}

	/**
	 * Gets a Transformer from the TransformerFactory
	 * 
	 * @return transformer used to transform a source tree to an output
	 *         stream/file
	 * @throws TransformerConfigurationException
	 *             a serious configuration error
	 */
	public static Transformer getTransformer()
			throws TransformerConfigurationException {
		if (transformer == null) {
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		}

		return transformer;
	}

	/**
	 * Checks if the file is valid XML (note: does not check it against a
	 * referenced schema)
	 * 
	 * @param file
	 *            checked if is a valid XML file
	 * @return <code>true</code> if the file is valid XML, <code>false</code>
	 *         otherwise
	 */
	public static boolean isValidXml(final File file) {
		try {
			/* try to parse the file */
			saxFactory.newSAXParser().parse(file, defaultHandler);
		} catch (Exception e) {
			/* file is not valid XML */
			return false;
		}

		/* file is valid XML */
		return true;
	}

	/**
	 * Parses a given XML file and returns the DOM Document tree.
	 * 
	 * @param file
	 *            XML file to be parsed
	 * @return Document tree representing XML file
	 * @throws ParserConfigurationException
	 *             a serious configuration error
	 * @throws SAXException
	 *             encapsulates a SAX parsing error
	 * @throws IOException
	 *             problem opening/reading the file
	 */
	public static Document parse(final File file)
			throws ParserConfigurationException, SAXException, IOException {
		return getDocumentBuilder().parse(file);
	}

	/**
	 * Parses a given XML input stream and returns the DOM Document tree.
	 * 
	 * @param inputStream
	 *            XML stream to be parsed
	 * @return Document tree representing XML input stream
	 * @throws ParserConfigurationException
	 *             a serious configuration error
	 * @throws SAXException
	 *             encapsulates a SAX parsing error
	 * @throws IOException
	 *             problem reading the input stream
	 */
	public static Document parse(final InputStream inputStream)
			throws ParserConfigurationException, SAXException, IOException {
		return getDocumentBuilder().parse(inputStream);
	}

	/**
	 * Reads a DOM Document tree to a piped reader.
	 * 
	 * @param document
	 *            DOM Document tree
	 * @return piped reader which reads XML document
	 * @throws IOException
	 *             problem reading the document
	 * @throws TransformerException
	 *             a problem during the transformation process
	 */
	public static Reader read(final Document document) throws IOException,
			TransformerException {
		final PipedWriter pipedWriter = new PipedWriter();
		final PipedReader pipedReader = new PipedReader(pipedWriter);

		final DOMSource source = new DOMSource(document);
		final StreamResult result = new StreamResult(pipedWriter);

		final Runnable runnable = new Runnable() {
			public void run() {
				try {
					getTransformer().transform(source, result);
					pipedWriter.close();
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}
		};

		new Thread(runnable).start();

		return pipedReader;
	}

	/**
	 * Streams a DOM Document tree to a piped input stream.
	 * 
	 * @param document
	 *            DOM Document tree
	 * @return piped input stream which reads XML document
	 * @throws IOException
	 *             problem streaming the document
	 * @throws TransformerException
	 *             a problem during the transformation process
	 */
	public static InputStream stream(final Document document)
			throws IOException, TransformerException {
		final PipedOutputStream pipedOutputStream = new PipedOutputStream();
		final PipedInputStream pipedInputStream = new PipedInputStream(
				pipedOutputStream);

		final DOMSource source = new DOMSource(document);
		final StreamResult result = new StreamResult(pipedOutputStream);

		final Runnable runnable = new Runnable() {
			public void run() {
				try {
					getTransformer().transform(source, result);
					pipedOutputStream.close();
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}
		};

		new Thread(runnable).start();

		return pipedInputStream;
	}

	/**
	 * Prints a DOM Document tree to System.out
	 * 
	 * @param document
	 *            DOM Document tree which represents an XML file
	 * @throws TransformerException
	 *             a problem during the transformation process
	 */
	public static void print(final Document document)
			throws TransformerException {
		final DOMSource source = new DOMSource(document);
		final StreamResult result = new StreamResult(new OutputStreamWriter(
				System.out));
		getTransformer().transform(source, result);
	}

	/**
	 * Writes a DOM Document tree to an XML file.
	 * 
	 * @param document
	 *            DOM Document tree
	 * @param outFile
	 *            XML output file
	 * @throws TransformerException
	 *             a problem during the transformation process
	 * @throws FileNotFoundException
	 *             output file is inaccessible
	 */
	public static void write(final Document document, final File outFile)
			throws TransformerException, FileNotFoundException {
		final DOMSource source = new DOMSource(document);
		final StreamResult result = new StreamResult(new OutputStreamWriter(
				new FileOutputStream(outFile)));
		getTransformer().transform(source, result);
	}

	/**
	 * Writes a DOM Document tree to a given XML output stream.
	 * 
	 * @param document
	 *            DOM Document tree
	 * @param outputStream
	 *            XML output stream
	 * @throws TransformerException
	 *             a problem during the transformation process
	 */
	public static void write(final Document document,
			final OutputStream outputStream) throws TransformerException {
		final DOMSource source = new DOMSource(document);
		final StreamResult result = new StreamResult(new OutputStreamWriter(
				outputStream));
		getTransformer().transform(source, result);
	}

	/**
	 * Transforms a given XML file with an XSLT file to an HTML file.
	 * 
	 * @param xmlFile
	 *            XML input file to be transformed
	 * @param xsltFile
	 *            XSLT file which determines how the XML is transformed to HTML
	 * @param htmlFile
	 *            HTML output file
	 * @throws TransformerException
	 *             a problem during the transformation process
	 * @throws FileNotFoundException
	 *             XML output file is inaccessible
	 */
	public static void transform(final File xmlFile, final File xsltFile,
			final File htmlFile) throws TransformerException,
			FileNotFoundException {
		transform(new StreamSource(xmlFile), xsltFile, htmlFile);
	}
	
	/**
	 * Transforms a given XML Document with an XSLT file to an HTML file.
	 * 
	 * @param xmlDom
	 *            XML input Document to be transformed
	 * @param xsltFile
	 *            XSLT file which determines how the XML is transformed to HTML
	 * @param htmlFile
	 *            HTML output file
	 * @throws TransformerException
	 *             a problem during the transformation process
	 * @throws FileNotFoundException
	 *             XML output file is inaccessible
	 */
	public static void transform(final Document xmlDom, final File xsltFile,
			final File htmlFile) throws TransformerException,
			FileNotFoundException {
		transform(new DOMSource(xmlDom), xsltFile, htmlFile);
	}

	/**
	 * Transforms a given XML source with an XSLT file to an HTML file.
	 * 
	 * @param xmlSource
	 *            XML input source to be transformed
	 * @param xsltFile
	 *            XSLT file which determines how the XML is transformed to HTML
	 * @param htmlFile
	 *            HTML output file
	 * @throws TransformerException
	 *             a problem during the transformation process
	 * @throws FileNotFoundException
	 *             XML output file is inaccessible
	 */
	public static void transform(final Source xmlSource, final File xsltFile,
			final File htmlFile) throws TransformerException,
			FileNotFoundException {
		final StreamResult result = new StreamResult(new OutputStreamWriter(
				new FileOutputStream(htmlFile)));

		final Transformer xslTransformer = transformerFactory
				.newTransformer(new StreamSource(xsltFile));
		xslTransformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		xslTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

		xslTransformer.transform(xmlSource, result);
	}
	
	/**
	 * Validates an XML file against an XML schema. Throws a SAXException if the
	 * XML is invalid. Else returns the XML file as a org.w3c.dom.Document
	 * 
	 * @param xmlFile
	 *            XML file to be validated
	 * @param schemaSource
	 *            XML Schema source - can be created this way: Source
	 *            schemaSource = new StreamSource(new File(schemaFileName));
	 * @return XML file as a org.w3c.dom.Document if successful
	 * @throws SAXException
	 *             encapsulates a problem validating the document
	 * @throws IOException
	 *             problem opening/reading the file
	 * @throws ParserConfigurationException
	 *             a serious configuration error
	 */
	public static Document validate(final File xmlFile,
			final Source schemaSource) throws SAXException, IOException,
			ParserConfigurationException {
		/* parse an XML document into a DOM tree */
		final Document document = parse(xmlFile);

		/* load an XML Schema, represented by a schema instance */
		final Schema schema = schemaFactory.newSchema(schemaSource);

		/* create a Validator instance */
		final Validator validator = schema.newValidator();

		/* validate the DOM tree, throw an exception if there is a problem */
		validator.validate(new DOMSource(document));

		/* return the valid document */
		return document;
	}

	/**
	 * Validates an input file against an internal schema. First parses the
	 * stream into a DOM Document. Then it gets the schema file (works for local
	 * or external (online) schemas) and validates the Document against the
	 * schema.
	 * 
	 * @param xmlFile
	 *            XML file containing schema reference
	 * @return representation of XML document tree
	 * @throws SAXException
	 *             encapsulates problem validating the XML document
	 * @throws IOException
	 *             problem reading the XML file
	 * @throws ParserConfigurationException
	 *             a serious configuration error
	 * @throws OperationNotSupportedException
	 */
	public static Document validateNoNamespace(final File xmlFile)
			throws SAXException, IOException, ParserConfigurationException {
		return validateNoNamespace(new FileInputStream(xmlFile));
	}

	/**
	 * Validates an input stream against an internal schema. First parses the
	 * stream into a DOM Document. Then it gets the schema file (works for local
	 * or external (online) schemas) and validates the Document against the
	 * schema. Online schemas should be accessed using HTTP (not HTTPS).
	 * 
	 * @param xmlStream
	 *            XML input stream containing schema reference
	 * @return representation of XML document tree
	 * @throws SAXException
	 *             encapsulates problem validating the XML document
	 * @throws IOException
	 *             problem reading the XML input stream
	 * @throws ParserConfigurationException
	 *             a serious configuration error
	 * @throws OperationNotSupportedException
	 */
	public static Document validateNoNamespace(final InputStream xmlStream)
			throws SAXException, IOException, ParserConfigurationException {
		/* parse an XML document into a DOM tree */
		final Document document = parse(xmlStream);

		/* load XML schema, represented by a Schema instance */
		final Element root = document.getDocumentElement();
		final String schemaFileName = root
				.getAttribute("xsi:noNamespaceSchemaLocation");
		final Source schemaSource;
		if (schemaFileName.startsWith("http://")) {
			/* download the schema from HTTP server */
			URL url = new URL(schemaFileName);
			URLConnection connection = url.openConnection();
			InputStream schemaStream = connection.getInputStream();
			schemaSource = new StreamSource(schemaStream);
		} else {
			schemaSource = new StreamSource(new File(schemaFileName));
		}
		final Schema schema = schemaFactory.newSchema(schemaSource);

		/* create a Validator instance */
		final Validator validator = schema.newValidator();

		/* validate the DOM tree. throws an exception if there is a problem */
		validator.validate(new DOMSource(document));

		/* return the valid document */
		return document;
	}
}
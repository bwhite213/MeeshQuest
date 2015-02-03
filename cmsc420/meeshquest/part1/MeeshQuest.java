package cmsc420.meeshquest.part1;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cmsc420.xml.XmlUtility;

public class MeeshQuest {

	/**
	 * Read in XML document of commands to createCity etc. Print out a result
	 * document with the results of the errors and successes of the commands
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Used for reading the input file
		Document d = null;

		// Use to process commands Holds dataDictionary and Spatial map.
		CommandProcesser cp = new CommandProcesser();

		// Read in the document.
		try {
			//d = XmlUtility.validateNoNamespace(new File("/Users/bwhite213/Documents/workspace/MeeshQuest/part1.Tests/part1.all2.input.xml"));
			d = XmlUtility.validateNoNamespace(System.in);
			Element rootElm = d.getDocumentElement();

			NodeList nl = rootElm.getChildNodes();

			// Process All commands
			cp.processCommands(nl, rootElm.getAttribute("spatialHeight"),
					rootElm.getAttribute("spatialWidth"), rootElm.getAttribute("pmOrder") );

		} catch (ParserConfigurationException | SAXException | IOException e) {
			// Print out XML with <fatalError />
			cp.printFatalError();
		}
	}

}

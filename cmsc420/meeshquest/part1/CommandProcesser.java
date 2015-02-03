package cmsc420.meeshquest.part1;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cmsc420.drawing.CanvasPlus;
import cmsc420.geom.Shape2DDistanceCalculator;
import cmsc420.xml.XmlUtility;

public class CommandProcesser {

	// TODO Used to see if there are any errors
	// private String error = null;
	private Document result;

	private Element results;

	private DataDictionary dictionary;

	public CommandProcesser() {
		try {
			this.result = XmlUtility.getDocumentBuilder().newDocument();
			this.result.setXmlStandalone(false);
			this.results = result.createElement("results");
			this.dictionary = new DataDictionary();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public DataDictionary getDictionary() {
		return this.dictionary;
	}

	public Element getResults() {
		return this.results;
	}

	public City createCity(Command c) {
		return new City(c.getAttribute("name"), c.getAttribute("color"),
				Integer.parseInt(c.getAttribute("x")), Integer.parseInt(c
						.getAttribute("y")), Integer.parseInt(c
						.getAttribute("radius")));
	}

	/**
	 * Loop through all commands processing them as stated in the spec Prints
	 * out success/errors to this classes Results Element.
	 * 
	 * @param sH
	 * @param sW
	 * @param string 
	 */
	public void processCommands(NodeList nl, String sW, String sH, String pmOrd) {

		int spatialWidth = new Integer(sW);
		int spatialHeight = new Integer(sH);

		CanvasPlus canvas = new CanvasPlus("MeeshQuest", spatialWidth,
				spatialHeight);
		PRQuadtree PRQT = new PRQuadtree(spatialWidth, spatialHeight, canvas);

		for (int i = 0; i < nl.getLength(); i++) {
			Node command = nl.item(i);

			// Ensure it is an element not a comment
			if (command instanceof Element) {
				Command commandObject = new Command(command.getNodeName(),
						(Element) command);

				// *************CREATE CITY COMMAND*************//
				if (commandObject.getCommandName().equals("createCity")) {
					City createdCity = this.createCity(commandObject);

					int res = this.dictionary.insert(createdCity);

					if (res == 0) {
						this.printCreateCitySuccess(commandObject, createdCity);
					} else if (res == -1) {
						this.results.appendChild(this.printError("createCity",
								"duplicateCityName", commandObject));
					} else if (res == 1) {
						this.results.appendChild(this.printError("createCity",
								"duplicateCityCoordinates", commandObject));

					}
				}
				// *************LIST CITIES COMMAND*************//
				else if (commandObject.getCommandName().equals("listCities")) {
					if (this.dictionary.getNameMap().isEmpty()) {
						// Error no cities to list
						this.results.appendChild(this.printError("listCities",
								"noCitiesToList", commandObject));
					} else if (commandObject.getAttribute("sortBy").equals(
							"coordinate")) {
						this.listCitiesByCoord();
					} else if (commandObject.getAttribute("sortBy").equals(
							"name")) {
						this.listCitiesByName();
					} else {
						// Error trying to sort by other than name or coord.
						this.results.appendChild(this.printError("listCities",
								"undefined", commandObject));
					}
				}
				// *************DELETE CITY COMMAND*************//
				else if (commandObject.getCommandName().equals("deleteCity")) {
					City cityToDelete = this.dictionary.getNameMap().get(
							commandObject.getAttribute("name"));

					if (cityToDelete == null) {
						this.results.appendChild(this.printError("deleteCity",
								"cityDoesNotExist", commandObject));
					} else {
						Element cityUnmapped = null;
						boolean inPRQT = PRQT.find(cityToDelete) != null;
						cmsc420.meeshquest.part1.Node newPRQT = PRQT.delete(cityToDelete);
						
						// Remove from PRQuadtree if present else leave null
						//cmsc420.meeshquest.part1.Node newPRQT = PRQT
						//		.delete(cityToDelete);
						if (inPRQT){
							//Remove from PRQT and print city unmapped							
							cityUnmapped = this.printCityUnmapped(cityToDelete);
							PRQT.setRoot(newPRQT);
						}

						// Now remove from dictionary
						if (this.dictionary.remove(this.dictionary.getNameMap()
								.get(cityToDelete.getName())) == 0) {

							// Success
							this.results.appendChild(this.createSuccess(
									commandObject, cityUnmapped));
						} else {
							this.results.appendChild(this.printError(
									"deleteCity", "cityDoesNotExist",
									commandObject));
						}
					}
				}
				// *************CLEAR ALL COMMAND*************//
				else if (commandObject.getCommandName().equals("clearAll")) {
					this.dictionary.clearAll();
					PRQT = new PRQuadtree(spatialWidth, spatialHeight, canvas);

					this.results.appendChild(this.createSuccess(commandObject,
							null));
				}
				// *************MAP CITY COMMAND*************//
				else if (commandObject.getCommandName().equals("mapCity")) {
					String cityName = commandObject.getAttribute("name");
					City cityToMap = this.dictionary.getNameMap().get(cityName);

					if (cityToMap != null) {

						if (PRQT.find(cityToMap) == null) { // Make sure city is
															// not already
															// mapped.
							if (cityToMap.getX() < 0
									|| cityToMap.getX() > spatialWidth
									|| cityToMap.getY() < 0
									|| cityToMap.getY() > spatialHeight) {
								this.results.appendChild(this.printError(
										commandObject.getCommandName(),
										"cityOutOfBounds", commandObject));
							} else { // Success
								PRQT.insert(cityToMap);

								this.printCityMappedSuccess(cityToMap);
							}
						} else {
							this.results.appendChild(this.printError(
									commandObject.getCommandName(),
									"cityAlreadyMapped", commandObject));
						}
					} else {
						this.results.appendChild(this.printError(
								commandObject.getCommandName(),
								"nameNotInDictionary", commandObject));
					}

				}
				// *************UNMAP CITY COMMAND*************//
				else if (commandObject.getCommandName().equals("unmapCity")) {// UnMap
																				// City
																				// COMMAND//
					City cityToRemove = this.dictionary.getNameMap().get(
							commandObject.getAttribute("name"));

					if (cityToRemove == null) {
						this.results.appendChild(this.printError(
								commandObject.getCommandName(),
								"nameNotInDictionary", commandObject));
					} else {

						if (PRQT.find(cityToRemove) != null) { // Successful
																// find
							cmsc420.meeshquest.part1.Node newPRQT = PRQT
									.delete(cityToRemove);
							
								PRQT.setRoot(newPRQT);
								this.printUnmapCitySuccess(cityToRemove);

						} else { // Failure
							this.results.appendChild(this.printError(
									commandObject.getCommandName(),
									"cityNotMapped", commandObject));
						}
					}
				}
				// ************* PRINT PRQUADTREE COMMAND*************//
				else if (commandObject.getCommandName().equals(
						"printPRQuadtree")) {
					if (PRQT.isEmpty()) { // Error if map is empty
						this.results.appendChild(this.printError(
								"printPRQuadtree", "mapIsEmpty", null));
					} else { // Success
						this.results.appendChild(this.printQuadtreeSucess(
								printQuadtree(PRQT.getRoot()), PRQT.getRoot()));
					}

				}
				// ************* SAVE MAP COMMAND*************//
				else if (commandObject.getCommandName().equals("saveMap")) {
					String fileName = commandObject.getAttribute("name");

					try {
						canvas.save(fileName);
						this.results.appendChild(this.createSuccess(
								commandObject, null));
					} catch (IOException e) {

						e.printStackTrace();
					}

				}
				// *************RANGE CITIES COMMAND*************//
				else if (commandObject.getCommandName().equals("rangeCities")) {
					int x = new Integer(commandObject.getAttribute("x"));
					int y = new Integer(commandObject.getAttribute("y"));
					int r = new Integer(commandObject.getAttribute("radius"));

					ArrayList<City> cityList = PRQT.rangeCities(x, y, r);

					// See if save map attribute is specified
					if (!commandObject.getAttribute("saveMap").isEmpty()) {

						if (cityList.isEmpty()) { // No cities exist in the
													// given ranges
							this.results.appendChild(this.printError(
									commandObject.getCommandName(),
									"noCitiesExistInRange", commandObject));
						} else {
							this.results.appendChild(this.printRangeCities(
									cityList, commandObject,
									commandObject.getAttribute("saveMap")));
							// Save the map
							String fileName = commandObject
									.getAttribute("saveMap");

							try {
								// First draw circle
								canvas.addCircle(x, y, r, Color.BLUE, false);
								canvas.save(fileName);
								// remove circle
								canvas.removeCircle(x, y, r, Color.BLUE, false);

							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else { // Don't save map
						if (cityList.isEmpty()) { // No cities exist in the given ranges
							this.results.appendChild(this.printError(
									commandObject.getCommandName(),
									"noCitiesExistInRange", commandObject));
						}else { // Print success
							this.results.appendChild(this.printRangeCities(
									cityList, commandObject, null));
						}
						
					}

				}
				// *************NEAREST CITY COMMAND*************//
				else if (commandObject.getCommandName().equals("nearestCity")) {
					Point2D point = new Point2D.Float(
							Float.valueOf(commandObject.getAttribute("x")),
							Float.valueOf(commandObject.getAttribute("y")));

					City nearestCity = this.Nearest(point, PRQT);

					if (!PRQT.isEmpty() && nearestCity != null) {// Success
						Element cityElm = result.createElement("city");
						cityElm.setAttribute("name", nearestCity.getName());
						cityElm.setAttribute("x",
								String.valueOf((int) nearestCity.getX()));
						cityElm.setAttribute("y",
								String.valueOf((int) nearestCity.getY()));
						cityElm.setAttribute("color", nearestCity.getColor());
						cityElm.setAttribute("radius",
								String.valueOf((int) nearestCity.getRadius()));
						this.results.appendChild(this.createSuccess(
								commandObject, cityElm));
					} else { // Failure
						this.results.appendChild(this.printError(
								commandObject.getCommandName(), "mapIsEmpty",
								commandObject));
					}
				}
			}

		}
		try {
			this.result.appendChild(this.results);

			XmlUtility.print(this.result);
		} catch (TransformerException e) {
			// TODO Error printing the results
			e.printStackTrace();
		}

		// Dispose of canvas
		canvas.dispose();
	}

	/**
	 * Finds and returns the nearest city to the specified point.
	 * @param p Point to find nearest city of.
	 * @param pRQT
	 * @return null if PRQT is null or empty, else returns the nearest city.
	 */
	private City Nearest(Point2D p, PRQuadtree pRQT) {

		if (pRQT == null || pRQT.isEmpty()) {
			// TODO Print error
			return null;
		} else {
			City nearest = nearestHelper(pRQT, p);
			return nearest;
		}

	}

	/**
	 * Finds and returns the nearest city to the point. Using a priority queue to ensure everytime you are checking 
	 * the next closest point, and returning the black city node when it is found.
	 * @param prqt PRQuadTree to search through
	 * @param p point to look closest too.
	 * @return The nearest city to the point.
	 */
	private City nearestHelper(PRQuadtree prqt, Point2D p) {

		cmsc420.meeshquest.part1.Node n = prqt.getRoot();
		PriorityQueue<NearestCity> pq = new PriorityQueue<NearestCity>();

		if (n.isLeaf()) {
			Black black = (Black) n;
			pq.add(new NearestCity(n, p.distance(black.city), black.city));
		} else if (!n.isLeaf()) {
			Gray gray = (Gray) n;
			Point2D.Float point = new Point2D.Float(gray.x,
					gray.y);
			pq.add(new NearestCity(n, Shape2DDistanceCalculator.distance(p, gray), point));
		}

		while (!pq.isEmpty()) {
			NearestCity nc = pq.remove();

			if (nc.node.isLeaf()) {
				Black black = (Black) nc.node;
				while (nc.node == pq.peek().node) {
					pq.remove();
				}
				return black.city;
			} else if (!nc.node.isLeaf()) {
				Gray gray = (Gray) nc.node;
				for (int i = 0; i < 4; i++) {
					if (gray.regions[i] != null) {
						if (gray.regions[i] != null && gray.regions[i].isLeaf()) {
							pq.add(new NearestCity((Black) gray.regions[i], p
									.distance(((Black) gray.regions[i]).city),
									((Black) gray.regions[i]).city));
						} else if (gray.regions[i] != null
								&& !gray.regions[i].isLeaf()) { // Gray node
							Gray g = (Gray) gray.regions[i];
							Point2D.Float point = new Point2D.Float(g.x,
									g.y);
							pq.add(new NearestCity(g, Shape2DDistanceCalculator.distance(p, gray), point));
						}
					}
				}
			}
		}

		return null;

	}

	private Element printRangeCities(ArrayList<City> cityList,
			Command commandObject, String saveMap) {
		Element success = this.result.createElement("success");
		Element command = this.result.createElement("command");
		Element output = this.result.createElement("output");
		Element parameters = this.result.createElement("parameters");
		Element x = this.result.createElement("x");
		Element radius = this.result.createElement("radius");
		Element y = this.result.createElement("y");
		Element cityListElm = this.result.createElement("cityList");

		// Set attributes
		command.setAttribute("name", "rangeCities");
		x.setAttribute("value", commandObject.getAttribute("x"));
		y.setAttribute("value", commandObject.getAttribute("y"));
		radius.setAttribute("value", commandObject.getAttribute("radius"));

		output.appendChild(cityListElm);
		// Append cities from list SORT THEM!
		Collections.sort(cityList, new CityComparator());

		for (City c : cityList) {
			Element city = result.createElement("city");
			city.setAttribute("name", c.getName());
			city.setAttribute("x", String.valueOf((int) c.getX()));
			city.setAttribute("y", String.valueOf((int) c.getY()));
			city.setAttribute("color", c.getColor());
			city.setAttribute("radius", String.valueOf((int) c.getRadius()));
			cityListElm.appendChild(city);
		}

		// Append Nodes
		success.appendChild(command);
		success.appendChild(parameters);
		success.appendChild(output);

		parameters.appendChild(x);
		parameters.appendChild(y);
		parameters.appendChild(radius);

		// Append saveMap if non null
		if (saveMap != null) {
			Element saveMapElm = result.createElement("saveMap");
			saveMapElm.setAttribute("value", saveMap);

			parameters.appendChild(saveMapElm);
		}

		return success;

	}

	private Element printQuadtreeSucess(Element e,
			cmsc420.meeshquest.part1.Node node) {
		Element success = this.result.createElement("success");
		Element command = this.result.createElement("command");
		Element output = this.result.createElement("output");
		Element parameters = this.result.createElement("parameters");
		Element quadtree = this.result.createElement("quadtree");

		command.setAttribute("name", "printPRQuadtree");

		quadtree.appendChild(printQuadtree(node));
		output.appendChild(quadtree);
		success.appendChild(command);
		success.appendChild(parameters);
		success.appendChild(output);

		return success;
	}

	private Element printQuadtree(cmsc420.meeshquest.part1.Node node) {
		if (node == null) {
			Element white = result.createElement("white");
			return white;
		} else if (node.isLeaf()) {
			Black black = (Black) node;
			Element blackElm = result.createElement("black");
			blackElm.setAttribute("name", black.city.getName());
			blackElm.setAttribute("x", String.valueOf((int) black.city.getX()));
			blackElm.setAttribute("y", String.valueOf((int) black.city.getY()));
			return blackElm;

		} else {
			Gray gray = (Gray) node;
			Element grayElm = result.createElement("gray");

			grayElm.setAttribute("x", String.valueOf((int) gray.x));
			grayElm.setAttribute("y", String.valueOf((int) gray.y));

			if (gray.regions[0] != null) {
				Element child = printQuadtree(gray.regions[0]);
				grayElm.appendChild(child);
			} else {
				Element white = result.createElement("white");
				grayElm.appendChild(white);
			}
			if (gray.regions[1] != null) {
				Element child = printQuadtree(gray.regions[1]);
				grayElm.appendChild(child);
			} else {
				Element white = result.createElement("white");
				grayElm.appendChild(white);
			}
			if (gray.regions[2] != null) {
				Element child = printQuadtree(gray.regions[2]);
				grayElm.appendChild(child);
			} else {
				Element white = result.createElement("white");
				grayElm.appendChild(white);
			}
			if (gray.regions[3] != null) {
				Element child = printQuadtree(gray.regions[3]);
				grayElm.appendChild(child);
			} else {
				Element white = result.createElement("white");
				grayElm.appendChild(white);
			}

			return grayElm;
		}
	}

	private void listCitiesByName() {
		Element success = result.createElement("success");
		Element commandName = result.createElement("command");
		Element parameters = result.createElement("parameters");
		Element output = result.createElement("output");
		Element cityList = result.createElement("cityList");
		Element sortBy = result.createElement("sortBy");

		// Create citys and append to cityList
		for (City c : this.dictionary.getNameMap().values()) {
			Element city = result.createElement("city");
			city.setAttribute("name", c.getName());
			city.setAttribute("x", String.valueOf((int) c.getX()));
			city.setAttribute("y", String.valueOf((int) c.getY()));
			city.setAttribute("color", c.getColor());
			city.setAttribute("radius", String.valueOf(c.getRadius()));

			cityList.appendChild(city);

		}

		// Set Attributes
		commandName.setAttribute("name", "listCities");
		sortBy.setAttribute("value", "name");

		// append elements
		this.results.appendChild(success);
		success.appendChild(commandName);
		success.appendChild(parameters);
		success.appendChild(output);
		parameters.appendChild(sortBy);
		output.appendChild(cityList);

	}

	private void listCitiesByCoord() {
		Element success = result.createElement("success");
		Element commandName = result.createElement("command");
		Element parameters = result.createElement("parameters");
		Element output = result.createElement("output");
		Element cityList = result.createElement("cityList");
		Element sortBy = result.createElement("sortBy");

		// Create citys and append to cityList
		// Error TODO if no cities in list
		for (City c : this.dictionary.getCoordMap().keySet()) {
			Element city = result.createElement("city");
			city.setAttribute("name", c.getName());
			city.setAttribute("x", String.valueOf((int) c.getX()));
			city.setAttribute("y", String.valueOf((int) c.getY()));
			city.setAttribute("color", c.getColor());
			city.setAttribute("radius", String.valueOf(c.getRadius()));
			cityList.appendChild(city);

		}

		// Set Attributes
		commandName.setAttribute("name", "listCities");
		sortBy.setAttribute("value", "coordinate");

		// append elements
		this.results.appendChild(success);
		success.appendChild(commandName);
		success.appendChild(parameters);
		success.appendChild(output);
		parameters.appendChild(sortBy);
		output.appendChild(cityList);

	}

	private void printUnmapCitySuccess(City city) {
		Element success = result.createElement("success");
		Element commandName = result.createElement("command");
		Element parameters = result.createElement("parameters");
		Element output = result.createElement("output");
		Element name = result.createElement("name");

		// Set Attributes
		commandName.setAttribute("name", "unmapCity");
		name.setAttribute("value", city.getName());

		// Append nodes
		this.results.appendChild(success);
		success.appendChild(commandName);
		success.appendChild(parameters);
		success.appendChild(output);
		parameters.appendChild(name);
	}

	private Element printCityUnmapped(City city) {
		Element cityUnmapped = this.result.createElement("cityUnmapped");
		cityUnmapped.setAttribute("name", city.getName());
		cityUnmapped.setAttribute("x", city.getXToString());
		cityUnmapped.setAttribute("y", city.getYToString());
		cityUnmapped.setAttribute("color", city.getColor());
		cityUnmapped.setAttribute("radius", String.valueOf(city.getRadius()));
		
	
		return cityUnmapped;
	}

	private void printCityMappedSuccess(City city) {
		// Print success results
		Element success = result.createElement("success");
		Element commandName = result.createElement("command");
		Element parameters = result.createElement("parameters");
		Element output = result.createElement("output");
		Element name = result.createElement("name");

		// Set Attributes
		commandName.setAttribute("name", "mapCity");
		name.setAttribute("value", city.getName());

		// Append nodes
		this.results.appendChild(success);
		success.appendChild(commandName);
		success.appendChild(parameters);
		success.appendChild(output);
		parameters.appendChild(name);

	}

	/**
	 * Print success after creating a city successfully
	 * 
	 * @param commandObject
	 * @param createdCity
	 */
	private void printCreateCitySuccess(Command commandObject, City createdCity) {
		// Print success results
		Element success = result.createElement("success");
		Element commandName = result.createElement("command");
		Element parameters = result.createElement("parameters");
		Element output = result.createElement("output");
		Element name = result.createElement("name");
		Element x = result.createElement("x");
		Element y = result.createElement("y");
		Element radius = result.createElement("radius");
		Element color = result.createElement("color");

		// Set Attributes
		commandName.setAttribute("name", commandObject.getCommandName());
		name.setAttribute("value", createdCity.getName());
		color.setAttribute("value", createdCity.getColor());
		x.setAttribute("value", "" + (int) createdCity.getX());
		y.setAttribute("value", "" + (int) createdCity.getY());
		radius.setAttribute("value", "" + (int) createdCity.getRadius());

		// append elements
		this.results.appendChild(success);
		success.appendChild(commandName);
		success.appendChild(parameters);
		success.appendChild(output);
		parameters.appendChild(name);
		parameters.appendChild(x);
		parameters.appendChild(y);
		parameters.appendChild(radius);
		parameters.appendChild(color);

	}

	private Element createSuccess(Command commandObject, Element cityUnmapped) {
		Element success = this.result.createElement("success");
		Element command = this.result.createElement("command");
		Element parameters = this.result.createElement("parameters");
		Element output = this.result.createElement("output");

		// Set parameters and attributes
		command.setAttribute("name", commandObject.getCommandName());

		// Command specific params here
		if (commandObject.getCommandName().equals("deleteCity")) {
			Element name = this.result.createElement("name");
			name.setAttribute("value", commandObject.getAttribute("name"));
			parameters.appendChild(name);

			// If unmapped from quadtree print city unmapped in output/..
			if (cityUnmapped != null) {
				output.appendChild(cityUnmapped);
			}
		} else if (commandObject.getCommandName().equals("saveMap")) {
			Element name = this.result.createElement("name");
			name.setAttribute("value", commandObject.getAttribute("name"));
			parameters.appendChild(name);
		} else if (commandObject.getCommandName().equals("nearestCity")) {
			Element x = result.createElement("x");
			Element y = result.createElement("y");
			Element city = cityUnmapped;

			x.setAttribute("value", commandObject.getAttribute("x"));
			y.setAttribute("value", commandObject.getAttribute("y"));

			output.appendChild(city);
			parameters.appendChild(x);
			parameters.appendChild(y);
		}

		// Append all nodes
		success.appendChild(command);
		success.appendChild(parameters);
		success.appendChild(output);

		return success;
	}

	/**
	 * Creates the error element needed depending on the command and error.
	 * 
	 * @param commandName
	 *            command when error occured
	 * @param errorType
	 *            error type occured
	 * @param commandObject
	 * @return <error> element
	 */
	public Element printError(String commandName, String errorType,
			Command commandObject) {
		Element error = this.result.createElement("error");
		Element command = this.result.createElement("command");
		Element parameters = this.result.createElement("parameters");

		// Set Attributes TODO set parameters
		command.setAttribute("name", commandName);
		error.setAttribute("type", errorType);

		/*** Add parameters for different commands here. **/
		if (commandName.equals("createCity")) {
			Element name = result.createElement("name");
			Element x = result.createElement("x");
			Element y = result.createElement("y");
			Element radius = result.createElement("radius");
			Element color = result.createElement("color");

			name.setAttribute("value", commandObject.getAttribute("name"));
			color.setAttribute("value", commandObject.getAttribute("color"));
			x.setAttribute("value", commandObject.getAttribute("x"));
			y.setAttribute("value", commandObject.getAttribute("y"));
			radius.setAttribute("value", commandObject.getAttribute("radius"));
			parameters.appendChild(name);
			parameters.appendChild(x);
			parameters.appendChild(y);
			parameters.appendChild(radius);
			parameters.appendChild(color);
		} else if (commandName.equals("listCities")) {
			Element sortBy = result.createElement("sortBy");
			sortBy.setAttribute("value", commandObject.getAttribute("sortBy"));
			parameters.appendChild(sortBy);
		} else if (commandName.equals("deleteCity")) {
			Element name = result.createElement("name");
			name.setAttribute("value", commandObject.getAttribute("name"));
			parameters.appendChild(name);
		} else if (commandName.equals("mapCity")) {
			Element name = result.createElement("name");
			name.setAttribute("value", commandObject.getAttribute("name"));
			parameters.appendChild(name);
		} else if (commandName.equals("unmapCity")) {
			Element name = result.createElement("name");
			name.setAttribute("value", commandObject.getAttribute("name"));
			parameters.appendChild(name);
		} else if (commandName.equals("printPRQuadtree")) {
			// No Params
		} else if (commandName.equals("rangeCities")) {
			Element x = result.createElement("x");
			Element y = result.createElement("y");
			Element radius = result.createElement("radius");
			Element saveMap = result.createElement("saveMap");

			x.setAttribute("value", commandObject.getAttribute("x"));
			y.setAttribute("value", commandObject.getAttribute("y"));
			radius.setAttribute("value", commandObject.getAttribute("radius"));

			parameters.appendChild(x);
			parameters.appendChild(y);
			parameters.appendChild(radius);

			if (!commandObject.getAttribute("saveMap").isEmpty()) { // If save
																	// map
																	// present
																	// add it
				saveMap.setAttribute("value",
						commandObject.getAttribute("saveMap"));
				parameters.appendChild(saveMap);
			}

		} else if (commandName.equals("nearestCity")) {
			Element x = result.createElement("x");
			Element y = result.createElement("y");

			x.setAttribute("value", commandObject.getAttribute("x"));
			y.setAttribute("value", commandObject.getAttribute("y"));

			parameters.appendChild(x);
			parameters.appendChild(y);
		}

		// Append nodes
		error.appendChild(command);
		error.appendChild(parameters);

		return error;

	}

	public void printFatalError() {
		// TODO Auto-generated method stub

		try {
			Element fatalError = result.createElement("fatalError");

			result.appendChild(fatalError);

			XmlUtility.print(this.result);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	public void printUndefinedError() {
		// TODO Auto-generated method stub

		try {
			Element undefinedError = result.createElement("undefinedError");

			results.appendChild(undefinedError);

			XmlUtility.print(this.result);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

}

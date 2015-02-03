package cmsc420.meeshquest.part1;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import cmsc420.drawing.CanvasPlus;
import cmsc420.geom.Circle2D;
import cmsc420.geom.Inclusive2DIntersectionVerifier;

public class PRQuadtree {

	private Node root = null;

	public Node getRoot() {
		return root;
	}

	CanvasPlus canvas = null;

	int xMax = 128, yMax = 128;

	public PRQuadtree(int x, int y, CanvasPlus canvas) {
		this.xMax = x;
		this.yMax = y;
		this.canvas = canvas;
		// Draw initial Rectangle
		this.canvas
				.addRectangle(0, 0, this.xMax, this.yMax, Color.BLACK, false);
	}

	static public int getCityX(City city) {
		return (int) city.getX();
	}

	static public int getCityY(City city) {
		return (int) city.getY();
	}

	public City insert(City city) {
		return insert(city, root, 0);
	}

	/**
	 * Inserts a city into the PRQuadtree. If the node is null the tree is empty
	 * so we create a black node If its a leaf, we split it into a gray node.
	 * 
	 * @param city
	 * @param node
	 * @param depth
	 * @return
	 */
	private City insert(City city, Node node, int depth) {
		if (node == null) { // if node is null the tree is empty, create black
							// node with city
							// and add to root.

			this.root = new Black(city, new Rectangle2D.Float(this.xMax / 2,
					this.yMax / 2, xMax, yMax));
			// Draw The canvas
			canvas.addPoint(city.getName(), city.getX(), city.getY(),
					Color.BLACK);
		} else if (node.isLeaf()) {
			Gray newNode = new Gray(this.xMax / 2, this.yMax / 2, node.height,
					node.width);
			// Draw on canvas
			canvas.addLine(this.xMax / 2, 0, this.xMax / 2, this.yMax,
					Color.BLACK);
			canvas.addLine(0, this.yMax / 2, this.xMax, this.yMax / 2,
					Color.BLACK);

			Black currNode = (Black) node;
			this.root = newNode;
			this.insert(currNode.city, this.root, 0);
			return this.insert(city, this.root, 0);
		} else {
			// else the node is an internal Grey node, find quadrant to insert
			// into.
			Gray internalNode = (Gray) node;
			int halfX = (new Double(this.xMax / Math.pow(2, depth + 2)))
					.intValue();
			int halfY = (new Double(this.yMax / Math.pow(2, depth + 2)))
					.intValue();
			int newX = 0, newY = 0;

			// Find the quadrant that the node we are inserting will belong to
			int quadInd = 0;
			int cityX = (int) city.getX();
			int cityY = (int) city.getY();

			if (cityX >= internalNode.x) { // In 2nd or 4th quadrant
				if (cityY >= internalNode.y) { // In 2nd Quadrant
					quadInd = 1;
					newX = (int) internalNode.x + halfX;
					newY = (int) internalNode.y + halfY;

					if (internalNode.regions[quadInd] != null
							&& internalNode.regions[quadInd].isLeaf()) {
						canvas.addLine(newX, newY - halfY, newX, newY + halfY,
								Color.BLACK);
						canvas.addLine(newX - halfX, newY, newX + halfX, newY,
								Color.BLACK);
					}
				} else { // In the 4th Quadrant
					quadInd = 3;
					newX = (int) internalNode.x + halfX;
					newY = (int) internalNode.y - halfY;

					if (internalNode.regions[quadInd] != null
							&& internalNode.regions[quadInd].isLeaf()) {
						canvas.addLine(newX, newY - halfY, newX,
								internalNode.y, Color.BLACK);
						canvas.addLine(newX - halfX, newY, newX + halfX, newY,
								Color.BLACK);
					}
				}
			} else { // In 1st or 3rd Quadrant
				if (cityY >= internalNode.y) { // 1st Quadrant
					quadInd = 0;
					newX = (int) internalNode.x - halfX;
					newY = (int) internalNode.y + halfY;

					if (internalNode.regions[quadInd] != null
							&& internalNode.regions[quadInd].isLeaf()) {
						canvas.addLine(newX, newY - halfY, newX,
								internalNode.y * 2, Color.BLACK);
						canvas.addLine(newX - halfX, newY, internalNode.x,
								newY, Color.BLACK);
					}
				} else { // in 3rd Quadrant
					quadInd = 2;
					newX = (int) internalNode.x - halfX;
					newY = (int) internalNode.y - halfY;

					if (internalNode.regions[quadInd] != null
							&& internalNode.regions[quadInd].isLeaf()) {
						canvas.addLine(newX, newY - halfY, newX,
								internalNode.y, Color.BLACK);
						canvas.addLine(0, halfY, halfX * 2, halfY, Color.BLACK);
					}
				}
			}

			// See if the quadrant to add the city to is null or not to insert.
			if (internalNode.regions[quadInd] == null) {
				internalNode.regions[quadInd] = new Black(
						city,
						new Rectangle2D.Float(newX, newY,
								internalNode.width / 2, internalNode.height / 2));
				canvas.addPoint(city.getName(), city.getX(), city.getY(),
						Color.BLACK);
			} else {
				if (internalNode.regions[quadInd].isLeaf()) {
					Black oldCity = (Black) internalNode.regions[quadInd];
					internalNode.regions[quadInd] = new Gray(newX, newY,
							oldCity.height / 2, oldCity.width / 2);

					insert(oldCity.city, internalNode.regions[quadInd],
							depth + 1);
					return insert(city, internalNode.regions[quadInd],
							depth + 1);
				} else {
					return insert(city, internalNode.regions[quadInd],
							depth + 1);
				}
			}
		}

		return null;
	}

	/**
	 * Check if two citys have equal coordinates.
	 * 
	 * @param c1
	 *            First City
	 * @param c2
	 *            Second city
	 * @return True if they have the same coordinates.
	 */
	public boolean isCoordEqual(City c1, City c2) {
		boolean xEqual = false;
		boolean yEqual = false;

		if (c1.getX() == c2.getX()) {
			xEqual = true;
		}
		if (c1.getY() == c2.getY()) {
			yEqual = true;
		}

		return yEqual && xEqual;
	}

	/**
	 * Finds the range of cities within the radius specified.
	 * 
	 * @param x
	 *            x coordinate of center
	 * @param y
	 *            y coordinate of center
	 * @param r
	 *            radius to look inside
	 * @return an ArrayList of cities within the given range.
	 */
	public ArrayList<City> rangeCities(int x, int y, int r) {

		ArrayList<City> cityList = new ArrayList<City>();

		Circle2D.Float circle = new Circle2D.Float((float) x, (float) y, r);

		if (this.isEmpty() || r == 0) {
			// Do nothing
		} else {
			this.rangeCities(circle, cityList, this.root);
		}

		return cityList;
	}

	/**
	 * Helper function for range cities, goes through tree to find nodes within
	 * the range
	 * 
	 * @param circle
	 *            To look for cities within
	 * @param cityList
	 *            City list to append to
	 * @param node
	 *            Node of PRQuadtree
	 */
	private void rangeCities(Circle2D circle, ArrayList<City> cityList,
			Node node) {
		if (node == null) {
			return;
		} else if (node.isLeaf()) {
			Black black = (Black) node;

			if (Inclusive2DIntersectionVerifier.intersects(black.city, circle)) {
				cityList.add(black.city);
				return;
			}
		} else {
			Gray gray = (Gray) node;

			// If child is non null see if it lies in the same region.
			if (gray.regions[0] != null) {
				this.rangeCities(circle, cityList, gray.regions[0]);
			}
			if (gray.regions[1] != null) {
				this.rangeCities(circle, cityList, gray.regions[1]);
			}
			if (gray.regions[2] != null) {
				this.rangeCities(circle, cityList, gray.regions[2]);
			}
			if (gray.regions[3] != null) {
				this.rangeCities(circle, cityList, gray.regions[3]);
			}

		}

	}

	/**
	 * Finds the city within the quadtree.
	 * 
	 * @param c
	 *            City to look for in the tree
	 * @return Returns the city if found, null if not
	 */
	public City find(City c) {
		return find(c, this.root);
	}

	/**
	 * Helper to find city
	 * 
	 * @param c
	 *            City to find
	 * @param n
	 *            Node to look within.
	 * @return
	 */
	private City find(City c, Node n) {
		if (n == null) {

		} else if (n.isLeaf()) {
			Black black = (Black) n;
			if (isCoordEqual(c, black.city)) {
				return black.city;
			}
		} else {
			Gray gray = (Gray) n;
			// Find quadrant
			if (getCityX(c) >= gray.x) { // 2nd or 4th
				if (getCityY(c) >= gray.y) {// 2nd
					return find(c, gray.regions[1]);
				} else {
					return find(c, gray.regions[3]); // 4th
				}
			} else { // 1st or 3rd
				if (getCityY(c) >= gray.y) { // in 1st
					return find(c, gray.regions[0]);
				} else {
					return find(c, gray.regions[2]);
				}
			}
		}
		return null;
	}

	public cmsc420.meeshquest.part1.Node delete(City city) {
		return delete(city, root);
	}

	/**
	 * Delete finds the city and deletes it if its found.
	 * 
	 * @param city
	 *            City needed to delete
	 * @param root
	 *            node of tree
	 */
	private Node delete(City city, Node node) {
		if (node == null || city == null /* || this.find(city) == null */) {
			// Tree is null nothing to delete TODO any error
			// messages?
		} else if (node.isLeaf()) {
			Black black = (Black) node;
			if (isCoordEqual(city, black.city)) {
				canvas.removePoint(city.getName(), city.getX(), city.getY(),
						Color.BLACK);
				return null;
			} else {
				return black;
			}
		} else {
			// Remove from a gray node
			Gray gray = (Gray) node;
			if (getCityX(city) >= gray.x) { // in 2nd or 4th quadrant
				if (getCityY(city) >= gray.y) {// in 2nd
					Gray temp = gray;

					// Remove from the 2nd quadrant
					temp.regions[1] = this.delete(city, gray.regions[1]);

					// Check if gray node has 3 null children return remaining
					// node and change rectangle
					Node singleChild = temp.singleChild();
					if (singleChild != null && singleChild.isLeaf()) {
						singleChild.setRect(temp.getBounds2D());
						return singleChild;
					}

					return temp;

				} else { // in 4th
					Gray temp = gray;

					temp.regions[3] = this.delete(city, gray.regions[3]);

					// Check if gray node has 3 null children return remaining
					// node and change rectangle
					Node singleChild = temp.singleChild();
					if (singleChild != null && singleChild.isLeaf()) {
						singleChild.setRect(temp.getBounds2D());
						return singleChild;
					}

					return temp;
				}
			}

			else { // In 1st or 3rd quadrant
				if (getCityY(city) >= gray.y) {// In 1st
					Gray temp = gray;

					temp.regions[0] = this.delete(city, gray.regions[0]);

					// Check if gray node has 3 null children return remaining
					// node and change rectangle
					Node singleChild = temp.singleChild();
					if (singleChild != null && singleChild.isLeaf()) {
						singleChild.setRect(temp.getBounds2D());
						return singleChild;
					}

					return temp;
				} else {// in 3rd
					Gray temp = gray;

					temp.regions[2] = this.delete(city, gray.regions[2]);

					// Check if gray node has 3 null children return remaining
					// node and change rectangle
					Node singleChild = temp.singleChild();
					if (singleChild != null && singleChild.isLeaf()) {
						singleChild.setRect(temp.getBounds2D());
						return singleChild;
					}

					return temp;
				}
			}

		}
		return null;

	}

	/**
	 * Checks to see if the quadtree is empty.
	 * 
	 * @return true if empty false if not.
	 */
	public boolean isEmpty() {
		return this.root == null;
	}

	/**
	 * Set the root node of the quadtree. Used in deleting.
	 * 
	 * @param root
	 *            root node to set.
	 */
	public void setRoot(Node root) {
		this.root = root;
	}
}
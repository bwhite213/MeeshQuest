/*
 * @(#)Circle2D.java        1.0 2007/02/12
 *
 * Copyright Ben Zoller (University of Maryland, College Park), 2007
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */
package cmsc420.geom;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The Circle2D class describes a circle that is defined by a bounding
 * rectangle. It is implemented as a java.awt.geom.Ellipse2D that has been
 * restricted such that the width must also equal the height. It also implements
 * the Geometry2D interface.
 * <p>
 * This class is only the abstract superclass for all objects which store a 2D
 * ellipse. The actual storage representation of the coordinates is left to the
 * subclass.
 * 
 * 
 * @author Ben Zoller
 * @version 1.0, 12 Feb 2007
 */
public abstract class Circle2D extends Ellipse2D implements Geometry2D {
	/**
	 * The Float class defines an ellipse specified in float precision.
	 * @author Ben Zoller
	 * @version 1.0, 12 Feb 2007
	 */
	public static class Float extends Circle2D {
		/** ellipse representing circle */
		protected Ellipse2D.Float circle;

		/**
		 * Constructs and initializes a Circle2D with the specified coordinates.
		 * 
		 * @param centerX
		 *            X coordinate of the center of this circle
		 * @param centerY
		 *            Y coordinate of the center of this circle
		 * @param radius
		 *            radius of this circle
		 */
		public Float(float centerX, float centerY, float radius) {
			float diameter = radius * 2;
			circle = new Ellipse2D.Float(centerX - radius, centerY - radius,
					diameter, diameter);
		}

		/**
		 * Constructs and initializes a Circle2D with the specified coordinates.
		 * 
		 * @param center
		 *            center point of this circle
		 * @param radius
		 *            radius of this circle
		 */
		public Float(Point2D.Float center, float radius) {
			this(center.x, center.y, radius);
		}

		/**
		 * Gets the radius of this circle.
		 * 
		 * @return radius of this circle
		 */
		public double getRadius() {
			return circle.width * .5;
		}

		/**
		 * Gets the X coordinate of the center of this circle.
		 * 
		 * @return the X coordinate of the center of this circle
		 */
		public double getCenterX() {
			return circle.getCenterX();
		}

		/**
		 * Gets the Y coordinate of the center of this circle.
		 * 
		 * @return the Y coordinate of the center of this circle
		 */
		public double getCenterY() {
			return circle.getCenterY();
		}

		/**
		 * Gets the height of this circle.
		 * 
		 * @return height of this circle
		 */
		public double getHeight() {
			return circle.height;
		}

		/**
		 * Gets the width of this circle.
		 * 
		 * @return width of this circle
		 */
		public double getWidth() {
			return circle.width;
		}

		/**
		 * Gets the X coordinate of the origin of this circle.
		 * 
		 * @return the X coordinate of the origin of this circle
		 */
		public double getX() {
			return circle.x;
		}

		/**
		 * Gets the Y coordinate of the origin of this circle.
		 * 
		 * @return the Y coordinate of the origin of this circle
		 */
		public double getY() {
			return circle.y;
		}

		/**
		 * Sets the center point of this circle.
		 * 
		 * @param point
		 *            center point of this circle
		 */
		public void setCenter(Point2D point) {
			circle.x = (float) point.getX() - circle.width;
			circle.y = (float) point.getY() - circle.height;
		}

		/**
		 * Sets the radius of this circle.
		 * 
		 * @param radius
		 *            radius of this circle
		 */
		public void setRadius(double radius) {
			circle.width = circle.height = (float) radius * 2;
		}

		/**
		 * Determines whether or not the bounding box of this Ellipse2D is
		 * empty.
		 * 
		 * @return true if the bounding rectangle of this Ellipse2D is empty;
		 *         false otherwise.
		 */
		public boolean isEmpty() {
			return circle.isEmpty();
		}

		/**
		 * Sets the location and size of this Ellipse2D to the specified double
		 * values.
		 * 
		 * @param x
		 *            the specified X coordinate to which to set the location of
		 *            the bounding box of this Ellipse2D
		 * @param y
		 *            the specified Y coordinate to which to set the location of
		 *            the bounding box of this Ellipse2D
		 * @param w
		 *            the specified width to which to set the width of this
		 *            Ellipse2D
		 * @param h
		 *            the specified height to which to set the height of this
		 *            Ellipse2D
		 * @throws IllegalArgumentException
		 *             if the width is not equal to the height
		 */
		public void setFrame(double x, double y, double w, double h) {
			if (w != h) {
				throw new IllegalArgumentException(
						"width must be equal to height");
			}
			circle.setFrame(x, y, w, h);
		}

		/**
		 * Returns the high precision bounding box of this Ellipse2D.
		 * 
		 * @return a Rectangle2D that is the bounding box of this Ellipse2D.
		 */
		public Rectangle2D getBounds2D() {
			return circle.getBounds2D();
		}
	}

	/**
	 * The Double class defines an ellipse specified in double precision.
	 * @author Ben Zoller
	 * @version 1.0, 12 Feb 2007
	 */
	public static class Double extends Circle2D {
		/** ellipse representing circle */
		protected Ellipse2D.Double circle;

		/**
		 * Constructs and initializes a Circle2D with the specified coordinates.
		 * 
		 * @param centerX
		 *            X coordinate of the center of this circle
		 * @param centerY
		 *            Y coordinate of the center of this circle
		 * @param radius
		 *            radius of this circle
		 */
		public Double(double centerX, double centerY, double radius) {
			double diameter = radius * 2;
			circle = new Ellipse2D.Double(centerX - radius, centerY - radius,
					diameter, diameter);
		}

		/**
		 * Constructs and initializes a Circle2D with the specified coordinates.
		 * 
		 * @param center
		 *            center point of this circle
		 * @param radius
		 *            radius of this circle
		 */
		public Double(Point2D.Double center, double radius) {
			this(center.x, center.y, radius);
		}

		/**
		 * Gets the radius of this circle.
		 * 
		 * @return radius of this circle
		 */
		public double getRadius() {
			return circle.width * .5;
		}

		/**
		 * Gets the X coordinate of the center of this circle.
		 * 
		 * @return the X coordinate of the center of this circle
		 */
		public double getCenterX() {
			return circle.getCenterX();
		}

		/**
		 * Gets the Y coordinate of the center of this circle.
		 * 
		 * @return the Y coordinate of the center of this circle
		 */
		public double getCenterY() {
			return circle.getCenterY();
		}

		/**
		 * Gets the height of this circle.
		 * 
		 * @return height of this circle
		 */
		public double getHeight() {
			return circle.height;
		}

		/**
		 * Gets the width of this circle.
		 * 
		 * @return width of this circle
		 */
		public double getWidth() {
			return circle.width;
		}

		/**
		 * Gets the X coordinate of the origin of this circle.
		 * 
		 * @return the X coordinate of the origin of this circle
		 */
		public double getX() {
			return circle.x;
		}

		/**
		 * Gets the Y coordinate of the origin of this circle.
		 * 
		 * @return the Y coordinate of the origin of this circle
		 */
		public double getY() {
			return circle.y;
		}

		/**
		 * Sets the center point of this circle.
		 * 
		 * @param point
		 *            center point of this circle
		 */
		public void setCenter(Point2D point) {
			circle.x = point.getX() - circle.width;
			circle.y = point.getY() - circle.height;
		}

		/**
		 * Sets the radius of this circle.
		 * 
		 * @param radius
		 *            radius of this circle
		 */
		public void setRadius(double radius) {
			circle.width = circle.height = radius * 2;
		}

		/**
		 * Determines whether or not the bounding box of this Ellipse2D is
		 * empty.
		 * 
		 * @return true if the bounding rectangle of this Ellipse2D is empty;
		 *         false otherwise.
		 */
		public boolean isEmpty() {
			return circle.isEmpty();
		}

		/**
		 * Sets the location and size of this Ellipse2D to the specified double
		 * values.
		 * 
		 * @param x
		 *            the specified X coordinate to which to set the location of
		 *            the bounding box of this Ellipse2D
		 * @param y
		 *            the specified Y coordinate to which to set the location of
		 *            the bounding box of this Ellipse2D
		 * @param w
		 *            the specified width to which to set the width of this
		 *            Ellipse2D
		 * @param h
		 *            the specified height to which to set the height of this
		 *            Ellipse2D
		 * @throws IllegalArgumentException
		 *             if the width is not equal to the height
		 */
		public void setFrame(double x, double y, double w, double h) {
			if (w != h) {
				throw new IllegalArgumentException(
						"width must be equal to height");
			}
			circle.setFrame(x, y, w, h);
		}

		/**
		 * Returns the high precision bounding box of this Ellipse2D.
		 * 
		 * @return a Rectangle2D that is the bounding box of this Ellipse2D.
		 */
		public Rectangle2D getBounds2D() {
			return circle.getBounds2D();
		}
	}
	
	/**
	 * Gets the type of this Geometry2D object.
	 * 
	 * @return circle type
	 */
	public int getType() {
		return Geometry2D.CIRCLE;
	}

	/**
	 * Gets the center point of this circle.
	 * 
	 * @return center point of this circle
	 */
	public Point2D getCenter() {
		return new Point2D.Double(getCenterX(), getCenterY());
	}

	/**
	 * Gets the radius of this circle.
	 * 
	 * @return radius of this circle
	 */
	public abstract double getRadius();

	/**
	 * Sets the center point of this circle.
	 * 
	 * @param point
	 *            center point of this circle
	 */
	public abstract void setCenter(Point2D point);

	/**
	 * Sets the radius of this circle.
	 * 
	 * @param radius
	 *            radius of this circle
	 */
	public abstract void setRadius(double radius);
}

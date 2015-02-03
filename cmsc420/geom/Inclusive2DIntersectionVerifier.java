/*
 * @(#)InclusiveIntersectionVerifier.java        1.0 2007/02/19
 *
 * Copyright Ben Zoller (University of Maryland, College Park), 2007
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */
package cmsc420.geom;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The InclusiveIntersectionVerifier class provides static methods used to
 * determine if two shapes intersect. Unlike the java.awt.geom methods, bounds
 * are included in all intersection checks.
 * 
 * @author Ben Zoller
 * @version 1.0, 19 Feb 2007
 */
public class Inclusive2DIntersectionVerifier {
	/**
	 * Returns <code>true</code> if a point intersects with another point.
	 * Else returns <code>false</code>.
	 */
	public static boolean intersects(Point2D pt1, Point2D pt2) {
		return pt1.equals(pt2);
	}

	/**
	 * Returns <code>true</code> if a point lies inclusively within a circle's
	 * bounds. Else returns <code>false</code>.
	 */
	public static boolean intersects(Point2D point, Circle2D circle) {
		/* determine if the point lies within rectangle bounds */
		if (intersects(point, new Rectangle2D.Double(circle.getX(), circle
				.getY(), circle.getWidth(), circle.getHeight()))) {
			/*
			 * point is within rectangle bounds, calculate if it is inside the
			 * circle
			 */
			final double xdiff = point.getX() - circle.getCenterX();
			final double ydiff = point.getY() - circle.getCenterY();
			return xdiff * xdiff + ydiff * ydiff <= circle.getRadius()
					* circle.getRadius();
		} else {
			/* point is outside of rectangle bounds */
			return false;
		}
	}

	/**
	 * Returns <code>true</code> if a point lies inclusively within a
	 * rectangle's bounds. Else returns <code>false</code>.
	 */
	public static boolean intersects(Point2D point, Rectangle2D rect) {
		return (point.getX() >= rect.getMinX()
				&& point.getX() <= rect.getMaxX()
				&& point.getY() >= rect.getMinY() && point.getY() <= rect
				.getMaxY());
	}

	/**
	 * Returns <code>true</code> if a point intersects with a line segment.
	 * Else returns <code>false</code>.
	 */
	public static boolean intersects(Point2D point, Line2D line) {
		return (line.ptSegDistSq(point) == 0.0d);
	}

	/**
	 * Returns <code>true</code> if any part of a segment intersects
	 * inclusively within the rectangle's bounds. Else returns
	 * <code>false</code>.
	 */
	public static boolean intersects(Line2D seg, Rectangle2D rect) {
		/*
		 * check if the line segment intersects with the interior of the
		 * rectangle
		 */
		if (rect.intersectsLine(seg)) {
			/* segment intersects with interior of rectangle */
			return true;
		} else {
			/*
			 * segment does not intersect with interior of rectangle, check
			 * sides of rectangle
			 */
			Line2D[] sides = new Line2D[4];
			sides[0] = new Line2D.Double(rect.getMinX(), rect.getMinY(), rect
					.getMinX(), rect.getMaxY());
			sides[1] = new Line2D.Double(rect.getMinX(), rect.getMaxY(), rect
					.getMaxX(), rect.getMaxY());
			sides[2] = new Line2D.Double(rect.getMaxX(), rect.getMaxY(), rect
					.getMaxX(), rect.getMinY());
			sides[3] = new Line2D.Double(rect.getMaxX(), rect.getMinY(), rect
					.getMinX(), rect.getMinY());
			
			for (int i = 0; i < 4; i++) {
				/* check each side of rectangle against line segment */
				if (intersects(seg, sides[i])) {
					/* segment intersects with a rectangle side */
					return true;
				}
			}
			/* no intersection exists */
			return false;
		}
	}

	/**
	 * Returns <code>true</code> if two line segments intersect. Else returns
	 * <code>false</code>.
	 */
	public static boolean intersects(Line2D seg1, Line2D seg2) {
		return seg1.intersectsLine(seg2);
	}

	/**
	 * Returns <code>true</code> if a rectangle and circle intersect. Else
	 * returns <code>false</code>.
	 * <p>
	 * Algorithm taken from Dave Mount's Geometry notes.
	 */
	public static boolean intersects(Rectangle2D rect, Circle2D circle) {
		/* circle-rectangle intersection if distance <= radius */
		return Shape2DDistanceCalculator.distance(circle.getCenter(), rect) <= circle
				.getRadius();
	}

	/**
	 * Returns <code>true</code> if a circle intersects with another circle.
	 * Else returns <code>false</code>.
	 */
	public static boolean intersects(Circle2D c1, Circle2D c2) {
		double centerDistance = c1.getCenter().distance(c2.getCenter());
		double maxRadiusDistance = c1.getRadius() + c2.getRadius();
		return centerDistance <= maxRadiusDistance;
	}
}

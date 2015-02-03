package cmsc420.geom;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The Shape2DDistanceCalculator class provides methods for finding the distance
 * between shapes on a two-dimensional plane.
 * 
 * @author Ben Zoller
 * 
 */
public class Shape2DDistanceCalculator {

	/**
	 * Gets the distance from a point to a rectangle.
	 * 
	 * @param pt
	 *            point
	 * @param rect
	 *            rectangle
	 * @return distance between the point and the rectangle
	 */
	public static double distance(Point2D pt, Rectangle2D rect) {
		double distanceSq = 0;

		if (pt.getX() < rect.getMinX()) {
			final double xdist = rect.getMinX() - pt.getX();
			distanceSq += xdist * xdist;
		} else if (pt.getX() > rect.getMaxX()) {
			final double xdist = pt.getX() - rect.getMaxX();
			distanceSq += xdist * xdist;
		}

		if (pt.getY() < rect.getMinY()) {
			final double ydist = rect.getMinY() - pt.getY();
			distanceSq += ydist * ydist;
		} else if (pt.getY() > rect.getMaxY()) {
			final double ydist = pt.getY() - rect.getMaxY();
			distanceSq += ydist * ydist;
		}

		/* circle-rectangle intersection if distance <= radius */
		return Math.sqrt(distanceSq);
	}
	
	/**
	 * Gets the distance from a line segment to a rectangle
	 * 
	 * 
	 * @param seg
	 * 				Line segment
	 * @param rect
	 * 				Rectangle
	 * @return
	 * 				The distance
	 */
	public static double distance(Line2D seg, Rectangle2D rect) {
		if (Inclusive2DIntersectionVerifier.intersects(seg, rect)) {
			return 0;
		}
		
		Point2D[] corners = new Point2D.Float[4];
		corners[0] = new Point2D.Float(
				(float) rect.getX(), 
				(float) rect.getY());
		corners[1] = new Point2D.Float(
				(float) rect.getX(), 
				(float) (rect.getY() + rect.getHeight()));
		corners[2] = new Point2D.Float(
				(float) (rect.getX() + rect.getWidth()), 
				(float) rect.getY());
		corners[3] = new Point2D.Float(
				(float) (rect.getX() + rect.getWidth()), 
				(float) (rect.getY() + rect.getHeight()));
		
		Line2D[] edges = new Line2D.Float[4];
		edges[0] = new Line2D.Float(corners[0], corners[1]);
		edges[1] = new Line2D.Float(corners[1], corners[3]);
		edges[2] = new Line2D.Float(corners[3], corners[2]);
		edges[3] = new Line2D.Float(corners[2], corners[0]);
		

		double dist = Double.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			double d = seg.ptSegDist(corners[i]);
			if (d < dist) {
				dist = d;
			}
		}
		for (int i = 0; i < 4; i++) {
			double d = edges[i].ptSegDist(seg.getP1());
			if (d < dist) {
				dist = d;
			}
		}
		for (int i = 0; i < 4; i++) {
			double d = edges[i].ptSegDist(seg.getP2());
			if (d < dist) {
				dist = d;
			}
		}
		return dist;
	}


}

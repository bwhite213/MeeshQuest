/*
 * @(#)Geometry2D.java        1.0 2007/01/25
 *
 * Copyright Ben Zoller (University of Maryland, College Park), 2007
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */
package cmsc420.geom;

/**
 * An interface for polygonal shapes on the (x,y) coordinate plane. Makes up for
 * the fact that java.awt.geom shape classes do not share an interface.
 * 
 * @author Ben Zoller
 * @version 1.0, 25 Jan 2007
 */
public interface Geometry2D {
	/** Type flag for Geometry2D points */
	public static final int POINT = 0;

	/** Type flag for Geometry2D line segments */
	public static final int SEGMENT = 1;

	/** Type flag for Geometry2D rectangles */
	public static final int RECTANGLE = 2;

	/** Type flag for Geometry2D circles */
	public static final int CIRCLE = 3;

	/**
	 * Returns the type of of an object that implements the Geometry2D
	 * interface.
	 */
	public int getType();
}

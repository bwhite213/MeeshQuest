package cmsc420.meeshquest.part1;

import java.awt.geom.Point2D;

import cmsc420.geom.Geometry2D;

public class City extends Point2D.Float implements Geometry2D {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String name;
	protected String color;
	protected int radius;

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getName() {
		return this.name;
	}

	public int getRadius() {
		return this.radius;
	}

	public String getColor() {
		return this.color;
	}

	public String getXToString() {
		return String.valueOf((int) this.x);
	}

	public String getYToString() {
		return String.valueOf((int) this.y);
	}

	/**
	 * Constructs a new City
	 * @param name
	 * @param color
	 * @param x
	 * @param y
	 * @param radius
	 */
	public City(String name, String color, int x, int y, int radius) {
		// TODO Ensure all info is given somewhere
		this.setLocation(x, y);
		this.name = name;
		this.color = color;
		this.radius = radius;
	}

}

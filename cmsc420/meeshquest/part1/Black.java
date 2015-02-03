package cmsc420.meeshquest.part1;

import java.awt.geom.Rectangle2D;

public class Black extends Node {

	public City city;

	public Black(City city, Rectangle2D.Float rect) {
		this.city = city;
		this.setRect(rect);
	}
	
	public boolean isLeaf() {
		return true;
	}

	@Override
	public boolean isWhite() {
		return false;
	}

}

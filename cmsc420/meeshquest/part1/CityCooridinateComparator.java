package cmsc420.meeshquest.part1;

import java.awt.geom.Point2D;
import java.util.Comparator;

public class CityCooridinateComparator implements Comparator<Point2D> {

	@Override
	public int compare(Point2D o1, Point2D o2) {
		if (o1.getX() == o2.getX()) {
			return (int) (o1.getY() - o2.getY());
		}
		return (int) (o1.getX() - o2.getX());
	}

}

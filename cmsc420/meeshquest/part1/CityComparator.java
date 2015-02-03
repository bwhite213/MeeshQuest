package cmsc420.meeshquest.part1;

import java.util.Comparator;

public class CityComparator implements Comparator<City> {

	public int compare(City c1, City c2) {
		// TODO Auto-generated method stub
		return c1.getName().compareTo(c2.getName());
	}

}
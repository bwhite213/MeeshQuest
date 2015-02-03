package cmsc420.meeshquest.part1;

import java.util.TreeMap;

public class DataDictionary {

	// Stores the keys as name and value of City object.
	private TreeMap<String, City> nameMap;

	// Stored the map of Locations and value City object.
	private TreeMap<City, String> coordMap;

	// Getters
	public TreeMap<City, String> getCoordMap() {
		return this.coordMap;
	}

	public TreeMap<String, City> getNameMap() {
		return this.nameMap;
	}

	public DataDictionary() {
		this.nameMap = new TreeMap<String, City>(new CityNameComparator());
		this.coordMap = new TreeMap<City, String>(
				new CityCooridinateComparator());
	}

	/**
	 * Inserts the city into the dictionary if the name and coords are not
	 * already contained.
	 * 
	 * @param city
	 *            The city to insert.
	 * @return 0 if success, -1 is name is contained, 1 if coordinates already
	 *         contained.
	 */
	public int insert(City city) {
		if (this.nameMap.containsKey(city.getName())) {
			return -1;
		} else if (this.coordMap.containsKey(city)) {
			return 1;
		} else {
			this.nameMap.put(city.getName(), city);
			this.coordMap.put(city, city.getName());
			return 0;
		}
	}

	/**
	 * Removes the given city from the data dictionary
	 * 
	 * @param city
	 * @return-1 if city is not in dictionary, 0 on success
	 */
	public int remove(City city) {
		if (this.nameMap.containsKey(city.getName())) {
			// Delete
			this.coordMap.remove(city);
			this.nameMap.remove(city.getName());
			return 0;
		} else {
			return -1;
		}
	}

	/**
	 * Removes all cities from dictionary.
	 */
	public void clearAll() {
		this.coordMap.clear();
		this.nameMap.clear();
	}

}

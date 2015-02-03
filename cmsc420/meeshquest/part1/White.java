package cmsc420.meeshquest.part1;

import java.util.HashSet;

import cmsc420.geom.Geometry2D;

/**
 * Represents an empty leaf node.
 * 
 * @author bwhite213
 * 
 */
public class White extends Node {

	protected White() {

	}
	
	@Override
	public boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean isWhite(){
		return true;
	}
}

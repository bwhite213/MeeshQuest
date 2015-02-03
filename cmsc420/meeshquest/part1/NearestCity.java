package cmsc420.meeshquest.part1;

import java.awt.geom.Point2D;

public class NearestCity implements Comparable<NearestCity> {

	public cmsc420.meeshquest.part1.Node node;
	public double distance;
	public Point2D.Float p;

	public NearestCity(Node node, double distance, Point2D.Float p) {
		this.node = node;
		this.distance = distance;
		this.p = p;
	}

	@Override
	public int compareTo(NearestCity o) {
		
		
		if (distance == o.distance){
			if (node.isLeaf() && o.node.isLeaf()){ // If both are leaves comare and return
				return ((Black)node).city.getName().compareTo(((Black) o.node).city.getName());
			}else if (this.node.isLeaf() && o.node != null && !o.node.isLeaf()){
				return -1;
			} else if (!this.node.isLeaf() && o.node.isLeaf()){
				return 1;
			}
		}
		
		if (node.isLeaf() && o.node.isLeaf()){ // If both are leaves comare and return
			return this.distance > o.distance ? 1 : -1 ;			
		}else if (this.node.isLeaf() && o.node != null && !o.node.isLeaf()){ // if o is Gray it comes before.
			return 1;
		} else if (!this.node.isLeaf() && o.node.isLeaf()){
			return -1;
		}
		
		return 0;
		
	}
}

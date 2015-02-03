package cmsc420.meeshquest.part1;

public class Gray extends Node {

	public Node[] regions;

	public Gray(int x, int y, float height, float width) {
		
		this.regions = null;
		this.regions = new Node[4];

		
		//Set rectangle fields.
		this.height = height;
		this.width = width;
		this.x = x;
		this.y = y;
		
		for (int i = 0; i < 4; i++) {
			this.regions[i] = null;
		}

	}

	public boolean isLeaf() {
		return false;
	}
	
	public boolean isWhite() {
		return false;
	}

	public boolean equals(Gray node) {
		return (node.x == this.x && node.y == this.y);
	}

	/**
	 * Returns the singleChild of the current node, null if it has more than one children.
	 * @return singleChild of the current node, null if it has more than one children.
	 */
	public cmsc420.meeshquest.part1.Node singleChild() {
		int count = 0;
		cmsc420.meeshquest.part1.Node ret = null;
		
		for (int i = 0; i < 4; i++){
			if (this.regions[i] == null){
				count ++;
			}else {
				ret = this.regions[i];
			}
		}
		
		if (count == 3){
			return ret;
		}else {
			return null;
		}
	}
}

package cmsc420.drawing;

import java.awt.Color;

/**
 * An example of drawing using <tt>CanvasPlus</tt>.
 * 
 * @author Brian Krznarich
 * @author Evan Machusak
 * @version 2.0
 * @since 1.0
 */
public class CanvasPlusDemo {
	/**
	 * Creates a new <tt>CanvasPlus</tt> named &quot;Canvas Plus Demo&quot;
	 * and adds a spattering of geometric primitives to the scene and draws it.
	 */
	static public void main(String[] args) {
		CanvasPlus testImage = new CanvasPlus("Canvas Plus Demo");

		testImage.addCross(512, 512, 512, Color.GRAY);
		testImage.addCross(256, 256, 256, Color.GRAY);
		testImage.addRectangle(512, 512, 512, 512, Color.BLACK, false);
		testImage.addCircle(512, 512, 256, Color.BLUE, false);
		testImage.addPoint("center", 512, 512, Color.BLACK);
		testImage.addPoint("extreme", 1024, 1024, Color.BLACK);

		testImage.setScaleMode(CanvasPlus.SCALE_FIT);
		testImage.draw();

		try {
			Thread.sleep(100);
		} catch (InterruptedException ie) {
		}

		testImage.setScaleMode(CanvasPlus.SCALE_SCROLL);

		for (int i = 0; testImage.isLive() && i < 768; ++i) {
			testImage.lookAt(i, i);
			try {
				Thread.sleep(1);
			} catch (InterruptedException ie) {
			}
		}
		testImage.setScaleMode(CanvasPlus.SCALE_NONE);
		for (int i = 512; testImage.isLive() && i >= 0; --i) {
			testImage.lookAt(i, i);
			try {
				Thread.sleep(1);
			} catch (InterruptedException ie) {
			}
		}
		testImage.setScaleMode(CanvasPlus.SCALE_SCROLL);
	}
}
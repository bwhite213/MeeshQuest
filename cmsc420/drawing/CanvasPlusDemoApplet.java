package cmsc420.drawing;

import java.awt.Color;

/**
 * An example of extending <tt>CanvasPlus</tt> for use in embedded applets.
 * 
 * @author Evan Machusak
 * @version 1.0
 * @since 1.0
 */
public class CanvasPlusDemoApplet extends CanvasPlus {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7358792033155418806L;

	/**
	 * Creates an instance of <tt>CanvasPlusDemoApplet</tt>.
	 * 
	 * @since 1.0
	 */
	public CanvasPlusDemoApplet() {
		super();
	}

	/**
	 * Adds a spattering of geometric primitives to the scene during the
	 * pre-render stage.
	 * 
	 * @since 1.0
	 */
	protected void setup() {
		addCross(512, 512, 512, Color.GRAY);
		addCross(256, 256, 256, Color.GRAY);
		addRectangle(512, 512, 512, 512, Color.BLACK, false);
		addCircle(512, 512, 256, Color.BLUE, false);
		addPoint("center", 512, 512, Color.BLACK);
		addPoint("extreme", 1024, 1024, Color.BLACK);
		this.setScaleMode(CanvasPlus.SCALE_SCROLL);
	}

	/**
	 * Executes a brief animation sequence in which the image is scrolled.
	 * 
	 * @since 1.0
	 */
	protected void run() {

		for (int i = 128; isLive() && i < 384; ++i) {
			lookAt(i, i);
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
			}
		}
		setScaleMode(CanvasPlus.SCALE_NONE);
		for (int i = 384; isLive() && i >= 128; --i) {
			lookAt(i, i);
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
			}
		}
		setScaleMode(CanvasPlus.SCALE_SCROLL);
	}
}
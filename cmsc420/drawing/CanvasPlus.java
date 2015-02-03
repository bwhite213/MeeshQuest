/* 
 * @(#)CanvasPlus.java		3.0 2007/02/19
 * Version 3.0 Copyright Ben Zoller (University of Maryland, College Park), 2007
 * Version 2.0 Copyright Evan Machusak (University of Maryland, College Park), 2003
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */

package cmsc420.drawing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

/**
 * A general 2D drawing utility backed by Java AWT. <tt>CanvasPlus</tt> allows
 * simple 2D graphics primitives to be placed into the scene and then drawn, by
 * invoking the {@link #draw()} method. The coordinate system used in
 * <tt>CanvasPlus</tt> places (0,0) in the lower-left corner of the window.
 * <p />
 * 
 * This version of <tt>CanvasPlus</tt> includes several noticeable
 * improvements over previous versions, including support for multiple viewport
 * configurations. Specifically, users can now indicate how they want the image
 * to be fit into the window, including support for scaling (see
 * {@link #setScaleMode(int)}). Background color can now be specified, and most
 * notably, the {@link #draw()} method no longer blocks execution until the
 * window is closed. However, users can still invoke a blocking draw method (see
 * {@link #drawBlocking()}).
 * <p />
 * 
 * Animation is also supported by this version of <tt>CanvasPlus</tt> in a
 * more sophisticated manner. By using the {@link #lookAt(int,int)} method,
 * users can programmatically scroll the canvas if the scale mode is fixed or
 * scrollable (for scaled-to-fit and fixed aspect modes, the entire canvas is
 * always visible within the window, so "looking at" a portion of the screen
 * would serve no purpose). For animation, the ideal scale mode is none. Scale
 * modes can be changed at any time (including while the scene is considered
 * <i>live</i>, i.e., after its <tt>draw</tt> method has been invoked but
 * before the window is closed).
 * <p />
 * 
 * <b>Warning:</b> be aware that any and all statements following
 * {@link #draw()} will continue to execute <i>while the window is open</i>,
 * and may take precedence. For example:
 * <ul>
 * <tt>
 * CanvasPlus canvasPlus = new CanvasPlus("my window");<br />
 * canvasPlus.draw();<br />
 * System.exit(0);</ul></tt>
 * 
 * This code will appear to do nothing; the window may flash for a second as it
 * opens, but as soon as <tt>draw()</tt> returns, <tt>System.exit(0)</tt> is
 * invoked and will force the window to close. If you want execution only to
 * resume when the window is closed, use {@link #drawBlocking()} instead, which
 * will only return when the window is no longer open. Alternatively,
 * {@link #suspendUntilClosed()} can be called at any time after <tt>draw()</tt>
 * to prevent further execution until the window is closed. Be advised, however,
 * that in multi-threaded environments, other threads may still proceed.
 * 
 * @author Sean Dougherty (original author)
 * @author Matt Lew (added functionality for circles, rectangles, and a basic
 *         priority queue)
 * @author Evan Machusak (rewrote code for new version, documentation)
 * @author Ben Zoller (added functionality for saving maps to file, removing
 *         shapes, rectangles are now drawn given its lower-left point)
 * @version 3.0, 14 Feb 2007
 */
public class CanvasPlus extends javax.swing.JApplet {
	private static final long serialVersionUID = 3903119390448325306L;

	/** The title of the window created by the <tt>draw</tt> method. */
	protected final String title;

	/**
	 * The size of the window to be opened by a call to {@link #draw()} or
	 * {@link #drawBlocking()}.
	 */
	protected final Dimension size;

	/**
	 * The coordinate in the upper-right corner of the canvas in terms of the
	 * objects entered. The x- and y- values of this coordinate do not
	 * necessarily belong to the same actual "largest point" in space. This
	 * value is used in scaling calculations and must be updated as necessary
	 * when new objects are added to the canvas.
	 */
	protected Point2D.Float max;

	/**
	 * The coordinate in the lower-left corner of the canvas in terms of the
	 * objects entered. The x- and y- values of this coordinate do not
	 * necessarily belong to the same actual "smallest point" in space. This
	 * value is used in scaling calculations and must be updated as necessary
	 * when new objects are added to the canvas.
	 */
	protected Point2D.Float min;

	/**
	 * The list of shapes, to be drawn in the order specified by the queue (to
	 * issue priority, add a shape to the beginning of this list).
	 */
	protected final Vector<Paintable2D> shapeQueue;

	/** The scaling mode specified for this <tt>CanvasPlus</tt>. */
	protected int scaleMode;

	/**
	 * The aspect ratio (the ratio between the width and height specified by the
	 * <tt>size</tt> field).
	 */
	protected float aspectRatio;

	/**
	 * The color of the background of the drawing pane of this
	 * <tt>CanvasPlus</tt>.
	 */
	protected Color backgroundColor;

	/** The font of all future text added to this canvas. */
	protected Font font = new Font("Dialog", Font.PLAIN, 10);

	/**
	 * When the canvas is drawn, live is set to true and future calls to draw()
	 * will do nothing until the window is closed. <b>Note:</b> this field will
	 * not be serialized.
	 */
	/* don't toy with this, and don't serialize it */
	protected transient boolean live = false;

	/**
	 * The scroll pane associated with this <tt>CanvasPlus</tt>. This will be
	 * the sole child of this <tt>JApplet</tt>'s content pane when the scale
	 * mode is set to scroll or none. In all cases, the drawing pane (<tt>drawingPane</tt>)
	 * is the sole object contained by this scroll pane.
	 * 
	 * @see javax.swing.JApplet#getContentPane()
	 */
	protected JScrollPane scrollPane;

	/**
	 * The <tt>DrawingPane</tt> associated with this <tt>CanvasPlus</tt>;
	 * this child of <tt>JPanel</tt> is the component on which all shapes are
	 * drawn. This will be the sole child of this <tt>JApplet</tt>'s content
	 * pane when the scale mode is set to fit or fixed aspect.
	 */
	protected DrawingPane drawingPane;

	/**
	 * The <tt>Frame</tt> that will be instantiated when a call to
	 * {@link #draw()} is made. The value of this member will never be
	 * <tt>null</tt>.
	 */
	protected JFrame frame = new JFrame();

	/**
	 * A flag that, when specified, directs an instance of <tt>CanvasPlus</tt>
	 * not to use any scaling. Any objects that fit into the coordinate space
	 * contained within the visible window will be displayed; all others will be
	 * obscured from view.
	 */
	public static final int SCALE_NONE = 1;

	/**
	 * A flag that, when specified, directs an instance of <tt>CanvasPlus</tt>
	 * to display scroll bars as needed to to provide viewing access to all
	 * portions of the coordinate space.
	 */
	public static final int SCALE_SCROLL = 2;

	/**
	 * A flag that, when specified, directs an instance of <tt>CanvasPlus</tt>
	 * to scale all coordinates to fit within the space provided by the
	 * enclosing window. The canvas will be stretched both horizontally and
	 * vertically as needed to occupy the entire space in the window.
	 */
	public static final int SCALE_FIT = 4;

	/**
	 * A flag that, when specified, directs an instance of <tt>CanvasPlus</tt>
	 * to scale coordinates to fit within the space provided by the enclosing
	 * window with respect to the aspect ratio of the initial window. The image
	 * will be centered in the window as appropriate.
	 */
	public static final int SCALE_FIXED_ASPECT = 8;

	/**
	 * Creates a new instance of <tt>CanvasPlus</tt> with the default title
	 * "untitled" and default width and height [512,512].
	 * 
	 * @since 1.0
	 */
	public CanvasPlus() {
		this("untitled", 512, 512);
	}

	/**
	 * Creates a new instance of <tt>CanvasPlus</tt> with the specified title
	 * and default width and height [512,512].
	 * 
	 * @param title
	 *            the title caption of the window opened to view this canvas
	 * @since 1.0
	 */
	public CanvasPlus(final String title) {
		this(title, 512, 512);
	}

	/**
	 * Creates a new instance of <tt>CanvasPlus</tt> with the specified title,
	 * width, and height.
	 * 
	 * @param title
	 *            the title caption of the window opened to view this canvas
	 * @param dim
	 *            the dimensions of this CanvasPlus in screen space
	 * @since 1.0
	 */
	public CanvasPlus(final String title, final Dimension dim) {
		this(title, dim.width, dim.height);
	}

	/**
	 * Creates a new instance of <tt>CanvasPlus</tt> with the specified title,
	 * width, and height.
	 * 
	 * @param title
	 *            the title caption of the window opened to view this canvas
	 * @param width
	 *            the width of this CanvasPlus in screen space
	 * @param height
	 *            the height of this CanvasPlus in scree space
	 * @since 1.0
	 */
	public CanvasPlus(final String title, final int width, final int height) {
		super(); /* JApplet requires some housekeeping; do it here */

		this.title = title;

		final GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		final Rectangle screen = ge.getDefaultScreenDevice()
				.getDefaultConfiguration().getBounds();

		this.size = new Dimension(Math.min(width, screen.width), Math.min(
				height, screen.height));
		this.max = new Point2D.Float(0f, 0f);
		this.min = new Point2D.Float(0f, 0f);

		this.scaleMode = SCALE_FIT;
		this.aspectRatio = (float) width / (float) height;

		this.shapeQueue = new Vector<Paintable2D>();

		this.backgroundColor = Color.WHITE;

		this.drawingPane = new DrawingPane(new Dimension(0, 0));
		this.scrollPane = new JScrollPane(drawingPane,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.getContentPane().add(scrollPane);
		this.live = false;
	}

	/**
	 * Called by the browser or applet viewer to inform this applet that it has
	 * been loaded into the system. Although granted public access for
	 * usability, the user should generally not invoke this method manually.
	 * <p>
	 * 
	 * <b>Note to extenders:</b> this method is marked final; its body contains
	 * only a call to the non-final method <tt>setup()</tt>. To override this
	 * method, override <tt>setup()</tt> instead.
	 * 
	 * @see #setup()
	 * @since 2.0
	 */
	public final void init() {
		setup();
	}

	/**
	 * Called by <tt>init()</tt> to perform initialization operations, such as
	 * adding geometry to a scene prior to display. This method should be
	 * overridden by extenders wishing to use <tt>CanvasPlus</tt> as an
	 * embedded applet to setup the initial visible scene.
	 * 
	 * @since 2.0
	 */
	protected void setup() {
		this.getContentPane().invalidate();
	}

	/**
	 * Called by the browser or applet viewer to inform this applet that it
	 * should start its execution. Although granted public access for usability,
	 * the user should generally not invoke this method manually.
	 * <p>
	 * 
	 * <b>Note to extenders</b>: this method is marked final; its body contains
	 * only a call to the non-final method <tt>run()</tt>. Because some
	 * browsers will not display anything in the applet body until this method
	 * exits, <tt>run()</tt> is wrapped in its own thread which will acquire a
	 * lock on this <tt>CanvasPlus</tt>, thus other threads wishing to invoke
	 * synchronized methods on this <tt>CanvasPlus</tt> will be blocked.
	 * 
	 * @see #run()
	 * @since 2.0
	 */
	public final void start() {
		/*
		 * presumably, this method is invoked either in true applet context
		 * (i.e., embedded in a browser) in which case the applet is live as
		 * soon as the applet loads and this method is invoked, OR MainFrame
		 * invokes it during its draw operations in which case live is already
		 * set to true
		 */
		this.live = true;
		this.refreshInterface();
		new Thread() {
			public void run() {
				CanvasPlus.this.safeRun();
			}
		}.start();
	}

	/**
	 * The purpose of this method is to ensure the synchronization safety of
	 * overridden <tt>run()</tt> method in derived classes. This allows
	 * extenders to omit the <tt>synchronization</tt> keyword and achieve
	 * synchronization on this <tt>CanvasPlus</tt> nevertheless.
	 * 
	 * @since 2.0
	 */
	protected final synchronized void safeRun() {
		this.repaint();
		run();
		teardown();
	}

	/**
	 * This method should be overridden by extenders to execute behaviors after
	 * the initial render. This method is ultimately encapsulated in its own
	 * thread (which automatically acquires a lock on this <tt>CanvasPlus</tt>).
	 * If a lock can't be acquired, this applet's <tt>start()</tt> method will
	 * never exit and the applet will fail to load. The lock on this
	 * <tt>CanvasPlus</tt> will be released as soon as the <tt>run()</tt>
	 * method exits. Given the execution context of this applet, it is unlikely
	 * that there will ever be any lock contentions involved with this method.
	 * Because Java's locks are re-entrant, it is safe to call other
	 * synchronized methods of <tt>CanvasPlus</tt> from within <tt>run()</tt>.
	 * 
	 * @since 2.0
	 */
	protected void run() {
	}

	/**
	 * Saves the current drawing pane to the file: &lt;name&gt;.png
	 * <p>
	 * Note: The resources used by <code>CanvasPlus</code> will not be cleared
	 * until {@link #dispose()} is called.
	 * 
	 * @param name
	 *            name of the png file the current drawing pane will be saved to
	 * @throws IOException
	 *             problem accessing the image file
	 * @since 3.0
	 */
	public void save(final String name) throws IOException {
		/* draw the map */
		draw();

		/* write the drawing pane to an image */
		int width = drawingPane.getWidth();
		int height = drawingPane.getHeight();
		final BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics2d = image.createGraphics();
		drawingPane.paint(graphics2d);
		graphics2d.dispose();
		ImageIO.write(image, "png", new File(name + ".png"));
	}

	/**
	 * Clears the map by clearing all of the shapes that are drawn when
	 * {@link #draw()} is called.
	 * 
	 * @since 3.0
	 */
	public void clear() {
		shapeQueue.clear();
	}

	/**
	 * Close the applet and release all of the native resources used by this
	 * Window, its subcomponents, and all of its children. That is, the
	 * resources for these Components will be destroyed, any memory they consume
	 * will be released to the OS, and they will be marked as undisplayable.
	 * 
	 * @since 3.0
	 */
	public void dispose() {
		frame.dispose();
	}

	/**
	 * Invoked immediately after the {@link #run()} method exits. Because the
	 * <tt>stop()</tt> method is typically controlled by the parent
	 * application (such as the browser), the <tt>teardown()</tt> method
	 * provides a logical abstraction for post-run actions to be applied to the
	 * canvas independent of the parent application's caprice in choosing when
	 * to execute <tt>stop()</tt>
	 * 
	 * @since 2.0
	 */
	protected void teardown() {
	}

	/**
	 * See {@link java.applet.Applet#stop()} for suggestions on overriding this
	 * method. The default implementation for <tt>CanvasPlus</tt> does
	 * nothing. Because <tt>stop()</tt> is less crucial to successful
	 * operation of this <tt>CanvasPlus</tt> as an applet, it is not marked
	 * final, but in general extenders should override <tt>teardown()</tt>.
	 * 
	 * @see java.applet.Applet#stop()
	 * @since 2.0
	 */
	public void stop() {
	}

	/**
	 * Called internally to adjust the Swing components when the scaling mode of
	 * this <tt>CanvasPlus</tt> is changed. This method can still be
	 * explicitly called while this <tt>CanvasPlus</tt> is <i>live</i>, but
	 * this is generally not recommended.
	 * 
	 * @since 2.0
	 */
	protected synchronized void refreshInterface() {
		if (this.scaleMode == SCALE_SCROLL) {
			scrollPane
					.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			this.drawingPane.setPreferredSize(new Dimension(
					(int) (this.max.x - this.min.x) + 20,
					(int) (this.max.y - this.min.y) + 20));
		} else if (this.scaleMode == SCALE_NONE) {

			/*
			 * DISCLAIMER: I have absolutely no idea why this works (or is
			 * necessary), but playing with scrollbar policies is finicky
			 * business. When switching from a scale mode like FIT or
			 * FIXED_ASPECT into NONE, the lookat() method won't work unless the
			 * policies are fooled with like this (as_needed->never)
			 */
			scrollPane
					.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane
					.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

			scrollPane
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		} else {
			scrollPane
					.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			scrollPane
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		}

		this.scrollPane.setViewportView(this.drawingPane);

	}

	/**
	 * Opens a window and displays the current scene depicted by this
	 * <tt>CanvasPlus</tt>. Once this method has been called, the scene is
	 * said to be <i>live</i>. Note that this method will exit as soon as the
	 * window has been created, allowing the statements which succeed it to
	 * execute freely. In order to prevent further execution until this method
	 * has closed, use {@link #drawBlocking()}.
	 * 
	 * @see #drawBlocking()
	 * @since 1.0
	 */
	public synchronized void draw() {

		/* Don't allow two simultaneous draws (this would be disastrous!) */
		if (live)
			return;

		this.refreshInterface();
		this.scrollPane.getVerticalScrollBar().setValue((int) this.max.y);
		live = true;
		try {
			this.frame = new JFrame(title);

			/* kills JFrame thread(s) on window close */
			this.frame
					.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			/* Continue after window is closed */
			this.frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					CanvasPlus.this.live = false;
				}
			});
			this.frame.setSize(this.size.width, this.size.height);
			this.frame.setContentPane(this.getContentPane());
			this.frame.setVisible(true);
		} catch (Error e) {
			throw new RuntimeException(
					"CanvasPlus.draw(): can't connect to display");
		}

		/* method exits, but window is still open */
	}

	public synchronized void draw(final int scaleMode) {
		this.setScaleMode(scaleMode);
		this.draw();
	}

	public synchronized void drawBlocking(final int scaleMode) {
		this.setScaleMode(scaleMode);
		this.drawBlocking();
	}

	/**
	 * Opens a window and displays the current scene depicted by this
	 * <tt>CanvasPlus</tt>, blocking all further execution until the user
	 * closes the window. In order to allow execution to proceed after the
	 * window is opened and before it is closed, use <tt>draw()</tt>.
	 * 
	 * @see #draw()
	 * @since 2.0
	 */
	public synchronized void drawBlocking() {
		if (live)
			return;
		this.refreshInterface();
		this.scrollPane.getVerticalScrollBar().setValue((int) this.max.y);
		live = true;
		try {
			this.frame = new JFrame(title);

			/* kills JFrame thread(s) on window close */
			this.frame
					.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			/* Continue after window is closed */
			this.frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					CanvasPlus.this.live = false;
				}
			});

			this.frame.setSize(this.size.width, this.size.height);
			this.frame.setContentPane(this.getContentPane());
			this.frame.setVisible(true);
		} catch (Error e) {
			throw new RuntimeException(
					"CanvasPlus.draw(): can't connect to display");
		}
		/*
		 * Spin until the window is closed, i.e., block further execution,
		 * unless this method was wrapped in its own thread.
		 */
		suspendUntilClosed();
	}

	/**
	 * Causes the calling thread to perform busy-waiting until the window opened
	 * by <tt>draw()</tt> is closed. This method will have no effect if the
	 * drawing window is not open.
	 * 
	 * @see #draw()
	 * @since 2.0
	 */
	public void suspendUntilClosed() {
		while (live)
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
			}
	}

	/**
	 * Causes the Swing components of this <tt>CanvasPlus</tt> to repaint
	 * themselves. Callers can use this method to force the drawing space to set
	 * its preferred size correctly so the scroll bars are correctly sized.
	 * Because windowing is inherently multi-threaded, certain execution
	 * scheduling can cause a window with its scale mode set to scrollable to
	 * display scrollbars that are too small. <tt>CanvasPlus</tt> will handle
	 * this problem itself when displayed by its <tt>draw()</tt> methods, but
	 * extenders may encounter this issue when using <tt>CanvasPlus</tt> as an
	 * embedded applet, and thus this method is largely a convenience to
	 * extenders.
	 * 
	 * @since 2.0
	 */
	public void repaint() {
		super.repaint();
		/*
		 * we do this in the paint method, but sometimes simply invalidating the
		 * window isn't enough. it's an annoyance, but setting the drawpane's
		 * preferred size seems to fix everything.
		 */
		drawingPane.paintComponent(drawingPane.getGraphics());
		drawingPane.invalidate();
		scrollPane.invalidate();
	}

	/**
	 * Centers the visible portion of the scene depicted by this
	 * <tt>CanvasPlus</tt> around the given point. At a minimum, this method
	 * guarantees to bring the point <tt>(x,y)</tt> into view. If the point
	 * exists beyond the values returned by <tt>getScrollableRange()</tt>, it
	 * will not be possible to center the view around the given point because
	 * doing so would violate the bounds of the scrollbars. There is no harm in
	 * calling <tt>lookAt()</tt> on points that do not exist or wildly violate
	 * the bounds of this <tt>CanvasPlus</tt>; the method will make a
	 * best-effort attempt to scroll to that value.
	 * <p>
	 * 
	 * If the current scale mode is set to <tt>SCALE_FIT</tt> or
	 * <tt>SCALE_FIXED_ASPECT</tt>, this method will have no effect since the
	 * entire scene is always visible under these scaling modes.
	 * 
	 * @param x
	 *            the X coordinate of the new center of focus
	 * @param y
	 *            the y-croodinate of the new center of focus
	 * @since 2.0
	 */

	public synchronized void lookAt(final int x, final int y) {
		if (live && scaleMode <= SCALE_SCROLL)
			/*
			 * An exception can be thrown here if the window is closed while
			 * operations directly affecting the scroll panel are applied after
			 * the window is destroyed
			 */
			try {
				scrollPane.getHorizontalScrollBar().setValue(
						x - (this.size.width >> 1));
				scrollPane.getVerticalScrollBar().setValue(
						((int) this.max.y - y) - (this.size.height >> 1));
				repaint();
			} catch (NullPointerException npe) {
			}
	}

	/**
	 * Centers the visible portion of the scene depicted by this
	 * <tt>CanvasPlus</tt> around the point given in floating point precision.
	 * The specified parameters will be truncated to integers.
	 * 
	 * @param x
	 *            the X coordinate of the new center of focus
	 * @param y
	 *            the y-croodinate of the new center of focus
	 * @see #lookAt(int,int)
	 * @since 2.0
	 */
	public synchronized void lookAt(final float x, final float y) {
		lookAt((int) x, (int) y);
	}

	/**
	 * Centers the visible portion of the scene depicted by this
	 * <tt>CanvasPlus</tt> around the point given in double precision. The
	 * specified parameters will be truncated to integers.
	 * 
	 * @param x
	 *            the X coordinate of the new center of focus
	 * @param y
	 *            the y-croodinate of the new center of focus
	 * @see #lookAt(int,int)
	 * @since 2.0
	 */
	public synchronized void lookAt(final double x, final double y) {
		lookAt((int) x, (int) y);
	}

	/**
	 * Centers the visible portion of the scene depicted by this
	 * <tt>CanvasPlus</tt> around the specified point. The specified
	 * parameters' x- and y- values will be truncated to integers.
	 * 
	 * @param p
	 *            the new point at which focus will be centered
	 * @see #lookAt(int,int)
	 * @since 2.0
	 */
	public synchronized void lookAt(final Point2D.Float p) {
		lookAt((int) p.x, (int) p.y);
	}

	/**
	 * Centers the visible portion of the scene depicted by this
	 * <tt>CanvasPlus</tt> around the specified point. The specified
	 * parameters' x- and y- values will be truncated to integers.
	 * 
	 * @param p
	 *            the new point at which focus will be centered
	 * @see #lookAt(int,int)
	 * @since 2.0
	 */
	public synchronized void lookAt(final Point2D.Double p) {
		lookAt((int) p.x, (int) p.y);
	}

	/**
	 * Returns the scrollable extent of this <tt>CanvasPlus</tt>. The value
	 * returned will hold the largest useful values specifiable via the
	 * {@link #lookAt(int,int)} method. In general, a call to
	 * <tt>lookAt(0,0)</tt> will scroll to the upper-left corner of the
	 * visible area and a call to <tt>lookAt(d.width, d.height)</tt> where
	 * <tt>d</tt> is the value returned by this function will scroll to the
	 * lower-right corner of the visible area.
	 * <p>
	 * 
	 * <b>Note:</b> this method will return unreliable values until the scene
	 * is live. In other words, before a call to <tt>draw()</tt> or
	 * <tt>drawBlocking()</tt> has been made, the value returned by this
	 * method is undefined. It will be non-<tt>null</tt>, but will most
	 * likely return a <tt>Dimension</tt> whose width and height values are
	 * one half of the current specified size for this <tt>CanvasPlus</tt>.
	 * 
	 * @return an instance of <tt>Dimension</tt> containing the width and
	 *         height of the scrollable range of this <tt>CanvasPlus</tt>
	 * @since 2.0
	 */
	public synchronized Dimension getScrollableRange() {
		return new Dimension(scrollPane.getHorizontalScrollBar().getMaximum()
				- scrollPane.getHorizontalScrollBar().getMinimum(), scrollPane
				.getVerticalScrollBar().getMaximum()
				- scrollPane.getVerticalScrollBar().getMinimum());

	}

	/**
	 * Indicates whether or not the scene is currently live (i.e., the drawing
	 * window is open).
	 * 
	 * @return <tt>true</tt> if this <tt>CanvasPlus</tt> is live,
	 *         <tt>false</tt> otherwise.
	 * @since 2.0
	 */
	public boolean isLive() {
		return live;
	}

	/**
	 * Returns the size of this <tt>CanvasPlus</tt>. This value is only used
	 * to define the size of the window when it is first opened and also as a
	 * way to define the aspect ratio of the desired image when using fixed
	 * aspect scaling mode. This method will <i>not</i> return the current size
	 * of the open window if this <tt>CanvasPlus</tt> is <i>live</i>. To get
	 * the current size, use <tt>getFrameSize()</tt> instead. The value
	 * returned is a copy and changing it will not change the size of this
	 * <tt>CanvasPlus</tt>; use <tt>setSize()</tt> instead.
	 * 
	 * @return an instance of {@link java.awt.Dimension} containing the width
	 *         and height of this <tt>CanvasPlus</tt>
	 * @see #getFrameSize()
	 * @since 1.0
	 */
	public synchronized Dimension getSize() {
		return new Dimension(size.width, size.height);
	}

	/**
	 * Sets the size of this <tt>CanvasPlus</tt>. This value is only used to
	 * define the size of the window when it is first opened and also as a way
	 * to define the aspect ratio of the desired image when using fixed aspect
	 * scaling mode. Calling this method on a live canvas will <i>not</i> cause
	 * the size of the open window to change. To change the size of the open
	 * window, use <tt>setFrameSize()</tt> instead.
	 * 
	 * @param width
	 *            the new desired width of this <tt>CanvasPlus</tt>
	 * @param height
	 *            the new desired height of this <tt>CanvasPlus</tt>
	 * @see #setFrameSize(int,int)
	 * @since 1.0
	 */
	public synchronized void setSize(final int width, final int height) {
		size.width = width;
		size.height = height;
	}

	/**
	 * Sets the size of this <tt>CanvasPlus</tt>. This value is only used to
	 * define the size of the window when it is first opened and also as a way
	 * to define the aspect ratio of the desired image when using fixed aspect
	 * scaling mode. Calling this method on a live canvas will <i>not</i> cause
	 * the size of the open window to change. To change the size of the open
	 * window, use <tt>setFrameSize()</tt> instead.
	 * 
	 * @param newSize
	 *            the new desired dimensions of this <tt>CanvasPlus</tt>
	 * @see #setFrameSize(int,int)
	 * @since 1.0
	 */
	public synchronized void setSize(final Dimension newSize) {
		size.width = newSize.width;
		size.height = newSize.height;
	}

	public synchronized void setBounds(final float xMin, final float yMin,
			final float xMax, final float yMax) {
		if (xMin < min.x)
			min.x = xMin;
		if (yMin < min.y)
			min.y = yMin;
		if (xMax > max.x)
			max.x = xMax;
		if (yMax > max.y)
			max.y = yMax;
	}

	/**
	 * Gets the current size of the visible window if this <tt>CanvasPlus</tt>
	 * is live. Otherwise, this method will return a size of [0,0]. This method
	 * returns a copy; changing the values of the returned <tt>Dimension</tt>
	 * will not affect the current window; use <tt>setFrameSize()</tt>
	 * instead.
	 * 
	 * @return the current dimensions of the open window if the scene is live, a
	 *         <tt>Dimension</tt> with width, height 0 otherwise
	 * @since 2.0
	 */
	public synchronized Dimension getFrameSize() {
		if (live)
			return new Dimension(this.frame.getSize().width, this.frame
					.getSize().height);
		else
			return new Dimension(0, 0);
	}

	/**
	 * Sets the current size of the visible window if this <tt>CanvasPlus</tt>
	 * is live. This method will have no effect if called before or after the
	 * scene is live.
	 * 
	 * @param width
	 *            the new desired width of the visible window
	 * @param height
	 *            the new desired height of the visible window
	 * @since 2.0
	 */
	public synchronized void setFrameSize(final int width, final int height) {
		this.frame.setSize(width, height);
	}

	/**
	 * Sets the current size of the visible window if this <tt>CanvasPlus</tt>
	 * is live. This method will have no effect if called before or after the
	 * scene is live.
	 * 
	 * @param newSize
	 *            the new desired dimensions of the visible window
	 * @since 2.0
	 */
	public synchronized void setFrameSize(final Dimension newSize) {
		this.frame.setSize(newSize);
	}

	/**
	 * Returns the point composed of the largest X-value and the largest Y-value
	 * so far added to this <tt>CanvasPlus</tt>. The X- and Y-values returned
	 * by this method will not necessarily belong to the same spatial point
	 * added to this <tt>CanvasPlus</tt>. As this value is maintained
	 * internally, it cannot be set; furthermore, this method returns a copy,
	 * therefore changing the return value's X- and Y- values will not affect
	 * this <tt>CanvasPlus</tt>.
	 * 
	 * @return an instance of {@link java.awt.geom.Point2D.Float} containing the
	 *         largest X- and Y-values present in this <tt>CanvasPlus</tt>
	 * @since 2.0
	 */
	public Point2D.Float getMax() {
		return new Point2D.Float(max.x, max.y);
	}

	/**
	 * Returns the point composed of the smallest X-value and the smallest
	 * Y-value so far added to this <tt>CanvasPlus</tt>. The X- and Y-values
	 * returned by this method will not necessarily belong to the same spatial
	 * point added to this <tt>CanvasPlus</tt>. As this value is maintained
	 * internally, it cannot be set; furthermore, this method returns a copy,
	 * therefore changing the return value's X- and Y- values will not affect
	 * this <tt>CanvasPlus</tt>.
	 * 
	 * @return an instance of {@link java.awt.geom.Point2D.Float} containing the
	 *         smallest X- and Y-values present in this <tt>CanvasPlus</tt>
	 * @since 2.0
	 */
	public Point2D.Float getMin() {
		return new Point2D.Float(min.x, min.y);
	}

	/**
	 * Returns the default font in which all future text added to this
	 * <tt>CanvasPlus</tt> will be rendered.
	 * 
	 * @return the default font associated with this <tt>CanvasPlus</tt>
	 * @since 2.0
	 */
	public synchronized Font getFont() {
		return this.font;
	}

	/**
	 * Sets the default font in which all future text added to this
	 * <tt>CanvasPlus</tt> will be rendered.
	 * 
	 * @param f
	 *            the new font to be associated with this <tt>CanvasPlus</tt>
	 * @since 2.0
	 */
	public synchronized void setFont(final Font f) {
		this.font = f;
	}

	/**
	 * Sets the default font in which all future text added to this
	 * <tt>CanvasPlus</tt> will be rendered.
	 * 
	 * @param face
	 *            the font face
	 * @param style
	 *            the style of the font (such as bold, italic, etc)
	 * @param size
	 *            the size of the font
	 * @since 2.0
	 */
	public synchronized void setFont(final String face, final int style,
			final int size) {
		this.font = new Font(face, style, size);
	}

	/**
	 * Returns the current background color of this <tt>CanvasPlus</tt>.
	 * 
	 * @return the background color of this <tt>CanvasPlus</tt>
	 * @since 2.0
	 */
	public synchronized Color getBackgroundColor() {
		return this.backgroundColor;
	}

	/**
	 * Sets the background color of this <tt>CanvasPlus</tt>
	 * 
	 * @param color
	 *            the new background color
	 * @since 2.0
	 */
	public synchronized void setBackgroundColor(final Color color) {
		this.backgroundColor = color;
	}

	/**
	 * Returns the current scale mode in use by this <tt>CanvasPlus</tt>.
	 * 
	 * @return the scale mode of this <tt>CanvasPlus</tt>
	 * @since 2.0
	 */
	public int getScaleMode() {
		return this.scaleMode;
	}

	/**
	 * Sets the scale mode for this <tt>CanvasPlus</tt>.
	 * 
	 * @param scaleMode
	 *            the scale mode to use for this <tt>CanvasPlus</tt>
	 * @throws IllegalArgumentException
	 *             if <tt>scaleMode</tt> is not one of {@link #SCALE_NONE},
	 *             {@link #SCALE_SCROLL}, {@link #SCALE_FIT},
	 *             {@link #SCALE_FIXED_ASPECT}
	 * @since 2.0
	 */
	public void setScaleMode(final int scaleMode) {
		if (scaleMode == SCALE_NONE || scaleMode == SCALE_SCROLL
				|| scaleMode == SCALE_FIT || scaleMode == SCALE_FIXED_ASPECT) {
			this.scaleMode = scaleMode;
			this.refreshInterface();
		} else
			throw new IllegalArgumentException(
					"Scale mode must be one of {SCALE_NONE, SCALE_SCROLL, SCALE_FIT, SCALE_FIXED_ASPECT}");
	}

	/**
	 * Returns the number of graphics primitives added to this
	 * <tt>CanvasPlus</tt>.
	 * 
	 * @return the size of this <tt>CanvasPlus</tt>'s shape queue
	 * @since 2.0
	 */
	public int getShapeCount() {
		return shapeQueue.size();
	}

	/**
	 * A class-level flag required to detect circular method invocation. We'll
	 * need to synchronize the <tt>add(Drawable2D)</tt> method on this flag,
	 * so it's an <tt>Object</tt> rather than a primitive <tt>boolean</tt>.
	 */
	private transient Boolean inAdd = Boolean.valueOf(false);

	/**
	 * Indicates that a circular method invocation loop has occurred. By
	 * default, in this event a <tt>StackOverflowException</tt> typically
	 * occurs. This exception should be used in situations where this event can
	 * be preemptively detected.
	 * 
	 * @author Evan Machusak
	 * @version 1.0
	 * @since 2.0
	 */
	protected static class CircularMethodInvocationException extends
			RuntimeException {
		private static final long serialVersionUID = -8187944671294919352L;

		/**
		 * Creates a new <tt>CircularMethodInvocationException</tt>.
		 * 
		 * @param message
		 *            the reason for this exception
		 */
		public CircularMethodInvocationException(final String message) {
			super(message);
		}
	}

	/**
	 * Adds an implementor of {@link Drawable2D} into the scene depicted by this
	 * <tt>CanvasPlus</tt>
	 * 
	 * @param obj
	 *            the object to draw onto this <tt>CanvasPlus</tt>
	 * @throws CircularMethodInvocationException
	 *             if <tt>obj.draw(this)</tt> invokes this method
	 * @since 2.0
	 */
	public void add(final Drawable2D obj) {
		synchronized (inAdd) {
			if (inAdd.booleanValue())
				throw new CircularMethodInvocationException(
						"CanvasPlus.add(Drawable2D = "
								+ obj.getClass().getName()
								+ "): parameter object attempting to reinvoke CanvasPlus.add(Drawable)");
			inAdd = Boolean.valueOf(true);
			obj.draw(this);
			inAdd = Boolean.valueOf(false);
		}
	}

	/**
	 * Adds a line segment into the scene.
	 * 
	 * @param x1
	 *            the X coordinate of the start point
	 * @param y1
	 *            the Y coordinate of the start point
	 * @param x2
	 *            the X coordinate of the end point
	 * @param y2
	 *            the Y coordinate of the end point
	 * 
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addLine(final float x1, final float y1, final float x2,
			final float y2, final Color color) {
		return shapeQueue.add(new PaintableLine2DFloat(x1, y1, x2, y2, color));
	}

	/**
	 * Adds a line segment into the scene.
	 * 
	 * @param x1
	 *            the X coordinate of the start point
	 * @param y1
	 *            the Y coordinate of the start point
	 * @param x2
	 *            the X coordinate of the end point
	 * @param y2
	 *            the Y coordinate of the end point
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addLine(final double x1, final double y1, final double x2,
			final double y2, final Color color) {
		return shapeQueue.add(new PaintableLine2DDouble(x1, y1, x2, y2, color));
	}

	/**
	 * Removes a line segment from the scene.
	 * 
	 * @param x1
	 *            the X coordinate of the start point
	 * @param y1
	 *            the Y coordinate of the start point
	 * @param x2
	 *            the X coordinate of the end point
	 * @param y2
	 *            the Y coordinate of the end point
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeLine(final float x1, final float y1, final float x2,
			final float y2, final Color color) {
		return shapeQueue
				.remove(new PaintableLine2DFloat(x1, y1, x2, y2, color));
	}

	/**
	 * Removes a line segment from the scene.
	 * 
	 * @param x1
	 *            the X coordinate of the start point
	 * @param y1
	 *            the Y coordinate of the start point
	 * @param x2
	 *            the X coordinate of the end point
	 * @param y2
	 *            the Y coordinate of the end point
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeLine(final double x1, final double y1,
			final double x2, final double y2, final Color color) {
		return shapeQueue.remove(new PaintableLine2DDouble(x1, y1, x2, y2,
				color));
	}

	/**
	 * Adds a circle into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the circle's center
	 * @param y
	 *            the Y coordinate of the circle's center
	 * @param radius
	 *            the circle's radius
	 * @param filled
	 *            <tt>true</tt> if the circle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addCircle(final float x, final float y, final float radius,
			final Color color, final boolean filled) {
		return shapeQueue.add(new PaintableCircle2DFloat(x, y, radius, color,
				filled));
	}

	/**
	 * Adds a circle into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the circle's center
	 * @param y
	 *            the Y coordinate of the circle's center
	 * @param radius
	 *            the circle's radius
	 * @param filled
	 *            <tt>true</tt> if the circle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addCircle(final double x, final double y,
			final double radius, final Color color, final boolean filled) {
		return shapeQueue.add(new PaintableCircle2DDouble(x, y, radius, color,
				filled));
	}

	/**
	 * Adds a circle into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the circle's center
	 * @param y
	 *            the Y coordinate of the circle's center
	 * @param radius
	 *            the circle's radius
	 * @param filled
	 *            <tt>true</tt> if the circle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeCircle(final float x, final float y,
			final float radius, final Color color, final boolean filled) {
		return shapeQueue.remove(new PaintableCircle2DFloat(x, y, radius,
				color, filled));
	}

	/**
	 * Adds a circle into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the circle's center
	 * @param y
	 *            the Y coordinate of the circle's center
	 * @param radius
	 *            the circle's radius
	 * @param filled
	 *            <tt>true</tt> if the circle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeCircle(final double x, final double y,
			final double radius, final Color color, final boolean filled) {
		return shapeQueue.remove(new PaintableCircle2DDouble(x, y, radius,
				color, filled));
	}

	/**
	 * Adds a rectangle into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the lower-left corner of the rectangle
	 * @param y
	 *            the Y coordinate of the lower-left corner of the rectangle
	 * @param width
	 *            the width of the rectangle
	 * @param height
	 *            the height of the rectangle
	 * @param filled
	 *            <tt>true</tt> if the rectangle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addRectangle(final float x, final float y,
			final float width, final float height, final Color color,
			final boolean filled) {
		return shapeQueue.add(new PaintableRectangle2DFloat(x, y, width,
				height, color, filled));
	}

	/**
	 * Adds a rectangle into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the lower-left corner of the rectangle
	 * @param y
	 *            the Y coordinate of the lower-left corner of the rectangle
	 * @param width
	 *            the width of the rectangle
	 * @param height
	 *            the height of the rectangle
	 * @param filled
	 *            <tt>true</tt> if the rectangle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addRectangle(final double x, final double y,
			final double width, final double height, final Color color,
			final boolean filled) {
		return shapeQueue.add(new PaintableRectangle2DDouble(x, y, width,
				height, color, filled));
	}

	/**
	 * Removes a rectangle from the scene.
	 * 
	 * @param x
	 *            the X coordinate of the lower-left corner of the rectangle
	 * @param y
	 *            the Y coordinate of the lower-left corner of the rectangle
	 * @param width
	 *            the width of the rectangle
	 * @param height
	 *            the height of the rectangle
	 * @param filled
	 *            <tt>true</tt> if the rectangle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeRectangle(final float x, final float y,
			final float width, final float height, final Color color,
			final boolean filled) {
		return shapeQueue.remove(new PaintableRectangle2DFloat(x, y, width,
				height, color, filled));
	}

	/**
	 * Removes a rectangle from the scene.
	 * 
	 * @param x
	 *            the X coordinate of the lower-left corner of the rectangle
	 * @param y
	 *            the Y coordinate of the lower-left corner of the rectangle
	 * @param width
	 *            the width of the rectangle
	 * @param height
	 *            the height of the rectangle
	 * @param filled
	 *            <tt>true</tt> if the rectangle is solid (filled) or
	 *            <tt>false</tt> if it is just an outline
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeRectangle(final double x, final double y,
			final double width, final double height, final Color color,
			final boolean filled) {
		return shapeQueue.remove(new PaintableRectangle2DDouble(x, y, width,
				height, color, filled));
	}

	/**
	 * Adds a labeled point into the scene.
	 * 
	 * @param caption
	 *            the point's label
	 * @param x
	 *            the X coordinate of the point
	 * @param y
	 *            the Y coordinate of the point
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addPoint(final String caption, final float x, final float y,
			final Color color) {
		return shapeQueue.add(new PaintablePoint2DFloat(caption, x, y, color));
	}

	/**
	 * Adds a labeled point into the scene.
	 * 
	 * @param caption
	 *            the point's label
	 * @param x
	 *            the X coordinate of the point
	 * @param y
	 *            the Y coordinate of the point
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addPoint(final String caption, final double x,
			final double y, final Color color) {
		return shapeQueue.add(new PaintablePoint2DDouble(caption, x, y, color));
	}

	/**
	 * Removes a labeled point from the scene.
	 * 
	 * @param caption
	 *            the point's label
	 * @param x
	 *            the X coordinate of the point
	 * @param y
	 *            the Y coordinate of the point
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removePoint(final String caption, final float x,
			final float y, final Color color) {
		return shapeQueue
				.remove(new PaintablePoint2DFloat(caption, x, y, color));
	}

	/**
	 * Removes a labeled point from the scene.
	 * 
	 * @param caption
	 *            the point's label
	 * @param x
	 *            the X coordinate of the point
	 * @param y
	 *            the Y coordinate of the point
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removePoint(final String caption, final double x,
			final double y, final Color color) {
		return shapeQueue.remove(new PaintablePoint2DDouble(caption, x, y,
				color));
	}

	/**
	 * Adds a text label into the scene.
	 * 
	 * @param text
	 *            the text of the label
	 * @param x
	 *            the X coordinate of the upper-left corner of the text
	 * @param y
	 *            the Y coordinate of the upper-left corner of the text
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addText(final String text, final int x, final int y) {
		return shapeQueue.add(new PaintableText2D(text, x, y));
	}

	/**
	 * Removes a text label from the scene.
	 * 
	 * @param text
	 *            the text of the label
	 * @param x
	 *            the X coordinate of the upper-left corner of the text
	 * @param y
	 *            the Y coordinate of the upper-left corner of the text
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeText(final String text, final int x, final int y) {
		return shapeQueue.remove(new PaintableText2D(text, x, y));
	}

	/**
	 * Adds an axis-aligned orthogonal cross into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the center of the cross
	 * @param y
	 *            the Y coordinate of the center of the cross
	 * @param radius
	 *            the radius of the cross (the distance from the center of the
	 *            cross to each of its four endpoints)
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addCross(final float x, final float y, final float radius,
			final Color color) {
		addLine(x - radius, y, x + radius, y, color);
		return addLine(x, y - radius, x, y + radius, color);
	}

	/**
	 * Adds an axis-aligned orthogonal cross into the scene.
	 * 
	 * @param x
	 *            the X coordinate of the center of the cross
	 * @param y
	 *            the Y coordinate of the center of the cross
	 * @param radius
	 *            the radius of the cross (the distance from the center of the
	 *            cross to each of its four endpoints)
	 * @return <code>true</code>, as per contract with queue
	 * @since 3.0
	 */
	public boolean addCross(final double x, final double y,
			final double radius, final Color color) {
		addLine(x - radius, y, x + radius, y, color);
		return addLine(x, y - radius, x, y + radius, color);
	}

	/**
	 * Removes an axis-aligned orthogonal cross from the scene.
	 * 
	 * @param x
	 *            the X coordinate of the center of the cross
	 * @param y
	 *            the Y coordinate of the center of the cross
	 * @param radius
	 *            the radius of the cross (the distance from the center of the
	 *            cross to each of its four endpoints)
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeCross(final float x, final float y,
			final float radius, final Color color) {
		if (shapeQueue.contains(new PaintableLine2DFloat(x - radius, y, x
				+ radius, y, color))
				&& shapeQueue.contains(new PaintableLine2DFloat(x, y - radius,
						x, y + radius, color))) {
			removeLine(x - radius, y, x + radius, y, color);
			return removeLine(x, y - radius, x, y + radius, color);
		} else {
			return false;
		}
	}

	/**
	 * Removes an axis-aligned orthogonal cross from the scene.
	 * 
	 * @param x
	 *            the X coordinate of the center of the cross
	 * @param y
	 *            the Y coordinate of the center of the cross
	 * @param radius
	 *            the radius of the cross (the distance from the center of the
	 *            cross to each of its four endpoints)
	 * @return <code>true</code> if the shape queue contained the shape
	 * @since 3.0
	 */
	public boolean removeCross(final double x, final double y,
			final double radius, final Color color) {
		if (shapeQueue.contains(new PaintableLine2DDouble(x - radius, y, x
				+ radius, y, color))
				&& shapeQueue.contains(new PaintableLine2DDouble(x, y - radius,
						x, y + radius, color))) {
			removeLine(x - radius, y, x + radius, y, color);
			return removeLine(x, y - radius, x, y + radius, color);
		} else {
			return false;
		}
	}

	/**
	 * A simple subclass of <tt>JPanel</tt> whose <tt>paintComponent()</tt>
	 * method has been overridden to support the functionality of
	 * <tt>CanvasPlus</tt>. All graphics primitives are drawn into the
	 * graphics context of an instance of <tt>DrawingPane</tt>.
	 * <p>
	 * 
	 * Generally, only a single instance of <tt>DrawingPane</tt> is necessary
	 * per instance of <tt>CanvasPlus</tt>, extenders of <tt>CanvasPlus</tt>
	 * may find it useful to use more than one.
	 * 
	 * @since 2.0
	 */
	protected class DrawingPane extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 252813044843946311L;

		/**
		 * A modifier by which all primitives are scaled and by which the
		 * offsets are calculated.
		 */
		protected final float adjustment;

		/** The image offset in the X direction (i.e., the horizontal margin). */
		protected float xOffset;

		/** The image offset in the Y direction (i.e., the vertical margin). */
		protected float yOffset;

		/** The scaling value in the X direction. */
		protected float xScale;

		/** The scaling value in the Y direction. */
		protected float yScale;

		/**
		 * The preferred size of this <tt>JPanel</tt>, which influences the
		 * span of the scrollbars for this control.
		 * 
		 * @see javax.swing.JPanel#setPreferredSize(java.awt.Dimension)
		 */
		protected final Dimension preferredSize;

		/**
		 * Creates a new instance of <tt>DrawingPane</tt> with the given
		 * preferred size.
		 * 
		 * @since 2.0
		 */
		public DrawingPane(final Dimension preferredSize) {
			this.preferredSize = new Dimension(preferredSize.width,
					preferredSize.height);
			this.setPreferredSize(preferredSize);
			this.setBackground(CanvasPlus.this.backgroundColor);
			adjustment = 0.95f;
			xOffset = yOffset = 0f;
			xScale = yScale = 1f;
		}

		/**
		 * Paints this component using the given graphics context. This method
		 * is overridden and called potentially many times, so avoid burdensome
		 * calculations as often as possible. Be sure to call
		 * <tt>super.paintComponent</tt>.
		 * 
		 * @since 2.0
		 */
		protected void paintComponent(final Graphics graphics) {
			super.paintComponent(graphics);
			if (CanvasPlus.this.scaleMode <= SCALE_SCROLL) {
				xOffset = yOffset = 0f;
				xScale = yScale = 1f;
				preferredSize.width = (int) (CanvasPlus.this.max.x - CanvasPlus.this.min.x);
				preferredSize.width += 20;
				preferredSize.height = (int) (CanvasPlus.this.max.y - CanvasPlus.this.min.y);
				preferredSize.height += 20;
				this.setPreferredSize(preferredSize);
			} else if (CanvasPlus.this.scaleMode == SCALE_FIT) {
				xOffset = frame.getSize().width * (1 - adjustment) * 0.5f;
				yOffset = frame.getSize().height * (1 - adjustment) * 0.5f;
				xScale = (this.getSize().width * adjustment)
						/ (CanvasPlus.this.max.x - CanvasPlus.this.min.x);
				yScale = (this.getSize().height * adjustment)
						/ (CanvasPlus.this.max.y - CanvasPlus.this.min.y);
			} else if (CanvasPlus.this.scaleMode == SCALE_FIXED_ASPECT) {
				final Dimension size = this.getSize();
				if (size.width > size.height) {
					float w = size.height * aspectRatio;
					xScale = (adjustment * w)
							/ (CanvasPlus.this.max.x - CanvasPlus.this.min.x);
					yScale = (frame.getSize().height * adjustment)
							/ (CanvasPlus.this.max.y - CanvasPlus.this.min.y);
					xOffset = (size.width - w) * 0.5f
							+ (size.width * (1 - adjustment) * 0.5f);
					yOffset = (size.height) * (1 - adjustment) * 0.5f;
				} else {
					float h = size.width * aspectRatio;
					xScale = (frame.getSize().width * adjustment)
							/ (CanvasPlus.this.max.x - CanvasPlus.this.min.x);
					yScale = (adjustment * h)
							/ (CanvasPlus.this.max.y - CanvasPlus.this.min.y);
					xOffset = size.width * (1 - adjustment) * 0.5f;
					yOffset = (size.height - h) * 0.5f
							+ (size.height * (1 - adjustment) * 0.5f);

				}
			}
			final Graphics2D graphics2d = (Graphics2D) graphics;
			/*
			 * make sure the paint method gets a lock on the shape queue to
			 * avoid a ConcurrentModificationException on the shape queue
			 */
			synchronized (CanvasPlus.this.shapeQueue) {
				for (final Paintable2D shape : shapeQueue) {
					shape.paint(graphics2d, xScale, yScale, xOffset, yOffset);
				}
			}
		}
	}

	/**
	 * Supertype for all objects paintable on <tt>CanvasPlus</tt>. Extenders
	 * adding new primitive types to be directly added to the shape queue must
	 * ensure that anything they place in the shape queue implements this
	 * interface at risk of a <tt>ClassCastException</tt> being thrown by
	 * {@link CanvasPlus.DrawingPane#paintComponent(Graphics)}.
	 * 
	 * @author Evan Machusak
	 * @version 1.0
	 * @since 2.0
	 */
	protected interface Paintable2D {
		/**
		 * Paints an object which implements the Paintable2D interface into the
		 * scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset);
	}

	/**
	 * A simple abstract class for representing points which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected abstract class PaintablePoint2D implements Paintable2D {

		/** caption for this point */
		protected String caption;

		/** color of this point */
		protected Color color;

		/**
		 * Gets the X coordinate of this point
		 * 
		 * @return X coordinate of this point
		 */
		public abstract double getX();

		/**
		 * Gets the Y coordinate of this point
		 * 
		 * @return Y coordinate of this point
		 */
		public abstract double getY();

		/**
		 * Determines whether another object is equal to this PaintablePoint2D.
		 * The result is true if and only if the argument is not null and is a
		 * PaintablePoint2D object that has the same caption, X and Y
		 * coordinates, and color.
		 * 
		 * @param obj
		 *            the object to test for equality with this PaintablePoint2D
		 * @return <code>true</code> if the objects are the same,
		 *         <code>false</code> otherwise
		 */
		public final boolean equals(final Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof PaintablePoint2D) {
				final PaintablePoint2D p = (PaintablePoint2D) obj;
				return (getX() == p.getX())
						&& (getY() == p.getY())
						&& (color.equals(p.color) && (caption.equals(p.caption)));
			}
			return false;
		}

		/**
		 * Computes a hash code for this PaintablePoint2D.
		 * 
		 * @return a hash code value for this PaintablePoint2D
		 */
		public final int hashCode() {
			int hash = 17;
			final long xBits = Double.doubleToLongBits(getX());
			final long yBits = Double.doubleToLongBits(getY());
			hash = 37 * hash + (int) (xBits ^ (xBits >>> 32));
			hash = 37 * hash + (int) (yBits ^ (yBits >>> 32));
			hash = 37 * hash + caption.hashCode();
			hash = 37 * hash + color.hashCode();
			return hash;
		}

		/**
		 * Returns a string representation of this PaintablePoint2D.
		 * 
		 * @return a string representation of this PaintablePoint2D
		 */
		public String toString() {
			return getClass().getName() + "[caption=" + caption + ",x="
					+ getX() + ",y=" + getY() + ",color=" + color.toString()
					+ "]";
		}
	}

	/**
	 * A simple class for representing points which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintablePoint2DFloat extends PaintablePoint2D {
		/** (x,y) coordinate representation of this point */
		protected final Point2D.Float pt;

		/**
		 * Constructs and initializes a PaintablePoint2DFloat object with the
		 * specified parameters.
		 * 
		 * @param caption
		 *            caption of this point
		 * @param x
		 *            X coordinate of this point
		 * @param y
		 *            Y coordinate of this point
		 * @param color
		 *            color of this point
		 */
		public PaintablePoint2DFloat(final String caption, final float x,
				final float y, final Color color) {
			if (x >= CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = x + 10 + caption.length() * 5;
			else if (x < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = x;
			if (y >= CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = y + 15;
			else if (y < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = y;
			pt = new Point2D.Float(x, y);
			this.caption = caption;
			this.color = color;
		}

		/**
		 * Paints this PaintablePoint2DFloat into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final Ellipse2D.Float canvasPt = new Ellipse2D.Float(pt.x, pt.y, 4,
					4);
			canvasPt.y = CanvasPlus.this.max.y - canvasPt.y;
			canvasPt.x *= xScale;
			canvasPt.x += xOffset - 2;
			canvasPt.y *= yScale;
			canvasPt.y += yOffset - 2;
			graphics.setColor(color);
			graphics.draw(canvasPt);
			graphics.fill(canvasPt);
			if (!caption.equals("")) {
				graphics.setFont(new Font("Dialog", Font.PLAIN, 10));
				graphics.drawString(this.caption, canvasPt.x + 5,
						canvasPt.y + 12);
			}
		}

		/**
		 * Gets the X coordinate of this point
		 * 
		 * @return X coordinate of this point
		 */
		@Override
		public double getX() {
			return pt.x;
		}

		/**
		 * Gets the Y coordinate of this point
		 * 
		 * @return Y coordinate of this point
		 */
		@Override
		public double getY() {
			return pt.y;
		}
	}

	/**
	 * A simple class for representing points which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintablePoint2DDouble extends PaintablePoint2D {
		/** (x,y) coordinate representation of this point */
		protected final Point2D.Double pt;

		/**
		 * Constructs and initializes a PaintablePoint2DDouble object with the
		 * specified parameters.
		 * 
		 * @param caption
		 *            caption of this point
		 * @param x
		 *            X coordinate of this point
		 * @param y
		 *            Y coordinate of this point
		 * @param color
		 *            color of this point
		 */
		public PaintablePoint2DDouble(final String caption, final double x,
				final double y, final Color color) {
			if (x >= CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = (float) x + 10 + caption.length() * 5;
			else if (x < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = (float) x;
			if (y >= CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = (float) y + 15;
			else if (y < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = (float) y;
			pt = new Point2D.Double(x, y);
			this.caption = caption;
			this.color = color;
		}

		/**
		 * Paints this PaintablePoint2DDouble into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final Ellipse2D.Double canvasPt = new Ellipse2D.Double(pt.x, pt.y,
					4, 4);
			canvasPt.y = CanvasPlus.this.max.y - canvasPt.y;
			canvasPt.x *= xScale;
			canvasPt.x += xOffset - 2;
			canvasPt.y *= yScale;
			canvasPt.y += yOffset - 2;
			graphics.setColor(color);
			graphics.draw(canvasPt);
			graphics.fill(canvasPt);
			if (!caption.equals("")) {
				graphics.setFont(new Font("Dialog", Font.PLAIN, 10));
				graphics.drawString(this.caption, (float) canvasPt.x + 5,
						(float) canvasPt.y + 12);
			}
		}

		/**
		 * Gets the X coordinate of this point
		 * 
		 * @return X coordinate of this point
		 */
		@Override
		public double getX() {
			return pt.x;
		}

		/**
		 * Gets the Y coordinate of this point
		 * 
		 * @return X coordinate of this point
		 */
		@Override
		public double getY() {
			return pt.y;
		}
	}

	/**
	 * A simple abstract class for representing line segments which implements
	 * the <tt>Paintable2D</tt> interface.
	 * <p>
	 * Lines are one way, that is, Line(A,B) is not the same as Line(B,A)
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected abstract class PaintableLine2D implements Paintable2D {
		/** color of this line segment */
		protected Color color;

		/**
		 * Gets the X coordinate of the start point of this line segment.
		 * 
		 * @return X coordinate of the start point of this line segment
		 */
		public abstract double getX1();

		/**
		 * Gets the Y coordinate of the start point of this line segment.
		 * 
		 * @return Y coordinate of the start point of this line segment
		 */
		public abstract double getY1();

		/**
		 * Gets the X coordinate of the end point of this line segment.
		 * 
		 * @return X coordinate of the end point of this line segment
		 */
		public abstract double getX2();

		/**
		 * Gets the Y coordinate of the end point of this line segment.
		 * 
		 * @return Y coordinate of the end point of this line segment
		 */
		public abstract double getY2();

		/**
		 * Determines whether another object is equal to this PaintableLine2D.
		 * The result is true if and only if the argument is not null and is a
		 * PaintableLine2D object that has the same start and end points, and
		 * color.
		 * 
		 * @param obj
		 *            the object to test for equality with this PaintableLine2D
		 * @return <code>true</code> if the objects are the same,
		 *         <code>false</code> otherwise
		 */
		public final boolean equals(final Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof PaintableLine2D) {
				final PaintableLine2D l = (PaintableLine2D) obj;
				return (getX1() == l.getX1()) && (getY1() == l.getY1())
						&& (getX2() == l.getX2()) && (getY2() == l.getY2())
						&& (color.equals(l.color));
			}
			return false;
		}

		/**
		 * Computes a hash code for this PaintableLine2D.
		 * 
		 * @return a hash code value for this PaintableLine2D
		 */
		public final int hashCode() {
			int hash = 5;
			final long x1Bits = Double.doubleToLongBits(getX1());
			final long y1Bits = Double.doubleToLongBits(getY1());
			final long x2Bits = Double.doubleToLongBits(getX2());
			final long y2Bits = Double.doubleToLongBits(getY2());
			hash = 37 * hash + (int) (x1Bits ^ (x1Bits >>> 32));
			hash = 37 * hash + (int) (y1Bits ^ (y1Bits >>> 32));
			hash = 37 * hash + (int) (x2Bits ^ (x2Bits >>> 32));
			hash = 37 * hash + (int) (y2Bits ^ (y2Bits >>> 32));
			hash = 37 * hash + color.hashCode();
			return hash;
		}

		/**
		 * Returns a string representation of this PaintableLine2D.
		 * 
		 * @return a string representation of this PaintableLine2D
		 */
		public String toString() {
			return getClass().getName() + "[x1=" + getX1() + ",y1=" + getY1()
					+ ",x2=" + getX2() + ",y2=" + getY2() + ",color="
					+ color.toString() + "]";
		}
	}

	/**
	 * A simple class for representing line segments which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintableLine2DFloat extends PaintableLine2D {
		/** 2D representation of this line segment */
		protected final Line2D.Float line;

		/**
		 * Constructs and initializes this line segment with the specified
		 * parameters.
		 * 
		 * @param x1
		 *            X coordinate of start point
		 * @param y1
		 *            Y coordinate of start point
		 * @param x2
		 *            X coordinate of end point
		 * @param y2
		 *            Y coordinate of end point
		 * @param color
		 *            color of this line segment
		 */
		public PaintableLine2DFloat(final float x1, final float y1,
				final float x2, final float y2, final Color color) {
			if (x1 > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = x1;
			else if (x1 < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = x1;
			if (y1 > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = y1;
			else if (y1 < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = y1;
			if (x2 > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = x2;
			else if (x2 < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = x2;
			if (y2 > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = y2;
			else if (y2 < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = y2;
			line = new Line2D.Float(x1, y1, x2, y2);
			this.color = color;
		}

		/**
		 * Gets the X coordinate of the start point of this line segment.
		 * 
		 * @return X coordinate of the start point of this line segment
		 */
		@Override
		public double getX1() {
			return line.x1;
		}

		/**
		 * Gets the Y coordinate of the start point of this line segment.
		 * 
		 * @return Y coordinate of the start point of this line segment
		 */
		@Override
		public double getY1() {
			return line.y1;
		}

		/**
		 * Gets the X coordinate of the end point of this line segment.
		 * 
		 * @return X coordinate of the end point of this line segment
		 */
		@Override
		public double getX2() {
			return line.x2;
		}

		/**
		 * Gets the Y coordinate of the end point of this line segment.
		 * 
		 * @return Y coordinate of the end point of this line segment
		 */
		@Override
		public double getY2() {
			return line.y2;
		}

		/**
		 * Paints this PaintableLine2DFloat into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final Line2D.Float canvasLine = new Line2D.Float(line.x1, line.y1,
					line.x2, line.y2);

			/* flip the Y coordinates so the origin is in the lower-left corner */
			canvasLine.y1 = (CanvasPlus.this.max.y - canvasLine.y1);
			canvasLine.y2 = (CanvasPlus.this.max.y - canvasLine.y2);
			canvasLine.x1 *= xScale;
			canvasLine.x1 += xOffset;
			canvasLine.x2 *= xScale;
			canvasLine.x2 += xOffset;
			canvasLine.y1 *= yScale;
			canvasLine.y1 += yOffset;
			canvasLine.y2 *= yScale;
			canvasLine.y2 += yOffset;
			graphics.setColor(color);
			graphics.draw(canvasLine);
		}
	}

	/**
	 * A simple class for representing line segments which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintableLine2DDouble extends PaintableLine2D {
		/** 2D representation of this line segment */
		protected final Line2D.Double line;

		/**
		 * Constructs and initializes this line segment with the specified
		 * parameters.
		 * 
		 * @param x1
		 *            X coordinate of start point
		 * @param y1
		 *            Y coordinate of start point
		 * @param x2
		 *            X coordinate of end point
		 * @param y2
		 *            Y coordinate of end point
		 * @param color
		 *            color of this line segment
		 */
		public PaintableLine2DDouble(final double x1, final double y1,
				final double x2, final double y2, final Color color) {
			if (x1 > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = (float) x1;
			else if (x1 < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = (float) x1;
			if (y1 > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = (float) y1;
			else if (y1 < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = (float) y1;
			if (x2 > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = (float) x2;
			else if (x2 < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = (float) x2;
			if (y2 > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = (float) y2;
			else if (y2 < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = (float) y2;
			line = new Line2D.Double(x1, y1, x2, y2);
			this.color = color;
		}

		/**
		 * Gets the X coordinate of the start point of this line segment.
		 * 
		 * @return X coordinate of the start point of this line segment
		 */
		@Override
		public double getX1() {
			return line.x1;
		}

		/**
		 * Gets the Y coordinate of the start point of this line segment.
		 * 
		 * @return Y coordinate of the start point of this line segment
		 */
		@Override
		public double getY1() {
			return line.y1;
		}

		/**
		 * Gets the X coordinate of the end point of this line segment.
		 * 
		 * @return X coordinate of the end point of this line segment
		 */
		@Override
		public double getX2() {
			return line.x2;
		}

		/**
		 * Gets the Y coordinate of the end point of this line segment.
		 * 
		 * @return Y coordinate of the end point of this line segment
		 */
		@Override
		public double getY2() {
			return line.y2;
		}

		/**
		 * Paints this PaintableLine2DDouble into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final Line2D.Double canvasLine = new Line2D.Double(line.x1,
					line.y1, line.x2, line.y2);

			/* flip the Y coordinates so the origin is in the lower-left corner */
			canvasLine.y1 = (CanvasPlus.this.max.y - canvasLine.y1);
			canvasLine.y2 = (CanvasPlus.this.max.y - canvasLine.y2);
			canvasLine.x1 *= xScale;
			canvasLine.x1 += xOffset;
			canvasLine.x2 *= xScale;
			canvasLine.x2 += xOffset;
			canvasLine.y1 *= yScale;
			canvasLine.y1 += yOffset;
			canvasLine.y2 *= yScale;
			canvasLine.y2 += yOffset;
			graphics.setColor(color);
			graphics.draw(canvasLine);
		}
	}

	/**
	 * A simple abstract class for representing rectangles which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected abstract class PaintableRectangle2D implements Paintable2D {
		/** color of this rectangle */
		protected Color color;

		/** whether this rectangle is filled */
		protected boolean filled;

		/**
		 * Gets the X coordinate of the lower-left corner of this rectangular
		 * shape.
		 * 
		 * @return X coordinate of the lower-left corner of this rectangular
		 *         shape
		 */
		public abstract double getX();

		/**
		 * Gets the Y coordinate of the lower-left corner of this rectangular
		 * shape.
		 * 
		 * @return Y coordinate of the lower-left corner of this rectangular
		 *         shape
		 */
		public abstract double getY();

		/**
		 * Gets the width of this rectangle.
		 * 
		 * @return width of this rectangle
		 */
		public abstract double getWidth();

		/**
		 * Gets the height of this rectangle
		 * 
		 * @return height of this rectangle
		 */
		public abstract double getHeight();

		/**
		 * Determines whether another object is equal to this
		 * PaintableRectangle2D. The result is true if and only if the argument
		 * is not null and is a PaintableRectangle2D object that has the same
		 * origin point, width, height, and color.
		 * 
		 * @param obj
		 *            the object to test for equality with this
		 *            PaintableRectangle2D
		 * @return <code>true</code> if the objects are the same,
		 *         <code>false</code> otherwise
		 */
		public final boolean equals(final Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof PaintableRectangle2D) {
				PaintableRectangle2D r = (PaintableRectangle2D) obj;
				return (getX() == r.getX()) && (getY() == r.getY())
						&& (getWidth() == r.getWidth())
						&& (getHeight() == r.getHeight())
						&& (color.equals(r.color)) && (filled == r.filled);
			}
			return false;
		}

		/**
		 * Computes a hash code for this PaintableRectangle2D.
		 * 
		 * @return a hash code value for this PaintableRectangle2D
		 */
		public final int hashCode() {
			int hash = 15;
			final long xBits = Double.doubleToLongBits(getX());
			final long yBits = Double.doubleToLongBits(getY());
			final long wBits = Double.doubleToLongBits(getWidth());
			final long hBits = Double.doubleToLongBits(getHeight());
			hash = 37 * hash + (int) (xBits ^ (xBits >>> 32));
			hash = 37 * hash + (int) (yBits ^ (yBits >>> 32));
			hash = 37 * hash + (int) (wBits ^ (hBits >>> 32));
			hash = 37 * hash + (int) (hBits ^ (wBits >>> 32));
			hash = 37 * hash + color.hashCode();
			hash = 37 * hash + (filled ? 0 : 1);
			return hash;
		}

		/**
		 * Gets a string representation of this rectangular shape.
		 * 
		 * @return string representation of this rectangular shape
		 */
		public String toString() {
			return getClass().getName() + "[x=" + getX() + ",y=" + getY()
					+ ",width=" + getWidth() + ",height=" + getHeight()
					+ ",color=" + color.toString() + ",filled=" + filled + "]";
		}
	}

	/**
	 * A simple class for representing rectangles which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintableRectangle2DFloat extends PaintableRectangle2D {
		/** 2D representation of this rectangle */
		protected final Rectangle2D.Float rect;

		/**
		 * Constructs and initializes this PaintableRectangle2DFloat with the
		 * specified parameters.
		 * 
		 * @param x
		 *            X coordinate of the origin of this rectangle
		 * @param y
		 *            Y coordinate of the origin of this rectangle
		 * @param w
		 *            width of this rectangle
		 * @param h
		 *            height of this rectangle
		 * @param color
		 *            color of this rectangle
		 * @param filled
		 *            whether this rectangle is filled
		 */
		public PaintableRectangle2DFloat(final float x, final float y,
				final float w, final float h, Color color, boolean filled) {
			if (x + w > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = (x + w);
			else if (x < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = x;
			if (y + h > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = (y + h);
			else if (y < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = y;
			rect = new Rectangle2D.Float(x, y, w, h);
			this.filled = filled;
			this.color = color;
		}

		/**
		 * Gets the X coordinate of the lower-left corner of this rectangular
		 * shape.
		 * 
		 * @return X coordinate of the lower-left corner of this rectangular
		 *         shape
		 */
		@Override
		public double getX() {
			return rect.x;
		}

		/**
		 * Gets the Y coordinate of the lower-left corner of this rectangular
		 * shape.
		 * 
		 * @return Y coordinate of the lower-left corner of this rectangular
		 *         shape
		 */
		@Override
		public double getY() {
			return rect.y;
		}

		/**
		 * Gets the width this rectangular shape.
		 * 
		 * @return width of this rectangular shape
		 */
		@Override
		public double getWidth() {
			return rect.width;
		}

		/**
		 * Gets the height this rectangular shape.
		 * 
		 * @return height of this rectangular shape
		 */
		@Override
		public double getHeight() {
			return rect.height;
		}

		/**
		 * Paints this PaintableRectangle2DFloat into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final Rectangle2D.Float canvasRect = new Rectangle2D.Float(rect.x,
					rect.y, rect.width, rect.height);
			canvasRect.y = CanvasPlus.this.max.y - canvasRect.y
					- canvasRect.height;
			canvasRect.x *= xScale;
			canvasRect.x += xOffset;
			canvasRect.y *= yScale;
			canvasRect.y += yOffset;
			canvasRect.width *= xScale;
			canvasRect.height *= yScale;
			graphics.setColor(color);
			graphics.draw(canvasRect);
			if (filled) {
				graphics.fill(canvasRect);
			}
		}
	}

	/**
	 * A simple class for representing rectangles which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintableRectangle2DDouble extends PaintableRectangle2D {
		/** 2D representation of this rectangle */
		protected final Rectangle2D.Double rect;

		/**
		 * Constructs and initializes this PaintableRectangle2DDouble with the
		 * specified parameters.
		 * 
		 * @param x
		 *            X coordinate of the origin of this rectangle
		 * @param y
		 *            Y coordinate of the origin of this rectangle
		 * @param w
		 *            width of this rectangle
		 * @param h
		 *            height of this rectangle
		 * @param color
		 *            color of this rectangle
		 * @param filled
		 *            whether this rectangle is filled
		 */
		public PaintableRectangle2DDouble(final double x, final double y,
				final double w, final double h, final Color color,
				final boolean filled) {
			if (x + w > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = (float) (x + w);
			else if (x < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = (float) x;
			if (y + h > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = (float) (y + h);
			else if (y < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = (float) y;

			rect = new Rectangle2D.Double(x, y, w, h);
			this.filled = filled;
			this.color = color;
		}

		/**
		 * Gets the X coordinate of the lower-left corner of this rectangular
		 * shape.
		 * 
		 * @return X coordinate of the lower-left corner of this rectangular
		 *         shape
		 */
		@Override
		public double getX() {
			return rect.x;
		}

		/**
		 * Gets the Y coordinate of the lower-left corner of this rectangular
		 * shape.
		 * 
		 * @return Y coordinate of the lower-left corner of this rectangular
		 *         shape
		 */
		@Override
		public double getY() {
			return rect.y;
		}

		/**
		 * Gets the width this rectangular shape.
		 * 
		 * @return width of this rectangular shape
		 */
		@Override
		public double getWidth() {
			return rect.width;
		}

		/**
		 * Gets the height this rectangular shape.
		 * 
		 * @return height of this rectangular shape
		 */
		@Override
		public double getHeight() {
			return rect.height;
		}

		/**
		 * Paints this PaintableRectangle2DDouble into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final Rectangle2D.Double canvasRect = new Rectangle2D.Double(
					rect.x, rect.y, rect.width, rect.height);
			canvasRect.y = CanvasPlus.this.max.y - canvasRect.y
					- canvasRect.height;
			canvasRect.x *= xScale;
			canvasRect.x += xOffset;
			canvasRect.y *= yScale;
			canvasRect.y += yOffset;
			canvasRect.width *= xScale;
			canvasRect.height *= yScale;
			graphics.setColor(color);
			graphics.draw(canvasRect);
			if (filled) {
				graphics.fill(canvasRect);
			}
		}
	}

	/**
	 * A simple abstract class for representing circles which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected abstract class PaintableCircle2D implements Paintable2D {
		/** color of this rectangle */
		protected Color color;

		/** whether this rectangle is filled */
		protected boolean filled;

		/**
		 * Gets the X coordinate of the center of this circle.
		 * 
		 * @return X coordinate of the center of this circle
		 */
		public abstract double getCenterX();

		/**
		 * Gets the Y coordinate of the center of this circle.
		 * 
		 * @return Y coordinate of the center of this circle
		 */
		public abstract double getCenterY();

		/**
		 * Gets the radius of this circle.
		 * 
		 * @return radius of this circle
		 */
		public abstract double getRadius();

		/**
		 * Determines whether another object is equal to this PaintableCircle2D.
		 * The result is true if and only if the argument is not null and is a
		 * PaintableCircle2D object that has the same origin point, width,
		 * height, and color.
		 * 
		 * @param obj
		 *            the object to test for equality with this
		 *            PaintableCircle2D
		 * @return <code>true</code> if the objects are the same,
		 *         <code>false</code> otherwise
		 */
		public final boolean equals(final Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof PaintableCircle2D) {
				final PaintableCircle2D c = (PaintableCircle2D) obj;
				return (getCenterX() == c.getCenterX())
						&& (getCenterY() == c.getCenterY())
						&& (getRadius() == c.getRadius())
						&& (color.equals(c.color)) && (filled == c.filled);
			}
			return false;
		}

		/**
		 * Computes a hash code for this PaintableCircle2D.
		 * 
		 * @return a hash code value for this PaintableCircle2D
		 */
		public final int hashCode() {
			int hash = 42;
			final long xBits = Double.doubleToLongBits(getCenterX());
			final long yBits = Double.doubleToLongBits(getCenterY());
			final long rBits = Double.doubleToLongBits(getRadius());
			hash = 37 * hash + (int) (xBits ^ (xBits >>> 32));
			hash = 37 * hash + (int) (yBits ^ (yBits >>> 32));
			hash = 37 * hash + (int) (rBits ^ (rBits >>> 32));
			hash = 37 * hash + color.hashCode();
			hash = 37 * hash + (filled ? 0 : 1);
			return hash;
		}

		/**
		 * Gets a string representation of this rectangular shape.
		 * 
		 * @return string representation of this rectangular shape
		 */
		public String toString() {
			return getClass().getName() + "[centerX=" + getCenterX() + ",centerY="
					+ getCenterY() + ",radius=" + getRadius() + ",color="
					+ color.toString() + ",filled=" + filled + "]";
		}
	}

	/**
	 * A simple class for representing circles which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintableCircle2DFloat extends PaintableCircle2D {
		/** center of this circle */
		protected final Point2D.Float center;

		/** radius of this circle */
		protected final float radius;

		/**
		 * Constructs and initializes this PaintableCircle2DFloat with the
		 * specified parameters.
		 * 
		 * @param x
		 *            X coordinate of the center of this circle
		 * @param y
		 *            Y coordinate of the center of this circle
		 * @param radius
		 *            radius of this circle
		 * @param color
		 *            color of this circle
		 * @param filled
		 *            whether this circle is filled
		 */
		public PaintableCircle2DFloat(float x, float y, float radius,
				Color color, boolean filled) {
			if (x + radius > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = x + radius;
			else if (x - radius < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = x - radius;
			if (y + radius > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = y + radius;
			else if (y - radius < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = y - radius;
			center = new Point2D.Float(x, y);
			this.radius = radius;
			this.filled = filled;
			this.color = color;
		}

		/**
		 * Gets the X coordinate of the center of this circle.
		 * 
		 * @return X coordinate of the center of this circle
		 */
		@Override
		public double getCenterX() {
			return center.x;
		}

		/**
		 * Gets the Y coordinate of the center of this circle.
		 * 
		 * @return Y coordinate of the center of this circle
		 */
		@Override
		public double getCenterY() {
			return center.y;
		}

		/**
		 * Gets the radius of this circle.
		 * 
		 * @return radius of this circle
		 */
		@Override
		public double getRadius() {
			return radius;
		}

		/**
		 * Paints this PaintableCircle2DFloat into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final float diameter = radius * 2;
			final Ellipse2D.Float canvasCircle = new Ellipse2D.Float(center.x
					- radius, center.y + radius, diameter, diameter);
			canvasCircle.y = CanvasPlus.this.max.y - canvasCircle.y;
			canvasCircle.x *= xScale;
			canvasCircle.x += xOffset;
			canvasCircle.y *= yScale;
			canvasCircle.y += yOffset;
			canvasCircle.width *= xScale;
			canvasCircle.height *= yScale;
			graphics.setColor(color);
			graphics.draw(canvasCircle);
			if (filled) {
				graphics.fill(canvasCircle);
			}
		}
	}

	/**
	 * A simple class for representing circles which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintableCircle2DDouble extends PaintableCircle2D {
		/** center of this circle */
		protected final Point2D.Double center;

		/** radius of this circle */
		protected final double radius;

		/**
		 * Constructs and initializes this PaintableCircle2DDouble with the
		 * specified parameters.
		 * 
		 * @param x
		 *            X coordinate of the center of this circle
		 * @param y
		 *            Y coordinate of the center of this circle
		 * @param radius
		 *            radius of this circle
		 * @param color
		 *            color of this circle
		 * @param filled
		 *            whether this circle is filled
		 */
		public PaintableCircle2DDouble(final double x, final double y,
				final double radius, final Color color, final boolean filled) {
			if (x + radius > CanvasPlus.this.max.x)
				CanvasPlus.this.max.x = (float) (x + radius);
			else if (x - radius < CanvasPlus.this.min.x)
				CanvasPlus.this.min.x = (float) (x - radius);
			if (y + radius > CanvasPlus.this.max.y)
				CanvasPlus.this.max.y = (float) (y + radius);
			else if (y - radius < CanvasPlus.this.min.y)
				CanvasPlus.this.min.y = (float) (y - radius);
			center = new Point2D.Double(x, y);
			this.radius = radius;
			this.filled = filled;
			this.color = color;
		}

		/**
		 * Gets the X coordinate of the center of this circle.
		 * 
		 * @return X coordinate of the center of this circle
		 */
		@Override
		public double getCenterX() {
			return center.x;
		}

		/**
		 * Gets the Y coordinate of the center of this circle.
		 * 
		 * @return Y coordinate of the center of this circle
		 */
		@Override
		public double getCenterY() {
			return center.y;
		}

		/**
		 * Gets the radius of this circle.
		 * 
		 * @return radius of this circle
		 */
		@Override
		public double getRadius() {
			return radius;
		}

		/**
		 * Paints this PaintableCircle2DDouble into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, final float xOffset, final float yOffset) {
			final double diameter = radius * 2;
			final Ellipse2D.Double canvasCircle = new Ellipse2D.Double(center.x
					- radius, center.y + radius, diameter, diameter);
			canvasCircle.y = CanvasPlus.this.max.y - canvasCircle.y;
			canvasCircle.x *= xScale;
			canvasCircle.x += xOffset;
			canvasCircle.y *= yScale;
			canvasCircle.y += yOffset;
			canvasCircle.width *= xScale;
			canvasCircle.height *= yScale;
			graphics.setColor(color);
			graphics.draw(canvasCircle);
			if (filled) {
				graphics.fill(canvasCircle);
			}
		}
	}

	/**
	 * A simple class for representing text which implements the
	 * <tt>Paintable2D</tt> interface.
	 * 
	 * @author Evan Machusak
	 * @author Ben Zoller
	 * @version 2.0
	 * @since 3.0
	 */
	protected final class PaintableText2D implements Paintable2D {
		/** text of this string */
		protected final String text;

		/** X coordinate of this string */
		protected final int x;

		/** Y coordinate of this string */
		protected final int y;

		/** font of this string */
		protected final Font font;

		/** affine transform of this string */
		protected final AffineTransform transform;

		/**
		 * Constructs and initializes this text with the specified parameters.
		 * Takes on the font of CanvasPlus.
		 * 
		 * @param text
		 *            text of this string
		 * @param x
		 *            X coordinate of this string
		 * @param y
		 *            Y coordinate of this string
		 */
		public PaintableText2D(final String text, final int x, final int y) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.font = CanvasPlus.this.font;
			this.transform = new AffineTransform();
		}

		/**
		 * Paints this PaintableText2D into the scene.
		 * 
		 * @param graphics
		 *            2D graphical context
		 * @param xScale
		 *            scaling value in the X direction
		 * @param yScale
		 *            scaling value in the Y direction
		 * @param xOffset
		 *            offset value in the X direction
		 * @param yOffset
		 *            offset value in the Y direction
		 */
		public void paint(final Graphics2D graphics, final float xScale,
				final float yScale, float xOffset, float yOffset) {
			float nx = x;
			float ny = (CanvasPlus.this.max.y - y);
			nx *= xScale;
			ny *= yScale;
			nx += xOffset;
			ny += yOffset;
			this.transform.setToScale(xScale, yScale);
			graphics.setFont(this.font.deriveFont(this.transform));
			graphics.drawString(this.text, nx, ny);
		}

		/**
		 * Determines whether another object is equal to this PaintableText2D.
		 * The result is true if and only if the argument is not null and is a
		 * PaintableText2D object that has the same text, font, (x,y) location,
		 * and affine transform.
		 * 
		 * @param obj
		 *            the object to test for equality with this PaintableText2D
		 * @return <code>true</code> if the objects are the same,
		 *         <code>false</code> otherwise
		 */
		public final boolean equals(final Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof PaintableText2D) {
				final PaintableText2D t = (PaintableText2D) obj;
				return text.equals(text) && (x == t.x) && (y == t.y)
						&& font.equals(t.font) && transform.equals(t.transform);
			}
			return false;
		}

		/**
		 * Computes a hash code for this PaintableText2D.
		 * 
		 * @return a hash code value for this PaintableText2D
		 */
		public final int hashCode() {
			int hash = 12;
			hash = 37 * hash + x;
			hash = 37 * hash + y;
			hash = 37 * hash + font.hashCode();
			hash = 37 * hash + transform.hashCode();
			return hash;
		}

		/**
		 * Returns a string representation of this PaintableText2D.
		 * 
		 * @return a string representation of this PaintableText2D
		 */
		public String toString() {
			return getClass().getName() + "[text=" + text + ",x=" + getX()
					+ ",y=" + getY() + ",font=" + font.toString()
					+ ",transform=" + transform.toString() + "]";
		}
	}
}
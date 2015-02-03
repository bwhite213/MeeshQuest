package cmsc420.drawing;

/**
 * The <tt>Drawable2D</tt> interface specifies that its implementors can
 * portray themselves in 2D form via an instance of <tt>CanvasPlus</tt>.
 * Implementors should add the appropriate geometry into the <tt>CanvasPlus</tt>
 * and exit; they should not call its {@link CanvasPlus#draw()} method.
 * <b>Warning:</b> implementors must <i>not</i> invoke
 * {@link CanvasPlus#add(Drawable2D)} by passing <tt>this</tt> as a parameter.
 * The implementation of {@link CanvasPlus#add(Drawable2D)} itself calls the
 * <tt>draw()</tt> method as specified in this interface, thereby creating
 * circular method invocation.
 * 
 * @author Evan Machusak (<a href="mailto:emach@cs.umd.edu">emach@cs.umd.edu</a>)
 * @version 1.0
 */
public interface Drawable2D {

	/**
	 * Draws this object into the 2D scene specified by the given
	 * <tt>CanvasPlus</tt>.
	 * 
	 * @param cp
	 *            the <tt>CanvasPlus</tt> to draw this object into
	 * @since 1.0
	 */
	public void draw(CanvasPlus cp);

}
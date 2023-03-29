package bigtrace.measure;

import net.imglib2.Point;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

/** class providing pixel rounded coordinates
 * of a 2D circle with a center at (0,0) **/
public class Circle2DMeasure {

	
	public HyperSphere< IntType > circle2D;
	public HyperSphereCursor< IntType > cursorCircle;
	private int nRadius = 0;
	
	public Circle2DMeasure()
	{
		//fake small RAI
		ArrayImg<IntType, IntArray> costInt = ArrayImgs.ints(new long[] {1,1});
		//center at zero
		Point center = new Point( 2 );
		center.setPosition( 0 , 0 );
		center.setPosition( 0 , 1 );
		circle2D =	new HyperSphere<>( Views.extendZero(costInt), center, 0);
		cursorCircle = circle2D.localizingCursor();		
	}
	
	/** set new Radius and reset cursor**/
	public void setRadius(final int nRadius_)
	{
		if(nRadius_!=nRadius)
		{
			nRadius = nRadius_;
			circle2D.updateRadius(nRadius);
			cursorCircle =circle2D.localizingCursor();
		}
		else
		{
			cursorCircle.reset();
		}
	}
}

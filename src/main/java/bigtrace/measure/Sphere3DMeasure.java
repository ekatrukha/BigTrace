package bigtrace.measure;

import net.imglib2.Point;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

public class Sphere3DMeasure {
	
	public HyperSphere< IntType > sphere3D;
	public HyperSphereCursor< IntType > cursorSphere;
	private int nRadius = 0;
	
	public Sphere3DMeasure()
	{
		//fake small RAI
		ArrayImg<IntType, IntArray> costInt = ArrayImgs.ints(new long[] {1,1,1});
		//center at zero
		Point center = new Point(3);
		center.setPosition( 0 , 0 );
		center.setPosition( 0 , 1 );
		center.setPosition( 0 , 2 );
		sphere3D =	new HyperSphere<>( Views.extendZero(costInt), center, 0);
		cursorSphere = sphere3D.localizingCursor();		
	}
	
	/** set new Radius and reset cursor**/
	public void setRadius(final int nRadius_)
	{
		if(nRadius_!=nRadius)
		{
			nRadius = nRadius_;
			sphere3D.updateRadius(nRadius);
			cursorSphere =sphere3D.localizingCursor();
		}
		else
		{
			cursorSphere.reset();
		}
	}
}

package tests;

import ij.ImageJ;
import net.imglib2.Point;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

public class HyperSphereTest {
	public static void main( final String[] args )
	{
		
		new ImageJ();
		double [] pos = new double [2];
		long [] dim = new long[] {31,31,10};
		Point center = new Point( 2 );
		center.setPosition( 15 , 0 );
		center.setPosition( 15 , 1 );
		ArrayImg<IntType, IntArray> costInt = ArrayImgs.ints(dim);
		int nRadius = 2;
		
		for (nRadius=0;nRadius<10;nRadius++)
		{
			//HyperSphere< IntType > hyperSphere =
//					new HyperSphere<>( Views.extendZero(costInt), center, nRadius);
			HyperSphere< IntType > hyperSphere =
					new HyperSphere<>( Views.hyperSlice(costInt,2,nRadius), center, nRadius);			
			HyperSphereCursor< IntType > cursor = hyperSphere.localizingCursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.get().setReal(10000);
				//cursor.localize(pos);
				//System.out.println(Double.toString(pos[0])+" "+Double.toString(pos[1]));
			}
		}		
		
		ImageJFunctions.show(costInt);
		 
	}
}

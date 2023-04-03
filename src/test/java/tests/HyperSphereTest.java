package tests;

import ij.ImageJ;
import net.imglib2.Point;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;

public class HyperSphereTest {
	public static void main( final String[] args )
	{
		
		new ImageJ();
		double [] pos = new double [2];
		long [] dim = new long[] {1,1};
		Point center = new Point( 2 );
		center.setPosition( 0 , 0 );
		center.setPosition( 0 , 1 );
		ArrayImg<IntType, IntArray> costInt = ArrayImgs.ints(dim);
		
		HyperSphere< IntType > hyperSphere =
				new HyperSphere<>( Views.extendZero(costInt), center, 0);
		
		HyperSphereCursor< IntType > cursor = hyperSphere.localizingCursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			//cursor.get().setReal(10000);
			cursor.localize(pos);
			System.out.println(Double.toString(pos[0])+" "+Double.toString(pos[1]));
		}
		hyperSphere.updateRadius(1);
		cursor = hyperSphere.localizingCursor();
		//cursor.reset();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			//cursor.get().setReal(10000);
			cursor.localize(pos);
			System.out.println(Double.toString(pos[0])+" "+Double.toString(pos[1]));
		}
		
		
		//ImageJFunctions.show(costInt);
		 
	}
}

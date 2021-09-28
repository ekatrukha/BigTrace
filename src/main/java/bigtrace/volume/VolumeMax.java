package bigtrace.volume;

import bigtrace.geometry.Cuboid3D;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class VolumeMax {
	/**
	 * Compute the location of the maximal intensity for any IterableInterval,
	 * like an {@link Img}, contained inside Cuboid3D
	 *
	 * The functionality we need is to iterate and retrieve the location. Therefore we need a
	 * Cursor that can localize itself.
	 * Note that we do not use a LocalizingCursor as localization just happens from time to time.
	 *
	 * @param input - the input that has to just be {@link IterableInterval}
	 * @param maxLocation - the location of the maximal value
	 * @param clickCone - Cuboid3D, limiting search 
	 */
	public static < T extends Comparable< T > & Type< T > > boolean findMaxLocationCuboid(
		final IterableInterval< T > input,  final RealPoint maxLocation, final Cuboid3D clickCone )
	{
		// create a cursor for the image (the order does not matter)
		final Cursor< T > cursor = input.cursor();
		
		boolean bFound=false;
		// initialize min and max with the first image value
		T type = cursor.next();
		T max = type.copy();
		double [] pos = new double [3];
		// loop over the rest of the data and determine min and max value
		while ( cursor.hasNext() )
		{
			// we need this type more than once
			type = cursor.next();
 
				if ( type.compareTo( max ) > 0 )
				{
					cursor.localize(pos);
					if(clickCone.isPointInsideShape(pos))
					{
						max.set( type );
						maxLocation.setPosition( cursor );
						bFound=true;
					}
				}
		}
		return bFound;
	}
	
	public static IntervalView< UnsignedByteType > localMax(final IntervalView< UnsignedByteType > input)
		{
			Shape voxShape = new RectangleShape( 2, true);
			long[] dim = Intervals.dimensionsAsLongArray( input );
			ArrayImg<UnsignedByteType, ByteArray> outBytes = ArrayImgs.unsignedBytes(dim);
			IntervalView< UnsignedByteType > output = Views.translate(outBytes, input.minAsLongArray());
			
			final RandomAccessible< Neighborhood< UnsignedByteType > > inputNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(input) );		
			final RandomAccess< Neighborhood< UnsignedByteType > > inRA = inputNeighborhoods.randomAccess();
			
			
			Cursor< UnsignedByteType > inC=input.cursor();
			Cursor< UnsignedByteType > ouC=output.cursor();
			Cursor< UnsignedByteType > neibC;
			int nMaxDet = 0;
			int nMaxNDet = 0;
			int currVal;
			boolean isMax;
			while ( inC.hasNext() )
			{
				inC.fwd();
				ouC.fwd();
				currVal=inC.get().get();
				if(currVal>20)
				{
					inRA.setPosition(inC.positionAsLongArray());
					neibC = inRA.get().cursor();
					isMax= true;
					while(neibC.hasNext())
					{
						neibC.fwd();
						if(neibC.get().get()>currVal)
						{
							isMax = false;
							break;
						}
							
					}
					if(isMax)
					{
						ouC.get().set(100);
						nMaxDet++;
						
					}
					else
					{
						ouC.get().set(0);
						nMaxNDet++;
					}
				}
			}
			System.out.println("max det:"+Integer.toString(nMaxDet));
			System.out.println("max N det:"+Integer.toString(nMaxNDet));
			return output;
		}
}

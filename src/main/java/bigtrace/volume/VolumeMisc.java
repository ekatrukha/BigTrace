package bigtrace.volume;

import java.util.ArrayList;

import bigtrace.geometry.Cuboid3D;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class VolumeMisc {
	
	/** A set of points pointArray calculates their bounding box
	 * and then finds overlap between it and provided IntervalView;
	 * If the overlap is zero or "less dimensional", returns false **/
	public static boolean newBoundBox(final IntervalView< UnsignedByteType > viewclick,final ArrayList<RealPoint> pointArray, final long [][] newMinMax)
	{ 
		//= new long [2][3];
		int ndim = viewclick.numDimensions();
		float [][] newMinMaxF = new float [2][ndim];
		int i, j;
		float temp;
		long [][] bigBox = new long[2][];
		bigBox[0]=viewclick.minAsLongArray();
		bigBox[1]=viewclick.maxAsLongArray();
		
		for (i=0;i<ndim;i++)
		{
			newMinMaxF[0][i]=Float.MAX_VALUE;
			newMinMaxF[1][i]=(-1)*Float.MAX_VALUE;
		}
		for (i=0;i<pointArray.size();i++)
		{
			
			for (j=0;j<ndim;j++)
			{
				temp=pointArray.get(i).getFloatPosition(j);
				if(temp>newMinMaxF[1][j])
					newMinMaxF[1][j]=temp;
				if(temp<newMinMaxF[0][j])
					newMinMaxF[0][j]=temp;
				
			}			
		}
		for (j=0;j<ndim;j++)
		{
				newMinMax[0][j]=Math.max(bigBox[0][j],(long)Math.round(newMinMaxF[0][j]));
				newMinMax[1][j]=Math.min(bigBox[1][j],(long)Math.round(newMinMaxF[1][j]));
				if(newMinMax[1][j]<=newMinMax[0][j])
					return false;
		}
		return true;

	}
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
	
	public static IntervalView<UnsignedByteType> convertFloatToUnsignedByte(IntervalView<FloatType> input, boolean inverse)
	{
		float minVal = Float.MAX_VALUE;
		float maxVal = -Float.MAX_VALUE;
		for ( final FloatType h : input )
		{
			final float dd = h.get();
			minVal = Math.min( dd, minVal );
			maxVal = Math.max( dd, maxVal );
		}

		
		//final RealUnsignedByteConverter<FloatType> cvU = new RealUnsignedByteConverter<FloatType>(minVal,maxVal);
		final RealUnsignedByteConverter<FloatType> cvU;
		if (inverse)
		{
			cvU = new RealUnsignedByteConverter<FloatType>(maxVal,minVal);
		}
		else
		{
			cvU = new RealUnsignedByteConverter<FloatType>(minVal, maxVal);
		}
		final ConvertedRandomAccessibleInterval< FloatType, UnsignedByteType > inputScaled = new ConvertedRandomAccessibleInterval<>( input, ( s, t ) -> {
			cvU.convert(s,t);
		}, new UnsignedByteType() );	
		return Views.interval(inputScaled,inputScaled.minAsLongArray(),inputScaled.maxAsLongArray());
		
	}
	/** function finds local "weak" maxima in the interval and returns its list as long[] **/	
	public static ArrayList<long []> localMaxPointList(final IntervalView< UnsignedByteType > input, final int maxVal)
	{
		Shape voxShape = new RectangleShape( 1, true);
		
		ArrayList<long []> finList= new ArrayList<long []> (); 
		final RandomAccessible< Neighborhood< UnsignedByteType > > inputNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(input) );		
		final RandomAccess< Neighborhood< UnsignedByteType > > inRA = inputNeighborhoods.randomAccess();
		
		
		Cursor< UnsignedByteType > inC=input.cursor();
		Cursor< UnsignedByteType > neibC;
		int nMaxDet = 0;
		int nMaxNDet = 0;
		int currVal;
		boolean isMax;
		while ( inC.hasNext() )
		{
			inC.fwd();
			currVal=inC.get().get();
			if(currVal>maxVal)
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
					nMaxDet++;
					long [] position = new long[3];
					inC.localize(position);
					finList.add(position);					
				}
				else
				{
					nMaxNDet++;
				}
			}
		}
		System.out.println("max det:"+Integer.toString(nMaxDet));
		System.out.println("max not det:"+Integer.toString(nMaxNDet));
		return finList;
	}
	
	/** function returns a pair of closest points between two provided 
	 * array lists **/
	public static ArrayList<long []> findClosestPoints(final ArrayList<long []> begCorners, final ArrayList<long []> endCorners)
	{
		ArrayList<long []> pair = new ArrayList<long []> ();
		
		long minDist = Long.MAX_VALUE;
		long currDist,dL;
		int indB=0;
		int indE=0;
		long [] pB, pE;
		int i,j,k;
		for (i=0;i<begCorners.size();i++)
		{
			pB=begCorners.get(i);
			for (j=0;j<endCorners.size();j++)
			{
				pE=endCorners.get(j);
				currDist=0;
				for(k=0;k<pB.length;k++)
				{
					dL=pE[k]-pB[k];
					currDist+=dL*dL;
				}
				if(currDist<minDist)
				{
					minDist=currDist;
					indB=i;
					indE=j;
				}
			}
		}
		pair.add(begCorners.get(indB));
		pair.add(endCorners.get(indE));
		return pair;
	}
	/** function calculates overlap between newMinMax bounding box
	 * and provided IntervalView and returns it in the newMinMax.
	 * Not sure if it handles zero overlap well, need to check. **/
	public static boolean checkBoxInside(final IntervalView< UnsignedByteType > viewclick, final long [][] newMinMax)
	{ 
		long [][] bigBox = new long[2][];
		bigBox[0]=viewclick.minAsLongArray();
		bigBox[1]=viewclick.maxAsLongArray();
		for (int j=0;j<3;j++)
		{
				newMinMax[0][j]=Math.max(bigBox[0][j],newMinMax[0][j]);
				newMinMax[1][j]=Math.min(bigBox[1][j],newMinMax[1][j]);
				if(newMinMax[1][j]<newMinMax[0][j])
					return false;
		}
		return true;
	}
	
	/**  function calculates transform allowing to align two vectors 
	 * @param align_direction - immobile vector
	 * @param moving - vector that aligned with align_direction
	 * @return affine transform (rotations)
	 * **/
	AffineTransform3D alignVectors(final RealPoint align_direction, final RealPoint moving)
	{
		double [] dstat = align_direction.positionAsDoubleArray();
		double [] dmov = moving.positionAsDoubleArray();
		double [] v = new double [3];
		double c;
		
		AffineTransform3D transform = new AffineTransform3D();
		LinAlgHelpers.normalize(dstat);
		LinAlgHelpers.normalize(dmov);
		c = LinAlgHelpers.dot(dstat, dmov);
		//exact opposite directions
		if ((c+1.0)<0.00001)
		{
			transform.identity();
			transform.scale(-1.0);			
		}
		
		LinAlgHelpers.cross( dstat,dmov, v);
		double [][] matrixV = new double [3][3];
		double [][] matrixV2 = new double [3][3];
		
		matrixV[0][1]=(-1.0)*v[2];
		matrixV[0][2]=v[1];
		matrixV[1][0]=v[2];
		matrixV[1][2]=(-1.0)*v[0];
		matrixV[2][0]=(-1.0)*v[1];
		matrixV[2][1]=v[0];
		
		LinAlgHelpers.mult(matrixV, matrixV, matrixV2);
		c=1.0/(1.0+c);
		LinAlgHelpers.scale(matrixV2, c, matrixV2);
		LinAlgHelpers.add(matrixV, matrixV2, matrixV);
		transform.set(1.0 + matrixV[0][0],       matrixV[0][1],       matrixV[0][2],
					        matrixV[1][0], 1.0 + matrixV[1][1],       matrixV[1][2], 
					        matrixV[2][0],       matrixV[2][1], 1.0 + matrixV[2][2],
					                  0.0,                 0.0,                 0.0);
		
		return transform;
	}
	
	FinalInterval RealIntervaltoInterval (RealInterval R_Int)	
	{
		int i;
		long [] minL = new long [3];
		long [] maxL = new long [3];
		double [] minR = new double [3];
		double [] maxR = new double [3];
		R_Int.realMax(maxR);
		R_Int.realMin(minR);
		for (i=0;i<3;i++)
		{
			minL[i]=(int)Math.round(minR[i]);
			maxL[i]=(int)Math.round(maxR[i]);			
		}
		return Intervals.createMinMax(minL[0],minL[1],minL[2], maxL[0],maxL[1],maxL[2]);
	}

}

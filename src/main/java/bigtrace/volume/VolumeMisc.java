package bigtrace.volume;

import java.util.ArrayList;

import bigtrace.BigTraceData;
import bigtrace.geometry.Cuboid3D;
import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.AbstractInterval;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.algorithm.region.BresenhamLine;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.converter.RealUnsignedShortConverter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class VolumeMisc {
	
	/** A set of points pointArray calculates their bounding box
	 * and then finds overlap between it and provided IntervalView;
	 * If the overlap is zero or "less dimensional", returns false **/
	public static boolean newBoundBox(final Interval viewclick,final ArrayList<RealPoint> pointArray, final long [][] newMinMax)
	{ 
		//= new long [2][3];
		int ndim = viewclick.numDimensions();
		float [][] newMinMaxF = new float [2][ndim];
		int i, j;
		float temp;
		long [][] bigBox = new long[2][];
		bigBox[0] = viewclick.minAsLongArray();
		bigBox[1] = viewclick.maxAsLongArray();
		
		for (i=0;i<ndim;i++)
		{
			newMinMaxF[0][i] = Float.MAX_VALUE;
			newMinMaxF[1][i] = (-1)*Float.MAX_VALUE;
		}
		for (i=0;i<pointArray.size();i++)
		{
			for (j=0;j<ndim;j++)
			{
				temp = pointArray.get(i).getFloatPosition(j);
				if(temp > newMinMaxF[1][j])
					newMinMaxF[1][j] = temp;
				if(temp < newMinMaxF[0][j])
					newMinMaxF[0][j] = temp;
				
			}			
		}
		for (j=0;j<ndim;j++)
		{
				newMinMax[0][j] = Math.max(bigBox[0][j],Math.round(newMinMaxF[0][j]));
				newMinMax[1][j] = Math.min(bigBox[1][j],Math.round(newMinMaxF[1][j]));
				if(newMinMax[1][j] <= newMinMax[0][j])
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
		final Cursor< T > cursor = input.localizingCursor();
		
		boolean bFound = false;
		// initialize max with the first image value
		T type = cursor.next();
		T max = type.copy();
		double [] pos = new double [3];
		cursor.localize( pos );
		if(clickCone.isPointInsideShape(pos))
		{
			maxLocation.setPosition( cursor );
		}
		
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
						bFound = true;
					}
				}
		}
		
		//System.out.println("al1 val:"+max.toString());
		//System.out.println("al1:"+Float.toString(maxLocation.getFloatPosition(0))+" "+Float.toString(maxLocation.getFloatPosition(1))+" "+Float.toString(maxLocation.getFloatPosition(2)));
		
		return bFound;
	}
	
	/** maximum intensity finding function, but without cuboid **/
	public static < T extends Comparable< T > & Type< T > > void findMaxLocation(
			final IterableInterval< T > input,  final RealPoint maxLocation )
	{
		// create a cursor for the image (the order does not matter)
		final Cursor< T > cursor = input.localizingCursor();


		// initialize min and max with the first image value
		T type = cursor.next();
		T max = type.copy();
		maxLocation.setPosition( cursor );
		// loop over the rest of the data and determine min and max value
		while ( cursor.hasNext() )
		{
			// we need this type more than once
			type = cursor.next();

			if ( type.compareTo( max ) > 0 )
			{
				max.set( type );
				maxLocation.setPosition( cursor );

			}
		}
		return ;
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
		//int nMaxDet = 0;
		//int nMaxNDet = 0;
		int currVal;
		boolean isMax;
		while ( inC.hasNext() )
		{
			inC.fwd();
			ouC.fwd();
			currVal=inC.get().get();
			if(currVal > 20)
			{
				inRA.setPosition(inC.positionAsLongArray());
				neibC = inRA.get().cursor();
				isMax = true;
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
					//nMaxDet++;

				}
				else
				{
					ouC.get().set(0);
					//nMaxNDet++;
				}
			}
		}
		//System.out.println("max det:"+Integer.toString(nMaxDet));
		//System.out.println("max N det:"+Integer.toString(nMaxNDet));
		return output;
	}
	
	public static IntervalView<UnsignedByteType> convertFloatToUnsignedByte(RandomAccessibleInterval<FloatType> input, boolean inverse)
	{
		
		float minVal = Float.MAX_VALUE;
		float maxVal = -Float.MAX_VALUE;
		IntervalView<FloatType> inInt = Views.interval(input,input.minAsLongArray(),input.maxAsLongArray());
		for ( final FloatType h : inInt )
		{
			final float dd = h.get();
			minVal = Math.min( dd, minVal );
			maxVal = Math.max( dd, maxVal );
		}


		//final RealUnsignedByteConverter<FloatType> cvU = new RealUnsignedByteConverter<FloatType>(minVal,maxVal);
		final RealUnsignedByteConverter<FloatType> cvU;
		if (inverse)
		{
			cvU = new RealUnsignedByteConverter<>(maxVal,minVal);
		}
		else
		{
			cvU = new RealUnsignedByteConverter<>(minVal, maxVal);
		}
				
		RandomAccessibleInterval<UnsignedByteType> inputScaled = Converters.convert(input, cvU, new UnsignedByteType());
		
		/*
		final ConvertedRandomAccessibleInterval< FloatType, Unsig nedByteType > inputScaled = new ConvertedRandomAccessibleInterval<FloatType, UnsignedByteType>( input, ( s, t ) -> {
			cvU.convert(s, t);
		}, new UnsignedByteType() );	
		*/
		return Views.interval(inputScaled,inputScaled.minAsLongArray(),inputScaled.maxAsLongArray());
		
	}
	
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T extends RealType< T > & NativeType< T > > Img< T > convertFloatToUnsignedShort(Img<FloatType> input)
	{
		double minVal = Float.MAX_VALUE;
		double maxVal = -Float.MAX_VALUE;
		for ( final FloatType h : input )
		{
			final float dd = h.get();
			minVal = Math.min( dd, minVal );
			maxVal = Math.max( dd, maxVal );
		}

		
		//final RealUnsignedByteConverter<FloatType> cvU = new RealUnsignedByteConverter<FloatType>(minVal,maxVal);
		final RealUnsignedShortConverter<FloatType> conv = new RealUnsignedShortConverter<>(minVal,maxVal);

		final ImagePlusImg output = new ImagePlusImgFactory<>( new UnsignedShortType() ).create( input );
		
		
		final Cursor< FloatType > in = input.cursor();
		final Cursor< UnsignedShortType > out = output.cursor();

		while ( in.hasNext() )
		{
			in.fwd();
			out.fwd();

			conv.convert( in.get(), out.get() );
		}
		return output;
		
	}
	/** function finds local "weak" maxima in the interval and returns its list as long[] **/	
	public static < T extends RealType< T > & NativeType< T > > ArrayList<long []> localMaxPointList(final IntervalView< T > input, final int maxVal)
	{
		Shape voxShape = new RectangleShape( 1, true);
		
		ArrayList<long []> finList = new ArrayList<> (); 
		final RandomAccessible< Neighborhood< T > > inputNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(input) );		
		final RandomAccess< Neighborhood< T > > inRA = inputNeighborhoods.randomAccess();
		
		
		Cursor< T > inC = input.cursor();
		Cursor< T > neibC;
		//int nMaxDet = 0;
		//int nMaxNDet = 0;
		double currVal;
		boolean isMax;
		while ( inC.hasNext() )
		{
			inC.fwd();
			currVal = inC.get().getRealDouble();
			if(currVal>maxVal)
			{
				inRA.setPosition(inC.positionAsLongArray());
				neibC = inRA.get().cursor();
				isMax = true;
				while(neibC.hasNext())
				{
					neibC.fwd();
					if(neibC.get().getRealDouble() > currVal)
					{
						isMax = false;
						break;
					}
						
				}
				if(isMax)
				{
					//nMaxDet++;
					long [] position = new long[3];
					inC.localize(position);
					finList.add(position);					
				}
				else
				{
					//nMaxNDet++;
				}
			}
		}
		//System.out.println("max det:"+Integer.toString(nMaxDet));
		//System.out.println("max not det:"+Integer.toString(nMaxNDet));
		return finList;
	}
	
	/** function returns a pair of closest points between two provided 
	 * array lists **/
	public static ArrayList<long []> findClosestPoints(final ArrayList<long []> begCorners, final ArrayList<long []> endCorners)
	{
		ArrayList<long []> pair = new ArrayList<> ();
		
		long minDist = Long.MAX_VALUE;
		long currDist,dL;
		int indB = 0;
		int indE = 0;
		long [] pB, pE;
		int i,j,k;
		for (i=0;i<begCorners.size();i++)
		{
			pB = begCorners.get(i);
			for (j=0;j<endCorners.size();j++)
			{
				pE = endCorners.get(j);
				currDist = 0;
				for(k=0;k<pB.length;k++)
				{
					dL = pE[k]-pB[k];
					currDist += dL*dL;
				}
				if(currDist < minDist)
				{
					minDist = currDist;
					indB = i;
					indE = j;
				}
			}
		}
		pair.add(begCorners.get(indB));
		pair.add(endCorners.get(indE));
		return pair;
	}
	
	/** function calculates overlap between newMinMax bounding box
	 * and provided AbstractInterval and returns it in the newMinMax.
	 * Not sure if it handles zero overlap well, need to check. **/
	//public static boolean checkBoxInside(final IntervalView< UnsignedByteType > viewclick, final long [][] newMinMax)
	public static boolean checkBoxInside(final AbstractInterval viewclick, final long [][] newMinMax)
	{ 
		long [][] bigBox = new long[2][];
		bigBox[0] = viewclick.minAsLongArray();
		bigBox[1] = viewclick.maxAsLongArray();
		for (int j=0;j<3;j++)
		{
				newMinMax[0][j] = Math.max(bigBox[0][j],newMinMax[0][j]);
				newMinMax[1][j] = Math.min(bigBox[1][j],newMinMax[1][j]);
				if(newMinMax[1][j] < newMinMax[0][j])
					return false;
		}
		return true;
	}
	
	public static ArrayList<RealPoint> BresenhamWrap(final RealPoint RP1, final RealPoint RP2)
	{
		ArrayList<RealPoint> linepx = new ArrayList<>();
		ArrayList<long []> br_line;
		final long[] lp1, lp2;
		
		int nDim = RP1.numDimensions();
		int i;
		
		lp1 = new long [nDim];
		lp2 = new long [nDim];
		for (i=0;i<nDim;i++)
		{
			lp1[i] = Math.round(RP1.getFloatPosition(i));
			lp2[i] = Math.round(RP2.getFloatPosition(i));
		}
			
		final Point P1 = new Point(lp1);
		final Point P2 = new Point(lp2);

		br_line = BresenhamLine.generateCoords(P1, P2);
		for(i=0;i<br_line.size();i++)
		{
			RealPoint lineRP = new RealPoint(nDim);
			lineRP.setPosition(br_line.get(i));
			linepx.add(lineRP);
		}

		return linepx;
	}
	
	public static FinalInterval RealIntervaltoInterval (RealInterval R_Int)	
	{
		int i;
		final int d = R_Int.numDimensions();
		long [] minL = new long [d];
		long [] maxL = new long [d];
		double [] minR = new double [d];
		double [] maxR = new double [d];
		R_Int.realMax( maxR );
		R_Int.realMin( minR );
		for (i=0;i<d;i++)
		{
			minL[i]=(int)Math.round( minR[i] );
			maxL[i]=(int)Math.round( maxR[i] );			
		}
		return new FinalInterval( minL, maxL );
	}
	
	/** assume the input image format is in XYZTC **/
	public static <T extends NumericType<T> > ImagePlus wrapImgImagePlusCal(RandomAccessibleInterval< T > img, String sTitle, Calibration cal)
	{
		ImagePlus outIP;// = ImageJFunctions.wrap(img,sTitle);
		
		//just a 3D volume
		if(img.numDimensions() == 3)
		{
			outIP = ImageJFunctions.wrap(img,sTitle);
			outIP.setDimensions(1, (int)img.dimension(2), 1);		
		}
		else
		{
			//multichannel 3D volume or one channel timelapse
			if(img.numDimensions()==4)
			{
				outIP = ImageJFunctions.wrap(Views.permute(img,2,3), sTitle);
				//multichannel
				if(BigTraceData.nNumTimepoints==1)
				{
					outIP.setDimensions((int)img.dimension(3), (int)img.dimension(2), 1);
				}
				//timelapse
				else
				{
					outIP.setDimensions(1, (int)img.dimension(2), (int)img.dimension(3));
				}
			}
			else
			{
				//outIP = ImageJFunctions.wrap(img, sTitle);
				outIP = ImageJFunctions.wrap(Views.permute(Views.permute(img,2,3),2,4), sTitle);
				//outIP.setDimensions((int)img.dimension(4), (int)img.dimension(2),(int)img.dimension(3));
			}
		}
		
		if(cal!=null)
		{
			outIP.setCalibration(cal);
		}
		return outIP;
		//outIP.show();
	}
	
	public static double clamp(double value, double min, double max) {
	    return Math.max(min, Math.min(max, value));
	}
	
	/** gets a box around "target" with half size of range in all axes.
	crops the box so it is inside viewclick interval **/
	public static FinalInterval getTraceBoxCentered(final AbstractInterval viewclick, final long range, final RealPoint target)
	{
		long[][] rangeM = new long[2][3];
		int i;
		float [] pos = new float[3];
		target.localize(pos);
		for(i=0;i<3;i++)
		{
			rangeM[0][i] = (long)(pos[i])-range ;
			rangeM[1][i] = (long)(pos[i])+range;								
		}
		VolumeMisc.checkBoxInside(viewclick, rangeM);
		FinalInterval finInt = new FinalInterval(rangeM[0],rangeM[1]);
		return finInt;							
	}

	//gets a box around "target" with half size of range
	public static FinalInterval getZoomBoxCentered(final long range, final RealPoint target)
	{
		long[][] rangeM = new long[2][3];
		int i;
		float [] pos = new float[3];
		target.localize(pos);
		for(i=0;i<3;i++)
		{
			rangeM[0][i] = (long)(pos[i])-range ;
			rangeM[1][i] = (long)(pos[i])+range;								
		}
		FinalInterval finInt = new FinalInterval(rangeM[0],rangeM[1]);
		return finInt;							
	}
	
}

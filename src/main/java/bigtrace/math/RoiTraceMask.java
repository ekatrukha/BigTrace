package bigtrace.math;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bigtrace.BigTrace;
import bigtrace.rois.Roi3D;

public class RoiTraceMask < T extends RealType< T > & NativeType< T > >
{
    final BigTrace<T> bt;
    IntervalView< T > traceInterval;
    boolean bMaskInit = false;
    //for now unsignedbyte
    IntervalView< UnsignedByteType > traceMask;
    
    public RoiTraceMask(final BigTrace<T> bt_)
	{
		bt = bt_;
    }
    
    public void initTraceMask (final IntervalView< T > traceInterval_)
    {
    	//current data
    	traceInterval = traceInterval_;
    	final long[] dim = Intervals.dimensionsAsLongArray(traceInterval);
    	//make a mask
    	ArrayImg<UnsignedByteType, ByteArray> maskArr = ArrayImgs.unsignedBytes(dim);
    	traceMask = Views.translate(maskArr, traceInterval.minAsLongArray());
    	
    	bMaskInit = true;
    	//fill mask with current ROI shapes
    	final ArrayList<Roi3D> rois = bt.roiManager.rois;
    	
    	for (final Roi3D roi : rois) 
    	{
    		 if (roi.getTimePoint() == bt.btData.nCurrTimepoint)
    		 {
    			 markROI(roi);
    		 }
    	}
    	
    	//ImageJFunctions.show( traceMask);
    }
    
    public boolean isOccupied(final RealPoint point)
    {    	
    	//see if the point inside the interval
     	if(bMaskInit)
    	{
     		if(!Intervals.contains( traceMask, point ))
     			return false;
     		final long [] posLong = new long[3];
     		for(int d=0;d<3;d++)
     		{
     			posLong[d] = Math.round( point.getFloatPosition( d ));
     		}
     		if(traceMask.getAt( posLong ).get() == 0)
     		{
     			return false;
     		}
     		return true;
    	}
		System.err.println("auto-trace mask was not initialized!");
		return false;
    }
    
    /** fast method, does not check boundaries of the mask 
     * or if it is initialized**/
    public boolean isOccupied(final long[] posLong)
    {
 		if(traceMask.getAt( posLong ).get() == 0)
 		{
 			return false;
 		}
 		return true;
    }
    
    public void markLocation(final RealPoint point)
    {
     	//see if the point inside the interval
     	if(bMaskInit)
    	{
     		if(!Intervals.contains( traceMask, point ))
     			return;
     		final long [] posLong = new long[3];
     		for(int d=0;d<3;d++)
     		{
     			posLong[d] = Math.round( point.getFloatPosition( d ));
     		}
     		traceMask.getAt( posLong ).set( 255 );
     		
     		return;
     	
    	}
		System.err.println("auto-trace mask was not initialized!");
		return;
    }
    
    public void markInterval(final Interval inputInt)
    {
     	//see if the point inside the interval
     	if(bMaskInit)
    	{
     		Cursor< UnsignedByteType > markInt = Views.interval( traceMask, Intervals.intersect( inputInt, traceMask ) ).cursor();
     		while(markInt.hasNext())
     		{
     			markInt.next().set( 255 );
     		}
     		return;
    	}
		System.err.println("auto-trace mask was not initialized!");
		return;
    }
    
    /** marks ROI in the current mask.
     * does not check timepoint **/
    public void markROI(final Roi3D roi)
    {
    	if(bMaskInit)
    	{
    		final Cursor< UnsignedByteType > cursorRoi = roi.getSingle3DVolumeCursor( traceMask );
    		
    		if(cursorRoi != null)
    		{
    			cursorRoi.reset();
    			while ( cursorRoi.hasNext() )
    			{
    				cursorRoi.fwd();
    				
    				if(Intervals.contains( traceMask, cursorRoi ))
    				{
    					cursorRoi.get().set( 255 );
    				}
    			}
    		}
    	}
    	else
    	{
    		System.err.println("auto-trace mask was not initialized!");
    	}
    }
    
    public ValuePair<Double, RealPoint> findMaskedMax()
    {
    	final RealPoint maxLocation = new RealPoint(3);
    	double maxVal = (-1)*Double.MAX_VALUE;
    	if(bMaskInit)
    	{
    		// create a cursor for the image (the order does not matter)
    		final Cursor< T > cursor = traceInterval.localizingCursor();
    		final Cursor< UnsignedByteType > cursorMask = traceMask.localizingCursor();

    		//double curVal;
    		cursor.reset();
    		cursorMask.reset();
    		T currVal;
    		while(cursor.hasNext())
    		{
    			currVal = cursor.next();
    			if(cursorMask.next().get()==0)
    			{
    				
    				if(currVal.getRealDouble()>maxVal)
    				{
    					maxVal = currVal.getRealDouble();
    					maxLocation.setPosition( cursor );
    				}
    			}
    			
    		}
  
    	}
    	else
    	{
    		System.err.println("auto-trace mask was not initialized!");
    	}
    	return new ValuePair< >(maxVal, maxLocation);
    }
}

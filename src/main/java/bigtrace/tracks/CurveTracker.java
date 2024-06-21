package bigtrace.tracks;


import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import bigtrace.math.OneClickTrace;
import bigtrace.measure.MeasureValues;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import bigtrace.rois.Roi3DGroup;
import bigtrace.volume.VolumeMisc;

public class CurveTracker < T extends RealType< T > & NativeType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	
	final BigTrace<T> bt;
	
	public int nFirstTP;
	
	public int nLastTP;
	
	public int nBoxExpand;
	
	Roi3D currentRoi;
	
	MeasureValues oldVect;
	MeasureValues newVect;
	
	private String progressState;
	
	@Override
	public String getProgressState()
	{
		return progressState;
	}
	@Override
	public void setProgressState(String state_)
	{
		progressState = state_;
	}
	
	public CurveTracker(BigTrace<T> bt)
	{
		this.bt = bt;
	}
	
	@Override
	protected Void doInBackground() throws Exception 
	{
		RandomAccessibleInterval<T> full_RAI = bt.btData.getAllDataRAI();		
		Interval boxNext;
		int nInitialTimePoint = bt.btData.nCurrTimepoint;
		
		long [][] nInt = new long [2][5];
		RealPoint rpMax = new RealPoint(3);
		
		currentRoi = bt.roiManager.getActiveRoi();
	
		bt.bInputLock = true;
		bt.roiManager.setLockMode(true);
		
		//make a New Group
		final Roi3DGroup newGroupTrack = new Roi3DGroup( currentRoi, String.format("%03d", BigTraceData.nTrackN.getAndIncrement())); 
		bt.roiManager.addGroup( newGroupTrack );		
		bt.roiManager.applyGroupToROI( currentRoi, newGroupTrack  );
		
		//get direction between ends of the current ROI
		oldVect = new MeasureValues();
		newVect = new MeasureValues();
		((AbstractCurve3D)currentRoi).getEndsDirection(oldVect, BigTraceData.globCal);
		
		//int nTP = nInitialTimePoint+1; 
		OneClickTrace<T> calcTask = new OneClickTrace<>();
		calcTask.bNewTrace = true;
		calcTask.bUnlockInTheEnd = false;
		calcTask.bUpdateProgressBar = false;
		calcTask.bt = this.bt;
		
		boolean bTracing = true;
		
		for(int nTP = nInitialTimePoint+1; nTP<BigTraceData.nNumTimepoints && bTracing; nTP++)
		{
			
			bt.viewer.setTimepoint(nTP);		
			boxNext = Intervals.intersect( bt.btData.getDataCurrentSourceFull(),Intervals.expand(currentRoi.getBoundingBox(),nBoxExpand));
			boxNext.min( nInt[0] );
			boxNext.max( nInt[1] );
			//set time point
			nInt[0][3] = nTP;
			nInt[1][3] = nTP;
			IntervalView<T> searchBox = Views.interval( full_RAI, new FinalInterval(nInt[0],nInt[1]) );
			VolumeMisc.findMaxLocation(searchBox,  rpMax );
			//ImageJFunctions.show( searchBox,"Test");
			
			final IntervalView<T> traceIV =  bt.getTraceInterval(bt.btData.bTraceOnlyClipped);			
			calcTask.fullInput = traceIV;
			calcTask.startPoint = rpMax;
			calcTask.runTracing();
			calcTask.releaseMultiThread();

			//get the new box
			//currentRoi = bt.roiManager.rois.get(bt.roiManager.rois.size()-1);
			currentRoi = bt.roiManager.getActiveRoi();
			//found just one vertex, abort
			if(((LineTrace3D)currentRoi).vertices.size()<2)
			{
				bt.roiManager.removeRoi(bt.roiManager.rois.size()-1);
				bTracing = false;
				System.out.println("Very short (one voxel) ROI found, stopping tracing at frame "+ Integer.toString( nTP )+".");
			}
			else
			{
				bt.roiManager.applyGroupToROI( currentRoi, newGroupTrack  );
				((AbstractCurve3D)currentRoi).getEndsDirection(newVect, BigTraceData.globCal);
				//if not looking at the same direction
				if(LinAlgHelpers.dot( oldVect.direction.positionAsDoubleArray(),  newVect.direction.positionAsDoubleArray())<0)
				{
					currentRoi.reversePoints();
					for(int d=0;d<3;d++)
					{
						newVect.direction.setPosition((-1)*newVect.direction.getDoublePosition( d ) , d );
					}
					//System.out.println("swapped");

				}
				for(int d=0;d<3;d++)
				{
					oldVect.direction.setPosition(newVect.direction.getDoublePosition( d ) , d );
				}
			}
			
		}
		
		return null;
	}
	
	//findNextTrace(final int nTP);
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() 
    {
    	//see if we have some errors
    	 try {
             get();
         	} 
    	 catch (ExecutionException e) {
             e.getCause().printStackTrace();
             String msg = String.format("Unexpected problem during tracking: %s", 
                            e.getCause().toString());
             System.out.println(msg);
         } catch (InterruptedException e) {
             // Process e here
         }
    

    	bt.visBox = null;

    	//unlock user interaction
    	bt.bInputLock = false;
    	bt.roiManager.setLockMode(false);
        	// bvv_trace = BvvFunctions.show(btdata.trace_weights, "weights", Bvv.options().addTo(bvv_main));

    }

}

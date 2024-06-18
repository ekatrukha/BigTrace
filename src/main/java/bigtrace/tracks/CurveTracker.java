package bigtrace.tracks;


import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import bigtrace.math.OneClickTrace;
import bigtrace.rois.Roi3D;
import bigtrace.rois.Roi3DGroup;
import bigtrace.volume.VolumeMisc;

public class CurveTracker < T extends RealType< T > & NativeType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	
	final BigTrace<T> bt;
	
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
		Roi3D currentRoi = bt.roiManager.getActiveRoi();
		Interval boxNext;
		int nInitialTimePoint = bt.btData.nCurrTimepoint;
		
		long [][] nInt = new long [2][5];
		RealPoint rpMax = new RealPoint(3);
		bt.roiManager.unselect();
		bt.bInputLock = true;
		bt.roiManager.setLockMode(true);
		
		//make a New Group
		final Roi3DGroup newGroupTrack = new Roi3DGroup(bt.roiManager.groups.get( currentRoi.getGroupInd() ), "test"); 
		bt.roiManager.groups.add(newGroupTrack);
		//int nTP = nInitialTimePoint+1; 
		OneClickTrace<T> calcTask = new OneClickTrace<>();
		
		for(int nTP = nInitialTimePoint+1; nTP<BigTraceData.nNumTimepoints; nTP++)
		{
			bt.viewer.setTimepoint(nTP);
			
			boxNext = Intervals.intersect( bt.btData.getDataCurrentSourceFull(),currentRoi.getBoundingBox());
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
			calcTask.bt = this.bt;
			calcTask.startPoint = rpMax;
			calcTask.bNewTrace = true;
			calcTask.bUnlockInTheEnd = false;
			calcTask.bUpdateProgressBar = false;
			//calcTask.addPropertyChangeListener(this);
			calcTask.runTracing();
		

			//bt.roiManager.unselect();
			//get the new box
			currentRoi = bt.roiManager.rois.get(bt.roiManager.rois.size()-1);
			currentRoi.setGroup( newGroupTrack );
			
		}
		
		return null;
	}
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

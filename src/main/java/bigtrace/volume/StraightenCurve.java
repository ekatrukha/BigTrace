package bigtrace.volume;

import java.util.ArrayList;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.rois.Roi3D;
import bigtrace.rois.PolyLine3D;
import bigtrace.rois.LineTrace3D;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.RealType;

public class StraightenCurve < T extends RealType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	public BigTrace<T> bt;
	float nRadius;
	Roi3D curveROI;
	
	public StraightenCurve(final Roi3D curveROI_, final BigTrace<T> bt_, final float nRadius_)
	{
		super();
		curveROI = curveROI_;
		bt = bt_;
		nRadius= nRadius_;
	}
	
	@Override
	public String getProgressState() {
		
		return progressState;
	}
	
	@Override
	public void setProgressState(String state_) {
		
		progressState=state_;
		
	}
	
	@Override
	protected Void doInBackground() throws Exception {

        bt.bInputLock = true;
        bt.roiManager.setLockMode(true);
        
		boolean nMultCh;
		if(bt.btdata.nTotalChannels==1)
		{
			nMultCh=false;
		}
		else
		{
			nMultCh=true;
		}

		try {
			  Thread.sleep(1);
		  } catch (InterruptedException ignore) {}
		setProgress(1);
		setProgressState("allocating volume..");
		
		ArrayList<RealPoint> points;
		if(curveROI.getType()==Roi3D.POLYLINE)
		{
			points = ((PolyLine3D)curveROI).makeJointSegment(BigTraceData.shapeInterpolation,bt.btdata.globCal);
		}
		else
		{
			points = ((LineTrace3D)curveROI).makeJointSegment(BigTraceData.shapeInterpolation);
		}
		
		ArrayList<double []> tangents = Pipe3D.getTangentsAverage(points);
		
		//double nLength = curveROI.getLength();
		
		return null;
	}
	
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() 
    {
		//unlock user interaction
    	bt.bInputLock = false;
        bt.roiManager.setLockMode(false);

    }
}

package bigtrace.math;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingWorker;

import net.imglib2.RealPoint;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.rois.LineTrace3D;

public class FullAutoTrace < T extends RealType< T > & NativeType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	final BigTrace<T> bt;
	
	private String progressState;
	
	public int nFirstTP;
	
	public int nLastTP;
	
	public double dAutoMinStartTraceInt = 128.;
	
	public int nAutoMinPointsCurve = 0;
	
	final OneClickTrace<T> oneClickTrace = new OneClickTrace<>();
	
	final RoiTraceMask<T> mask;
	
	public JButton butAuto = null;
	
	public ImageIcon tabIconRestore = null;
	
	public FullAutoTrace(BigTrace<T> bt)
	{
		this.bt = bt;
		mask = new RoiTraceMask<>(bt);
	}
	
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
	
	@Override
	protected Void doInBackground() throws Exception 
	{
		
		//full_RAI = bt.btData.getAllDataRAI();	
		
		//int nInitialTimePoint = bt.btData.nCurrTimepoint;	
		
		//int nTotTP = nLastTP - nFirstTP;
		//int nTPCount = 0;
		
		
		oneClickTrace.bNewTrace = true;
		oneClickTrace.bUnlockInTheEnd = false;
		oneClickTrace.bUpdateProgressBar = false;
		oneClickTrace.bt = this.bt;	
		oneClickTrace.bInsertROI = false;
		oneClickTrace.bUseMask = true;
		
		oneClickTrace.bInit = false;
		oneClickTrace.init();
			
		for(int nTP = nFirstTP; nTP <= nLastTP; nTP++)
		{
			bt.viewer.setTimepoint(nTP);
			IntervalView<T> traceIV =  bt.getTraceInterval(bt.btData.bTraceOnlyClipped);	
			mask.initTraceMask( traceIV );
			oneClickTrace.traceMask = mask;
			oneClickTrace.fullInput = traceIV;
			
			boolean bKeepTracing = true;
			//int nCount = 0;
			while(bKeepTracing)
			{
				if(isCancelled())
				{
					bt.visBox = null;
					return null;				
				}

				ValuePair<Double, RealPoint> newMax = mask.findMaskedMax();
				//final ValuePair<Double, RealPoint> newMax = mask.findMaskedMaxAboveThreshold(dAutoMinStartTraceInt);
				//System.out.println(newMax.getA());

				if(newMax.getA()>dAutoMinStartTraceInt)
				{
					oneClickTrace.startPoint = newMax.getB();					
					setProgressState(Double.toString( newMax.getA() ));
					oneClickTrace.runTracing();
					mask.markLocation(newMax.getB() );
					if(!oneClickTrace.bStartLocationOccupied)
					{
						mask.markROI( bt.roiManager.getActiveRoi() );
					}
					if(bt.roiManager.getActiveRoi() instanceof LineTrace3D)
					{
						final LineTrace3D newtrace = (LineTrace3D)bt.roiManager.getActiveRoi();
						if(newtrace.getNumberOfPointsInJointSegment()<nAutoMinPointsCurve)
						{
							bt.roiManager.deleteActiveROI();
						}
					}
				}
				else
				{
					bKeepTracing = false;
				}
			}
		}
		oneClickTrace.releaseMultiThread();
		System.out.println("done");
		//System.out.println(nCount);
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
    	catch (ExecutionException e) 
    	{
    		e.getCause().printStackTrace();
    		String msg = String.format("Unexpected error during auto-tracing: %s", 
    				e.getCause().toString());
    		System.out.println(msg);
    	} 
    	catch (InterruptedException e) 
    	{
    		// Process e here
    	}
    	catch (Exception e)
    	{
    		bt.visBox = null;
        	setProgressState("Auto-tracing interrupted by user.");
        	setProgress(100);	
    	}
    	
    	if(butAuto != null && tabIconRestore != null)
    	{
    		butAuto.setIcon( tabIconRestore );
    		butAuto.setToolTipText( "Full auto tracing" );
    	}
    	bt.visBox = null;
    	oneClickTrace.releaseMultiThread();
    	
    	//unlock user interaction
    	bt.bInputLock = false;
    	bt.setLockMode(false);
    }
    
}

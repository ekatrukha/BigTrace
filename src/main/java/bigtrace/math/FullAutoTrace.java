package bigtrace.math;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.imglib2.RealPoint;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;

public class FullAutoTrace < T extends RealType< T > & NativeType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	final BigTrace<T> bt;
	
	private String progressState;
	
	public int nFirstTP;
	
	public int nLastTP;
	
	public double dMinStartTraceInt = 128.;
	
	final OneClickTrace<T> oneClickTrace = new OneClickTrace<>();
	final RoiTraceMask<T> mask;
	
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
				//System.out.println(newMax.getA());

				if(newMax.getA()>dMinStartTraceInt)
				{
					oneClickTrace.startPoint = newMax.getB();
					mask.markLocation( oneClickTrace.startPoint );
					setProgressState(Double.toString( newMax.getA() ));
					oneClickTrace.runTracing();
					if(!oneClickTrace.bStartLocationOccupied)
					{
						mask.markROI( bt.roiManager.getActiveRoi() );
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
    }
    
}

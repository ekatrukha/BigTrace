package bigtrace.math;

import java.util.ArrayList;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.volume.VolumeMisc;
import net.imglib2.RealPoint;

public class TracingBG extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	
	public BigTrace bt;
	private String progressState;
	public ArrayList<RealPoint> trace;
	public RealPoint target;

	@Override
	public String getProgressState() {
		// TODO Auto-generated method stub
		return progressState;
	}

	@Override
	public void setProgressState(String state_) {
		// TODO Auto-generated method stub
		progressState=state_;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
				
		trace= new ArrayList<RealPoint>();
		

		long start1, end1;
		boolean found_path_end;
		
		setProgress(30);
		setProgressState("finding trace..");	
		try {
			Thread.sleep(1);
		} catch (InterruptedException ignore) {}

		bt.roiManager.setLockMode(true);
		
		start1 = System.currentTimeMillis();
		//init Dijkstra from initial click point
		bt.dijkRBegin = new DijkstraFHRestricted(bt.btdata.trace_weights);
		found_path_end = bt.dijkRBegin.calcCostTwoPoints(bt.roiManager.getLastTracePoint(),target);
		end1 = System.currentTimeMillis();
		System.out.println("fDijkstra Restr search BEGIN: elapsed Time in milli seconds: "+ (end1-start1));
	
		
		
		//showCorners(dijkRBegin.exploredCorners(jump_points));
		//both points in the connected area
		if (found_path_end)
		{
			bt.dijkRBegin.getTrace(target, trace);
			setProgress(100);
			setProgressState("finding trace done.");
			return null;
		}
		//need to find shortcut through jumping points
		else
		{
			//showCorners(jump_points);
			// get corners in the beginning
			ArrayList<long []> begCorners = bt.dijkRBegin.exploredCorners(bt.btdata.jump_points);
			setProgress(50);
			start1 = System.currentTimeMillis();
			bt.dijkREnd = new DijkstraFHRestricted(bt.btdata.trace_weights);
			bt.dijkREnd.calcCost(target);
			end1 = System.currentTimeMillis();
			System.out.println("Dijkstra Restr search END: elapsed Time in milli seconds: "+ (end1-start1));
			ArrayList<long []> endCorners = bt.dijkREnd.exploredCorners(bt.btdata.jump_points);
			setProgress(80);
			//there are corners (jump points) in the trace area
			// let's construct the path
			if(begCorners.size()>0 && endCorners.size()>0)
			{
				//find a closest pair of corners
				ArrayList<long []> pair = VolumeMisc.findClosestPoints(begCorners,endCorners);
				RealPoint pB = new RealPoint(3);
				RealPoint pE = new RealPoint(3);
				pB.setPosition(pair.get(0));
				pE.setPosition(pair.get(1));
				ArrayList<RealPoint> traceB = new ArrayList<RealPoint> (); 
				bt.dijkRBegin.getTrace(pB, traceB);
				ArrayList<RealPoint> traceE = new ArrayList<RealPoint> ();
				ArrayList<RealPoint> traceM = new ArrayList<RealPoint> ();
				bt.dijkREnd.getTrace(pE, traceE);
				int i;
				//connect traces
				for(i=0;i<traceB.size();i++)
				{
					trace.add(traceB.get(i));
				}
				//3D bresenham connecting jumping points here
				traceM=VolumeMisc.BresenhamWrap(traceB.get(traceB.size()-1),
												traceE.get(traceE.size()-1));
				for(i=1;i<traceM.size()-1 ;i++)	
				{
					trace.add(traceM.get(i));
				}				
				for(i=traceE.size()-1;i>=0 ;i--)				
				{
					trace.add(traceE.get(i));
				}

			}
			//no corners, just do a straight line
			else
			{
				//3D bresenham here
				trace=VolumeMisc.BresenhamWrap(bt.roiManager.getLastTracePoint(),target);
			}
			//return trace;
			setProgress(100);
			setProgressState("finding trace done.");
			return null;
		}
		
		
		
		//return null;
	}
	
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() 
    {
		if(trace.size()>1)
		{
			bt.roiManager.addSegment(target, trace);
			bt.btdata.nPointsInTraceBox++;
			System.out.print("next trace!");
		}

		//unlock user interaction
    	bt.bInputLock = false;
    	//bt.roiManager.setLockMode(false);
    }

}

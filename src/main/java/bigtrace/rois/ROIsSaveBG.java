package bigtrace.rois;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;

public class ROIsSaveBG extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	public BigTrace bt;
	public String sFilename;
	
	@Override
	public String getProgressState()
	{
		return progressState;
	}
	@Override
	public void setProgressState(String state_)
	{
		progressState=state_;
	}

	@Override
	protected Void doInBackground() throws Exception {
		int nRoi, nRoiN;		

		bt.bInputLock = true;
    	bt.roiManager.setLockMode(true);
    	
    	//get the group manager to save groups
    	Roi3DGroupManager roiGM = new Roi3DGroupManager(bt.roiManager);
    	
        try {
			final File file = new File(sFilename);
			
			final FileWriter writer = new FileWriter(file);
			setProgressState("saving Groups...");
			roiGM.saveGroups(writer);
			setProgressState("saving ROIs...");
			writer.write("BigTrace_ROIs,version," + bt.btdata.sVersion + "\n");
			nRoiN=bt.roiManager.rois.size();
			writer.write("ROIsNumber,"+Integer.toString(nRoiN)+"\n");
			for(nRoi=0;nRoi<nRoiN;nRoi++)
			{
				  //Sleep for up to one second.
				try {
					Thread.sleep(1);
				} catch (InterruptedException ignore) {}
				setProgress(nRoi*100/nRoiN);
				writer.write("BT_Roi,"+Integer.toString(nRoi+1)+"\n");
				bt.roiManager.rois.get(nRoi).saveRoi(writer);
			}
			writer.write("End of BigTrace ROIs\n");
			writer.close();
			setProgress(100);
			setProgressState("saving ROIs done.");
		} catch (IOException e) {	
			System.err.print(e.getMessage());
			//e.printStackTrace();
		}
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

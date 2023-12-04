package bigtrace.volume;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.SwingWorker;

import com.jidesoft.icons.IconSet.View;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ExtractCrop < T extends RealType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker{
	
	private String progressState;
	public BigTrace<T> bt;
	Calibration cal;
	int nOutput;
	public int nMinTimePoint, nMaxTimePoint;

	public ExtractCrop( final BigTrace<T> bt_, final int nTimePointMin_, final int nTimePointMax_, final int nOutput_)
	{
		super();
		
		bt = bt_;
		nMinTimePoint = nTimePointMin_;
		nMaxTimePoint = nTimePointMax_;
		nOutput = nOutput_;
		cal = new Calibration();
	}
	@Override
	protected Void doInBackground() throws Exception {
        bt.bInputLock = true;
        bt.roiManager.setLockMode(true);

		try {
			  Thread.sleep(1);
		  } catch (InterruptedException ignore) {}
		setProgress(0);
		setProgressState("extracting volume..");
		
		//get the all data RAI
		//XYZTC
		RandomAccessibleInterval<T> full_RAI = bt.btdata.getAllDataRAI();
		
		FinalInterval cropInt = new FinalInterval (full_RAI);
		long[] cropMin = cropInt.minAsLongArray();
		long[] cropMax = cropInt.maxAsLongArray();
		for(int d=0;d<3;d++)
		{
			cropMin[d]=bt.btdata.nDimCurr[0][d];
			cropMax[d]=bt.btdata.nDimCurr[1][d];
		}
		cropMin[3]= nMinTimePoint;
		cropMax[3]= nMaxTimePoint;
		cropInt = new FinalInterval(cropMin,cropMax);
			
		//output calibration
		cal.setUnit(bt.btdata.sVoxelUnit);
		cal.setTimeUnit(bt.btdata.sTimeUnit);
		cal.pixelWidth= BigTraceData.dMinVoxelSize;
		cal.pixelHeight= BigTraceData.dMinVoxelSize;
		cal.pixelDepth= BigTraceData.dMinVoxelSize;
		
		Path p = Paths.get(bt.btdata.sFileNameFullImg);
		String filename = p.getFileName().toString();
		ImagePlus ip = VolumeMisc.wrapImgImagePlusCal(Views.interval(full_RAI, cropInt), filename + "_crop",cal);
		if(nOutput ==0)
		{
			ip.show();
		}
		else
		{
			IJ.saveAsTiff(ip, null);
		}
		
		try {
			  Thread.sleep(1);
		  } catch (InterruptedException ignore) {}
		setProgress(100);
		setProgressState("extracting volume...done.");
		
		return null;
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
    public void done() 
    {
		//unlock user interaction
    	bt.bInputLock = false;
        bt.roiManager.setLockMode(false);

    }
}

package bigtrace.volume;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.rois.Roi3D;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import bigtrace.rois.AbstractCurve3D;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class StraightenCurve < T extends RealType< T > & NativeType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	final public BigTrace<T> bt;
	final float fRadiusIn;
	final int nStraightenAxis;
	/** 0 - single time point, 1 - all time points **/
	final int nTimeRange;
	final ArrayList<AbstractCurve3D> curveROIArr;	
	final int nOutput;
	final String sSaveFolderPath;
	final Calibration cal;
		
	public StraightenCurve(final ArrayList<AbstractCurve3D> curveROIArr_, final BigTrace<T> bt_, final float fRadius_, final int nStraightenAxis_, final int nTimePoint_, final int nOutput_, final String sSaveFolderPath_)
	{
		super();
		curveROIArr = curveROIArr_;
		bt = bt_;
		nTimeRange = nTimePoint_;
		fRadiusIn = fRadius_;
		nStraightenAxis = nStraightenAxis_;
		nOutput = nOutput_;
		sSaveFolderPath = sSaveFolderPath_;
		cal = new Calibration();
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
        

		try {
			  Thread.sleep(1);
		  } catch (InterruptedException ignore) {}
		setProgress(1);
		setProgressState("Starting straightening..");
		
		//get the all data RAI
		//XYZTC
		RandomAccessibleInterval<T> full_RAI = bt.btdata.getAllDataRAI();
		
		//output calibration
		cal.setUnit(bt.btdata.sVoxelUnit);
		cal.setTimeUnit(bt.btdata.sTimeUnit);
		cal.pixelWidth= BigTraceData.dMinVoxelSize;
		cal.pixelHeight= BigTraceData.dMinVoxelSize;
		cal.pixelDepth= BigTraceData.dMinVoxelSize;
		final int nTotROIs = curveROIArr.size();  
		if(nTotROIs == 0)
			return null;
		String sRoiName;
		if(nTotROIs == 1)
		{
			sRoiName = bt.roiManager.getTimeGroupPrefixRoiName(curveROIArr.get(0));
			IntervalView<T> extractedRAI = extractCurveRAI(curveROIArr.get(0),  full_RAI, true);
			outputImagePlus(VolumeMisc.wrapImgImagePlusCal(extractedRAI, sRoiName + "_straight",cal));
		}
		else
		{
			
			for(int nRoi=0; nRoi<nTotROIs; nRoi++)
			{
				sRoiName = bt.roiManager.getTimeGroupPrefixRoiName(curveROIArr.get(nRoi));
				try {
					  Thread.sleep(1);
				  } catch (InterruptedException ignore) {}
				setProgress(100*nRoi/(nTotROIs));
				setProgressState("extracting ROI ("+Integer.toString(nRoi+1)+"/"+Integer.toString(nTotROIs)+") "+ sRoiName);
				IntervalView<T> extractedRAI = extractCurveRAI(curveROIArr.get(nRoi),  full_RAI, false);
				setProgressState("storing ROI ("+Integer.toString(nRoi+1)+"/"+Integer.toString(nTotROIs)+") "+ sRoiName);
				outputImagePlus(VolumeMisc.wrapImgImagePlusCal(extractedRAI, sRoiName + "_straight",cal));
			}
			setProgressState("ROI straightening finished.");
			setProgress(100);
		}
			

		return null;
	}
	
	void outputImagePlus(ImagePlus ip)
	{
		if(nOutput == 0)
		{
			ip.show();
		}
		else
		{
			IJ.saveAsTiff(ip, sSaveFolderPath+ip.getTitle());
		}
	}

	
	IntervalView<T> extractCurveRAI(final AbstractCurve3D curveROI, final RandomAccessibleInterval<T> all_RAI, boolean bUpdateProgressBar)
	{
		//get the curve and tangent vectors
		//curve points in SPACE units
		//sampled with dMin step
		ArrayList<RealPoint> points_space = curveROI.getJointSegmentResampled();
		//get tangent vectors		
		ArrayList<double []> tangents = curveROI.getJointSegmentTangentsResampled();
		
		int nRadius;
		
		//take radius from ROI
		if(fRadiusIn < 0)
		{
			nRadius = (int) Math.round(0.5*curveROI.getLineThickness());			
		}
		else
		{
			nRadius = (int) Math.round(fRadiusIn);
		}
		int dimXY = (int)(nRadius*2+1);
		
		int nTotDim = all_RAI.numDimensions();
		long [] dimS =new long[nTotDim];
		all_RAI.dimensions(dimS);
		dimS[0]=points_space.size(); //length along the line becomes X
		dimS[1]=dimXY;
		dimS[2]=dimXY;
		long nChannelN = 1;
		
		int nMinTimePoint, nMaxTimePoint;
		double [] currXYmCh = new double[nTotDim];
		//single time point
		if(nTimeRange == 0)
		{
			dimS[3] = 1;
			nMinTimePoint = curveROI.getTimePoint();
			nMaxTimePoint = curveROI.getTimePoint();
		}
		else
		{
			nMinTimePoint = 0;
			nMaxTimePoint = BigTraceData.nNumTimepoints-1;
		}
		//channels number
		nChannelN = dimS[4];

		//this is where we store straightened volume
		Img<T> out1 = Util.getSuitableImgFactory(all_RAI, Util.getTypeFromInterval(all_RAI)).create(new FinalInterval(dimS));
		
		
		//get a frame around line
		double [][][] rsVect =  Pipe3D.rotationMinimizingFrame(points_space, tangents);

		
		//plane perpendicular to the line
		ArrayList< RealPoint > planeNorm;

		double [] current_point = new double [3];
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(all_RAI), bt.btdata.nInterpolatorFactory);
		RealRandomAccess<T> ra = interpolate.realRandomAccess();
		RandomAccess<T> ra_out = out1.randomAccess();
	
		RealPoint curr_XY;
		if(bUpdateProgressBar)
		{
			setProgressState("sampling pipe " + curveROI.getName() +"..");
		}
		//go over all points
		for (int nPoint = 0;nPoint<points_space.size();nPoint++)
		{
			
			points_space.get(nPoint).localize(current_point); 
			planeNorm = getNormPlaneGridXY((int)nRadius, BigTraceData.dMinVoxelSize,rsVect[0][nPoint],rsVect[1][nPoint], current_point);
			
			for (int i=0;i<dimXY;i++)
				for (int j=0;j<dimXY;j++)
				{
					//current XY point coordinates
					curr_XY = new RealPoint( planeNorm.get(j+i*dimXY));

					//back to voxel units
					curr_XY = Roi3D.scaleGlobInv(curr_XY, BigTraceData.globCal);
					for(int nTimePoint = nMinTimePoint;nTimePoint<=nMaxTimePoint;nTimePoint++)
					{
						for (int nCh=0;nCh<nChannelN;nCh++)
						{
	
							curr_XY.localize(currXYmCh);
							//time
							currXYmCh[3] = nTimePoint; 
							//channel
							currXYmCh[4] = nCh; 
							ra.setPosition(currXYmCh);
							ra_out.setPosition(new int [] {nPoint,j,i,nTimePoint-nMinTimePoint,nCh});

							ra_out.get().setReal(ra.get().getRealDouble());
						}
					}
				}	
			if(bUpdateProgressBar)
			{
				setProgress(100*nPoint/(points_space.size()-1));
			}
			
		}
		if(bUpdateProgressBar)
		{
			setProgressState("ROI straightening finished.");
			setProgress(100);
		}
		/** in case line is along Y or Z**/
		if(nStraightenAxis>0)
		{
			return Views.permute(out1, 0, nStraightenAxis);
		}
		else
		{
			return Views.interval(out1,out1);
		}
		
	}
	
	/** generates initial square XY plane sampling with data from -nRadius till nRadius values (in dPixSize units) in XY 
	 * centered around (0,0)**/
	public static ArrayList< RealPoint > iniNormPlane(final int nRadius,final double dPixSize)
	{
		 ArrayList< RealPoint > planeXY = new  ArrayList< RealPoint > ();
		 		 
		 for (int i=-nRadius;i<=nRadius;i++)
		 {
			 for (int j=-nRadius;j<=nRadius;j++)
			 {
				 planeXY.add(new RealPoint(i*dPixSize,j*dPixSize,0.0));
			 }
		 }
		 
		 return planeXY;
	}
	/** generates XY plane sampling grid coordinates from -nRadius till nRadius values (in dPixSize units) in XY plane 
	 * defined by two normalized perpendicular vectors X and Y and with center at c **/
	public static ArrayList< RealPoint > getNormPlaneGridXY(final int nRadius,final double dPixSize,final double [] x,final double [] y, final double [] c)
	{
		 ArrayList< RealPoint > planeXY = new  ArrayList< RealPoint > ();
		 double [] xp = new double[3];
		 double [] yp = new double[3];
		 
		 for (int i=-nRadius;i<=nRadius;i++)
		 {
			 for (int j=-nRadius;j<=nRadius;j++)
			 {
				 LinAlgHelpers.scale(x, i*dPixSize, xp);
				 LinAlgHelpers.scale(y, j*dPixSize, yp);
				 LinAlgHelpers.add(xp, yp,xp);
				 LinAlgHelpers.add(xp, c,xp);
				 planeXY.add(new RealPoint(xp));
			 }
		 }
		 
		 return planeXY;
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
    		String msg = String.format("Unexpected problem during straightening: %s", 
    				e.getCause().toString());
    		System.out.println(msg);
    	} catch (InterruptedException e) {
    		// Process e here
    	}
		//unlock user interaction
    	bt.bInputLock = false;
        bt.roiManager.setLockMode(false);

    }
}

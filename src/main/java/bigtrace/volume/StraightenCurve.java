package bigtrace.volume;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import bdv.spimdata.SequenceDescriptionMinimal;
import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.rois.Roi3D;
import ij.measure.Calibration;
import bigtrace.rois.AbstractCurve3D;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class StraightenCurve < T extends RealType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	public BigTrace<T> bt;
	float nRadius;
	AbstractCurve3D curveROI;
	public String sRoiName="";
	
	public StraightenCurve(final AbstractCurve3D curveROI_, final BigTrace<T> bt_, final float nRadius_)
	{
		super();
		curveROI = curveROI_;
		bt = bt_;
		//round it up
		nRadius= (float)Math.ceil(nRadius_);
	}
	
	@Override
	public String getProgressState() {
		
		return progressState;
	}
	
	@Override
	public void setProgressState(String state_) {
		
		progressState=state_;
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Void doInBackground() throws Exception {

        bt.bInputLock = true;
        bt.roiManager.setLockMode(true);
        

		try {
			  Thread.sleep(1);
		  } catch (InterruptedException ignore) {}
		setProgress(1);
		setProgressState("allocating volume..");
		
		//get the data RAI
		if(bt.bBDVsource)
		{
			final SequenceDescriptionMinimal seq = bt.spimData.getSequenceDescription();
			
			List<RandomAccessibleInterval<T>> hyperslices = new ArrayList<RandomAccessibleInterval<T>> ();
			
			
			for (int setupN=0;setupN<seq.getViewSetupsOrdered().size();setupN++)
			{
				hyperslices.add((RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(setupN).getImage(0));
			}
			//for()
			bt.all_ch_RAI = Views.stack(hyperslices);
		}
		
		
		//get the curve and tangent vectors
		ArrayList<RealPoint> points_space;
		//get tangent vectors		
		ArrayList<double []> tangents;
		//curve points in SPACE units
		//sampled with dMin step

		points_space = curveROI.getJointSegmentResampled();
		tangents = curveROI.getJointSegmentTangentsResampled();
	

		int dimXY = (int)(nRadius*2+1);
		
		final int nTotDim = bt.all_ch_RAI.numDimensions();
		long [] dimS =new long[nTotDim];
		bt.all_ch_RAI.dimensions(dimS);
		dimS[0]=points_space.size(); //length along the line becomes Z
		dimS[1]=dimXY;
		dimS[2]=dimXY;
		long nChannelN = 1;
		boolean nMultCh = false;
		double [] currXYmCh = new double[nTotDim];
		if(nTotDim>3)
		{
			nChannelN = dimS[3];
			nMultCh = true;
		}
		
		//this is where we store straightened volume
		Img<T> out1 = Util.getSuitableImgFactory(bt.all_ch_RAI, Util.getTypeFromInterval(bt.all_ch_RAI)).create(new FinalInterval(dimS));
		
		
		//get a frame around line
		double [][][] rsVect =  Pipe3D.rotationMinimizingFrame(points_space, tangents);

		
		//plane perpendicular to the line
		ArrayList< RealPoint > planeNorm;

		double [] current_point = new double [3];
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(bt.all_ch_RAI), bt.roiManager.roiMeasure.nInterpolatorFactory);
		final RealRandomAccess<T> ra = interpolate.realRandomAccess();
		final RandomAccess<T> ra_out = out1.randomAccess();
	
		RealPoint curr_XY;
		setProgressState("sampling pipe..");
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
					curr_XY =Roi3D.scaleGlobInv(curr_XY, BigTraceData.globCal);
					
					for (int nCh=0;nCh<nChannelN;nCh++)
					{
						//position RA at corresponding points
						//multichannel
						if(nMultCh)
						{
							curr_XY.localize(currXYmCh);
							//channel
							currXYmCh[3] = nCh; 
							ra.setPosition(currXYmCh);
							ra_out.setPosition(new int [] {nPoint,j,i,nCh});
						}
						//one channel
						else
						{							
							ra.setPosition(curr_XY);
							ra_out.setPosition(new int [] {nPoint,j,i});
						}
						ra_out.get().setReal(ra.get().getRealDouble());
						
					}
				}	
			setProgress(100*nPoint/(points_space.size()-1));
			
		}
		
		Calibration cal = new Calibration();
		cal.setUnit(bt.btdata.sVoxelUnit);
		cal.pixelWidth= BigTraceData.dMinVoxelSize;
		cal.pixelHeight= BigTraceData.dMinVoxelSize;
		cal.pixelDepth= BigTraceData.dMinVoxelSize;
		//switch Z and X for convenience
		VolumeMisc.wrapImgImagePlusCal(out1, sRoiName+"_straight",cal).show();
		//VolumeMisc.wrapImgImagePlusCal(Views.permute(out1,0,2), "test",cal).show();
		//double nLength = curveROI.getLength();
		setProgressState("straighten ROI done.");
		setProgress(100);
		return null;
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
		//unlock user interaction
    	bt.bInputLock = false;
        bt.roiManager.setLockMode(false);

    }
}

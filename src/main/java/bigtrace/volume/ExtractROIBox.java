package bigtrace.volume;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.measure.Circle2DMeasure;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import bigtrace.scene.VisPolyLineMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.alg.MeshCursor;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class ExtractROIBox < T extends RealType< T > & NativeType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	public BigTrace<T> bt;
	
	int nExpandROIBox;
	/** 0 - single time point, 1 - all time points **/
	int nTimeRange;
	ArrayList<Roi3D> listROIs;	
	int nOutput;
	String sSaveFolderPath;
	Calibration cal;
//	Img<T> extractedRAI = extractCurveRAI
	public ExtractROIBox(final ArrayList<Roi3D> listROIs_, final BigTrace<T> bt_, final int nExpandROIBox_, final int nTimePoint_, final int nOutput_, final String sSaveFolderPath_)
	{
		super();
		listROIs = listROIs_;
		bt = bt_;
		nExpandROIBox = nExpandROIBox_;
		nTimeRange = nTimePoint_;

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
		setProgressState("Extracting box around ROI(s)..");
		
		//get the all data RAI
		//XYZTC
		RandomAccessibleInterval<T> full_RAI = bt.btData.getAllDataRAI();
		
		//output calibration
		cal.setUnit(bt.btData.sVoxelUnit);
		cal.setTimeUnit(bt.btData.sTimeUnit);
		cal.pixelWidth = BigTraceData.globCal[0];
		cal.pixelHeight= BigTraceData.globCal[1];
		cal.pixelDepth = BigTraceData.globCal[2];
		final int nTotROIs = listROIs.size();  
		if(nTotROIs == 0)
			return null;
		String sRoiName;

		for(int nRoi=0; nRoi<nTotROIs; nRoi++)
		{
			sRoiName = bt.roiManager.getTimeGroupPrefixRoiName(listROIs.get(nRoi));
			try {
				Thread.sleep(1);
			} catch (InterruptedException ignore) {}
			setProgress(100*nRoi/(nTotROIs));
			setProgressState("extracting ROI ("+Integer.toString(nRoi+1)+"/"+Integer.toString(nTotROIs)+") "+ sRoiName);
			IntervalView<T> extractedRAI = extractBoxRoiRAI(listROIs.get(nRoi),  full_RAI);
			setProgressState("storing ROI ("+Integer.toString(nRoi+1)+"/"+Integer.toString(nTotROIs)+") "+ sRoiName);
			outputImagePlus(VolumeMisc.wrapImgImagePlusCal(extractedRAI, sRoiName + "_bbox",cal));
		}
		
		setProgressState("ROI bounding box extraction finished.");
		setProgress(100);	

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
	IntervalView<T> extractBoxRoiRAI(final Roi3D roiIn, final RandomAccessibleInterval<T> all_RAI)
	{
		FinalInterval roiBoxInt = (FinalInterval) roiIn.getBoundingBox();
		

		
		//expand box
		FinalInterval finBox3D = Intervals.expand(roiBoxInt, nExpandROIBox);
		long [][] nInt = new long [2][5];
		
		finBox3D.min(nInt[0]);
		finBox3D.max(nInt[1]);
		//channel
		nInt[0][4] = all_RAI.min(4);
		nInt[1][4] = all_RAI.max(4);
		//time
		
		//current timepoint
		if(nTimeRange == 0)
		{
			nInt[0][3] = roiIn.getTimePoint();
			nInt[1][3] = roiIn.getTimePoint();
			
		}
		//all time points
		else
		{
			nInt[0][3] = all_RAI.min(3);
			nInt[1][3] = all_RAI.max(3);
		}

		FinalInterval finBoxInt = new FinalInterval(nInt[0],nInt[1]);
			
		
		//check the time axis
		
		
		IntervalView<T> bboxOut = Views.interval(Views.extendZero(all_RAI),finBoxInt);
		Img<T> out1 =  Util.getSuitableImgFactory(bboxOut, Util.getTypeFromInterval(bboxOut)).create(bboxOut);
		
		IntervalView< T > trans = Views.translate( out1, bboxOut.minAsLongArray() );
		RandomAccess< T > ra = trans.randomAccess();
		if(roiIn.getType() == Roi3D.LINE_TRACE)
		{
			LineTrace3D roiline = (LineTrace3D)roiIn;
			ArrayList< RealPoint > points = roiline.getVerticesVisual();
			ArrayList< double[] > tangents = roiline.getTangentsVisual();
			ArrayList<ArrayList< RealPoint >> point_contours = Pipe3D.getCountours(points, tangents, BigTraceData.sectorN, 0.5*roiline.getLineThickness()*BigTraceData.dMinVoxelSize);
			//return to voxel space	for the render		
			for(int i=0; i<point_contours.size(); i++)
			{
				point_contours.set(i, Roi3D.scaleGlobInv(point_contours.get(i), BigTraceData.globCal));
			}
			BufferMesh meshx = VisPolyLineMesh.initMeshCappedEnds(point_contours, Roi3D.scaleGlobInv(points, BigTraceData.globCal) );
			
			final Mesh m2 = Meshes.removeDuplicateVertices( meshx, 2 );
			//BufferMesh mesh = new BufferMesh( m2.vertices().size(), m2.triangles().size(), true );
			//Meshes.calculateNormals( m2, mesh );
			
			final Cursor< T > c1 = new MeshCursor<>( ra, m2, new double[] { 1., 1., 1. } );
		
			final int [] pos = new int[5];
			RandomAccess< T > bboxra = bboxOut.randomAccess();
			while ( c1.hasNext() )
			{
				c1.fwd();
				c1.localize( pos );
				bboxra.setPosition( pos );
				c1.get().set( bboxra.get());
				//c1.get().setOne();
			}
		}
		
		
		return Views.interval( out1, out1 );
		//return Views.interval(Views.extendZero(all_RAI),finBoxInt);
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
    		String msg = String.format("Unexpected problem during ROI bounding box extraction: %s", 
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

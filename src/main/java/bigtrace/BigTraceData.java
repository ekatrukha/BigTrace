package bigtrace;

import java.util.ArrayList;
import java.util.List;

import bdv.spimdata.SequenceDescriptionMinimal;
import bigtrace.gui.BCsettings;
import bigtrace.gui.RenderSettings;
import ij.Prefs;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/** class that stores settings and main data from BigTrace **/
public class BigTraceData < T extends RealType< T > > {

	/** current plugin version **/
	public static String sVersion = "0.3.2";
	
	/** plugin instanse **/
	BigTrace<T> bt;
	
	/** path and full input filename **/
	public String sFileNameFullImg;
	
	/** if the source is BDV HDF file, it is true, otherwise we take it from ImageJ **/
	public boolean bBDVsource = false;
	
	///////////////////////////// volume/image  and rendering
	
	/** dimensions of the volume/image (without crop) **/
	public long [][] nDimIni = new long [2][3];
	
	/** current dimensions of the volume/image (after crop) **/
	public long [][] nDimCurr = new long [2][3];
	
	/** global voxel size (for now one for all)  **/
	public static double [] globCal = new double [3];
	
	/** minimum voxel size  **/
	public static double  dMinVoxelSize = 0.0;
	
	/** units of voxel **/
	public String sVoxelUnit = "pixel";
	
	/** units of time **/
	public String sTimeUnit = "frame";
	
	/** frame interval  **/
	public double dFrameInterval = 1.0;
	
	/** whether or not display color coded origin of coordinates **/
	public boolean bShowOrigin = true;
	
	/** whether or not display a box around volume/image **/
	public boolean bVolumeBox = true;
	
	/** camera position for BVV **/
	double dCam = Prefs.get("BigTrace.dCam",2000.0);
	
	/** near clip plane position for BVV **/
	double dClipNear = Prefs.get("BigTrace.dClipNear",1000.0);
	
	/** far clip plane position for BVV  **/
	double dClipFar = Prefs.get("BigTrace.dClipFar",1000.0);
	
	public static RenderSettings renderParams = new RenderSettings();	
	
	/** object to store brightness/alpha range of the channel during tracing**/
	public BCsettings bcTraceChannel = new BCsettings();
	
	/** object to store brightness/alpha range of the tracebox **/
	public BCsettings bcTraceBox = new BCsettings();
	
	/** the number of current channel used for analysis/tracing **/
	public int nChAnalysis = 0;
	
	/** total number of channels in the dataset**/
	public int nTotalChannels = 0;
	
	/** currently selected timePoint**/
	public int nCurrTimepoint = 0;
	
	/** total number of TimePoints**/
	static public int nNumTimepoints = 0;
	
	/** whether or not deselect ROI on time point change **/
	public boolean bDeselectROITime = true;
	
	/** dataset rendering method 
	 * 0 maximum intensity, 
	 * 1 volumetric **/
	public int nRenderMethod = (int)Prefs.get("BigTrace.nRenderMethod",0.0);
		
	/////////////////////////////////clicking interface 
	
	/** half size of rectangle around click point (in screen pixels)
	 * used to find maximum intensity voxel **/
	public int nHalfClickSizeWindow = (int)Prefs.get("BigTrace.nHalfClickSizeWindow",5.0);
	
	
	/** whether to crop volume when zooming **/
	public boolean bZoomCrop = Prefs.get("BigTrace.bZoomCrop", false);
	
	/** characteristic size of zoom in area (in pixels of original volume) **/
	public int nZoomBoxSize = 150;
	
	/** fraction of screen occupied by zoom box **/
	public double dZoomBoxScreenFraction = 1.0;
	
	/** animation speed, i.e. duration of transform **/
	public long nAnimationDuration =  (int)Prefs.get("BigTrace.nAnimationDuration",400);
		
	///////////////////////////// TRACING
	
	/** weights of curve probability (saliency) for the trace box**/
	public IntervalView< UnsignedByteType > trace_weights = null;
	
	/** directions of curve at each voxel for the trace box**/
	public IntervalView< FloatType > trace_vectors = null;
	
	/**special points Dijkstra search for the trace box**/
	public ArrayList<long []> jump_points = null;
	
///////////////////////////// TRACING SETTINGS GENERAL
	
	/** characteristic size (SD) of lines (for each dimension)**/
	public double [] sigmaTrace = new double [3];
	
	/** whether to limit tracing to cropped area**/
	public boolean bTraceOnlyCrop = false;
	
	///////////////////////////// TRACING SETTINGS SEMI AUTO
	
	/** whether (1) or not (0) remove visibility of volume data during tracing **/
	public int nTraceBoxView = 1;
	
	/** half size of tracing box (for now in all dimensions) **/
	public long lTraceBoxSize;
	
	/** fraction of screen occupied by trace box **/
	public double dTraceBoxScreenFraction;	
	
	/** After advancing tracebox, this parameter defines 
	 * how much tracebox is going to follow the last direction of trace (with respect to the last added point):
	 * in the range [0..1], 0 = last point in the center of new tracebox, 1 = previous point is at the edge of the new tracebox**/
	public float fTraceBoxAdvanceFraction;
	
	/** weight of orientation vs saliency in the tracing**/
	public double gammaTrace;
	
	/** current number of vertices in the tracebox **/
	public int nPointsInTraceBox = 0;
	
	///////////////////////////// TRACING SETTINGS ONE CLICK
	
	/** current number of vertices in the tracebox **/
	public int nVertexPlacementPointN;
	
	/** directionality constrain for one click **/
	public double dDirectionalityOneClick;
	
	/** ROI shape interpolation types **/
	public static final int SHAPE_Voxel=0, SHAPE_Smooth=1, SHAPE_Spline=2; 
	
	/** current ROI shape interpolation **/
	public static int shapeInterpolation = (int) Prefs.get("BigTrace.ShapeInterpolation",SHAPE_Spline);
	
	/** algorithm to build rotation minimizing frame **/
	public static int rotationMinFrame = (int) Prefs.get("BigTrace.RotationMinFrame",0);
	
	/** size of moving average window to smooth traces (in points) **/
	public static int nSmoothWindow = (int) Prefs.get("BigTrace.nSmoothWindow", 5);

	/** number of segments in the cylinder cross-section (for polyline/trace ROIs),
	 *  3 = prism, 4 = cuboid, etc.
	 *  The more the number, more smooth is surface **/
	public static int sectorN = (int) Prefs.get("BigTrace.nSectorN", 16);
	
	/** approximate distance between contrours along the pipe visualizing a curve
	 *  in minimum voxel size units **/
	public static int wireCountourStep = (int) Prefs.get("BigTrace.wireCountourStep",1);
	
	/** step of gridline displaying cross-section ROI in wired mode (in voxels)**/
	public static int crossSectionGridStep = (int) Prefs.get("BigTrace.crossSectionGridStep", 20);
	
	/** time rendering option **/
	public static int timeRender = (int) Prefs.get("BigTrace.timeRender",0);
	
	/** time rendering fade in frames **/
	public static int timeFade = (int) Prefs.get("BigTrace.timeFade",0);
	
	public AffineTransform3D transformBeforeTracing = new AffineTransform3D(); 
	

	
	public BigTraceData(final BigTrace<T> bt_)
	{
		
		bt = bt_;
		//default scale
		globCal[0]= 1.0;
		globCal[1]= 1.0;
		globCal[2]= 1.0;
		
		//view 
		nZoomBoxSize = (int) Prefs.get("BigTrace.nZoomBoxSize", 150);
		dZoomBoxScreenFraction = Prefs.get("BigTrace.dZoomBoxScreenFraction", 1.0);

		//tracing
		sigmaTrace = new double [3];
		sigmaTrace[0] = Prefs.get("BigTrace.sigmaTraceX", 3.0);
		sigmaTrace[1] = Prefs.get("BigTrace.sigmaTraceY", 3.0);
		sigmaTrace[2] = Prefs.get("BigTrace.sigmaTraceZ", 3.0);
		bTraceOnlyCrop= Prefs.get("BigTrace.bTraceOnlyCrop", false);
		
		lTraceBoxSize =(long) Prefs.get("BigTrace.lTraceBoxSize", 50);				
		fTraceBoxAdvanceFraction = (float) Prefs.get("BigTrace.fTraceBoxAdvanceFraction", 0.9);
		dTraceBoxScreenFraction = Prefs.get("BigTrace.dTraceBoxScreenFraction", 0.5);
		gammaTrace =  Prefs.get("BigTrace.gammaTrace", 0.0);
		
		nVertexPlacementPointN = (int) Prefs.get("BigTrace.nVertexPlacementPointN", 10);
		dDirectionalityOneClick = Prefs.get("BigTrace.dDirectionalityOneClick", 0.6);
	}
	
	/** returns data sources for specific channel and time point,
	 * limits output to the current cropped area **/
	@SuppressWarnings("unchecked")
	public IntervalView< T > getDataSourceCropped(final int nChannel, final int nTimePoint)
	{
		if(bBDVsource)
		{
			return Views.interval((RandomAccessibleInterval<T>) bt.spimData.getSequenceDescription().getImgLoader().getSetupImgLoader(nChannel).getImage(nTimePoint), nDimCurr[0], nDimCurr[1]);
		}
		else
		{
			return Views.interval(Views.hyperSlice(Views.hyperSlice(bt.all_ch_RAI,4,nChannel),3,nTimePoint), nDimCurr[0], nDimCurr[1]);
			
		}
	}
	
	/** returns data sources for specific channel and time point,
	 * does not limit output to the current cropped area **/
	@SuppressWarnings("unchecked")
	public RandomAccessibleInterval<T> getDataSourceFull(final int nChannel, final int nTimePoint)
	{
		if(bBDVsource)
		{
			return (RandomAccessibleInterval<T>) bt.spimData.getSequenceDescription().getImgLoader().getSetupImgLoader(nChannel).getImage(nTimePoint);
		}
		else
		{
			return Views.hyperSlice(Views.hyperSlice(bt.all_ch_RAI,4,nChannel),3,nTimePoint);
			
		}
	}
	
	@SuppressWarnings("unchecked")
	public RandomAccessibleInterval<T> getAllDataRAI()
	{
		//output should be XYZTC
		
		if(bBDVsource)
		{
			final SequenceDescriptionMinimal seq = bt.spimData.getSequenceDescription();
			
			List<RandomAccessibleInterval<T>> raiXYZTC = new ArrayList<RandomAccessibleInterval<T>> ();
			List<RandomAccessibleInterval<T>> raiXYZT;// = new ArrayList<RandomAccessibleInterval<T>> ();
			
			
			for (int setupN=0;setupN<seq.getViewSetupsOrdered().size();setupN++)
			{
				raiXYZT = new ArrayList<RandomAccessibleInterval<T>> ();
				for(int nTimePoint = 0;nTimePoint<BigTraceData.nNumTimepoints;nTimePoint++)
				{
					raiXYZT.add((RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(setupN).getImage(nTimePoint));
				}
				raiXYZTC.add(Views.stack(raiXYZT));
			}
		
			return Views.stack(raiXYZTC);
		}
		else
		{
			return bt.all_ch_RAI;
		}
	}
}

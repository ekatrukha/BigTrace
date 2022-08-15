package bigtrace;

import java.util.ArrayList;

import bigtrace.geometry.ShapeInterpolation;
import ij.Prefs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;

/** clas that stores settings and main data from BigTrace **/
public class BigTraceData {

	
	public String sVersion = "0.0.6";
	
	/////////////// input file
	public String sFileNameImg;
	///////////////////////////// volume/image  and rendering
	
	/** dimensions of the volume/image (without crop) **/
	public long [][] nDimIni = new long [2][3];
	
	/** current dimensions of the volume/image (after crop) **/
	public long [][] nDimCurr = new long [2][3];
	
	/** global voxel size (for now one for all)  **/
	public double [] globCal = new double [3];
	
	/** unit of voxel**/
	public String sVoxelUnit = "pixel";
	
	/** whether or not display color coded origin of coordinates **/
	public boolean bShowOrigin = true;
	
	/** whether or not display a box around volume/image **/
	public boolean bVolumeBox = true;
	
	/** whether or not display a world grid **/
	public boolean bShowWorldGrid = false;
	
	/** camera position for BVV **/
	double dCam = 1100.;
	/** near clip plane position for BVV **/
	double dClipNear = 1000.;
	/** far clip plane position for BVV  **/
	double dClipFar = 1000.;
	
	public double [] bBrightnessRange = new double [2];
	
	/**number of channel used for analysis/tracing **/
	public int nChAnalysis=0;
	
	
	
	public int nTotalChannels=0;
	
	
	/////////////////////////////////clicking interface 
	
	/** half size of rectangle around click point (in screen pixels)
	 * used to find maximim intensity **/
	public int nHalfClickSizeWindow = 5;
	
	/** characteristic size of zoom in area (in pixels of original volume) **/
	public int nZoomBoxSize = 150;
	
	/** fraction of screen occupied by zoom box **/
	public double dZoomBoxScreenFraction = 1.0;
	
	
	///////////////////////////// tracing box
	
	/** weights of curve probability (saliency) for the trace box**/
	public IntervalView< UnsignedByteType > trace_weights = null;
	/** directions of curve at each voxel for the trace box**/
	public IntervalView< FloatType > trace_vectors=null;
	/**special points Dijkstra search for the trace box**/
	public ArrayList<long []> jump_points = null;
	
	
	
	/** characteristic size (SD) of lines (for each dimension)**/
	public double [] sigmaTrace = new double [3];
	
	/** weight of orientation vs saliency in the tracing**/
	public double gammaTrace = 0.0;
	
	/** whether (1) or not (0) remove visibility of volume data during tracing **/
	public int nTraceBoxView = 1;
	
	/** half size of tracing box (for now in all dimensions) **/
	public long lTraceBoxSize = 50;
	
	/** fraction of screen occupied by trace box **/
	public double dTraceBoxScreenFraction = 0.5;
	
	
	/** After advancing tracebox, this parameter defines 
	 * how much tracebox is going to follow the last direction of trace (with respect to the last added point):
	 * in the range [0..1], 0 = last point in the center of new tracebox, 1 = previous point is at the edge of the new tracebox**/
	public float fTraceBoxAdvanceFraction = 0.9f;
	
	/** whether to limit tracing to cropped area**/
	public boolean bTraceOnlyCrop = false;
	
	/** current number of vertices in the tracebox **/
	public int nPointsInTraceBox=0;
	
	/** ROI shape interpolation types **/
	public static final int SHAPE_Voxel=0, SHAPE_Subvoxel=1; 
	/** current ROI shape interpolation **/
	public static int shapeInterpolation = Prefs.getInt("BigTrace.ShapeInterpolation",SHAPE_Subvoxel);
	
	/** size of moving average window to smooth traces (in points) **/
	public static int nSmoothWindow=(int) Prefs.get("BigTrace.nSmoothWindow", 5);

	/** number of segments in the cylinder cross-section (for polyline/trace ROIs),
	 *  3 = prism, 4 = cuboid, etc.
	 *  The more the number, more smooth is surface **/
	public static int sectorN = (int) Prefs.get("BigTrace.nSectorN", 16);
	
	/** step of gridline displaying cross-section ROI in wired mode (in voxels)**/
	public static int crossSectionGridStep = (int) Prefs.get("BigTrace.crossSectionGridStep", 20);
	
	public AffineTransform3D transformBeforeTracing = new AffineTransform3D(); 
	
	public BigTraceData()
	{
		//default scale
		globCal[0]= 1.0;
		globCal[1]= 1.0;
		globCal[2]= 1.0;
		
		//view 
		nZoomBoxSize = (int) Prefs.get("BigTrace.nZoomBoxSize", 150);
		dZoomBoxScreenFraction = Prefs.get("BigTrace.dZoomBoxScreenFraction", 1.0);

		//tracing
		sigmaTrace = new double [3];
		sigmaTrace[0] = Prefs.get("BigTrace.sigmaTraceX", 2.0);
		sigmaTrace[1] = Prefs.get("BigTrace.sigmaTraceY", 2.0);
		sigmaTrace[2] = Prefs.get("BigTrace.sigmaTraceZ", 2.0);
		gammaTrace =  Prefs.get("BigTrace.gammaTrace", 0.0);
		lTraceBoxSize =(long) Prefs.get("BigTrace.lTraceBoxSize", 50);				
		fTraceBoxAdvanceFraction = (float) Prefs.get("BigTrace.fTraceBoxAdvanceFraction", 0.9);
		dTraceBoxScreenFraction = Prefs.get("BigTrace.dTraceBoxScreenFraction", 0.5);
		bTraceOnlyCrop= Prefs.get("BigTrace.bTraceOnlyCrop", false);
	}
}

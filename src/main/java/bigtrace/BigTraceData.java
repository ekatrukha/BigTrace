package bigtrace;

import java.util.ArrayList;

import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;

/** clas that stores settings and main data from BigTrace **/
public class BigTraceData {

	
	///////////////////////////// volume/image  and rendering
	
	/** dimensions of the volume/image (without crop) **/
	public long [][] nDimIni = new long [2][3];
	
	/** current dimensions of the volume/image (after crop) **/
	public long [][] nDimCurr = new long [2][3];
	
	/** whether or not display color coded origin of coordinates **/
	public boolean bShowOrigin = true;
	
	/** whether or not display a box around volume/image **/
	public boolean bVolumeBox = true;
	
	/** whether or not display a world grid **/
	public boolean bShowWorldGrid = false;
	
	///////////////////////////// tracing box
	
	/** weights of curve probability (saliency) for the trace box**/
	public IntervalView< UnsignedByteType > trace_weights = null;
	/** directions of curve at each voxel for the trace box**/
	public IntervalView< FloatType > trace_vectors=null;
	/**special points Dijkstra search for the trace box**/
	public ArrayList<long []> jump_points = null;
	
	/** characteristic size (SD) of lines (for now in all dimensions)**/
	public double sigmaGlob = 3.0;
	
	/** whether (1) or not (0) remove visibility of volume data during tracing **/
	public int nTraceBoxView = 1;
	
	/** half size of tracing box (for now in all dimensions) **/
	long lTraceBoxSize = 50;
	
	/** How much the tracebox will follow the last direction of trace:
	 * in the range [0..1], 0 = no following (center), 1 = previous point is at the edge of the box**/
	float fTraceBoxShift = 0.9f;
}

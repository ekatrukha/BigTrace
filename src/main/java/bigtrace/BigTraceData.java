package bigtrace;

import java.util.ArrayList;

import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;

/** clas that stores settings and main data from BigTrace **/
public class BigTraceData {

	

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
	
	IntervalView< UnsignedByteType > trace_weights = null;
	IntervalView< FloatType > trace_vectors=null;
	ArrayList<long []> jump_points = null;
	
}

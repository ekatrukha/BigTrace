package bigtrace.geometry;

import java.util.ArrayList;

import bigtrace.rois.Roi3D;
import net.imglib2.RealPoint;
/** linear interpolation wrapper for each 3D coordinates of points set **/
public class LerpCurve3D {

	LinearInterpolation [] interpXYZ = new LinearInterpolation[3];

	double [] xnodes = null;
	public LerpCurve3D(ArrayList<RealPoint> points)
	{
		init(points);
	}

	public void init(ArrayList<RealPoint> points)
	{
		double [][] coords = new double[3][points.size()];
		double [] curr_point = new double[3]; 
		xnodes = Roi3D.getSegmentTabLength(points);
		int i,d;
		for (i =0;i<points.size();i++)
		{
			points.get(i).localize(curr_point);
			for (d=0;d<3;d++)
			{
				coords[d][i]=curr_point[d];
			}
		}
		for (d=0;d<3;d++)
		{
			interpXYZ[d] = new LinearInterpolation(xnodes, coords[d]);
		}
		
		
	}

	public ArrayList<RealPoint> interpolate(double [] xp)
	{
		
		int nP = xp.length;
		double [] curr_point = new double[3]; 
		ArrayList<RealPoint> out = new ArrayList<>();
		//just in case
		if (xnodes == null)
			return null;
		for (int i=0;i<nP;i++)
		{
			for (int d=0;d<3;d++)
			{
				curr_point[d]=interpXYZ[d].evalLinearInterp(xp[i]);				
			}
			out.add(new RealPoint(curr_point));
			
		}
		return out;
	}
	/** interpolates slopes at xp positions **/
	public ArrayList<double []> interpolateSlopes(double [] xp)
	{
		
		int nP = xp.length;
		double [] curr_point = new double[3]; 
		ArrayList<double []> out = new ArrayList<>();
		for (int i=0;i<nP;i++)
		{
			for (int d=0;d<3;d++)
			{
				curr_point[d]=interpXYZ[d].evalSlopeLinearInterp(xp[i]);				
			}
			out.add(curr_point.clone());
			
		}
		return out;
	}
	//Lenght of the polyline
	public double getMaxLength()
	{
		if(xnodes == null)
		{
			return Double.NaN;
			
		}
		return xnodes[xnodes.length-1];
		
	}
	
}

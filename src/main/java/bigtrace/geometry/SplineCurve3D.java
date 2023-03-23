package bigtrace.geometry;

import java.util.ArrayList;

import bigtrace.rois.Roi3D;
import net.imglib2.RealPoint;
/** cubic spline interpolation wrapper for each 3D coordinates of points set **/
public class SplineCurve3D {
	/** interpolation components for each coordinate**/
	CubicSpline [] interpXYZ = new CubicSpline[3];
	/** nodes (usually parametrized as length)**/
	double [] xnodes=null;
	/** do natural cubic spline interpolation **/
	public SplineCurve3D(ArrayList<RealPoint> points)
	{
		init(points,0);
	}
	/** do cubic spline interpolation with end derivatives estimated 
	 * from nDeriveEst points from each end **/
	public SplineCurve3D(ArrayList<RealPoint> points, final int nDeriveEst)
	{
		init(points, nDeriveEst);
	}
	
	public void init(ArrayList<RealPoint> points, final int nDeriveEst)
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
			if(nDeriveEst==0)
			{
				interpXYZ[d] = new CubicSpline(xnodes, coords[d]);
			}
			else
			{
				interpXYZ[d] = new CubicSpline(xnodes, coords[d], nDeriveEst);
			}
		}
				
	}
	

	/** interpolates vectors at xp positions **/
	public ArrayList<RealPoint> interpolate(double [] xp)
	{
		
		int nP = xp.length;
		double [] curr_point = new double[3]; 
		ArrayList<RealPoint> out = new ArrayList<RealPoint>();
		//just in case
		if (xnodes == null)
			return null;
		for (int i=0;i<nP;i++)
		{
			for (int d=0;d<3;d++)
			{
				curr_point[d]=interpXYZ[d].evalSpline(xp[i]);				
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
		ArrayList<double []> out = new ArrayList<double []>();
		for (int i=0;i<nP;i++)
		{
			for (int d=0;d<3;d++)
			{
				curr_point[d]=interpXYZ[d].evalSlope(xp[i]);				
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
		else
		{
			return xnodes[xnodes.length-1];
		}
		
	}
}

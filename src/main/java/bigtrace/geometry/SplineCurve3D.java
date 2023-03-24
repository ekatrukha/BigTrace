package bigtrace.geometry;

import java.util.ArrayList;

import bigtrace.rois.Roi3D;
import net.imglib2.RealPoint;
/** cubic spline interpolation wrapper for each 3D coordinates of points set **/
public class SplineCurve3D {
	
	
	/** interpolation components for each coordinate**/
	CubicSpline [] interpXYZ = new CubicSpline[3];
	/** nodes (arbitrary parametrization, usually as a length of polyline)**/
	double [] xnodes=null;
	
	/** arclength of spline through the nodes **/
	double [] arclength=null;
	
	/** reparametrization of arclength spline**/
	CubicSpline arcToNodes = null;
	
	
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
		//calculate arclength
		arclength=getTabulatedArcLength();
		arcToNodes = new CubicSpline(arclength, xnodes,2);
				
	}
	

	/** interpolates vectors at xp positions along the arclength of the curve **/
	public ArrayList<RealPoint> interpolate(double [] xp)
	{
		
		int nP = xp.length;
		
		double [] curr_point = new double[3]; 
		double currNodePos;
		ArrayList<RealPoint> out = new ArrayList<RealPoint>();
		//just in case
		if (xnodes == null)
			return null;
		for (int i=0;i<nP;i++)
		{
			for (int d=0;d<3;d++)
			{
				currNodePos=arcToNodes.evalSpline(xp[i]);
				curr_point[d]=interpXYZ[d].evalSpline(currNodePos);				
			}
			out.add(new RealPoint(curr_point));
			
		}
		return out;
	}
	
	/** interpolates slopes at xp positions along the arclength of the curve **/
	public ArrayList<double []> interpolateSlopes(double [] xp)
	{
		
		int nP = xp.length;
		double [] curr_point = new double[3];
		double currNodePos;
		ArrayList<double []> out = new ArrayList<double []>();
		for (int i=0;i<nP;i++)
		{
			for (int d=0;d<3;d++)
			{
				currNodePos=arcToNodes.evalSpline(xp[i]);
				curr_point[d]=interpXYZ[d].evalSlope(currNodePos);				
			}
			out.add(curr_point.clone());
			
		}
		return out;
	}
	//arc lenght of spline
	public double getMaxArcLength()
	{
		if(arclength == null)
		{
			return Double.NaN;
		}
		else
		{
			return arclength[xnodes.length-1];
		}
		
	}
	
	/** arc lenght of spline calculated using Gaussian quadrature at two points**/
	public double [] getTabulatedArcLength()
	{
		//just is case 
		if(xnodes == null)
		{
			return null;
		}
		final int nNodesN = xnodes.length;
		double [] out = new double [nNodesN];
		double diff,aver,curr;
		final double evalGauss = 1.0/Math.sqrt(3);// 1/sqrt(3)
		out[0]=0.0;
		//integrate spline length
		for (int i=1;i<nNodesN; i++)
		{
			curr=0.0;
			diff= 0.5*(xnodes[i]-xnodes[i-1]);
			aver = 0.5*(xnodes[i]+xnodes[i-1]);

			curr+=getIntegrFunction(diff*evalGauss+aver);
			curr+=getIntegrFunction(aver-diff*evalGauss);
			curr*=diff;
			out[i]=out[i-1]+curr;
		}
		
		return out;
		
	}
	/** helper function for the calculation of arclength **/
	private double getIntegrFunction (double x)
	{
		double out = 0.0;
		double v;
		for (int d=0;d<3; d++)
		{
			v= interpXYZ[d].evalSlope(x);
			out+=v*v;
		}
		out = Math.sqrt(out);
		return out;
		
	}
}

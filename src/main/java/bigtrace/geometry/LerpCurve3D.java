package bigtrace.geometry;

import java.util.ArrayList;

import bigtrace.rois.Roi3D;
import net.imglib2.RealPoint;

public class LerpCurve3D {

	LinearInterpolation [] interpXYZ = new LinearInterpolation[3];
	public boolean bInit = false;
	double [] xnodes;
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
		
		bInit = true;
		
	}
	public void notInit()
	{
		bInit = false;
	}
	public ArrayList<RealPoint> interpolate(double [] xp)
	{
		
		int nP = xp.length;
		double [] curr_point = new double[3]; 
		ArrayList<RealPoint> out = new ArrayList<RealPoint>();
		//just in case
		if (!bInit)
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
	//Lenght of the polyline
	public double getMaxLength()
	{
		if(bInit)
		{
			return xnodes[xnodes.length-1];
		}
		else
		{
			return Double.NaN;
		}
		
	}
	
}

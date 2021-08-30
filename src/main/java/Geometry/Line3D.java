package Geometry;

import org.joml.Vector3f;

import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

/**
 *  vector notation for a line in 3D:
 *  linev[0]+ linev[1]*d
 *  
 *  linev[0] - some vector on the line
 *  linev[1] - vector along the line (normalized)
 * **/
public class Line3D {

	public double [][] linev;
	
    /** empty constructor **/
	public Line3D()
	{
		linev= new double [2][3]; 
	}
	
	/** from two vectors on the line (from RealPoint) **/
	public Line3D(final RealPoint v1, final RealPoint v2)
	{

		linev= new double [2][3];
		v2.localize(linev[1]);
		v1.localize(linev[0]);
		LinAlgHelpers.subtract(linev[1], linev[0], linev[1]);
		LinAlgHelpers.normalize(linev[1]);
	}	
	/** from two vectors on the line (for Vector3f) **/
	public Line3D(final Vector3f v1, final Vector3f v2)
	{
		linev= new double [2][3];
		for (int i=0;i<3;i++)
		{
			linev[0][i]=v1.get(i);
			linev[1][i]=v2.get(i)-linev[0][i];
		}
		LinAlgHelpers.normalize(linev[1]);
	}
	
	/** from two vectors on the line (for double) **/
	public Line3D(final double [] v1, final double [] v2)
	{
		linev= new double [2][3];
		for (int i=0;i<3;i++)
		{
			linev[0][i]=v1[i];
			linev[1][i]=v2[i]-linev[0][i];
		}
		LinAlgHelpers.normalize(linev[1]);
	}
	//return a vector on the line at d
	public void value(final double d, final Vector3f out)
	{
		
		out.x = (float) (linev[0][0]+linev[1][0]*d);
		out.y = (float) (linev[0][1]+linev[1][1]*d);
		out.z = (float) (linev[0][2]+linev[1][2]*d);
	}
	//return a vector on the line at d
	public void value(final double d, final RealPoint out)
	{
		out.setPosition(linev[0][0]+linev[1][0]*d, 0);
		out.setPosition(linev[0][1]+linev[1][1]*d, 1);
		out.setPosition(linev[0][2]+linev[1][2]*d, 2);			
	}
	//return a vector on the line at d
	public void value(final double d, final double [] out)
	{
		out[0]=linev[0][0]+linev[1][0]*d;
		out[1]=linev[0][1]+linev[1][1]*d;
		out[2]=linev[0][2]+linev[1][2]*d;
	}
		
	public boolean planeIntersect(final Plane3D plane, final double [] intersectionPoint)
	{
		double dln=LinAlgHelpers.dot(linev[1], plane.n);
		if (Math.abs(dln)<0.00000001)
			{return false;}
		else
		{
			for(int i =0; i<3; i++)
			{
				intersectionPoint[i]=plane.p0[i]-linev[0][i];
			}
			dln=LinAlgHelpers.dot(intersectionPoint, plane.n)/dln;
			value(dln,intersectionPoint);
			return true;
		}
			
				
					
		
	}
}

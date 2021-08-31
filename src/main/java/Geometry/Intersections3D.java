package Geometry;

import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

public class Intersections3D {
	
	/** Function returns intersection points between "Plane3D" and "Line3D" 
	 *  in the "intersectionPoint" variable.
	 *  Returns "false" if the line does not intersect a plane
	 *  or lies in the plane completely. **/
	public static boolean planeLineIntersect(final Plane3D plane, final Line3D line, final double [] intersectionPoint)
	{
		double dln=LinAlgHelpers.dot(line.linev[1], plane.n);
		// maybe it is not a good idea, but ok for now
		if (Math.abs(dln)<0.00000000001) //maybe 2 * Double.MIN_VALUE (?)
			{return false;}
		else
		{
			for(int i =0; i<3; i++)
			{
				intersectionPoint[i]=plane.p0[i]-line.linev[0][i];
			}
			dln=LinAlgHelpers.dot(intersectionPoint, plane.n)/dln;
			line.value(dln,intersectionPoint);
			return true;
		}												
		
	}
	
	public static ArrayList<RealPoint> cuboidLinesIntersect(final Cuboid3D cuboid, final ArrayList<Line3D> lines)
	{
		ArrayList<RealPoint> int_points = new ArrayList<RealPoint>();
		int i,j;
		int nIntersLineN;
		double [] intersectionPoint = new double [3];
		
		for (i=0;i<lines.size();i++)
		{
			nIntersLineN = 0;
			for(j=0;j<6;j++)
			{
				//line intersects a cube
				if(planeLineIntersect(cuboid.faces.get(j), lines.get(i),intersectionPoint))
				{
					if(cuboid.isPointInsideMinMax(intersectionPoint))
					{
						int_points.add(new RealPoint(intersectionPoint)); 
						nIntersLineN++;
					}
				}
				if (nIntersLineN==2)
					break;
			}
			
		}
		return int_points;
		
	}


}

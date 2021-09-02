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
		if (Math.abs(dln)< 2 * Double.MIN_VALUE )
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

		//make sure we have faces
		if(!cuboid.faces_init)
			cuboid.iniFaces();
		
		for (i=0;i<lines.size();i++)
		{
			nIntersLineN = 0;
			for(j=0;j<6;j++)
			{
				//line intersects a cube
				if(planeLineIntersect(cuboid.faces.get(j), lines.get(i),intersectionPoint))
				{
					//if(cuboid.isPointInsideMinMax(intersectionPoint))
					if(cuboid.isPointInsideShape(intersectionPoint))
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
	
	public static boolean makeBoundBox(final ArrayList<RealPoint> pointArray, final long [][] newMinMax)
	{ 
		//= new long [2][3];
		float [][] newMinMaxF = new float [2][3];
		int i, j;
		float temp;

		for (i=0;i<3;i++)
		{
			newMinMaxF[0][i]=Float.MAX_VALUE;
			newMinMaxF[1][i]=(-1)*Float.MAX_VALUE;
		}
		for (i=0;i<pointArray.size();i++)
		{
			
			for (j=0;j<3;j++)
			{
				temp=pointArray.get(i).getFloatPosition(j);
				if(temp>newMinMaxF[1][j])
					newMinMaxF[1][j]=temp;
				if(temp<newMinMaxF[0][j])
					newMinMaxF[0][j]=temp;
				
			}			
		}

		return true;

	}
	public static boolean makeBoundBox(final ArrayList<RealPoint> pointArray, final double [][] newMinMax)
	{ 
		
		
		int i, j;
		double temp;

		for (i=0;i<3;i++)
		{
			newMinMax[0][i]=Double.MAX_VALUE;
			newMinMax[1][i]=(-1)*Double.MAX_VALUE;
		}
		for (i=0;i<pointArray.size();i++)
		{
			
			for (j=0;j<3;j++)
			{
				temp=pointArray.get(i).getDoublePosition(j);
				if(temp>newMinMax[1][j])
					newMinMax[1][j]=temp;
				if(temp<newMinMax[0][j])
					newMinMax[0][j]=temp;
				
			}			
		}

		return true;

	}


}

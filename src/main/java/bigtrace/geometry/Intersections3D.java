package bigtrace.geometry;

import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
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
	
	/** Function returns intersection points between "Plane3D" and an edge (defined by RP1 and RP2)  
	 *  in the "intersectionPoint" variable.
	 *  Returns "false" if the edge does not intersect a plane
	 *  or lies in the plane completely. **/
	public static boolean planeEdgeIntersect(final Plane3D plane, final RealPoint RP1, final RealPoint RP2, final double [] intersectionPoint)
	{
		
		final Line3D line = new Line3D(RP1,RP2);
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
			double edgeLn = LinAlgHelpers.distance(RP1.positionAsDoubleArray(), RP2.positionAsDoubleArray());
			if(dln>=0 && dln <=edgeLn)
			{
				line.value(dln,intersectionPoint);
				return true;
			}
			else
			{
				return false;
			}
		}												
		
	}
	/** given a set of input lines, generates points of intersection
	 * between them and input cuboid **/
	public static ArrayList<RealPoint> cuboidLinesIntersect(final Cuboid3D cuboid, final ArrayList<Line3D> lines)
	{
		ArrayList<RealPoint> int_points = new ArrayList<RealPoint>();
		int nIntersLineN;
		double [] intersectionPoint = new double [3];

		//make sure we have faces
		if(!cuboid.faces_init)
			cuboid.iniFaces();
		
		for (int i=0;i<lines.size();i++)
		{
			nIntersLineN = 0;
			for(int j=0;j<6;j++)
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
	/** given a set of input lines, generates points of intersection
	 * between them a plane **/
	public static ArrayList<RealPoint> planeLinesIntersect(final Plane3D plane, final ArrayList<Line3D> lines)
	{
		
		
		ArrayList<RealPoint> int_points = new ArrayList<RealPoint>();
		double [] intersectionPoint = new double [3];
		
		for (int i=0;i<lines.size();i++)
		{

			planeLineIntersect(plane, lines.get(i),intersectionPoint);
			int_points.add(new RealPoint(intersectionPoint)); 		
			
		}
		return int_points;
		
	}
	/**  function calculates transform allowing to align two vectors 
	 * @param align_direction - immobile vector
	 * @param moving - vector that aligned with align_direction
	 * @return affine transform (rotations)
	 * **/
	public static AffineTransform3D alignVectors(final RealPoint align_direction, final RealPoint moving)
	{
		double [] dstat = align_direction.positionAsDoubleArray();
		double [] dmov = moving.positionAsDoubleArray();
		double [] v = new double [3];
		double c;
		
		AffineTransform3D transform = new AffineTransform3D();
		LinAlgHelpers.normalize(dstat);
		LinAlgHelpers.normalize(dmov);
		c = LinAlgHelpers.dot(dstat, dmov);
		
		//exact opposite directions
		if ((c+1.0)<0.00001)
		{
			transform.identity();
			transform.scale(-1.0);
			return transform;
		}
		
		LinAlgHelpers.cross( dmov, dstat, v);
		double [][] matrixV = new double [3][3];
		double [][] matrixV2 = new double [3][3];
		
		matrixV[0][1]=(-1.0)*v[2];
		matrixV[0][2]=v[1];
		matrixV[1][0]=v[2];
		matrixV[1][2]=(-1.0)*v[0];
		matrixV[2][0]=(-1.0)*v[1];
		matrixV[2][1]=v[0];
		
		LinAlgHelpers.mult(matrixV, matrixV, matrixV2);
		c=1.0/(1.0+c);
		LinAlgHelpers.scale(matrixV2, c, matrixV2);
		LinAlgHelpers.add(matrixV, matrixV2, matrixV);
		transform.set(1.0 + matrixV[0][0],       matrixV[0][1],       matrixV[0][2], 0.0,
					        matrixV[1][0], 1.0 + matrixV[1][1],       matrixV[1][2], 0.0,
					        matrixV[2][0],       matrixV[2][1], 1.0 + matrixV[2][2], 0.0);
		
		return transform;
	}
	
	
	
	/** generates long minmax box from pointArray **/
	public static boolean makeBoundBox(final ArrayList<RealPoint> pointArray, final long [][] newMinMax)
	{ 
		int i,d;
		double [][] newMinMaxD = new double [2][3];
		if(makeBoundBox(pointArray, newMinMaxD))
		{
			for (i=0;i<2;i++)
			{		
				for (d=0;d<3;d++)
				{
					if(i==0)
					{
						newMinMax[i][d]=(long)Math.floor(newMinMaxD[i][d]);
					}
					else
					{
						newMinMax[i][d]=(long)Math.ceil(newMinMaxD[i][d]);
					}
				}
			}
		}
		{
			return false;
		}
		


	}
	/** generates double minmax box from pointArray **/
	public static boolean makeBoundBox(final ArrayList<RealPoint> pointArray, final double [][] newMinMax)
	{ 
		
		
		int i, d;
		double temp;

		for (i=0;i<3;i++)
		{
			newMinMax[0][i]=Double.MAX_VALUE;
			newMinMax[1][i]=(-1)*Double.MAX_VALUE;
		}
		for (i=0;i<pointArray.size();i++)
		{
			
			for (d=0;d<3;d++)
			{
				temp=pointArray.get(i).getDoublePosition(d);
				if(temp>newMinMax[1][d])
					newMinMax[1][d]=temp;
				if(temp<newMinMax[0][d])
					newMinMax[0][d]=temp;
				
			}			
		}

		return true;

	}


}

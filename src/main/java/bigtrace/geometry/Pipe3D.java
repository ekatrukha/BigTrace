package bigtrace.geometry;

import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

public class Pipe3D {
	
	
	/** given a set of points (polyline) generates "cylindrical contours/circles" 
	 *  around each point with a thickness of fLineThickness (diameter).
	 *  Each contour has BigTraceData.sectorN sectors**/
	public static ArrayList<ArrayList< RealPoint >> getCountours(final ArrayList< RealPoint > points, final int nSectorN, final double dRadius)
	{
		int i, iPoint;
		double [][] path = new double [3][3];
		double [] prev_segment = new double [3];
		//double [] next_segment = new double [3];
		double [] plane_norm = new double[3];
		double [] cont_point = new double [3];
		Plane3D planeBetween =new Plane3D();
		ArrayList<Line3D> extrudeLines;
		Line3D contLine;
		int contour_curr;
		int contour_next;
		ArrayList<double []> tangents;
		ArrayList<ArrayList< RealPoint >> allCountours = new ArrayList<ArrayList<RealPoint>>();
	
		int nPointsNum=points.size();
		if(nPointsNum>1)
		{
			
			//calculate tangents at each point
			tangents = getTangentsAverage(points);
	
			//first contour around first line
			
			//first two points
			for (i=0;i<2;i++)
			{
				points.get(i).localize(path[i]);
			}
			
			//vector between first two points 
			//equal to initial tangent vector
			prev_segment = tangents.get(0);
			RealPoint iniVec = new RealPoint(prev_segment );			
			RealPoint zVec = new RealPoint(0.0,0.0,1.0);
			//transform that rotates vector (0,0,1) to be aligned with the vector between first two points
			AffineTransform3D ini_rot = Intersections3D.alignVectors( iniVec,zVec);
			//also we want to move it to the beginning of the curve
			ini_rot.translate(path[0]);
			//generate a "circle/contour" with given sectors N in XY plane
			contour_curr = 0;
			allCountours.add(iniSectorContour( dRadius, nSectorN));
			
			//not translate it to the position of the beginning
			//and align it with the vector between first two point
			for(i=0;i<nSectorN;i++)
			{
				ini_rot.apply(allCountours.get(contour_curr).get(i), allCountours.get(contour_curr).get(i));
			}
			
			//just a verification, probably not needed
			//ini_rot.apply(zVec, zVec);
			
			
			//other contours, inbetween
			for (iPoint=1;iPoint<(points.size()-1);iPoint++)
			{
				//tangent vector at this point between segments
				plane_norm = tangents.get(iPoint);
				//calculate intersection
				// also there is a special case: reverse, basically do nothing
				// use the same contour
				if(LinAlgHelpers.length(plane_norm)>0.0000001)
				{					
					// plane of segments cross-section
					planeBetween.setVectors(path[1], plane_norm);
					//make a set of lines emanating from the current contour
					extrudeLines = new ArrayList<Line3D>();
					for(i=0;i<nSectorN;i++)
					{
						contLine=new Line3D();
						allCountours.get(contour_curr).get(i).localize(cont_point);
						contLine.setVectors(cont_point, prev_segment);
						extrudeLines.add(contLine);
					}
					
					//intersections
					allCountours.add(Intersections3D.planeLinesIntersect(planeBetween, extrudeLines));
					
					//test
					//if (bNormalize)
					//{
					//	allCountours.add(normalizeSector(Intersections3D.planeLinesIntersect(planeBetween, extrudeLines),points.get(iPoint),dRadius));
					//}
									
					contour_next =contour_curr+1;					
				}
				else
				{
					allCountours.add(allCountours.get(contour_curr));
					//reversed direction
					contour_next=contour_curr+1;
				}
				
				points.get(iPoint+1).localize(path[2]);
				LinAlgHelpers.subtract(path[2], path[1], prev_segment);
				LinAlgHelpers.normalize(prev_segment);

				//prepare to move forward, switch indexes
				for(i=0;i<3;i++)
				{					
					path[1][i]=path[2][i];
				}
				contour_curr=contour_next;
			}
			
			//THE last point
			points.get(nPointsNum-2).localize(path[0]);
			points.get(nPointsNum-1).localize(path[1]);
			//last segment is also just a last tangent (as with first)
			plane_norm = tangents.get(nPointsNum-1);
			
			planeBetween.setVectors(path[1], plane_norm);
			
			extrudeLines = new ArrayList<Line3D>();
			for(i=0;i<nSectorN;i++)
			{
				contLine=new Line3D();
				allCountours.get(contour_curr).get(i).localize(cont_point);
				contLine.setVectors(cont_point, plane_norm);
				extrudeLines.add(contLine);
			}
			//intersections
			allCountours.add(Intersections3D.planeLinesIntersect(planeBetween, extrudeLines));
			
			//test
			//if (bNormalize)
			//{
			//	allCountours.add(normalizeSector(Intersections3D.planeLinesIntersect(planeBetween, extrudeLines),points.get(nPointsNum-1),dRadius));
			//}
			
		}
		return allCountours;
		
	}
	/** given a set of points defining polyline the function calculates 
	 * tangent vector (not normalized) at each point as a average angle 
	 * between two adjacent segments **/
	public static ArrayList<double []> getTangentsAverage(final ArrayList< RealPoint > points)
	{
		int i,j;
		ArrayList<double []> tangents = new ArrayList<double []>();
		double [][] path = new double [3][3];
		double [] prev_segment = new double [3];
		double [] next_segment = new double [3];
		double [] tanVector = new double [3];
		int nPointsNum=points.size();
		if(nPointsNum>1)
		{
			//first two points
			for (i=0;i<2;i++)
			{
				points.get(i).localize(path[i]);
			}
			//vector between first two points 
			LinAlgHelpers.subtract(path[1], path[0], prev_segment );
			LinAlgHelpers.normalize(prev_segment);
			tangents.add(prev_segment.clone());
			//the middle
			for (i=1;i<(points.size()-1);i++)
			{
				//next segment
				points.get(i+1).localize(path[2]);
				LinAlgHelpers.subtract(path[2], path[1], next_segment);
				LinAlgHelpers.normalize(next_segment);

				//2) average angle/vector between segments
				LinAlgHelpers.add(prev_segment, next_segment, tanVector);
				LinAlgHelpers.scale(tanVector, 0.5, tanVector);
				tangents.add(tanVector.clone());
				
				//prepare to move forward
				for(j=0;j<3;j++)
				{
					prev_segment[j]=next_segment[j];
					path[1][j]=path[2][j];
				}

			}
			points.get(nPointsNum-2).localize(path[0]);
			points.get(nPointsNum-1).localize(path[1]);
			LinAlgHelpers.subtract(path[0],path[1],tanVector);
			LinAlgHelpers.normalize(tanVector);
			tangents.add(tanVector.clone());
		}
		return tangents;
	}
	
	/** generates initial (first point) contour around path in XY plane **/
	public static ArrayList< RealPoint > iniSectorContour(double dRadius, final int nSectorN)
	{
		 ArrayList< RealPoint > contourXY = new  ArrayList< RealPoint > ();
		 
		 double dAngleInc = 2.0*Math.PI/(nSectorN);
		 double dAngle = 0.0;
		 
		 for (int i=0;i<nSectorN;i++)
		 {
			 contourXY.add(new RealPoint(dRadius*Math.cos(dAngle),dRadius*Math.sin(dAngle),0.0));
			 dAngle+=dAngleInc;
		 }
		 
		 return contourXY;
	}
	
	/** for each contour normalizes distances till center **/
	public static ArrayList< RealPoint >  normalizeSector(final ArrayList< RealPoint > points, final RealPoint center, final double dRadius)
	{
		int i;
		double [] contPoint = new double [3];
		double [] centPoint = new double [3];
		ArrayList< RealPoint > out = new ArrayList<RealPoint>();
		
		center.localize(centPoint);
		for(i=0;i<points.size();i++)
		{
			points.get(i).localize(contPoint);
			LinAlgHelpers.subtract(contPoint,centPoint,contPoint);
			LinAlgHelpers.normalize(contPoint);
			LinAlgHelpers.scale(contPoint, dRadius, contPoint);
			LinAlgHelpers.add(contPoint, centPoint, contPoint);
			out.add(new RealPoint(contPoint));
		}
		return out;
		
	}
}

package bigtrace.rois;

import java.util.ArrayList;

import bigtrace.BigTrace;

import bigtrace.geometry.CurveShapeInterpolation;
import bigtrace.geometry.Pipe3D;
import bigtrace.measure.Circle2DMeasure;
import bigtrace.measure.MeasureValues;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


/** joint methods for Polyline3D and LineTrace3D**/

public abstract class AbstractCurve3D extends AbstractRoi3D
{
	public ArrayList<RealPoint> vertices;
	CurveShapeInterpolation interpolator = null;
	
	/** returns the length of Polyline using globCal voxel size **/
	public double getLength(int nShapeInterpolation, final double [] globCal)
	{

		return interpolator.getLength();
		
	}
	/** returns distance between ends in SPACE units **/
	public double getEndsDistance(final double [] globCal)
	{
		if(vertices.size()>1)
		{
			double [] posB = new double [3];
			double [] posE = new double [3];
			Roi3D.scaleGlob(vertices.get(0),globCal).localize(posB);
			Roi3D.scaleGlob(vertices.get(vertices.size()-1),globCal).localize(posE);
			return LinAlgHelpers.distance(posB, posE);
		}
		else
		{
			
			return Double.NaN;
		}
			
	}
	
	/** puts ends coordinates to the val **/
	public void getEnds(final MeasureValues val, final double [] globCal)
	{
		val.ends = new RealPoint [2];
		val.ends[0]= new RealPoint(Roi3D.scaleGlob(vertices.get(0),globCal));
		if(vertices.size()>1)
		{
			val.ends[1]= new RealPoint(Roi3D.scaleGlob(vertices.get(vertices.size()-1),globCal));
		}
		else
		{
			val.ends[1] =Roi3D.getNaNPoint();
		}
		return;
	}
	
	/** returns direction of the vector from one to another end (in val) **/
	public void getEndsDirection(final MeasureValues val, final double [] globCal)
	{
		if(vertices.size()>1)
		{
			double [] posB = new double [3];
			double [] posE = new double [3];
			Roi3D.scaleGlob(vertices.get(0),globCal).localize(posB);
			Roi3D.scaleGlob(vertices.get(vertices.size()-1),globCal).localize(posE);
			LinAlgHelpers.subtract(posE, posB, posE);
			LinAlgHelpers.normalize(posE);
			val.direction=new RealPoint(posE);
		}
		else
		{
			
			val.direction = Roi3D.getNaNPoint();
		}
			
	}
	
	/** Returns points sampled along the reference curve with a smallest voxel size step.
	 * **/
	public ArrayList<RealPoint> getJointSegmentResampled()
	{
		return interpolator.getVerticesResample();

	}
	/** Returns tangents at points sampled along the reference curve with a smallest voxel size step.*/
	public ArrayList<double[]> getJointSegmentTangentsResampled()
	{
		return interpolator.getTangentsResample();
	}
	
	/** Measures intensity profile along the ROI;
	 * @param	source	IntervalView of measurement
	 * @param	globCal voxel size
	 * @param	nRadius	Radius of pipe around line
	 * @param	nInterpolatorFactory intensity Interpolation method (factory)
	 * @param	nShapeInterpolation curve shape interpolation
	 * @return double [i][j] array where for position i
	 * 0 is length along the line (in scaled units)
	 * 1 intensity
	 * 2 x coordinate (in scaled units) 
	 * 3 y coordinate (in scaled units) 
	 * 4 z coordinate (in scaled units) **/
	public < T extends RealType< T > >  double [][] getIntensityProfilePipe(final IntervalView<T> source, final double [] globCal, final int nRadius, final InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory, final int nShapeInterpolation)
	{
	
		final ArrayList<RealPoint> allPoints = getJointSegmentResampled();
		
		if(allPoints == null)
			return null;
		final ArrayList<double []> allTangents = getJointSegmentTangentsResampled();
		
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(source),nInterpolatorFactory);
		
		
		return getIntensityProfilePointsPipe(allPoints,allTangents, nRadius, interpolate,globCal);
	}
	
	/**
	 *  OBSOLETE, works only with 1 pix tickness
	 *
	 * @deprecated use {@link #getIntensityProfilePipe(IntervalView, double [], int, InterpolatorFactory, int)} instead.  
	 */
	@Deprecated
	public < T extends RealType< T > >  double [][] getIntensityProfile(final IntervalView<T> source, final double [] globCal, final InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory, final int nShapeInterpolation)
	{
	
		ArrayList<RealPoint> allPoints = getJointSegmentResampled();
		if(allPoints==null)
			return null;

		
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(source),nInterpolatorFactory);
		
		return getIntensityProfilePoints(allPoints,interpolate,globCal);
	}
	
	/** gets intensity values at allPoints position (provided in SPACE units) at 
	 * the interpolate RRA 
	 * it is assumes that allPoints are sampled with the same length step,
	 * equal to dMinVoxelSize **/
	public < T extends RealType< T > >  double [][] getIntensityProfilePoints(final ArrayList<RealPoint> allPoints, RealRandomAccessible<T> interpolate, final double [] globCal)
	{
		double [][] out = new double [5][allPoints.size()];
		
		final RealRandomAccess<T> ra =   interpolate.realRandomAccess();
		double [] pos = new double[3];
		double [] xyz;
		final double dMinVoxelSize = Math.min(Math.min(globCal[0], globCal[1]),globCal[2]);
		
		int i,d;
		
		//first point
		out[0][0]=0.0;
		allPoints.get(0).localize(pos);
		xyz = pos.clone();
		//in voxels
		pos = Roi3D.scaleGlobInv(pos, globCal);
		ra.setPosition(pos);
		//intensity
		out[1][0]=ra.get().getRealDouble();
		//length
		
		//point location
		for(d=0;d<3;d++)
		{
			out[2+d][0]=xyz[d];
		}
		for(i=1;i<allPoints.size();i++)
		{
			allPoints.get(i).localize(pos);
			out[0][i] = dMinVoxelSize*i;
			xyz= pos.clone();			
			//in voxels
			pos = Roi3D.scaleGlobInv(pos, globCal);
			ra.setPosition(pos);
			//intensity
			out[1][i]=ra.get().getRealDouble();
			for(d=0;d<3;d++)
			{
				out[2+d][i]=xyz[d];
			}
		}

		return out;
	}
	/** TEST voxel placement**/
	public < T extends RealType< T > >  double [][] getIntensityProfilePipeTEST(final BigTrace<T> bt, final IntervalView<T> source, final double [] globCal, final int nRadius, final InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory, final int nShapeInterpolation)
	{
	
		final ArrayList<RealPoint> allPoints = getJointSegmentResampled();
		
		if(allPoints == null)
			return null;
		final ArrayList<double []> allTangents = getJointSegmentTangentsResampled();
		
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(source),nInterpolatorFactory);
		
		
		return getIntensityProfilePointsPipeTEST(bt, allPoints,allTangents, nRadius, interpolate,globCal);
	}
	/** gets intensity values at allPoints position (provided in SPACE units) at 
	 * the interpolate RRA 
	 * it is assumes that allPoints are sampled with the same length step,
	 * equal to dMinVoxelSize **/
	public < T extends RealType< T > >  double [][] getIntensityProfilePointsPipe(final ArrayList<RealPoint> points, final ArrayList<double[]> tangents, final int nRadius, RealRandomAccessible<T> interpolate, final double [] globCal)
	{
		double [][] out = new double [5][points.size()];
		
		final RealRandomAccess<T> ra =   interpolate.realRandomAccess();
		double [] current_pixel = new double[3];
		double dInt;
		int nPixN;
		final double dMinVoxelSize = Math.min(Math.min(globCal[0], globCal[1]),globCal[2]);
		
		//get a frame around line
		double [][][] rsVect =  Pipe3D.rotationMinimizingFrame(points, tangents);
		int d;
		double [] current_point = new double [3];
		Circle2DMeasure measureCircle = new Circle2DMeasure();
		
		measureCircle.setRadius(nRadius);
		for (int nPoint = 0;nPoint<points.size();nPoint++)
		{
			//length
			out[0][nPoint] = dMinVoxelSize*nPoint;
			
			//log point location
			points.get(nPoint).localize(current_point); 
			for(d=0;d<3;d++)
			{
				out[2+d][nPoint]=current_point[d];
			}
			//reset cursor
			measureCircle.cursorCircle.reset();
			
			//get intensities in perpendicular plane
			//iterate over voxels of circle
			nPixN =0;
			dInt =0.0;
			while (measureCircle.cursorCircle.hasNext())
			{
				measureCircle.cursorCircle.fwd();
				measureCircle.cursorCircle.localize(current_pixel);
				LinAlgHelpers.scale(current_pixel, dMinVoxelSize, current_pixel);
				getVoxelInPlane(rsVect[0][nPoint],rsVect[1][nPoint], current_point,current_pixel);
				//back to voxel units
				current_pixel =Roi3D.scaleGlobInv(current_pixel, globCal);
				ra.setPosition(current_pixel);
				//intensity
				dInt+=ra.get().getRealDouble();
				nPixN++;
			}
			out[1][nPoint] = dInt/nPixN;
		}

		return out;
	}
	
	/** TEST voxel placement **/
	public < T extends RealType< T > >  double [][] getIntensityProfilePointsPipeTEST(final BigTrace<T> bt, final ArrayList<RealPoint> points, final ArrayList<double[]> tangents, final int nRadius, RealRandomAccessible<T> interpolate, final double [] globCal)
	{
		double [][] out = new double [5][points.size()];
		
		final RealRandomAccess<T> ra =   interpolate.realRandomAccess();
		double [] current_pixel = new double[3];
		double dInt;
		int nPixN;
		final double dMinVoxelSize = Math.min(Math.min(globCal[0], globCal[1]),globCal[2]);
		
		//get a frame around line
		double [][][] rsVect =  Pipe3D.rotationMinimizingFrame(points, tangents);
		int d;
		double [] current_point = new double [3];
		Circle2DMeasure measureCircle = new Circle2DMeasure();
		
		measureCircle.setRadius(nRadius);
		for (int nPoint = 0;nPoint<points.size();nPoint++)
		{
			//length
			out[0][nPoint] = dMinVoxelSize*nPoint;
			
			//log point location
			points.get(nPoint).localize(current_point); 
			for(d=0;d<3;d++)
			{
				out[2+d][nPoint]=current_point[d];
			}
			//reset cursor
			measureCircle.cursorCircle.reset();
			
			//get intensities in perpendicular plane
			//iterate over voxels of circle
			nPixN =0;
			dInt =0.0;
			while (measureCircle.cursorCircle.hasNext())
			{
				measureCircle.cursorCircle.fwd();
				measureCircle.cursorCircle.localize(current_pixel);
				LinAlgHelpers.scale(current_pixel, dMinVoxelSize, current_pixel);
				getVoxelInPlane(rsVect[0][nPoint],rsVect[1][nPoint], current_point,current_pixel);
				//back to voxel units
				current_pixel =Roi3D.scaleGlobInv(current_pixel, globCal);
				if(nPoint<10 && nPoint%2==0)
				{
					bt.roiManager.addPoint(new RealPoint(current_pixel));
				}
				ra.setPosition(current_pixel);
				//intensity
				dInt+=ra.get().getRealDouble();
				nPixN++;
			}
			out[1][nPoint] = dInt/nPixN;
		}

		return out;
	}
	
	private static void getVoxelInPlane(final double [] x,final double [] y, final double [] c, final double [] voxel)
	{
		 double [] xp = new double[3];
		 double [] yp = new double[3];
		 LinAlgHelpers.scale(x, voxel[0], xp);
		 LinAlgHelpers.scale(y, voxel[1], yp);
		 LinAlgHelpers.add(xp, yp,xp);
		 LinAlgHelpers.add(xp, c,voxel);
	}
	/** returns cosine or an angle (from 0 to pi, determined by bCosine) 
	 *  between dir_vector (assumed to have length of 1.0) and each segment of the line Roi. 
	 *  The output is double [i][j] array where for position i
	 * 0 is length along the line (in scaled units)
	 * 1 orientation (cosine or angle in radians)
	 * 2 x coordinate (in scaled units) 
	 * 3 y coordinate (in scaled units) 
	 * 4 z coordinate (in scaled units) **/
	public double [][] getCoalignmentProfile(final double [] dir_vector, final double [] globCal, final int nShapeInterpolation, final boolean bCosine)
	{

		final ArrayList<RealPoint> allPoints = getJointSegmentResampled();
		if(allPoints == null)
			return null;
		
		return getCoalignmentProfilePoints(allPoints, dir_vector, bCosine);
	}
	
	
	
	/** assumes that input allPoints are in SPACE coordinates.
	 *  calculates angle between each segment and dir_vector.
	 *  If bCosine = true returns cosine. Otherwise returns angle value (in radians).
	 *  Returned array contains
	 *  [0] - cumulative length values at the middle of each segment
	 *  [1] - cosine or angle
	 *  [2] - x coordinate of the middle of the segment (in SPACE scaled coordinates)
	 *  [3] - y coordinate of the middle of the segment (in SPACE scaled coordinates)
	 *  [4] - z coordinate of the middle of the segment (in SPACE scaled coordinates)
	 *  of second dimension allPoints.size()-1 (number of segments) **/
	public double [][] getCoalignmentProfilePoints(final ArrayList<RealPoint> allPoints, final double [] dir_vector, final boolean bCosine)
	{
		int i,d;
		double [][] out = new double [5][allPoints.size()-1];
		double [] pos1 = new double[3];
		double [] pos2 = new double[3];
		double [] segmDir = new double[3];
		double segmLength;
		double prevCumLength = 0.0;
		
		allPoints.get(0).localize(pos1);
		//pos1 = Roi3D.scaleGlob(pos1, globCal);
		for(i=1;i<allPoints.size();i++)
		{
			
			allPoints.get(i).localize(pos2);			
			//pos2 = Roi3D.scaleGlob(pos2, globCal);
			segmLength = LinAlgHelpers.distance(pos1,pos2);
			out[0][i-1] = prevCumLength+segmLength*0.5;
			prevCumLength+=segmLength;
			LinAlgHelpers.subtract(pos2, pos1, segmDir);
			for(d=0;d<3;d++)
			{
				//position of the middle of segment 
				out[2+d][i-1]=pos1[d]+segmDir[d]*0.5;
				pos1[d]=pos2[d];
				//normalize segment's direction length
				segmDir[d]=segmDir[d]/segmLength;
			}
			if(bCosine)
			{
				out[1][i-1] = LinAlgHelpers.dot(dir_vector, segmDir);
			}
			else
			{
				out[1][i-1] = Math.acos(LinAlgHelpers.dot(dir_vector, segmDir));
			}
		}
		return out;
	}
}
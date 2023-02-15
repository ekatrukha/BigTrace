package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.BigTraceData;
import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineScaled;
import bigtrace.volume.VolumeMisc;
import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.real.WritablePolyline;
import net.imglib2.roi.util.RealLocalizableRealPositionable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class PolyLine3D extends AbstractRoi3D implements Roi3D, WritablePolyline 
{
	
	public ArrayList<RealPoint> vertices;
	public VisPointsScaled verticesVis;
	public VisPolyLineScaled edgesVis;


	public PolyLine3D(final Roi3DGroup preset_in)
	{
		type = Roi3D.POLYLINE;
		
		pointSize = preset_in.pointSize;
		lineThickness=preset_in.lineThickness;
		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());
		lineColor = new Color(preset_in.lineColor.getRed(),preset_in.lineColor.getGreen(),preset_in.lineColor.getBlue(),preset_in.lineColor.getAlpha());
				
		renderType = preset_in.renderType;

		vertices = new ArrayList<RealPoint>();
		verticesVis = new VisPointsScaled();
		verticesVis.setColor(pointColor);
		verticesVis.setSize(pointSize);
		edgesVis = new VisPolyLineScaled();
	
		edgesVis.bSmooth = false;
		edgesVis.setColor(lineColor);
		edgesVis.setThickness(lineThickness);
		edgesVis.setRenderType(renderType);
		name = "polyl"+Integer.toString(this.hashCode());

	}
	
	//adds a point to the "end" of polyline
	public void addPointToEnd(final RealPoint in_)
	{

		if (vertices.size()>0)
		{
			//check if the new point is at the same place that previous or not
			double [] dist = new double [3];
			LinAlgHelpers.subtract(vertices.get(vertices.size()-1).positionAsDoubleArray(), in_.positionAsDoubleArray(), dist);
			if(LinAlgHelpers.length(dist)>0.000001)
			{
				vertices.add(new RealPoint(in_));
			}
		}
		else
		{
			vertices.add(new RealPoint(in_));			
		}
		updateRenderVertices();
	}
	//removes the point from the "end" and returns "true"
	//if it is the last point, returns "false"
	public boolean removeEndPoint()
	 {
		 
		 int nP= vertices.size();
		 if(nP>0)
			{
			 	vertices.remove(nP-1);
			 	updateRenderVertices();
			 	if(nP==1)
			 		return false;
			 	else
			 		return true;
			}
		 return false;
	 }
	
	
	public void updateRenderVertices()
	{

		verticesVis.setVertices(vertices);
		//System.out.println("1");
		switch (BigTraceData.shapeInterpolation)
		{
			case BigTraceData.SHAPE_Voxel:
				edgesVis.setVerticesBresenham(vertices);	
				break;

			case BigTraceData.SHAPE_Subvoxel:
				edgesVis.setVertices(vertices);	
				break;
		}
		//edgesVis.setVerticesBresenham(vertices);		
		//System.out.println("2");
		
	}
	public void setVertices(ArrayList<RealPoint> vertices_)
	{
		vertices = new ArrayList<RealPoint>();
		for(int i=0;i<vertices_.size();i++)
			vertices.add(new RealPoint(vertices_.get(i)));		
		updateRenderVertices();
		
	}

	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) 
	{
		verticesVis.draw(gl, pvm, screen_size);
		edgesVis.draw(gl, pvm);
		
	}


	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());
		verticesVis.setColor(pointColor);
	}
	
	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		edgesVis.setColor(lineColor);
	}


	@Override
	public void setLineThickness(float line_thickness) {

		lineThickness=line_thickness;
		edgesVis.setThickness(lineThickness);
		updateRenderVertices();
	}

	@Override
	public void setPointSize(float point_size) {

		pointSize=point_size;
		verticesVis.setSize(pointSize);
		
	}
	

	
	@Override
	public void setRenderType(int nRenderType){
	
		renderType=nRenderType;
		edgesVis.setRenderType(renderType);
		updateRenderVertices();
	}	
	
	
	/** Methods from imglib2 Polyline, 
	 * I do not really understand them yet, 
	 * but added for the future and implemented them
	 * to the best of my knowledge (and so they do not produce errors)
	 */
	@Override
	public int numVertices() {
		return vertices.size();
	}

	@Override
	public RealMaskRealInterval and(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.and(this, paramPredicate);
	}

	@Override
	public RealMaskRealInterval minus(Predicate<? super RealLocalizable> paramPredicate) {
		
		// TODO Auto-generated method stub
		return Masks.minus(this, paramPredicate);
	}

	@Override
	public RealMask negate() {
		// TODO Auto-generated method stub
		return Masks.negate(this);
	}

	@Override
	public RealMask or(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.or(this, paramPredicate);
	}

	@Override
	public RealMask xor(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.xor(this, paramPredicate);
	}

	@Override
	public boolean test(RealLocalizable arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int numDimensions() {
		// TODO Auto-generated method stub
		return 3;
	}

	@Override
	public double realMin(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double realMax(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RealLocalizableRealPositionable vertex(int paramInt) {
		// TODO Auto-generated method stub
		return (RealLocalizableRealPositionable) vertices.get(paramInt);
	}

	@Override
	public void addVertex(int paramInt, RealLocalizable paramRealLocalizable) {
		// TODO Auto-generated method stub
		vertices.add((RealPoint) paramRealLocalizable);
	}

	@Override
	public void removeVertex(int paramInt) {
		// TODO Auto-generated method stub
		vertices.remove(paramInt);
	}

	@Override
	public void addVertices(int paramInt, Collection<RealLocalizable> paramCollection) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void saveRoi(final FileWriter writer)
	{
		int i, iPoint;
		float [] vert;
		
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		DecimalFormat df3 = new DecimalFormat ("#.###", symbols);
		try {
			writer.write("Type," + Roi3D.intTypeToString(this.getType())+"\n");
			writer.write("Name," + this.getName()+"\n");
			writer.write("GroupInd," + Integer.toString(this.getGroupInd())+"\n");
			writer.write("PointSize," + df3.format(this.getPointSize())+"\n");
			writer.write("PointColor,"+ Integer.toString(pointColor.getRed()) +","
									  +	Integer.toString(pointColor.getGreen()) +","
									  +	Integer.toString(pointColor.getBlue()) +","
									  +	Integer.toString(pointColor.getAlpha()) +"\n");
			writer.write("LineThickness," + df3.format(this.getLineThickness())+"\n");
			writer.write("LineColor,"+ Integer.toString(lineColor.getRed()) +","
									  +	Integer.toString(lineColor.getGreen()) +","
									  +	Integer.toString(lineColor.getBlue()) +","
									  +	Integer.toString(lineColor.getAlpha()) +"\n");
			writer.write("RenderType,"+ Integer.toString(this.getRenderType())+"\n");
			
			writer.write("Vertices,"+Integer.toString(vertices.size())+"\n");
			vert = new float[3];
			for (iPoint = 0;iPoint<vertices.size();iPoint++)
			{ 
				vertices.get(iPoint).localize(vert);
				for(i=0;i<3;i++)
				{
					writer.write(df3.format(vert[i])+",");
				}
				//time point
				writer.write("0.0\n");
			}
		}
		catch (IOException e) {	
			System.err.print(e.getMessage());
			
		}
	}
	@Override
	public void reversePoints() {
		
		vertices = Roi3D.reverseArrayRP(vertices); 		
		updateRenderVertices();
		return;
		
	}

	@Override
	public void setGroup(final Roi3DGroup preset_in) {
		
		setPointColor(preset_in.pointColor);
		setLineColor(preset_in.lineColor);

		setRenderType(preset_in.renderType);
		setPointSize(preset_in.pointSize);
		setLineThickness(preset_in.lineThickness);
	}

	/** returns the length of Polyline using globCal voxel size **/
	public double getLength(int nShapeInterpolation, final double [] globCal)
	{

		return Roi3D.getSegmentLength(makeJointSegment(nShapeInterpolation, globCal), globCal);
		
	}
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
	/** returns direction of the vector from one to another end**/
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
	/** returns double [i][j] array where for position i
	 * 0 is length along the line (in scaled units)
	 * 1 intensity
	 * 2 x coordinate (in scaled units) 
	 * 3 y coordinate (in scaled units) 
	 * 4 z coordinate (in scaled units) **/
	public < T extends RealType< T > >  double [][] getIntensityProfile(final IntervalView<T> source, final double [] globCal, final InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory, final int nShapeInterpolation)
	{
		final ArrayList<RealPoint> allPoints = makeJointSegment(nShapeInterpolation, globCal);
		
		if(allPoints == null)
			return null;
		
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(source),nInterpolatorFactory);
		
		
		return getIntensityProfilePoints(allPoints,interpolate,globCal);
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
		final ArrayList<RealPoint> allPoints = makeJointSegment(nShapeInterpolation, globCal);
		if(allPoints == null)
			return null;
		
		return getCoalignmentProfilePoints(allPoints, dir_vector, globCal,  bCosine);
	}
	
	/**Creates a sampled set of points along the polyline in VOXEL coordinates,
	 * based on the shape interpolation value.
	 * It needs globCal only to sample linear segments with a smallest voxel size step,
	 * the output is still in VOXEL coordinates **/
	private ArrayList<RealPoint> makeJointSegment(int nShapeInterpolation, final double [] globCal) {
		
		ArrayList<RealPoint> out = new ArrayList<RealPoint>();
		ArrayList<RealPoint> segment = new ArrayList<RealPoint>();
		double [] pos1 = new double [3];
		double [] pos2 = new double [3];

		if(vertices.size()>1)
		{

			//first vertex
			out.add(vertices.get(0));
			for(int i=1;i<vertices.size(); i++)
			{
				vertices.get(i-1).localize(pos1);
				vertices.get(i).localize(pos2);
				if(LinAlgHelpers.distance(pos1, pos2)>0.0)
				{
					switch (nShapeInterpolation)
					{
					case BigTraceData.SHAPE_Voxel:
						//bresenham voxel by voxel shape interpolation
						segment = VolumeMisc.BresenhamWrap(vertices.get(i-1), vertices.get(i));
						break;
					case BigTraceData.SHAPE_Subvoxel:
						//interpolation along the line (taking into account non-isotropic voxel size)
						segment = subPixelLine(vertices.get(i-1), vertices.get(i),globCal);
						break;
					}
					for(int j = 1; j<segment.size();j++)
					{
						out.add(segment.get(j));
					}
				}
			}
			//in case vertices are at the same locations
			if(out.size()<=1)
			{
				out= null;
			}
		}
		else 
		{
			return null;
		}
		return out;
	}
	/** Function samples a line between two 3D points with the step which approximate length is equal to 
	 *  the smallest size of the voxel in all dimensions (stored in globCal variable).
	 *  Since voxel can have different size in any dimension, it takes that to account.
	 *  Returns a set of points in VOXEL (not space) coordinates. **/
	public static ArrayList<RealPoint> subPixelLine(final RealPoint RP1, final RealPoint RP2, final double [] globCal)
	{
		ArrayList<RealPoint> out = new ArrayList<RealPoint>();
		//smallest size of the voxel in all dimensions
		double minPixSize = Math.min(globCal[0], Math.min(globCal[1], globCal[2]));
		double [][] pos = new double [3][3];
		double length =0.0;
		int nPoints;
		double dStepSize;
		//distance between points in space units
		Roi3D.scaleGlob(RP1, globCal).localize(pos[0]);
		Roi3D.scaleGlob(RP2, globCal).localize(pos[1]);
		length = LinAlgHelpers.distance(pos[0], pos[1]);
		nPoints = (int) Math.round(length/minPixSize);
		dStepSize = length/nPoints;
		out.add(RP1);
		//direction of the vector between points
		LinAlgHelpers.subtract(pos[1], pos[0], pos[2]);
		LinAlgHelpers.normalize(pos[2]);
		for(int i=1;i<=nPoints;i++)
		{
			LinAlgHelpers.scale(pos[2], i*dStepSize, pos[1]);
			LinAlgHelpers.add( pos[1], pos[0], pos[1]);
			out.add(new RealPoint(Roi3D.scaleGlobInv(pos[1], globCal)));
		}
		return out;
	}
	

}


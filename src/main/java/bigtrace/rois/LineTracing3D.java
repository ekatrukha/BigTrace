package bigtrace.rois;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineSimple;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.real.WritablePolyline;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

public class LineTracing3D implements Roi3D, WritablePolyline
{
	
	public ArrayList<RealPoint> vertices;
	public ArrayList<ArrayList<RealPoint>> segments;
	public VisPointsScaled verticesVis;
	public ArrayList<VisPolyLineSimple> segmentsVis;
	public float lineThickness;
	public float pointSize;
	public Color lineColor;
	public Color pointColor;
	public String name;
	public int type;
	
	public LineTracing3D(final float lineThickness_, final float pointSize_, final Color lineColor_, final Color pointColor_)
	{
		type = Roi3D.LINE_TRACE;
		lineThickness=lineThickness_;
		pointSize = pointSize_;
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());		
		vertices = new ArrayList<RealPoint>();
		segments = new ArrayList<ArrayList<RealPoint>>();
		verticesVis = new VisPointsScaled();
		verticesVis.setColor(pointColor_);
		verticesVis.setSize(pointSize_);
		segmentsVis = new ArrayList<VisPolyLineSimple>();
		

	}
	/** adds initial vertex **/
	public void addFirstPoint(final RealPoint in_)
	{
		vertices.add(new RealPoint(in_));
		verticesVis.setVertices(vertices);
	}
	
	public void addPointAndSegment(final RealPoint in_, final ArrayList<RealPoint> segments_)
	{
		vertices.add(new RealPoint(in_));
		verticesVis.setVertices(vertices);
		segments.add(segments_);
		segmentsVis.add(new VisPolyLineSimple(segments_,lineThickness,lineColor));
	}
	
	/** removes last segment of the tracing.
	 * if there was just one spot, returns false**/
	public boolean removeLastSegment() 
	{
		
		vertices.remove(vertices.size()-1);
		verticesVis.setVertices(vertices);
		if(vertices.size()>0)
		{
			segments.remove(segments.size()-1);
			segmentsVis.remove(segmentsVis.size()-1);
			return true;
		}
		else
		{
			return false;
		}
		
	}
	
	/** returns the last segment of the tracing.**/
	public ArrayList<RealPoint> getLastSegment() 
	{
		
		return segments.get(segments.size()-1);
		
	}

	/*
	//adds a point to the "end" of polyline
	public void addPointToEnd(final RealPoint in_)
	{
		vertices.add(new RealPoint(in_));
	}
	//removes the point from the "end" and returns "true"
	//if it is the last point, returns "false"
	public boolean removeEndPoint()
	 {
		 
		 int nP= vertices.size();
		 if(nP>0)
			{
			 	vertices.remove(nP-1);
			 	if(nP==1)
			 		return false;
			 	else
			 		return true;
			}
		 return false;
	 }

*/

	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) {
		
		
		//float[] colorComp  = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
		//pointColor.getComponents(colorComp);
		//VisPointsScaled points= new VisPointsScaled(vertices,pointSize, pointColor);
		//points.draw( gl, pvm, screen_size);		
		//VisPolyLineSimple lines;
		//lineColor.getComponents(colorComp);
		verticesVis.draw(gl, pvm, screen_size);
		
		for (VisPolyLineSimple segment : segmentsVis)
		{
			segment.draw(gl, pvm);
		}
		//for (int i=0;i<segments.size();i++)
		//{
			//lines = new VisPolyLineSimple(segments.get(i), lineThickness,lineColor);		
			//lines.draw( gl, pvm);
		//}
	}

	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		
		for (VisPolyLineSimple segment : segmentsVis)
		{
			segment.setColor(lineColor);
		}
	}

	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());	
		verticesVis.setColor(pointColor);
	}
	@Override
	public void setLineThickness(float line_thickness) {

		lineThickness=line_thickness;
		for (VisPolyLineSimple segment : segmentsVis)
		{
			segment.setThickness(lineThickness);
		}
	}

	@Override
	public void setPointSize(float point_size) {

		pointSize=point_size;
		verticesVis.setSize(pointSize);
	}

	@Override
	public int getType() {
		
		return type;
	}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	
	/** Methods from imglib2 Polyline, 
	 * I do not really understand them yet, 
	 * but added for the future and implemented them
	 * to the best of my knowledge (and so they do not produce errors)
	 */
	
	public int numVertices() {
		return vertices.size();
	}
	
	
	public int numSegments() {
		return segments.size();
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
	public float getLineThickness() {

		return lineThickness;
	}

	@Override
	public float getPointSize() {

		return pointSize;
	}



	
}


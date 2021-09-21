package bigtrace.rois;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsSimple;
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
	}
	/** adds initial vertex **/
	public void addFirstPoint(final RealPoint in_)
	{
		vertices.add(new RealPoint(in_));
	}
	
	public void addPointAndSegment(final RealPoint in_, final ArrayList<RealPoint> segments_)
	{
		vertices.add(new RealPoint(in_));
		segments.add(segments_);
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
	public void draw(GL3 gl, Matrix4fc pvm, double[] screen_size, double dNear, double dFar) {
		
		
		float[] colorComp  = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
		pointColor.getComponents(colorComp);
		VisPointsSimple points= new VisPointsSimple(colorComp, vertices, pointSize);
		points.draw( gl, pvm, screen_size, dNear, dFar);		
		VisPolyLineSimple lines;
		lineColor.getComponents(colorComp);
		for (int i=0;i<segments.size();i++)
		{
			lines = new VisPolyLineSimple(colorComp, segments.get(i), lineThickness);		
			lines.draw( gl, pvm);
		}
	}

	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
	}

	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());	
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

	@Override
	public void setLineThickness(float line_thickness) {

		lineThickness=line_thickness;
	}

	@Override
	public void setPointSize(float point_size) {

		pointSize=point_size;
	}
	
}


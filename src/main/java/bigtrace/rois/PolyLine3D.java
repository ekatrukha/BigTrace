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

public class PolyLine3D implements Roi3D, WritablePolyline
{
	
	public ArrayList<RealPoint> vertices;
	public VisPointsScaled verticesVis;
	public VisPolyLineSimple edgesVis;
	public float lineThickness;
	public float pointSize;
	public Color lineColor;
	public Color pointColor;
	public String name;
	public int type;
	
	public PolyLine3D(final float lineThickness_, final float pointSize_, final Color lineColor_, final Color pointColor_)
	{
		type = Roi3D.POLYLINE;
		lineThickness=lineThickness_;
		pointSize = pointSize_;
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());		
		vertices = new ArrayList<RealPoint>();
		verticesVis = new VisPointsScaled();
		verticesVis.setColor(pointColor_);
		verticesVis.setSize(pointSize_);
		edgesVis = new VisPolyLineSimple();
		edgesVis.setColor(lineColor_);
		edgesVis.setThickness(lineThickness_);

	}
	
	//adds a point to the "end" of polyline
	public void addPointToEnd(final RealPoint in_)
	{
		vertices.add(new RealPoint(in_));
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
		edgesVis.setVertices(vertices);		
		
	}


	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) {
		
		
		//float[] colorComp  = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
		//pointColor.getComponents(colorComp);
		//VisPointsScaled points= new VisPointsScaled(colorComp, vertices, pointSize);
		//VisPolyLineSimple lines;
		//lineColor.getComponents(colorComp);
		//lines = new VisPolyLineSimple(colorComp, vertices, lineThickness);
		//points.draw( gl, pvm, screen_size);
		//lines.draw( gl, pvm);
		
		verticesVis.draw(gl, pvm, screen_size);
		edgesVis.draw(gl, pvm);
		
	}

	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		edgesVis.setColor(lineColor);
	}

	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());
		verticesVis.setColor(pointColor);
	}
	@Override
	public void setLineThickness(float line_thickness) {

		lineThickness=line_thickness;
		edgesVis.setThickness(lineThickness);
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
	public float getLineThickness() {

		return lineThickness;
	}

	@Override
	public float getPointSize() {

		return pointSize;
	}


	
}


package bigtrace.rois;

import java.awt.Color;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPointsSimple;
import net.imglib2.RealPoint;

public class Point3D implements Roi3D {

	public RealPoint vertex;
	public float pointSize;
	public Color pointColor;
	public String name;
	public int type;
	
	public Point3D(final float pointSize_, final Color pointColor_, final RealPoint vertex_)
	{
		type = Roi3D.POINT;
		pointSize = pointSize_;		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());		
		vertex = new RealPoint(vertex_);
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

	@Override
	public void draw(GL3 gl, Matrix4fc pvm, double[] screen_size) 
	{
		float[] colorComp  = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
		pointColor.getComponents(colorComp);
		VisPointsScaled point= new VisPointsScaled(colorComp, vertex, pointSize);
		point.draw( gl, pvm, screen_size);
	}
	@Override
	public void setLineColor(Color lineColor_) {
		return;
	}

	@Override
	public void setPointColor(Color pointColor_) {

		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());
	}

	@Override
	public float getLineThickness() {
		return 0;
	}

	@Override
	public float getPointSize() {
		return pointSize;
	}

	@Override
	public void setLineThickness(float line_thickness) {
		return;
	}

	@Override
	public void setPointSize(float point_size) {
		this.pointSize=point_size;
	}

}

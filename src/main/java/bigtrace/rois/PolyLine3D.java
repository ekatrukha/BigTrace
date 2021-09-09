package bigtrace.rois;

import java.awt.Color;
import java.util.ArrayList;


import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsSimple;
import bigtrace.scene.VisPolyLineSimple;
import net.imglib2.RealPoint;

public class PolyLine3D implements Roi3D {
	
	public ArrayList<RealPoint> vertices;
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
	}
	
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



	@Override
	public void draw(GL3 gl, Matrix4fc pvm, double[] screen_size, double dNear, double dFar) {
		
		
		float[] colorComp  = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
		pointColor.getComponents(colorComp);
		VisPointsSimple points= new VisPointsSimple(colorComp, vertices, pointSize);
		VisPolyLineSimple lines;
		lineColor.getComponents(colorComp);
		lines = new VisPolyLineSimple(colorComp, vertices, lineThickness);
		points.draw( gl, pvm, screen_size, dNear, dFar);
		lines.draw( gl, pvm);
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
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public void setName(String name) {
		
		this.name = name;
	}
	
}


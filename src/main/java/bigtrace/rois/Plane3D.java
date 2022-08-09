package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.util.ArrayList;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineScaled;
import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

public class Plane3D extends AbstractRoi3D implements Roi3D {
	
	public ArrayList<RealPoint> vertices;
	public VisPointsScaled verticesVis;
	public VisPolyLineScaled edgesVis;


	public Plane3D(final Roi3DGroup preset_in)
	{
		type = Roi3D.PLANE;
		
		pointSize = preset_in.pointSize;
		lineThickness=preset_in.lineThickness;
		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());
		lineColor = new Color(preset_in.lineColor.getRed(),preset_in.lineColor.getGreen(),preset_in.lineColor.getBlue(),preset_in.lineColor.getAlpha());
				
		renderType = preset_in.renderType;
		nSectorN = preset_in.sectorN;
		
		
		vertices = new ArrayList<RealPoint>();
		verticesVis = new VisPointsScaled();
		verticesVis.setColor(pointColor);
		verticesVis.setSize(pointSize);
		edgesVis = new VisPolyLineScaled();
	
		edgesVis.bSmooth = false;
		edgesVis.setColor(lineColor);
		edgesVis.setThickness(lineThickness);
		edgesVis.setSectorN(nSectorN);
		edgesVis.setRenderType(renderType);
		name = "plane"+Integer.toString(this.hashCode());

	}
	/** adds initial vertex **/
	public void addPoint(final RealPoint in_)
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
	public boolean removePoint()
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

	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) {
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
	public void reversePoints() {
		// TODO Auto-generated method stub
		
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
	public void setRenderType(int nRenderType) 
	{
		
		renderType=nRenderType;
		
	}



	@Override
	public void saveRoi(FileWriter writer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGroup(Roi3DGroup preset_in) {
		
		setPointColor(preset_in.pointColor);
		setLineColor(preset_in.lineColor);

		setRenderType(preset_in.renderType);
		nSectorN = preset_in.sectorN;
		setPointSize(preset_in.pointSize);
		setLineThickness(preset_in.lineThickness);
		updateRenderVertices();
	}

	@Override
	public void updateRenderVertices() {
		
		verticesVis.setVertices(vertices);
		
	}

}

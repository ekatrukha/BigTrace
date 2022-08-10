package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Line3D;
import bigtrace.geometry.Plane3D;
import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineScaled;
import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

public class CrossSection3D extends AbstractRoi3D implements Roi3D {
	
	public ArrayList<RealPoint> vertices;
	public VisPointsScaled verticesVis;
	public VisPolyLineScaled edgesVis;
	public float [][] nDimBox;


	public CrossSection3D(final Roi3DGroup preset_in, final long [][] nDimIni_)
	{
		type = Roi3D.PLANE;
		
		pointSize = preset_in.pointSize;
		lineThickness=preset_in.lineThickness;
		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());
		lineColor = new Color(preset_in.lineColor.getRed(),preset_in.lineColor.getGreen(),preset_in.lineColor.getBlue(),preset_in.lineColor.getAlpha());
				
		//renderType = preset_in.renderType;
		renderType = VisPolyLineScaled.CENTER_LINE;
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
		nDimBox = new float [2][3];
		for(int i=0;i<2;i++)
			for(int j=0;j<3;j++)
				nDimBox[i][j]=(float)nDimIni_[i][j];

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
		//for cross-section wire and center line is the same for now
		//if(nRenderType==VisPolyLineScaled.WIRE)
	//	{
			renderType=VisPolyLineScaled.CENTER_LINE;
		//}
		//else
		//{
			renderType=nRenderType;
		//}
		edgesVis.setRenderType(renderType);
		updateRenderVertices();
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
		ArrayList< RealPoint > outline = new ArrayList< RealPoint >();
		if(vertices.size()>2)
		{
			double [] intersectionPoint = new double[3];
			//for now;
			Plane3D planeG = new Plane3D(vertices.get(0),vertices.get(1),vertices.get(2));
			ArrayList<ArrayList< RealPoint >> boxEdges = Box3D.getEdgesPairPoints(nDimBox);
			
			for(int i = 0;i<boxEdges.size();i++)
			{
		
				if(Intersections3D.planeEdgeIntersect(planeG,boxEdges.get(i).get(0),boxEdges.get(i).get(1),intersectionPoint))
				{
					outline.add(new RealPoint(intersectionPoint));
				}
			}
			if(outline.size()>1)
			{
				sortPolygonVertices(outline,planeG.n);
				outline.add(new RealPoint(outline.get(0)));
				edgesVis.setVertices(outline);
			}
		}
	}
	/** given a set of points (more than 2), assuming they form convex polygon,
	 * sorts them to form outline. Maybe better to use ConvexHull, but ok for now.
	 * Taken from https://www.asawicki.info/news_1428_finding_polygon_of_plane-aabb_intersection **/
	public static void sortPolygonVertices(final ArrayList< RealPoint > vertices, final double [] planeNormal )
	{
		double [] origin = vertices.get(0).positionAsDoubleArray();
		
		Comparator<RealPoint> compareRP = new Comparator<RealPoint> ()
		{
			  
		    @Override
		    public int compare(RealPoint rp1, RealPoint rp2)
		    {
		    	double [] v = new double [3];
		    	double [] lhs = new double [3];
		    	double [] rhs = new double [3];
		    	rp1.localize(lhs);
		    	rp2.localize(rhs);
		    	LinAlgHelpers.subtract(lhs, origin, lhs);
		    	LinAlgHelpers.subtract(rhs, origin, rhs);
		    	LinAlgHelpers.cross(lhs,rhs, v);
		    	if(LinAlgHelpers.dot(v, planeNormal)<0)
		    		return 1;
		    	else
		    		return -1;
		    }
		};
		Collections.sort(vertices,compareRP);
		
	}

}

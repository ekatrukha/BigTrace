package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JFrame;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Plane3D;
import bigtrace.gui.RangeSliderTF;
import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineScaled;
import bigtrace.scene.VisPolygonFlat;
import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

public class CrossSection3D extends AbstractRoi3D implements Roi3D {
	
	public ArrayList<RealPoint> vertices;
	public VisPointsScaled verticesVis;
	public VisPolygonFlat planeVis;
	public float [][] nDimBox;
	
	public Plane3D fittedPlane;


	public CrossSection3D(final Roi3DGroup preset_in, final long [][] nDimIni_)
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
		planeVis = new VisPolygonFlat();
	
	
		planeVis.setColor(lineColor);
		planeVis.setThickness(lineThickness);
		planeVis.setRenderType(renderType);
		name = "plane"+Integer.toString(this.hashCode());
		nDimBox = new float [2][3];
		for(int i=0;i<2;i++)
			for(int j=0;j<3;j++)
				nDimBox[i][j]=(float)nDimIni_[i][j];

	}
	/** adds initial vertex **/
	public void addPoint(final RealPoint in_)
	{
		if (vertices.size()==1)
		{
			//check if the second point is at the same place that the first 
			double [] dist = new double [3];
			LinAlgHelpers.subtract(vertices.get(0).positionAsDoubleArray(), in_.positionAsDoubleArray(), dist);
			if(LinAlgHelpers.length(dist)>0.000001)
			{
				vertices.add(new RealPoint(in_));
			}
		}
		else
		{
			//check that first three points do not lay on the line, 
			// so we can build a plane
			if(vertices.size()==2)
			{
				double [] dist1 = new double [3];
				double [] dist2 = new double [3];
				double [] cross = new double [3];
				
				LinAlgHelpers.subtract(vertices.get(1).positionAsDoubleArray(), vertices.get(0).positionAsDoubleArray(), dist1);
				LinAlgHelpers.subtract(in_.positionAsDoubleArray(),vertices.get(1).positionAsDoubleArray(), dist2);
				LinAlgHelpers.cross(dist1, dist2, cross);
				if(LinAlgHelpers.length(cross)>0.000001)
				{
					vertices.add(new RealPoint(in_));
				}
			}
			else
			{
				vertices.add(new RealPoint(in_));
			}
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
		planeVis.draw(gl, pvm);
		
	}


	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());
		verticesVis.setColor(pointColor);
	}
	
	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		planeVis.setColor(lineColor);
	}


	@Override
	public void reversePoints() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setLineThickness(float line_thickness) {

		lineThickness=line_thickness;
		planeVis.setThickness(lineThickness);
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
		planeVis.setRenderType(renderType);
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
			fittedPlane = new Plane3D(vertices.get(0),vertices.get(1),vertices.get(2));
			fittedPlane = fitPlane(vertices);
			ArrayList<ArrayList< RealPoint >> boxEdges = Box3D.getEdgesPairPoints(nDimBox);
			
			for(int i = 0;i<boxEdges.size();i++)
			{
		
				if(Intersections3D.planeEdgeIntersect(fittedPlane,boxEdges.get(i).get(0),boxEdges.get(i).get(1),intersectionPoint))
				{
					outline.add(new RealPoint(intersectionPoint));
				}
			}
			if(outline.size()>1)
			{
				sortPolygonVertices(outline,fittedPlane.n);
				//outline.add(new RealPoint(outline.get(0)));
				planeVis.setVertices(outline);
			}
		}
		else
		{
			planeVis.setVertices(new ArrayList<RealPoint>());
		}
	}
	/** given a set of points (more than 2) 
	 * 1) all lying in the same plane (with its normal vector in planeNormal); 
	 * 2) assuming they form convex polygon,
	 * sorts them to form outline. 
	 * Maybe better to use ConvexHull, but ok for now.
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
	private static Plane3D fitPlane(final ArrayList<RealPoint> vertices)
	{
		RealPoint centroidRP = centroid(vertices);
			
		double [][] matrixD = new double [vertices.size()][3];
		int d;
		//subtract centroid
		for(int i=0;i<vertices.size();i++)
			for(d=0;d<3;d++)
			{
				matrixD[i][d]=vertices.get(i).getDoublePosition(d)-centroidRP.getDoublePosition(d);
			}
		Matrix A = new Matrix(matrixD);
		SingularValueDecomposition svd = new SingularValueDecomposition(A);		
		matrixD = svd.getV().getArray();
		RealPoint normalRP = new RealPoint(3);
		for(d=0;d<3;d++)
		{
			normalRP.setPosition(matrixD[d][2], d);
		}
		Plane3D out = new Plane3D();
		
		out.setVectors(centroidRP.positionAsDoubleArray(), normalRP.positionAsDoubleArray());
		
		return out;
		
		
	}
	public static RealPoint centroid(final ArrayList<RealPoint> vertices)
	{
		if(vertices.size()<1)
			return null;
		final int nDim=vertices.get(0).numDimensions();
		int d;
		double [] meanV = new double [nDim];
		for(int i=0;i<vertices.size();i++)
		{
			for(d=0;d<nDim;d++)
			{
				meanV[d]+=vertices.get(i).getDoublePosition(d);
			}
		}
		for(d=0;d<nDim;d++)
		{
			meanV[d]/=(double)nDim;
		
		}
		return new RealPoint(meanV);
	}
	/*
	public static void main(String[] args) 
	{
		ArrayList<RealPoint> vertices = new ArrayList<RealPoint>();
		for(int i=0;i<4;i++)
		{
			RealPoint rp = new RealPoint(3);
			for(int d=0;d<3;d++)
			{
				rp.setPosition(5*i*(i-d)+d, d);				
			}
			vertices.add(rp);
		}
		Plane3D fitplane = fitPlane(vertices);
		int i=10;
		i++;
	}
	*/

}

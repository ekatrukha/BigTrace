package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import org.joml.Matrix4fc;
import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineSimple;
import net.imglib2.RealPoint;

public class Box3D extends AbstractRoi3D implements Roi3D {

	public ArrayList<RealPoint> vertices;
	public ArrayList<ArrayList<RealPoint>> edges;
	public ArrayList<VisPointsScaled> verticesVis;
	public ArrayList<VisPolyLineSimple> edgesVis;

	
	public Box3D(final Roi3DGroup preset_in)
	{
		type = Roi3D.BOX;

		pointSize = preset_in.pointSize;
		lineThickness=preset_in.lineThickness;
		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());
		lineColor = new Color(preset_in.lineColor.getRed(),preset_in.lineColor.getGreen(),preset_in.lineColor.getBlue(),preset_in.lineColor.getAlpha());

	}
	public Box3D(float [][] nDimBox, final float lineThickness_, final float pointSize_, final Color lineColor_, final Color pointColor_)
	{
		type = Roi3D.BOX;
		lineThickness=lineThickness_;
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		verticesVis = new ArrayList<VisPointsScaled>();
		edgesVis = new ArrayList<VisPolyLineSimple>();
		int i;
		
		ArrayList<ArrayList< RealPoint >> edgesPairPoints = getEdgesPairPoints(nDimBox);
		for(i=0;i<edgesPairPoints.size(); i++)
		{
			edgesVis.add(new VisPolyLineSimple(edgesPairPoints.get(i), lineThickness,lineColor));
		}
	}
	
	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) {
	
		for (int i=0;i<edgesVis.size();i++)
		{
			edgesVis.get(i).draw(gl, pvm);
		}
	}
	
	
	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());	
		for(int i =0; i<verticesVis.size();i++)
		{
			verticesVis.get(i).setColor(pointColor);
		}
	}

	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		for(int i =0; i<edgesVis.size();i++)
		{
			edgesVis.get(i).setColor(lineColor);
		}
	}
	

	@Override
	public void setLineThickness(float line_thickness) {

		lineThickness=line_thickness;
		for(int i =0; i<edgesVis.size();i++)
		{
			edgesVis.get(i).setThickness(lineThickness);
		}
	}
	
	@Override
	public void setPointSize(float point_size) {
	
		pointSize=point_size;
	}
	
	@Override
	public void setRenderType(int nRenderType){
		return;
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
			writer.write("RenderType,"+ Integer.toString(this.getRenderType()));
			
			writer.write("Vertices,"+Integer.toString(vertices.size())+"\n");
			vert = new float[3];
			for (iPoint = 0;iPoint<vertices.size();i++)
			{ 
				vertices.get(iPoint).localize(vert);
				for(i=0;i<3;i++)
				{
					writer.write(df3.format(vert[i])+",");
				}
				writer.write("\n");
			}
		}
		catch (IOException e) {	
			System.err.print(e.getMessage());
			
		}
	}
	@Override
	public void reversePoints() {
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

	@Override
	public void updateRenderVertices() {
		// TODO Auto-generated method stub
		
	}
	/** returns array of paired coordinates for each edge of the box,
	 * specified by nDimBox[0] - one corner, nDimBox[1] - opposite corner.
	 * no checks on provided coordinates performed  **/
	public static ArrayList<ArrayList< RealPoint >> getEdgesPairPoints(final float [][] nDimBox)
	{
		int i,j,z;
		ArrayList<ArrayList< RealPoint >> out = new ArrayList<ArrayList< RealPoint >>();
		int [][] edgesxy = new int [5][2];
		edgesxy[0]=new int[]{0,0};
		edgesxy[1]=new int[]{1,0};
		edgesxy[2]=new int[]{1,1};
		edgesxy[3]=new int[]{0,1};
		edgesxy[4]=new int[]{0,0};
		//draw front and back
		RealPoint vertex1=new RealPoint(0,0,0);
		RealPoint vertex2=new RealPoint(0,0,0);
		for (z=0;z<2;z++)
		{
			for (i=0;i<4;i++)
			{
				for (j=0;j<2;j++)
				{
					vertex1.setPosition(nDimBox[edgesxy[i][j]][j], j);
					vertex2.setPosition(nDimBox[edgesxy[i+1][j]][j], j);
				}
				//z coord
				vertex1.setPosition(nDimBox[z][2], 2);
				vertex2.setPosition(nDimBox[z][2], 2);
				
				ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();
				point_coords.add(new RealPoint(vertex1));
				point_coords.add(new RealPoint(vertex2));

				out.add(point_coords);

			}
		}
		//draw the rest 4 edges

		for (i=0;i<4;i++)
		{
			for (j=0;j<2;j++)
			{
				vertex1.setPosition(nDimBox[edgesxy[i][j]][j], j);
				vertex2.setPosition(nDimBox[edgesxy[i][j]][j], j);
			}
			//z coord
			vertex1.setPosition(nDimBox[0][2], 2);
			vertex2.setPosition(nDimBox[1][2], 2);
			ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();

			point_coords.add(new RealPoint(vertex1));
			point_coords.add(new RealPoint(vertex2));
			out.add(point_coords);
	
		}	
		return out;
	}
}
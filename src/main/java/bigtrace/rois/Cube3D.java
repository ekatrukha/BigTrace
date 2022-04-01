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

public class Cube3D implements Roi3D {

	public ArrayList<RealPoint> vertices;
	public ArrayList<ArrayList<RealPoint>> edges;
	public ArrayList<VisPointsScaled> verticesVis;
	public ArrayList<VisPolyLineSimple> edgesVis;
	private float lineThickness;
	private float pointSize;
	private Color lineColor;
	private Color pointColor;
	private String name;
	private int type;
	
	
	public Cube3D(float [][] nDimBox, final float lineThickness_, final float pointSize_, final Color lineColor_, final Color pointColor_)
	{
		lineThickness=lineThickness_;
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		verticesVis = new ArrayList<VisPointsScaled>();
		edgesVis = new ArrayList<VisPolyLineSimple>();
		int i,j,z;
		//float [] vbox_color = new float[]{0.4f,0.4f,0.4f};
		//float vbox_thickness = 0.5f;
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

				edgesVis.add(new VisPolyLineSimple(point_coords, lineThickness,lineColor));

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

			edgesVis.add(new VisPolyLineSimple(point_coords, lineThickness,lineColor));
	
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
	public void setPointColorRGB(Color pointColor_){
		setPointColor(new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor.getAlpha()));
	}
	
	@Override
	public void setLineColorRGB(Color lineColor_){
		setLineColor(new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor.getAlpha()));
	}
	@Override
	public Color getPointColor()
	{
		return new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),pointColor.getAlpha());
	}
	
	@Override
	public Color getLineColor()
	{
		return new Color(lineColor.getRed(),lineColor.getGreen(),lineColor.getBlue(),lineColor.getAlpha());
	}
	
	@Override
	public void setOpacity(float fOpacity)
	{
		setPointColor(new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),(int)(fOpacity*255)));
		setLineColor(new Color(lineColor.getRed(),lineColor.getGreen(),lineColor.getBlue(),(int)(fOpacity*255)));
	}
	
	@Override
	public float getOpacity()
	{
		return ((float)(pointColor.getAlpha())/255.0f);
	}
	
	@Override
	public float getLineThickness() {
		return lineThickness;
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
	public float getPointSize() {
		return pointSize;
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
	public int getRenderType(){
		return 0;
	}
	
	@Override
	public int getType() {
		return type;
	}

	@Override
	public String getName() {
		return new String(name);
	}

	@Override
	public void setName(String name) {
		this.name = new String(name);		
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


}

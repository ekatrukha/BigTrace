package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsScaled;
import net.imglib2.RealPoint;

public class Point3D implements Roi3D {

	public RealPoint vertex;
	public VisPointsScaled vertexVis;
	public float pointSize;
	public Color pointColor;
	public String name;
	public int type;
	private int groupIndex=-1;

	//redundant?
	/*public Point3D(final RealPoint vertex_, final float pointSize_, final Color pointColor_)
	{
		type = Roi3D.POINT;
		pointSize = pointSize_;		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());		
		vertex = new RealPoint(vertex_);
		vertexVis = new VisPointsScaled(vertex_,pointSize_,pointColor_);
		name = "point"+Integer.toString(this.hashCode());

	}*/
	/*public Point3D(final RealPoint vertex_, final Roi3DGroup preset_in)
	{
		type = Roi3D.POINT;
		pointSize = preset_in.pointSize;		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());		
		vertex = new RealPoint(vertex_);
		vertexVis = new VisPointsScaled(vertex_,pointSize,pointColor);
		name = "point"+Integer.toString(this.hashCode());
		
	}*/
	public Point3D( final Roi3DGroup preset_in)
	{
		type = Roi3D.POINT;
		pointSize = preset_in.pointSize;		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());		
		//vertex = new RealPoint(vertex_);
		//vertexVis = new VisPointsScaled(vertex_,pointSize,pointColor);
		name = "point"+Integer.toString(this.hashCode());
		vertex = null;
		vertexVis = null;
	}
	public Point3D(final float pointSize_, final Color pointColor_)
	{
		type = Roi3D.POINT;
		pointSize = pointSize_;		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());		
		name = "point"+Integer.toString(this.hashCode());

	}
	
	public void setVertex(final RealPoint vertex_)
	{
		vertex = new RealPoint(vertex_);
		vertexVis = new VisPointsScaled(vertex_,pointSize,pointColor);
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
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) 
	{
		if(vertexVis!=null)
			vertexVis.draw( gl, pvm, screen_size);
	}


	@Override
	public void setPointColor(Color pointColor_) {

		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());
		vertexVis.setColor(pointColor);
	}
	
	@Override
	public void setPointColorRGB(Color pointColor_){
		setPointColor(new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor.getAlpha()));
	}
	
	@Override
	public void setLineColor(Color lineColor_) {
		return;
	}
	
	@Override
	public void setLineColorRGB(Color lineColor_) {
		return;
	}
	@Override
	public Color getPointColor()
	{
		return new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),pointColor.getAlpha());
	}
	
	@Override
	public Color getLineColor()
	{
		return new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),pointColor.getAlpha());
	}
	
	@Override
	public void setOpacity(float fOpacity)
	{
		setPointColor(new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),(int)(fOpacity*255)));
	}
	@Override
	public float getOpacity()
	{
		return ((float)(pointColor.getAlpha())/255.0f);
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
		vertexVis.setSize(pointSize);
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
	public void saveRoi(final FileWriter writer)
	{
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
			writer.write("Vertices,1\n");
			float [] vert = new float[3];
			vertex.localize(vert);
			for(int i=0;i<3;i++)
			{
				writer.write(df3.format(vert[i])+",");
			}
			//time point
			writer.write("0.0\n");
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
		// TODO Auto-generated method stub
		pointSize = preset_in.pointSize;		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());		
		if(vertex!=null)
			setVertex(vertex);
	}
	@Override
	public void setGroupInd(final int nGIndex)
	{
		groupIndex = nGIndex;
	}
	@Override
	public int getGroupInd()
	{
		return groupIndex;
	}
}

package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.util.ArrayList;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import net.imglib2.RealPoint;

public interface Roi3D 
{

	public static final int POINT=0, POLYLINE=1, LINE_TRACE=2, CUBE=3; // Types
	
	/** returns ROI type**/
	public int getType();
	
	/** Returns the name of this ROI, or null. */
	public String getName();

	/** Sets the name of this ROI. */
	public void setName(String name);
	
	/** Draws ROI into the volume **/
	public void draw( GL3 gl, Matrix4fc pvm,  final int [] screen_size);
	
	public void setLineColorRGB(Color lineColor_);
	public void setPointColorRGB(Color pointColor_);
	public void setLineColor(Color lineColor_);
	public void setPointColor(Color pointColor_);
	public void reversePoints();
	
	public Color getLineColor();
	public Color getPointColor();	
	
	public void setOpacity(float fOpacity);
	public float getOpacity();

	public void setLineThickness(final float line_thickness);
	public float getLineThickness();
	
	public void setPointSize(final float point_size);
	public float getPointSize();

	public void setRenderType(int nRenderType);
	public int getRenderType();
	public void saveRoi(final FileWriter writer);
	
	public void setGroup(final Roi3DGroup preset_in);
	public void setGroupInd(final int nGIndex);
	public int getGroupInd();
	
	public static String intTypeToString(int nType)
	{
		String sType = "Point";
		switch (nType)
		{
		 case POINT:
			 sType= "Point";
			 break;
		 case POLYLINE:
			 sType= "Polyline";
			 break;
		 case LINE_TRACE:
			 sType = "LineTrace";
			 break;
		 case CUBE:
			 sType = "Cube";
			 break;
		
		}
		return sType;
	}
	public static int stringTypeToInt(String sType)
	{
		if(sType.equals("Point"))
		{ return POINT;};
		if(sType.equals("Polyline"))
		{ return POLYLINE;};
		if(sType.equals("LineTrace"))
		{ return LINE_TRACE;};
		if(sType.equals("Cube"))
		{ return CUBE;};
		
		return -1;
	}
	public static ArrayList<RealPoint> reverseArrayRP(final ArrayList<RealPoint> vert_in) 
	{
		ArrayList<RealPoint> reversed = new ArrayList<RealPoint>();
		
		for(int i=vert_in.size()-1;i>=0;i--)
		{
			reversed.add(new RealPoint(vert_in.get(i)));
		}
		return reversed; 
	}
}

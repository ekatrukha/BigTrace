package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.util.ArrayList;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

public interface Roi3D 
{

	public static final int POINT=0, POLYLINE=1, LINE_TRACE=2, PLANE=3, CUBE=4; // Types
	
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
	public void updateRenderVertices();
	

	
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
		 case PLANE:
			 sType = "Plane";
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
		if(sType.equals("Plane"))
		{ return PLANE;};
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
	/**
	 * transforms realpoint in pixel coordinates to space units
	 * **/
	public static RealPoint scaleGlob(final RealPoint in, final double [] globCal)
	{
		RealPoint out = new RealPoint(in);
		for (int i=0;i<in.numDimensions();i++)
		{
			out.setPosition(out.getDoublePosition(i)*globCal[i],i);
		}
		return out;
	}
	/**
	 * transforms realpoint in space units to pixels(voxels) coordinates 
	 * **/
	public static RealPoint scaleGlobInv(final RealPoint in, final double [] globCal)
	{
		RealPoint out = new RealPoint(in);
		for (int i=0;i<in.numDimensions();i++)
		{
			out.setPosition(out.getDoublePosition(i)/globCal[i],i);
		}
		return out;
	}
	/**
	 * transforms double array of pixel coordinates to space units
	 * **/
	public static double [] scaleGlob(final double [] in, final double [] globCal)
	{
		double [] out = new double [in.length];
		for (int i=0;i<in.length;i++)
		{
			out[i]=in[i]*globCal[i];
		}
		return out;
	}
	/**
	 * transforms double array of coordinates in space units to pixels
	 * **/
	public static double [] scaleGlobInv(final double [] in, final double [] globCal)
	{
		double [] out = new double [in.length];
		for (int i=0;i<in.length;i++)
		{
			out[i]=in[i]/globCal[i];
		}
		return out;
	}
	/** calculates cumulative length between vert_in 3D points using globCal calibration **/
	public static double getSegmentLength(final ArrayList<RealPoint> vert_in, double [] globCal)
	{
		double length=0.0;
		double [] pos1 = new double [3];
		double [] pos2 = new double [3];
		
		if (vert_in==null)
			{return 0.0;}
		for (int i=0;i<vert_in.size()-1; i++)
		{
			vert_in.get(i).localize(pos1);
			vert_in.get(i+1).localize(pos2);
			for (int j = 0; j<3; j++)
			{
				pos1[j]*=globCal[j];
				pos2[j]*=globCal[j];
			}
			length+=LinAlgHelpers.distance(pos1, pos2);
		}
		//System.out.print(length);
		return length;
	}

	/**
	 * transforms realpoint in pixel coordinates to space units
	 * **/
	public static RealPoint getNaNPoint()
	{
		RealPoint out = new RealPoint(3);
		for (int i = 0;i<3; i++)
			out.setPosition(Double.NaN, i);
		return out;
	}
	

}

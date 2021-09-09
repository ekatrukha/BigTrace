package bigtrace.rois;

import java.awt.Color;
import java.util.ArrayList;


import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import net.imglib2.RealPoint;



public class RoiManager3D {
	
	 public static final int ADD_POINT=0, ADD_POINT_LINE=1;
	 public ArrayList<Roi3D> rois =  new ArrayList<Roi3D >();
	 public int activeRoi = -1;
	 public Color activeLineColor = Color.RED;
	 public Color nonActiveLineColor = Color.BLUE;
	 public Color activePointColor = Color.YELLOW;
	 public Color nonActivePointColor = Color.GREEN;
	/* public Color activeLineColor = Color.WHITE;
	 public Color nonActiveLineColor = Color.WHITE;
	 public Color activePointColor = Color.WHITE;
	 public Color nonActivePointColor = Color.WHITE;*/
	 public int mode;
	 public float currLineThickness = 15.0f;
	 public float currPointSize = 40.0f;
		
	 public RoiManager3D()
	 {
		 mode = ADD_POINT_LINE;
	 }
	 
	 public void addRoi(Roi3D roi_in)
	 {
		 rois.add(roi_in);
		 activeRoi = rois.size()-1;
	 }
	 
	 public void removeRoi(int roiIndex)
	 {
		 if(roiIndex<rois.size())
		 {
			 rois.remove(roiIndex);
			 activeRoi = -1;
		 }
	 }
	 
	 public void removeAll()
	 {
		 rois =  new ArrayList<Roi3D >();
	 }
	 
	 /** Draw all ROIS **/
	 public void draw(GL3 gl, Matrix4fc pvm, double[] screen_size, double dNear, double dFar)
	 {
	       Roi3D roi;
	       int i;
	       for (i=0;i<rois.size();i++) 
	       {
	    	   roi=rois.get(i);
	    	   if(i==activeRoi)
	    	   {
	    		   roi.setPointColor(activePointColor);
	    		   roi.setLineColor(activeLineColor);
	    	   }
	    	   else
	    	   {
	    		   roi.setPointColor(nonActivePointColor);
	    		   roi.setLineColor(nonActiveLineColor);	    		   
	    	   }
	           roi.draw(gl, pvm, screen_size, dNear, dFar);
	       }
	 }
	 /** adds point to active polyline
	  *  if active ROI is not a polyline, does nothing
	  *  if there are no active ROIS, starts new polyline **/
	 public void addPointToLine(RealPoint point_)
	 {
		 PolyLine3D polyline;
		 //new Line
		 if(activeRoi<0)
		 {
			 polyline = new PolyLine3D(currLineThickness, currPointSize, activeLineColor, activePointColor);
			 polyline.addPointToEnd(point_);
			 rois.add(polyline);
			 activeRoi = rois.size()-1; 
			 return;
		 }
		 //active ROI is not a line
		 if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 return;
		 }
		 //add point to line
		 else
		 {
			 polyline = (PolyLine3D) rois.get(activeRoi);
			 polyline.addPointToEnd(point_);
		 }
	 
	 }
	 /** removes point from the active polyline
	  *  if active ROI is not a polyline, does nothing
	  *  if it is a last point, removes polyline object
	  *  and activates previous Roi in the list (if any) 
	  * **/
	 public void removePointFromLine()
	 {
		 PolyLine3D polyline;
		 if(activeRoi<0)
			 return;
		 //active ROI is not a line or none ROI selected
		 if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 return;
		 }
		 else
		 {
			 polyline = (PolyLine3D) rois.get(activeRoi);
			 if(!polyline.removeEndPoint())
			 {
				 rois.remove(activeRoi);				 
				 activeRoi--;
				 if(activeRoi>=0)
				 {
					 if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
						 activeRoi=-1;
					 
				 }
			 }
		 }
	 }
}

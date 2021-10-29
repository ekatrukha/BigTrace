package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPolyLineScaled;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.real.WritablePolyline;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

public class LineTrace3D implements Roi3D, WritablePolyline
{
	
	public ArrayList<RealPoint> vertices;
	public ArrayList<ArrayList<RealPoint>> segments;
	public VisPointsScaled verticesVis;
	public ArrayList<VisPolyLineScaled> segmentsVis;
	public float lineThickness;
	public float pointSize;
	public Color lineColor;
	public Color pointColor;
	public int nSectorN;
	public String name;
	public int type;
	public int renderType;
	
	public LineTrace3D(final float lineThickness_, final float pointSize_, final Color lineColor_, final Color pointColor_, final int nRenderType, final int nSectorN_)
	{
		type = Roi3D.LINE_TRACE;
		lineThickness=lineThickness_;
		pointSize = pointSize_;
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());		
		vertices = new ArrayList<RealPoint>();
		segments = new ArrayList<ArrayList<RealPoint>>();
		verticesVis = new VisPointsScaled();
		verticesVis.setColor(pointColor_);
		verticesVis.setSize(pointSize_);
		segmentsVis = new ArrayList<VisPolyLineScaled>();
		renderType= nRenderType;
		nSectorN = nSectorN_;
		name = "trace"+Integer.toString(this.hashCode());

	}
	/** adds initial vertex **/
	public void addFirstPoint(final RealPoint in_)
	{
		vertices.add(new RealPoint(in_));
		verticesVis.setVertices(vertices);
	}
	
	public void addPointAndSegment(final RealPoint in_, final ArrayList<RealPoint> segments_)
	{
		vertices.add(new RealPoint(in_));
		verticesVis.setVertices(vertices);
		segments.add(segments_);
		segmentsVis.add(new VisPolyLineScaled(segments_,lineThickness, lineColor, nSectorN, renderType));
	}
	
	/** removes last segment of the tracing.
	 * if there was just one spot, returns false**/
	public boolean removeLastSegment() 
	{
		
		vertices.remove(vertices.size()-1);
		verticesVis.setVertices(vertices);
		if(vertices.size()>0)
		{
			segments.remove(segments.size()-1);
			segmentsVis.remove(segmentsVis.size()-1);
			return true;
		}
		else
		{
			return false;
		}
		
	}
	
	/** returns the last segment of the tracing.**/
	public ArrayList<RealPoint> getLastSegment() 
	{
		
		return segments.get(segments.size()-1);
		
	}

	/*
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

*/

	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) {
		
	
		verticesVis.draw(gl, pvm, screen_size);
		
		for (VisPolyLineScaled segment : segmentsVis)
		{
			segment.draw(gl, pvm);
		}

	}
	
	@Override
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());	
		verticesVis.setColor(pointColor);
	}
	
	@Override
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
		
		for (VisPolyLineScaled segment : segmentsVis)
		{
			segment.setColor(lineColor);
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
	public void setPointSize(float point_size) {

		pointSize=point_size;
		verticesVis.setSize(pointSize);
	}
	
	@Override
	public float getPointSize() {

		return pointSize;
	}
	
	@Override
	public void setLineThickness(float line_thickness) {


		lineThickness=line_thickness;
		for (int i=0;i<segmentsVis.size();i++)
		{
			segmentsVis.get(i).setThickness(lineThickness);
			segmentsVis.get(i).setVertices(segments.get(i));
		}
	}
	
	@Override
	public float getLineThickness() {

		return lineThickness;
	}

	
	@Override
	public void setRenderType(int nRenderType){
		
		
		renderType=nRenderType;
		for (int i=0;i<segmentsVis.size();i++)
		{
			segmentsVis.get(i).setRenderType(renderType);
			segmentsVis.get(i).setVertices(segments.get(i));
		}

	}	
	
	@Override
	public int getRenderType(){
		return renderType;
	}

	@Override
	public int getType() {
		
		return type;
	}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	
	/** Methods from imglib2 Polyline, 
	 * I do not really understand them yet, 
	 * but added for the future and implemented them
	 * to the best of my knowledge (and so they do not produce errors)
	 */
	
	public int numVertices() {
		return vertices.size();
	}
	
	
	public int numSegments() {
		return segments.size();
	}

	@Override
	public RealMaskRealInterval and(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.and(this, paramPredicate);
	}

	@Override
	public RealMaskRealInterval minus(Predicate<? super RealLocalizable> paramPredicate) {
		
		// TODO Auto-generated method stub
		return Masks.minus(this, paramPredicate);
	}

	@Override
	public RealMask negate() {
		// TODO Auto-generated method stub
		return Masks.negate(this);
	}

	@Override
	public RealMask or(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.or(this, paramPredicate);
	}

	@Override
	public RealMask xor(Predicate<? super RealLocalizable> paramPredicate) {
		// TODO Auto-generated method stub
		return Masks.xor(this, paramPredicate);
	}

	@Override
	public boolean test(RealLocalizable arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int numDimensions() {
		// TODO Auto-generated method stub
		return 3;
	}

	@Override
	public double realMin(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double realMax(int d) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RealLocalizableRealPositionable vertex(int paramInt) {
		// TODO Auto-generated method stub
		return (RealLocalizableRealPositionable) vertices.get(paramInt);
	}

	@Override
	public void addVertex(int paramInt, RealLocalizable paramRealLocalizable) {
		// TODO Auto-generated method stub
		vertices.add((RealPoint) paramRealLocalizable);
	}

	@Override
	public void removeVertex(int paramInt) {
		// TODO Auto-generated method stub
		vertices.remove(paramInt);
	}

	@Override
	public void addVertices(int paramInt, Collection<RealLocalizable> paramCollection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveRoi(final FileWriter writer)
	{
		int i, iPoint, iSegment;
		float [] vert;
		ArrayList<RealPoint> segment;
		
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
			writer.write("RenderType,"+ Integer.toString(this.getRenderType())+"\n");
			writer.write("SectorN,"+ Integer.toString(this.nSectorN)+"\n");
			
			writer.write("Vertices,"+Integer.toString(vertices.size())+"\n");
			vert = new float[3];
			for (iPoint = 0;iPoint<vertices.size();iPoint++)
			{ 
				vertices.get(iPoint).localize(vert);
				for(i=0;i<3;i++)
				{
					writer.write(df3.format(vert[i])+",");
				}
				//time point
				writer.write("0.0\n");
			}
			writer.write("SegmentsNumber,"+Integer.toString(segments.size())+"\n");
			for(iSegment=0;iSegment<segments.size();iSegment++)
			{
				segment=segments.get(iSegment);
				writer.write("Segment,"+Integer.toString(iSegment+1)+",Points,"+Integer.toString(segment.size())+"\n");
				for (iPoint = 0;iPoint<segment.size();iPoint++)
				{ 
					segment.get(iPoint).localize(vert);
					for(i=0;i<3;i++)
					{
						writer.write(df3.format(vert[i])+",");
					}
					//time point
					writer.write("0.0\n");
				}
			}
		}
		catch (IOException e) {	
			System.err.print(e.getMessage());
			
		}
	}




	
}

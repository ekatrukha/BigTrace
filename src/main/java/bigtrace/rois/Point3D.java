package bigtrace.rois;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import org.joml.Matrix4fc;

import com.jogamp.opengl.GL3;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.geometry.Line3D;
import bigtrace.measure.MeasureValues;
import bigtrace.measure.Sphere3DMeasure;
import bigtrace.scene.VisPointsScaled;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class Point3D extends AbstractRoi3D implements Roi3D {

	public RealPoint vertex;
	public VisPointsScaled vertexVis;


	public Point3D( final Roi3DGroup preset_in)
	{
		type = Roi3D.POINT;
		pointSize = preset_in.pointSize;		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());		
		lineColor = new Color(preset_in.lineColor.getRed(),preset_in.lineColor.getGreen(),preset_in.lineColor.getBlue(),preset_in.lineColor.getAlpha());

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
		lineColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());

		name = "point"+Integer.toString(this.hashCode());

	}
	
	public void setVertex(final RealPoint vertex_)
	{
		vertex = new RealPoint(vertex_);
		vertexVis = new VisPointsScaled(vertex_,pointSize,pointColor);
	}
	

	@Override
	public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size) 
	{
		if(vertexVis!=null)
			vertexVis.draw( gl, pvm, screen_size);
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

		pointSize = preset_in.pointSize;		
		pointColor = new Color(preset_in.pointColor.getRed(),preset_in.pointColor.getGreen(),preset_in.pointColor.getBlue(),preset_in.pointColor.getAlpha());		
		if(vertex!=null)
			setVertex(vertex);
	}

	public void getEnds(final MeasureValues val, final double [] globCal)
	{
		val.ends = new RealPoint [2];
		val.ends[0]= new RealPoint(Roi3D.scaleGlob(vertex,globCal));
	
		val.ends[1] = Roi3D.getNaNPoint();
		return;
	}
	
	/** get intensity values in Sphere around the point **/
	public < T extends RealType< T > > double[] getIntensityValues(final IntervalView<T> source, final InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory)
	{
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(source),nInterpolatorFactory);
		RealRandomAccess<T> ra =   interpolate.realRandomAccess();
		
		final double dMinVoxelSize = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]),BigTraceData.globCal[2]);
		ArrayList<Double> intVals = new ArrayList<Double>();
		Sphere3DMeasure measureSphere = new Sphere3DMeasure();
		measureSphere.setRadius((int)(0.5*Math.floor(pointSize)));
		double [] current_pixel = new double [3];
		double [] center = new double [3];
		vertex.localize(center);
		center =Roi3D.scaleGlob(center, BigTraceData.globCal);
		measureSphere.cursorSphere.reset();
		while (measureSphere.cursorSphere.hasNext())
		{
			measureSphere.cursorSphere.fwd();
			measureSphere.cursorSphere.localize(current_pixel);
			LinAlgHelpers.scale(current_pixel, dMinVoxelSize, current_pixel);
			LinAlgHelpers.add(center, current_pixel, current_pixel);
			current_pixel= Roi3D.scaleGlobInv(current_pixel, BigTraceData.globCal);
			ra.setPosition(current_pixel);
			intVals.add(ra.get().getRealDouble());
		}
		double [] out = new double[intVals.size()];
		for (int i =0;i<intVals.size();i++)
		{
			out[i]=intVals.get(i).doubleValue();
		}
		return out;
	}
	
	/** get intensity values in Sphere around the point **/
	public < T extends RealType< T > > double[] getIntensityValuesTEST(BigTrace bt, final IntervalView<T> source, final InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory)
	{
		RealRandomAccessible<T> interpolate = Views.interpolate(Views.extendZero(source),nInterpolatorFactory);
		RealRandomAccess<T> ra =   interpolate.realRandomAccess();
		
		final double dMinVoxelSize = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]),BigTraceData.globCal[2]);
		ArrayList<Double> intVals = new ArrayList<Double>();
		Sphere3DMeasure measureSphere = new Sphere3DMeasure();
		measureSphere.setRadius((int)(0.5*Math.floor(pointSize)));
		double [] current_pixel = new double [3];
		double [] center = new double [3];
		vertex.localize(center);
		center =Roi3D.scaleGlob(center, BigTraceData.globCal);
		measureSphere.cursorSphere.reset();
		while (measureSphere.cursorSphere.hasNext())
		{
			measureSphere.cursorSphere.fwd();
			measureSphere.cursorSphere.localize(current_pixel);
			LinAlgHelpers.scale(current_pixel, dMinVoxelSize, current_pixel);
			LinAlgHelpers.add(center, current_pixel, current_pixel);
			current_pixel= Roi3D.scaleGlobInv(current_pixel, BigTraceData.globCal);
			bt.roiManager.addPoint3D(new RealPoint(current_pixel));
			ra.setPosition(current_pixel);
			intVals.add(ra.get().getRealDouble());
		}
		double [] out = new double[intVals.size()];
		for (int i =0;i<intVals.size();i++)
		{
			out[i]=intVals.get(i).doubleValue();
		}
		return out;
	}
	
	@Override
	public void updateRenderVertices() 
	{
		
		
	}
	@Override
	public void setLineColor(Color lineColor_) {
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
	}
	@Override
	public void setPointColor(Color pointColor_) {

		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());
		vertexVis.setColor(pointColor);
	}
	
	@Override
	public void setLineThickness(float line_thickness) {
		
		lineThickness=line_thickness;
		return;
	}
	@Override
	public double getMinDist(Line3D line) {
		// TODO Auto-generated method stub
		return Line3D.distancePointLine(vertex, line);
	}
}

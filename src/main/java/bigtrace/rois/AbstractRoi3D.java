package bigtrace.rois;

import java.awt.Color;
import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;

public abstract class AbstractRoi3D implements Roi3D {
	
	public float lineThickness;
	public float pointSize;
	public Color lineColor;
	public Color pointColor;
	public String name;
	public int type;
	public int renderType;
	public int groupIndex = -1;
	
	
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
	public void setPointColorRGB(Color pointColor_){
		setPointColor(new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor.getAlpha()));
	}
	
	@Override
	public void setLineColorRGB(Color lineColor_){
		setLineColor(new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor.getAlpha()));
	}
	
	@Override
	public Color getLineColor()
	{
		return new Color(lineColor.getRed(),lineColor.getGreen(),lineColor.getBlue(),lineColor.getAlpha());
	}

	@Override
	public Color getPointColor()
	{
		return new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),pointColor.getAlpha());
	}
	@Override
	public float getOpacity()
	{
		return ((float)(pointColor.getAlpha())/255.0f);
	}
	@Override
	public void setOpacity(float fOpacity)
	{
		setPointColor(new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),(int)(fOpacity*255)));
		setLineColor(new Color(lineColor.getRed(),lineColor.getGreen(),lineColor.getBlue(),(int)(fOpacity*255)));
	}
	
	@Override
	public float getPointSize() {

		return pointSize;
	}
	
	@Override
	public float getLineThickness() {

		return lineThickness;
	}
	
	@Override
	public int getRenderType(){
		return renderType;
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
	/** gets intensity values at allPoints position (provided in SPACE units) at 
	 * the interpolate RRA 
	 * it is assumes that allPoints are sampled with the same length step,
	 * equal to dMinVoxelSize **/
	public < T extends RealType< T > >  double [][] getIntensityProfilePoints(final ArrayList<RealPoint> allPoints, RealRandomAccessible<T> interpolate, final double [] globCal)
	{
		double [][] out = new double [5][allPoints.size()];
		
		final RealRandomAccess<T> ra =   interpolate.realRandomAccess();
		double [] pos = new double[3];
		double [] xyz;
		final double dMinVoxelSize = Math.min(Math.min(globCal[0], globCal[1]),globCal[2]);
		
		int i,d;
		
		//first point
		out[0][0]=0.0;
		allPoints.get(0).localize(pos);
		xyz = pos.clone();
		//in voxels
		pos = Roi3D.scaleGlobInv(pos, globCal);
		ra.setPosition(pos);
		//intensity
		out[1][0]=ra.get().getRealDouble();
		//length
		
		//point location
		for(d=0;d<3;d++)
		{
			out[2+d][0]=xyz[d];
		}
		for(i=1;i<allPoints.size();i++)
		{
			allPoints.get(i).localize(pos);
			out[0][i] = dMinVoxelSize*i;
			xyz= pos.clone();			
			//in voxels
			pos = Roi3D.scaleGlobInv(pos, globCal);
			ra.setPosition(pos);
			//intensity
			out[1][i]=ra.get().getRealDouble();
			for(d=0;d<3;d++)
			{
				out[2+d][i]=xyz[d];
			}
		}

		return out;
	}
	/** assumes that input allPoints are in SPACE coordinates.
	 *  calculates angle between each segment and dir_vector.
	 *  If bCosine = true returns cosine. Otherwise returns angle value (in radians).
	 *  Returned array contains
	 *  [0] - cumulative length values at the middle of each segment
	 *  [1] - cosine or angle
	 *  [2] - x coordinate of the middle of the segment (in SPACE scaled coordinates)
	 *  [3] - y coordinate of the middle of the segment (in SPACE scaled coordinates)
	 *  [4] - z coordinate of the middle of the segment (in SPACE scaled coordinates)
	 *  of second dimension allPoints.size()-1 (number of segments) **/
	public double [][] getCoalignmentProfilePoints(final ArrayList<RealPoint> allPoints, final double [] dir_vector, final boolean bCosine)
	{
		int i,d;
		double [][] out = new double [5][allPoints.size()-1];
		double [] pos1 = new double[3];
		double [] pos2 = new double[3];
		double [] segmDir = new double[3];
		double segmLength;
		double prevCumLength = 0.0;
		
		allPoints.get(0).localize(pos1);
		//pos1 = Roi3D.scaleGlob(pos1, globCal);
		for(i=1;i<allPoints.size();i++)
		{
			
			allPoints.get(i).localize(pos2);			
			//pos2 = Roi3D.scaleGlob(pos2, globCal);
			segmLength = LinAlgHelpers.distance(pos1,pos2);
			out[0][i-1] = prevCumLength+segmLength*0.5;
			prevCumLength+=segmLength;
			LinAlgHelpers.subtract(pos2, pos1, segmDir);
			for(d=0;d<3;d++)
			{
				//position of the middle of segment 
				out[2+d][i-1]=pos1[d]+segmDir[d]*0.5;
				pos1[d]=pos2[d];
				//normalize segment's direction length
				segmDir[d]=segmDir[d]/segmLength;
			}
			if(bCosine)
			{
				out[1][i-1] = LinAlgHelpers.dot(dir_vector, segmDir);
			}
			else
			{
				out[1][i-1] = Math.acos(LinAlgHelpers.dot(dir_vector, segmDir));
			}
		}
		return out;
	}
}

package bigtrace.measure;


import net.imglib2.RealPoint;

public class MeasureValues implements Measurements {
	
	public String roiName;
	public String roiGroupName;
	float pointSize;
	float lineThickness;
	public int nTimePoint;
	public int roiType;
	public double length;
	public double endsDistance;
	public double mean;
	public double stdDev;
	public double straightness;
	/** coordinates of the ends **/
	public RealPoint [] ends;
	/** normalized vector pointing between ends **/
	public RealPoint direction = null;
	public double [] intensity_values = null;
	


	void setRoiName(String roiname_)
	{
		this.roiName = new String(roiname_);	
	}
	
	void setRoiGroupName(String groupname_)
	{
		this.roiGroupName = new String(groupname_);	
	}
	
	public void setRoiType(final int roiType_)
	{
		this.roiType = roiType_;
	}
	
	void setTimePoint(int nTimePoint_)
	{
		nTimePoint = nTimePoint_;
	}
	
	String getRoiName()
	{
		return new String(roiName);
	}
	
	String getRoiGroupName()
	{
		return new String(roiGroupName);
	}
	
	public int getRoiType() {		
		return roiType;
	}
	
	public int getTimePoint() {		
		return nTimePoint;
	}
	
	void setPointSize(float pointSize_)
	{
		pointSize = pointSize_;
	}
	
	public float getPointSize()
	{
		return pointSize;
	}
	
	void setLineThickness(float lineThickness_)
	{
		lineThickness = lineThickness_;
	}
	
	public float getLineThickness()
	{
		return lineThickness;
	}
}

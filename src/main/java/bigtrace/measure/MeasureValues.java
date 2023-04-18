package bigtrace.measure;

import ij.measure.Measurements;
import net.imglib2.RealPoint;

public class MeasureValues implements Measurements {
	
	public String roiName;
	public String roiGroupName;
	public int roiType;
	public double length;
	public double endsDistance;
	public double mean;
	public double stdDev;
	public double straightness;
	public RealPoint [] ends;
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
	public void setRoiType(final int roiType_)
	{
		this.roiType=roiType_;
	}
}

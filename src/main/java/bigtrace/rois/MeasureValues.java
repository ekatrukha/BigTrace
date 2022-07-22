package bigtrace.rois;

import ij.measure.Measurements;
import net.imglib2.RealPoint;

public class MeasureValues implements Measurements {
	
	public String roiName;
	public int roiType;
	public double length;
	public double mean;
	public double stdDev;
	public double endsDistance;
	public RealPoint [] ends;
	public RealPoint direction = null;
	

	void setRoiName(String roiname_)
	{
		this.roiName = new String(roiname_);	
	}
	String getRoiName()
	{
		return new String(roiName);
	}
	public int getRoiType() {
		
		return roiType;
	}
	public void setRoiType(final int roiType_)
	{
		this.roiType=roiType_;
	}
}

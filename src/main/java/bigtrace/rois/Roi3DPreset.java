package bigtrace.rois;

import java.awt.Color;

import bigtrace.scene.VisPolyLineScaled;

public class Roi3DPreset {
	 public Color pointColor = Color.GREEN;
	 public Color lineColor = Color.BLUE;
	 public String sPresetName;
	 
	 public float lineThickness = 4.0f;
	 public float pointSize = 6.0f;
	 public int renderType = VisPolyLineScaled.WIRE;
	 public int sectorN = 16;
	 
	 public Roi3DPreset(String sPresetName_, Color pointColor_, Color lineColor_, float lineThickness_, float pointSize_, int renderType_)
	 {
		 sPresetName=sPresetName_;
		 lineThickness=lineThickness_;
		 pointSize=pointSize_;
		 renderType = renderType_;
		 setPointColor(pointColor_);
		 setLineColor(lineColor_);
	 }
	 
	public void setPointColor(Color pointColor_) {
		
		pointColor = new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor_.getAlpha());		
	}
	
	public void setLineColor(Color lineColor_) {
		
		lineColor = new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor_.getAlpha());
	
	}

	public void setPointColorRGB(Color pointColor_){
		setPointColor(new Color(pointColor_.getRed(),pointColor_.getGreen(),pointColor_.getBlue(),pointColor.getAlpha()));
	}
	

	public void setLineColorRGB(Color lineColor_){
		setLineColor(new Color(lineColor_.getRed(),lineColor_.getGreen(),lineColor_.getBlue(),lineColor.getAlpha()));
	}
}

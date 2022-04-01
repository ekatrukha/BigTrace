package bigtrace.rois;

import java.awt.Color;

import bigtrace.scene.VisPolyLineScaled;

public class Roi3DPreset {
	 public Color pointColor = Color.GREEN;
	 public Color lineColor = Color.BLUE;
	 public String sName;
	 
	 public float lineThickness = 4.0f;
	 public float pointSize = 6.0f;
	 public int renderType = VisPolyLineScaled.WIRE;
	 public int sectorN = 16;
	 
	 public Roi3DPreset(String sPresetName_, float pointSize_, Color pointColor_, float lineThickness_, Color lineColor_,  int renderType_, int sectorN_)
	 {
		 sName=new String(sPresetName_);
		 lineThickness=lineThickness_;
		 pointSize=pointSize_;
		 renderType = renderType_;
		 sectorN = sectorN_;
		 setPointColor(pointColor_);
		 setLineColor(lineColor_);
	 }
	 public Roi3DPreset(Roi3DPreset preset_in, String name)
	 {
		 sName=new String(name);
		 lineThickness=preset_in.getLineThickness();
		 pointSize=preset_in.getPointSize();
		 renderType = preset_in.getRenderType();
		 sectorN = preset_in.sectorN;
		 setPointColor(preset_in.getPointColor());
		 setLineColor(preset_in.getLineColor());		 
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
	
	public void setPointSize(float point_size) {
		pointSize=point_size;		
	}
	
	public void setLineThickness(float line_thickness) {
		lineThickness=line_thickness;
	}
	
	public void setOpacity(float fOpacity)
	{
		setPointColor(new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),(int)(fOpacity*255)));
		setLineColor(new Color(lineColor.getRed(),lineColor.getGreen(),lineColor.getBlue(),(int)(fOpacity*255)));
	}
	
	public void setRenderType(int nRenderType){		
		renderType=nRenderType;
	}
	public String getName() {
		return new String(sName);
	}

	
	public void setName(String name) {
		this.sName = new String(name);		
	}
	
	public float getLineThickness() {

		return lineThickness;
	}

	public float getPointSize() {

		return pointSize;
	}
	public float getOpacity()
	{
		return ((float)(pointColor.getAlpha())/255.0f);
	}
	
	
	public Color getPointColor()
	{
		return new Color(pointColor.getRed(),pointColor.getGreen(),pointColor.getBlue(),pointColor.getAlpha());
	}
	
	
	public Color getLineColor()
	{
		return new Color(lineColor.getRed(),lineColor.getGreen(),lineColor.getBlue(),lineColor.getAlpha());
	}
	public int getRenderType()
	{
		return renderType;
	}
}

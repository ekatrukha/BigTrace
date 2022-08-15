package bigtrace.rois;

import java.awt.Color;

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

}

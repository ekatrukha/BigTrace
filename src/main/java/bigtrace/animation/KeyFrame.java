package bigtrace.animation;


public class KeyFrame
{
	
	float fMovieTimePoint;
	Scene scene;
	String name;
	int nIndex = 0;
	
	public KeyFrame(final Scene scene_, float fMovieTimePoint_)
	{
		scene = new Scene(scene_.getViewerTransform(),scene_.getClipBox(),scene_.getTimeFrame());
		name = "key"+Integer.toString(this.hashCode());
		//name = name_;
		fMovieTimePoint = fMovieTimePoint_;
		
	}
	
	public KeyFrame (String kfName)
	{
		name = kfName;
	}
	

	@Override
	public String toString()
	{
		return "["+Integer.toString( nIndex )+"]"+name;
	}
	
	public int getIndex()
	{
		return nIndex;
	}
	
	public String getName()
	{
		return name;
	}
	public void setName(String sName)
	{
		name = sName;
		return;
	}
	
	public Scene getScene()
	{
		return scene;
	}
	
	public void setScene(Scene scene_)
	{
		scene = scene_;
		return;
	}
	
	public void setMovieTimePoint(float fMovieTimePoint_)
	{
		fMovieTimePoint = fMovieTimePoint_;
		return;
	}
	
	public KeyFrame duplicate()
	{
		final KeyFrame out = new KeyFrame(scene, fMovieTimePoint);
		out.name = this.name;
		return out;
	}
	
}

package bigtrace.animation;


public class KeyFrame
{
	
	float fMovieTimePoint;
	Scene scene;
	String name;
	int nIndex = 0;
	
	public KeyFrame (final Scene scene_, float fMovieTimePoint_)
	{
		scene = new Scene(scene_.getViewerTransform(),scene_.getClipBox(),scene_.getTimeFrame());
		name = "key"+Integer.toString(this.hashCode());
		//name = name_;
		fMovieTimePoint = fMovieTimePoint_;
		
	}
	@Override
	public String toString()
	{
		return "["+Integer.toString( nIndex )+"]"+name;
	}
	public Scene getScene()
	{
		return scene;
	}
	public KeyFrame duplicate()
	{
		final KeyFrame out = new KeyFrame(scene, fMovieTimePoint);
		out.name = this.name;
		return out;
	}
}

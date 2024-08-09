package bigtrace.animation;


public class KeyFrame
{
	
	float fMovieTimePoint;
	Scene scene;
	String name;
	
	public KeyFrame (final Scene scene_, float fMovieTimePoint_)
	{
		scene = new Scene(scene_.getViewerTransform(),scene_.getClipBox(),scene_.getTimePoint());
		name = "key"+Integer.toString(this.hashCode());
		//name = name_;
		fMovieTimePoint = fMovieTimePoint_;
		
	}
	@Override
	public String toString()
	{
		return name;
	}
	public Scene getScene()
	{
		return scene;
	}
}

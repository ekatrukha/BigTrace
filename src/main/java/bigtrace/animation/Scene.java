package bigtrace.animation;

import net.imglib2.realtransform.AffineTransform3D;

public class Scene
{
	final AffineTransform3D viewerTransform;
	final long [][] clipBox;	
	int nTimePoint;
	
	public Scene(final AffineTransform3D viewerTransform_, final long [][] clipBox_, int nTimePoint_)
	{
		viewerTransform = new AffineTransform3D();
		viewerTransform.set( viewerTransform_ );
		clipBox = new long [2][3];
		nTimePoint = nTimePoint_;
		for(int i=0;i<2;i++)
			for(int j=0;j<3;j++)
				clipBox[i][j]=clipBox_[i][j];
	}
	
	public AffineTransform3D getViewerTransform()
	{
		return viewerTransform;
	}
	
	public long [][] getClipBox()
	{
		return clipBox;
	}
	
	public int getTimePoint()
	{
		return nTimePoint;
	}
	
}

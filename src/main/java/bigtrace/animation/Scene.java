package bigtrace.animation;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import net.imglib2.realtransform.AffineTransform3D;

public class Scene
{
	final AffineTransform3D viewerTransform;
	final long [][] clipBox;	
	int nTimeFrame;
	
	public Scene(final AffineTransform3D viewerTransform_, final long [][] clipBox_, int nTimeFrame_)
	{
		viewerTransform = new AffineTransform3D();
		viewerTransform.set( viewerTransform_ );
		clipBox = new long [2][3];
		nTimeFrame = nTimeFrame_;
		for(int i=0;i<2;i++)
			for(int j=0;j<3;j++)
				clipBox[i][j]=clipBox_[i][j];
	}
	public Scene()
	{
		viewerTransform = new AffineTransform3D();
		clipBox = new long [2][3];
	}
	
	public AffineTransform3D getViewerTransform()
	{
		return viewerTransform;
	}
	
	public void setViewerTransform( final double... values )
	{
		viewerTransform.set( values );
	}
	
	public long [][] getClipBox()
	{
		return clipBox;
	}
	
	public void setClipBox(long [][] cb)
	{
		for(int i=0;i<2;i++)
			for(int j=0;j<3;j++)
				clipBox[i][j]=cb[i][j];
		return;
	}
	
	public int getTimeFrame()
	{
		return nTimeFrame;
	}
	
	public void setTimeFrame(int nTimeFrame_)
	{
		nTimeFrame = nTimeFrame_;
		return;
	}
	
	public void save(final FileWriter writer)
	{
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		DecimalFormat df3 = new DecimalFormat ("#.#######", symbols);
		try
		{
			writer.write("TimePoint," + Integer.toString(nTimeFrame) + "\n");
			writer.write("ViewTransform");
			final double [] transform = new double [12];
			viewerTransform.toArray(transform);
			for (int m = 0; m<12; m++)
			{
				writer.write("," + df3.format(transform[m]));
			}
			writer.write("\n");
			writer.write("ClipMin");
			for (int m = 0; m<3; m++)
			{
				writer.write("," + Long.toString(clipBox[0][m]));
			}
			writer.write("\n");
			writer.write("ClipMax");
			for (int m = 0; m<3; m++)
			{
				writer.write("," + Long.toString(clipBox[1][m]));
			}
			writer.write("\n");			
			
		}
		catch ( IOException exc )
		{
			exc.printStackTrace();
		}
		
	}
}
	


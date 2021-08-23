package BigTrace;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOverlay;
import bdv.viewer.ViewerPanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.ArrayList;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.LinAlgHelpers;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import sc.fiji.simplifiedio.SimplifiedIO;

/**
 * Combining Overlays and Actions to make a simple distance measuring tool
 */
public class E07MeasureDistance
{
	public static void main( final String[] args )
	{
		final Img< UnsignedByteType > img = SimplifiedIO.openImage(
				E07MeasureDistance.class.getResource( "/t1-head.tif" ).getFile(),
				new UnsignedByteType() );

		final Bdv bdv = BdvFunctions.show( img, "t1-head" );

		final ArrayList< RealPoint > points = new ArrayList<>();

		final ViewerPanel viewerPanel = bdv.getBdvHandle().getViewerPanel();
		final Actions actions = new Actions( new InputTriggerConfig() );
		actions.runnableAction(
				() -> {
					RealPoint point = new RealPoint( 3 );
					viewerPanel.getGlobalMouseCoordinates( point );
					points.add( point );
					viewerPanel.requestRepaint();
				},
				"my action",
				"A" );
		actions.install( bdv.getBdvHandle().getKeybindings(), "my actions" );

		final BdvOverlay overlay = new BdvOverlay()
		{
			private Color getColor( final double depth )
			{
				int alpha = 255 - ( int ) Math.round( Math.abs( depth ) );

				if ( alpha < 64 )
					alpha = 64;

				final int r = ARGBType.red( info.getColor().get() );
				final int g = ARGBType.green( info.getColor().get() );
				final int b = ARGBType.blue( info.getColor().get() );
				return new Color( r , g, b, alpha );
			}

			private int getSize( final double depth )
			{
				return ( int ) Math.max( 1, 10 - 0.1 * Math.round( Math.abs( depth ) ) );
			}

			@Override
			protected void draw( final Graphics2D g )
			{
				AffineTransform3D transform = new AffineTransform3D();
				this.getCurrentTransform3D( transform );

				final double[] lPos = new double[ 3 ];
				final double[] gPos = new double[ 3 ];
				for ( final RealPoint p : points )
				{
					p.localize( lPos );
					transform.apply( lPos, gPos );
					final int size = getSize( gPos[ 2 ] );
					final int x = ( int ) ( gPos[ 0 ] - 0.5 * size );
					final int y = ( int ) ( gPos[ 1 ] - 0.5 * size );
					g.setColor( getColor( gPos[ 2 ] ) );
					g.fillOval( x, y, size, size );
				}

				final double[] lPos2 = new double[ 3 ];
				final double[] gPos2 = new double[ 3 ];
				for ( int i = 0; i < points.size() - 1; i += 2 )
				{
					points.get( i ).localize( lPos );
					transform.apply( lPos, gPos );
					final int size = getSize( gPos[ 2 ] );

					points.get( i + 1 ).localize( lPos2 );
					transform.apply( lPos2, gPos2 );

					final int x0 = ( int ) gPos[ 0 ];
					final int y0 = ( int ) gPos[ 1 ];
					final int x1 = ( int ) gPos2[ 0 ];
					final int y1 = ( int ) gPos2[ 1 ];

					g.setPaint( new GradientPaint( x0, y0, getColor( gPos[ 2 ] ), x1, y1, getColor( gPos2[ 2 ] ) ) );
					g.setStroke(new BasicStroke(7));
					g.drawLine( x0, y0, x1, y1 );

					g.drawString(
							String.format( "%.3f", LinAlgHelpers.distance( lPos, lPos2 ) ),
							( x0 + x1 ) / 2,
							( y0 + y1 ) / 2 );
				}
			}
		};

		BdvFunctions.showOverlay( overlay, "overlay", Bdv.options().addTo( bdv ) );
	}
}

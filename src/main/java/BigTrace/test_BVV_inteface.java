/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2021 Max Planck Institute of Molecular Cell Biology
 * and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package BigTrace;



import java.awt.Dimension;

import java.io.IOException;

import java.util.ArrayList;


import javax.swing.JFrame;
import javax.swing.JPanel;


import org.joml.Matrix4f;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import BigTrace.polyline.BTPolylines;
import BigTrace.scene.VisPointsSimple;
import BigTrace.scene.VisPolyLineSimple;

import animation3d.gui.CroppingPanel;

import bdv.viewer.SynchronizedViewerState;


import bvv.util.BvvStackSource;
import ij.IJ;
import ij.ImagePlus;
import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvHandleFrame;
import bvv.util.Bvv;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.simplifiedio.SimplifiedIO;
import tpietzsch.example2.VolumeViewerPanel;


public class test_BVV_inteface
{
	public  BvvStackSource< ? > bvv;
	public  BvvStackSource< ? > bvv2;
	RandomAccessibleInterval< UnsignedByteType > view;
	static RandomAccessibleInterval< UnsignedByteType > view2=null;
	Img< UnsignedByteType > img;
	VolumeViewerPanel handl;
	SynchronizedViewerState state;
	CroppingPanel slider;
	
	int nW;
	int nH;
	int nD;
	
	//ArrayList< RealPoint > point_coords = new ArrayList<>();
	BTPolylines traces = new BTPolylines ();
	
		
	public void runBVV()
	{
		
		
		//img = SimplifiedIO.openImage(
					//test_BVV_inteface.class.getResource( "home/eugene/workspace/ExM_MT.tif" ).getFile(),
					//new UnsignedByteType() );
		img = SimplifiedIO.openImage("/home/eugene/workspace/ExM_MT.tif", new UnsignedByteType());
		//final ImagePlus imp = IJ.openImage(		"/home/eugene/workspace/ExM_MT.tif");	
		//img = ImageJFunctions.wrapByte( imp );
		//img = SimplifiedIO.openImage(
		//		test_BVV_inteface.class.getResource( "home/t1-head.tif" ).getFile(),
		//		new UnsignedByteType() );
		
		nW=(int)img.dimension(0);
		nH=(int)img.dimension(1);
		nD=(int)img.dimension(2);
	


		final JPanel panel = new JPanel();
		panel.setPreferredSize( new Dimension( 400, 400 ) );
		
		

		slider = new CroppingPanel(new int[] { 0, 1000}, nW-1, nH-1, nD-1);
		

		//slider.addCroppingPanelListener((Listener) handler);
		slider.addCroppingPanelListener(new CroppingPanel.Listener() {

			@Override
			public void nearFarChanged(int near, int far) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				
						// TODO Auto-generated method stub
				if(bbx0>=0 && bby0>=0 &&bbz0>=0 && bbx1<=(nW-1) && bby1<=(nH-1) && bbz1<=(nD-1))
				{
					
					bvv2.removeFromBdv();
					System.gc();
					view2=Views.interval( img, new long[] { bbx0, bby0, bbz0 }, new long[]{ bbx1, bby1, bbz1 } );						
					bvv2 = BvvFunctions.show( view2, "crop", Bvv.options().addTo(bvv));
					

				}
			}

			@Override
			public void cutOffROI() {
				// TODO Auto-generated method stub
				
			}
		
		});
		
		
		
		final JFrame frame = new JFrame( "Cropping" );
		frame.add(slider);
		//frame.add( panel );
		frame.pack();
		frame.setVisible( true );
		panel.requestFocusInWindow();


		

		view =				 
				 Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ 1, 1, 1 } );
		//Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );
		
		AffineTransform3D t = new AffineTransform3D();
				
		bvv = BvvFunctions.show( view, "t1-head" );
		
		
		//scale the view to fit the original dataset
		double scale;
		if(800.0/(double)nW<600./(double)nH)
		{
			scale=800.0/(double)nW;
		}
		else
		{
			scale=600.0/(double)nH;
		}
		t.set(scale, 0.0, 0.0, 400.0-(0.5)*scale*(double)nW, 0.0, scale, 0.0, 300.0-(0.5)*scale*(double)nH, 0.0, 0.0, scale, (-0.5)*scale*(double)nD);
		

		handl=bvv.getBvvHandle().getViewerPanel();
		handl.state().setViewerTransform(t);
		handl.requestRepaint();		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "crop", Bvv.options().addTo(bvv));
		

		
		final Actions actions = new Actions( new InputTriggerConfig() );
		
		//actions.
		actions.runnableAction(
				() -> {
					//Point point_mouse = new Point( 2 );
					java.awt.Point point_mouse  =bvv.getBvvHandle().getViewerPanel().getMousePosition();
					System.out.println( "drag x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
					

					AffineTransform3D transform = new AffineTransform3D();
					handl.state().getViewerTransform(transform);
					
					//RandomAccessibleInterval< UnsignedByteType > view_tr;
					//interpolate
					RealRandomAccessible< UnsignedByteType > view_tr = Views.interpolate( Views.extendZero( view2 ), new NLinearInterpolatorFactory<>());
					//inverse transform
					view_tr = RealViews.affine(view_tr,transform);

					RandomAccessible< UnsignedByteType > view_tr_raster = Views.raster( view_tr );
					IntervalView< UnsignedByteType > intRay = 					
					 Views.interval(view_tr_raster, Intervals.createMinSize( point_mouse.x-5,point_mouse.y-5, -10000, 5, 5, 20000 ) );

					RealPoint locationMin = new RealPoint( 3 );
					RealPoint locationMax = new RealPoint( 3 );					
					computeMinMaxLocation( intRay , locationMin, locationMax );
					//transform.applyInverse(view_tr,view2);
					//float [] target = new float [3];
					RealPoint target = new RealPoint( 3 );
					
					//transform.applyInverse(locationMax, target);
					transform.applyInverse( target,locationMax);
					//transform.applyInverse(target,new float [] {point_mouse.x,point_mouse.y,0.0f});
					//point_coords.add(new RealPoint(target[0], target[1], target[2]));
					traces.addPointToActive(target);
					
					render_pl();
					handl.showMessage("Point added");
					
				},
				"add point",
				"Z" );
				
		actions.runnableAction(
				() -> {
					
				
					if(traces.removeLastPointFromActive())
					{
						render_pl();
						handl.showMessage("Point removed");
					}
					
				},
				"remove point",
				"X" );
		actions.runnableAction(
				() -> {
					
						traces.addNewLine();
						render_pl();
				},
				"new trace",
				"A" );
		//actions.namedAction(action, defaultKeyStrokes);
		actions.install( bvv.getBvvHandle().getKeybindings(), "my actions" );
		
		
	

//		bdv.getBdvHandle().getKeybindings().removeInputMap( "my actions" );

	}

	public void render_pl()
	{
		
		handl.setRenderScene( ( gl, data ) -> {
			
			for (int i=0;i<traces.nLinesN;i++)
			{
				ArrayList< RealPoint > point_coords = traces.get(i);
				VisPointsSimple points= new VisPointsSimple(new float[]{0.0f,1.0f,0.0f},point_coords, 60.0f);
				VisPolyLineSimple lines;
				if (i==traces.activeLine)
					lines = new VisPolyLineSimple(new float[]{1.0f,0.0f,0.0f}, point_coords, 5.0f);
				else
					lines = new VisPolyLineSimple(new float[]{0.0f,0.0f,1.0f}, point_coords, 5.0f);

				final Matrix4f pointtransform = new Matrix4f().translate( 0.5f*points.max_pos, 0.5f*points.max_pos, 0.5f*points.max_pos ).scale( points.max_pos );
				final Matrix4f linestransform = new Matrix4f().translate( 0.5f*lines.max_pos, 0.5f*lines.max_pos, 0.5f*lines.max_pos ).scale( lines.max_pos );
				points.draw( gl, new Matrix4f( data.getPv() ).mul( pointtransform  ), new double [] {data.getScreenWidth(), data.getScreenHeight()}, data.getDClipNear(), data.getDClipFar());
				lines.draw( gl, new Matrix4f( data.getPv() ).mul( linestransform  ));
			}
		} );

		handl.requestRepaint();

	}
	
	public static void main( String... args) throws IOException
	{
		
		test_BVV_inteface testI=new test_BVV_inteface(); 
		
		testI.runBVV();
		
		
	}
	/**
	 * Compute the location of the minimal and maximal intensity for any IterableInterval,
	 * like an {@link Img}.
	 *
	 * The functionality we need is to iterate and retrieve the location. Therefore we need a
	 * Cursor that can localize itself.
	 * Note that we do not use a LocalizingCursor as localization just happens from time to time.
	 *
	 * @param input - the input that has to just be {@link IterableInterval}
	 * @param minLocation - the location for the minimal value
	 * @param maxLocation - the location of the maximal value
	 */
	public < T extends Comparable< T > & Type< T > > void computeMinMaxLocation(
		final IterableInterval< T > input, final RealPoint minLocation, final RealPoint maxLocation )
	{
		// create a cursor for the image (the order does not matter)
		final Cursor< T > cursor = input.cursor();
 
		// initialize min and max with the first image value
		T type = cursor.next();
		T min = type.copy();
		T max = type.copy();
 
		// loop over the rest of the data and determine min and max value
		while ( cursor.hasNext() )
		{
			// we need this type more than once
			type = cursor.next();
 
			if ( type.compareTo( min ) < 0 )
			{
				min.set( type );
				minLocation.setPosition( cursor );
			}
 
			if ( type.compareTo( max ) > 0 )
			{
				max.set( type );
				maxLocation.setPosition( cursor );
			}
		}
	}
}

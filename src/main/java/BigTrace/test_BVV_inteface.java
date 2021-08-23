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
import java.io.StringReader;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;


import org.joml.Matrix4f;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;


import BigTrace.scene.VisTestCubeDiskRed;
import BigTrace.scene.VisTestCubeTextPoint;
import animation3d.gui.CroppingPanel;

import bdv.viewer.SynchronizedViewerState;


import bvv.util.BvvStackSource;
import ij.IJ;
import ij.ImagePlus;
import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvHandleFrame;
import bvv.util.Bvv;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import sc.fiji.simplifiedio.SimplifiedIO;
import tpietzsch.example2.VolumeViewerPanel;
import tpietzsch.scene.TexturedDepthRamp;

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
	



		/*
		 * Display a JPanel with the MouseAndKeyHandler registered.
		 */
		final JPanel panel = new JPanel();
		panel.setPreferredSize( new Dimension( 400, 400 ) );
		
		
		//DoubleSlider slider = new DoubleSlider("X cropping", new int[] {-100, 100}, new int[] {20, 50}, new Color(255, 0, 0, 100));
		//final JSlider slXaxis = new JSlider(JSlider.HORIZONTAL,
        //        0, nW, nW);
		//slXaxis.add
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
		//t.set(1.1363636363636365, 0.0, 0.0, 103.47727272727269, 0.0, 1.1363636363636365, 0.0, 0.06818181818181301, 0.0, 0.0, 1.1363636363636365, -140.90909090909093);
		

		handl=bvv.getBvvHandle().getViewerPanel();
		handl.state().setViewerTransform(t);
		handl.requestRepaint();		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "crop", Bvv.options().addTo(bvv));
		VisTestCubeDiskRed points= new VisTestCubeDiskRed();
		//VisTestCubeTextPoint points= new VisTestCubeTextPoint();
		
		handl.setRenderScene( ( gl, data ) -> {
			//final Matrix4f cubetransform = new Matrix4f().translate( 140, 150, 65 ).scale( 80 );
			final Matrix4f cubetransform = new Matrix4f().translate( 40, 40, 40 ).scale( 80 );
			points.draw( gl, new Matrix4f( data.getPv() ).mul( cubetransform ), new double [] {data.getScreenWidth(), data.getScreenHeight()});
			//points.draw( gl, new Matrix4f( data.getPv() ).mul( cubetransform ));
		} );

		handl.requestRepaint();
		
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.behaviour(
				new DragBehaviour()
				{
					@Override
					public void init( final int x, final int y )
					{
						System.out.println( "init x = [" + x + "], y = [" + y + "]" );
					}

					@Override
					public void drag( final int x, final int y )
					{
						System.out.println( "drag x = [" + x + "], y = [" + y + "]" );
					}

					@Override
					public void end( final int x, final int y )
					{
						System.out.println( "end x = [" + x + "], y = [" + y + "]" );
					}
				},
				"my behaviour",
				"meta button1", "A" );
		behaviours.install( bvv.getBvvHandle().getTriggerbindings(), "my behaviours" );

//		bdv.getBdvHandle().getKeybindings().removeInputMap( "my actions" );

	}

	public static void main( String... args) throws IOException
	{
		
		test_BVV_inteface testI=new test_BVV_inteface(); 
		
		testI.runBVV();
		
		
	}
}

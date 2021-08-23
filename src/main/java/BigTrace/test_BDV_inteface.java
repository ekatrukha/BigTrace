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
import java.io.StringReader;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

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

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import sc.fiji.simplifiedio.SimplifiedIO;

public class test_BDV_inteface
{
	static class MyDragBehaviour extends AbstractNamedBehaviour implements DragBehaviour
	{
		public MyDragBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void init( final int x, final int y )
		{
			System.out.println( name() + ": init(" + x + ", " + y + ")" );
		}

		@Override
		public void drag( final int x, final int y )
		{
			System.out.println( name() + ": drag(" + x + ", " + y + ")" );
		}

		@Override
		public void end( final int x, final int y )
		{
			System.out.println( name() + ": end(" + x + ", " + y + ")" );
		}
	}

	static class MyClickBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		public MyClickBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void click( final int x, final int y )
		{
			System.out.println( name() + ": click(" + x + ", " + y + ")" );
		}
	}

	static class MyScrollBehaviour extends AbstractNamedBehaviour implements ScrollBehaviour
	{
		public MyScrollBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			System.out.println( name() + ": scroll(" + wheelRotation + ", " + isHorizontal + ", " + x + ", " + y + ")" );
		}
	}

	public static void main( final String[] args )
	{
		/*
		 * Create InputTriggerMap and BehaviourMap. This is analogous to
		 * javax.swing InputMap and ActionMap.
		 */
		final InputTriggerMap inputMap = new InputTriggerMap();
		final BehaviourMap behaviourMap = new BehaviourMap();

		/*
		 * Create a MouseAndKeyHandler that dispatches to registered Behaviours.
		 */
		final MouseAndKeyHandler handler = new MouseAndKeyHandler();
		handler.setInputMap( inputMap );
		handler.setBehaviourMap( behaviourMap );

		/*
		 * Display a JPanel with the MouseAndKeyHandler registered.
		 */
		final JPanel panel = new JPanel();
		panel.setPreferredSize( new Dimension( 400, 400 ) );
		panel.addMouseListener( handler );
		panel.addMouseMotionListener( handler );
		panel.addMouseWheelListener( handler );
		panel.addKeyListener( handler );
		panel.addFocusListener( handler );
		final JFrame frame = new JFrame( "UsageExample" );
		frame.add( panel );
		frame.pack();
		frame.setVisible( true );
		panel.requestFocusInWindow();

		/*
		 * Load YAML config "file".
		 */
		final StringReader reader = new StringReader( "---\n" +
				"- !mapping"               + "\n" +
				"  action: drag1"          + "\n" +
				"  contexts: [all]"        + "\n" +
				"  triggers: [button1, G]" + "\n" +
				"- !mapping"               + "\n" +
				"  action: scroll1"        + "\n" +
				"  contexts: [all]"        + "\n" +
				"  triggers: [scroll]"     + "\n" +
				"" );
		final List< InputTriggerDescription > triggers = YamlConfigIO.read( reader );
		final InputTriggerConfig config = new InputTriggerConfig( triggers );

		/*
		 * Create behaviours and input mappings.
		 */
		Behaviours behaviours = new Behaviours( inputMap, behaviourMap, config, "all" );
		behaviours.namedBehaviour( new MyDragBehaviour( "drag1" ) ); // put input trigger as defined in config
		behaviours.namedBehaviour( new MyDragBehaviour( "drag2" ),
				"button1", "shift A | G" ); // default triggers if not defined in config
		behaviours.namedBehaviour( new MyScrollBehaviour( "scroll1" ),
				"alt scroll" );
		behaviours.namedBehaviour( new MyClickBehaviour( "click1" ),
				"button3", "B | all" );

		/*
		 * See org.scijava.ui.behaviour.util.InputActionBindings and
		 * org.scijava.ui.behaviour.util.TriggerBehaviourBindings for chaining
		 * InputMaps and BehaviourMaps.
		 */
		
		final Img< UnsignedByteType > img = SimplifiedIO.openImage(
				test_BDV_inteface.class.getResource( "/t1-head.tif" ).getFile(),
				new UnsignedByteType() );
		//img.realMax(new double[] { 100, 100, 100 });
		RandomAccessibleInterval< UnsignedByteType > view =
				 Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ 100, 100, 100 } );
		final Bdv bdv = BdvFunctions.show( view, "t1-head" );
	}
}

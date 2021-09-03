
package BigTrace;



import java.awt.Dimension;

import java.io.IOException;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;


import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import BigTrace.polyline.BTPolylines;
import BigTrace.scene.VisPointsSimple;
import BigTrace.scene.VisPolyLineSimple;
import Geometry.Cuboid3D;
import Geometry.Intersections3D;
import Geometry.Line3D;
import animation3d.gui.CroppingPanel;

import bdv.viewer.SynchronizedViewerState;

import net.imagej.ops.OpService;
import bvv.util.BvvStackSource;
import ij.IJ;
import ij.ImagePlus;
import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvHandleFrame;
import bvv.util.Bvv;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.simplifiedio.SimplifiedIO;
import tpietzsch.example2.VolumeViewerPanel;
import tpietzsch.util.MatrixMath;


public class test_BVV_inteface
{
	public  BvvStackSource< ? > bvv;
	public  BvvStackSource< ? > bvv2;
	RandomAccessibleInterval< UnsignedByteType > view;
	static IntervalView< UnsignedByteType > view2=null;
	Img< UnsignedByteType > img;
	VolumeViewerPanel handl;
	//SynchronizedViewerState state;
	CroppingPanel slider;
	
	int nW;
	int nH;
	int nD;
	//static long [] nDimIn = new long [3]; 
	long [][] nDimCurr = new long [2][3];
	double dCam = 1100.;
	double dClipNear = 1000.;
	double dClipFar = 1000.;
	
	boolean bShowWorldGrid = false;
	boolean bShowOrigin = true;
	
	//ArrayList< RealPoint > point_coords = new ArrayList<>();
	BTPolylines traces = new BTPolylines ();
	BTPolylines origin_data = new BTPolylines ();
	
		
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
		//nDimIn = img.dimensionsAsLongArray();
		nDimCurr[1][0] = nW-1;
		nDimCurr[1][1] = nH-1;
		nDimCurr[1][2] = nD-1;


		init(0.25*Math.min(nD, Math.min(nW,nH)));
		
		final JPanel panel = new JPanel();
		panel.setPreferredSize( new Dimension( 400, 400 ) );
		slider = new CroppingPanel(new int[] { -1000, 1000}, nW-1, nH-1, nD-1);
		slider.addCroppingPanelListener(new CroppingPanel.Listener() {

			@Override
			public void nearFarChanged(int near, int far) {
				// TODO Auto-generated method stub
				//VolumeViewer
				dClipNear = Math.abs(near);
				dClipFar = (double)far;
				bvv.getBvvHandle().getViewerPanel().setCamParams(dCam, dClipNear, dClipFar);
				handl.requestRepaint();
				
			}

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				
				// TODO Auto-generated method stub
				if(bbx0>=0 && bby0>=0 &&bbz0>=0 && bbx1<=(nW-1) && bby1<=(nH-1) && bbz1<=(nD-1))
				{
					nDimCurr[0]=new long[] { bbx0, bby0, bbz0 };
					nDimCurr[1]=new long[] { bbx1, bby1, bbz1 };
					
					bvv2.removeFromBdv();
					System.gc();
					view2=Views.interval( img, nDimCurr[0], nDimCurr[1] );						
					bvv2 = BvvFunctions.show( view2, "cropresize", Bvv.options().addTo(bvv));
					

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


		
		

		
		final Actions actions = new Actions( new InputTriggerConfig() );
		
		//find a brightest pixel in the direction of a click
		// (not really working properly yet
		actions.runnableAction(
				() -> {
					//addPoint();
					addPoint(5);
				},
				"add point",
				"Z" );
		
		//creates a line along the click taking into account 
		// frustum projection
		actions.runnableAction(
				() -> {

					addLineAlongTheClick();
				},
				"render click",
				"W" );
		//rotates a view along the axis of the click
		// in frustum projection
		actions.runnableAction(
				() -> {
					viewClickArea(10);
				},
				"rotate view click",
				"E" );
		
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
		actions.runnableAction(
				() -> {
					resetViewXY(false);
				},
				"reset view XY",
				"1" );
			actions.runnableAction(
				() -> {
					resetViewYZ(false);
				},
				"reset view YZ",
				"2" );
			
			actions.runnableAction(
					() -> {
						bShowOrigin=(!bShowOrigin);
						handl.requestRepaint();
					},
					"render origin",
					"O" );
			actions.runnableAction(
					() -> {
						bShowWorldGrid=(!bShowWorldGrid);
						handl.requestRepaint();
					},
					"render world grid",
					"P" );
			
			actions.runnableAction(
					() -> {
						traces = new BTPolylines ();
						handl.requestRepaint();
					},
					"reset tracings",
					"I" );
			
		//actions.namedAction(action, defaultKeyStrokes);
		actions.install( bvv.getBvvHandle().getKeybindings(), "my actions" );
		
		
	

//		bdv.getBdvHandle().getKeybindings().removeInputMap( "my actions" );

	}
	public void init(double axis_length)
	{
		int i;
		
		//basis vectors 
		RealPoint basis = new RealPoint(-0.1*axis_length, -0.1*axis_length,-0.1*axis_length);				
		for(i=0;i<3;i++)
		{			
			origin_data.addPointToActive(basis);
			basis.move(axis_length, i);
			origin_data.addPointToActive(basis);
			basis.move((-1.0)*axis_length, i);
			origin_data.addNewLine();
		}
		
		traces = new BTPolylines ();
		
		// init bigvolumeviewer
		view =				 
				 Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ 1, 1, 1 } );
		
						
		bvv = BvvFunctions.show( view, "t1-head" ,Bvv.options().dCam(dCam).dClipNear(dClipNear).dClipFar(dClipFar));	
		
		resetViewXY(true);
	}

	public void render_pl()
	{
		
		handl.setRenderScene( ( gl, data ) -> {
			
			for (int i=0;i<traces.nLinesN;i++)
			{
				ArrayList< RealPoint > point_coords = traces.get(i);
				VisPointsSimple points= new VisPointsSimple(new float[]{0.0f,1.0f,0.0f},point_coords, 30.0f);
				VisPolyLineSimple lines;
				if (i==traces.activeLine)
					lines = new VisPolyLineSimple(new float[]{1.0f,0.0f,0.0f}, point_coords, 5.0f);
				else
					lines = new VisPolyLineSimple(new float[]{0.0f,0.0f,1.0f}, point_coords, 5.0f);

				points.draw( gl, new Matrix4f( data.getPv() ), new double [] {data.getScreenWidth(), data.getScreenHeight()}, data.getDClipNear(), data.getDClipFar());
				lines.draw( gl, new Matrix4f( data.getPv() ));
			}
			
			//render the origin of coordinates
			if (bShowOrigin)
			{
				for (int i=0;i<3;i++)
				{
					ArrayList< RealPoint > point_coords = origin_data.get(i);
					VisPolyLineSimple lines;
					float [] color_orig = new float[]{0.0f,0.0f,0.0f};
					color_orig[i] = 1.0f;
					lines = new VisPolyLineSimple(color_orig, point_coords, 3.0f);
					color_orig[i] = 0.0f;								
					lines.draw( gl, new Matrix4f( data.getPv() ));	
				}
			}
			
			//render world grid
			
			if(bShowWorldGrid)
			{

				int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
				int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
				Matrix4f  matPerspWorld = new Matrix4f();
				MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, matPerspWorld );
				ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();
				//for (int i =0; i<sW; i+=40)
				float [] world_grid_color = new float[]{0.5f,0.5f,0.5f}; 
				float world_grith_thickness = 1.0f;
	
				//bottom grid
				float i;
				//vertical
				float j=(float)sH;
				float di=3.0f*(float)sW/20.0f;
				for (i =-sW; i<2*sW; i+=di)
				{
					point_coords.add(new RealPoint(i,j,(float)(-1.0*dClipNear)));
					point_coords.add(new RealPoint(i,j,(float)(1.0*dClipFar)));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grith_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}
				//horizontal
				float k=0; 
				float dk = (float)((dClipFar+dClipNear)/10.0);
				for (k= (float)(dClipFar*0.99); k>=(-1.0*dClipNear); k-=dk)
				{
					point_coords.add(new RealPoint((float)(-sW),j,k));
					point_coords.add(new RealPoint((float)2*sW,j,k));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grith_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}
				//middle grid
				// vertical
				for (i =-sW; i<2*sW; i+=di)
				{
					point_coords.add(new RealPoint(i,0,(float)(dClipFar*0.99)));
					point_coords.add(new RealPoint(i,sH,(float)(dClipFar*0.99)));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grith_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}	
				//horizontal
				float dj=(float)sH/10.0f;
				for (j =0; j<=sH; j+=dj)
				{
					point_coords.add(new RealPoint((float)(-sW),j,(float)(dClipFar*0.99)));
					point_coords.add(new RealPoint((float)2*sW,j,(float)(dClipFar*0.99)));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grith_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}	
				
				//top grid
				//vertical
				j=0.0f;
				//di=3.0f*(float)sW/20.0f;
				for (i =-sW; i<2*sW; i+=di)
				{
					point_coords.add(new RealPoint(i,j,(float)(-1.0*dClipNear)));
					point_coords.add(new RealPoint(i,j,(float)(1.0*dClipFar)));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grith_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}
				//horizontal			
				for (k= (float)(dClipFar*0.99); k>=(-1.0*dClipNear); k-=dk)
				{
					point_coords.add(new RealPoint((float)(-sW),j,k));
					point_coords.add(new RealPoint((float)2*sW,j,k));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grith_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}
			}
			
		} );

		handl.requestRepaint();

	}
	public void resetViewXY(boolean firstCall)
	{
		
		double scale;
		
		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		
		if((double)sW/(double)nW<(double)sH/(double)nH)
		{
			scale=(double)sW/(double)nW;
		}
		else
		{
			scale=(double)sH/(double)nH;
		}
		scale = 0.9*scale;
		AffineTransform3D t = new AffineTransform3D();
		t.set(scale, 0.0, 0.0, 0.5*((double)sW-scale*(double)nW), 0.0, scale, 0.0, 0.5*((double)sH-scale*(double)nH), 0.0, 0.0, scale, (-0.5)*scale*(double)nD);
		//t.set(1, 0.0, 0.0, 0.5*((double)sW-(double)nW), 0.0, 1.0, 0.0, 0.5*((double)sH-(double)nH), 0.0, 0.0, 1., 0.0);
		//t.identity();
		

		//traces = new BTPolylines ();						
		
		
		handl=bvv.getBvvHandle().getViewerPanel();
		handl.state().setViewerTransform(t);
		handl.requestRepaint();
		if(!firstCall)
			bvv2.removeFromBdv();
		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "cropreset", Bvv.options().addTo(bvv));
		
		render_pl();
	}
	
	public void resetViewYZ(boolean firstCall)
	{
		
		double scale;
		
		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		
		if((double)sW/(double)nD<(double)sH/(double)nH)
		{
			scale=(double)sW/(double)nD;
		}
		else
		{
			scale=(double)sH/(double)nH;
		}
		scale = 0.9*scale;
		AffineTransform3D t = new AffineTransform3D();
		AffineTransform3D t2 = new AffineTransform3D();
		//t.set(scale, 0.0, 0.0, 0.5*((double)sW-scale*(double)nD), 0.0, scale, 0.0, 0.5*scale*((double)sH-scale*(double)nH), 0.0, 0.0, scale, (-0.5)*scale*(double)nD);
		
		t.rotate(1, (-1)*Math.PI/2.0);
		//t.identity();
		t2.set(scale, 0.0, 0.0, 0.0, 0.0, scale, 0.0,0.5*((double)sH-scale*(double)nH) , 0.0, 0.0, scale, -0.5*((double)sW+scale*(double)nD));
		t.concatenate(t2);
		//t.set(scale, 0.0, 0.0, 0.5*((double)sW-scale*(double)nD), 0.0, scale, 0.0, 0.0, 0.0, 0.0, scale, 0.0);
		//t.identity();
		
		//t.set(1, 0.0, 0.0, 0.5*((double)sW-(double)nW), 0.0, 1.0, 0.0, 0.5*((double)sH-(double)nH), 0.0, 0.0, 1., 0.0);
		

		//traces = new BTPolylines ();						
		//render_pl();
		
		handl=bvv.getBvvHandle().getViewerPanel();
		handl.state().setViewerTransform(t);
		handl.requestRepaint();
		if(!firstCall)
			bvv2.removeFromBdv();
		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "cropresetYZ", Bvv.options().addTo(bvv));
		
		
	}
	
	public void addPoint(final int nHalfWindow)
	{
		int i,j;

		java.awt.Point point_mouse  = bvv.getBvvHandle().getViewerPanel().getMousePosition();
		System.out.println( "drag x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
														
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		handl.state().getViewerTransform(transform);
		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		Matrix4f matPerspWorld = new Matrix4f();
		MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		
		
		ArrayList<RealPoint> clickFrustum = new ArrayList<RealPoint> ();
		Vector3f temp = new Vector3f(); 
		
		//float [] zVals = new float []{0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f};
		for (i = -nHalfWindow;i<3*nHalfWindow;i+=2*nHalfWindow)
			for (j = -nHalfWindow;j<3*nHalfWindow;j+=2*nHalfWindow)
				for (int z =0 ; z<2; z++)
				{
					//take coordinates in original data volume space
					matPerspWorld.unproject((float)point_mouse.x+i,sH-(float)point_mouse.y+j,(float)z, //z=1 ->far from camera z=0 -> close to camera
							new int[] { 0, 0, sW, sH },temp);
					//persp.unproject((float)point_mouse.x+i,sH-(float)point_mouse.y+j,zVals[nCount+1], //z=1 ->far from camera z=0 -> close to camera
					//new int[] { 0, 0, sW, sH },temp);

					clickFrustum.add(new RealPoint(temp.x,temp.y,temp.z));
					
				}
		//build lines (rays)
		ArrayList<Line3D> frustimLines = new ArrayList<Line3D>();
		for(i =0;i<clickFrustum.size();i+=2)
		{
			frustimLines.add(new Line3D(clickFrustum.get(i),clickFrustum.get(i+1)));
		}
		
		/*
		// original lines (rays)
		for(i =0;i<clickFrustum.size();i+=2)
		{
			traces.addNewLine();
			traces.addPointToActive(clickFrustum.get(i));
			traces.addPointToActive(clickFrustum.get(i+1));
		}
		*/
		
		//current dataset
		Cuboid3D dataCube = new Cuboid3D(nDimCurr); 
		dataCube.iniFaces();
		ArrayList<RealPoint> intersectionPoints = Intersections3D.cuboidLinesIntersect(dataCube, frustimLines);
		// Lines(rays) truncated to the volume.
		// For now, all of them must contained inside datacube.
		
		if(intersectionPoints.size()==8)
		{
			/*
			for(i =0;i<intersectionPoints.size();i+=2)
			{
				traces.addNewLine();
				traces.addPointToActive(intersectionPoints.get(i));
				traces.addPointToActive(intersectionPoints.get(i+1));
			}
			*/
		}		
		else
		{
			System.out.println( "#intersection points " + intersectionPoints.size());
			return;
		}
		long [][] nClickMinMax = new long[2][3];
		
		if(newBoundBox(intersectionPoints, nClickMinMax))
		{
			/*
			//show volume that was cut-off
			bvv2.removeFromBdv();
			System.gc();
			view2=Views.interval( img, nClickMinMax[0], nClickMinMax[1]);	
		
			bvv2 = BvvFunctions.show( view2, "cropclick", Bvv.options().addTo(bvv));
			*/
			
			
			IntervalView< UnsignedByteType > intRay = Views.interval(view2, Intervals.createMinMax(nClickMinMax[0][0],nClickMinMax[0][1],nClickMinMax[0][2],
																								   nClickMinMax[1][0],nClickMinMax[1][1],nClickMinMax[1][2]));
			
			//double [][] singleCube  = new double [2][3];
			//for(i=0;i<3;i++)
			//	singleCube[1][i]=1.0;
			//Cuboid3D clicktest = new Cuboid3D(singleCube);
			//Cuboid3D clickVolume = new Cuboid3D(intersectionPoints);
			Cuboid3D clickVolume = new Cuboid3D(clickFrustum);
			clickVolume.iniFaces();
			RealPoint target = new RealPoint( 3 );
			//RealPoint locationMax = new RealPoint( 3 );
			
			if(computeMaxLocationCuboid(intRay,target,clickVolume))
			{
				traces.addPointToActive(target);
				handl.showMessage("point found");
			}
			else
			{
				handl.showMessage("not found :(");
			}
				

						
		}
		

		render_pl();		
		
	}
	
	public void addLineAlongTheClick()
	{
		java.awt.Point point_mouse  =bvv.getBvvHandle().getViewerPanel().getMousePosition();
		System.out.println( "drag x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
		

		AffineTransform3D transform = new AffineTransform3D();
		
		handl.state().getViewerTransform(transform);
		//transform.concatenate(affine)
		ArrayList<RealPoint> viewclick = new ArrayList<RealPoint>();
		//int dW=5;

		/**/

		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		//Matrix4f viewm = MatrixMath.affine( transform, new Matrix4f() );
		Matrix4f persp = new Matrix4f();
		MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, persp ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		
		
		Matrix4f matPers = new Matrix4f();
		MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, matPers );
		
		Vector3f temp1 = new Vector3f();
		Vector3f temp2 = new Vector3f(); 
		matPers.unproject((float)point_mouse.x,sH-(float)point_mouse.y,1.0f, //z=1 ->far from camera z=0 -> close to camera
				new int[] { 0, 0, sW, sH },temp1);
		
		matPers.unproject((float)point_mouse.x,sH-(float)point_mouse.y,0.0f, //z=1 ->far from camera z=0 -> close to camera
				new int[] { 0, 0, sW, sH },temp2);
		
		
		Vector3f worldCoords1 = new Vector3f();
		Vector3f worldCoords2 = new Vector3f();

		
		persp.unproject((float)point_mouse.x,sH-(float)point_mouse.y,1.0f, //far from camera
				  new int[] { 0, 0, sW, sH },worldCoords1);
		persp.unproject((float)point_mouse.x,sH-(float)point_mouse.y,0.0f, //close to camera 
				  new int[] { 0, 0, sW, sH },worldCoords2);


		//double [] intersect_point = new double[3];
		
		Cuboid3D viewCube = new Cuboid3D(nDimCurr); 
		viewCube.iniFaces();
		//dW++;
		
		Line3D clickRay = new Line3D(worldCoords1,worldCoords2);
		/*for(int i=0;i<6;i++)
		{
			if(Intersections3D.planeLineIntersect(viewCube.faces.get(i), clickRay, intersect_point))
			{
				//if(viewCube.isPointInsideShape(intersect_point))
				if(viewCube.isPointInsideMinMax(intersect_point))
					viewclick.add(new RealPoint(intersect_point));
			}
		}*/
		ArrayList<Line3D> linex= new ArrayList<Line3D>();
		linex.add(clickRay);
		viewclick=Intersections3D.cuboidLinesIntersect(viewCube,linex);
		System.out.println( "Intersect points #: " + viewclick.size());
		

		if(viewclick.size()>1)
		{
			for(int i =0;i<2;i+=2)
			{
				traces.addNewLine();
				traces.addPointToActive(viewclick.get(i));
				traces.addPointToActive(viewclick.get(i+1));
			}
		}
		if(viewclick.size()==1)
		{
			viewclick=Intersections3D.cuboidLinesIntersect(viewCube,linex);
		}

		render_pl();
	}
	
	public void viewClickArea(final int nHalfWindow)
	{
		int i,j;

		java.awt.Point point_mouse  = bvv.getBvvHandle().getViewerPanel().getMousePosition();
		System.out.println( "drag x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
														
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		handl.state().getViewerTransform(transform);
		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		Matrix4f matPerspWorld = new Matrix4f();
		MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );


		ArrayList<RealPoint> clickFrustum = new ArrayList<RealPoint> ();
		Vector3f temp = new Vector3f(); 

		//float [] zVals = new float []{0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f};
		for (i = -nHalfWindow;i<3*nHalfWindow;i+=2*nHalfWindow)
			for (j = -nHalfWindow;j<3*nHalfWindow;j+=2*nHalfWindow)
				for (int z =0 ; z<2; z++)
				{
					//take coordinates in original data volume space
					matPerspWorld.unproject((float)point_mouse.x+i,sH-(float)point_mouse.y+j,(float)z, //z=1 ->far from camera z=0 -> close to camera
							new int[] { 0, 0, sW, sH },temp);
					//persp.unproject((float)point_mouse.x+i,sH-(float)point_mouse.y+j,zVals[nCount+1], //z=1 ->far from camera z=0 -> close to camera
					//new int[] { 0, 0, sW, sH },temp);

					clickFrustum.add(new RealPoint(temp.x,temp.y,temp.z));
					
				}
		//build lines (rays)
		ArrayList<Line3D> frustimLines = new ArrayList<Line3D>();
		for(i =0;i<clickFrustum.size();i+=2)
		{
			frustimLines.add(new Line3D(clickFrustum.get(i),clickFrustum.get(i+1)));
		}
		
		
		// original lines (rays)
		/*
		for(i =0;i<clickFrustum.size();i+=2)
		{
			traces.addNewLine();
			traces.addPointToActive(clickFrustum.get(i));
			traces.addPointToActive(clickFrustum.get(i+1));
		}
		*/
		
		//current dataset
		Cuboid3D dataCube = new Cuboid3D(nDimCurr); 
		dataCube.iniFaces();
		ArrayList<RealPoint> intersectionPoints = Intersections3D.cuboidLinesIntersect(dataCube, frustimLines);
		// Lines(rays) truncated to the volume.
		// For now, all of them must contained inside datacube.
		
		traces = new BTPolylines ();
			
		for(i =0;i<intersectionPoints.size();i+=2)
		{
			traces.addNewLine();
			traces.addPointToActive(intersectionPoints.get(i));
			if(i<(intersectionPoints.size()-1))
				traces.addPointToActive(intersectionPoints.get(i+1));
		}
		
		if(intersectionPoints.size()==8)
		{

			
		}		
		else
		{
			System.out.println( "#intersection points " + intersectionPoints.size());
			return;
		}
		long [][] nClickMinMax = new long[2][3];
		
		if(newBoundBox(intersectionPoints, nClickMinMax))
		{
			
			//show volume that was cut-off
			bvv2.removeFromBdv();
			System.gc();
			view2=Views.interval( img, nClickMinMax[0], nClickMinMax[1]);	
		
			bvv2 = BvvFunctions.show( view2, "cropclick", Bvv.options().addTo(bvv));
			
			/*
			
			IntervalView< UnsignedByteType > intRay = Views.interval(view2, Intervals.createMinMax(nClickMinMax[0][0],nClickMinMax[0][1],nClickMinMax[0][2],
																								   nClickMinMax[1][0],nClickMinMax[1][1],nClickMinMax[1][2]));
			
			//double [][] singleCube  = new double [2][3];
			//for(i=0;i<3;i++)
			//	singleCube[1][i]=1.0;
			//Cuboid3D clicktest = new Cuboid3D(singleCube);
			
			//Cuboid3D clickVolume = new Cuboid3D(intersectionPoints); 
			Cuboid3D clickVolume = new Cuboid3D(clickFrustum);
			clickVolume.iniFaces();
			RealPoint target = new RealPoint( 3 );
			//RealPoint locationMax = new RealPoint( 3 );
			
			if(computeMaxLocationCuboid(intRay,target,clickVolume))
			{
				traces.addPointToActive(target);
				handl.showMessage("point found");
			}
			else
			{
				handl.showMessage("not found :(");
			}
				
			 */
						
		}
		

		render_pl();
	}
	public static void main( String... args) throws IOException
	{
		
		test_BVV_inteface testI=new test_BVV_inteface(); 
		
		testI.runBVV();
		
		
	}

	
	/**
	 * Compute the location of the maximal intensity for any IterableInterval,
	 * like an {@link Img}, contained inside Cuboid3D
	 *
	 * The functionality we need is to iterate and retrieve the location. Therefore we need a
	 * Cursor that can localize itself.
	 * Note that we do not use a LocalizingCursor as localization just happens from time to time.
	 *
	 * @param input - the input that has to just be {@link IterableInterval}
	 * @param maxLocation - the location of the maximal value
	 * @param clickCone - Cuboid3D, limiting search 
	 */
	public < T extends Comparable< T > & Type< T > > boolean computeMaxLocationCuboid(
		final IterableInterval< T > input,  final RealPoint maxLocation, final Cuboid3D clickCone )
	{
		// create a cursor for the image (the order does not matter)
		final Cursor< T > cursor = input.cursor();
		
		boolean bFound=false;
		// initialize min and max with the first image value
		T type = cursor.next();
		T max = type.copy();
		double [] pos = new double [3];
		// loop over the rest of the data and determine min and max value
		while ( cursor.hasNext() )
		{
			// we need this type more than once
			type = cursor.next();
 
				if ( type.compareTo( max ) > 0 )
				{
					cursor.localize(pos);
					if(clickCone.isPointInsideShape(pos))
					{
						max.set( type );
						maxLocation.setPosition( cursor );
						bFound=true;
					}
				}
		}
		return bFound;
	}
	/**  function calculates transform allowing to align two vectors 
	 * @param align_direction - immobile vector
	 * @param moving - vector that aligned with align_direction
	 * @return affine transform (rotations)
	 * **/
	AffineTransform3D alignVectors(final RealPoint align_direction, final RealPoint moving)
	{
		double [] dstat = align_direction.positionAsDoubleArray();
		double [] dmov = moving.positionAsDoubleArray();
		double [] v = new double [3];
		double c;
		
		AffineTransform3D transform = new AffineTransform3D();
		LinAlgHelpers.normalize(dstat);
		LinAlgHelpers.normalize(dmov);
		c = LinAlgHelpers.dot(dstat, dmov);
		//exact opposite directions
		if ((c+1.0)<0.00001)
		{
			transform.identity();
			transform.scale(-1.0);			
		}
		
		LinAlgHelpers.cross( dstat,dmov, v);
		double [][] matrixV = new double [3][3];
		double [][] matrixV2 = new double [3][3];
		
		matrixV[0][1]=(-1.0)*v[2];
		matrixV[0][2]=v[1];
		matrixV[1][0]=v[2];
		matrixV[1][2]=(-1.0)*v[0];
		matrixV[2][0]=(-1.0)*v[1];
		matrixV[2][1]=v[0];
		
		LinAlgHelpers.mult(matrixV, matrixV, matrixV2);
		c=1.0/(1.0+c);
		LinAlgHelpers.scale(matrixV2, c, matrixV2);
		LinAlgHelpers.add(matrixV, matrixV2, matrixV);
		transform.set(1.0 + matrixV[0][0],       matrixV[0][1],       matrixV[0][2],
					        matrixV[1][0], 1.0 + matrixV[1][1],       matrixV[1][2], 
					        matrixV[2][0],       matrixV[2][1], 1.0 + matrixV[2][2],
					                  0.0,                 0.0,                 0.0);
		
		return transform;
	}
	
	FinalInterval RealIntervaltoInterval (RealInterval R_Int)	
	{
		int i;
		long [] minL = new long [3];
		long [] maxL = new long [3];
		double [] minR = new double [3];
		double [] maxR = new double [3];
		R_Int.realMax(maxR);
		R_Int.realMin(minR);
		for (i=0;i<3;i++)
		{
			minL[i]=(int)Math.round(minR[i]);
			maxL[i]=(int)Math.round(maxR[i]);			
		}
		return Intervals.createMinMax(minL[0],minL[1],minL[2], maxL[0],maxL[1],maxL[2]);
	}
	
	public boolean newBoundBox(final ArrayList<RealPoint> pointArray, final long [][] newMinMax)
	{ 
		//= new long [2][3];
		float [][] newMinMaxF = new float [2][3];
		int i, j;
		float temp;

		for (i=0;i<3;i++)
		{
			newMinMaxF[0][i]=Float.MAX_VALUE;
			newMinMaxF[1][i]=(-1)*Float.MAX_VALUE;
		}
		for (i=0;i<pointArray.size();i++)
		{
			
			for (j=0;j<3;j++)
			{
				temp=pointArray.get(i).getFloatPosition(j);
				if(temp>newMinMaxF[1][j])
					newMinMaxF[1][j]=temp;
				if(temp<newMinMaxF[0][j])
					newMinMaxF[0][j]=temp;
				
			}			
		}
		for (j=0;j<3;j++)
		{
				newMinMax[0][j]=Math.max(nDimCurr[0][j],(long)Math.round(newMinMaxF[0][j]));
				newMinMax[1][j]=Math.min(nDimCurr[1][j],(long)Math.round(newMinMaxF[1][j]));
				if(newMinMax[1][j]<=newMinMax[0][j])
					return false;
		}
		return true;

	}
	
}


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
	static long [] nDimIn = new long [3]; 
	long [][] nDimCurr = new long [2][3];
	double dCam = 2000.;
	double dClipNear = 1000.;
	double dClipFar = 1000.;
	OpService ops;
	
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
		nDimIn = img.dimensionsAsLongArray();
		nDimCurr[1] = img.dimensionsAsLongArray();


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
				bvv.getBvvHandle().getViewerPanel().setCamParams(2000., dClipNear, dClipFar);
				
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


		

		view =				 
				 Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ 1, 1, 1 } );
		//Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );
		
						
		bvv = BvvFunctions.show( view, "t1-head" ,Bvv.options().dCam(2000.).dClipNear(dClipNear).dClipFar(dClipFar));	
		
		resetView(true);
		

		
		final Actions actions = new Actions( new InputTriggerConfig() );
		
		//find a brightest pixel in the direction of a click
		// (not really working properly yet
		actions.runnableAction(
				() -> {
					addPoint();
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
					resetView(false);
				},
				"reset view",
				"R" );
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
				VisPointsSimple points= new VisPointsSimple(new float[]{0.0f,1.0f,0.0f},point_coords, 30.0f);
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
	public void resetView(boolean firstCall)
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
		
		AffineTransform3D t = new AffineTransform3D();
		t.set(scale, 0.0, 0.0, 0.5*((double)sW-scale*(double)nW), 0.0, scale, 0.0, 0.5*scale*((double)sH-scale*(double)nH), 0.0, 0.0, scale, (-0.5)*scale*(double)nD);
		//t.set(1, 0.0, 0.0, 0.5*((double)sW-(double)nW), 0.0, 1.0, 0.0, 0.5*((double)sH-(double)nH), 0.0, 0.0, 1., 0.0);
		

		traces = new BTPolylines ();						
		//render_pl();
		
		handl=bvv.getBvvHandle().getViewerPanel();
		handl.state().setViewerTransform(t);
		handl.requestRepaint();
		if(!firstCall)
			bvv2.removeFromBdv();
		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "cropreset", Bvv.options().addTo(bvv));
		
		
	}
	
	public void addPoint()
	{
		//Point point_mouse = new Point( 2 );
		java.awt.Point point_mouse  =bvv.getBvvHandle().getViewerPanel().getMousePosition();
		System.out.println( "drag x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
		

		AffineTransform3D transform = new AffineTransform3D();
		handl.state().getViewerTransform(transform);
		
		int dW=5;

		/**/

		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		//Matrix4f viewm = MatrixMath.affine( transform, new Matrix4f() );
		Matrix4f persp = new Matrix4f();
		MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, persp ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		Vector3f worldCoords1 = new Vector3f();
		Vector3f worldCoords2 = new Vector3f();
		Vector3f worldCoords1r = new Vector3f();
		Vector3f worldCoords2r = new Vector3f();
		
		persp.unproject((float)point_mouse.x,sH-(float)point_mouse.y,1.0f, //far from camera
				  new int[] { 0, 0, sW, sH },worldCoords1);
		persp.unproject((float)point_mouse.x,sH-(float)point_mouse.y,0.0f, //close to camera 
				  new int[] { 0, 0, sW, sH },worldCoords2);
		
		persp.unproject((float)(point_mouse.x+dW),sH-(float)point_mouse.y,1.0f, //far from camera
				  new int[] { 0, 0, sW, sH },worldCoords1r);
		persp.unproject((float)(point_mouse.x+dW),sH-(float)point_mouse.y,0.0f, //close to camera 
				  new int[] { 0, 0, sW, sH },worldCoords2r);

		
/*
											
		
		RealRandomAccessible< UnsignedByteType > view_tr = Views.interpolate(  Views.extendZero(view2) , new NLinearInterpolatorFactory<>());
		AffineRandomAccessible<UnsignedByteType, AffineGet > view_trxxx = RealViews.affine(view_tr,transform);
		RealInterval intervalBounds = transform.estimateBounds(view2);
		
		int maxX = (int)Math.ceil(Math.max(clickCone.r1, clickCone.r2));
		double minZ=Math.min(worldCoords1.z, worldCoords2.z);
		double rangeZ=Math.abs(worldCoords1.z- worldCoords2.z);
		IntervalView< UnsignedByteType > intRay = Views.interval(view_trxxx, Intervals.createMinSize( point_mouse.x-maxX,point_mouse.y-maxX, (int)(minZ), 2*maxX, 2*maxX, (int)(rangeZ)));
		//IntervalView< UnsignedByteType > intRayx = Views.interval(view_trxxx, Math.round(intervalBounds.realMin(0)));
		
		RealPoint locationMax = new RealPoint( 3 );		
		boolean bFound=false;
		bFound = computeMaxLocationConicalFrustum( intRay ,  locationMax,clickCone);
		System.out.println(bFound);
		//transform.applyInverse(view_tr,view2);
		//float [] target = new float [3];
		RealPoint target = new RealPoint( 3 );
		

		transform.applyInverse( target,locationMax);

		
		traces.addPointToActive(target);
		
		render_pl();*/
		handl.showMessage("Point added");
		
		
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
		Vector3f worldCoords1 = new Vector3f();
		Vector3f worldCoords2 = new Vector3f();

		
		persp.unproject((float)point_mouse.x,sH-(float)point_mouse.y,1.0f, //far from camera
				  new int[] { 0, 0, sW, sH },worldCoords1);
		persp.unproject((float)point_mouse.x,sH-(float)point_mouse.y,0.0f, //close to camera 
				  new int[] { 0, 0, sW, sH },worldCoords2);


		double [] intersect_point = new double[3];
		
		Cuboid3D viewCube = new Cuboid3D(nDimCurr); 
		viewCube.iniFaces();
		//dW++;
		
		Line3D clickRay = new Line3D(worldCoords1,worldCoords2);
		for(int i=0;i<6;i++)
		{
			if(Intersections3D.planeLineIntersect(viewCube.faces.get(i), clickRay, intersect_point))
			{
				//if(viewCube.isPointInsideShape(intersect_point))
				if(viewCube.isPointInsideMinMax(intersect_point))
					viewclick.add(new RealPoint(intersect_point));
			}
		}
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

		render_pl();
	}
	public void viewClickArea(final int nHalfWindow)
	{
		int i,j;

		java.awt.Point point_mouse  =bvv.getBvvHandle().getViewerPanel().getMousePosition();
		System.out.println( "drag x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
														
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		handl.state().getViewerTransform(transform);
		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		Matrix4f persp = new Matrix4f();
		MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, persp ).mul( MatrixMath.affine( transform, new Matrix4f() ) );


		ArrayList<RealPoint> clickFrustum = new ArrayList<RealPoint> ();
		Vector3f temp = new Vector3f(); 
		int nCount = -1;
		for (i = -nHalfWindow;i<3*nHalfWindow;i+=2*nHalfWindow)
			for (j = -nHalfWindow;j<3*nHalfWindow;j+=2*nHalfWindow)
				for (int z =0 ; z<2; z++)
				{
					
					
					persp.unproject((float)point_mouse.x+i,sH-(float)point_mouse.y+j,(float)z, //far from camera
							new int[] { 0, 0, sW, sH },temp);
					clickFrustum.add(new RealPoint(temp.x,temp.y,temp.z));
					nCount++;
				}
		ArrayList<Line3D> frustimLines = new ArrayList<Line3D>();
		for(i =0;i<clickFrustum.size();i+=2)
		{
			frustimLines.add(new Line3D(clickFrustum.get(i),clickFrustum.get(i+1)));
		}
		
		/*
		// original lines
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
		// lines truncated to the volume
		// for now, must all be contained inside
		if(intersectionPoints.size()==8)
		{
			for(i =0;i<intersectionPoints.size();i+=2)
			{
				traces.addNewLine();
				traces.addPointToActive(intersectionPoints.get(i));
				traces.addPointToActive(intersectionPoints.get(i+1));
			}
		}		
		else
		{
			return;
		}
		long [][] nClickMinMax = new long[2][3];
		if(newBoundBox(intersectionPoints, nClickMinMax))
		{
			bvv2.removeFromBdv();
			System.gc();
			view2=Views.interval( img, nClickMinMax[0], nClickMinMax[1]);	
		
			bvv2 = BvvFunctions.show( view2, "cropclick", Bvv.options().addTo(bvv));
			handl.showMessage("Cut success");			
		}
		
		/*
		long[][] nMinMax = new long [2][3]; 
		if(newBoundBox(clickFrustum,nMinMax))
		{
			bvv2.removeFromBdv();
			System.gc();
			view2=Views.interval( img, nMinMax[0], nMinMax[1]);	
		
			bvv2 = BvvFunctions.show( view2, "cropclick", Bvv.options().addTo(bvv));
			handl.showMessage("Cut success");
		}
		*/
		render_pl();
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
	public < T extends Comparable< T > & Type< T > > boolean computeMinMaxLocation(
		final IterableInterval< T > input, final RealPoint minLocation, final RealPoint maxLocation )
	{
		// create a cursor for the image (the order does not matter)
		final Cursor< T > cursor = input.cursor();
		
		boolean bFound=false;
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
				bFound=true;
			}
		}
		return bFound;
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
	public < T extends Comparable< T > & Type< T > > boolean computeMaxLocationCuboidMinMax(
		final IterableInterval< T > input,  final RealPoint maxLocation, final Cuboid3D clickCone )
	{
		// create a cursor for the image (the order does not matter)
		final Cursor< T > cursor = input.cursor();
		
		boolean bFound=false;
		// initialize min and max with the first image value
		T type = cursor.next();
		T max = type.copy();
		RealPoint pos = new RealPoint();
		// loop over the rest of the data and determine min and max value
		while ( cursor.hasNext() )
		{
			// we need this type more than once
			type = cursor.next();
 
				if ( type.compareTo( max ) > 0 )
				{
					cursor.localize(pos);
					if(clickCone.isPointInsideMinMax(pos))
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

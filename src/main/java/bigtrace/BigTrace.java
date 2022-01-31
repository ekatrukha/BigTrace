
package bigtrace;


import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.jogamp.opengl.GL3;

import bdv.viewer.animate.SimilarityTransformAnimator;
import bigtrace.geometry.Cuboid3D;
import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Line3D;
import bigtrace.math.DijkstraFHRestricted;
import bigtrace.math.TraceBoxMath;
import bigtrace.math.TracingBG;
import bigtrace.rois.Cube3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import bigtrace.rois.RoiManager3D;
import bigtrace.scene.VisPolyLineSimple;
import bigtrace.volume.VolumeMisc;

import bvv.util.BvvStackSource;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import bvv.util.BvvFunctions;
import bvv.util.Bvv;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import tpietzsch.example2.RenderData;
import tpietzsch.example2.VolumeViewerPanel;
import tpietzsch.util.MatrixMath;


public class BigTrace implements PlugIn, WindowListener
{
	public  BvvStackSource< UnsignedByteType > bvv = null;
	public  BvvStackSource< UnsignedByteType > bvv2 = null;
	public  BvvStackSource< UnsignedByteType > bvv_trace = null;
	RandomAccessibleInterval< UnsignedByteType > view;
	IntervalView< UnsignedByteType > currentView = null;

	//IntervalView< UnsignedByteType > trace_weights = null;
	//IntervalView< FloatType > trace_vectors=null;
	//ArrayList<long []> jump_points = null;
	boolean bTraceMode = false;
	
	Img< UnsignedByteType> img;

	public JFrame finFrame;
	VolumeViewerPanel panel;

	public boolean bInputLock = false;
	

	/** visualization of coordinates origin axes **/
	ArrayList<VisPolyLineSimple> originVis = new ArrayList<VisPolyLineSimple>();

	/** box around volume **/
	Cube3D volumeBox;

	
	public BigTraceData btdata = new BigTraceData();
	public BigTraceControlPanel btpanel;
	
	public DijkstraFHRestricted dijkRBegin;
	public DijkstraFHRestricted dijkREnd;
	//DijkstraBinaryHeap dijkBH;
	//DijkstraFibonacciHeap dijkFib;

	public RoiManager3D roiManager;
		
	public void run(String arg)
	{
		
		
		//img = SimplifiedIO.openImage(
					//test_BVV_inteface.class.getResource( "home/eugene/workspace/ExM_MT.tif" ).getFile(),
					//new UnsignedByteType() );
		if(arg.equals(""))
		{
			btdata.sFileNameImg=IJ.getFilePath("Open ONI results table");
		}
		else
		{
			btdata.sFileNameImg=arg;
		}

		if(btdata.sFileNameImg==null)
			return;
		final ImagePlus imp = IJ.openImage( btdata.sFileNameImg );
		
		/*if(!(imp.getType()==ImagePlus.GRAY8 || imp.getType()==ImagePlus.GRAY16 || imp.getType()==ImagePlus.GRAY32))
		{
			IJ.showMessage("Only 8/16/32-bit images supported for now.");
			return;
		}*/
		if(imp.getType()!=ImagePlus.GRAY8)
		{
			IJ.showMessage("Only 8-bit images supported for now.");
			return;
		}

		if(imp.getNChannels()>1)
		{
			IJ.showMessage("Only single channel images supported for now.");
			return;			
		}
		//img = convertInput(ImagePlusAdapter.wrapImgPlus( imp ), new UnsignedByteType());
		//ImagePlusAdapter.wrapImgPlus( imp );
		img = ImageJFunctions.wrapByte( imp );
		/*
		ImgOpener io = new ImgOpener();
		
		img = io.openImgs( btdata.sFileNameImg,
			new UnsignedByteType() ).get( 0 );*/
		//img = SimplifiedIO.openImage(btdata.sFileNameImg, new UnsignedByteType());
		//img = SimplifiedIO.openImage("/home/eugene/workspace/linetest_horz.tif", new UnsignedByteType());
		//final ImagePlus imp = IJ.openImage(		"/home/eugene/workspace/ExM_MT.tif");	
		//img = ImageJFunctions.wrapByte( imp );
		//img = SimplifiedIO.openImage(
		//		test_BVV_inteface.class.getResource( "/t1-head.tif" ).getFile(),
		//		new UnsignedByteType() );
	

		img.min(btdata.nDimIni[0]);
		img.max(btdata.nDimIni[1]);
		img.min(btdata.nDimCurr[0]);
		img.max(btdata.nDimCurr[1]);


		roiManager = new RoiManager3D(this);
		init(0.25*Math.min(btdata.nDimIni[1][2], Math.min(btdata.nDimIni[1][0],btdata.nDimIni[1][1])));
		
		
		
		
		//FlatIntelliJLaf.setup();
	
		
		
		try {
		    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

//		bdv.getBdvHandle().getKeybindings().removeInputMap( "BigTrace" );

	}
	
	public void initOriginAndBox(double axis_length)
	{
		int i;
		//basis vectors 
		RealPoint basis = new RealPoint(-0.1*axis_length, -0.1*axis_length,-0.1*axis_length);				
		for(i=0;i<3;i++)
		{		
			ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();
			point_coords.add(new RealPoint(basis));
			//origin_data.addPointToActive(basis);
			basis.move(axis_length, i);
			//origin_data.addPointToActive(basis);
			point_coords.add(new RealPoint(basis));
			basis.move((-1.0)*axis_length, i);
			float [] color_orig = new float[3];
			color_orig[i] = 1.0f;
			originVis.add(new VisPolyLineSimple( point_coords, 3.0f,new Color(color_orig[0],color_orig[1],color_orig[2])));						
		}
		float [][] nDimBox = new float [2][3];
		
		for(i=0;i<3;i++)
		{
			//why is this shift?! I don't know,
			// but looks better like this
			nDimBox[0][i]=btdata.nDimIni[0][i]+0.5f;
			nDimBox[1][i]=btdata.nDimIni[1][i]-1.0f;
		}
		volumeBox = new Cube3D(nDimBox,0.5f,0.0f,Color.LIGHT_GRAY,Color.LIGHT_GRAY);
	}
	public void init(double origin_axis_length)
	{
		
		initOriginAndBox(origin_axis_length);

		// init bigvolumeviewer
		final Img< UnsignedByteType > imgx = ArrayImgs.unsignedBytes( new long[]{ 2, 2, 2 } );
		view =				 
				 Views.interval( imgx, new long[] { 0, 0, 0 }, new long[]{ 1, 1, 1 } );
		
						
		bvv = BvvFunctions.show( view, "empty" ,Bvv.options().dCam(btdata.dCam).dClipNear(btdata.dClipNear).dClipFar(btdata.dClipFar).renderWidth( 800).renderHeight( 800 ));	
		bvv.setActive(false);
		panel=bvv.getBvvHandle().getViewerPanel();
		//polyLineRender = new VisPolyLineSimple();
		panel.setRenderScene(this::renderScene);
		
		installActions();
		resetViewXY(true);
	}
	private void createAndShowGUI() 
	{
		btpanel = new BigTraceControlPanel(this, btdata,roiManager);
		finFrame = new JFrame("BigTrace");
		//btpanel.frame = new JFrame("BigTrace");
		//btpanel.setName("TEEEST");
		//btpanel.frame = (BigTraceControlPanel)new JFrame("BigTrace");
		btpanel.bvv_frame=(JFrame) SwingUtilities.getWindowAncestor(bvv.getBvvHandle().getViewerPanel());
	 	
	 	//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//btpanel.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		finFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		btpanel.bvv_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        //Create and set up the content pane.
        //JComponent newContentPane = btframe;
        //newContentPane.setOpaque(true); //content panes must be opaque
        //frame.setContentPane(newContentPane);
		//btpanel.frame.add(btpanel);
		finFrame.add(btpanel);
        //Display the window.
		finFrame.setSize(400,600);
        //frame.pack();
		finFrame.setVisible(true);
	    java.awt.Point bvv_p = btpanel.bvv_frame.getLocationOnScreen();
	    java.awt.Dimension bvv_d = btpanel.bvv_frame.getSize();
	
	    finFrame.setLocation(bvv_p.x+bvv_d.width, bvv_p.y);
	    finFrame.addWindowListener(this);
	    btpanel.bvv_frame.addWindowListener(this);
	    //finFrame.addWindowStateListener(this);
	    //addWindowListener(addWindowListener(this));
	}
	
	/** find a brightest pixel in the direction of a click
	 *  and add a new 3D point to active ROI OR
	 *  start a new ROI (if none selected)
	 **/ 
	public void actionAddPoint()
	{
		
		if(!bInputLock)
		{
			//addPoint();
			RealPoint target = new RealPoint(3);
			if(!bTraceMode)
			{
				if(findPointLocationFromClick(currentView, btdata.nHalfClickSizeWindow,target))
				{
					//point or line
					if(roiManager.mode<=RoiManager3D.ADD_POINT_LINE)
					{
						roiManager.addPoint(target);
					}
					//semi auto tracing initialize
					else
					{
						bTraceMode= true;								
						roiManager.setLockMode(bTraceMode);
						
						//nothing selected, make a new tracing
						if(roiManager.activeRoi==-1)
						{
							roiManager.addSegment(target, null);																
							calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi());
						}
						else
						{
							int nRoiType = roiManager.getActiveRoi().getType();
							//continue tracing for the selected tracing
							if(nRoiType ==Roi3D.LINE_TRACE)
							{
								calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi());
							}
							//otherwise make a new tracing
							else
							{
								roiManager.addSegment(target, null);																
								calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi());
							}
						}
					}
				}
			}
			//continue to trace within the trace box
			else
			{
				if(findPointLocationFromClick(btdata.trace_weights, btdata.nHalfClickSizeWindow, target))
				{
					//run trace finding in a separate thread
					getSemiAutoTrace(target);

				}						
			}
		}
	}
	/** remove last added point from ROI
	 * (and delete ROI if it is the last point in it)
	 * **/
	public void actionRemovePoint()
	{
		if(!bInputLock)
		{
			if(!bTraceMode)
			{
				roiManager.removePointFromLine();
			}
			else
			{

				//if the last point in the tracing, leave tracing mode
				if(!roiManager.removeSegment())
				{
					btdata.nPointsInTraceBox--;
					roiManager.removeActiveRoi();
					bTraceMode= false;
					roiManager.setLockMode(bTraceMode);							
					removeTraceBox();
					if(btdata.nTraceBoxView==1)
					{
						bvv2.setActive(true);
					}
					
				}
				//not the last point, see if we need to move trace box back
				else
				{
					btdata.nPointsInTraceBox--;
					if(btdata.nPointsInTraceBox==0)
					{
						calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi());
					}
				}
				
			}
			panel.showMessage("Point removed");

		}					
	}
	/** deselects current ROI (and finishes tracing)
	 *   
	 * **/
	public void actionDeselect()
	{
		if(!bInputLock)
		{
			if(!bTraceMode)
			{
				roiManager.unselect();
			}
			else
			{
				roiManager.unselect();
				bTraceMode= false;
				roiManager.setLockMode(bTraceMode);	
				removeTraceBox();
			}
		}
	}
	/** reverses order of points/segments in PolyLine and LineTrace,
	 * so the active end (where point addition happens) is reversed **/
	public void actionReversePoints() 
	{
		
		if(!bInputLock)
		{
			if(roiManager.activeRoi>=0)
			{
				int nRoiType = roiManager.getActiveRoi().getType();
				//continue tracing for the selected tracing
				if(nRoiType ==Roi3D.POLYLINE)
				{
					roiManager.getActiveRoi().reversePoints();
					
				}
				
				if(nRoiType ==Roi3D.LINE_TRACE)
				{
					roiManager.getActiveRoi().reversePoints();
					if(bTraceMode)
					{
						calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi());
						btdata.nPointsInTraceBox=1;
					}
				}							
			}

		}
	}
	/** move trace box to a position around current last added point **/
	public void actionMoveTraceBox()
	{
		if(!bInputLock)
		{
			if(bTraceMode && btdata.nPointsInTraceBox>1)
			{
				calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi());
				btdata.nPointsInTraceBox=1;
			}
		}
	}
	/** in a trace mode build a straight (not a curved trace) segment 
	 * connecting clicked and last point (to overcome semi-auto errors)**/
	public void actionSemiTraceStraightLine()
	{
		if(!bInputLock)
		{
			if(bTraceMode)
			{
				//make a straight line
				RealPoint target = new RealPoint(3);							
				if(findPointLocationFromClick(btdata.trace_weights, btdata.nHalfClickSizeWindow, target))
				{								
					roiManager.addSegment(target, 
							VolumeMisc.BresenhamWrap(roiManager.getLastTracePoint(),target));
					btdata.nPointsInTraceBox++;
				}
			}
		}
	}
	public void installActions()
	{
		final Actions actions = new Actions( new InputTriggerConfig() );
		actions.runnableAction(() -> actionAddPoint(),	            "add point", "F" );
		actions.runnableAction(() -> actionRemovePoint(),       	"remove point",	"G" );
		actions.runnableAction(() -> actionDeselect(),	            "deselect", "H" );
		actions.runnableAction(() -> actionReversePoints(),         "reverse curve point order","Y" );
		actions.runnableAction(() -> actionMoveTraceBox(),          "move trace box", "T" );
		actions.runnableAction(() -> actionSemiTraceStraightLine(),	"straight line semitrace", "R" );
						
		actions.runnableAction(
				() -> {
					resetViewXY(false);
				},
				"reset view XY",
				"1" );
			actions.runnableAction(
				() -> {
					resetViewYZ();
				},
				"reset view YZ",
				"2" );
			

		//actions.namedAction(action, defaultKeyStrokes);
		actions.install( bvv.getBvvHandle().getKeybindings(), "BigTrace actions" );
		
		actions.runnableAction(
				() -> {
					
					AffineTransform3D transform = new AffineTransform3D();
				
					panel.state().getViewerTransform(transform);
					int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
					int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
					int [] bothXY = new int [2];
					bothXY[0]=sW;
					bothXY[1]=sH;
					Matrix4f matPerspWorld = new Matrix4f();
					MatrixMath.screenPerspective( btdata.dCam, btdata.dClipNear, btdata.dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
					
					//center of the screen in the transformed coordinates
					//take coordinates in original data volume space
					Vector3f temp = new Vector3f(); 
					matPerspWorld.unproject(0.5f*sW,0.5f*sH,0.95f, //z=1 ->far from camera z=0 -> close to camera
							new int[] { 0, 0, sW, sH },temp);
					RealPoint target = new RealPoint(3);
					for(int i=0;i<3;i++)
					{
						target.setPosition(temp.get(i), i);
					}
					roiManager.addPoint(target);
					
				},
				"test CENTER",
				"A" );

	}

	public void calcShowTraceBox(final LineTrace3D trace)
	{
		long[][] rangeTraceBox;
		
		if(trace.numVertices()==1)
		{
			rangeTraceBox = getTraceBoxCentered(currentView,btdata.lTraceBoxSize, trace.vertices.get(0));
		}
		else
		{
			rangeTraceBox = getTraceBoxNext(currentView,btdata.lTraceBoxSize, btdata.fTraceBoxShift, trace);
		}
		
		IntervalView<UnsignedByteType> traceInterval = Views.interval(currentView, rangeTraceBox[0], rangeTraceBox[1]);
		
		//getCenteredView(traceInterval);
		panel.setTransformAnimator(getCenteredView(traceInterval));
		//long start1, end1;

		//start1 = System.currentTimeMillis();
		//calcWeightVectrosCorners(traceInterval, sigmaGlob);
		//end1 = System.currentTimeMillis();
		bInputLock = true;
		TraceBoxMath calcTask = new TraceBoxMath();
		calcTask.input=traceInterval;
		calcTask.bt=this;
		calcTask.addPropertyChangeListener(btpanel);
		calcTask.execute();
		//System.out.println("+corners: elapsed Time in milli seconds: "+ (end1-start1));		

		//showTraceBox(btdata.trace_weights);
		btdata.nPointsInTraceBox = 1;
	}

	
	//public ArrayList<RealPoint> getSemiAutoTrace(RealPoint target)
	public void getSemiAutoTrace(RealPoint target)
	{
		
		bInputLock = true;
		TracingBG traceBG = new TracingBG();
		traceBG.target = target;
		traceBG.bt=this;
		traceBG.addPropertyChangeListener(btpanel);
		traceBG.execute();
		return ;
		
	}

	
	
	void showCorners(ArrayList<long []> corners)
	{
		roiManager.mode=RoiManager3D.ADD_POINT;
		for(int i=0;i<corners.size();i++)
		{
			RealPoint vv = new RealPoint(0.,0.,0.);
			vv.setPosition(corners.get(i));
			roiManager.addPoint(vv);	
		}
	}

	//gets a box around "target" with half size of range
	public long[][] getTraceBoxCentered(final IntervalView< UnsignedByteType > viewclick, final long range, final RealPoint target)
	{
		long[][] rangeM = new long[3][3];
		int i;
		float [] pos = new float[3];
		target.localize(pos);
		for(i=0;i<3;i++)
		{
			rangeM[0][i]=(long)(pos[i])-range ;
			rangeM[1][i]=(long)(pos[i])+range;								
		}
		VolumeMisc.checkBoxInside(viewclick, rangeM);
		return rangeM;							
	}

	//gets a box around "target" with half size of range
	public long[][] getTraceBoxNext(final IntervalView< UnsignedByteType > viewclick, final long range, final float fFollowDegree, LineTrace3D trace)
	{
		long[][] rangeM = new long[3][3];
		int i;
		double [] pos = new double[3];
		double [] beforeLast = new double[3];
		
		
		//get centered box
		trace.vertices.get(trace.vertices.size()-1).localize(pos);		
		for(i=0;i<3;i++)
		{
			rangeM[0][i]=(long)(pos[i])-range ;
			rangeM[1][i]=(long)(pos[i])+range;								
		}
		//now shift it in the last direction of the trace to fFollowDegree
		ArrayList<RealPoint> lastSegment =trace.getLastSegment();
		lastSegment.get(lastSegment.size()-2).localize(beforeLast);
		LinAlgHelpers.subtract(pos, beforeLast, pos);
		LinAlgHelpers.normalize(pos);
		for(i=0;i<3;i++)
		{
			rangeM[0][i]+=(long)(pos[i]*range*fFollowDegree);
			rangeM[1][i]+=(long)(pos[i]*range*fFollowDegree);								
		}		
		
		VolumeMisc.checkBoxInside(viewclick, rangeM);
		return rangeM;							
	}
	
	public SimilarityTransformAnimator getCenteredView(IntervalView<UnsignedByteType> inInterval)
	{
		int i;
		int nDim = inInterval.numDimensions();
		final long [] minDim = inInterval.minAsLongArray();
		final long [] maxDim = inInterval.maxAsLongArray();
		float [] centerCoord = new float[nDim];
		
		for(i=0;i<nDim;i++)
		{
			centerCoord[i] = (float)Math.round(minDim[i]+ 0.5*(maxDim[i]-minDim[i]));
		}
		
		AffineTransform3D transform = new AffineTransform3D();
		AffineTransform3D transform_new = new AffineTransform3D();
		panel.state().getViewerTransform(transform);
		panel.state().getViewerTransform(transform_new);
		//coordinates in the current transform view
		transform.apply(centerCoord, centerCoord);
		panel.state().getViewerTransform(transform);
		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		int [] bothXY = new int [2];
		bothXY[0]=sW;
		bothXY[1]=sH;
		Matrix4f matPerspWorld = new Matrix4f();
		MatrixMath.screenPerspective( btdata.dCam, btdata.dClipNear, btdata.dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		
		
		//center of the screen in the transformed coordinates
		//take coordinates in original data volume space

		/*
		Vector3f position = new Vector3f();
		position.set(centerCoord);
		Vector3f winCoordsDest = new Vector3f();
		matPerspWorld.project(position, new int[] { 0, 0, sW, sH },winCoordsDest);
		*/
		
		Vector3f temp = new Vector3f();
		matPerspWorld.unproject(0.5f*sW,0.5f*sH,0.95f, //z=1 ->far from camera z=0 -> close to camera
				new int[] { 0, 0, sW, sH },temp);
		//double scale;
		//transform_new.identity();
		float [] newCent = new float[3];
		for(i=0;i<3;i++)
		{
			newCent[i] = temp.get(i);
		}
		transform.apply(newCent, newCent);
		double [] dl = transform_new.getTranslation();
		
		//translation
		for(i=0;i<3;i++)
		{
			//scale = transform.get(i, i);
			//transform_new.set(0.5*bothXY[i]-(double)scale*centerCoord[i], i, 3);
			//transform_new.set(tempC-(temp.get(i)-centerCoord[i]), i, 3);
			//transform_new.set(tempC+(temp.get(i)-centerCoord[i])/scale, i, 3);
			dl[i]+= (newCent[i]-centerCoord[i]);
		}
		transform_new.setTranslation(dl);
		//transform_new.concatenate(transform);
		//double scale = 1.0;
		//transform_new.set(scale, 0.0, 0.0, 0.5*sW+(double)(-1)*centerCoord[0], 0.0, scale, 0.0, 0.5*sH+(double)(-1)*centerCoord[1], 0.0, 0.0, scale,0.0);//(double)centerCoord[2]);// (-0.5)*scale*(double)nD);
		//panel.state().setViewerTransform(transform_new);
		//panel.requestRepaint();
		/*
		for(i=0;i<2;i++)
		{
			//dl[i]-=winCoordsDest.get(i)-0.5f*bothXY[i];
		
		}
		double scale = transform.get(0, 0);
		double dx = 0.5f*bothXY[0]-winCoordsDest.get(0);
		dl[0]+=dx;
		//dl[0]-=100;
		scale = transform.get(1, 1);
		double dy = 0.5f*bothXY[1]-winCoordsDest.get(1);
		dl[1]-=dy;
		//dl[1]+=100;
		*/

		Matrix4f matPerspWorldNew = new Matrix4f();
		MatrixMath.screenPerspective( btdata.dCam, btdata.dClipNear, btdata.dClipFar, sW, sH, 0, matPerspWorldNew ).mul( MatrixMath.affine( transform_new, new Matrix4f() ) );

		Vector3f position = new Vector3f();
		position.set(centerCoord);
		
		Vector3f winCoordsDest = new Vector3f();
		matPerspWorldNew.project(position, new int[] { 0, 0, sW, sH }, winCoordsDest);
		
		//transform_new.apply(centerCoord, centerCoord);
		SimilarityTransformAnimator anim = new SimilarityTransformAnimator(transform,transform_new,0,0,300);
		
		//panel.setTransformAnimator(anim);
		
		return anim;
	}

	public void showTraceBox(IntervalView<UnsignedByteType> weights)
	{

		// there is a trace box already, let's remove it
		if(bvv_trace!=null)
		{
			bvv_trace.removeFromBdv();
			System.gc();
		}
		//there is no tracebox, let's dim the main volume first
		else
		{
			if(btdata.nTraceBoxView==1)
			{
				//bvv2.setActive(false);
				btdata.bBrightnessRange[0]=bvv2.getConverterSetups().get(0).getDisplayRangeMin();
				btdata.bBrightnessRange[1]=bvv2.getConverterSetups().get(0).getDisplayRangeMax();
				bvv2.setDisplayRange(0.0, 0.0);
				//bvv2.getConverterSetups().get(0).setDisplayRange(0.0, 0.0);
			}
			
		}
		bvv_trace = BvvFunctions.show(weights, "weights", Bvv.options().addTo(bvv));
		bvv_trace.setCurrent();
		bvv_trace.setDisplayRange(0., 150.0);
		//handl.setDisplayMode(DisplayMode.SINGLE);
	}
	
	/** removes tracebox from BVV **/
	public void removeTraceBox()
	{

	
		if(bvv_trace!=null)
		{
			bvv_trace.removeFromBdv();
			System.gc();
		}
		bvv_trace=null;
		//handl.setDisplayMode(DisplayMode.SINGLE);
		if(btdata.nTraceBoxView==1)
		{

			bvv2.setDisplayRange(btdata.bBrightnessRange[0],btdata.bBrightnessRange[1]);
		}
	}	


	public void renderScene(final GL3 gl, final RenderData data)
	{
		
		int [] screen_size = new int [] {(int)data.getScreenWidth(), (int) data.getScreenHeight()};
		//handl.setRenderScene( ( gl, data ) -> {
		Matrix4f pvm=new Matrix4f( data.getPv() );
		synchronized (roiManager)
		{
			roiManager.draw(gl, pvm, screen_size);
		}	
		
			//render the origin of coordinates
			if (btdata.bShowOrigin)
			{
				for (int i=0;i<3;i++)
				{
					originVis.get(i).draw(gl, pvm);
				}
			}
			
			//render a box around  the volume 
			if (btdata.bVolumeBox)
			{
				volumeBox.draw(gl, pvm, screen_size);
			}
		
			//render world grid			
			if(btdata.bShowWorldGrid)
			{
/*
				int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
				int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
				Matrix4f  matPerspWorld = new Matrix4f();
				MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, matPerspWorld );
				ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();
				//for (int i =0; i<sW; i+=40)
				float [] world_grid_color = new float[]{0.5f,0.5f,0.5f}; 
				float world_grid_thickness = 1.0f;
	
				//bottom grid
				float i;
				//vertical
				float j=(float)sH;
				float di=3.0f*(float)sW/20.0f;
				for (i =-sW; i<2*sW; i+=di)
				{
					point_coords.add(new RealPoint(i,j,(float)(-1.0*dClipNear)));
					point_coords.add(new RealPoint(i,j,(float)(1.0*dClipFar)));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grid_thickness );
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
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grid_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}
				//middle grid
				// vertical
				for (i =-sW; i<2*sW; i+=di)
				{
					point_coords.add(new RealPoint(i,0,(float)(dClipFar*0.99)));
					point_coords.add(new RealPoint(i,sH,(float)(dClipFar*0.99)));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grid_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}	
				//horizontal
				float dj=(float)sH/10.0f;
				for (j =0; j<=sH; j+=dj)
				{
					point_coords.add(new RealPoint((float)(-sW),j,(float)(dClipFar*0.99)));
					point_coords.add(new RealPoint((float)2*sW,j,(float)(dClipFar*0.99)));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grid_thickness );
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
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grid_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}
				//horizontal			
				for (k= (float)(dClipFar*0.99); k>=(-1.0*dClipNear); k-=dk)
				{
					point_coords.add(new RealPoint((float)(-sW),j,k));
					point_coords.add(new RealPoint((float)2*sW,j,k));
					VisPolyLineSimple lines = new VisPolyLineSimple(world_grid_color, point_coords, world_grid_thickness );
					lines.draw( gl, matPerspWorld);
					point_coords.clear();
				}
				*/
			}
			
		//} );

		panel.requestRepaint();

	}
	public void resetViewXY(boolean firstCall)
	{
		
		double scale;
		int nW= (int)btdata.nDimIni[1][0];
		int nH= (int)btdata.nDimIni[1][1];
		int nD= (int)btdata.nDimIni[1][2];
		
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
		t.set(scale, 0.0, 0.0, 0.5*((double)sW-scale*(double)nW), 0.0, scale, 0.0, 0.5*((double)sH-scale*(double)nH), 0.0, 0.0, scale,0.0);// (-0.5)*scale*(double)nD);

		//t.identity();
		//t.set((double)0.5*sH,1,3);

		if(!firstCall)
		{
			SimilarityTransformAnimator anim = new SimilarityTransformAnimator(panel.state().getViewerTransform(),t,0,0,400);
			
			panel.setTransformAnimator(anim);
			
		}
		else
		{
			panel.state().setViewerTransform(t);
			currentView=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
			bvv2 = BvvFunctions.show( currentView, "cropreset", Bvv.options().addTo(bvv));
			panel.requestRepaint();
		}

//        RealRandomAccessible<UnsignedByteType> rra = new Procedural3DImageByte(
//                p -> {
//                	 
//                	 int maxIterations= 50;
//                	 double n=2;
//                	 //double n=3;
//                	 int i = 0;
//                	 double x= 0;
//                	 double y= 0;
//                	 double z= 0;
//                     double r, theta, phi;
//                     double a = (p[0]/btdata.nDimIni[1][0])*2.47 - 2.0;
//                     double b = (p[1]/btdata.nDimIni[1][1])*2.24-1.12;
//                     double c = (p[2]/btdata.nDimIni[1][2]-0.5);
//                     //double c = 2.0*(p[2]/btdata.nDimIni[1][2]-0.5);
//
//                     double dV = 0;
//                     
//                	 for ( ; i <= maxIterations; ++i )
//                	 {
//                		 r = Math.sqrt(x*x+y*y+z*z);
//                		 theta = Math.atan2(z,Math.sqrt(x*x+y*y));
//                		 phi = Math.atan2(y,x);
//                		 x = Math.pow(r,n) * Math.cos(n*phi) * Math.cos(n*theta)+a;
//                		 y = Math.pow(r,n) * Math.sin(n*phi) * Math.cos(n*theta)+b;
//                		 z = Math.pow(r,n) *  Math.sin(n*theta)+c;
//                		 dV=x*x +y*y+z*z;
//                		 if ( dV>8 )
//                             break;
//                	 }
//
//
//                    return (int)Math.round(255*i/maxIterations);
//                }
//        ).getRRA();
//        RandomAccessibleOnRealRandomAccessible<UnsignedByteType> RAc = new RandomAccessibleOnRealRandomAccessible<UnsignedByteType>(rra);
//        currentView = Views.interval( RAc, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );	
//        bvv2 = BvvFunctions.show( currentView, "cropreset", Bvv.options().addTo(bvv));
	}
	
	public void resetViewYZ()
	{
		
		double scale;
		int nH= (int)btdata.nDimIni[1][1];
		int nD= (int)btdata.nDimIni[1][2];
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
	
		t.rotate(1, (-1)*Math.PI/2.0);

		t2.set(scale, 0.0, 0.0, 0.0, 0.0, scale, 0.0,0.5*((double)sH-scale*(double)nH) , 0.0, 0.0, scale, -0.5*((double)sW+scale*(double)nD));
		t.concatenate(t2);

		
		panel=bvv.getBvvHandle().getViewerPanel();
		
		SimilarityTransformAnimator anim = new SimilarityTransformAnimator(panel.state().getViewerTransform(),t,0,0,300);
		
		panel.setTransformAnimator(anim);

	}
	
	public boolean findPointLocationFromClick(final IntervalView< UnsignedByteType > viewclick, final int nHalfWindowSize, final RealPoint target)
	{
		int i,j;

		java.awt.Point point_mouse  = bvv.getBvvHandle().getViewerPanel().getMousePosition();
		System.out.println( "click x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
														
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		panel.state().getViewerTransform(transform);
		int sW = bvv.getBvvHandle().getViewerPanel().getWidth();
		int sH = bvv.getBvvHandle().getViewerPanel().getHeight();
		Matrix4f matPerspWorld = new Matrix4f();
		MatrixMath.screenPerspective( btdata.dCam, btdata.dClipNear, btdata.dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		
		
		ArrayList<RealPoint> clickFrustum = new ArrayList<RealPoint> ();
		Vector3f temp = new Vector3f(); 
		
		//float [] zVals = new float []{0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f};
		for (i = -nHalfWindowSize;i<3*nHalfWindowSize;i+=2*nHalfWindowSize)
			for (j = -nHalfWindowSize;j<3*nHalfWindowSize;j+=2*nHalfWindowSize)
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
		Cuboid3D dataCube = new Cuboid3D(btdata.nDimCurr); 
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
			return false;
		}
		long [][] nClickMinMax = new long[2][3];
		
		if(VolumeMisc.newBoundBox(viewclick, intersectionPoints, nClickMinMax))
		{
			/*
			//show volume that was cut-off
			bvv2.removeFromBdv();
			System.gc();
			view2=Views.interval( img, nClickMinMax[0], nClickMinMax[1]);	
		
			bvv2 = BvvFunctions.show( view2, "cropclick", Bvv.options().addTo(bvv));
			*/
			
			
			IntervalView< UnsignedByteType > intRay = Views.interval(viewclick, Intervals.createMinMax(nClickMinMax[0][0],nClickMinMax[0][1],nClickMinMax[0][2],
																								   nClickMinMax[1][0],nClickMinMax[1][1],nClickMinMax[1][2]));
			
			//double [][] singleCube  = new double [2][3];
			//for(i=0;i<3;i++)
			//	singleCube[1][i]=1.0;
			//Cuboid3D clicktest = new Cuboid3D(singleCube);
			//Cuboid3D clickVolume = new Cuboid3D(intersectionPoints);
			Cuboid3D clickVolume = new Cuboid3D(clickFrustum);
			clickVolume.iniFaces();
			RealPoint target_found = new RealPoint( 3 );
			//RealPoint locationMax = new RealPoint( 3 );
			
			if(VolumeMisc.findMaxLocationCuboid(intRay,target_found,clickVolume))
			{
				//traces.addPointToActive(target);
				panel.showMessage("point found");
				target.setPosition(target_found);
				return true;
				//roiManager.addPoint(target);
				//roiManager.addPointToLine(target);
			}
			else
			{
				panel.showMessage("not found :(");
				return false;
			}
				

						
		}
		else
		{
			return false;
		}

		//render_pl();		
		
	}
	



	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		//System.out.println("yay1");
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
		//System.out.println("yay");
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		closeWindows();
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		//System.out.println("yay3");
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		//System.out.println("yay4");
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		//System.out.println("yay5");
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		//System.out.println("yay6");
	}

	
	public void closeWindows()
	{
		if(bvv_trace!=null)
		{
			bvv_trace.removeFromBdv();
			System.gc();
		}
		if(bvv2!=null)
		{
			bvv2.removeFromBdv();
		}
		if(bvv!=null)
		{
			bvv.removeFromBdv();
		}
		//panel.stop();
		btpanel.bvv_frame.dispose();		
		finFrame.dispose();
	}
	
	@SuppressWarnings( "rawtypes" )
	private static  < T extends NativeType< T > > ImgPlus< T > convertInput(ImgPlus img_in, RealType type)
	{
		RandomAccessibleInterval< T > convertedRAI = RealTypeConverters.convert( img_in, type);
		Img< T > convertedImg = ImgView.wrap( convertedRAI, new ArrayImgFactory<T>( convertedRAI.randomAccess().get().createVariable() ) );
		return new ImgPlus<>( convertedImg, img_in );
	}

	
	public static void main( String... args) throws Exception
	{
		
		/*
		Class<?> clazz = BigTrace.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		java.io.File file = new java.io.File(url.toURI());
		System.setProperty("plugins.dir", file.getAbsolutePath());
		
		new ImageJ();*/
		BigTrace testI=new BigTrace(); 
		
		testI.run("/home/eugene/Desktop/BigTrace_data/ExM_MT_8bit.tif");
		//testI.run("");
		
		
	}
	
}

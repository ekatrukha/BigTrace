
package bigtrace;



import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.Insets;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.jogamp.opengl.GL3;

import bdv.viewer.DisplayMode;
import bigtrace.geometry.Cuboid3D;
import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Line3D;
import bigtrace.gui.CropPanel;
import bigtrace.gui.PanelTitle;
import bigtrace.math.DerivConvolutionKernels;
import bigtrace.math.DijkstraBinaryHeap;
import bigtrace.math.DijkstraFibonacciHeap;
import bigtrace.math.DijkstraFHRestricted;
import bigtrace.math.EigenValVecSymmDecomposition;
import bigtrace.polyline.BTPolylines;
import bigtrace.rois.Cube3D;
import bigtrace.rois.LineTracing3D;
import bigtrace.rois.RoiManager3D;
import bigtrace.scene.VisPointsScaled;
import bigtrace.scene.VisPointsSimple;
import bigtrace.scene.VisPolyLineSimple;
import bigtrace.volume.VolumeMisc;
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
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;

import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.realtransform.AffineTransform3D;

import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.simplifiedio.SimplifiedIO;
import tpietzsch.example2.RenderData;
import tpietzsch.example2.VolumeViewerFrame;
import tpietzsch.example2.VolumeViewerPanel;
import tpietzsch.util.MatrixMath;


public class BigTrace
{
	public  BvvStackSource< UnsignedByteType > bvv;
	public  BvvStackSource< UnsignedByteType > bvv2;
	public  BvvStackSource< UnsignedByteType > bvv_trace = null;
	RandomAccessibleInterval< UnsignedByteType > view;
	IntervalView< UnsignedByteType > view2 = null;
	
	
	IntervalView< UnsignedByteType > trace_weights = null;
	IntervalView< FloatType > trace_vectors=null;
	ArrayList<long []> jump_points = null;
	boolean bTraceMode = false;
	
	Img< UnsignedByteType> img;

	VolumeViewerPanel panel;
	VolumeViewerFrame frame;
	
	
	CropPanel cropPanel;

	/** visualization of coordinates origin axes **/
	ArrayList<VisPolyLineSimple> originVis = new ArrayList<VisPolyLineSimple>();

	/** box around volume **/
	Cube3D volumeBox;
	
	/** half size of rectangle around click point (in screen pixels)
	 * used to find maximim intensity **/
	int nHalfClickSizeWindow = 5;

	/** characteristic size (SD) of lines (for now in all dimensions)**/
	double sigmaGlob = 3.0;
	/** half size of tracing box (for now in all dimensions) **/
	long lTraceBoxSize = 50;
	/** How much the tracebox will follow the last direction of trace:
	 * in the range [0..1], 0 = no following (center), 1 = previous point is at the edge of the box**/
	float fTraceBoxShift = 0.9f;

	/** whether (1) or not (0) remove visibility of volume data during tracing **/
	int nTraceBoxView = 1;
	
	
	long [][] nDimCurr = new long [2][3];
	long [][] nDimIni = new long [2][3];
	double dCam = 1100.;
	double dClipNear = 1000.;
	double dClipFar = 1000.;
	
	boolean bShowWorldGrid = false;
	boolean bShowOrigin = true;
	boolean bVolumeBox = true;
	DijkstraFHRestricted dijkRBegin;
	DijkstraFHRestricted dijkREnd;
	//DijkstraBinaryHeap dijkBH;
	//DijkstraFibonacciHeap dijkFib;

	
	//ArrayList< RealPoint > point_coords = new ArrayList<>();
	BTPolylines traces = new BTPolylines ();
	

	RoiManager3D roiManager = new RoiManager3D();
	JProgressBar progressBar;
		
	public void runBVV()
	{
		
		
		//img = SimplifiedIO.openImage(
					//test_BVV_inteface.class.getResource( "home/eugene/workspace/ExM_MT.tif" ).getFile(),
					//new UnsignedByteType() );
		img = SimplifiedIO.openImage("/home/eugene/workspace/ExM_MT-1.tif", new UnsignedByteType());
		//img = SimplifiedIO.openImage("/home/eugene/workspace/linetest_horz.tif", new UnsignedByteType());
		//final ImagePlus imp = IJ.openImage(		"/home/eugene/workspace/ExM_MT.tif");	
		//img = ImageJFunctions.wrapByte( imp );
		//img = SimplifiedIO.openImage(
		//		test_BVV_inteface.class.getResource( "/t1-head.tif" ).getFile(),
		//		new UnsignedByteType() );
	

		img.min(nDimIni[0]);
		img.max(nDimIni[1]);
		img.min(nDimCurr[0]);
		img.max(nDimCurr[1]);



		init(0.25*Math.min(nDimIni[1][2], Math.min(nDimIni[1][0],nDimIni[1][1])));
		
		try {
		    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		
		
		   javax.swing.SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	            	initMainDialog();
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
			nDimBox[0][i]=nDimIni[0][i]+0.5f;
			nDimBox[1][i]=nDimIni[1][i]-1.0f;
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
		
						
		bvv = BvvFunctions.show( view, "empty" ,Bvv.options().dCam(dCam).dClipNear(dClipNear).dClipFar(dClipFar));	
		bvv.setActive(false);
		panel=bvv.getBvvHandle().getViewerPanel();
		//polyLineRender = new VisPolyLineSimple();
		panel.setRenderScene(this::renderScene);
		installActions();
		resetViewXY(true);
	}
	
	public void initMainDialog()
	{
		//Interface
		
		//CropPanel
		//cropPanel = new CropPanel( nW-1, nH-1, nD-1);
		cropPanel = new CropPanel(nDimIni[1]);
		
		cropPanel.addCropPanelListener(new CropPanel.Listener() {

		/*	@Override
			public void nearFarChanged(int near, int far) {
				// TO DO Auto-generated method stub
				//VolumeViewer
				dClipNear = Math.abs(near);
				dClipFar = (double)far;
				bvv.getBvvHandle().getViewerPanel().setCamParams(dCam, dClipNear, dClipFar);
				handl.requestRepaint();
				//handl.state().setViewerTransform(transform);
				
			}
			*/

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				
				if(bbx0>=0 && bby0>=0 &&bbz0>=0 && bbx1<=(nDimIni[1][0]) && bby1<=(nDimIni[1][1]) && bbz1<=(nDimIni[1][2]))
				{
					nDimCurr[0]=new long[] { bbx0, bby0, bbz0 };
					nDimCurr[1]=new long[] { bbx1, bby1, bbz1 };
					
					bvv2.removeFromBdv();
					
					System.gc();
					view2=Views.interval( img, nDimCurr[0], nDimCurr[1] );
					
					bvv2 = BvvFunctions.show( view2, "cropresize", Bvv.options().addTo(bvv));
					//bvv2.getConverterSetups().get(0).setColor(new ARGBType( ARGBType.rgba(0, 0, 255, 255)));										
				}
			}
		
		});
		
		
		JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);
		
		JPanel panNavigation = new JPanel(new GridBagLayout());
	    
		GridBagConstraints c = new GridBagConstraints();

		ClassLoader classLoader = getClass().getClassLoader();

		//View Panel
	  
		JPanel panView=new JPanel(new GridBagLayout()); 
		panView.setBorder(new PanelTitle(" View "));
		
		
		String icon_path = classLoader.getResource("icons/orig.png").getFile();
	    ImageIcon tabIcon = new ImageIcon(icon_path);
	    JToggleButton butOrigin = new JToggleButton(tabIcon);
	    butOrigin.setSelected(bShowOrigin);
	    butOrigin.setToolTipText("Show XYZ axes");
	    butOrigin.addItemListener(new ItemListener() {
	
	    	@Override
		public void itemStateChanged(ItemEvent e) {
	    	      if(e.getStateChange()==ItemEvent.SELECTED){
	    	    	  bShowOrigin=true;
	    	    	  //render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	bShowOrigin=false;
	    	        	//render_pl();
	    	        }
			}
	    	});
	    c.gridx=0;
	    c.gridy=0;
		panView.add(butOrigin,c);
		
		
		icon_path = classLoader.getResource("icons/boxvolume.png").getFile();
	    tabIcon = new ImageIcon(icon_path);
	    JToggleButton butVBox = new JToggleButton(tabIcon);
	    butVBox.setSelected(bVolumeBox);
	    butVBox.setToolTipText("Volume Box");
	    butVBox.addItemListener(new ItemListener() {
	
	    @Override
		public void itemStateChanged(ItemEvent e) {
	    	      if(e.getStateChange()==ItemEvent.SELECTED){
	    	    	  bVolumeBox=true;
	    	    	  //render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	bVolumeBox=false;
	    	        	//render_pl();
	    	        }
			}
	    	});
	    c.gridx++;
	    
		panView.add(butVBox,c);
		icon_path = classLoader.getResource("icons/worldgrid.png").getFile();
	    tabIcon = new ImageIcon(icon_path);
	    JToggleButton butWorld = new JToggleButton(tabIcon);
	    butWorld.setSelected(bShowWorldGrid);
	    butWorld.setToolTipText("World Grid");
	    butWorld.addItemListener(new ItemListener() {
	
	    @Override
		public void itemStateChanged(ItemEvent e) {
	    	      if(e.getStateChange()==ItemEvent.SELECTED){
	    	    	  bShowWorldGrid=true;
	    	    	  //render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	bShowWorldGrid=false;
	    	        	//render_pl();
	    	        }
			}
	    	});
	    c.gridx++;
		panView.add(butWorld,c);
		
		
		
		
		//Cropping Panel
		JPanel panCrop=new JPanel(new GridBagLayout()); 
		panCrop.setBorder(new PanelTitle(" Cropping "));

        icon_path = classLoader.getResource("icons/cube_icon.png").getFile();
	    tabIcon = new ImageIcon(icon_path);

	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.fill=GridBagConstraints.HORIZONTAL;
	    panCrop.add(cropPanel,c);
	    
	    
	    //add panels to Navigation
	    c.insets=new Insets(4,4,2,2);
	    //View
	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.gridwidth=1;
	    c.anchor = GridBagConstraints.WEST;
	    panNavigation.add(panView,c);
	    
	    //Crop
	    c.gridy++;	
	    panNavigation.add(panCrop,c);
	    
        // Blank/filler component
	    c.gridx++;
	    c.gridy++;
	    c.weightx = 0.01;
        c.weighty = 0.01;
        panNavigation.add(new JLabel(), c);

	    tabPane.addTab("",tabIcon,panNavigation, "View/Crop");

	    //ROI MANAGER
	    icon_path = classLoader.getResource("icons/node.png").getFile();
	    tabIcon = new ImageIcon(icon_path);
	    tabPane.addTab("",tabIcon ,roiManager,"Tracing");

	    roiManager.addRoiManager3DListener(new RoiManager3D.Listener() {

			@Override
			public void activeRoiChanged(int nRoi) {
				//render_pl();
			}
	    	
	    });
	    
	    
	    tabPane.setSize(350, 300);
	    tabPane.setSelectedIndex(1);

	    
	    progressBar = new JProgressBar(0,100);
	    //progressBar.setIndeterminate(true);
	    progressBar.setValue(0);
	    progressBar.setStringPainted(true);
		
	    JPanel finalPanel = new JPanel(new GridBagLayout());
	    GridBagConstraints cv = new GridBagConstraints();
	    cv.gridx=0;
	    cv.gridy=0;	    
	    cv.weightx=0.5;
	    cv.weighty=1.0;
	    cv.anchor = GridBagConstraints.NORTHWEST;
	    cv.gridwidth = GridBagConstraints.REMAINDER;
	    //cv.gridheight = GridBagConstraints.REMAINDER;
	    cv.fill = GridBagConstraints.HORIZONTAL;
	    cv.fill = GridBagConstraints.BOTH;
	    finalPanel.add(tabPane,cv);
	    cv.gridx=0;
	    cv.gridy=1;	    
	    cv.gridwidth = GridBagConstraints.RELATIVE;
	    cv.gridheight = GridBagConstraints.RELATIVE;
	    cv.weighty=0.01;
	    cv.anchor = GridBagConstraints.SOUTHEAST;
	    cv.fill = GridBagConstraints.HORIZONTAL;
	    finalPanel.add(progressBar,cv);
	    
	    JFrame bvv_frame=(JFrame) SwingUtilities.getWindowAncestor(bvv.getBvvHandle().getViewerPanel());
	    //final JDialog mainWindow = new JDialog(bvv_frame,"BigTrace");
	    
	    //mainWindow.add(finalPanel);
	    //mainWindow.setVisible(true);
	    //mainWindow.setSize(400, 500);
	    //mainWindow.requestFocusInWindow();
	    final JFrame frame = new JFrame( "BigTrace" );
	    //roiManager.setParentFrame(frame);
		frame.add(finalPanel);
	    frame.setSize(400,500);

	    java.awt.Point bvv_p = bvv_frame.getLocationOnScreen();
	    java.awt.Dimension bvv_d = bvv_frame.getSize();
	
	    frame.setLocation(bvv_p.x+bvv_d.width, bvv_p.y);

	    
	    frame.setVisible( true );
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	{
		    		panel.stop();
		    		bvv.close();
		            System.exit(0);
		        }
		    }
		});
		
		
	}
	public void installActions()
	{
		final Actions actions = new Actions( new InputTriggerConfig() );
		
		//find a brightest pixel in the direction of a click
		// (not really working properly yet
		actions.runnableAction(
				() -> {
					//addPoint();
					RealPoint target = new RealPoint(3);
					if(!bTraceMode)
					{
						if(findPointLocationFromClick(view2, nHalfClickSizeWindow,target))
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
								roiManager.setTraceMode(bTraceMode);
								roiManager.addSegment(target, null);																
								calcShowTraceBoxIni(target);
								if(nTraceBoxView==1)
								{
									bvv2.setActive(false);
								}
								
							}
							//render_pl();
						}
					}
					//continue to trace
					else
					{
						if(findPointLocationFromClick(trace_weights, nHalfClickSizeWindow, target))
						{
							ArrayList<RealPoint> trace = getSemiAutoTrace(target);							
							if(trace.size()>1)
							{
								roiManager.addSegment(target, trace);
								System.out.print("next trace!");
								//render_pl();
							}
						}						
					}
				},
				"add point",
				"F" );
		


		
		actions.runnableAction(
				() -> {
					if(!bTraceMode)
					{
						roiManager.removePointFromLine();
					}
					else
					{
						//roiManager.removeSegment();
						//if the last point, leave tracing mode
						if(!roiManager.removeSegment())
						{
							roiManager.removeActiveRoi();
							bTraceMode= false;
							roiManager.setTraceMode(bTraceMode);							
							removeTraceBox();
							if(nTraceBoxView==1)
							{
								bvv2.setActive(true);
							}
						}
						
					}
					//render_pl();
					panel.showMessage("Point removed");

					
				},
				"remove point",
				"G" );
		actions.runnableAction(
				() -> {
					if(!bTraceMode)
					{
						roiManager.unselect();
					}
					else
					{
						roiManager.unselect();
						bTraceMode= false;
						roiManager.setTraceMode(bTraceMode);	
						removeTraceBox();
						if(nTraceBoxView==1)
						{
							bvv2.setActive(true);
						}
					}
				},
				"new trace",
				"H" );
			actions.runnableAction(
					() -> {
						if(bTraceMode)
						{
							//make a straight line
							RealPoint target = new RealPoint(3);							
							if(findPointLocationFromClick(trace_weights, nHalfClickSizeWindow, target))
							{
								ArrayList<RealPoint> trace = new ArrayList<RealPoint>();
								trace.add(roiManager.getLastTracePoint());
								trace.add(target);
								//TODO add BRESENHAM 3D
								roiManager.addSegment(target, trace);
								//render_pl();
							}
						}
					},
					"straight line semitrace",
					"R" );		
		actions.runnableAction(
				() -> {
					if(bTraceMode)
					{
						removeTraceBox();
						calcShowTraceBoxNext((LineTracing3D)roiManager.getActiveRoi());
						//initTracing(roiManager.getLastTracePoint());
					}
				},
				"move trace box",
				"T" );
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
			

		//actions.namedAction(action, defaultKeyStrokes);
		actions.install( bvv.getBvvHandle().getKeybindings(), "BigTrace actions" );

	}
	
	public void calcShowTraceBoxIni(final RealPoint target)
	{
		long[][] rangeTraceBox = getTraceBoxCentered(view2,lTraceBoxSize,target);
		
		IntervalView<UnsignedByteType> traceInterval = Views.interval(view2, rangeTraceBox[0], rangeTraceBox[1]);
		long start1, end1;

		start1 = System.currentTimeMillis();
		calcWeightVectrosCorners(traceInterval, sigmaGlob);
		end1 = System.currentTimeMillis();
		System.out.println("+corners: elapsed Time in milli seconds: "+ (end1-start1));		
/*
		start1 = System.currentTimeMillis();
		float[] vDir =testDeriv(traceInterval, sigmaGlob, target);
		end1 = System.currentTimeMillis();
		System.out.println("Deriv part: elapsed Time in milli seconds: "+ (end1-start1));*/
		/*
		start1 = System.currentTimeMillis();
		dijkBH = new DijkstraBinaryHeap(trace_weights, target);
		end1 = System.currentTimeMillis();
		System.out.println("Dijkstra binary heap: elapsed Time in milli seconds: "+ (end1-start1));

		start1 = System.currentTimeMillis();
		dijkFib = new DijkstraFibonacciHeap(trace_weights, target);
		end1 = System.currentTimeMillis();
		System.out.println("Dijkstra fibonacci: elapsed Time in milli seconds: "+ (end1-start1));
		
		start1 = System.currentTimeMillis();
		dijkR = new DijkstraRestricted(trace_weights, target);
		end1 = System.currentTimeMillis();
		System.out.println("Dijkstra fibonacci: elapsed Time in milli seconds: "+ (end1-start1));
*/
		showTraceBox(trace_weights);

	}
	public void calcShowTraceBoxNext(final LineTracing3D trace)
	{
		long[][] rangeTraceBox = getTraceBoxNext(view2,lTraceBoxSize, fTraceBoxShift, trace);
		
		IntervalView<UnsignedByteType> traceInterval = Views.interval(view2, rangeTraceBox[0], rangeTraceBox[1]);
		long start1, end1;

		start1 = System.currentTimeMillis();
		calcWeightVectrosCorners(traceInterval, sigmaGlob);
		end1 = System.currentTimeMillis();
		System.out.println("+corners: elapsed Time in milli seconds: "+ (end1-start1));		

		showTraceBox(trace_weights);

	}

	
	public ArrayList<RealPoint> getSemiAutoTrace(RealPoint target)
	{
		ArrayList<RealPoint> trace = new ArrayList<RealPoint>(); 
		long start1, end1;
		boolean found_path_end;
		
		start1 = System.currentTimeMillis();
		//init Dijkstra from initial click point
		dijkRBegin = new DijkstraFHRestricted(trace_weights);
		found_path_end = dijkRBegin.calcCostTwoPoints(roiManager.getLastTracePoint(),target);
		end1 = System.currentTimeMillis();
		System.out.println("Dijkstra Restr search BEGIN: elapsed Time in milli seconds: "+ (end1-start1));

		//showCorners(dijkRBegin.exploredCorners(jump_points));
		//both points in the connected area
		if (found_path_end)
		{
			dijkRBegin.getTrace(target, trace);
			return trace;
		}
		//need to find shortcut through jumping points
		else
		{
			//showCorners(jump_points);
			// get corners in the beginning
			ArrayList<long []> begCorners = dijkRBegin.exploredCorners(jump_points);
			start1 = System.currentTimeMillis();
			dijkREnd = new DijkstraFHRestricted(trace_weights);
			dijkREnd.calcCost(target);
			end1 = System.currentTimeMillis();
			System.out.println("Dijkstra Restr search END: elapsed Time in milli seconds: "+ (end1-start1));
			ArrayList<long []> endCorners = dijkREnd.exploredCorners(jump_points);
			//there are corners (jump points) in the trace area
			// let's construct the path
			if(begCorners.size()>0 && endCorners.size()>0)
			{
				//find a closest pair of corners
				ArrayList<long []> pair = findClosestPoints(begCorners,endCorners);
				RealPoint pB = new RealPoint(3);
				RealPoint pE = new RealPoint(3);
				pB.setPosition(pair.get(0));
				pE.setPosition(pair.get(1));
				ArrayList<RealPoint> traceB = new ArrayList<RealPoint> (); 
				dijkRBegin.getTrace(pB, traceB);
				ArrayList<RealPoint> traceE = new ArrayList<RealPoint> ();
				dijkREnd.getTrace(pE, traceE);
				int i;
				//connect traces
				for(i=0;i<traceB.size();i++)
				{
					trace.add(traceB.get(i));
				}
				//TODO: insert connecting 3D bresenham here
				for(i=traceE.size()-1;i>=0 ;i--)
				
				{
					trace.add(traceE.get(i));
				}

			}
			//no corners, just do a straight line
			else
			{
				trace.add(roiManager.getLastTracePoint());
				//TODO: insert connecting 3D bresenham here
				trace.add(target);
			}
			return trace;
		}
		
		
	}
	
	public ArrayList<long []> findClosestPoints(ArrayList<long []> begCorners, ArrayList<long []> endCorners)
	{
		ArrayList<long []> pair = new ArrayList<long []> ();
		
		long minDist = Long.MAX_VALUE;
		long currDist,dL;
		int indB=0;
		int indE=0;
		long [] pB, pE;
		int i,j,k;
		for (i=0;i<begCorners.size();i++)
		{
			pB=begCorners.get(i);
			for (j=0;j<endCorners.size();j++)
			{
				pE=endCorners.get(j);
				currDist=0;
				for(k=0;k<pB.length;k++)
				{
					dL=pE[k]-pB[k];
					currDist+=dL*dL;
				}
				if(currDist<minDist)
				{
					minDist=currDist;
					indB=i;
					indE=j;
				}
			}
		}
		pair.add(begCorners.get(indB));
		pair.add(endCorners.get(indE));
		return pair;
	}
	
	public void calcWeightVectrosCorners(final IntervalView<UnsignedByteType> input, final double sigma)
	{
		int i;
		double [][] kernels;
		Kernel1D[] derivKernel;
		final long[] dim = Intervals.dimensionsAsLongArray( input );
		long[] minV = input.minAsLongArray();
		long[] nShift = new long [input.numDimensions()+1];
		
		for (i=0;i<input.numDimensions();i++)
		{
			nShift[i]=minV[i];
		}
		
		ArrayImg<FloatType, FloatArray> hessFloat = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 6 );
		IntervalView<FloatType> hessian = Views.translate(hessFloat, nShift);
		
		
		//ArrayImg<FloatType, FloatArray> gradient = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );
		
		//long start1, end1;
		
		
		int count = 0;
		int [] nDerivOrder;
		/**/
		Convolution convObj;
		//start1 = System.currentTimeMillis();
		final int nThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService es = Executors.newFixedThreadPool( nThreads );

		//second derivatives
		for (int d1=0;d1<3;d1++)
		{
			for ( int d2 = d1; d2 < 3; d2++ )
			{
				IntervalView< FloatType > hs2 = Views.hyperSlice( hessian, 3, count );
				nDerivOrder = new int [3];
				nDerivOrder[d1]++;
				nDerivOrder[d2]++;
				kernels = DerivConvolutionKernels.convolve_derive_kernel(sigma, nDerivOrder );
				derivKernel = Kernel1D.centralAsymmetric(kernels);
				convObj=SeparableKernelConvolution.convolution( derivKernel );
				convObj.setExecutor(es);
				convObj.process(Views.extendBorder(input), hs2 );
				//SeparableKernelConvolution.convolution( derivKernel ).process( input, hs2 );
				count++;
				System.out.println(count);
			}
		}
		//end1 = System.currentTimeMillis();
		//System.out.println("THREADED Elapsed Time in milli seconds: "+ (end1-start1));

		EigenValVecSymmDecomposition<FloatType> mEV = new EigenValVecSymmDecomposition<FloatType>(3);
		ArrayImg<FloatType, FloatArray> dV = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );
		ArrayImg<FloatType, FloatArray> sW = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ]);
		ArrayImg<FloatType, FloatArray> nC = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ]);
		IntervalView<FloatType> directionVectors =  Views.translate(dV, nShift);
		IntervalView<FloatType> salWeights =  Views.translate(sW, minV);
		IntervalView<FloatType> lineCorners =  Views.translate(nC, minV);
		
		mEV.computeVWCRAI(hessian, directionVectors,salWeights, lineCorners,nThreads,es);
		es.shutdown();
		trace_weights=VolumeMisc.convertFloatToUnsignedByte(salWeights,false);
		jump_points =VolumeMisc.localMaxPointList(VolumeMisc.convertFloatToUnsignedByte(lineCorners,false), 10);
		//showCorners(jump_points);
		showTraceBox(trace_weights);
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
		checkBoxInside(viewclick, rangeM);
		return rangeM;							
	}

	//gets a box around "target" with half size of range
	public long[][] getTraceBoxNext(final IntervalView< UnsignedByteType > viewclick, final long range, final float fFollowDegree, LineTracing3D trace)
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
		
		checkBoxInside(viewclick, rangeM);
		return rangeM;							
	}

	public void showTraceBox(IntervalView<UnsignedByteType> weights)
	{

	
		if(bvv_trace!=null)
		{
			bvv_trace.removeFromBdv();
			System.gc();
		}
		bvv_trace = BvvFunctions.show(weights, "weights", Bvv.options().addTo(bvv));
		bvv_trace.setCurrent();
		bvv_trace.setDisplayRange(0., 150.0);
		//handl.setDisplayMode(DisplayMode.SINGLE);
	}
	public void removeTraceBox()
	{

	
		if(bvv_trace!=null)
		{
			bvv_trace.removeFromBdv();
			System.gc();
		}
		bvv_trace=null;
		//handl.setDisplayMode(DisplayMode.SINGLE);
	}	


	public void renderScene(final GL3 gl, final RenderData data)
	{
		
		int [] screen_size = new int [] {(int)data.getScreenWidth(), (int) data.getScreenHeight()};
		//handl.setRenderScene( ( gl, data ) -> {
		synchronized (roiManager)
		{
			roiManager.draw(gl, new Matrix4f( data.getPv() ), new int [] {(int)data.getScreenWidth(), (int)data.getScreenHeight()});
		}	
			//render the origin of coordinates
			if (bShowOrigin)
			{
				for (int i=0;i<3;i++)
				{
					originVis.get(i).draw(gl, new Matrix4f( data.getPv() ));
				}
			}
			
			//render a box around  the volume 
			if (bVolumeBox)
			{
				volumeBox.draw(gl, new Matrix4f( data.getPv() ), screen_size);
				/*
				float [][] nDimBox = new float [2][3];
				
				int i,j,z;
				for(i=0;i<3;i++)
				{
					//why is this shift?! I don't know,
					// but looks better like this
					nDimBox[0][i]=nDimIni[0][i]+0.5f;
					nDimBox[1][i]=nDimIni[1][i]-1.0f;
				}
				float [] vbox_color = new float[]{0.4f,0.4f,0.4f};
				float vbox_thickness = 0.5f;
				int [][] edgesxy = new int [5][2];
				edgesxy[0]=new int[]{0,0};
				edgesxy[1]=new int[]{1,0};
				edgesxy[2]=new int[]{1,1};
				edgesxy[3]=new int[]{0,1};
				edgesxy[4]=new int[]{0,0};
				
				//draw front and back
				RealPoint vertex1=new RealPoint(0,0,0);
				RealPoint vertex2=new RealPoint(0,0,0);
				for (z=0;z<2;z++)
				{
					for (i=0;i<4;i++)
					{
						for (j=0;j<2;j++)
						{
							vertex1.setPosition(nDimBox[edgesxy[i][j]][j], j);
							vertex2.setPosition(nDimBox[edgesxy[i+1][j]][j], j);
						}
						//z coord
						vertex1.setPosition(nDimBox[z][2], 2);
						vertex2.setPosition(nDimBox[z][2], 2);
						ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();
						VisPolyLineSimple lines;
						point_coords.add(new RealPoint(vertex1));
						point_coords.add(new RealPoint(vertex2));
						
						lines = new VisPolyLineSimple(vbox_color, point_coords, vbox_thickness);
														
						lines.draw( gl, new Matrix4f( data.getPv() ));	
					}
				}
				//draw the rest 4 edges

				for (i=0;i<4;i++)
				{
					for (j=0;j<2;j++)
					{
						vertex1.setPosition(nDimBox[edgesxy[i][j]][j], j);
						vertex2.setPosition(nDimBox[edgesxy[i][j]][j], j);
					}
					//z coord
					vertex1.setPosition(nDimBox[0][2], 2);
					vertex2.setPosition(nDimBox[1][2], 2);
					ArrayList< RealPoint > point_coords = new ArrayList< RealPoint >();
					VisPolyLineSimple lines;
					point_coords.add(new RealPoint(vertex1));
					point_coords.add(new RealPoint(vertex2));

					lines = new VisPolyLineSimple(vbox_color, point_coords, vbox_thickness);

					lines.draw( gl, new Matrix4f( data.getPv() ));	
				}
				*/

			}
		
			//render world grid			
			if(bShowWorldGrid)
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
		int nW= (int)nDimIni[1][0];
		int nH= (int)nDimIni[1][1];
		int nD= (int)nDimIni[1][2];
		
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
		
		
		//handl=bvv.getBvvHandle().getViewerPanel();
		panel.state().setViewerTransform(t);
		panel.requestRepaint();
		if(!firstCall)
			bvv2.removeFromBdv();
		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "cropreset", Bvv.options().addTo(bvv));
		
		//render_pl();
	}
	
	public void resetViewYZ(boolean firstCall)
	{
		
		double scale;
		int nW= (int)nDimIni[1][0];
		int nH= (int)nDimIni[1][1];
		int nD= (int)nDimIni[1][2];
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
		
		panel=bvv.getBvvHandle().getViewerPanel();
		panel.state().setViewerTransform(t);
		panel.requestRepaint();
		if(!firstCall)
			bvv2.removeFromBdv();
		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "cropresetYZ", Bvv.options().addTo(bvv));
		
		
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
		MatrixMath.screenPerspective( dCam, dClipNear, dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		
		
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
			return false;
		}
		long [][] nClickMinMax = new long[2][3];
		
		if(newBoundBox(viewclick, intersectionPoints, nClickMinMax))
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
	
	public void addLineAlongTheClick()
	{
		java.awt.Point point_mouse  =bvv.getBvvHandle().getViewerPanel().getMousePosition();
		System.out.println( "click x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
		

		AffineTransform3D transform = new AffineTransform3D();
		
		panel.state().getViewerTransform(transform);
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

		//render_pl();
	}
	
	public void viewClickArea(final int nHalfWindow)
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
		
		if(newBoundBox(view2, intersectionPoints, nClickMinMax))
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
		

		//render_pl();
	}
	public static void main( String... args) throws IOException
	{
		
		BigTrace testI=new BigTrace(); 
		
		testI.runBVV();
		
		
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
	public boolean checkBoxInside(final IntervalView< UnsignedByteType > viewclick, final long [][] newMinMax)
	{ 
		long [][] bigBox = new long[2][];
		bigBox[0]=viewclick.minAsLongArray();
		bigBox[1]=viewclick.maxAsLongArray();
		for (int j=0;j<3;j++)
		{
				newMinMax[0][j]=Math.max(bigBox[0][j],newMinMax[0][j]);
				newMinMax[1][j]=Math.min(bigBox[1][j],newMinMax[1][j]);
				if(newMinMax[1][j]<newMinMax[0][j])
					return false;
		}
		return true;
	}
	
	public boolean newBoundBox(final IntervalView< UnsignedByteType > viewclick,final ArrayList<RealPoint> pointArray, final long [][] newMinMax)
	{ 
		//= new long [2][3];
		float [][] newMinMaxF = new float [2][3];
		int i, j;
		float temp;
		long [][] bigBox = new long[2][];
		bigBox[0]=viewclick.minAsLongArray();
		bigBox[1]=viewclick.maxAsLongArray();
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
				newMinMax[0][j]=Math.max(bigBox[0][j],(long)Math.round(newMinMaxF[0][j]));
				newMinMax[1][j]=Math.min(bigBox[1][j],(long)Math.round(newMinMaxF[1][j]));
				if(newMinMax[1][j]<=newMinMax[0][j])
					return false;
		}
		return true;

	}
	
}

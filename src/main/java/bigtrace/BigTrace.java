package bigtrace;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.jogamp.opengl.GL3;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.AbstractInterval;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bdv.tools.InitializeViewerState;
import bdv.tools.transformation.TransformedSource;
import bdv.util.Bounds;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import bdv.viewer.ViewerState;

import btbvv.vistools.BvvFunctions;
import btbvv.vistools.Bvv;
import btbvv.vistools.BvvStackSource;
import btbvv.core.render.RenderData;
import btbvv.core.render.VolumeRenderer.RepaintType;
import btbvv.btuitools.BvvGamma;
import btbvv.core.VolumeViewerPanel;
import btbvv.core.util.MatrixMath;

import bigtrace.geometry.Cuboid3D;
import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Line3D;
import bigtrace.gui.AnisotropicTransformAnimator3D;
import bigtrace.math.OneClickTrace;
import bigtrace.math.TraceBoxMath;
import bigtrace.math.TracingBGVect;
import bigtrace.rois.Box3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import bigtrace.rois.RoiManager3D;
import bigtrace.scene.VisPolyLineSimple;
import bigtrace.volume.VolumeMisc;


public class BigTrace < T extends RealType< T > > implements PlugIn, WindowListener, TimePointListener
{
	/** main instance of BVV **/
	public  BvvStackSource< ? > bvv_main = null;
	
	/** BVV sources used for the volume visualization **/
	public  ArrayList<BvvStackSource< ? >> bvv_sources = new ArrayList<BvvStackSource< ? >>();
	
	/** saliency view (TraceBox) for semi-auto tracing **/
	public  BvvStackSource< UnsignedByteType > bvv_trace = null;

	/** whether or not TraceMode is active **/
	private boolean bTraceMode = false;
	
	/** input from XML/HDF5 or BioFormats (cached) **/
	public SpimData spimData;
	
	/** input data in RAI XYZC format**/
	public RandomAccessibleInterval<T> all_ch_RAI;
	
	/** whether LLS transform was applied **/	
	public boolean bTestLLSTransform = false;
	
	/** LLS transform **/
	public AffineTransform3D afDataTransform = new AffineTransform3D();

	/** Panel of BigVolumeViewer **/
	public VolumeViewerPanel viewer;

	public Actions actions = null;
	
	/** flag to check if user interface is frozen **/
	public boolean bInputLock = false;
	
	/** visualization of coordinates origin axes **/
	ArrayList<VisPolyLineSimple> originVis = new ArrayList<VisPolyLineSimple>();

	/** box around volume **/
	Box3D volumeBox;
	
	/** helper box to visualize one-click tracing things **/
	public Box3D visBox = null;
	
	/** helper box to visualize one-click tracing things **/
	public Box3D clipBox = null;

	/** object storing main data/variables **/
	public BigTraceData<T> btdata;
	
	/** object loading data **/
	public BigTraceLoad<T> btload;	
	
	/** BigTrace interface panel **/
	public BigTraceControlPanel<T> btpanel;
	
	/** ROI manager + list tab **/
	public RoiManager3D roiManager;
		
	public void run(String arg)
	{
		//switch to FlatLaf theme
		
		try {
		    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		    FlatLaf.registerCustomDefaultsSource( "flatlaf" );
		    FlatIntelliJLaf.setup();
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		
		btdata = new BigTraceData<T>(this);
		btload = new BigTraceLoad<T>(this);
		
		if(arg.equals(""))
		{
			btdata.sFileNameFullImg = IJ.getFilePath("Open TIF/BDV/Bioformats file (3D, composite, time)...");
		}
		else
		{
			btdata.sFileNameFullImg = arg;
		}

		if(btdata.sFileNameFullImg == null)
			return;
		
		//load data sources
		
		// TIF files are fully loaded (to RAM) for now
		if(btdata.sFileNameFullImg.endsWith(".tif"))
		{
			btdata.bSpimSource = false;
			if(!btload.initDataSourcesImageJ())
				return;
		}
		else
		{
			// BDV XML/HDF5 format 
			btdata.bSpimSource = true;
			if(btdata.sFileNameFullImg.endsWith(".xml"))
			{				
				try {
					if(!btload.initDataSourcesHDF5())
						return;
				} catch (SpimDataException e) {
					e.printStackTrace();
					IJ.showMessage("BigTrace: cannot open selected XML file.\nMaybe it is not BDV compartible HDF5?\nPlugin terminated.");
					return;
				}
			}
			//try BioFormats
			else
			{
				String outLog = btload.initDataSourcesBioFormats(); 
				if(outLog != null)
				{
					IJ.error(outLog);
					return;
				}
			
			}
		}	

		roiManager = new RoiManager3D(this);
		
		initSourcesCanvas(0.25*Math.min(btdata.nDimIni[1][0], Math.min(btdata.nDimIni[1][1],btdata.nDimIni[1][2])));
		
		//not sure we really need it, but anyway
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
        

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
			nDimBox[1][i]=(btdata.nDimIni[1][i]-1.0f);
		}
		volumeBox = new Box3D(nDimBox,0.5f,0.0f,Color.LIGHT_GRAY,Color.LIGHT_GRAY, 0);
		clipBox = new Box3D(nDimBox,0.5f,0.0f,Color.LIGHT_GRAY,Color.LIGHT_GRAY, 0);
	}
	public void initSourcesCanvas(double origin_axis_length)
	{
		
		initOriginAndBox(origin_axis_length);
	
		if(btdata.bSpimSource)
		{
			initBVVSourcesSpimData();
		}
		else
		{
			initBVVSourcesImageJ();
		}
		
		viewer = bvv_main.getBvvHandle().getViewerPanel();
		viewer.setRenderScene(this::renderScene);
		actions = new Actions( new InputTriggerConfig() );
		installActions(actions);
		setInitialTransform();
		viewer.addTimePointListener(this);
	}
	
	
	private void createAndShowGUI() 
	{
		btpanel = new BigTraceControlPanel<T>(this, btdata,roiManager);
		btpanel.finFrame = new JFrame("BigTrace");

		btpanel.bvv_frame=(JFrame) SwingUtilities.getWindowAncestor(viewer);
	 	
		btpanel.finFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		btpanel.bvv_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		btpanel.finFrame.add(btpanel);
		
        //Display the window.
		btpanel.finFrame.setSize(400,600);
		btpanel.finFrame.setVisible(true);
	    java.awt.Point bvv_p = btpanel.bvv_frame.getLocationOnScreen();
	    java.awt.Dimension bvv_d = btpanel.bvv_frame.getSize();
	
	    btpanel.finFrame.setLocation(bvv_p.x+bvv_d.width, bvv_p.y);
	    btpanel.finFrame.addWindowListener(this);
	    btpanel.bvv_frame.addWindowListener(this);

	}
	
	/** find a brightest pixel in the direction of a click
	 *  and add a new 3D point to active ROI OR
	 *  start a new ROI (if none selected)
	 **/ 
	public void actionAddPoint()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
								
			RealPoint target = new RealPoint(3);
			if(!bTraceMode)
			{
				if(findPointLocationFromClick(btdata.getDataCurrentSourceClipped(), btdata.nHalfClickSizeWindow,target))
				{
					
//					System.out.println(target.getDoublePosition(0));
//					System.out.println(target.getDoublePosition(1));
//					System.out.println(target.getDoublePosition(2));
					
					switch (RoiManager3D.mode)
					{
						case RoiManager3D.ADD_POINT_SEMIAUTOLINE:
							
							setTraceBoxMode(true);
							
							//nothing selected, make a new tracing
							if(roiManager.activeRoi==-1)
							{
								//make a temporary ROI to calculate TraceBox
								LineTrace3D tracing_for_box = (LineTrace3D) roiManager.makeRoi(Roi3D.LINE_TRACE, btdata.nCurrTimepoint);
								tracing_for_box.addFirstPoint(target);
								//calculate a box around maximum intensity point
								calcShowTraceBox(tracing_for_box, true);

							}
							else
							{
								int nRoiType = roiManager.getActiveRoi().getType();
								//continue tracing for the selected tracing
								if(nRoiType == Roi3D.LINE_TRACE)
								{
									calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi(),false);
								}
								//otherwise make a new tracing
								else
								{
									roiManager.addSegment(target, null);																
									calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi(),false);
								}
							}
							break;
						case RoiManager3D.ADD_POINT_ONECLICKLINE:
							roiManager.unselect();
							//setTraceBoxMode(true);
							roiManager.setLockMode(true);
							runOneClickTrace(target);
							break;
						default:
							roiManager.addPoint(target);
					}
					
				}
			}
			//we are in the tracebox mode,
			//continue to trace within the trace box
			else
			{
				if(RoiManager3D.mode==RoiManager3D.ADD_POINT_SEMIAUTOLINE)
				{
					if(findPointLocationFromClick(btdata.trace_weights, btdata.nHalfClickSizeWindow, target))
					{
						//run trace finding in a separate thread
						getSemiAutoTrace(target);
						
					}
				}
			}
		}
		
	}
	
	/** selects ROI upon user click **/
	public void actionSelectRoi()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			if(!bTraceMode)
			{
				Line3D clickLine = findClickLine();
				if(clickLine!=null)
					roiManager.selectClosestToLineRoi(findClickLine());
				
			}
		}
		
	}
	
	/** works only in trace mode, deselects current tracing
	 * and starts a new one in the trace mode**/
	public void actionNewRoiTraceMode()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
								
			RealPoint target = new RealPoint(3);
			if(bTraceMode)
			{
				if(findPointLocationFromClick(btdata.trace_weights, btdata.nHalfClickSizeWindow, target))
				{
					roiManager.unselect();
					roiManager.addSegment(target, null);																
					calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi(),false);
				}				
			}
		}
	}
	/** remove last added point from ROI
	 * (and delete ROI if it is the last point in it)
	 * **/
	public void actionRemovePoint()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			if(!bTraceMode)
			{
				roiManager.removePointLinePlane();
			}
			else
			{
				//if the last point in the tracing, leave tracing mode
				if(!roiManager.removeSegment())
				{
					btdata.nPointsInTraceBox--;
					roiManager.removeActiveRoi();
					roiManager.activeRoi=-1;
					setTraceBoxMode(false);						
					removeTraceBox();
					
				}
				//not the last point, see if we need to move trace box back
				else
				{
					btdata.nPointsInTraceBox--;
					
					if(btdata.nPointsInTraceBox==0)
					{
						calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi(),false);
					}
				}
				
			}
			viewer.showMessage("Point removed");

		}					
		
	}
	/** deselects current ROI (and finishes tracing)
	 *   
	 * **/
	public void actionDeselect()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			if(!bTraceMode)
			{
				roiManager.unselect();
			}
			else
			{
				roiManager.unselect();
				setTraceBoxMode(false);
				//bTraceMode= false;
				//roiManager.setLockMode(bTraceMode);	
				removeTraceBox();
			}
		}
	}
	/** reverses order of points/segments in PolyLine and LineTrace,
	 * so the active end (where point addition happens) is reversed **/
	public void actionReversePoints() 
	{
		
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			if(roiManager.activeRoi>=0)
			{
				int nRoiType = roiManager.getActiveRoi().getType();
				//continue tracing for the selected tracing
				if(nRoiType == Roi3D.POLYLINE)
				{
					roiManager.getActiveRoi().reversePoints();					
				}
				
				if(nRoiType == Roi3D.LINE_TRACE)
				{
					roiManager.getActiveRoi().reversePoints();
					if(bTraceMode)
					{
						calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi(),false);
						btdata.nPointsInTraceBox=1;
					}
				}
				repaintBVV();
			}

		}
	}
	/** move trace box to a position around current last added point **/
	public void actionAdvanceTraceBox()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			if(bTraceMode && btdata.nPointsInTraceBox>1)
			{
				calcShowTraceBox((LineTrace3D)roiManager.getActiveRoi(),false);
				btdata.nPointsInTraceBox=1;
			}
		}
	}
	/** in a trace mode build a straight (not a curved trace) segment 
	 * connecting clicked and last point (to overcome semi-auto errors)**/
	public void actionSemiTraceStraightLine()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
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
	
	/** find a brightest pixel in the direction of a click
	 *  zoom main view to it, limiting to nZoomBoxSize
	 **/ 
	public void actionZoomIn()
	{
		
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			//addPoint();
			RealPoint target = new RealPoint(3);
			if(!bTraceMode)
			{
				if(findPointLocationFromClick(btdata.getDataCurrentSourceClipped(), btdata.nHalfClickSizeWindow,target))
				{
					
					final FinalInterval zoomInterval = getTraceBoxCentered(getTraceInterval(!btdata.bZoomClip),btdata.nZoomBoxSize, target);

					if(btdata.bZoomClip)
					{
						btpanel.clipPanel.setBoundingBox(zoomInterval);
					}
	
					//animate
					viewer.setTransformAnimator(getCenteredViewAnim(zoomInterval,btdata.dZoomBoxScreenFraction));
				}
			}
			else
			{
				if(findPointLocationFromClick(btdata.trace_weights, btdata.nHalfClickSizeWindow,target))
				{
					//FinalInterval zoomInterval = getTraceBoxCentered(btdata.trace_weights,(long)(btdata.lTraceBoxSize*0.8), target);
					FinalInterval zoomInterval = getZoomBoxCentered((long)(btdata.lTraceBoxSize*0.5), target);
			
					viewer.setTransformAnimator(getCenteredViewAnim(zoomInterval,btdata.dZoomBoxScreenFraction));
				}
			}

		}
	}
	
	/** zoom out to get full overview of current active volume view
	 **/ 
	public void actionZoomOut()
	{
		
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			if(!bTraceMode)
			{		
					viewer.setTransformAnimator(getCenteredViewAnim(btdata.getDataCurrentSourceClipped(),1.0));
			}
			else
			{
					viewer.setTransformAnimator(getCenteredViewAnim(btdata.trace_weights,0.8));
			}

		}
	}
	
	public void actionResetClip()
	{
		Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		//solution for now, to not interfere with typing
		if(!bInputLock && !(c instanceof JTextField))
		{
			if(!bTraceMode)
			{
				btpanel.clipPanel.setBoundingBox(btdata.nDimIni);				
			}
		}
	}
	
	public void actionToggleRender()
	{
		if(btdata.nRenderMethod==0)
		{
			btpanel.renderMethodPanel.cbRenderMethod.setSelectedIndex(1);
			viewer.showMessage("volumetric");
		}
		else
		{
			btpanel.renderMethodPanel.cbRenderMethod.setSelectedIndex(0);
			viewer.showMessage("maximum intensity");
		}
	}
	public void installActions(final Actions actions)
	{
		//final Actions actions = new Actions( new InputTriggerConfig() );
		actions.runnableAction(() -> actionAddPoint(),	            "add point", "F" );
		actions.runnableAction(() -> actionNewRoiTraceMode(),	    "new trace", "V" );		
		actions.runnableAction(() -> actionRemovePoint(),       	"remove point",	"G" );
		actions.runnableAction(() -> actionDeselect(),	            "deselect", "H" );
		actions.runnableAction(() -> actionReversePoints(),         "reverse curve point order","Y" );
		actions.runnableAction(() -> actionAdvanceTraceBox(),       "advance trace box", "T" );
		actions.runnableAction(() -> actionSemiTraceStraightLine(),	"straight line semitrace", "R" );
		actions.runnableAction(() -> actionZoomIn(),			"zoom in to click", "D" );
		actions.runnableAction(() -> actionZoomOut(),				"center view (zoom out)", "C" );
		actions.runnableAction(() -> actionResetClip(),				"reset clipping", "X" );
		actions.runnableAction(() -> actionToggleRender(),			"toggle render mode", "O" );
		actions.runnableAction(() -> actionSelectRoi(),	            "select ROI", "E" );
				
		
		
		actions.runnableAction(
				() -> {
					Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
					if(!(c instanceof JTextField))
						resetViewXY();
					
				},
				"reset view XY",
				"1" );
			actions.runnableAction(
				() -> {
					Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
					if(!(c instanceof JTextField))
						resetViewYZ();
				},
				"reset view YZ",
				"2" );
			actions.runnableAction(
					() -> {
						Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
						if(!(c instanceof JTextField))
							resetViewXZ();
					},
					"reset view XZ",
					"3" );			

		//actions.namedAction(action, defaultKeyStrokes);
		actions.install( bvv_main.getBvvHandle().getKeybindings(), "BigTrace actions" );


	}
	
	public void focusOnInterval(Interval interval_in)
	{
		if(!bInputLock && !bTraceMode)
		{
			viewer.setTransformAnimator(getCenteredViewAnim(interval_in,btdata.dZoomBoxScreenFraction));
		}
	}
	
	/** calculates trace box around last vertice of provided trace.
	 * if bRefine is true, it will refine the position of the dot
	 * and add it to the ROI manager **/
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void calcShowTraceBox(final LineTrace3D trace, final boolean bRefine)
	{
		FinalInterval rangeTraceBox;
		
		IntervalView<?> traceIV;
		
		traceIV = getTraceInterval(btdata.bTraceOnlyClipped);
		
		if(trace.numVertices()==1)
		{
			rangeTraceBox = getTraceBoxCentered(traceIV,btdata.lTraceBoxSize, trace.vertices.get(0));
		}
		else
		{
			rangeTraceBox = getTraceBoxNext(traceIV,btdata.lTraceBoxSize, btdata.fTraceBoxAdvanceFraction, trace);
		}
		
		IntervalView<?> traceInterval = Views.interval(traceIV, rangeTraceBox);
		
		//getCenteredView(traceInterval);
		viewer.setTransformAnimator(getCenteredViewAnim(traceInterval,btdata.dTraceBoxScreenFraction));
		//long start1, end1;
		

		//start1 = System.currentTimeMillis();
		//calcWeightVectrosCorners(traceInterval, sigmaGlob);
		//end1 = System.currentTimeMillis();
		bInputLock = true;
		TraceBoxMath calcTask = new TraceBoxMath();
		if(bRefine)
		{
			calcTask.refinePosition = trace.vertices.get(0);
		}
		calcTask.input = traceInterval;
		calcTask.bt = this;
		calcTask.addPropertyChangeListener(btpanel);
		calcTask.execute();
		//System.out.println("+corners: elapsed Time in milli seconds: "+ (end1-start1));		

		//showTraceBox(btdata.trace_weights);
		btdata.nPointsInTraceBox = 1;
	}
	
	/** calculates trace box around last vertice of provided trace.
	 * if bRefine is true, it will refine the position of the dot
	 * and add it to the ROI manager **/
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void runOneClickTrace(final RealPoint pclick)
	{
		
		IntervalView<?> traceIV;
		
		traceIV = getTraceInterval(btdata.bTraceOnlyClipped);	
//		System.out.println(pclick.getDoublePosition(0));
//		System.out.println(pclick.getDoublePosition(1));
//		System.out.println(pclick.getDoublePosition(2));

		bInputLock = true;
		OneClickTrace calcTask = new OneClickTrace();
		calcTask.fullInput = traceIV;
		calcTask.bt = this;
		calcTask.startPoint = pclick;
		calcTask.addPropertyChangeListener(btpanel);
		calcTask.execute();
	
	}
	
	/** returns current Interval for the tracing. If bClippedInterval is true,
	 * returns clipped volume, otherwise returns full original volume. **/
	IntervalView<T> getTraceInterval(boolean bClippedInterval)
	{
		if(bClippedInterval)
		{
			return btdata.getDataSourceClipped(btdata.nChAnalysis, btdata.nCurrTimepoint);
		}
		else
		{
			RandomAccessibleInterval<T> full_int = btdata.getDataSourceFull(btdata.nChAnalysis, btdata.nCurrTimepoint);
			
			return Views.interval(full_int, full_int);
		}
	}
	

	
	/** calculate optimal path **/
	public void getSemiAutoTrace(RealPoint target)
	{
		
		bInputLock = true;
		TracingBGVect traceBG = new TracingBGVect();
		traceBG.target = target;
		traceBG.bt=this;
		traceBG.addPropertyChangeListener(btpanel);
		traceBG.execute();
		return ;
		
	}

	
	
	public void showCorners(ArrayList<long []> corners)
	{
		RoiManager3D.mode=RoiManager3D.ADD_POINT;
		for(int i=0;i<corners.size();i++)
		{
			RealPoint vv = new RealPoint(0.,0.,0.);
			vv.setPosition(corners.get(i));
			roiManager.addPoint(vv);	
		}
	}

	/** gets a box around "target" with half size of range in all axes.
		crops the box so it is inside viewclick interval **/
	public FinalInterval getTraceBoxCentered(final AbstractInterval viewclick, final long range, final RealPoint target)
	{
		long[][] rangeM = new long[2][3];
		int i;
		float [] pos = new float[3];
		target.localize(pos);
		for(i=0;i<3;i++)
		{
			rangeM[0][i]=(long)(pos[i])-range ;
			rangeM[1][i]=(long)(pos[i])+range;								
		}
		VolumeMisc.checkBoxInside(viewclick, rangeM);
		FinalInterval finInt = new FinalInterval(rangeM[0],rangeM[1]);
		return finInt;							
	}
	
	//gets a box around "target" with half size of range
	public FinalInterval getZoomBoxCentered(final long range, final RealPoint target)
	{
		long[][] rangeM = new long[2][3];
		int i;
		float [] pos = new float[3];
		target.localize(pos);
		for(i=0;i<3;i++)
		{
			rangeM[0][i]=(long)(pos[i])-range ;
			rangeM[1][i]=(long)(pos[i])+range;								
		}
		FinalInterval finInt = new FinalInterval(rangeM[0],rangeM[1]);
		return finInt;							
	}
	
	//gets a box around "target" with half size of range
	public FinalInterval getTraceBoxNext(final IntervalView< ? > viewclick, final long range, final float fFollowDegree, LineTrace3D trace)
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
		FinalInterval finInt = new FinalInterval(rangeM[0],rangeM[1]);
		return finInt;
									
	}


	public AffineTransform3D getCenteredViewTransform(final AffineTransform3D ini_transform, final Interval inInterval, double zoomFraction)
	{
		int i;
		int nDim = inInterval.numDimensions();
		final long [] minDim = inInterval.minAsLongArray();
		final long [] maxDim = inInterval.maxAsLongArray();
		float [] centerCoord = new float[nDim];
		float [] centerCoordWorldOld = new float[nDim];
		
		
		//center of the interval in the volume coordinates
		for(i=0;i<nDim;i++)
		{
			centerCoord[i] = (float)Math.round(minDim[i]+ 0.5*(maxDim[i]-minDim[i]));
		}
		
		//current window dimensions
		final int sW = viewer.getWidth();
		final int sH = viewer.getHeight();
		
		//current view transform
		final AffineTransform3D transform = ini_transform.copy();//new AffineTransform3D(ini_transform);
		//panel.state().getViewerTransform(transform);
		
		//calculate scale factor
		//current width/height
		FinalRealInterval boxAfter = transform.estimateBounds(inInterval);
		double dCurrW = boxAfter.realMax(0)-boxAfter.realMin(0);
		double dCurrH = boxAfter.realMax(1)-boxAfter.realMin(1);
		double scaleX = (zoomFraction)*sW/dCurrW;
		double scaleY = (zoomFraction)*sH/dCurrH;
		double scalefin=Math.min(scaleX, scaleY);
		
		transform.scale(scalefin);
		
		
		//current coordinates of center in the current(old) transform
		transform.apply(centerCoord, centerCoordWorldOld);

		//center of the screen "volume" 		
		Vector3f vcenter = new Vector3f();			
		Matrix4f matPersp = new Matrix4f();
		MatrixMath.screenPerspective( btdata.dCam, btdata.dClipNear, btdata.dClipFar, sW, sH, 0, matPersp );
		
		matPersp.unproject(0.5f*sW,0.5f*sH,0.0f, //z=0 does not matter here
				new int[] { 0, 0, sW, sH },vcenter);
		float [] centerViewPoint = new float[3];
		centerViewPoint[0]=vcenter.x;
		centerViewPoint[1]=vcenter.y;
		centerViewPoint[2]=0.0f; //center of the "screen" volume		
		
		//position center of the volume in the center of "screen" volume
		double [] dl = transform.getTranslation();
		
		//translation after source transform to new position
		for(i=0;i<3;i++)
		{
			dl[i]+= (centerViewPoint[i]-centerCoordWorldOld[i]);
		}
		transform.setTranslation(dl);
		
		return transform;
	}
	
	public AffineTransform3D getCenteredViewTransform(final Interval inInterval, double zoomFraction)
	{
		
		final AffineTransform3D transform = new AffineTransform3D();
		
		viewer.state().getViewerTransform(transform);
		
		return getCenteredViewTransform(transform, inInterval, zoomFraction);
	}
	
	public AnisotropicTransformAnimator3D getCenteredViewAnim(final Interval inInterval, double zoomFraction)
	{
		final AffineTransform3D transform = new AffineTransform3D();
		viewer.state().getViewerTransform(transform);
		
		final AffineTransform3D transform_scale = getCenteredViewTransform(inInterval,zoomFraction);
		
		final AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(transform,transform_scale,0,0,btdata.nAnimationDuration);			
		
		return anim;
	}

	public void showTraceBox()
	{

		// there is a trace box already, let's remove it
		if(bvv_trace!=null)
		{
			btdata.bcTraceBox.storeBC(bvv_trace);
			bvv_trace.removeFromBdv();
			System.gc();
		}
		//there is no tracebox, let's dim the main volume first
		else
		{
			if(btdata.nTraceBoxView==1)
			{
				btdata.bcTraceChannel.storeBC(bvv_sources.get(btdata.nChAnalysis));
				bvv_sources.get(btdata.nChAnalysis).setDisplayRange(0.0, 0.0);
				bvv_sources.get(btdata.nChAnalysis).setAlphaRange(0.0, 0.0);
			}
			
		}

		bvv_trace = BvvFunctions.show(btdata.trace_weights, "weights", Bvv.options().addTo(bvv_main));
		bvv_trace.setCurrent();
		bvv_trace.setRenderType(btdata.nRenderMethod);
		bvv_trace.setDisplayRangeBounds(0, 255);
		bvv_trace.setAlphaRangeBounds(0, 255);
		if(btdata.bcTraceBox.bInit)
		{
			btdata.bcTraceBox.setBC(bvv_trace);
		}
		else	
		{
			bvv_trace.setDisplayRangeBounds(0, 255);
			bvv_trace.setAlphaRangeBounds(0, 255);
			bvv_trace.setDisplayRange(0., 150.0);
			bvv_trace.setAlphaRange(0., 150.0);

		}

	}
	
	/** removes tracebox from BVV **/
	public void removeTraceBox()
	{

		if(bvv_trace != null)
		{
			btdata.bcTraceBox.storeBC(bvv_trace);
			bvv_trace.removeFromBdv();
			System.gc();
		}
		bvv_trace=null;
		//handl.setDisplayMode(DisplayMode.SINGLE);
		if(btdata.nTraceBoxView==1)
		{
			btdata.bcTraceChannel.setBC(bvv_sources.get(btdata.nChAnalysis));
		}
	}	
	
	/** Locks interface and enters Trace mode**/
	public void setTraceBoxMode(boolean bStatus)
	{
		bTraceMode = bStatus;								
		roiManager.setLockMode(bStatus);
		//entering trace mode, 
		//let's save current view
		if(bStatus)
		{
			viewer.state().getViewerTransform(btdata.transformBeforeTracing);
			viewer.showMessage("TraceBox mode on");
			//disable time slider
			//panel.setNumTimepoints(1);
			//transformBeforeTracing.set(panel.);
		}
		//exiting trace mode,
		//let's go back
		else
		{
			final AffineTransform3D transform = new AffineTransform3D();
			viewer.state().getViewerTransform(transform);
			final AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(transform,btdata.transformBeforeTracing,0,0,1500);
			viewer.setTransformAnimator(anim);
			viewer.showMessage("TraceBox mode off");

		}
	}


	public void renderScene(final GL3 gl, final RenderData data)
	{
		
		int [] screen_size = new int [] {(int)data.getScreenWidth(), (int) data.getScreenHeight()};
		//handl.setRenderScene( ( gl, data ) -> {
		
		final Matrix4f pvm = new Matrix4f( data.getPv() );
		final Matrix4f view = MatrixMath.affine( data.getRenderTransformWorldToScreen(), new Matrix4f() );
		final Matrix4f camview = MatrixMath.screen( btdata.dCam, screen_size[0], screen_size[1], new Matrix4f() ).mul( view );

		
		//to be able to change point size in shader
		gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
		
		synchronized (roiManager)
		{
			roiManager.draw(gl, pvm, camview, screen_size);
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
				volumeBox.draw(gl, pvm, camview, screen_size);
			}
			//render a box around  the volume 
			if (btdata.bClipBox)
			{
				clipBox.draw(gl, pvm, camview, screen_size);
			}
			
			//one click tracing box
			if(visBox != null)
			{
				visBox.draw(gl, pvm, camview, screen_size);
				
			}
		//panel.requestRepaint(RepaintType.SCENE);

	}
	
	public void repaintBVV()
	{
		viewer.requestRepaint();
	}
	public void repaintScene()
	{
		viewer.requestRepaint(RepaintType.SCENE);
	}
	
	public void initBVVSourcesImageJ()
	{
		
		Path p = Paths.get(btdata.sFileNameFullImg);
		String filename = p.getFileName().toString();
		
		Bvv Tempbvv = BvvFunctions.show( Bvv.options().
				dCam(btdata.dCam).
				dClipNear(btdata.dClipNear).
				dClipFar(btdata.dClipFar).				
				renderWidth( BigTraceData.renderParams.renderWidth).
				renderHeight( BigTraceData.renderParams.renderHeight).
				numDitherSamples( BigTraceData.renderParams.numDitherSamples ).
				cacheBlockSize( BigTraceData.renderParams.cacheBlockSize ).
				maxCacheSizeInMB( BigTraceData.renderParams.maxCacheSizeInMB ).
				ditherWidth(BigTraceData.renderParams.ditherWidth).
				frameTitle(filename)
				);

		for(int i=0;i<btdata.nTotalChannels;i++)
		{
	
			bvv_sources.add(BvvFunctions.show( Views.hyperSlice(all_ch_RAI,4,i), "ch_"+Integer.toString(i+1), Bvv.options().addTo(Tempbvv)));
			if(btdata.nBitDepth<=8)
			{
				bvv_sources.get(i).setDisplayRangeBounds(0, 255);
				bvv_sources.get(i).setAlphaRangeBounds(0, 255);
			}
			else
			{
				bvv_sources.get(i).setDisplayRangeBounds(0, 65535);
				bvv_sources.get(i).setAlphaRangeBounds(0, 65535);
			}
			bvv_sources.get(i).setColor( new ARGBType( btload.colorsCh[i].getRGB() ));
			bvv_sources.get(i).setDisplayRange(btload.channelRanges[0][i], btload.channelRanges[1][i]);
			bvv_sources.get(i).setAlphaRange(btload.channelRanges[0][i], btload.channelRanges[1][i]);
			bvv_sources.get(i).setRenderType(btdata.nRenderMethod);

		}
		
		bvv_main = bvv_sources.get(0);

		viewer = bvv_main.getBvvHandle().getViewerPanel();
	}
	
	public void initBVVSourcesSpimData()
	{

		Path p = Paths.get(btdata.sFileNameFullImg);
		String filename = p.getFileName().toString();
		List<BvvStackSource<?>> sourcesSPIM = BvvFunctions.show(spimData,Bvv.options().
				dCam(btdata.dCam).
				dClipNear(btdata.dClipNear).
				dClipFar(btdata.dClipFar).				
				renderWidth( BigTraceData.renderParams.renderWidth).
				renderHeight( BigTraceData.renderParams.renderHeight).
				numDitherSamples( BigTraceData.renderParams.numDitherSamples ).
				cacheBlockSize( BigTraceData.renderParams.cacheBlockSize ).
				maxCacheSizeInMB( BigTraceData.renderParams.maxCacheSizeInMB ).
				ditherWidth(BigTraceData.renderParams.ditherWidth).
				dCam( btdata.dCam ).
				dClipNear( btdata.dClipNear ).
				dClipFar( btdata.dClipFar ).
				frameTitle(filename)//.
				//sourceTransform(afDataTransform)
				);		
		
		for(int i=0;i<sourcesSPIM.size();i++)
		{
			bvv_sources.add(sourcesSPIM.get(i));
			bvv_sources.get(i).setRenderType(btdata.nRenderMethod);
		}

		bvv_main = bvv_sources.get(0);
		viewer = bvv_main.getBvvHandle().getViewerPanel();
		
		//AffineTransform3D transformfin = new AffineTransform3D();

		// Remove voxel scale and any translation transforms for all sources.
		// We needed it, because later voxel size transform is applied to the general ViewerPanel.
		for ( SourceAndConverter< ? > source : viewer.state().getSources() )
		{
			AffineTransform3D transformSource = new AffineTransform3D();

			(( TransformedSource< ? > ) source.getSpimSource() ).getSourceTransform(0, 0, transformSource);
			
			AffineTransform3D transformScale = new AffineTransform3D();
			AffineTransform3D transformTranslation = new AffineTransform3D();
			transformTranslation.identity();
			double [] shiftTR = new double [3];
			for(int j=0;j<3;j++)
			{
				//BigTraceData.globCal[j] = transformSource.get(j, j);
				transformScale.set(1.0/BigTraceData.globCal[j], j, j);
				shiftTR[j]= (-1.0)*transformSource.get(j, 3);
			}
			transformTranslation.identity();
			transformTranslation.translate(shiftTR);
			//AffineTransform3D transformFinal = transformScale.concatenate(transformTranslation);
			AffineTransform3D transformFinal = transformScale.concatenate(transformTranslation);
			(( TransformedSource< ? > ) source.getSpimSource() ).setFixedTransform(transformFinal);
//			(( TransformedSource< ? > ) source.getSpimSource() ).setIncrementalTransform(transformFinal);
			
			//(( TransformedSource< ? > ) source.getSpimSource() ).getSourceTransform(0, 0, transformfin);
		}

		if(bTestLLSTransform)
		{
			//remove translation
			for ( SourceAndConverter< ? > source : viewer.state().getSources() )
			{
				AffineTransform3D transformExtra = afDataTransform.copy();
				//not sure why, but overlap with raster data looks better under those settings 
				transformExtra.translate(0.5,2.0,0.0);
				(( TransformedSource< ? > ) source.getSpimSource() ).setIncrementalTransform(transformExtra);
				//(( TransformedSource< ? > ) source.getSpimSource() ).setIncrementalTransform(afDataTransform);
				//adjust pixel size to homogeneous scaling
				for(int d=0;d<3;d++)
				{
					//store original values
					BigTraceData.inputCal[d] = BigTraceData.globCal[d];
					//since we are resampling
					BigTraceData.globCal[d] = BigTraceData.dMinVoxelSize;
				}

			}
			
			for(int i=0;i<bvv_sources.size();i++)
			{
				bvv_sources.get(i).setClipTransform(afDataTransform);

			}
			//check the alignment
			//BvvFunctions.show(btdata.getDataSourceFull(0, 0),"test",Bvv.options().addTo(bvv_main));
			//ImageJFunctions.show(btdata.getDataSourceFull(0, 0));
		}

		BvvGamma.initBrightness( 0.001, 0.999, viewer.state(), viewer.getConverterSetups());

	}
	
	public void setInitialTransform()
	{
		AffineTransform3D t = new AffineTransform3D();
		t.identity();
		t.scale(BigTraceData.globCal[0],BigTraceData.globCal[1],BigTraceData.globCal[2]);
		t.rotate(0, Math.PI/2.0);
		t.rotate(1, (-1)*Math.PI/6.0);
		t.rotate(0, Math.PI/9.0);
		viewer.state().setViewerTransform(t);
		t = getCenteredViewTransform(new FinalInterval(btdata.nDimCurr[0],btdata.nDimCurr[1]), 0.9);
		viewer.state().setViewerTransform(t);
	}

	
	public void resetViewXY()
	{
		
		double scale;
		long [][] nBox;
		if(!bTraceMode)
		{
			nBox = btdata.nDimCurr;
		}
		else
		{
			nBox = new long [2][3];
			nBox[0] = btdata.trace_weights.minAsLongArray();
			nBox[1] = btdata.trace_weights.maxAsLongArray();
		}
		
		double nW = (double)(nBox[1][0]-nBox[0][0])*BigTraceData.globCal[0];
		double nH = (double)(nBox[1][1]-nBox[0][1])*BigTraceData.globCal[1];
		double nWoff = (double)(2.0*nBox[0][0])*BigTraceData.globCal[0];
		double nHoff = (double)(2.0*nBox[0][1])*BigTraceData.globCal[1];
		double nDoff = (double)(2.0*nBox[0][2])*BigTraceData.globCal[2];
		
		double sW = viewer.getWidth();
		double sH = viewer.getHeight();
		
		if(sW/nW<sH/nH)
		{
			scale = sW/nW;
		}
		else
		{
			scale = sH/nH;
		}
		scale = 0.9*scale;
		AffineTransform3D t = new AffineTransform3D();
		t.identity();

		t.scale(BigTraceData.globCal[0]*scale, BigTraceData.globCal[1]*scale, BigTraceData.globCal[2]*scale);
		t.translate(0.5*(sW-scale*(nW+nWoff)),0.5*(sH-scale*(nH+nHoff)),(-0.5)*scale*(nDoff));
		
		AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(viewer.state().getViewerTransform(),t,0,0,(long)(btdata.nAnimationDuration*0.5));
		viewer.setTransformAnimator(anim);
			
	}
	
	public void resetViewYZ()
	{
		
		double scale;
		long [][] nBox;
		if(!bTraceMode)
		{
			nBox = btdata.nDimCurr;
		}
		else
		{
			nBox = new long [2][3];
			nBox[0] = btdata.trace_weights.minAsLongArray();
			nBox[1] = btdata.trace_weights.maxAsLongArray();
		}
		double nH = (double)(nBox[1][1]-nBox[0][1])*BigTraceData.globCal[1];
		double nD = (double)(nBox[1][2]-nBox[0][2])*BigTraceData.globCal[2];
		double nWoff = (double)(2.0*nBox[0][0])*BigTraceData.globCal[0];
		double nHoff = (double)(2.0*nBox[0][1])*BigTraceData.globCal[1];
		double nDoff = (double)(2.0*nBox[0][2])*BigTraceData.globCal[2];
		double sW = viewer.getWidth();
		double sH = viewer.getHeight();
		
		if(sW/nD<sH/nH)
		{
			scale = sW/nD;
		}
		else
		{
			scale = sH/nH;
		}
		scale = 0.9*scale;
		AffineTransform3D t = new AffineTransform3D();
	
		t.identity();
		t.scale(BigTraceData.globCal[0]*scale, BigTraceData.globCal[1]*scale, BigTraceData.globCal[2]*scale);
		t.rotate(1, (-1)*Math.PI/2.0);
		t.translate(0.5*(sW+scale*(nD+nDoff)),0.5*(sH-scale*(nH+nHoff)),(-0.5)*scale*nWoff);
		
		AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(viewer.state().getViewerTransform(),t,0,0,(long)(btdata.nAnimationDuration*0.5));
		
		viewer.setTransformAnimator(anim);

	}
	
	public void resetViewXZ()
	{
		
		double scale;
		long [][] nBox;
		if(!bTraceMode)
		{
			nBox = btdata.nDimCurr;
		}
		else
		{
			nBox = new long [2][3];
			nBox[0] = btdata.trace_weights.minAsLongArray();
			nBox[1] = btdata.trace_weights.maxAsLongArray();
		}
		double nW = (double)(nBox[1][0]-nBox[0][0])*BigTraceData.globCal[0];
		double nD = (double)(nBox[1][2]-nBox[0][2])*BigTraceData.globCal[2];
		double nWoff = (double)(2.0*nBox[0][0])*BigTraceData.globCal[0];
		double nHoff = (double)(2.0*nBox[0][1])*BigTraceData.globCal[1];
		double nDoff = (double)(2.0*nBox[0][2])*BigTraceData.globCal[2];
		double sW = viewer.getWidth();
		double sH = viewer.getHeight();
		
		if(sW/nW<sH/nD)
		{
			scale = sW/nW;
		}
		else
		{
			scale = sH/nD;
		}
		scale = 0.9*scale;
		AffineTransform3D t = new AffineTransform3D();
	
		t.identity();
		
		t.scale(BigTraceData.globCal[0]*scale, BigTraceData.globCal[1]*scale, BigTraceData.globCal[2]*scale);
		t.rotate(0, Math.PI/2.0);
		t.translate(0.5*(sW-scale*(nW+nWoff)),0.5*(sH+scale*(nD+nDoff)),(-0.5)*scale*nHoff);
			
		AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(viewer.state().getViewerTransform(),t,0,0,(long)(btdata.nAnimationDuration*0.5));
		
		viewer.setTransformAnimator(anim);

	}
	
	public Line3D findClickLine()
	{

		java.awt.Point point_mouse  = viewer.getMousePosition();

		if(point_mouse ==null)
		{
			return null;
		}
														
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		viewer.state().getViewerTransform(transform);
		int sW = viewer.getWidth();
		int sH = viewer.getHeight();
		Matrix4f matPerspWorld = new Matrix4f();
		MatrixMath.screenPerspective( btdata.dCam, btdata.dClipNear, btdata.dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		

		Vector3f temp = new Vector3f(); 
		
		//Main click Line 
		RealPoint [] mainLinePoints = new RealPoint[2];
		for (int z =0 ; z<2; z++)
		{
			//take coordinates in original data volume space
			matPerspWorld.unproject((float)point_mouse.x,sH-(float)point_mouse.y,(float)z, //z=1 ->far from camera z=0 -> close to camera
					new int[] { 0, 0, sW, sH },temp);

			mainLinePoints[z] = new RealPoint(temp.x,temp.y,temp.z);			
		}

		Line3D clickLine = new Line3D(mainLinePoints[0],mainLinePoints[1]);

		return clickLine;
	}
	
	/** function that locates user mouse click (in RealPoint target) inside viewclick IntervalView
	 * using frustum of nHalfWindowSize **/
	public <X extends RealType< X >>boolean findPointLocationFromClick(final IntervalView< X > viewclick, final int nHalfWindowSize, final RealPoint target)
	{
		int i,j;

		java.awt.Point point_mouse  = viewer.getMousePosition();
		if(point_mouse ==null)
		{
			return false;
		}
		
		//check if mouse position it is inside bvv window
		//java.awt.Rectangle windowBVVbounds = btpanel.bvv_frame.getContentPane().getBounds();		
		//System.out.println( "click x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
														
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		viewer.state().getViewerTransform(transform);
		int sW = viewer.getWidth();
		int sH = viewer.getHeight();
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
		Cuboid3D dataCube = new Cuboid3D(viewclick); 
		dataCube.iniFaces();
		ArrayList<RealPoint> intersectionPoints = Intersections3D.cuboidLinesIntersect(dataCube, frustimLines);
		// Lines(rays) truncated to the volume.
		// For now, all of them must contained inside datacube.
		
		if(intersectionPoints.size()==8)
		{
			btpanel.progressBar.setString("click point found");
	/*
			for(i =0;i<intersectionPoints.size();i++)
			{
				Point3D point = new Point3D(roiManager.groups.get(0));
				point.setVertex(intersectionPoints.get(i));
				point.setGroupInd(0);
				roiManager.addRoi(point);
			}
			*/
		}		
		else
		{
			btpanel.progressBar.setString("cannot find clicked point");
			//System.out.println( "#intersection points " + intersectionPoints.size());
			return false;
		}
		long [][] nClickMinMax = new long[2][3];
		
		if(VolumeMisc.newBoundBox(viewclick, intersectionPoints, nClickMinMax))
		{

			IntervalView< X > intRay = Views.interval(viewclick, Intervals.createMinMax(nClickMinMax[0][0],nClickMinMax[0][1],nClickMinMax[0][2],
																								   nClickMinMax[1][0],nClickMinMax[1][1],nClickMinMax[1][2]));
			Cuboid3D clickVolume = new Cuboid3D(clickFrustum);
			clickVolume.iniFaces();
			RealPoint target_found = new RealPoint( 3 );
			
			if(VolumeMisc.findMaxLocationCuboid(intRay,target_found,clickVolume))
			{
				btpanel.progressBar.setString("click point found");
				target.setPosition(target_found);
				return true;
			}
			else
			{
				btpanel.progressBar.setString("cannot find clicked point");
				return false;
			}					
		}
		else
		{
			return false;
		}	
		
	}

	@Override
	public void timePointChanged(int timePointIndex) {
					
		if(btdata.nCurrTimepoint != viewer.state().getCurrentTimepoint())
		{
			btdata.nCurrTimepoint = viewer.state().getCurrentTimepoint();
			if(btdata.bDeselectROITime)
			{
				actionDeselect();
			}
			else
			{
				btdata.bDeselectROITime = true;
			}
			//if(btpanel!=null)
			btpanel.updateViewDataSources();
		}

		
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
	
		viewer.stop();
		btpanel.bvv_frame.dispose();		
		btpanel.finFrame.dispose();
	}
	
	public static double [] initBrightnessBVV( final double cumulativeMinCutoff, final double cumulativeMaxCutoff, final ViewerState state )
	{
		final SourceAndConverter< ? > current = state.getCurrentSource();
		if ( current == null )
			return null;
		final Source< ? > source = current.getSpimSource();
		final int timepoint = state.getCurrentTimepoint();
		final Bounds bounds = InitializeViewerState.estimateSourceRange( source, timepoint, cumulativeMinCutoff, cumulativeMaxCutoff );
		double [] out = new double [2];
		out[0]=bounds.getMinBound();
		out[1]=bounds.getMaxBound();
		return out;

	}

	
	@SuppressWarnings("rawtypes")
	public static void main( String... args) throws Exception
	{
		
		new ImageJ();
		BigTrace testI = new BigTrace(); 
		
		testI.run("");
		
		//testI.run("/home/eugene/Desktop/BigTrace_data/ExM_MT_8bit.tif");
		
		/*
		testI.roiManager.setLockMode(true);
		float [] point = new float[3];
//		point[0]=23;
//		point[1]=23;
//		point[2]=10;

		point[0]=37;
		point[1]=61;
		point[2]=10;

		RealPoint target = new RealPoint(point);

		testI.runOneClickTrace(target);
		*/
	/*	testI.roiManager.setLockMode(true);

		point[0]=24;
		point[1]=24;
		point[2]=10;
		target = new RealPoint(point);
		

		testI.runOneClickTrace(target);*/

		
		/**/
		//directionality test
		//testI.run("");
		/*
		//performance test
		testI.run("/home/eugene/Desktop/ExM_MT_8bit-small_crop.tif"); 
		
		
		testI.roiManager.setLockMode(true);
		float [] point = new float[3];
		point[0]=17;
		point[1]=61;
		point[2]=110;
		RealPoint target = new RealPoint(point);

		testI.runOneClickTrace(target);
		
		*/
		
	}



	
}

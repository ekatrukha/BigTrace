package bigtrace;

import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.joml.Matrix4f;
import org.joml.Vector3f;


import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.jogamp.opengl.GL3;

import ij.IJ;
import ij.ImageJ;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.plugin.PlugIn;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.AbstractInterval;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
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
import btbvv.btuitools.GammaConverterSetup;
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
import bigtrace.rois.RoiManager3D;
import bigtrace.scene.VisPolyLineSimple;
import bigtrace.volume.VolumeMisc;


public class BigTrace < T extends RealType< T > & NativeType< T > > implements PlugIn, MacroExtension, WindowListener, TimePointListener
{
	/** main instance of BVV **/
	public  BvvStackSource< ? > bvv_main = null;
	
	/** BVV sources used for the volume visualization **/
	public  ArrayList<BvvStackSource< ? >> bvv_sources = new ArrayList<>();
	
	/** saliency view (TraceBox) for semi-auto tracing **/
	public  BvvStackSource< UnsignedByteType > bvv_trace = null;

	/** whether or not TraceMode is active **/
	public boolean bTraceMode = false;
	
	/** input from XML/HDF5 or BioFormats (cached) **/
	public SpimData spimData;
	
	/** input data in RAI XYZC format**/
	public RandomAccessibleInterval<T> all_ch_RAI;
	
	/** whether LLS transform was applied **/	
	public boolean bApplyLLSTransform = false;
	
	/** LLS transform **/
	public AffineTransform3D afDataTransform = new AffineTransform3D();

	/** Panel of BigVolumeViewer **/
	public VolumeViewerPanel viewer;
	
	/** flag to check if user interface is frozen **/
	public volatile boolean bInputLock = false;
	
	/** visualization of coordinates origin axes **/
	ArrayList<VisPolyLineSimple> originVis = new ArrayList<>();

	/** box around volume **/
	Box3D volumeBox;
	
	/** helper box to visualize one-click tracing things **/
	public Box3D visBox = null;
	
	/** helper box to visualize one-click tracing things **/
	public Box3D clipBox = null;

	/** object storing main data/variables **/
	public BigTraceData<T> btData;
	
	/** object loading data **/
	public BigTraceLoad<T> btLoad;	
	
	/** BigTrace interface panel **/
	public BigTraceControlPanel<T> btPanel;
	
	/** BigTrace main actions **/
	BigTraceActions<T> btActions;
	
	/** ROI manager + list tab **/
	public RoiManager3D<T> roiManager;
	
	/** BigTrace macro interface**/
	BigTraceMacro<T> btMacro;
	
	/**macro extensions **/
	private ExtensionDescriptor[] extensions = {
			
			ExtensionDescriptor.newDescriptor("btLoadROIs", this, ARG_STRING, ARG_STRING),	
			ExtensionDescriptor.newDescriptor("btStraighten", this, ARG_NUMBER, ARG_STRING, ARG_STRING),	
			ExtensionDescriptor.newDescriptor("btShapeInterpolation", this, ARG_STRING,ARG_NUMBER),
			ExtensionDescriptor.newDescriptor("btIntensityInterpolation", this, ARG_STRING),
			ExtensionDescriptor.newDescriptor("btTest", this),
			ExtensionDescriptor.newDescriptor("btClose", this),
			ExtensionDescriptor.newDescriptor("btTest", this),

	};
		
	@Override
	public void run(String arg)
	{
		//lock interface for initialization
		bInputLock = true;
		
		btMacro = new BigTraceMacro<>(this);
		//register IJ macro extensions
		if (IJ.macroRunning())
		{
			Functions.registerExtensions(this);
		}
		
		
		
		//switch to FlatLaf theme		
		try {
		    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		    FlatLaf.registerCustomDefaultsSource( "flatlaf" );
		    FlatIntelliJLaf.setup();
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		
		btData = new BigTraceData<>(this);
		btLoad = new BigTraceLoad<>(this);
		
		
		if(arg.equals(""))
		{
			btData.sFileNameFullImg = IJ.getFilePath("Open TIF/BDV/Bioformats file (3D, composite, time)...");
		}
		else
		{
			btData.sFileNameFullImg = arg;
		}

		if(btData.sFileNameFullImg == null)
			return;
		
		//load data sources
		
		// TIF files are fully loaded (to RAM) for now
		if(btData.sFileNameFullImg.endsWith(".tif"))
		{
			btData.bSpimSource = false;
			if(!btLoad.initDataSourcesImageJ())
				return;
		}
		else
		{
			// BDV XML/HDF5 format 
			btData.bSpimSource = true;
			if(btData.sFileNameFullImg.endsWith(".xml"))
			{				
				try {
					if(!btLoad.initDataSourcesHDF5())
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
				String outLog = btLoad.initDataSourcesBioFormats(); 
				if(outLog != null)
				{
					IJ.error(outLog);
					return;
				}
			
			}
		}	

		roiManager = new RoiManager3D<>(this);
		
		initSourcesCanvas(0.25*Math.min(btData.nDimIni[1][0], Math.min(btData.nDimIni[1][1],btData.nDimIni[1][2])));
		
		//not sure we really need it, but anyway
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
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
			ArrayList< RealPoint > point_coords = new ArrayList< >();
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
			nDimBox[0][i]=btData.nDimIni[0][i]+0.5f;
			nDimBox[1][i]=(btData.nDimIni[1][i]-1.0f);
		}
		volumeBox = new Box3D(nDimBox,0.5f,0.0f,Color.LIGHT_GRAY,Color.LIGHT_GRAY, 0);
		clipBox = new Box3D(nDimBox,0.5f,0.0f,Color.LIGHT_GRAY,Color.LIGHT_GRAY, 0);
	}
	public void initSourcesCanvas(double origin_axis_length)
	{
		
		initOriginAndBox(origin_axis_length);
	
		if(btData.bSpimSource)
		{
			initBVVSourcesSpimData();
		}
		else
		{
			initBVVSourcesImageJ();
		}
		
		viewer = bvv_main.getBvvHandle().getViewerPanel();
		viewer.setRenderScene(this::renderScene);
		btActions  = new BigTraceActions<>(this);
		setInitialTransform();
		viewer.addTimePointListener(this);
	}
	
	
	private void createAndShowGUI() 
	{
		btPanel = new BigTraceControlPanel<>(this, btData,roiManager);
		btPanel.finFrame = new JFrame("BigTrace");

		btPanel.bvv_frame=(JFrame) SwingUtilities.getWindowAncestor(viewer);
	 	
		btPanel.finFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		btPanel.bvv_frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		btPanel.finFrame.add(btPanel);
		
        //Display the window.
		btPanel.finFrame.setSize(400,600);
		btPanel.finFrame.setVisible(true);
	    java.awt.Point bvv_p = btPanel.bvv_frame.getLocationOnScreen();
	    java.awt.Dimension bvv_d = btPanel.bvv_frame.getSize();
	
	    btPanel.finFrame.setLocation(bvv_p.x+bvv_d.width, bvv_p.y);
	    btPanel.finFrame.addWindowListener(this);
	    btPanel.bvv_frame.addWindowListener(this);
		bInputLock = false;
	}
	
	

	public void focusOnInterval(Interval interval_in)
	{
		if(!bInputLock && !bTraceMode)
		{
			viewer.setTransformAnimator(getCenteredViewAnim(interval_in,btData.dZoomBoxScreenFraction));
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
		
		traceIV = getTraceInterval(btData.bTraceOnlyClipped);
		
		if(trace.numVertices()==1)
		{
			rangeTraceBox = getTraceBoxCentered(traceIV,btData.lTraceBoxSize, trace.vertices.get(0));
		}
		else
		{
			rangeTraceBox = getTraceBoxNext(traceIV,btData.lTraceBoxSize, btData.fTraceBoxAdvanceFraction, trace);
		}
		
		IntervalView<?> traceInterval = Views.interval(traceIV, rangeTraceBox);
		
		//getCenteredView(traceInterval);
		viewer.setTransformAnimator(getCenteredViewAnim(traceInterval,btData.dTraceBoxScreenFraction));
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
		calcTask.addPropertyChangeListener(btPanel);
		calcTask.execute();
		//System.out.println("+corners: elapsed Time in milli seconds: "+ (end1-start1));		

		//showTraceBox(btdata.trace_weights);
		btData.nPointsInTraceBox = 1;
	}
	
	/** calculates trace box around last vertice of provided trace.
	 * if bRefine is true, it will refine the position of the dot
	 * and add it to the ROI manager **/
	//@SuppressWarnings({ "rawtypes", "unchecked" })
	public void runOneClickTrace(final RealPoint pclick, final boolean bNewTrace_)
	{
		
		final IntervalView<T> traceIV =  getTraceInterval(btData.bTraceOnlyClipped);	
//		System.out.println(pclick.getDoublePosition(0));
//		System.out.println(pclick.getDoublePosition(1));
//		System.out.println(pclick.getDoublePosition(2));

		bInputLock = true;
		OneClickTrace<T> calcTask = new OneClickTrace<>();
		calcTask.fullInput = traceIV;
		calcTask.bt = this;
		calcTask.startPoint = pclick;
		calcTask.bNewTrace = bNewTrace_;
		calcTask.addPropertyChangeListener(btPanel);
		calcTask.execute();
	
	}
	
	/** returns current Interval for the tracing. If bClippedInterval is true,
	 * returns clipped volume, otherwise returns full original volume. **/
	public IntervalView<T> getTraceInterval(boolean bClippedInterval)
	{
		if(bClippedInterval)
		{
			return btData.getDataSourceClipped(btData.nChAnalysis, btData.nCurrTimepoint);
		}
		RandomAccessibleInterval<T> full_int = btData.getDataSourceFull(btData.nChAnalysis, btData.nCurrTimepoint);
		
		return Views.interval(full_int, full_int);
	}
		
	/** calculate optimal path **/
	public void getSemiAutoTrace(RealPoint target)
	{
		
		bInputLock = true;
		TracingBGVect traceBG = new TracingBGVect();
		traceBG.target = target;
		traceBG.bt=this;
		traceBG.addPropertyChangeListener(btPanel);
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
			centerCoord[i] = Math.round(minDim[i]+ 0.5*(maxDim[i]-minDim[i]));
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
		MatrixMath.screenPerspective( btData.dCam, btData.dClipNear, btData.dClipFar, sW, sH, 0, matPersp );
		
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
		
		final AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(transform,transform_scale,0,0,btData.nAnimationDuration);			
		
		return anim;
	}

	public void showTraceBox()
	{

		// there is a trace box already, let's remove it
		if(bvv_trace!=null)
		{
			btData.bcTraceBox.storeBC(bvv_trace);
			bvv_trace.removeFromBdv();
			System.gc();
		}
		//there is no tracebox, let's dim the main volume first
		else
		{
			if(btData.nTraceBoxView==1)
			{
				btData.bcTraceChannel.storeBC(bvv_sources.get(btData.nChAnalysis));
				bvv_sources.get(btData.nChAnalysis).setDisplayRange(0.0, 0.0);
				bvv_sources.get(btData.nChAnalysis).setAlphaRange(0.0, 0.0);
			}
			
		}

		bvv_trace = BvvFunctions.show(btData.trace_weights, "weights", Bvv.options().addTo(bvv_main));
		bvv_trace.setCurrent();
		bvv_trace.setRenderType(btData.nRenderMethod);
		bvv_trace.setDisplayRangeBounds(0, 255);
		bvv_trace.setAlphaRangeBounds(0, 255);
		if(btData.bcTraceBox.bInit)
		{
			btData.bcTraceBox.setBC(bvv_trace);
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
			btData.bcTraceBox.storeBC(bvv_trace);
			bvv_trace.removeFromBdv();
			System.gc();
		}
		bvv_trace=null;
		//handl.setDisplayMode(DisplayMode.SINGLE);
		if(btData.nTraceBoxView==1)
		{
			btData.bcTraceChannel.setBC(bvv_sources.get(btData.nChAnalysis));
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
			viewer.state().getViewerTransform(btData.transformBeforeTracing);
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
			final AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(transform,btData.transformBeforeTracing,0,0,1500);
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
		final Matrix4f camview = MatrixMath.screen( btData.dCam, screen_size[0], screen_size[1], new Matrix4f() ).mul( view );

		
		//to be able to change point size in shader
		gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
		
		synchronized (roiManager)
		{
			roiManager.draw(gl, pvm, camview, screen_size);
		}	
		
			//render the origin of coordinates
			if (btData.bShowOrigin)
			{
				for (int i=0;i<3;i++)
				{
					originVis.get(i).draw(gl, pvm);
				}
			}
			
			//render a box around  the volume 
			if (btData.bVolumeBox)
			{
				volumeBox.draw(gl, pvm, camview, screen_size);
			}
			//render a box around  the volume 
			if (btData.bClipBox)
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
		
		Path p = Paths.get(btData.sFileNameFullImg);
		String filename = p.getFileName().toString();
		
		Bvv Tempbvv = BvvFunctions.show( Bvv.options().
				dCam(btData.dCam).
				dClipNear(btData.dClipNear).
				dClipFar(btData.dClipFar).				
				renderWidth( BigTraceData.renderParams.renderWidth).
				renderHeight( BigTraceData.renderParams.renderHeight).
				numDitherSamples( BigTraceData.renderParams.numDitherSamples ).
				cacheBlockSize( BigTraceData.renderParams.cacheBlockSize ).
				maxCacheSizeInMB( BigTraceData.renderParams.maxCacheSizeInMB ).
				ditherWidth(BigTraceData.renderParams.ditherWidth).
				frameTitle(filename)
				);

		for(int i=0;i<btData.nTotalChannels;i++)
		{
	
			bvv_sources.add(BvvFunctions.show( Views.hyperSlice(all_ch_RAI,4,i), "ch_"+Integer.toString(i+1), Bvv.options().addTo(Tempbvv)));
			if(btData.nBitDepth<=8)
			{
				bvv_sources.get(i).setDisplayRangeBounds(0, 255);
				bvv_sources.get(i).setAlphaRangeBounds(0, 255);
			}
			else
			{
				bvv_sources.get(i).setDisplayRangeBounds(0, 65535);
				bvv_sources.get(i).setAlphaRangeBounds(0, 65535);
			}
			bvv_sources.get(i).setColor( new ARGBType( btLoad.colorsCh[i].getRGB() ));
			bvv_sources.get(i).setDisplayRange(btLoad.channelRanges[0][i], btLoad.channelRanges[1][i]);
			bvv_sources.get(i).setAlphaRange(btLoad.channelRanges[0][i], btLoad.channelRanges[1][i]);
			bvv_sources.get(i).setRenderType(btData.nRenderMethod);

		}
		
		bvv_main = bvv_sources.get(0);

		viewer = bvv_main.getBvvHandle().getViewerPanel();
	}
	
	public void initBVVSourcesSpimData()
	{

		Path p = Paths.get(btData.sFileNameFullImg);
		String filename = p.getFileName().toString();
		List<BvvStackSource<?>> sourcesSPIM = BvvFunctions.show(spimData,Bvv.options().
				dCam(btData.dCam).
				dClipNear(btData.dClipNear).
				dClipFar(btData.dClipFar).				
				renderWidth( BigTraceData.renderParams.renderWidth).
				renderHeight( BigTraceData.renderParams.renderHeight).
				numDitherSamples( BigTraceData.renderParams.numDitherSamples ).
				cacheBlockSize( BigTraceData.renderParams.cacheBlockSize ).
				maxCacheSizeInMB( BigTraceData.renderParams.maxCacheSizeInMB ).
				ditherWidth(BigTraceData.renderParams.ditherWidth).
				dCam( btData.dCam ).
				dClipNear( btData.dClipNear ).
				dClipFar( btData.dClipFar ).
				frameTitle(filename)//.
				//sourceTransform(afDataTransform)
				);		
		
		for(int i=0;i<sourcesSPIM.size();i++)
		{
			bvv_sources.add(sourcesSPIM.get(i));
			bvv_sources.get(i).setRenderType(btData.nRenderMethod);
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

		if(bApplyLLSTransform)
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
		t = getCenteredViewTransform(new FinalInterval(BigTraceData.nDimCurr[0],BigTraceData.nDimCurr[1]), 0.9);
		viewer.state().setViewerTransform(t);
	}
	
	public void resetViewXY()
	{
		
		double scale;
		final long [][] nBox;
		if(!bTraceMode)
		{
			nBox = BigTraceData.nDimCurr;
		}
		else
		{
			nBox = new long [2][3];
			nBox[0] = btData.trace_weights.minAsLongArray();
			nBox[1] = btData.trace_weights.maxAsLongArray();
		}
		
		final double nW = (nBox[1][0]-nBox[0][0])*BigTraceData.globCal[0];
		final double nH = (nBox[1][1]-nBox[0][1])*BigTraceData.globCal[1];
		final double nWoff = 2.0*nBox[0][0]*BigTraceData.globCal[0];
		final double nHoff = 2.0*nBox[0][1]*BigTraceData.globCal[1];
		final double nDoff = 2.0*nBox[0][2]*BigTraceData.globCal[2];
		
		final double sW = viewer.getWidth();
		final double sH = viewer.getHeight();
		
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
		
		AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(viewer.state().getViewerTransform(),t,0,0,(long)(btData.nAnimationDuration*0.5));
		viewer.setTransformAnimator(anim);
			
	}
	
	public void resetViewYZ()
	{
		
		double scale;
		final long [][] nBox;
		if(!bTraceMode)
		{
			nBox = BigTraceData.nDimCurr;
		}
		else
		{
			nBox = new long [2][3];
			nBox[0] = btData.trace_weights.minAsLongArray();
			nBox[1] = btData.trace_weights.maxAsLongArray();
		}
		final double nH = (nBox[1][1]-nBox[0][1])*BigTraceData.globCal[1];
		final double nD = (nBox[1][2]-nBox[0][2])*BigTraceData.globCal[2];
		final double nWoff = 2.0*nBox[0][0]*BigTraceData.globCal[0];
		final double nHoff = 2.0*nBox[0][1]*BigTraceData.globCal[1];
		final double nDoff = 2.0*nBox[0][2]*BigTraceData.globCal[2];
		final double sW = viewer.getWidth();
		final double sH = viewer.getHeight();
		
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
		
		AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(viewer.state().getViewerTransform(),t,0,0,(long)(btData.nAnimationDuration*0.5));
		
		viewer.setTransformAnimator(anim);

	}
	
	public void resetViewXZ()
	{
		
		double scale;
		final long [][] nBox;
		if(!bTraceMode)
		{
			nBox = BigTraceData.nDimCurr;
		}
		else
		{
			nBox = new long [2][3];
			nBox[0] = btData.trace_weights.minAsLongArray();
			nBox[1] = btData.trace_weights.maxAsLongArray();
		}
		final double nW = (nBox[1][0]-nBox[0][0])*BigTraceData.globCal[0];
		final double nD = (nBox[1][2]-nBox[0][2])*BigTraceData.globCal[2];
		final double nWoff = 2.0*nBox[0][0]*BigTraceData.globCal[0];
		final double nHoff = 2.0*nBox[0][1]*BigTraceData.globCal[1];
		final double nDoff = 2.0*nBox[0][2]*BigTraceData.globCal[2];
		final double sW = viewer.getWidth();
		final double sH = viewer.getHeight();
		
		if(sW/nW<sH/nD)
		{
			scale = sW/nW;
		}
		else
		{
			scale = sH/nD;
		}
		
		scale = 0.9*scale;
		final AffineTransform3D t = new AffineTransform3D();
	
		t.identity();
		
		t.scale(BigTraceData.globCal[0]*scale, BigTraceData.globCal[1]*scale, BigTraceData.globCal[2]*scale);
		t.rotate(0, Math.PI/2.0);
		t.translate(0.5*(sW-scale*(nW+nWoff)),0.5*(sH+scale*(nD+nDoff)),(-0.5)*scale*nHoff);
			
		final AnisotropicTransformAnimator3D anim = new AnisotropicTransformAnimator3D(viewer.state().getViewerTransform(),t,0,0,(long)(btData.nAnimationDuration*0.5));
		
		viewer.setTransformAnimator(anim);

	}
	/** given mouse click coordinates, returns the line along the view ray **/
	public Line3D findClickLine(final java.awt.Point point_mouse)
	{
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		viewer.state().getViewerTransform(transform);
		int sW = viewer.getWidth();
		int sH = viewer.getHeight();
		Matrix4f matPerspWorld = new Matrix4f();
		MatrixMath.screenPerspective( btData.dCam, btData.dClipNear, btData.dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		

		Vector3f temp = new Vector3f(); 
		
		//Main click Line 
		RealPoint [] mainLinePoints = new RealPoint[2];
		for (int z =0 ; z<2; z++)
		{
			//take coordinates in original data volume space
			matPerspWorld.unproject(point_mouse.x,sH-(float)point_mouse.y,z, //z=1 ->far from camera z=0 -> close to camera
					new int[] { 0, 0, sW, sH },temp);

			mainLinePoints[z] = new RealPoint(temp.x,temp.y,temp.z);			
		}

		Line3D clickLine = new Line3D(mainLinePoints[0],mainLinePoints[1]);

		return clickLine;
	}
	public Line3D findClickLine()
	{

		java.awt.Point point_mouse  = viewer.getMousePosition();

		if(point_mouse ==null)
		{
			return null;
		}
														
		return findClickLine(point_mouse);
	}
	
	/** function that locates user mouse click (in RealPoint target) inside viewclick IntervalView
	 * using frustum of nHalfWindowSize **/
	public <X extends RealType< X >>boolean findPointLocationFromClick(final IntervalView< X > viewclick, final RealPoint target)
	{
		
		java.awt.Point point_mouse  = viewer.getMousePosition();
		if(point_mouse ==null)
		{
			return false;
		}
		// ??? maybe move these functions to BVV to speed up?? 
		if(btData.nRenderMethod == BigTraceData.DATA_RENDER_MAX_INT)
		{
			//long start1, end1;
			//start1 = System.currentTimeMillis();
			//findPointLocationMaxIntensity(viewclick, point_mouse, target);
			//end1 = System.currentTimeMillis();
			//System.out.println("alg1: "+ (end1-start1));
			//start1 = System.currentTimeMillis();
			boolean bRes =findPointLocationMaxIntensity(viewclick, point_mouse, target);
			//end1 = System.currentTimeMillis();
			//System.out.println("alg2: "+ (end1-start1));
			return bRes;
		}
		else
		if(btData.nRenderMethod == BigTraceData.DATA_RENDER_VOLUMETRIC)
		{
			return findPointLocationVolumetric(viewclick, point_mouse, target);
		}
		
		return false;		
		
	}
	/** find click location in 3D when using volumetric render 
	 **/
	public <X extends RealType< X >>boolean findPointLocationVolumetric(final IntervalView< X > viewclick, java.awt.Point point_mouse_in, final RealPoint target)
	{
		//view line
		Line3D viewLine;
		//current dataset
		final Cuboid3D dataCube = new Cuboid3D(viewclick);
		ArrayList<RealPoint> intersectionPoints; 
		final AffineTransform3D transform = new AffineTransform3D();
		final RealPoint firstP = new RealPoint (3);
		final RealPoint lastP = new RealPoint (3);
		
		double [] closeP = new double [3];
		double [] farP = new double [3];
		double [] vect = new double [3];
		double totLength;
		final int nHalfWindowSize = btData.nHalfClickSizeWindow;
		int indC = 0;
		int indF = 1;
		boolean bFound;

		// not sure it is the best way, but it works
		GammaConverterSetup setup = null;
		int nChCount = 0;
		for ( SourceAndConverter< ? > source : viewer.state().getSources() )
		{
			if(nChCount == btData.nChAnalysis )
			{
				setup = (GammaConverterSetup) viewer.getConverterSetups().getConverterSetup(source);
			}
			nChCount++;
		}
		
		if(setup == null)
			return false;

		final double aOffset = setup.getAlphaRangeMin();
		final double aScale = Math.max(setup.getAlphaRangeMax() - aOffset,1.0);
		final double aGamma = 1.0/setup.getAlphaGamma();
		RandomAccess<X> raZ = Views.extendZero(viewclick).randomAccess();
		double alpha = 0;
		double curr_a = 0.0;
		double curr_v = 0.0;

		final double dStep = 0.5;// seems like a good step
		double dCurrStep = 0.0;
		Point finalP = new Point(3);
		
		ArrayList<Point> foundPositions = new ArrayList<>();
		ArrayList<Double> foundValues = new ArrayList<>();
		
		//init stuff
		viewer.state().getViewerTransform(transform);
		dataCube.iniFaces();
		java.awt.Point point_mouse = new java.awt.Point();
		
		for (int dx = -nHalfWindowSize;dx<(nHalfWindowSize+1); dx++)
			for (int dy = -nHalfWindowSize;dy<(nHalfWindowSize+1); dy++)
			{
				point_mouse.x = point_mouse_in.x + dx;
				point_mouse.y = point_mouse_in.y + dy;
				viewLine = findClickLine(point_mouse);
		
				intersectionPoints = Intersections3D.cuboidLinesIntersect(dataCube, viewLine);
				
				if(intersectionPoints.size()!=2)
				{
					return false;
				}
				//we have 2 intersection points
				//let's figure out which one is closer to the viewer
				transform.apply(intersectionPoints.get(0), firstP);
				transform.apply(intersectionPoints.get(1), lastP);
				
				indC = 0;
				indF = 1;
				
				if(firstP.getFloatPosition(2)>lastP.getFloatPosition(2))
				{
					indF = 0;
					indC = 1;
				}
					
				for(int d=0;d<3; d++)
				{
					closeP[d] = intersectionPoints.get(indC).getDoublePosition(d);
					farP[d] = intersectionPoints.get(indF).getDoublePosition(d);
				}
				
				//find the vector between two points	
				LinAlgHelpers.subtract(farP, closeP, vect);
				totLength = LinAlgHelpers.length(vect);
				LinAlgHelpers.normalize(vect);
		
				bFound = false;
		
				alpha = 0;
				curr_a = 0.0;
				curr_v = -1.0;
				
				for(dCurrStep = 0.0; dCurrStep < totLength &&!bFound; dCurrStep+=dStep)
				{
					//set position
					LinAlgHelpers.scale(vect, dCurrStep, farP);
					LinAlgHelpers.add(farP, closeP, farP);
					for(int d=0;d<3;d++)
					{
						finalP.setPosition(Math.round(farP[d]), d);
					}
					//closeP.setPosition(viewSegment.get(i));
					raZ.setPosition(finalP);
					curr_v = raZ.get().getRealDouble();
					curr_a = Math.pow(VolumeMisc.clamp((curr_v-aOffset)/aScale, 0.0, 1.0), aGamma);
					//curr_v = Math.pow(VolumeMisc.clamp(vOffset+vScale*curr_v, 0.0, 1.0), vGamma);
					//value += (1-alpha)*curr_v*curr_a;
					alpha += (1-alpha)*curr_a;
					if(alpha>0.99)
						bFound = true;
				}
				foundPositions.add(finalP.positionAsPoint());
				foundValues.add(curr_v);
			}
		int finInd = 0;
		double maxVal = (-1)*Double.MAX_VALUE;
		for(int i=0;i<foundValues.size();i++)
		{
			if(foundValues.get(i)>maxVal)
			{
				maxVal = foundValues.get(i);
				finInd = i;
			}
			
		}
		//find max around it
		HyperSphere< X > hyperSphere =
				new HyperSphere<>( Views.extendZero(viewclick), foundPositions.get(finInd), 3);
		//RealPoint outP = new RealPoint(3);
		VolumeMisc.findMaxLocation(hyperSphere, target);
		
		//target.setPosition(foundPositions.get(finInd));
		
		
		return true;	
	}
	/** function finds a voxel with max intensity value
	 *  In case of success returns true and coordinates of voxel in target **/
	public <X extends RealType< X >> boolean findPointLocationMaxIntensity(final IntervalView< X > viewclick, java.awt.Point point_mouse_in, final RealPoint target)
	{
		//view line
		Line3D viewLine;
		//current dataset
		final Cuboid3D dataCube = new Cuboid3D(viewclick);
		ArrayList<RealPoint> intersectionPoints; 
		
		double [] closeP = new double [3];
		double [] farP = new double [3];
		double [] vect = new double [3];
		double totLength;
		final int nHalfWindowSize = btData.nHalfClickSizeWindow;

		RandomAccess<X> raZ = Views.extendZero(viewclick).randomAccess();
		double curr_v = 0.0;

		final double dStep = 0.5;// seems like a good step
		double dCurrStep = 0.0;
		Point finalP = new Point(3);
		
		Point foundMaxPosition = new Point(3);
		double foundMaxValue = (-1)*Double.MAX_VALUE;
		
		//init stuff

		dataCube.iniFaces();
		java.awt.Point point_mouse = new java.awt.Point();
		
		for (int dx = -nHalfWindowSize;dx<(nHalfWindowSize+1); dx++)
			for (int dy = -nHalfWindowSize;dy<(nHalfWindowSize+1); dy++)
			{
				point_mouse.x = point_mouse_in.x + dx;
				point_mouse.y = point_mouse_in.y + dy;
				viewLine = findClickLine(point_mouse);
		
				intersectionPoints = Intersections3D.cuboidLinesIntersect(dataCube, viewLine);
				
				if(intersectionPoints.size()!=2)
				{
					return false;
				}
				//we have 2 intersection points
					
				for(int d=0;d<3; d++)
				{
					closeP[d] = intersectionPoints.get(0).getDoublePosition(d);
					farP[d] = intersectionPoints.get(1).getDoublePosition(d);
				}
				
				//find the vector between two points	
				LinAlgHelpers.subtract(farP, closeP, vect);
				totLength = LinAlgHelpers.length(vect);
				LinAlgHelpers.normalize(vect);
				curr_v = -1.0;
				
				for(dCurrStep = 0.0; dCurrStep < totLength; dCurrStep+=dStep)
				{
					//set position
					LinAlgHelpers.scale(vect, dCurrStep, farP);
					LinAlgHelpers.add(farP, closeP, farP);
					for(int d=0;d<3;d++)
					{
						finalP.setPosition(Math.round(farP[d]), d);
					}
					//closeP.setPosition(viewSegment.get(i));
					raZ.setPosition(finalP);
					curr_v = raZ.get().getRealDouble();
					if(curr_v > foundMaxValue)
					{
						foundMaxValue = curr_v;
						foundMaxPosition.setPosition(finalP);
					}
				}
			}
		target.setPosition(foundMaxPosition);
		//System.out.println("al2 val:"+Double.toString(foundMaxValue));
		//System.out.println("al2:"+Integer.toString(foundMaxPosition.getIntPosition(0))+" "+Integer.toString(foundMaxPosition.getIntPosition(1))+" "+Integer.toString(foundMaxPosition.getIntPosition(2)));
		
		return true;	
	}
	/** find click location in 3D when using maximum intensity render **/
	@Deprecated
	public <X extends RealType< X >>boolean findPointLocationMaxIntensityOLD(final IntervalView< X > viewclick, java.awt.Point point_mouse, final RealPoint target)
	{
		int i,j;
		//check if mouse position it is inside bvv window
		//java.awt.Rectangle windowBVVbounds = btpanel.bvv_frame.getContentPane().getBounds();		
		//System.out.println( "click x = [" + point_mouse.x + "], y = [" + point_mouse.y + "]" );
			
		final int nHalfWindowSize = btData.nHalfClickSizeWindow;
		//get perspective matrix:
		AffineTransform3D transform = new AffineTransform3D();
		viewer.state().getViewerTransform(transform);
		int sW = viewer.getWidth();
		int sH = viewer.getHeight();
		Matrix4f matPerspWorld = new Matrix4f();
		MatrixMath.screenPerspective( btData.dCam, btData.dClipNear, btData.dClipFar, sW, sH, 0, matPerspWorld ).mul( MatrixMath.affine( transform, new Matrix4f() ) );
		
		
		ArrayList<RealPoint> clickFrustum = new ArrayList<> ();
		Vector3f temp = new Vector3f(); 
		
		//float [] zVals = new float []{0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f};
		for (i = -nHalfWindowSize;i<3*nHalfWindowSize;i+=2*nHalfWindowSize)
			for (j = -nHalfWindowSize;j<3*nHalfWindowSize;j+=2*nHalfWindowSize)
				for (int z =0 ; z<2; z++)
				{
					//take coordinates in original data volume space
					matPerspWorld.unproject((float)point_mouse.x+i,sH-(float)point_mouse.y+j,z, //z=1 ->far from camera z=0 -> close to camera
							new int[] { 0, 0, sW, sH },temp);
					//persp.unproject((float)point_mouse.x+i,sH-(float)point_mouse.y+j,zVals[nCount+1], //z=1 ->far from camera z=0 -> close to camera
					//new int[] { 0, 0, sW, sH },temp);

					clickFrustum.add(new RealPoint(temp.x,temp.y,temp.z));
					
				}
		//build lines (rays)
		ArrayList<Line3D> frustumLines = new ArrayList<>();
		for(i =0;i<clickFrustum.size();i+=2)
		{
			frustumLines.add(new Line3D(clickFrustum.get(i),clickFrustum.get(i+1)));
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
		ArrayList<RealPoint> intersectionPoints = Intersections3D.cuboidLinesIntersect(dataCube, frustumLines);
		// Lines(rays) truncated to the volume.
		// For now, all of them must contained inside datacube.
		
		if(intersectionPoints.size()==8)
		{
			btPanel.progressBar.setString("click point found");
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
			btPanel.progressBar.setString("cannot find clicked point");
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
				btPanel.progressBar.setString("click point found");
				target.setPosition(target_found);
				return true;
			}
			btPanel.progressBar.setString("cannot find clicked point");
			return false;					
		}
		return false;	
	}
	
	
	@Override
	public void timePointChanged(int timePointIndex) 
	{
					
		if(btData.nCurrTimepoint != timePointIndex)
		{
			btData.nCurrTimepoint = timePointIndex;
			if(btData.bDeselectROITime)
			{
				btActions.actionDeselect();
			}
			else
			{
				btData.bDeselectROITime = true;
			}
			//if(btpanel!=null)
			btPanel.updateViewDataSources();
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
		btPanel.bvv_frame.dispose();		
		btPanel.finFrame.dispose();
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

	
	@Override
	public ExtensionDescriptor[] getExtensionFunctions() {
		return extensions;
	}
	
	@Override
	public String handleExtension(String name, Object[] args) {
	
		try
		{
			if (name.equals("btLoadROIs")) 
			{
				btMacro.macroLoadROIs( (String)args[0],(String)args[1]);
			}
			if (name.equals("btStraighten")) 
			{
				if(args.length==2)
				{
					//backwards compartibility
					btMacro.macroStraighten((int)Math.round(((Double)args[0]).doubleValue()), (String)args[1], "Square");					
				}
				else
				{
					btMacro.macroStraighten((int)Math.round(((Double)args[0]).doubleValue()), (String)args[1], (String)args[2]);
				}
			}
			if (name.equals("btShapeInterpolation")) 
			{
				btMacro.macroShapeInterpolation( (String)args[0],(int)Math.round(((Double)args[1]).doubleValue()));
			}
			if (name.equals("btIntensityInterpolation")) 
			{
				btMacro.macroIntensityInterpolation( (String)args[0]);
			}
			if (name.equals("btClose")) 
			{
				btMacro.macroCloseBT();			
			}
			if (name.equals("btTest")) 
			{
				btMacro.macroTest();
			} 
		}
		catch ( InterruptedException exc )
		{
			exc.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public static void main(String... args) throws Exception
	{
		
		new ImageJ();
		BigTrace testI = new BigTrace(); 
		
		testI.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_data/ExM_MT.tif");
		
		//testI.run("");
		///macros test
//		testI.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_data/ExM_MT_8bit.tif");
//		testI.btMacro.macroLoadROIs( "/home/eugene/Desktop/projects/BigTrace/macro/ExM_MT_8bit.tif_btrois.csv","Clean" );
//		IJ.log( "shapeInt1" );
//		testI.btMacro.macroShapeInterpolation("Voxel", 10);
//		testI.btMacro.macroIntensityInterpolation("Neighbor");
//		IJ.log( "straight1" );
//		testI.btMacro.macroStraighten(1, "/home/eugene/Desktop/test1/");
//		IJ.log( "shapeInt2" );
//		testI.btMacro.macroShapeInterpolation("Spline", 10);
//		testI.btMacro.macroIntensityInterpolation("Linear");
//		IJ.log( "straight2" );
//		testI.btMacro.macroStraighten(1, "/home/eugene/Desktop/test2/");

		//testI.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_data/ExM_MT.tif");
		//testI.run("/home/eugene/Desktop/projects/BigTrace/BT_tracks/Snejana_small_example.tif");
		
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

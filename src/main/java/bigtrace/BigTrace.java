
package bigtrace;



import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.Insets;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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


import bigtrace.geometry.Cuboid3D;
import bigtrace.geometry.Intersections3D;
import bigtrace.geometry.Line3D;
import bigtrace.gui.CropPanel;
import bigtrace.gui.PanelTitle;
import bigtrace.math.DerivConvolutionKernels;
import bigtrace.math.EigenValVecSymmDecomposition;
import bigtrace.polyline.BTPolylines;
import bigtrace.rois.RoiManager3D;
import bigtrace.scene.VisPointsSimple;
import bigtrace.scene.VisPolyLineSimple;

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
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;

import net.imglib2.img.array.ArrayImgs;
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
import tpietzsch.example2.VolumeViewerPanel;
import tpietzsch.util.MatrixMath;


public class BigTrace
{
	public  BvvStackSource< UnsignedByteType > bvv;
	public  BvvStackSource< UnsignedByteType > bvv2;
	RandomAccessibleInterval< UnsignedByteType > view;
	static IntervalView< UnsignedByteType > view2=null;
	IntervalView< FloatType > gauss=null;
	Img< UnsignedByteType> img;
	VolumeViewerPanel handl;
	//SynchronizedViewerState state;
	CropPanel cropPanel;
	

	long [][] nDimCurr = new long [2][3];
	long [][] nDimIni = new long [2][3];
	double dCam = 1100.;
	double dClipNear = 1000.;
	double dClipFar = 1000.;
	
	boolean bShowWorldGrid = false;
	boolean bShowOrigin = true;
	
	//ArrayList< RealPoint > point_coords = new ArrayList<>();
	BTPolylines traces = new BTPolylines ();
	
	
	BTPolylines origin_data = new BTPolylines ();

	RoiManager3D roiManager = new RoiManager3D();
	
		
	public void runBVV()
	{
		
		
		//img = SimplifiedIO.openImage(
					//test_BVV_inteface.class.getResource( "home/eugene/workspace/ExM_MT.tif" ).getFile(),
					//new UnsignedByteType() );
		img = SimplifiedIO.openImage("/home/eugene/workspace/ExM_MT.tif", new UnsignedByteType());
		//img = SimplifiedIO.openImage("/home/eugene/workspace/linetest.tif", new UnsignedByteType());
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
		
		
		
		initMainDialog();



//		bdv.getBdvHandle().getKeybindings().removeInputMap( "BigTrace" );

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
		
		//traces = new BTPolylines ();
		
		// init bigvolumeviewer
		final Img< UnsignedByteType > imgx = ArrayImgs.unsignedBytes( new long[]{ 2, 2, 2 } );
		view =				 
				 Views.interval( imgx, new long[] { 0, 0, 0 }, new long[]{ 1, 1, 1 } );
		
						
		bvv = BvvFunctions.show( view, "empty" ,Bvv.options().dCam(dCam).dClipNear(dClipNear).dClipFar(dClipFar));	
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
				// TODO Auto-generated method stub
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
	    	    	  render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	bShowOrigin=false;
	    	        	render_pl();
	    	        }
			}
	    	});
	    c.gridx=0;
	    c.gridy=0;
		panView.add(butOrigin,c);
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
	    	    	  render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	bShowWorldGrid=false;
	    	        	render_pl();
	    	        }
			}
	    	});
	    c.gridx=1;
	    c.gridy=0;
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
				render_pl();
			}
	    	
	    });
	    
	    
	    tabPane.setSize(350, 300);
	    tabPane.setSelectedIndex(1);

	    JProgressBar progressBar;
	    progressBar = new JProgressBar(0, 100);
	    progressBar.setValue(0);
	    //progressBar.setStringPainted(true);
		
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
	    roiManager.setParentFrame(frame);
		frame.add(finalPanel);
	    frame.setSize(400,500);

	    java.awt.Point bvv_p = bvv_frame.getLocationOnScreen();
	    java.awt.Dimension bvv_d = bvv_frame.getSize();
	
	    frame.setLocation(bvv_p.x+bvv_d.width, bvv_p.y);

	    
	    frame.setVisible( true );
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	
		
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
					
					if(findPointLocationFromClick(5,target))
					{
						roiManager.addPoint(target);
						render_pl();
					}
					//addPointToRoiManager(5);
					// listModel.addElement("test");
				},
				"add point",
				"Q" );
		
		//creates a line along the click taking into account 
		// frustum projection
		actions.runnableAction(
				() -> {

					addLineAlongTheClick();
				},
				"render click",
				"T" );
		//rotates a view along the axis of the click
		// in frustum projection
		actions.runnableAction(
				() -> {
					viewClickArea(10);
				},
				"rotate view click",
				"Y" );
		
		actions.runnableAction(
				() -> {
					
					roiManager.removePointFromLine();
					//if(traces.removeLastPointFromActive())
					//{
					render_pl();
					handl.showMessage("Point removed");
					//}
					
				},
				"remove point",
				"W" );
		actions.runnableAction(
				() -> {
						roiManager.unselect();
				},
				"new trace",
				"E" );
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

						RealPoint target = new RealPoint(3);
						double range = 60.0;
						double sigma=3.0;
						float stretch = (float)range;
						if(findPointLocationFromClick(5,target))
						{
							float [] pos = new float[3];
							target.localize(pos);
							long[][] rangeM = new long[3][3];
							int i;
							for(i=0;i<3;i++)
							{
								rangeM[0][i]=(long)(pos[i]-range );
								rangeM[1][i]=(long)(pos[i]+range);								
							}
							if(checkBoxInside(rangeM))
							{
								
								IntervalView<UnsignedByteType> input = Views.interval(img, rangeM[0], rangeM[1]);
								
								float[] vDir =testDeriv(input, sigma, target);
								//RealPoint origin = new RealPoint(3);
								RealPoint eV = new RealPoint(3);
								float[] evX= new float[3];
								
								for(i=-1;i<2;i+=1)
								{
																
									for(int j=0;j<3;j++)
									{
										evX[j]=pos[j]+stretch*vDir[j]*(i);
									}
									eV.setPosition(evX);
									roiManager.addPoint(eV);
									
								}
								roiManager.unselect();
								
								render_pl();
							}
						}
						
						
						
					},
					"reset tracings",
					"I" );
			actions.runnableAction(
					() -> {
						showHessianComponent(view2, 3.0);
					},
					"show hessian ",
					"F" );
		//actions.namedAction(action, defaultKeyStrokes);
		actions.install( bvv.getBvvHandle().getKeybindings(), "BigTrace actions" );

	}
	
	
	public float[] testDeriv(IntervalView<UnsignedByteType> input, double sigma, RealPoint target)
	//public void testDeriv(IntervalView<UnsignedByteType> input, double sigma, double range)
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
		IntervalView<FloatType> directionVectors =  Views.translate(dV, nShift);
		IntervalView<FloatType> salWeights =  Views.translate(sW, minV);
	    
		//direction vectors and saliency weights for each voxel
		 
		mEV.computeRAI( hessian,directionVectors,salWeights,nThreads,es);
		es.shutdown();
        
		float[] vDirv = new float[3];
		RandomAccess< FloatType > rA = directionVectors.randomAccess();		

		for(i=0;i<3;i++)
		{
			rA.setPosition(new long[] {(long)target.getFloatPosition(0), (long)target.getFloatPosition(1), (long)target.getFloatPosition(2) ,(long)i});
			vDirv[i]=rA.get().get();
		}
		
		System.out.println("done");
		showFloat(Views.interval(salWeights,salWeights.minAsLongArray(),salWeights.maxAsLongArray()));
		
		return vDirv;		
	}

	
	public void showFloat(IntervalView<FloatType> input)
	{
		float minVal = Float.MAX_VALUE;
		float maxVal = -Float.MAX_VALUE;
		for ( final FloatType h : input )
		{
			final float dd = h.get();
			minVal = Math.min( dd, minVal );
			maxVal = Math.max( dd, maxVal );
		}

		
		//final RealUnsignedByteConverter<FloatType> cvU = new RealUnsignedByteConverter<FloatType>(minVal,maxVal);
		final RealUnsignedByteConverter<FloatType> cvU = new RealUnsignedByteConverter<FloatType>(minVal, maxVal);
		final ConvertedRandomAccessibleInterval< FloatType, UnsignedByteType > inputScaled = new ConvertedRandomAccessibleInterval<>( input, ( s, t ) -> {
			cvU.convert(s,t);
		}, new UnsignedByteType() );	

		/*
		bvv2.removeFromBdv();
		view2=Views.interval( hsStretched, nDimCurr[0], nDimCurr[1] );
		view2=Views.interval(inputScaled,inputScaled.minAsLongArray(),inputScaled.maxAsLongArray());		
		bvv2 = BvvFunctions.show( view2, "weights", Bvv.options().addTo(bvv));
		*/
		bvv2 = BvvFunctions.show( Views.interval(inputScaled,inputScaled.minAsLongArray(),inputScaled.maxAsLongArray()), "weights", Bvv.options().addTo(bvv));
	}
	
	public void showHessianComponent(IntervalView<UnsignedByteType> input, double sigma)
	{
		double [][] kernels;
		Kernel1D[] derivKernel;
		final long[] dim = Intervals.dimensionsAsLongArray( input );
		//ArrayImg<UnsignedByteType, ByteArray> hss = ArrayImgs.unsignedBytes( dim[ 0 ], dim[ 1 ], dim[ 2 ] );
		ArrayImg<FloatType, FloatArray> hessian = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 6 );
			//ArrayImg<FloatType, FloatArray> gradient = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );
		int count = 0;
		int [] nDerivOrder; 
		for (int d1=0;d1<3;d1++)
		{
			for ( int d2 = d1; d2 < 3; d2++ )
			{
				final IntervalView< FloatType > hs2 = Views.hyperSlice( hessian, 3, count );
				nDerivOrder = new int [3];
				nDerivOrder[d1]++;
				nDerivOrder[d2]++;
				kernels = DerivConvolutionKernels.convolve_derive_kernel(sigma, nDerivOrder );
				derivKernel = Kernel1D.centralAsymmetric(kernels);
				SeparableKernelConvolution.convolution( derivKernel ).process( Views.extendBorder(input), hs2 );		
				//SeparableKernelConvolution.convolution( derivKernel ).process( input, hs2 );
				count++;
				System.out.println(count);
			}
		}

		final IntervalView< FloatType > hss = Views.hyperSlice( hessian, 3, 1 );
		float minVal = Float.MAX_VALUE;
			float maxVal = -Float.MAX_VALUE;
			for ( final FloatType h : hss )
			{
				final float dd = h.get();
				minVal = Math.min( dd, minVal );
				maxVal = Math.max( dd, maxVal );
			}

			
			//final RealUnsignedByteConverter<FloatType> cvU = new RealUnsignedByteConverter<FloatType>(minVal,maxVal);
			final RealUnsignedByteConverter<FloatType> cvU = new RealUnsignedByteConverter<FloatType>(minVal, maxVal);
			final ConvertedRandomAccessibleInterval< FloatType, UnsignedByteType > hsStretched = new ConvertedRandomAccessibleInterval<>( hss, ( s, t ) -> {
				cvU.convert(s,t);
			}, new UnsignedByteType() );	
			
		bvv2.removeFromBdv();
		System.gc();
		//view2=Views.interval( hsStretched, nDimCurr[0], nDimCurr[1] );
		view2=Views.interval(hsStretched,nDimIni[0],nDimIni[1]);
		bvv2 = BvvFunctions.show( view2, "hessian", Bvv.options().addTo(bvv));
		

		
	}

	public void render_pl()
	{
		
		handl.setRenderScene( ( gl, data ) -> {
			
			/* DEBUG traces helper
			 * 
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
			*/
			roiManager.draw(gl, new Matrix4f( data.getPv() ), new double [] {data.getScreenWidth(), data.getScreenHeight()}, data.getDClipNear(), data.getDClipFar());
			
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
		
		handl=bvv.getBvvHandle().getViewerPanel();
		handl.state().setViewerTransform(t);
		handl.requestRepaint();
		if(!firstCall)
			bvv2.removeFromBdv();
		
		view2=Views.interval( img, new long[] { 0, 0, 0 }, new long[]{ nW-1, nH-1, nD-1 } );				
		bvv2 = BvvFunctions.show( view2, "cropresetYZ", Bvv.options().addTo(bvv));
		
		
	}
	
	public boolean findPointLocationFromClick(final int nHalfWindowSize, final RealPoint target)
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
			RealPoint target_found = new RealPoint( 3 );
			//RealPoint locationMax = new RealPoint( 3 );
			
			if(computeMaxLocationCuboid(intRay,target_found,clickVolume))
			{
				//traces.addPointToActive(target);
				handl.showMessage("point found");
				target.setPosition(target_found);
				return true;
				//roiManager.addPoint(target);
				//roiManager.addPointToLine(target);
			}
			else
			{
				handl.showMessage("not found :(");
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
		
		BigTrace testI=new BigTrace(); 
		
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
	public boolean checkBoxInside(final long [][] newMinMax)
	{ 
		for (int j=0;j<3;j++)
		{
				newMinMax[0][j]=Math.max(nDimCurr[0][j],newMinMax[0][j]);
				newMinMax[1][j]=Math.min(nDimCurr[1][j],newMinMax[1][j]);
				if(newMinMax[1][j]<newMinMax[0][j])
					return false;
		}
		return true;
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

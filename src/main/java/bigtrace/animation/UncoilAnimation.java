package bigtrace.animation;


import io.scif.codec.CompressionType;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.StopWatch;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.geometry.Plane3D;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import bigtrace.volume.StraightenCurve;
import bigtrace.volume.VolumeMisc;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

public class UncoilAnimation < T extends RealType< T > & NativeType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	
	private String progressState;
	
	public AbstractCurve3D inputROI;
	
	/** total number of intermediate frames **/
	public int nFrames = 100;
	
	public int nUnCoilTask = 0;
	
	/** interval containing all ROIs **/
	FinalInterval allInt;
	
	/** plugin instance **/
	BigTrace<T> bt;
	
	/** frames of unbending ROIs **/
	ArrayList <double [][][]> alFrames;
	/** frames of unbending ROIs **/
	ArrayList <ArrayList<RealPoint>> alSegments;
	
	public boolean bUseTemplate = false;

	IntervalView <T> template = null;
	
	boolean bAddROIs = false;
	
	ArrayList <AbstractCurve3D> alRois;
	
	double [] finalOrientation = null;
	
	public String sSaveFolderPath;

	//private RandomAccess<T> ra_template;
	
	public boolean bUseCompression = true;
	
	public UncoilAnimation(final BigTrace<T> bt_)
	{
		bt = bt_;	
	}
	@Override
	public String getProgressState() 
	{		
		return progressState;
	}
	
	@Override
	public void setProgressState(String state_) 
	{		
		progressState = state_;		
	}
	
	@Override
	protected Void doInBackground() throws Exception 
	{
        bt.bInputLock = true;
        bt.setLockMode(true);
        

        try 
        {
        	Thread.sleep(1);
        } 
        catch (InterruptedException ignore) {}
        setProgress(1);
        setProgressState("Creating uncoil animation...");

		//generate only rois for show
		if(nUnCoilTask==0)
		{
			bAddROIs = true;
			generateROIs(inputROI);
			
		}
		//generate ROIs and volumes
		if(nUnCoilTask == 1)
		{
			bAddROIs = true;
			//need to edit ROIs to fit the new volumes
			if(!generateROIs(inputROI))
				return null;
			generateAllVolumes();
		}
		//generate only volumes
		if(nUnCoilTask==2)
		{
			bAddROIs = false;
			if(!generateROIs(inputROI))
				return null;
			generateAllVolumes();
		}
		return null;
		
	}
	
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() 
    {
    	//see if we have some errors
    	try {
    		get();
    	} 
    	catch (ExecutionException e) {
    		e.getCause().printStackTrace();
    		String msg = String.format("Unexpected problem during straightening animation generation: %s", 
    				e.getCause().toString());
    		System.out.println(msg);
    	} catch (InterruptedException e) {
    		// Process e here
    	}
		//unlock user interaction
    	bt.bInputLock = false;
        bt.setLockMode(false);

    }
    
	/** Given input ROI (of AbstractCurve type), 
	generates output ROIs that gradually are straightened.
	They are added to ROI manager at subsequent time frames **/
	public boolean generateROIs(final AbstractCurve3D firstLine)
	{
		
        setProgress(0);
        setProgressState("Generating ROIs...");
		alRois = new ArrayList<>();
		alRois.add( firstLine );
		
		ArrayList<double []> tangents = firstLine.getJointSegmentTangentsResampled();
		
		//get all the points along the line with pixel sampling in scaled coords
		ArrayList<RealPoint> segment = firstLine.getJointSegmentResampled();
		alSegments = new ArrayList<>();
		alSegments.add( segment );
		
		//get a frame around line in SCALED
		double [][][] rsVect =  Pipe3D.rotationMinimizingFrame(segment, tangents);

		alFrames = new ArrayList<>();
		alFrames.add( rsVect );
		
		//get all the points along the line with pixel sampling in UNSCALED coords
		segment = Roi3D.scaleGlobInv(segment,BigTraceData.globCal);
		
		int nTotPoints = segment.size();
		
		//if using template, check its dimensions.
		if(bUseTemplate)
		{
			long [] dimsTemplate = template.dimensionsAsLongArray();
			boolean bTemplateFits = true;
			int dimYZ =(int)( Math.round(0.5*inputROI.getLineThickness()))*2+1;

			if(dimsTemplate[0] != segment.size())
				bTemplateFits  = false;
			if(dimsTemplate[1]!=dimYZ || dimsTemplate[2]!=dimYZ)
				bTemplateFits  = false;

			
			if(!bTemplateFits)
			{
				String dTempDims = Long.toString( dimsTemplate[0] );
				for(int i=1;i<template.numDimensions();i++)
				{
					dTempDims = dTempDims +"x"+ Long.toString( dimsTemplate[i] );
				}
				String sExpectedDims = Integer.toString( segment.size() );
				sExpectedDims = sExpectedDims +"x" +Integer.toString( dimYZ );
				sExpectedDims = sExpectedDims +"x" +Integer.toString( dimYZ );
				sExpectedDims = sExpectedDims +"x" +Integer.toString( bt.btData.nTotalChannels );
				IJ.log( "Provided template size for the straighten/uncoil animation is wrong! Aborting." );
				IJ.log( "Provided template dimensions (XYZC):" + dTempDims);
				IJ.log( "Expected dimensions (XYZC):" + sExpectedDims);
				IJ.log( "Make sure it is the same ROI and that the template straightening ");
				IJ.log( "happened with the same ROI Shape Interpolation/Smoothing window settings.");
				return false;
			}
		}
		
		allInt = Intervals.union( firstLine.getBoundingBox(), firstLine.getBoundingBox() );
		
		//specify the final starting end orientation.
		// it is a vector added in the beginning,
		// which specifies, where the final straight line ROI is gonna point.
		double [] iniOrientP = segment.get( 0 ).positionAsDoubleArray();
		if(finalOrientation == null)
		{
			finalOrientation = new double[3];
			double [] nextP = segment.get( 1 ).positionAsDoubleArray();
			LinAlgHelpers.subtract( nextP,iniOrientP, finalOrientation);
			LinAlgHelpers.normalize( finalOrientation );
		}
		else
		{
			//just in case
			LinAlgHelpers.normalize( finalOrientation );			
		}		
		
		LinAlgHelpers.subtract( iniOrientP, finalOrientation, iniOrientP );		
		
		RealPoint iniOrientRP = new RealPoint(iniOrientP);	
		
		
		//angles at each triplets of points
		final double [] lineAngles = new double[nTotPoints-1];
		//planes containing each triplets of points
		final Plane3D [] planes = new Plane3D[nTotPoints-1];
		
		
		lineAngles[0] = calculateAngle(iniOrientRP,segment.get(0),segment.get(1));
		planes[0] = new Plane3D(iniOrientRP,segment.get(0),segment.get(1));
		
		for(int i = 0; i<nTotPoints-2;i++)
		{
			lineAngles[i+1] = calculateAngle(segment.get( i ),segment.get(i+1),segment.get(i+2));
			planes[i+1] = new Plane3D(segment.get(i),segment.get(i+1),segment.get(i+2));
		}	
		
		final double [] q = new double[4];
		final double [] joint = new double [3];
		double [] notrotated = new double [3];
		double [] moved = new double [3];
		double [] rotated = new double [3];
		
		//for (int i=1; i<20; i++)
		for (int i=1; i<nFrames ; i++)
		{
			setProgress(100*i/(nFrames-1));
			setProgressState("generating ROI ("+Integer.toString(i+1)+"/"+Integer.toString(nFrames)+")");
			ArrayList<RealPoint> segment_new = new ArrayList<>();
			for (int nPoint = 0; nPoint<nTotPoints; nPoint++)
			{
				segment_new.add( segment.get( nPoint ) );
			}
			//new frame around ROI
			double [][][] newFrame = new double [2][nTotPoints][3];
			for(int ii=0;ii<2;ii++)
				for(int jj=0;jj<nTotPoints;jj++)
					for(int kk=0;kk<3;kk++)
						newFrame[ii][jj][kk]=rsVect[ii][jj][kk];
			
			for(int nTrio = nTotPoints-2; nTrio>=0; nTrio--)
			{
				LinAlgHelpers.quaternionFromAngleAxis(planes[nTrio].n, lineAngles[nTrio]*i/(nFrames-1), q);
				//LinAlgHelpers.quaternionFromAngleAxis(planes[nTrio].n, lineAngles[nTrio], q);
				segment_new.get(nTrio).localize( joint );
				for(int nPoint = nTrio+1; nPoint<nTotPoints; nPoint++)
				{
					segment_new.get( nPoint ).localize( notrotated );
					LinAlgHelpers.subtract( notrotated, joint, moved );
					LinAlgHelpers.quaternionApply( q, moved, rotated );
					LinAlgHelpers.add( rotated, joint, rotated );
					segment_new.set(nPoint, new RealPoint (rotated) );
					//taking care of the frame
					for(int nFr = 0; nFr<2;nFr++)
					{
						//in SPACED UNITS, add POINT
						LinAlgHelpers.add(newFrame[nFr][nPoint],Roi3D.scaleGlob(notrotated,BigTraceData.globCal),newFrame[nFr][nPoint]);

						//subtract rotation center
						LinAlgHelpers.subtract( newFrame[nFr][nPoint],Roi3D.scaleGlob( joint,BigTraceData.globCal), newFrame[nFr][nPoint] );
						
						//go to UNSCALED
						newFrame[nFr][nPoint] =  Roi3D.scaleGlobInv(newFrame[nFr][nPoint],BigTraceData.globCal);
						//rotate
						LinAlgHelpers.quaternionApply( q, newFrame[nFr][nPoint], newFrame[nFr][nPoint] );
						
						//move back to 
						//move back rotation center
						LinAlgHelpers.add( newFrame[nFr][nPoint], joint, newFrame[nFr][nPoint] );
						//subtract new position of the point
						LinAlgHelpers.subtract(newFrame[nFr][nPoint],rotated,newFrame[nFr][nPoint]);
						//move back to scaled
						newFrame[nFr][nPoint] =  Roi3D.scaleGlob(newFrame[nFr][nPoint],BigTraceData.globCal);
					}
				}
			}
			alFrames.add( newFrame );
			alSegments.add(  Roi3D.scaleGlob(segment_new,BigTraceData.globCal) );
			//LineTrace3D newROI = addROIsegment(bt, segment_new, i, ( int ) firstLine.getLineThickness());
			LineTrace3D newROI = addROIsegment(bt, segment_new,firstLine.getTimePoint(), ( int ) firstLine.getLineThickness(), bAddROIs);
			alRois.add( newROI );
			allInt = Intervals.union( allInt, newROI.getBoundingBox() );
		}
		//ouput final size
		String sTotDims = "Final volume dimensions, voxels: " + Long.toString( allInt.dimension( 0 ) );
		for (int d=1;d<3; d++)
		{
			sTotDims = sTotDims + "x"+ Long.toString( allInt.dimension( d ) );
		}
		System.out.println( sTotDims  );
		
		//add channel dimension to the final interval
		allInt = Intervals.addDimension( allInt, 0, bt.btData.nTotalChannels-1 );
		return true;
	}
	
	public void testRMFrames()
	{
		for (int nFrame = 0; nFrame<alFrames.size();nFrame++)
		{
			ArrayList<RealPoint> frameX1 = new ArrayList<>();
			ArrayList<RealPoint> frameX2 = new ArrayList<>();
			double [] pos = new double[3];
			double [] comp =  new double[3];
			double [] addition = new double[3];
			for(int nPoint = 0;nPoint<alSegments.get( nFrame ).size();nPoint++)
			{
				alSegments.get( nFrame ).get( nPoint ).localize( pos );
				LinAlgHelpers.scale( alFrames.get( nFrame )[0][nPoint], 20*BigTraceData.dMinVoxelSize, addition );
				LinAlgHelpers.add( addition, pos, comp );
				frameX1.add( new RealPoint(Roi3D.scaleGlobInv(comp,BigTraceData.globCal)));
				LinAlgHelpers.scale( alFrames.get( nFrame )[1][nPoint], 20*BigTraceData.dMinVoxelSize, addition );
				LinAlgHelpers.add( addition, pos, comp );
				frameX2.add( new RealPoint(Roi3D.scaleGlobInv(comp,BigTraceData.globCal)));

				
//				frameX1.add( new RealPoint(Roi3D.scaleGlobInv(pos,BigTraceData.globCal)));
//				frameX2.add( new RealPoint(Roi3D.scaleGlobInv(pos,BigTraceData.globCal)));

			}
			addROIsegment(bt, frameX1, 0, 2, true);
			addROIsegment(bt, frameX2, 0, 2, true);
		}
		
	}
	
	@SuppressWarnings( "null" )
	public IntervalView<T> generateSingleVolume(int nInd)
	{
		
		FinalInterval roiIntervBox = Intervals.expand( alRois.get(nInd).getBoundingBox(),2);
		roiIntervBox = Intervals.addDimension( roiIntervBox, 0, bt.btData.nTotalChannels-1 );
		//FinalInterval test = Intervals.expand( allInt,2);
	
		RandomAccessibleInterval<T> all_RAI =  bt.btData.getAllDataRAI();
		
		Img<T> outImg = Util.getSuitableImgFactory(roiIntervBox, Util.getTypeFromInterval(all_RAI) ).create(roiIntervBox);
		IntervalView<T> outInterval = Views.translate( outImg, roiIntervBox.minAsLongArray() );
		
		// get only timePoint of the ROI
		IntervalView< T > data_read = Views.hyperSlice( all_RAI, 3, inputROI.getTimePoint() );
		RealRandomAccessible<T> interpolate = Views.interpolate( Views.extendZero(data_read), bt.btData.nInterpolatorFactory);
		RealRandomAccess<T> ra = interpolate.realRandomAccess();
		RandomAccess<T> ra_out = outInterval.randomAccess();
		RandomAccess<T> ra_template = null;
		if(bUseTemplate)
		{
			ra_template = template.randomAccess();
		}	
		int nRadius = (int) Math.round(0.5*alRois.get(0).getLineThickness());
		ArrayList< ValuePair< int[],RealPoint>  > planeNormZero;
		ArrayList< ValuePair< int[],RealPoint>  > planeNormNew;
		double [] current_point_zero = new double [3];
		double [] current_point_new = new double [3];

		double[] curr_XY_zero = new double [3];
		double [] currXY_zero_mCh = new double[data_read.numDimensions()];
		double[] curr_XY_new = new double [3];
		long [] posInt = new long[data_read.numDimensions()];
		double newVal;
		double oldVal;
		for (int nPoint = 0;nPoint<alSegments.get( 0 ).size();nPoint++)
		{
			alSegments.get( 0 ).get(nPoint).localize(current_point_zero); 
			planeNormZero = StraightenCurve.getNormCircleGridXYPairs(nRadius, BigTraceData.dMinVoxelSize,alFrames.get( 0 )[0][nPoint],alFrames.get( 0 )[1][nPoint], current_point_zero);
			
			alSegments.get( nInd ).get(nPoint).localize(current_point_new); 
			planeNormNew = StraightenCurve.getNormCircleGridXYPairs(nRadius, BigTraceData.dMinVoxelSize,alFrames.get( nInd )[0][nPoint],alFrames.get( nInd )[1][nPoint], current_point_new);
			//double teee1 = LinAlgHelpers.dot( alFrames.get( 0)[0][nPoint],alFrames.get( 0)[1][nPoint]);

			//double teee = LinAlgHelpers.dot( alFrames.get( nInd )[0][nPoint],alFrames.get( nInd )[1][nPoint]);
			for(int i=0;i<planeNormZero.size();i++)
			{
				planeNormZero.get(i).getB().localize( curr_XY_zero );
				curr_XY_zero = Roi3D.scaleGlobInv(curr_XY_zero, BigTraceData.globCal);
				
				planeNormNew.get(i).getB().localize( curr_XY_new );
				curr_XY_new = Roi3D.scaleGlobInv(curr_XY_new, BigTraceData.globCal);

				for(int d=0;d<3;d++)
				{
					currXY_zero_mCh[d] = curr_XY_zero[d];
					posInt[d] = Math.round( curr_XY_new[d]);
				}
				for(int nCh=0; nCh<bt.btData.nTotalChannels; nCh++)
				{
					posInt[3] = nCh;
					currXY_zero_mCh[3] = nCh;
					ra_out.setPosition( posInt );
					T posFill = ra_out.get();
					if(bUseTemplate)
					{
						ra_template.setPosition( new int [] {nPoint,planeNormNew.get(i).getA()[1],planeNormNew.get(i).getA()[0], nCh} );
						newVal = ra_template.get().getRealDouble();
					}
					else
					{
						ra.setPosition(currXY_zero_mCh);
						newVal = ra.get().getRealDouble();
					}
					oldVal = posFill.getRealDouble();
					if(newVal>oldVal)
					{
						posFill.setReal(newVal);
					}
				}
				
			}
		}
		
		return outInterval;

	}
	
	public void generateAllVolumes()
	{
		
		int i;
        setProgress(0);
        setProgressState("Generating volumes...");

		for(i=0;i<nFrames;i++)
		{

			setProgress(100*i/(nFrames-1));
			setProgressState("generating/saving frame ("+Integer.toString(i+1)+"/"+Integer.toString(nFrames)+")");

			final IntervalView< T > outInt = generateSingleVolume(i);	
			if(bUseCompression)
			{
				saveCompressedTIFF(outInt,i);
			}
			else
			{
				saveUncompressedTIFF(outInt,i);
			}
		}

	}
	
	public boolean loadTemplate(String sTemplateTIF)
	{
		final ImagePlus imp = IJ.openImage( sTemplateTIF );
		
		int nBitD = imp.getBitDepth();
		T ff = Util.getTypeFromInterval( bt.btData.getAllDataRAI() );
		
		if(ff instanceof UnsignedByteType)
		{
			if(nBitD!=8)
			{
				IJ.log("Provided template bit depth ("+Integer.toString( nBitD )+") does"
						+ " not match dataset bit depth (8)");
				return false;
			}
		}
		if(ff instanceof UnsignedShortType)
		{
			if(nBitD!=16)
			{
				IJ.log("Provided template bit depth ("+Integer.toString( nBitD )+") does"
						+ " not match dataset bit depth (16)");
				return false;
			}
		}

		
		Img<T> templateImg = ImageJFunctions.wrap( imp );
		boolean bTemplateFine = true;
		
		
		
		//do some basic checks on dimensions
		// sizes will be additionally checked later during ROI generation
		if(templateImg.numDimensions()<3 || templateImg.numDimensions()>4)
		{
			bTemplateFine = false;
		}
		if(templateImg.numDimensions()==3 && bt.btData.nTotalChannels>1)
		{
			bTemplateFine = false;
		}
		else
		{
			template = Views.addDimension( templateImg,0 ,0 );
		}
		if(templateImg.numDimensions()==4)
		{
			if(templateImg.dimension( 2 ) != bt.btData.nTotalChannels)
			{
				bTemplateFine = false;
			}
			else
			{
				template = Views.permute( templateImg, 2, 3 );
			}
		}
		if(!bTemplateFine)
		{
			IJ.log( "Provided template dimensions do not fit dataset dimensions"
					+ " for the straighten/uncoil animation! Aborting." );
		}
		return bTemplateFine;
	}
	public boolean saveUncompressedTIFF(final IntervalView<T> outInterval, final int nInd)
	{
		final Calibration cal = new Calibration();
		cal.setUnit(bt.btData.sVoxelUnit);
		cal.setTimeUnit(bt.btData.sTimeUnit);
		cal.pixelWidth = BigTraceData.globCal[0];
		cal.pixelHeight = BigTraceData.globCal[1];
		cal.pixelDepth = BigTraceData.globCal[2];
		
		final IntervalView< T > imgOut = Views.zeroMin(  Views.interval( Views.extendZero( outInterval ),allInt ));
		final ImagePlus ip = VolumeMisc.wrapImgImagePlusCal(imgOut, inputROI.getName() + "_vol_T"+ String.format("%0"+String.valueOf(nFrames).length()+"d", nInd),cal);
		IJ.saveAsTiff(ip, sSaveFolderPath+ip.getTitle());

		return true;
	}
	
	public boolean saveCompressedTIFF(final IntervalView<T> outInterval, final int nInd)
	{
//		final Calibration cal = new Calibration();
//		cal.setUnit(bt.btData.sVoxelUnit);
//		cal.setTimeUnit(bt.btData.sTimeUnit);
//		cal.pixelWidth= BigTraceData.dMinVoxelSize;
//		cal.pixelHeight= BigTraceData.dMinVoxelSize;
//		cal.pixelDepth= BigTraceData.dMinVoxelSize;
		final SCIFIOConfig config = new SCIFIOConfig();
		config.imgSaverSetWriteRGB(false);
		if(bUseCompression)
		{
			//config.writerSetCompression(CompressionType.LZW.getCompression());
			config.writerSetCompression(CompressionType.ZLIB.getCompression());
		}
		ImgSaver saver = new ImgSaver();
		String sPathOutTif = sSaveFolderPath+"/"+inputROI.getName()+"_vol_T"+ String.format("%0"+String.valueOf(nFrames).length()+"d", nInd) +".tif";
		File outTif = new File(sPathOutTif);

		try
		{
			boolean result = Files.deleteIfExists(outTif.toPath());
			if(result)
			{
				IJ.log( "Overwriting "+ sPathOutTif +".");
			}
		}
		catch ( IOException exc )
		{
			IJ.log(exc.getMessage());
			exc.printStackTrace();
			return false;
		}
		
		final AxisType[] axisTypes = new AxisType[]{ Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL};

		final IntervalView< T > imgOut = Views.zeroMin(  Views.interval( Views.extendZero( outInterval ),allInt ));
		
		final ImgPlus<?> saveImg = new ImgPlus<>(wrapIntervalForSCIFIO(imgOut),"test",axisTypes);
		
		saveImg.setCompositeChannelCount((int)imgOut.dimension(3));
		StopWatch stopwatch = StopWatch.createAndStart();
		saver.saveImg( sPathOutTif, saveImg, config );
		System.out.println( "saving time: " + stopwatch );

		return true;
	}
	
	public Img< T > wrapIntervalForSCIFIO(IntervalView<T> intView)
	{
		ArrayList< RandomAccessibleInterval< T > > allZC = new ArrayList<>();
		
		long [] dims = intView.dimensionsAsLongArray();
		if(dims.length!=4)
		{
			System.err.println("Only XYZC Imgs are supported for now");
			return null;
		}
		
		if(dims[3]==0)
		{
			return ImgView.wrap(intView);
		}

		for(int z=0;z<intView.dimension( 2 );z++)
		{		
			for(int c=0;c<intView.dimension( 3 );c++)
			{
				allZC.add( Views.hyperSlice(Views.hyperSlice( intView, 3, c ),2,z) );
			}
		}
		
		//make a version using Views.concatenate( ) ???
		return ImgView.wrap( Views.stack( allZC ));
	}
	
	public void generateSpiralROI(double R, double dz, double dAngleStep)
	{		
		//double dz = 2;
		//double R = 10;
		//double dAngleStep = 0.2;
		
		// add a ROI to ROI manager
		LineTrace3D firstLine = (LineTrace3D) bt.roiManager.makeRoi(Roi3D.LINE_TRACE, 0);
		//LineTrace3D firstLine = new LineTrace3D((Roi3DGroup)bt.roiManager.groups.get( 0 ), 0);
		
		ArrayList<RealPoint> segment = new ArrayList<>();
		double [] point = new double [3];
		
		double dAngleCurr = 0;

		firstLine.addFirstPoint( new RealPoint(point) );
		segment.add( new RealPoint(point) );
		int nTotPoints = 100;
		for (int nPoint = 1; nPoint<nTotPoints; nPoint++)
		{
			dAngleCurr += dAngleStep;
			point[0] += R*Math.cos( dAngleCurr );
			point[1] += R*Math.sin( dAngleCurr );
			point[2] += dz;
			segment.add( new RealPoint(point) );
		}
		firstLine.addPointAndSegment( segment.get( nTotPoints-1 ), segment );
		bt.roiManager.addRoi( firstLine );
	}
	
	/** calculates a PI minus angle between vectors p1-p2 and p3-p1 **/
	public static double calculateAngle(RealPoint p1, RealPoint p2, RealPoint p3)
	{
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		LinAlgHelpers.subtract( p1.positionAsDoubleArray(), p2.positionAsDoubleArray(), vect1 );
		LinAlgHelpers.subtract( p3.positionAsDoubleArray(), p2.positionAsDoubleArray(), vect2 );
		LinAlgHelpers.normalize( vect1 );
		LinAlgHelpers.normalize( vect2 );
		double dDot = Math.min( Math.max( LinAlgHelpers.dot( vect1, vect2 ),-1.0),1.0);
		return Math.PI-Math.acos(dDot);
		
	}
	
	/** adds a new LineTrace ROI from ArrayList of RealPoints to the ROI manager **/
	public static LineTrace3D addROIsegment(final BigTrace< ? > bt, ArrayList<RealPoint> segment, int nTimeFrame, int nThickness, boolean bAddROIs)
	{
		LineTrace3D aLine = (LineTrace3D) bt.roiManager.makeRoi(Roi3D.LINE_TRACE, nTimeFrame);
		aLine.addFirstPoint( segment.get( 0 ));
		aLine.addPointAndSegment( segment.get( segment.size()-1 ), segment );
		aLine.setLineThickness( nThickness );
		if(bAddROIs)
		{
			bt.roiManager.addRoi( aLine );	
		}
		return aLine;
	}
	

	
//	public static void main( final String[] args )
//	{
//		new ImageJ();
//		BigTrace<UnsignedShortType> bt = new BigTrace<>(); 
//		
//		//bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/HyperStack.tif");
//		bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/Chlorophis_irregularis-XYZ_divby8.tif");
//		//bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/Chlorophis_irregularis-XYZ_divby8_8bit_3fr.tif");
//
//		try
//		{
//			bt.btMacro.macroLoadROIs( "/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/Chlorophis_irregularis-XYZ_divby8.tif_btrois.csv", "Clean" );
//			bt.btMacro.macroShapeInterpolation( "Spline", 20 );
//		}
//		catch ( InterruptedException exc )
//		{
//			// TODO Auto-generated catch block
//			exc.printStackTrace();
//		}
//		//wait to load things
//		while(bt.bInputLock)
//		{
//			try
//			{
//				Thread.sleep(100);
//			}
//			catch ( InterruptedException exc )
//			{
//				// TODO Auto-generated catch block
//				exc.printStackTrace();
//			}
//		}
//
//		LineTrace3D firstLine = (LineTrace3D) bt.roiManager.rois.get( 0 );
//		
//		UnCurveAnimation<UnsignedShortType> unBend = new UnCurveAnimation<>(bt);
//		
//		unBend.generateROIs( firstLine );	
//		//unBend.testFrames();
//		unBend.loadTemplate( "/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/clean_X_trace172232825_straight.tif" );
//		
//		unBend.generateAllVolumes("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/out/");
//		//unBend.generateSingleVolume(1);
//		
//	}
	

}

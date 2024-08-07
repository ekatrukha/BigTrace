package bigtrace.animation;

import io.scif.codec.CompressionType;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

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
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.geometry.Pipe3D;
import bigtrace.geometry.Plane3D;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import bigtrace.volume.StraightenCurve;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.Calibration;

public class UnCurveAnimation < T extends RealType< T > & NativeType< T > > 
{
	/** total number of intermediate frames **/
	int nFrames = 100;
	
	/** interval containing all ROIs **/
	FinalInterval allInt;
	
	/** plugin instance **/
	BigTrace<T> bt;
	
	/** frames of unbending ROIs **/
	ArrayList <double [][][]> alFrames;
	/** frames of unbending ROIs **/
	ArrayList <ArrayList<RealPoint>> alSegments;

	Img<T> template;
	
	boolean bAddROIs = false;
	
	ArrayList <AbstractCurve3D> alRois;
	
	public UnCurveAnimation(final BigTrace<T> bt_)
	{
		bt = bt_;	
	}
	
	/** Given input ROI (of AbstractCurve type), 
	generates output ROIs that gradually are straightened.
	They are added to ROI manager at subsequent time frames **/
	public void generateROIs(final AbstractCurve3D firstLine)
	{
		
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
		
		allInt = Intervals.union( firstLine.getBoundingBox(), firstLine.getBoundingBox() );
		
		//specify the final starting end orientation.
		// it is a vector added in the beginning,
		// which specifies, where the final straight line ROI is gonna point.
		double [] finalOrientation = new double[] {-1.0,0.,0.0};
		double [] iniOrientP = segment.get( 0 ).positionAsDoubleArray();
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
		
		for (int i=1; i<20; i++)
		//for (int i=1; i<nFrames ; i++)
		{
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
			LineTrace3D newROI = addROIsegment(bt, segment_new,0, ( int ) firstLine.getLineThickness(), bAddROIs);
			alRois.add( newROI );
			allInt = Intervals.union( allInt, newROI.getBoundingBox() );
		}
		for (int d=0;d<3; d++)
		{
			System.out.println( allInt.dimension( d ) );
		}
		
		
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
	
	public boolean generateSingleVolume(int nInd, String sOutputPath)
	{
		
		FinalInterval test = Intervals.expand( alRois.get(nInd).getBoundingBox(),2);
		//FinalInterval test = Intervals.expand( allInt,2);
	
		RandomAccessibleInterval<T> all_RAI =  bt.btData.getAllDataRAI();
		
		Img<T> out1 = Util.getSuitableImgFactory(test, Util.getTypeFromInterval(all_RAI) ).create(test);
		IntervalView<T> outS = Views.translate( out1, test.minAsLongArray() );
		
		RealRandomAccessible<T> interpolate = Views.interpolate( Views.extendZero(all_RAI), bt.btData.nInterpolatorFactory);
		RealRandomAccess<T> ra = interpolate.realRandomAccess();
		RandomAccess<T> ra_out = outS.randomAccess();

		
		int nRadius = (int) Math.round(0.5*alRois.get(0).getLineThickness());
		ArrayList< ValuePair< int[],RealPoint>  > planeNormZero;
		ArrayList< ValuePair< int[],RealPoint>  > planeNormNew;
		double [] current_point_zero = new double [3];
		double [] current_point_new = new double [3];

		double[] curr_XY_zero = new double [3];
		double [] currXY_zero_mCh = new double[all_RAI.numDimensions()];
		double[] curr_XY_new = new double [3];
		long [] posInt = new long[3];
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
					currXY_zero_mCh[d]=curr_XY_zero[d];
					posInt[d] = Math.round( curr_XY_new[d]);
				}
				ra.setPosition(currXY_zero_mCh);
				ra_out.setPosition( posInt );
				T posFill = ra_out.get(); 
				newVal = ra.get().getRealDouble();
				oldVal = posFill.getRealDouble();
				if(newVal>oldVal)
				{
					posFill.setReal(newVal);
				}
				
			}
		}
		final Calibration cal = new Calibration();
		cal.setUnit(bt.btData.sVoxelUnit);
		cal.setTimeUnit(bt.btData.sTimeUnit);
		cal.pixelWidth= BigTraceData.dMinVoxelSize;
		cal.pixelHeight= BigTraceData.dMinVoxelSize;
		cal.pixelDepth= BigTraceData.dMinVoxelSize;
		final SCIFIOConfig config = new SCIFIOConfig();
		config.writerSetCompression(CompressionType.LZW.getCompression());
		
		ImgSaver saver = new ImgSaver();
		String sPathOutTif = sOutputPath+"/vol_T"+ String.format("%0"+String.valueOf(nFrames).length()+"d", nInd) +".tif";
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

		AxisType[] axisTypes = new AxisType[]{ Axes.X, Axes.Y, Axes.Z };
		ImgPlus<?> sourceImg = new ImgPlus<>(ImgView.wrap(Views.zeroMin(  Views.interval( Views.extendZero( outS),allInt ))),"test",axisTypes);
		//saver.saveImg( sPathOutTif, ImageJFunctions.wrap( VolumeMisc.wrapImgImagePlusCal(Views.interval( Views.extendZero( outS),allInt ),"test",cal)), config );
		saver.saveImg( sPathOutTif,sourceImg, config );

		//IJ.saveAsTiff(VolumeMisc.wrapImgImagePlusCal(Views.interval( Views.extendZero( outS),allInt ),"test",cal),sPathOutTif);
		//ImageJFunctions.show( outS );
		return true;
	}

	public boolean generateSingleVolumeTemplate(int nInd, String sOutputPath)
	{
		
		FinalInterval test = Intervals.expand( alRois.get(nInd).getBoundingBox(),2);
		//FinalInterval test = Intervals.expand( allInt,2);
	
		RandomAccessibleInterval<T> all_RAI =  bt.btData.getAllDataRAI();
		
		Img<T> out1 = Util.getSuitableImgFactory(test, Util.getTypeFromInterval(all_RAI) ).create(test);
		IntervalView<T> outS = Views.translate( out1, test.minAsLongArray() );
		
		RealRandomAccessible<T> interpolate = Views.interpolate( Views.extendZero(all_RAI), bt.btData.nInterpolatorFactory);
		RealRandomAccess<T> ra = interpolate.realRandomAccess();
		RandomAccess<T> ra_out = outS.randomAccess();
		RandomAccess<T> ra_template = template.randomAccess();
		
		int nRadius = (int) Math.round(0.5*alRois.get(0).getLineThickness());
		ArrayList< ValuePair< int[],RealPoint>  > planeNormZero;
		ArrayList< ValuePair< int[],RealPoint>  > planeNormNew;
		double [] current_point_zero = new double [3];
		double [] current_point_new = new double [3];

		double[] curr_XY_zero = new double [3];
		double [] currXY_zero_mCh = new double[all_RAI.numDimensions()];
		double[] curr_XY_new = new double [3];
		long [] posInt = new long[3];
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
					currXY_zero_mCh[d]=curr_XY_zero[d];
					posInt[d] = Math.round( curr_XY_new[d]);
				}
				ra.setPosition(currXY_zero_mCh);
				ra_out.setPosition( posInt );
				T posFill = ra_out.get(); 
				ra_template.setPosition( new int [] {nPoint,planeNormNew.get(i).getA()[1],planeNormNew.get(i).getA()[0]} );
				//newVal = ra.get().getRealDouble();
				newVal = ra_template.get().getRealDouble();
				oldVal = posFill.getRealDouble();
				if(newVal>oldVal)
				{
					posFill.setReal(newVal);
				}
				
			}
		}
		final Calibration cal = new Calibration();
		cal.setUnit(bt.btData.sVoxelUnit);
		cal.setTimeUnit(bt.btData.sTimeUnit);
		cal.pixelWidth= BigTraceData.dMinVoxelSize;
		cal.pixelHeight= BigTraceData.dMinVoxelSize;
		cal.pixelDepth= BigTraceData.dMinVoxelSize;
		final SCIFIOConfig config = new SCIFIOConfig();
		
		config.writerSetCompression(CompressionType.LZW.getCompression());
	
		ImgSaver saver = new ImgSaver();
		String sPathOutTif = sOutputPath+"/vol_T"+ String.format("%0"+String.valueOf(nFrames).length()+"d", nInd) +".tif";
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

		AxisType[] axisTypes = new AxisType[]{ Axes.X, Axes.Y, Axes.Z };
		ImgPlus<?> sourceImg = new ImgPlus<>(ImgView.wrap(Views.zeroMin(  Views.interval( Views.extendZero( outS),allInt ))),"test",axisTypes);
		//saver.saveImg( sPathOutTif, ImageJFunctions.wrap( VolumeMisc.wrapImgImagePlusCal(Views.interval( Views.extendZero( outS),allInt ),"test",cal)), config );
		saver.saveImg( sPathOutTif,sourceImg, config );

		//IJ.saveAsTiff(VolumeMisc.wrapImgImagePlusCal(Views.interval( Views.extendZero( outS),allInt ),"test",cal),sPathOutTif);
		//ImageJFunctions.show( outS );
		return true;
	}
	
	public void generateAllVolumes(String sOutputPath)
	{
		//for(int i=0;i<20;i++)
		for(int i=0;i<nFrames;i++)
		{
			generateSingleVolumeTemplate(i, sOutputPath);
			//generateSingleVolume(i);
			IJ.log( "Saved volume "+Integer.toString( i+1 )+" from "+Integer.toString( nFrames )+"." );
		}
	}
	
	public void loadTemplate(String sTemplateTIF)
	{
		final ImagePlus imp = IJ.openImage( sTemplateTIF );
		template = ImageJFunctions.wrap( imp );
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
		return Math.PI-Math.acos( LinAlgHelpers.dot( vect1, vect2 ));
		
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
	

	
	public static void main( final String[] args )
	{
		new ImageJ();
		BigTrace<UnsignedShortType> bt = new BigTrace<>(); 
		
		//bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/HyperStack.tif");
		bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/Chlorophis_irregularis-XYZ_divby8.tif");
		//bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/Chlorophis_irregularis-XYZ_divby8_8bit_3fr.tif");

		try
		{
			bt.btMacro.macroLoadROIs( "/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/Chlorophis_irregularis-XYZ_divby8.tif_btrois.csv", "Clean" );
			bt.btMacro.macroShapeInterpolation( "Spline", 20 );
		}
		catch ( InterruptedException exc )
		{
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		//wait to load things
		while(bt.bInputLock)
		{
			try
			{
				Thread.sleep(100);
			}
			catch ( InterruptedException exc )
			{
				// TODO Auto-generated catch block
				exc.printStackTrace();
			}
		}

		LineTrace3D firstLine = (LineTrace3D) bt.roiManager.rois.get( 0 );
		
		UnCurveAnimation<UnsignedShortType> unBend = new UnCurveAnimation<>(bt);
		
		unBend.generateROIs( firstLine );	
		//unBend.testFrames();
		unBend.loadTemplate( "/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/clean_X_trace172232825_straight.tif" );
		
		unBend.generateAllVolumes("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/out/");
		//unBend.generateSingleVolume(1);
		
	}
	

}

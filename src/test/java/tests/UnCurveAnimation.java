package tests;

import java.util.ArrayList;

import net.imglib2.FinalInterval;
import net.imglib2.RealPoint;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.geometry.Plane3D;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import ij.ImageJ;

public class UnCurveAnimation < T extends RealType< T > & NativeType< T > > 
{
	/** total number of intermediate frames **/
	int nFrames = 100;
	
	/** interval containing all ROIs **/
	FinalInterval allInt;
	
	/** plugin instance **/
	BigTrace<T> bt;
	
	public UnCurveAnimation(final BigTrace<T> bt_)
	{
		bt = bt_;	
	}
	
	/** Given input ROI (of AbstractCurve type), 
	generates output ROIs that gradually are straightened.
	They are added to ROI manager at subsequent time frames **/
	public void generateROIs(final AbstractCurve3D firstLine)
	{
		//ArrayList<double []> tangents = firstLine.getJointSegmentTangentsResampled();
		
		//get all the points along the line with pixel sampling
		ArrayList<RealPoint> segment = Roi3D.scaleGlobInv(firstLine.getJointSegmentResampled(),BigTraceData.globCal);
		
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
		double [] rotated = new double [3];
		
		for (int i=1; i<nFrames ; i++)
		{
			ArrayList<RealPoint> segment_new = new ArrayList<>();
			for (int nPoint = 0; nPoint<nTotPoints; nPoint++)
			{
				segment_new.add( segment.get( nPoint ) );
			}

			for(int nTrio =nTotPoints-2; nTrio>=0; nTrio--)
			{
				LinAlgHelpers.quaternionFromAngleAxis(planes[nTrio].n, lineAngles[nTrio]*i/(nFrames-1), q);
				//LinAlgHelpers.quaternionFromAngleAxis(planes[nTrio].n, lineAngles[nTrio], q);
				segment_new.get(nTrio).localize( joint );
				for(int nPoint = nTrio+1; nPoint<nTotPoints; nPoint++)
				{
					segment_new.get( nPoint ).localize( notrotated );
					LinAlgHelpers.subtract( notrotated, joint, notrotated );
					LinAlgHelpers.quaternionApply( q, notrotated, rotated );
					LinAlgHelpers.add( rotated, joint, rotated );
					segment_new.set(nPoint, new RealPoint (rotated) );
				}
			}
			LineTrace3D newROI = addROIsegment(bt, segment_new, i, ( int ) firstLine.getLineThickness());
			allInt = Intervals.union( allInt, newROI.getBoundingBox() );
		}
		for (int d=0;d<3; d++)
		{
			System.out.print( allInt.dimension( d ) );
		}
		
	}
	
	public static void main( final String[] args )
	{
		new ImageJ();
		BigTrace bt = new BigTrace(); 
		
		bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/HyperStack.tif");
		//bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/snake/Chlorophis_irregularis-XYZ_divby8.tif");

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
		
		UnCurveAnimation unBend = new UnCurveAnimation(bt);
		
		unBend.generateROIs( firstLine );		

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
	
	public static double calculateAngle (RealPoint p1,RealPoint p2,RealPoint p3)
	{
		double [] vect1 = new double[3];
		double [] vect2 = new double[3];
		LinAlgHelpers.subtract( p1.positionAsDoubleArray(), p2.positionAsDoubleArray(), vect1 );
		LinAlgHelpers.subtract( p3.positionAsDoubleArray(), p2.positionAsDoubleArray(), vect2 );
		LinAlgHelpers.normalize( vect1 );
		LinAlgHelpers.normalize( vect2 );
		return Math.PI-Math.acos( LinAlgHelpers.dot( vect1, vect2 ));
		
	}
	
	public static LineTrace3D addROIsegment(final BigTrace bt, ArrayList<RealPoint> segment, int nTimeFrame, int nThickness)
	{
		LineTrace3D aLine = (LineTrace3D) bt.roiManager.makeRoi(Roi3D.LINE_TRACE, nTimeFrame);
		aLine.addFirstPoint( segment.get( 0 ));
		aLine.addPointAndSegment( segment.get( segment.size()-1 ), segment );
		aLine.setLineThickness( nThickness );
		bt.roiManager.addRoi( aLine );	
		return aLine;
	}
}

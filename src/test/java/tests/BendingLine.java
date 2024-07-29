package tests;

import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

import bigtrace.BigTrace;
import bigtrace.geometry.Plane3D;
import bigtrace.rois.LineTrace3D;
import bigtrace.rois.Roi3D;
import bigtrace.rois.Roi3DGroup;
import ij.ImageJ;

public class BendingLine
{
	public static void main( final String[] args )
	{
		new ImageJ();
		BigTrace bt = new BigTrace(); 
		
		bt.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_progress/20240726_bending/HyperStack.tif");
		
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
		//add a ROI
		LineTrace3D firstLine = (LineTrace3D) bt.roiManager.makeRoi(Roi3D.LINE_TRACE, 0);
		//LineTrace3D firstLine = new LineTrace3D((Roi3DGroup)bt.roiManager.groups.get( 0 ), 0);
		
		ArrayList<RealPoint> segment = new ArrayList<>();
		double [] point = new double [3];
		
		double dz = 2;
		double R = 10;
		double dAngleCurr = 0;
		double dAngleStep = 0.2;
		firstLine.addFirstPoint( new RealPoint(point) );
		segment.add( new RealPoint(point) );
		int nTotPoints = 100;
		for (int nPoint = 1; nPoint<nTotPoints; nPoint++)
		{
			dAngleCurr += dAngleStep;
			point[0]+=R*Math.cos( dAngleCurr );
			point[1]+=R*Math.sin( dAngleCurr );
			point[2]+=dz;
			segment.add( new RealPoint(point) );
		}
		firstLine.addPointAndSegment( segment.get( nTotPoints-1 ), segment );
		bt.roiManager.addRoi( firstLine );
		
		//initial orientation
		double [] iniOrientP = segment.get( 0 ).positionAsDoubleArray();
		LinAlgHelpers.subtract( iniOrientP, new double[] {1.0,0.,0.0}, iniOrientP );
		RealPoint iniOrientRP = new RealPoint(iniOrientP);
		//analysis, let's measure angles
		
		double [] lineAngles = new double[nTotPoints-1];
		Plane3D [] planes = new Plane3D [nTotPoints-1];
		
		lineAngles[0] = calculateAngle(iniOrientRP,segment.get(0),segment.get(1));
		planes[0] = new Plane3D(iniOrientRP,segment.get(0),segment.get(1));
		
		for(int i = 0; i<nTotPoints-2;i++)
		{
			lineAngles[i+1] = calculateAngle(segment.get( i ),segment.get(i+1),segment.get(i+2));
			planes[i+1] = new Plane3D(segment.get(i),segment.get(i+1),segment.get(i+2));
		}
		
		
		
		double [] q = new double[4];
		double [] joint = new double [3];
		double [] notrotated = new double [3];
		double [] rotated = new double [3];
		int nFrames = 100;
		for (int i=1; i<nFrames ; i++)
		{
			ArrayList<RealPoint> segment_new = new ArrayList<>();
			for (int nPoint = 0; nPoint<nTotPoints; nPoint++)
			{
				segment_new.add( segment.get( nPoint ) );
			}
			//int nTrio=0;
			for(int nTrio =nTotPoints-2; nTrio>=0; nTrio--)
			//for(int nTrio =0; nTrio<nTotPoints-1; nTrio++)
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
			addROIsegment(bt, segment_new, i);
		}
		
		
//		Plane3D plane = new Plane3D(segment.get(0),segment.get(1),segment.get(2));
//		double [] q = new double[4];
//		double [] joint = segment.get(1).positionAsDoubleArray();
//		double [] notrotated = new double [3];
//		double [] rotated = new double [3];
//		for (int i=1; i<50; i++)
//		{
//			ArrayList<RealPoint> segment_new = new ArrayList<>();
//			for (int nPoint = 0; nPoint<nTotPoints; nPoint++)
//			{
//				segment_new.add( segment.get( nPoint ) );
//			}
//			LinAlgHelpers.quaternionFromAngleAxis( plane.n, Math.PI/30.*i, q);
//
//			for(int nPoint = 2; nPoint<nTotPoints; nPoint++)
//			{
//				segment_new.get( nPoint ).localize( notrotated );
//				LinAlgHelpers.subtract( notrotated, joint, notrotated );
//				LinAlgHelpers.quaternionApply( q, notrotated, rotated );
//				LinAlgHelpers.add( rotated, joint, rotated );
//				segment_new.set(nPoint, new RealPoint (rotated) );
//			}
//			addROIsegment(bt, segment_new, i);
//		}
		
//		double [] q = new double[4];
//		double [] axis = new double [3];
//		axis[0]=1;
//		axis[1]=1;
//		axis[2]=1;
//		LinAlgHelpers.subtract( segment.get( 1 ).positionAsDoubleArray(), segment.get( 0 ).positionAsDoubleArray(), axis );
//		
//		LinAlgHelpers.normalize( axis );
//		
//		for (int i=1; i<50; i++)
//		{
//			LinAlgHelpers.quaternionFromAngleAxis( axis, Math.PI/20.*i, q);
//			ArrayList<RealPoint> segment_new = new ArrayList<>();
//			double [] notrotated = new double [3];
//			double [] rotated = new double [3];
//			for (int nPoint = 0; nPoint<nTotPoints; nPoint++)
//			{
//				segment.get( nPoint ).localize( notrotated );
//				LinAlgHelpers.quaternionApply( q, notrotated, rotated );
//				segment_new.add( new RealPoint (rotated) );
//			}
//			addROIsegment(bt, segment_new, i+1);
//		}

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
	
	public static void addROIsegment(final BigTrace bt, ArrayList<RealPoint> segment, int nTimeFrame)
	{
		LineTrace3D aLine = (LineTrace3D) bt.roiManager.makeRoi(Roi3D.LINE_TRACE, nTimeFrame);
		aLine.addFirstPoint( segment.get( 0 ));
		aLine.addPointAndSegment( segment.get( segment.size()-1 ), segment );
		bt.roiManager.addRoi( aLine );	
	}
}

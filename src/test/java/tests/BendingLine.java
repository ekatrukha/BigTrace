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
		
		double dz = 20;
		double R = 30;
		double dAngleCurr = 0;
		double dAngleStep = 1.0;
		firstLine.addFirstPoint( new RealPoint(point) );
		segment.add( new RealPoint(point) );
		int nTotPoints = 4;
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
		
		
		Plane3D plane = new Plane3D(segment.get(0),segment.get(1),segment.get(2));
		double [] q = new double[4];
		double [] joint = segment.get(1).positionAsDoubleArray();
		double [] notrotated = new double [3];
		double [] rotated = new double [3];
		for (int i=1; i<50; i++)
		{
			ArrayList<RealPoint> segment_new = new ArrayList<>();
			segment_new.add( segment.get(0) );
			segment_new.add( segment.get(1) );
			LinAlgHelpers.quaternionFromAngleAxis( plane.n, Math.PI/30.*i, q);

			for(int nPoint = 2; nPoint<nTotPoints; nPoint++)
			{
				segment.get( nPoint ).localize( notrotated );
				LinAlgHelpers.subtract( notrotated, joint, notrotated );
				LinAlgHelpers.quaternionApply( q, notrotated, rotated );
				LinAlgHelpers.add( rotated, joint, rotated );
				segment_new.add( new RealPoint (rotated) );
			}
			addROIsegment(bt, segment_new, i);
		}
		
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
	
	public static void addROIsegment(final BigTrace bt, ArrayList<RealPoint> segment, int nTimeFrame)
	{
		LineTrace3D aLine = (LineTrace3D) bt.roiManager.makeRoi(Roi3D.LINE_TRACE, nTimeFrame);
		aLine.addFirstPoint( segment.get( 0 ));
		aLine.addPointAndSegment( segment.get( segment.size()-1 ), segment );
		bt.roiManager.addRoi( aLine );	
	}
}

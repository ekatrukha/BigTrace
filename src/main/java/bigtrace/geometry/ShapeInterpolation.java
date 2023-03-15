package bigtrace.geometry;


import java.util.ArrayList;

import bigtrace.BigTraceData;
import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

public class ShapeInterpolation {
	
	
	/**
	 * returns smoothed coordinates along each axis
	 * using running average with window defined by 
	 * static variable of Smoothing class (one for everywhere for now, it is static BigTraceData.nSmoothWindow);
	 * 
	 * Boundaries (ends) are handled in a special way:
	 * 1) end points' positions are not averaged
	 * 2) moving average window's size when dealing with points that are close to the end points is reduced. 
	 *  If the distance (in numbers/points) between current point and end point
	 *  is less than half of average window, it becomes new half average window.
	 *  It works similar to Matlab's "smooth()" function;
	 * **/
	public static ArrayList< RealPoint > getSmoothVals (final ArrayList< RealPoint > points)
	{
		ArrayList< RealPoint > out = new ArrayList< RealPoint >();
		double [][] coords= new double[points.size()][3];
		double [] aver = new double[3];
		int i,j,k;
		//int nCount;
		int nHalfWindow = (int)Math.floor(BigTraceData.nSmoothWindow*0.5);
		int nCurrWindow = 0;

		if(points.size()<3)
		{
			return points;
		}


		for (i=0;i<points.size();i++)
		{
				points.get(i).localize(coords[i]);
		}
		
		
		out.add(new RealPoint(points.get(0)));
		for (i=1;i<points.size()-1;i++)
		{
			//nCount = 0;
			
			for(j=0;j<3;j++)
			{
				aver[j]=0.0;
			}

					
			nCurrWindow = Math.min(Math.min(nHalfWindow,i), Math.min(nHalfWindow, points.size()-i-1));
			for(j=(i-nCurrWindow);j<(i+nCurrWindow+1);j++)
			{
				for(k=0;k<3;k++)
				{
					aver[k]+=points.get(j).getDoublePosition(k);
				}
				//nCount++;
			}
			LinAlgHelpers.scale(aver, 1.0/(nCurrWindow*2+1), aver);
			out.add(new RealPoint(aver));
		}
		out.add(new RealPoint(points.get(points.size()-1)));
		return out;
	}
}

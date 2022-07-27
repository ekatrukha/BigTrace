package bigtrace.geometry;


import java.util.ArrayList;

import net.imglib2.RealPoint;
import net.imglib2.util.LinAlgHelpers;

public class Smoothing {
	
	public static int nSmoothWindow=5;
	/**
	 * returns smoothed coordinates along each axis
	 * using running average with window defined by 
	 * static variable of Smoothing class (one for all)
	 * **/
	public static ArrayList< RealPoint > getSmoothVals (final ArrayList< RealPoint > points)
	{
		ArrayList< RealPoint > out = new ArrayList< RealPoint >();
		double [][] coords= new double[points.size()][3];
		double [] aver = new double[3];
		int i,j,k;
		int nCount;
		int nHalfWindow = (int)Math.floor(nSmoothWindow*0.5);
		if(nSmoothWindow>points.size() || Math.abs(nSmoothWindow)<3)
		{
			return points;
		}
		for (i=0;i<points.size();i++)
		{
			//for(j=0;j<points.size();j++)
			//{
				points.get(i).localize(coords[i]);
			//}
		}
		out.add(new RealPoint(points.get(0)));
		for (i=1;i<points.size()-1;i++)
		{
			nCount = 0;
			
			for(j=0;j<3;j++)
			{
				aver[j]=0.0;
			}
			for(j=Math.max(0, i-nHalfWindow);j<Math.min(points.size(), i+nHalfWindow+1);j++)
			{
				for(k=0;k<3;k++)
				{
					aver[k]+=points.get(j).getDoublePosition(k);
				}
				nCount++;
			}
			LinAlgHelpers.scale(aver, 1.0/nCount, aver);
			out.add(new RealPoint(aver));
		}
		out.add(new RealPoint(points.get(points.size()-1)));
		return out;
	}
}

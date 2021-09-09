package bigtrace.polyline;

import java.util.ArrayList;

import net.imglib2.RealPoint;

public class BTPolylines {
	
	 public ArrayList<ArrayList<RealPoint> > lines =              new ArrayList<ArrayList<RealPoint> >();
	 public int nLinesN = 0;
	 public int activeLine = -1;
	 
	 public BTPolylines()
	 {
		 addNewLine();
	 }
	 
	 public void addNewLine()
	 {
		 lines.add(new ArrayList<RealPoint>());
		 activeLine = lines.size()-1; 
		 nLinesN=lines.size();
	 }
	 
	 //public ArrayList<RealPoint> getActive()
	 //{
		// return lines.get(activeLine);
	 //}
	 public void addPointToActive(RealPoint in_)
	 {
		 lines.get(activeLine).add(new RealPoint(in_));
	 }
	 
	 public boolean removeLastPointFromActive()
	 {
		 
		 int nP=  lines.get(activeLine).size();
		 if(nP>0)
			{
				lines.get(activeLine).remove(nP-1);
				return true;
			}
		 return false;
	 }
	 public ArrayList<RealPoint> get(int i)
	 {
		 return lines.get(i);
	 }

}

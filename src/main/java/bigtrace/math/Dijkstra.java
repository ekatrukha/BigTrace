package bigtrace.math;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Predicate;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
public class Dijkstra {
	
	
	

	/** weights (saliency) of each voxel  **/
	IntervalView< UnsignedByteType > trace_weights;
	/** Interval containing calculated costs AND simulataneously used as
	 * marks for visiting/processing. Processed voxel values are negative,
	 * while unprocessed are positive or zero (initial, unprocessed)  **/
	public IntervalView< IntType > ccost;
	public IntervalView< UnsignedByteType > dirs;

	public PriorityQueue<Cursor< IntType >> queue;
	
	Shape voxShape = new RectangleShape( 1, true);
	long [] iniPoint;
	long[] dim;
	/** Computes the shortest path based on the given cost values and
	 vectors.
	 	**/
	public Dijkstra(IntervalView< UnsignedByteType > trace_weights_, RealPoint startPoint_)
	//public Dijkstra(IntervalView< UnsignedByteType > trace_weights_, IntervalView< FloatType > trace_vectors_, RealPoint startPoint_)
	{
		
		//int nCount=0;
		trace_weights = trace_weights_;
		//trace_vectors = trace_vectors_;
		dim = Intervals.dimensionsAsLongArray( trace_weights );
		ArrayImg<IntType, IntArray> costInt = ArrayImgs.ints(dim);
		ccost = Views.translate(costInt, trace_weights.minAsLongArray());

		ArrayImg<UnsignedByteType, ByteArray> dirsInt = ArrayImgs.unsignedBytes(dim);
		dirs = Views.translate(dirsInt, trace_weights.minAsLongArray());

		
		long [] currPoint = new long [dim.length];
		iniPoint = new long [dim.length];
		for (int i =0;i<dim.length; i++)
		{
			iniPoint[i]=(long)Math.round(startPoint_.getFloatPosition(i));
			currPoint[i]=iniPoint[i];
		}
		
		queue = new PriorityQueue<Cursor< IntType >>(14000, new CursorCompare());
		
		
		//new cost neighborhood
		final RandomAccessible< Neighborhood< IntType > > costNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendValue(ccost,-1) );				
		final RandomAccess< Neighborhood< IntType > > cnRA = costNeighborhoods.randomAccess();
		
		//weight of each pixel
		final RandomAccessible< Neighborhood< UnsignedByteType > > weightNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(trace_weights) );		
		final RandomAccess< Neighborhood< UnsignedByteType > > wnRA = weightNeighborhoods.randomAccess();

		//direction to the "best pixel"
		final RandomAccessible< Neighborhood< UnsignedByteType > > dirsNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(dirs) );		
		final RandomAccess< Neighborhood< UnsignedByteType > > dnRA = dirsNeighborhoods.randomAccess();
		
		
		final RandomAccess< IntType> ccostRA = ccost.randomAccess();			
		final RandomAccess< UnsignedByteType> weightRA = trace_weights.randomAccess();
		final RandomAccess< UnsignedByteType> dirsRA = dirs.randomAccess();

		Neighborhood< IntType > cnNH; 
		Neighborhood< UnsignedByteType > wnNH;
		Neighborhood< UnsignedByteType > dnNH;
		Cursor< IntType > cnC;
		Cursor< UnsignedByteType > wnC;
		Cursor< UnsignedByteType > dnC;
		Cursor< IntType > nextNode;
		
		//starting point
		ccostRA.setPosition(currPoint);
		ccostRA.get().set(1);
		boolean bQueue = true;

		//int nCount = 0;
		//int [] queueSize = new int[(int)(dim[0]*dim[1])];//*dim[2])];
		int nMaxQ=0;
		int nValVox;
		int nDir = 0;
		// Path searching:
		while (bQueue) 
		{

			ccostRA.setPosition(currPoint);
			weightRA.setPosition(currPoint);
			dirsRA.setPosition(currPoint);

			cnRA.setPosition(currPoint);
			wnRA.setPosition(currPoint);
			dnRA.setPosition(currPoint);
			cnNH = cnRA.get();
			wnNH = wnRA.get();
			dnNH = dnRA.get();
			cnC=cnNH.cursor();
			wnC=wnNH.cursor();
			dnC=dnNH.cursor();
			//mark current position as processed, i.e. make cost negative
			ccostRA.get().mul(-1.0);
			//iterate through the neighborhood
			nDir=-1;
			while ( cnC.hasNext() )
			{
				nDir++;
				cnC.fwd();
				wnC.fwd();
				dnC.fwd();
				//int currCost=(-1)*ccostRA.get().get();
				//long [] lll=cnC.positionAsLongArray();
				//long  [] ll2=wnC.positionAsLongArray();
				nValVox=(-1)*ccostRA.get().get();
				if(cnC.get().get()>=0)
				{
					int iCurCCost = cnC.get().get();	
					int nW = wnC.get().get();
					//if (nW==0)
					//{
					//	cnC.get().set(-1);
					//	dnC.get().set(100);
					//}
					//else
					{
						int iNewCCost = nValVox + (255 - nW);
						if (iNewCCost < iCurCCost || iCurCCost==0)
						{
							cnC.get().set(iNewCCost);
							queue.add(cnC.copyCursor());
							dnC.get().set(nDir);
						}
					}
				}
			}//iteration through neighborhood end
			
			boolean bNextFound = false;
			//let's look for a node with minimal cost
			while (!bNextFound)
			{
				//clear the queue
				//Predicate<Cursor<IntType>> removeChecked = curs -> curs.get().get()<0;
				//queue.removeIf(removeChecked);
				nextNode =	queue.poll();
				//end of queue, finish everything
				if (nextNode== null)
				{
					bNextFound = true;
					bQueue = false;
				}
				else
				{
					//maybe it was already processed?
					//no? ok, let's get it
					if(nextNode.get().get()>0)
					{
						bNextFound = true;
						nextNode.localize(currPoint);
					}
				}
			}
			
			if(queue.size()>nMaxQ)
				nMaxQ=queue.size();
			
			
			// debug stuff
			//print current cost matrix
			/*
			queueSize[nCount]=queue.size(); 
			nCount++;
			System.out.println("Cost Matrix step="+Integer.toString(nCount));
		    RandomAccess<IntType> rA = ccost.randomAccess();
		    int [] pos = new int [2];
		    int N=3;
		    for (int i=0;i<N;i++)
		    {
		    	for (int j=0;j<N;j++)
		    	{
		    		pos[0]=j;
		    		pos[1]=i;
		    		rA.setPosition(pos);
		    		System.out.print(rA.get().get());
		    		System.out.print(" ");
		    	}
		    	System.out.print("\n");
		    }
		    */
		}//queue end 
		System.out.println("max queue:"+Integer.toString(nMaxQ));
		//mark initial node with zero cost
		ccostRA.setPosition(iniPoint);
		ccostRA.get().setZero();
		dirsRA.setPosition(iniPoint);
		//middle of neighborhood
		dirsRA.get().set(100);
	}
	
	public ArrayList<RealPoint> getTrace(final RealPoint click)
	{
		ArrayList<RealPoint> finSegment = new ArrayList<RealPoint>();
		RealPoint currRP = new RealPoint(click);
		int i;
		float [][] neibIndexes = new float [26][3];
		float [] currV = new float[3];
		long [] currPoint =new long [dim.length];
		long [] endPoint = new long [dim.length];
		iniPoint = new long [dim.length];
		for (i =0;i<dim.length; i++)
		{
			endPoint[i]=(long)Math.round(click.getFloatPosition(i));
		}
		
	
		
		
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes(1, 1, 1 );
		final RandomAccessible< Neighborhood< UnsignedByteType > > neighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero( img) );
		final RandomAccess< Neighborhood< UnsignedByteType > > na = neighborhoods.randomAccess();
		na.setPosition( new int[] { 0, 0, 0 } );
		
		final Neighborhood< UnsignedByteType > neighborhood = na.get();

		Cursor< UnsignedByteType > c = neighborhood.cursor();
		int nCount = 0;
		while ( c.hasNext() )
		{
			c.fwd();
			c.localize(currV);
			for (i=0;i<3;i++)
			{
				neibIndexes[nCount][i]=-currV[i];
			}
			nCount++;
		}
		
		final RandomAccess< UnsignedByteType> dirsRA = dirs.randomAccess();
		
		boolean bArrived = false;
		//initial spot
		finSegment.add(new RealPoint(click));
		while (!bArrived)
		{
			dirsRA.setPosition(endPoint);
			i=dirsRA.get().get();

			if (i==100)
			{
				bArrived = true;
			}
			else
			{
				currRP.move(neibIndexes[i]);
				finSegment.add(new RealPoint(currRP));
				for (i =0;i<dim.length; i++)
				{
					endPoint[i]=(long)Math.round(currRP.getFloatPosition(i));
				}
			}
			
		}
		//click.setPosition(currRP);
		return finSegment;
	}
	
	public class CursorCompare implements Comparator<Cursor<IntType>> 
	{

		@Override
		public int compare(Cursor<IntType> o1, Cursor<IntType> o2) {
			// TODO Auto-generated method stub
			
			return Integer.compare(o1.get().get(), o2.get().get());
		}

		
	}
	//test Dijkstra on some simple example
	public static void main(String[] args) 
	{
		
		int N=3;
		/*final byte[] values = {
				1, 1, 1, 1,
				1, 1, 1, 1,
				1, 1, 1, 1,
				1, 1, 1, 1,
		};		*/
		final byte[] values = {
				1, 1, 1,
				0, 1, 1,
				1, 1, 1
		};
		for (int i=0;i<values.length;i++)
		{
			values[i] = (byte) (127-values[i]);
		}
		
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( values, N, N);
		IntervalView< UnsignedByteType > trace_weights = Views.interval(img, new long[] {0,0},new long[] {N-1,N-1});
		
		Dijkstra dijTest = new Dijkstra(trace_weights, new RealPoint(0,0));
		

		System.out.println("Final Cost Matrix");
	    RandomAccess<IntType> rA = dijTest.ccost.randomAccess();
	    int [] pos = new int [2];
	    for (int i=0;i<N;i++)
	    {
	    	for (int j=0;j<N;j++)
	    	{
	    		pos[0]=j;
	    		pos[1]=i;
	    		rA.setPosition(pos);
	    		System.out.print(rA.get().get());
	    		System.out.print(" ");
	    	}
	    	System.out.print("\n");
	    }
	}
}

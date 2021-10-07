package bigtrace.math;

import java.util.ArrayList;

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
public class DijkstraFHRestricted {
	
	public static final int ININODE=100, BORDER_ADD=50;
	

	/** weights (saliency) of each voxel  **/
	IntervalView< UnsignedByteType > trace_weights;
	/** Interval containing calculated costs AND simulataneously used as
	 * marks for visiting/processing. Processed voxel values are negative,
	 * while unprocessed are positive or zero (initial, unprocessed)  **/
	public IntervalView< IntType > ccost;
	public IntervalView< UnsignedByteType > dirs;


	public FibonacciHeap<Integer> queue;

	
	Shape voxShape = new RectangleShape( 1, true);
	long [] iniPoint;
	long[] dim;
	
	/** Computes the shortest path based on the given cost values and
	 vectors.
	 	**/
	public DijkstraFHRestricted(IntervalView< UnsignedByteType > trace_weights_, RealPoint startPoint_)
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
		long nTotPix = 1;
		for (int i =0;i<dim.length; i++)
		{
			iniPoint[i]=(long)Math.round(startPoint_.getFloatPosition(i));
			currPoint[i]=iniPoint[i];
			nTotPix *=dim[i];
		}
		long [][] pos = new long [(int)(nTotPix)][dim.length];
		//queue = new PriorityQueue<Cursor< IntType >>(25000, new CursorCompare());
		queue = new FibonacciHeap<Integer>();
		//entries = new ArrayList<Entry<Cursor< IntType >>>(25000);
	
		
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

		final RandomAccess< UnsignedByteType> dirsRA = dirs.randomAccess();


		Cursor< IntType > cnC;
		//Cursor< IntType > enC;
		Cursor< UnsignedByteType > wnC;
		Cursor< UnsignedByteType > dnC;
		//Cursor< IntType > nextNode;
		//Entry<Cursor< IntType >> nextEntry;
		
		//starting point
		ccostRA.setPosition(currPoint);
		ccostRA.get().set(1);
		boolean bQueue = true;

		//int nCount = 0;
		//int [] queueSize = new int[(int)(dim[0]*dim[1])];//*dim[2])];
		int nMaxQ=0;
		int nValVox;
		int nDir = 0;
		int nEnQ=0;
		int iCurCCost;
		int iNewCCost;
		int nW;
		int maxCost=0;
		// Path searching:
		while (bQueue) 
		{

			ccostRA.setPosition(currPoint);
			
			cnRA.setPosition(currPoint);
			wnRA.setPosition(currPoint);
			dnRA.setPosition(currPoint);
			cnC = cnRA.get().cursor();
			//enNH = enRA.get();
			wnC = wnRA.get().cursor();
			dnC = dnRA.get().cursor();
			

			//mark current position as processed, i.e. make cost negative
			nValVox=ccostRA.get().get();
			ccostRA.get().set(nValVox*(-1));
			//ccostRA.get().mul(-1.0);
			//iterate through the neighborhood
			//count starts from 1
			nDir=0;
			//nValVox=(-1)*ccostRA.get().get();
			while ( cnC.hasNext() )
			{
				nDir++;
				cnC.fwd();
				//enC.fwd();
				wnC.fwd();
				dnC.fwd();

				iCurCCost = cnC.get().get();
				if(iCurCCost>=0)
				{
						
					nW = wnC.get().get();
					if (nW==0)
					{
						cnC.get().set(-1);
						dnC.get().set(BORDER_ADD+nDir);
					}
					else
					{
						//iNewCCost = nValVox + (255 - nW);
						iNewCCost = nValVox + (255 - nW);
						if (iNewCCost < iCurCCost || iCurCCost==0)
						{
							cnC.get().set(iNewCCost);
							//add element to the queue
							//queue.enqueue(cnC.copyCursor(), iNewCCost);
							cnC.localize(pos[nEnQ]);
							if(iCurCCost==0)
							{
								queue.enqueue(nEnQ, iNewCCost);
							}
							else
							{
								int nn = 1;//enC.get().get();
							
							}

							//queue.enqueue(pos[nEnQ], iNewCCost);
							/*if(iNewCCost>maxCost)
								maxCost=iNewCCost;*/
							nEnQ++;
							int vv=0;
							if (iCurCCost==0)
							{																								
								//entries.add(queue.enqueue(cnC.copyCursor(), iNewCCost));
								//enC.get().set(entries.size()-1);
								vv=1;
							}
							else
							{
								//int nn = enC.get().get();
								//queue.decreaseKey(entries.get(enC.get().get()), iNewCCost);
								vv=2;
							}
							dnC.get().set(nDir);
						}
					}
				}
			}//iteration through neighborhood end
			
			if(queue.isEmpty())
			{
				bQueue = false;
			}
			else
			{
				currPoint=pos[queue.dequeueMin().getValue()];
			}

	
		}//queue end 
		System.out.println("max queue:"+Integer.toString(nMaxQ));
		System.out.println("max cost:"+Integer.toString(maxCost));
		//mark initial node with zero cost
		ccostRA.setPosition(iniPoint);
		ccostRA.get().setZero();
		dirsRA.setPosition(iniPoint);
		//middle of neighborhood
		dirsRA.get().set(ININODE);
	}
	
	public boolean getTrace(final RealPoint click, final ArrayList<RealPoint> finSegment)
	{
		RealPoint currRP = new RealPoint(click);
		int i, nDir;
		float [][] neibIndexes = new float [26][3];
		float [] currV = new float[3];
		//long [] currPoint =new long [dim.length];
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
		//check that it was explored
		dirsRA.setPosition(endPoint);
		nDir=dirsRA.get().get();			
		if(nDir>0)
		{
			boolean bArrived = false;

			//initial spot
			finSegment.add(new RealPoint(click));

			while (!bArrived)
			{
				dirsRA.setPosition(endPoint);
				nDir=dirsRA.get().get();
				//was it explored?
						
					if (nDir==ININODE)
					{
						bArrived = true;
					}
					else
					{
						if(nDir>BORDER_ADD)
						{
							nDir-=BORDER_ADD;
						}
						nDir--;
						currRP.move(neibIndexes[nDir]);
						finSegment.add(new RealPoint(currRP));
						for (i =0;i<dim.length; i++)
						{
							endPoint[i]=(long)Math.round(currRP.getFloatPosition(i));
						}
					}
				
			}
		}
		else
		{
			System.out.println("no contact :(");
			return false;
		}

		//click.setPosition(currRP);
		return true;
	}

	/** provided list of coordinates in allCorners, 
	 * function returns only those that were in explored area,
	 * (i.e. dirs array was marked) **/
	public ArrayList<long []> exploredCorners(ArrayList<long []> allCorners)
	{
		int i;
		int nCornNum=0;
		
		
		ArrayList<long []> markedCorners = new ArrayList<long []> (); 
		final RandomAccess< UnsignedByteType> dirsRA = dirs.randomAccess();
		for (i=0;i<allCorners.size(); i++)
		{
			dirsRA.setPosition(allCorners.get(i));
			
			if(dirsRA.get().get()>0)
			{
				markedCorners.add(allCorners.get(i));
				nCornNum++;
			}
		}
		System.out.println("Corners found:"+Integer.toString(nCornNum));
		return markedCorners;
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
				1, -120, -120,
				1, 1, -120,
				1, 1, -120
		};
		for (int i=0;i<values.length;i++)
		{
			values[i] = (byte) (127-values[i]);
		}
		
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( values, N, N);
		IntervalView< UnsignedByteType > trace_weights = Views.interval(img, new long[] {0,0},new long[] {N-1,N-1});
		
		DijkstraFHRestricted dijTest = new DijkstraFHRestricted(trace_weights, new RealPoint(0,0));
		

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

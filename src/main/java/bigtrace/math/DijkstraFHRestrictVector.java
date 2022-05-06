package bigtrace.math;

import java.util.ArrayList;
import java.util.Collections;

import bigtrace.math.FibonacciHeap.Entry;
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
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.Composite;
import net.imglib2.view.composite.GenericComposite;


public class DijkstraFHRestrictVector {
	
	public static final int ININODE=100, BORDER_ADD=50, END_POINT=Integer.MAX_VALUE;
	

	/** weights (saliency) of each voxel  **/
	IntervalView< UnsignedByteType > trace_weights;
	
	/** Interval containing calculated costs AND simulataneously used as
	 * marks for visiting/processing. Processed voxel values are negative,
	 * while unprocessed are positive or zero (initial, unprocessed)  **/
	public IntervalView< IntType > ccost;

	public IntervalView< UnsignedByteType > dirs;
	
	public IntervalView< FloatType > vectors;
	
	public IntervalView< IntType > entriesInd;
	public ArrayList<Entry< Integer >> entries;

	public FibonacciHeap<Integer> queue;
	float [][] neibIndexes = new float [26][3];
	double [][] neibVectors = new double [26][3];
	
	public double orientationWeight = 0.0;
	
	Shape voxShape = new RectangleShape( 1, true);

	long [] iniPoint;
	long[] dim;
	
	/** Computes the shortest path based on the given cost values and
	 vectors.
	 	**/
	public DijkstraFHRestrictVector(IntervalView< UnsignedByteType > trace_weights_, IntervalView<FloatType> directionVectors, final double orientationW)//, RealPoint startPoint_)
	{
		
		//int nCount=0;
		trace_weights = trace_weights_;
		//trace_vectors = trace_vectors_;
		dim = Intervals.dimensionsAsLongArray( trace_weights );
		ArrayImg<IntType, IntArray> costInt = ArrayImgs.ints(dim);
		ccost = Views.translate(costInt, trace_weights.minAsLongArray());
		ArrayImg<IntType, IntArray> entryIndInt = ArrayImgs.ints(dim);
		entriesInd = Views.translate(entryIndInt, trace_weights.minAsLongArray());

		ArrayImg<UnsignedByteType, ByteArray> dirsInt = ArrayImgs.unsignedBytes(dim);
		dirs = Views.translate(dirsInt, trace_weights.minAsLongArray());
		//vectors = directionVectors;
		vectors = directionVectors;

		queue = new FibonacciHeap<Integer>();		
		orientationWeight = orientationW; 
		getNeighboursIndexesVectors();
	}
	
	/** calculates cost to all other points/voxels of the volume from the startPoint **/
	/*
	public void calcCost(RealPoint startPoint_)
	{
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
	
		
		//new cost neighborhood
		final RandomAccessible< Neighborhood< IntType > > costNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendValue(ccost,-1) );				
		final RandomAccess< Neighborhood< IntType > > cnRA = costNeighborhoods.randomAccess();
		
		//weight of each pixel
		final RandomAccessible< Neighborhood< UnsignedByteType > > weightNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(trace_weights) );		
		final RandomAccess< Neighborhood< UnsignedByteType > > wnRA = weightNeighborhoods.randomAccess();

		//direction to the "best pixel"
		final RandomAccessible< Neighborhood< UnsignedByteType > > dirsNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(dirs) );		
		final RandomAccess< Neighborhood< UnsignedByteType > > dnRA = dirsNeighborhoods.randomAccess();
		
		//indexes of positions in the queue
		final RandomAccessible< Neighborhood< IntType > > entryNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendValue(entriesInd,-1) );				
		final RandomAccess< Neighborhood< IntType > > enRA = entryNeighborhoods.randomAccess();
		
		// curve orientation 
		final RandomAccessible< Neighborhood< Composite<FloatType> > > orientNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.collapse(Views.extendZero(vectors)) );	
		final RandomAccess< Neighborhood< Composite<FloatType> > > onRA = orientNeighborhoods.randomAccess();
		
		final RandomAccess< IntType> ccostRA = ccost.randomAccess();
		
		//RealComposite <FloatType > fff;
		//final CompositeView<FloatType, ? extends GenericComposite<FloatType>> vectorsRAI = Views.collapse(vectors);
		final RandomAccess<? extends GenericComposite<FloatType>> orientRA = Views.collapse(vectors).randomAccess();

		final RandomAccess< UnsignedByteType> dirsRA = dirs.randomAccess();

			
		Cursor< IntType > cnC;
		Cursor< IntType > enC;
		Cursor< UnsignedByteType > wnC;
		Cursor< UnsignedByteType > dnC;
		Cursor< Composite<FloatType> > onC;
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
		int d;
		
		double [] currentOrient = new double [dim.length];
		double [] neighbourOrient = new double [dim.length];
		double angleCost;

		// Path searching:
		while (bQueue) 
		{

			ccostRA.setPosition(currPoint);
			
			cnRA.setPosition(currPoint);
			wnRA.setPosition(currPoint);
			dnRA.setPosition(currPoint);
			onRA.setPosition(currPoint);
			enRA.setPosition(currPoint);
			
			cnC = cnRA.get().cursor();
			wnC = wnRA.get().cursor();
			dnC = dnRA.get().cursor();
			onC = onRA.get().cursor();
			enC = enRA.get().cursor();
			//get current cost
			nValVox=ccostRA.get().get();
			//mark current position as processed, i.e. make cost negative
			ccostRA.get().set(nValVox*(-1));
			
			//get current orientation vector
			for(d=0;d<dim.length;d++)
			{
				currentOrient[d] = orientRA.get().get(d).get();
			}
			
			//iterate through the neighborhood
			//count starts from 1
			nDir=0;
			while ( cnC.hasNext() )
			{
				nDir++;
				cnC.fwd();
				wnC.fwd();
				dnC.fwd();
				onC.fwd();
				enC.fwd();

				iCurCCost = cnC.get().get();
				if(iCurCCost>=0)
				{
						
					//point with zero weight, i.e. "emptyness"
					nW = wnC.get().get();
					if (nW==0)
					{
						cnC.get().set(-1);
						dnC.get().set(BORDER_ADD+nDir);
					}
					else
					{
						
						//get orientation vector
						for(d=0;d<dim.length;d++)
						{
							neighbourOrient[d] = onC.get().get(d).get();
						}
						//calculate difference in orientation
						//angleCost = Math.round(255*(1.0-Math.abs(LinAlgHelpers.dot(currentOrient, neighbourOrient))));
						angleCost = getAngleCost(currentOrient, neighbourOrient,nDir-1);
						
						//old cost
						iNewCCost = nValVox + (int)(Math.round((1.0-orientationWeight)*(255 - nW))+Math.round(orientationWeight*255.0*angleCost));
						if (iNewCCost < iCurCCost || iCurCCost==0)
						{
							cnC.get().set(iNewCCost);
							dnC.get().set(nDir);
							//add element to the queue
							//queue.enqueue(cnC.copyCursor(), iNewCCost);
							
							//new record
							if(iCurCCost==0)
							{
								entries.add(queue.enqueue(nEnQ, iNewCCost));
								enC.get().set(nEnQ);
								cnC.localize(pos[nEnQ]);
								nEnQ++;
							}
							//this pixel we visited and new cost is lower
							else
							{
								queue.decreaseKey(entries.get(enC.get().get()), iNewCCost);
							
							}
							
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
	*/
	
	/** calculates cost to all other points/voxels of the volume from the startPoint_. 
	 * Stops and returns true if it reaches endPoint_,
	 * otherwise returns false **/
	public boolean calcCostTwoPoints(RealPoint startPoint_, RealPoint endPoint_)
	{
		long [] currPoint = new long [dim.length];
		long [] endPoint = new long [dim.length];
		iniPoint = new long [dim.length];
		long nTotPix = 1;
		for (int i =0;i<dim.length; i++)
		{
			iniPoint[i]=(long)Math.round(startPoint_.getFloatPosition(i));			
			currPoint[i]=iniPoint[i];
			nTotPix *=dim[i];
			endPoint[i]=(long)Math.round(endPoint_.getFloatPosition(i));	
		}
		long [][] pos = new long [(int)(nTotPix)][dim.length];
		entries = new ArrayList<Entry< Integer >>((int)(nTotPix));
		
		//int [] vals_test = new int[(int)(nTotPix)];
	
		
		//new cost neighborhood
		final RandomAccessible< Neighborhood< IntType > > costNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendValue(ccost,-1) );				
		final RandomAccess< Neighborhood< IntType > > cnRA = costNeighborhoods.randomAccess();
		
		//weight of each pixel
		final RandomAccessible< Neighborhood< UnsignedByteType > > weightNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(trace_weights) );		
		final RandomAccess< Neighborhood< UnsignedByteType > > wnRA = weightNeighborhoods.randomAccess();

		//direction to the "best pixel"
		final RandomAccessible< Neighborhood< UnsignedByteType > > dirsNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero(dirs) );		
		final RandomAccess< Neighborhood< UnsignedByteType > > dnRA = dirsNeighborhoods.randomAccess();
		
		//indexes of positions in the queue
		final RandomAccessible< Neighborhood< IntType > > entryNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendValue(entriesInd,-1) );				
		final RandomAccess< Neighborhood< IntType > > enRA = entryNeighborhoods.randomAccess();
		
		// curve orientation 
		final RandomAccessible< Neighborhood< Composite<FloatType> > > orientNeighborhoods = voxShape.neighborhoodsRandomAccessible(Views.collapse(Views.extendZero(vectors)) );	
		final RandomAccess< Neighborhood< Composite<FloatType> > > onRA = orientNeighborhoods.randomAccess();
		
		
		final RandomAccess< IntType> ccostRA = ccost.randomAccess();
		
		final RandomAccess<? extends GenericComposite<FloatType>> orientRA = Views.collapse(vectors).randomAccess();

		final RandomAccess< UnsignedByteType> dirsRA = dirs.randomAccess();
	
		Cursor< IntType > cnC;
		Cursor< IntType > enC;
		Cursor< UnsignedByteType > wnC;
		Cursor< UnsignedByteType > dnC;
		Cursor< Composite<FloatType> > onC;
		//Cursor< IntType > nextNode;
		//Entry<Cursor< IntType >> nextEntry;
	
		//end point
		ccostRA.setPosition(endPoint);
		ccostRA.get().set(END_POINT);
		
		//starting point
		ccostRA.setPosition(currPoint);
		ccostRA.get().set(1);

		boolean bQueue = true;

	
		int nMaxQ=0;
		int nValVox;
		int nDir = 0;
		int nEnQ=0;
		int iCurCCost;
		int iNewCCost;
		int nW;
		int d;
		
		double [] currentOrient = new double [dim.length];
		double [] neighbourOrient = new double [dim.length];
		double angleCost;
		
		
		// Path searching:
		while (bQueue) 
		{

			ccostRA.setPosition(currPoint);
			
			cnRA.setPosition(currPoint);
			wnRA.setPosition(currPoint);
			dnRA.setPosition(currPoint);
			onRA.setPosition(currPoint);
			enRA.setPosition(currPoint);
			
			cnC = cnRA.get().cursor();
			wnC = wnRA.get().cursor();
			dnC = dnRA.get().cursor();
			onC = onRA.get().cursor();
			enC = enRA.get().cursor();
			

			//mark current position as processed, i.e. make cost negative
			nValVox=ccostRA.get().get();
			ccostRA.get().set(nValVox*(-1));

			//get current orientation vector
			for(d=0;d<dim.length;d++)
			{
				currentOrient[d] = orientRA.get().get(d).get();
			}
			
			
			//iterate through the neighborhood
			//(direction count starts from 1)
			nDir=0;
			while ( cnC.hasNext() )
			{
				nDir++;
				cnC.fwd();
				wnC.fwd();
				dnC.fwd();
				onC.fwd();
				enC.fwd();

				iCurCCost = cnC.get().get();
				if(iCurCCost>=0)
				{
					//check if it is an endpoint
					if(iCurCCost==END_POINT)
					{
						
						dnC.get().set(nDir);
						ccostRA.setPosition(iniPoint);
						ccostRA.get().setZero();
						dirsRA.setPosition(iniPoint);
						//middle of neighborhood
						dirsRA.get().set(ININODE);
						System.out.println("Dijkstra not full");
						return true;
					}
					//check if it is "emptyness"
					nW = wnC.get().get();
					if (nW==0)
					{
						cnC.get().set(-1);
						dnC.get().set(BORDER_ADD+nDir);
					}
					else
					{
						
						//get orientation vector
						for(d=0;d<dim.length;d++)
						{
							neighbourOrient[d] = onC.get().get(d).get();
						}
						//calculate difference in orientation
						angleCost = getAngleCost(currentOrient, neighbourOrient,nDir-1);
						
						
						iNewCCost = nValVox + (int)(Math.round((1.0-orientationWeight)*(255 - nW))+Math.round(orientationWeight*255.0*angleCost));
						
						//old cost
						//iNewCCost = nValVox + (255 - nW);
						
						//new element or element with reduced cost
						if (iNewCCost < iCurCCost || iCurCCost==0)
						{
							//update weight + direction
							cnC.get().set(iNewCCost);
							dnC.get().set(nDir);
														
							//new record, add element to the queue
							if(iCurCCost==0)
							{
								entries.add(queue.enqueue(nEnQ, iNewCCost));
								enC.get().set(nEnQ);
								cnC.localize(pos[nEnQ]);
								//vals_test[nEnQ]=iNewCCost;
								nEnQ++;
							}
							//this pixel we visited and new cost is lower
							else
							{
								//cnC.localize(testloc);
								//int indRec = enC.get().get();								
								//queue.decreaseKey(entries.get(indRec), iNewCCost);
								//vals_test[indRec]=iNewCCost;
								queue.decreaseKey(entries.get(enC.get().get()), iNewCCost);

							}
							
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
				//int valZZ = queue.dequeueMin().getValue();
				currPoint=pos[queue.dequeueMin().getValue()];
				if(nMaxQ>queue.size())
				{
					nMaxQ=queue.size();
				}
				//System.out.println("queue size="+ Integer.toString(queue.size()));
			}

	
		}//queue end 
		System.out.println("max queue:"+Integer.toString(nMaxQ));

		//mark initial node with zero cost
		ccostRA.setPosition(iniPoint);
		ccostRA.get().setZero();
		dirsRA.setPosition(iniPoint);
		//middle of neighborhood
		dirsRA.get().set(ININODE);
		return false;
		
	}

	/** function calculates cost of directionality between two voxels **/
	double getAngleCost(double [] currentPoint, double [] candidatePoint, int nDir)
	{
		double currentPointCost = 1.0-Math.abs(LinAlgHelpers.dot(currentPoint, neibVectors[nDir]));
		double candidatePointCost = 1.0-Math.abs(LinAlgHelpers.dot(candidatePoint, neibVectors[nDir]));
		
		return 0.5*(Math.sqrt(currentPointCost)+Math.sqrt(candidatePointCost));
	}
	
	public boolean getTrace(final RealPoint click, final ArrayList<RealPoint> finSegment)
	{
		RealPoint currRP = new RealPoint(click);
		int i, nDir;
		//float [][] neibIndexes = new float [26][3];
		//float [] currV = new float[3];
		//long [] currPoint =new long [dim.length];
		long [] endPoint = new long [dim.length];
		iniPoint = new long [dim.length];
		for (i =0;i<dim.length; i++)
		{
			endPoint[i]=(long)Math.round(click.getFloatPosition(i));
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
		//so we go from the beginning to the end
		Collections.reverse(finSegment);
		//click.setPosition(currRP);
		return true;
	}
	
	void getNeighboursIndexesVectors()
	{
		float [] currV = new float[3];
		int i;
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes(1, 1, 1 );
		final RandomAccessible< Neighborhood< UnsignedByteType > > neighborhoods = voxShape.neighborhoodsRandomAccessible(Views.extendZero( img) );
		final RandomAccess< Neighborhood< UnsignedByteType > > na = neighborhoods.randomAccess();
		na.setPosition( new int[] { 0, 0, 0 } );
		
		final Neighborhood< UnsignedByteType > neighborhood = na.get();

		Cursor< UnsignedByteType > c = neighborhood.cursor();
		int nCount = 0;
		double length;
		while ( c.hasNext() )
		{
			c.fwd();
			c.localize(currV);
			length=0;
			for (i=0;i<3;i++)
			{
				neibIndexes[nCount][i]=-currV[i];
				neibVectors[nCount][i]= currV[i];
				length+=currV[i]*currV[i];
			}
			length=Math.sqrt(length);
			for (i=0;i<3;i++)
			{
				neibVectors[nCount][i]/=length;
			}
			nCount++;
		}
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

}

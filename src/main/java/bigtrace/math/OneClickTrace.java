package bigtrace.math;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.volume.VolumeMisc;
import btbvv.util.Bvv;
import btbvv.util.BvvFunctions;
import net.imglib2.AbstractInterval;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class OneClickTrace < T extends RealType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	public BigTrace<T> bt;
	public IntervalView<T> fullInput; 
	public RealPoint startPoint;
	public RealPoint currentVector;
	public long [] boxHalfRange;
	public long[] dim;
	public double rangeBox = 3.0;
	
	private HashMap<String, ArrayList<int[]>> neighborsMap = new HashMap<String, ArrayList<int[]>>();
	
	
	Convolution [] convObjects = new Convolution[6];
	ExecutorService es;// = Executors.newFixedThreadPool( nThreads );
	int nThreads;
	
	/** eigenvectors container **/
	ArrayImg<FloatType, FloatArray> dV;
	/** weights (saliency) container **/
	ArrayImg<FloatType, FloatArray> sW;
	
	IntervalView<FloatType> directionVectors;
	IntervalView<FloatType> salWeights;
	IntervalView<UnsignedByteType> salWeightsUB;
	
	ArrayImg<FloatType, FloatArray> gradFloat;
	ArrayImg<FloatType, FloatArray> hessFloat;
	
	private String progressState;
	
	public String getProgressState()
	{
		return progressState;
	}
	public void setProgressState(String state_)
	{
		progressState=state_;
	}
	@Override
	protected Void doInBackground() throws Exception {
	
		RealPoint nextPoint;

		int nCountPoints = 0;
		ArrayList<RealPoint> points = new ArrayList<RealPoint>();
		init();
		
		long start1, end1;
		start1 = System.currentTimeMillis();
		
		
		getMathForCurrentPoint(startPoint);
		
		points.add(refinePointUsingSaliency(startPoint));
		
		//bt.roiManager.addPoint3D(points.get(0));
		nCountPoints++;
		nextPoint = getNextPoint(points.get(0));
		int testCount = 0;
		int nCountReset = 3;
		int ptCount = nCountReset-1;
		while (nextPoint != null)
		//while (nextPoint != null && testCount<100)
		{
			points.add(new RealPoint(nextPoint));
			nCountPoints++;
			//bt.roiManager.addPoint3D(points.get(nCountPoints-1));
			ptCount--;
			if(ptCount ==0)
			{
				getMathForCurrentPoint(points.get(nCountPoints-1));
				ptCount = nCountReset;
			}
			nextPoint = getNextPoint(points.get(nCountPoints-1));
			testCount++;
			//setProgress(100);
			setProgressState(Integer.toString(testCount)+"points found.");
		}
		end1 = System.currentTimeMillis();
		System.out.println("THREADED Elapsed Time in seconds: "+ 0.001*(end1-start1));
		
		//show it
		System.out.println("YOHOOO!");
		bt.roiManager.addSegment(points.get(0),null);
		bt.roiManager.addSegment(points.get(points.size()-1), points);

		return null;
	}
	
	public void init()
	{
		boxHalfRange = new long[3];
		
		for (int d=0;d<3;d++)
		{
			boxHalfRange[d]=(long) (Math.ceil(rangeBox*bt.btdata.sigmaTrace[d])); 
			
		}
		IntervalView<T> input = Views.interval(fullInput, getLocalTraceBox(fullInput,boxHalfRange,startPoint));
		dim = Intervals.dimensionsAsLongArray( input );
		
		//gradient, first derivatives, 3 values per voxel
		//gradFloat = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );

		//hessian, second derivatives, 6 values per voxel
		hessFloat = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 6 );
		
		dV = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );
		sW = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ]);
		fillNeighborsHashMap();
		
		nThreads = Runtime.getRuntime().availableProcessors();
		es = Executors.newFixedThreadPool( nThreads );

		int count = 0;
		int d1,d2;
		double [][] kernels;
		Kernel1D[] derivKernel;
		int [] nDerivOrder;
		for (d1=0;d1<3;d1++)
		{
			for (d2 = d1; d2 < 3; d2++ )
			{

				nDerivOrder = new int [3];
				nDerivOrder[d1]++;
				nDerivOrder[d2]++;
				kernels = DerivConvolutionKernels.convolve_derive_kernel(bt.btdata.sigmaTrace, nDerivOrder);
				derivKernel = Kernel1D.centralAsymmetric(kernels);
				convObjects[count] = SeparableKernelConvolution.convolution( derivKernel );
				convObjects[count].setExecutor(es);
				count++;
			}
		}
	}
	
	public RealPoint getNextPoint(RealPoint currpoint)
	{
		RealPoint out = null;
		int i,d;
		//let's get the direction vector
		RandomAccess<FloatType> raV = directionVectors.randomAccess();
		//RandomAccess<UnsignedByteType> raW = salWeightsUB.randomAccess();
		RandomAccess<FloatType> raW = salWeights.randomAccess();
		
		
		//double [] currDirVector = getDirectionVector(currpoint,raV);
		
		double [] currDirVector = new double[3];
		long [] currpos = new long [4];

		for (d=0;d<3;d++)
		{
			currpos[d]=Math.round(currpoint.getFloatPosition(d));
		}
		
		for(d=0;d<3;d++)
		{
			currpos[3]=d;
			raV.setPosition(currpos);
			currDirVector[d] = raV.get().getRealDouble();
		}
		
	
		
		int [] currNeighbor = new int[3];
		String currNeighborHash="";
		for (d=0;d<3;d++)
		{
			currNeighbor[d]=(int)Math.round(currDirVector[d]);
			currNeighborHash=currNeighborHash+Integer.toString(currNeighbor[d]);
		}
		//get all relevant neighbors
		//int maxSal = 0;
		//int currSal;
		float maxSal = (-1)*Float.MAX_VALUE;
		float currSal = (-1)*Float.MAX_VALUE;
		ArrayList<int[]> scannedPos = neighborsMap.get(currNeighborHash);
		int [] candidateNeighbor; 

		long[] candPos = new long[3];
		float[] finPos = new float[3];
		for(int nScan=0;nScan<scannedPos.size();nScan++)
		{
			candidateNeighbor=scannedPos.get(nScan);
			for(d=0;d<3;d++)
			{
				candPos[d]=currpos[d]+candidateNeighbor[d];
			}
			if(Intervals.contains(fullInput, new RealPoint(new float []{candPos[0],candPos[1],candPos[2]})))
			{
				currSal=raW.setPositionAndGet(candPos).get();
				if(currSal>0)
				{
					if(currSal>maxSal)
					{
						maxSal=currSal;
						for(i=0;i<3;i++)
						{
							finPos[i] = candPos[i];
						}
					}
				}
			}
		}
		//System.out.println(maxSal);
		if(maxSal>0.000001)
		{
			out = new RealPoint(finPos);
		}
		else
		{
			out = null;
		}

		return out;
		
	}

	/** calculate hessian/eigen vectors around current point **/
	public void getMathForCurrentPoint(final RealPoint currPoint)
	{
		//let's figure out the volume around the point
		IntervalView<T> currentBox = Views.interval(fullInput, getLocalTraceBox(fullInput,boxHalfRange,currPoint));
		int i;
		long[] minV = currentBox.minAsLongArray();
		long[] nShift = new long [currentBox.numDimensions()+1];
		
		for (i=0;i<currentBox.numDimensions();i++)
		{
			nShift[i]=minV[i];
		}
		
		//IntervalView<FloatType> gradient = Views.translate(gradFloat, nShift);
		IntervalView<FloatType> hessian = Views.translate(hessFloat, nShift);
		
		

	/*
		double [][] kernels;
		Kernel1D[] derivKernel;
		int [] nDerivOrder;
		Convolution convObj;
		//start1 = System.currentTimeMillis();
		final int nThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		//setProgress(0);
		/*
		//first derivatives
		for (int d1=0;d1<3;d1++)
		{
			//current coordinate
			IntervalView< FloatType > gd2 = Views.hyperSlice( gradient, 3, d1 );
			nDerivOrder = new int [3];
			nDerivOrder[d1]++;
			kernels = DerivConvolutionKernels.convolve_derive_kernel(bt.btdata.sigmaTrace, nDerivOrder);
			derivKernel = Kernel1D.centralAsymmetric(kernels);
			convObj = SeparableKernelConvolution.convolution( derivKernel );
			convObj.setExecutor(es);
			convObj.process(Views.extendBorder(currentBox), gd2 );
		}
		*/
		
		//second derivatives
		int count = 0;
		int d1,d2;
		for (d1=0;d1<3;d1++)
		{
			for (d2 = d1; d2 < 3; d2++ )
			{
				IntervalView< FloatType > hs2 = Views.hyperSlice( hessian, 3, count );
				convObjects[count].process(Views.extendBorder(currentBox), hs2 );
				count++;
			}
		}


		EigenValVecSymmDecomposition<FloatType> mEV = new EigenValVecSymmDecomposition<FloatType>(3);

		directionVectors =  Views.translate(dV, nShift);
		salWeights =  Views.translate(sW, minV);
		salWeightsUB = VolumeMisc.convertFloatToUnsignedByte(salWeights,false);
		mEV.computeVWRAI(hessian, directionVectors, salWeights, nThreads, es);
		
	}
	
	/** gets a box around "target" extending in each side according to range in each axis.
	crops the box so it is inside viewclick interval **/
	public FinalInterval getLocalTraceBox(final AbstractInterval fullInterval, final long [] range, final RealPoint target)
	{
		long[][] rangeM = new long[2][3];

		for(int d=0;d<3;d++)
		{
			rangeM[0][d]=(long)(target.getDoublePosition(d))-range[d] ;
			rangeM[1][d]=(long)(target.getDoublePosition(d))+range[d];								
		}
		VolumeMisc.checkBoxInside(fullInterval, rangeM);
		FinalInterval finInt = new FinalInterval(rangeM[0],rangeM[1]);
		return finInt;							
	}
	
	/** returns local maximum of saliency (using trace_weights) 
	 * around target_in point. The search box is equal to SD of tracing x2 **/
	RealPoint refinePointUsingSaliency(RealPoint target_in)
	{
		long[][] rangeMax = new long[2][3];
		float dSDN = 2.0f;
		for(int d=0;d<3;d++)
		{
			rangeMax[0][d] = Math.round(target_in.getFloatPosition(d)-dSDN*bt.btdata.sigmaTrace[d]);
			rangeMax[1][d] = Math.round(target_in.getFloatPosition(d)+dSDN*bt.btdata.sigmaTrace[d]);
		}
		//get an box around the target
		FinalInterval maxSearchArea = new FinalInterval(rangeMax[0],rangeMax[1]);
		
		RealPoint out = new RealPoint(3);
		
		VolumeMisc.findMaxLocation(Views.interval(Views.extendZero(salWeights), maxSearchArea),out);
		return out;
		
	}
	//for each voxel in the 26 neighborhood
	//it maps adjacent voxels
	void fillNeighborsHashMap()
	{
		int nCount = 0;
		for (int d1=-1;d1<2;d1++)
		{
			for ( int d2 = -1; d2 < 2; d2++ )
			{
				for ( int d3 = -1; d3 < 2; d3++ )
				{
					//let's determine "neighbor type"
					int nType = Math.abs(d1)+Math.abs(d2)+Math.abs(d3);
					//remove central pixel
					if(nType!=0)
					{
						//let's make a hash string
						String sKey = Integer.toString(d1)+Integer.toString(d2)+Integer.toString(d3);
						ArrayList<int[]> around =new ArrayList<int[]>();
						//System.out.print(sKey);
						
						nCount++;	

						//iterating around the neighborhood of each pixel
						for (int m1=-1;m1<2;m1++)
						{
							for ( int m2 = -1; m2 < 2; m2++ )
							{
								for ( int m3 = -1; m3 < 2; m3++ )
								{
									int [] nb = new int [3];
									//"face cross" neighbor
									if(nType == 1)
									{
										nb[0] = m1+d1*2;
										nb[1] = m2+d2*2;
										nb[2] = m3+d3*2;
									}
									else
									{
										nb[0] = m1+d1;
										nb[1] = m2+d2;
										nb[2] = m3+d3;								
									}
									//check if it belongs to the original neighborguud
									
									if(Math.abs(nb[0])<2 && Math.abs(nb[1])<2 && Math.abs(nb[2])<2)
									{
										switch(nType)
										{
											//face, central neighbor
											case 1:
												around.add(nb);
												break;
											//neighbor and the middle of the edge
											case 2:
												//(let's remove central pixel)
												if(Math.abs(nb[0])+Math.abs(nb[1])+Math.abs(nb[2])!=0)
												{
													//remove sides
													if(Math.abs(m1)+Math.abs(m2)+Math.abs(m3)<3)
													{
														around.add(nb);
													}
												}
												break;
											//corner neighbor (let's remove central pixel)
											case 3:
												if(Math.abs(nb[0])+Math.abs(nb[1])+Math.abs(nb[2])!=0)
												{
													around.add(nb);
												}
												break;
											
										}
											
									}
								}
							}
						}
						neighborsMap.put(sKey, around);
						//System.out.println(" "+around.size()+" type"+Integer.toString(nType));
					}
				}
			}
		}
		//System.out.println(nCount);
		
//		ArrayList<int[]> test = neighborsMap.get("011");
//		for(int d=0;d<test.size();d++)
//		{
//			int[] vals=test.get(d);
//			System.out.println(Integer.toString(vals[0])+Integer.toString(vals[1])+Integer.toString(vals[2]));
//		}
		
		
	}
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() 
    {
    	//see if we have some errors
    	 try {
             get();
         	} 
    	 catch (ExecutionException e) {
             e.getCause().printStackTrace();
             String msg = String.format("Unexpected problem during Hessian calculations: %s", 
                            e.getCause().toString());
             System.out.println(msg);
         } catch (InterruptedException e) {
             // Process e here
         }
    	 es.shutdown();
    	//bt.showTraceBox();
    	bt.setTraceBoxMode(false);
    	// bvv_trace = BvvFunctions.show(btdata.trace_weights, "weights", Bvv.options().addTo(bvv_main));
		//unlock user interaction
    	bt.bInputLock = false;
    }
    
    /*
    public static void main(String[] args) 
    {
    	OneClickTrace test =new OneClickTrace();
    	test.fillNeighborsHashMap();
   
    }
    */
	/*
	//show gradient absolute value in salWeights
	Cursor<FloatType> cu = salWeights.cursor();
	while(cu.hasNext())
	{
		cu.next().set(0.0f);
	}

	/// let's make a gradient image
	RandomAccess<FloatType> rasW = salWeights.randomAccess();
	cu = gradient.localizingCursor();

	long [] pos = new long[3];
	float zz;
	while(cu.hasNext())
	{
		cu.fwd();
		for(i=0;i<3;i++)
		{
			pos[i]=cu.getLongPosition(i);
		}
		rasW.setPosition(pos);
		//if(cu.getLongPosition(3)==1)
		//{
			zz = cu.get().getRealFloat();
			zz=rasW.get().getRealFloat()+zz*zz;
			rasW.get().set(zz);
		//}
	}
	//square roor
	cu = salWeights.cursor();
	cu.reset();
	while(cu.hasNext())
	{
		cu.fwd();
		zz=cu.get().getRealFloat();
		cu.get().set((float)Math.sqrt(zz));
	}
*/


}

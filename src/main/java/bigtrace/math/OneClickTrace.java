package bigtrace.math;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.volume.VolumeMisc;
import bigtrace.rois.Box3D;
import net.imglib2.AbstractInterval;
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
	/** full dataset to trace **/
	public IntervalView<T> fullInput; 
	/** if a tracing leaving this box,
	 * new full box is recalculated at the current location **/
	public FinalInterval innerTraceBox; 
	public RealPoint startPoint;
	public RealPoint currentVector;
	public long [] boxFullHalfRange;
	public long [] boxInnerHalfRange;

	/** dimensions of the box where saliency + vectors 
	 * will be calculated in rangeFullBoxDim * sigma of the axis **/
	final double rangeFullBoxDim = 3.0;
	/** dimensions of the box where tracing will happen
	 	(in sigmas) before new box needs to be calculated**/
	final double rangeInnerBoxDim = 1.0;
	
	long [] minV;
	
	public double dAngleThreshold;// = 0.8;
	
	int nNeighborsMethods = 0;
	int nPointPerSegment;
	
	
	int [][] nNeighborsIndexes = new int[26][3];
	double [][] nNeighborsVectors = new double[26][3];
	
	private HashMap<String, ArrayList<int[]>> neighborsMap = new HashMap<String, ArrayList<int[]>>();
	double [] lastDirectionVector;
	
	Convolution [] convObjects = new Convolution[6];
	ExecutorService es;// = Executors.newFixedThreadPool( nThreads );
	int nThreads;
	
	//boolean bPrint = false;
	
	/** eigenvectors container **/
	ArrayImg<FloatType, FloatArray> dV;
	/** weights (saliency) container **/
	ArrayImg<FloatType, FloatArray> sW;
	
	IntervalView<FloatType> directionVectors;
	IntervalView<FloatType> salWeights;
	RandomAccess<FloatType> raV;
	RandomAccess<FloatType> raW;

	
	//IntervalView<UnsignedByteType> salWeightsUB;
	
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
	
		//RealPoint nextPoint;
		//RealPoint startPointRefined;

		//int nCountPoints = 0;
		

		setProgress(0);
		init();
		

		long start1, end1;
		start1 = System.currentTimeMillis();
		
		//init math
		getMathForCurrentPoint(startPoint);
		startPoint=refinePointUsingSaliency(startPoint);
		
		double [] startDirectionVector = getVectorAtLocation(startPoint);
		lastDirectionVector = new double [3];
		for (int d=0; d<3; d++)
		{
			lastDirectionVector[d]=startDirectionVector[d];
		}
	
		//trace in one direction
		int nTotPoints = traceOneDirection(true, 0);
		setProgress(50);
		setProgressState(Integer.toString(nTotPoints)+"points found.");
		//look for another direction
		for (int d=0; d<3; d++)
		{
			lastDirectionVector[d]=(-1)*startDirectionVector[d];
		}
		//reverse ROI
		bt.roiManager.rois.get(bt.roiManager.activeRoi).reversePoints();
		//init math
		getMathForCurrentPoint(startPoint);
		nTotPoints = traceOneDirection(false, nTotPoints);
		
		end1 = System.currentTimeMillis();
		System.out.println("THREADED Elapsed Time in seconds: "+ 0.001*(end1-start1));
		
		//show it
		//System.out.println("YOHOOO!");

		setProgress(100);
		setProgressState("trace with "+Integer.toString(nTotPoints)+" points. auto-tracing done.");

		return null;
	}
	
	
	public int traceOneDirection(boolean bFirstTrace, int nCountIn)
	{
		
		ArrayList<RealPoint> points = new ArrayList<RealPoint>();
		
		RealPoint nextPoint;
		
		int nCountPoints=0;
		points.add(startPoint);
		if(bFirstTrace)
		{
			bt.roiManager.addSegment(points.get(0),null);
		}
		else
		{
			nCountPoints = nCountIn;
		}
		
		nextPoint = getNextPoint(points.get(0));
		
		//points inside the current math box
		while (nextPoint != null)
		//while (nextPoint != null && testCount<100)
		{
			if(points.size()==nPointPerSegment)
			{
				bt.roiManager.addSegment(points.get(points.size()-1), points);
				points = new ArrayList<RealPoint>();
				setProgressState(Integer.toString(nCountPoints)+"points found.");
				if(bFirstTrace)
				{
					setProgress(20);
				}
				else
				{
					setProgress(70);
				}
				
				try {
					Thread.sleep(1);
				} catch (InterruptedException ignore) {}
			}
			points.add(new RealPoint(nextPoint));
			nCountPoints++;
			//see if we are still inside reasonable 
			//values for math box
			if(!Intervals.contains(innerTraceBox, points.get(points.size()-1)))
			{
				getMathForCurrentPoint(points.get(points.size()-1));
			}

			/*
			if(nCountPoints==73)
			{
				int d=0;
				d=10;
			}*/
			nextPoint = getNextPoint(points.get(points.size()-1));
			if(nextPoint!=null)
			{
				double [] nextPointD = nextPoint.positionAsDoubleArray();
				double [] prevPointD = points.get(points.size()-1).positionAsDoubleArray();
				LinAlgHelpers.subtract(nextPointD, prevPointD, nextPointD);
				LinAlgHelpers.scale(nextPointD, 1.0/LinAlgHelpers.length(nextPointD), nextPointD);
				
				//System.out.println(LinAlgHelpers.length(saveVector)+" "+LinAlgHelpers.length(nextPointD)+" "+LinAlgHelpers.dot(saveVector, nextPointD));
				//System.out.println(LinAlgHelpers.dot(saveVector, nextPointD));
				//System.out.println(nCountPoints+" "+LinAlgHelpers.dot(saveVector, nextPointD));
			}
			//setProgress(100);

		}
		bt.roiManager.addSegment(points.get(points.size()-1), points);
		
		return nCountPoints;
	}
	
	public void init()
	{
		
		nPointPerSegment = bt.btdata.nVertexPlacementPointN;
		dAngleThreshold = bt.btdata.dDirectionalityOneClick;
		
		//nCountReset = Math.max(Math.max(bt.btdata.sigmaTrace[0], bt.btdata.sigmaTrace[1]),bt.btdata.sigmaTrace[2]);
		boxFullHalfRange = new long[3];
		boxInnerHalfRange = new long[3];
		long [] boxFullRange = new long[3];

		
		for (int d=0;d<3;d++)
		{
			boxFullHalfRange[d]=(long) (Math.ceil(rangeFullBoxDim*bt.btdata.sigmaTrace[d])); 
			boxInnerHalfRange[d]=(long) (Math.ceil(rangeInnerBoxDim*bt.btdata.sigmaTrace[d])); 
			boxFullRange[d] = (long) (Math.ceil(rangeFullBoxDim*bt.btdata.sigmaTrace[d])*2+1); 		
		}
		//IntervalView<T> input = Views.interval(fullInput, getLocalTraceBox(fullInput,boxFullHalfRange,startPoint));
		//dim = Intervals.dimensionsAsLongArray( input );
		
		//gradient, first derivatives, 3 values per voxel
		//gradFloat = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );

		//hessian, second derivatives, 6 values per voxel
		//hessFloat = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 6 );
		//dV = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );
		//sW = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ]);
		minV = new long [4];

		hessFloat = ArrayImgs.floats( boxFullRange[ 0 ], boxFullRange[ 1 ], boxFullRange[ 2 ], 6 );
		dV = ArrayImgs.floats( boxFullRange[ 0 ], boxFullRange[ 1 ], boxFullRange[ 2 ], 3 );
		sW = ArrayImgs.floats( boxFullRange[ 0 ], boxFullRange[ 1 ], boxFullRange[ 2 ]);

		
		initNeighborsHashMap();
		//initNeighbors();
		
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
		double [] newDirection = null;
		double [] candDirection;
	
		//find a pixel in the neighborhood 
		//according to direction vector
		int [] currNeighbor = new int[3];
		String currNeighborHash="";
		for (d=0;d<3;d++)
		{
			currNeighbor[d]=(int)Math.round(lastDirectionVector[d]);
			currNeighborHash=currNeighborHash+Integer.toString(currNeighbor[d]);
		}
		//get all relevant neighbors
		float maxSal = (-1)*Float.MAX_VALUE;
		float currSal = (-1)*Float.MAX_VALUE;
		//double [] candVector;
		ArrayList<int[]> scannedPos;
		if(nNeighborsMethods==0)
		{
			scannedPos = neighborsMap.get(currNeighborHash);
		}
		else
		{
			scannedPos = getNeighborPixels(lastDirectionVector);
		}
		int [] candidateNeighbor; 

		long[] candPos = new long[3];
		float[] finPos = new float[3];
		for(int nScan=0;nScan<scannedPos.size();nScan++)
		{
			candidateNeighbor=scannedPos.get(nScan);
			for(d=0;d<3;d++)
			{
				candPos[d]=Math.round(currpoint.getFloatPosition(d))+candidateNeighbor[d];
			}
		
			if(Intervals.contains(fullInput, new RealPoint(new float []{candPos[0],candPos[1],candPos[2]})))
			{
				currSal=raW.setPositionAndGet(candPos).get();
				candDirection = getVectorAtLocation(candPos);
//				if(bPrint)
//				{
//					//System.out.println(candidateNeighbor[0]+"\t"+candidateNeighbor[1]+"\t"+candidateNeighbor[2]+"\t"+currSal);
//					System.out.println(currSal);
//				}
//				double [] dirX = new double [3];
//				for (int zz=0;zz<2;zz++)
//				{
//					dirX[zz] = (double)candidateNeighbor[zz];
//				}
//				LinAlgHelpers.normalize(dirX);
//				currSal *= LinAlgHelpers.dot(dirX, lastDirectionVector);
				
				if(currSal>0)
				{
					//double valS = Math.abs(LinAlgHelpers.dot(candDirection, lastDirectionVector));
					//if(Math.abs(LinAlgHelpers.dot(candDirection, hystVector))<dAngleThreshold)
					if(Math.abs(LinAlgHelpers.dot(candDirection, lastDirectionVector))<dAngleThreshold)
					{
						currSal=0.0f;
					}
					if(currSal>maxSal)
					{
						maxSal=currSal;
						for(i=0;i<3;i++)
						{
							finPos[i] = candPos[i];
						}
						newDirection=candDirection;
					}
				}
			}
		}
		//System.out.println(maxSal);
		if(maxSal>0.000001)
		{
			// newDirection = getVectorAtLocation(new RealPoint(finPos));
			double dCos = LinAlgHelpers.dot(newDirection, lastDirectionVector);
			//System.out.println(dCos);
			if(dCos>0)
			//if(LinAlgHelpers.dot(newDirection, lastDirectionVector)>0)
			{
				lastDirectionVector = newDirection;
			}
			else
			{
				LinAlgHelpers.scale(newDirection,-1.0,lastDirectionVector);
			}
			out = new RealPoint(finPos);
//			if(Math.abs(out.getDoublePosition(0)-49)+Math.abs(out.getDoublePosition(1)-48)<0.001)
//			{
//				System.out.println("Found deviation point");
//				bPrint = true;
//				
//			}
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
		IntervalView<T> currentBox = Views.interval(fullInput, getLocalTraceBox(fullInput,boxFullHalfRange,currPoint));
		innerTraceBox = getLocalTraceBox(fullInput,boxInnerHalfRange,currPoint);
		//long[] minV = new long [currentBox.numDimensions()+1];
		//minV = new long [currentBox.numDimensions()+1];
		currentBox.min(minV);
		
		//IntervalView<FloatType> gradient = Views.translate(gradFloat, nShift);
		IntervalView<FloatType> hessian = Views.translate(hessFloat, minV);
		
		bt.visBox= new Box3D(currentBox,0.5f,0.0f,Color.LIGHT_GRAY,Color.LIGHT_GRAY, 0);

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
				//FinalInterval test = (FinalInterval) convObjects[count].requiredSourceInterval(hs2);
				convObjects[count].process(Views.extendBorder(currentBox), hs2 );
				count++;
			}
		}


		EigenValVecSymmDecomposition<FloatType> mEV = new EigenValVecSymmDecomposition<FloatType>(3);

		directionVectors =  Views.translate(dV, minV);
		salWeights =  Views.translate(sW, minV[0],minV[1],minV[2]);
		//salWeightsUB = VolumeMisc.convertFloatToUnsignedByte(salWeights,false);
		mEV.computeVWRAI(hessian, directionVectors, salWeights, nThreads, es);
		raV = directionVectors.randomAccess();
		raW = salWeights.randomAccess();
		//calculate inner box
		
		
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
	public double[] getVectorAtLocation(long[] point)
	{
		RealPoint out = new RealPoint(3);
		//for(int d=0;d<3;d++)
		//{
			out.setPosition(point);
			return getVectorAtLocation(out);
		//}
		
	}
	public double[] getVectorAtLocation(RealPoint point)
	{
		int d;
		double [] currDirVector = new double[3];
		long [] currpos = new long [4];

		for (d=0;d<3;d++)
		{
			currpos[d]=Math.round(point.getFloatPosition(d));
		}
		
		for(d=0;d<3;d++)
		{
			currpos[3]=d;
			raV.setPosition(currpos);
			currDirVector[d] = raV.get().getRealDouble();
		}
		return currDirVector;
	}
	
	ArrayList<int[]> getNeighborPixels(double [] directionVector)
	{
		ArrayList<int[]> out = new ArrayList<int[]>();
		for(int i=0;i<26;i++)
		{
			if(LinAlgHelpers.dot(directionVector, nNeighborsVectors[i])>0.5)
			{
				out.add(nNeighborsIndexes[i]);
			}
		}
		return out;

	}
	
	void initNeighbors()
	{
		int nCount = 0;
		int [] dx = new int[3];
		for (dx[0]=-1; dx[0]<2; dx[0]++)
		{
			for (dx[1]=-1; dx[1]<2; dx[1]++)
			{
				for (dx[2]=-1; dx[2]<2; dx[2]++)
				{
					int nType = Math.abs(dx[0])+Math.abs(dx[1])+Math.abs(dx[2]);
					//remove central pixel
					if(nType!=0)
					{
						nNeighborsIndexes[nCount]= new int[3];
						nNeighborsVectors[nCount]= new double[3];
						for(int d=0;d<3;d++)
						{
							nNeighborsIndexes[nCount][d]= dx[d];
							nNeighborsVectors[nCount][d]= (double)dx[d];
							LinAlgHelpers.normalize(nNeighborsVectors[nCount]);
						}
						nCount++;
						
					}
				}
			}
		}
		
	}
	
	/** for each voxel in the 26 neighborhood
	 * it maps adjacent voxels
	 */
	void initNeighborsHashMap()
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
		/*
		ArrayList<int[]> test = neighborsMap.get("0-11");
		for(int d=0;d<test.size();d++)
		{
			int[] vals=test.get(d);
			//System.out.println(Integer.toString(vals[0])+Integer.toString(vals[1])+Integer.toString(vals[2]));
			double [] origvector = new double[3];
			origvector[0] = 0.0;
			origvector[1] = -1.0;
			origvector[2] = 1.0;
			LinAlgHelpers.normalize(origvector);
			double [] neighbor = new double [3];
			for(int i=0;i<3;i++)
			{
				neighbor[i] = vals[i];
			}
			LinAlgHelpers.normalize(neighbor);
			//LinAlgHelpers.scale(neighbor, LinAlgHelpers.length(neighbor), neighbor);
			System.out.println(LinAlgHelpers.dot(origvector, neighbor));
			
		}
		*/
		
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
    	//bt.setTraceBoxMode(false);
    	bt.visBox = null;
    	bt.roiManager.setLockMode(false);
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
	//square root
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

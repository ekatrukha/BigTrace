package bigtrace.math;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker;

import bigtrace.volume.VolumeMisc;
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.SeparableKernelConvolution;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class TraceBoxMath extends SwingWorker<Void, Void> 
{
	public IntervalView<UnsignedByteType> input; 
	IntervalView< UnsignedByteType > trace_weights = null;
	IntervalView< FloatType > trace_vectors=null;
	ArrayList<long []> jump_points = null;
	public double sigma;
	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		int i;
		double [][] kernels;
		Kernel1D[] derivKernel;
		final long[] dim = Intervals.dimensionsAsLongArray( input );
		long[] minV = input.minAsLongArray();
		long[] nShift = new long [input.numDimensions()+1];
		
		for (i=0;i<input.numDimensions();i++)
		{
			nShift[i]=minV[i];
		}
		
		ArrayImg<FloatType, FloatArray> hessFloat = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 6 );
		IntervalView<FloatType> hessian = Views.translate(hessFloat, nShift);
		
	
		//ArrayImg<FloatType, FloatArray> gradient = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );
		
		//long start1, end1;
		
		
		int count = 0;
		int [] nDerivOrder;
		/**/
		Convolution convObj;
		//start1 = System.currentTimeMillis();
		final int nThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService es = Executors.newFixedThreadPool( nThreads );
		setProgress(0);
		//second derivatives
		for (int d1=0;d1<3;d1++)
		{
			for ( int d2 = d1; d2 < 3; d2++ )
			{
				IntervalView< FloatType > hs2 = Views.hyperSlice( hessian, 3, count );
				nDerivOrder = new int [3];
				nDerivOrder[d1]++;
				nDerivOrder[d2]++;
				kernels = DerivConvolutionKernels.convolve_derive_kernel(sigma, nDerivOrder );
				derivKernel = Kernel1D.centralAsymmetric(kernels);
				convObj=SeparableKernelConvolution.convolution( derivKernel );
				convObj.setExecutor(es);
				convObj.process(Views.extendBorder(input), hs2 );
				//SeparableKernelConvolution.convolution( derivKernel ).process( input, hs2 );
				count++;
				System.out.println(count);
				  //Sleep for up to one second.
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ignore) {}
                //Make random progress.
               // progress += random.nextInt(10);
                setProgress(count*100/6);
			}
		}
		//end1 = System.currentTimeMillis();
		//System.out.println("THREADED Elapsed Time in milli seconds: "+ (end1-start1));

		EigenValVecSymmDecomposition<FloatType> mEV = new EigenValVecSymmDecomposition<FloatType>(3);
		ArrayImg<FloatType, FloatArray> dV = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ], 3 );
		ArrayImg<FloatType, FloatArray> sW = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ]);
		ArrayImg<FloatType, FloatArray> nC = ArrayImgs.floats( dim[ 0 ], dim[ 1 ], dim[ 2 ]);
		IntervalView<FloatType> directionVectors =  Views.translate(dV, nShift);
		IntervalView<FloatType> salWeights =  Views.translate(sW, minV);
		IntervalView<FloatType> lineCorners =  Views.translate(nC, minV);
		
		mEV.computeVWCRAI(hessian, directionVectors,salWeights, lineCorners,nThreads,es);
		es.shutdown();
		trace_weights=VolumeMisc.convertFloatToUnsignedByte(salWeights,false);
		jump_points =VolumeMisc.localMaxPointList(VolumeMisc.convertFloatToUnsignedByte(lineCorners,false), 10);
		
		return null;
	}

}

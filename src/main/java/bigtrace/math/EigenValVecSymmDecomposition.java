package bigtrace.math;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.RealComposite;
/** Eigenvalues and eigenvectors of a real matrix. 
<P>
    If A is symmetric, then A = V*D*V' where the eigenvalue matrix D is
    diagonal and the eigenvector matrix V is orthogonal.
    I.e. A = V.times(D.times(V.transpose())) and 
    V.times(V.transpose()) equals the identity matrix.
<P>
    If A is not symmetric, then the eigenvalue matrix D is block diagonal
    with the real eigenvalues in 1-by-1 blocks and any complex eigenvalues,
    lambda + i*mu, in 2-by-2 blocks, [lambda, mu; -mu, lambda].  The
    columns of V represent the eigenvectors in the sense that A*V = V*D,
    i.e. A.times(V) equals V.times(D).  The matrix V may be badly
    conditioned, or even singular, so the validity of the equation
    A = V*D*inverse(V) depends upon V.cond().
 * @param <T>
**/

public class EigenValVecSymmDecomposition<T extends RealType< T >>{//{ implements java.io.Serializable {

/* ------------------------
   Class variables
 * ------------------------ */

   /**
	 * 
	 */
	//private static final long serialVersionUID = -2036425524286417428L;

/** Row and column dimension (square matrix).
   @serial matrix dimension.
   */
   private int n;


   /** Arrays for internal storage of eigenvalues.
   @serial internal storage of eigenvalues.
   */
   private double[] d, e;

   /** Array for internal storage of eigenvectors.
   @serial internal storage of eigenvectors.
   */
   //RealCompositeSymmetricMatrix< T > Vm;
   private double[][] V;


/* ------------------------
   Constructors
 * ------------------------ */

   /** Default constructor allocating memory for given dimentionality
   @param nDim   Number of dimensions
   */
   public EigenValVecSymmDecomposition( final int nDim )
   {
	   n=nDim;
	   V = new double[n][n];
       d = new double[n];
       e = new double[n];

   }
   
   private void computeRAIthreaded( final RandomAccessibleInterval< T > RAIin, 
		   final RandomAccessibleInterval< T > eVector, 
		   final RandomAccessibleInterval< T > eWeight)
   {
	   EigenValVecSymmDecomposition<T> dCalc = new EigenValVecSymmDecomposition<T>(eWeight.numDimensions());
	   
	   dCalc.computeRAI(  RAIin, eVector, eWeight);
   }
   public void computeRAI( final RandomAccessibleInterval< T > RAIin, final RandomAccessibleInterval< T > eVector, final RandomAccessibleInterval< T > eWeight)
   {
	   //final Cursor< RealComposite< T > > m = Views.iterable( Views.collapseReal( RAIin ) ).cursor();
	   final Cursor< RealComposite< T > > m = Views.iterable( Views.collapseReal( RAIin ) ).cursor();
	   final Cursor< RealComposite< T > > eV = Views.iterable( Views.collapseReal( eVector ) ).cursor();
	   final Cursor<  T > eW = Views.iterable(  eWeight  ).cursor();
	   //final Cursor< RealComposite< T > > ev = Views.iterable( Views.collapseReal( eigenvectors ) ).cursor();
	  // int [] posss = new int[3];
	   while ( m.hasNext() )
	   {
		  //m.localize(posss);
		  //System.out.println("posH "+Integer.toString(posss[0])+" "+Integer.toString(posss[1])+" "+Integer.toString(posss[2]));
		  //eV.localize(posss);
		  //System.out.println("posV "+Integer.toString(posss[0])+" "+Integer.toString(posss[1])+" "+Integer.toString(posss[2]));
		  //eV.localize(posss);
		  //System.out.println("posW "+Integer.toString(posss[0])+" "+Integer.toString(posss[1])+" "+Integer.toString(posss[2]));

			//computeTensor( m.next(), ev.next());
		   //computeTensor( m.next(), eigenvectors);
		   computeVectorWeight(m.next(),eV.next(),eW.next());
	   }
	   //return eVector;
   }
   public void computeCornersRAI( final RandomAccessibleInterval< T > RAIin, final RandomAccessibleInterval< T > eHarris)
   {
	   //final Cursor< RealComposite< T > > m = Views.iterable( Views.collapseReal( RAIin ) ).cursor();
	   final Cursor< RealComposite< T > > m = Views.iterable( Views.collapseReal( RAIin ) ).cursor();
	   final Cursor<  T > eH = Views.iterable(  eHarris  ).cursor();
	   //final Cursor< RealComposite< T > > ev = Views.iterable( Views.collapseReal( eigenvectors ) ).cursor();
	  // int [] posss = new int[3];
	   while ( m.hasNext() )
	   {
		  //m.localize(posss);
		  //System.out.println("posH "+Integer.toString(posss[0])+" "+Integer.toString(posss[1])+" "+Integer.toString(posss[2]));
		  //eV.localize(posss);
		  //System.out.println("posV "+Integer.toString(posss[0])+" "+Integer.toString(posss[1])+" "+Integer.toString(posss[2]));
		  //eV.localize(posss);
		  //System.out.println("posW "+Integer.toString(posss[0])+" "+Integer.toString(posss[1])+" "+Integer.toString(posss[2]));

			//computeTensor( m.next(), ev.next());
		   //computeTensor( m.next(), eigenvectors);
		   computeCorners(m.next(),eH.next());
	   }
	   //return eVector;
   }
   public void computeRAI( final RandomAccessibleInterval< T > tensor, 
		   final RandomAccessibleInterval< T > eVector, 
		   final RandomAccessibleInterval< T > eWeight,
		   final int nTasks,
		   final ExecutorService es )
   {
	   	assert nTasks > 0: "Passed nTasks < 1";

		final int tensorDims = tensor.numDimensions();

		long dimensionMax = Long.MIN_VALUE;
		long dimensionMin = Long.MAX_VALUE;
		int dimensionArgMax = -1;

		for ( int d = 0; d < tensorDims - 1; ++d )
		{
			final long size = tensor.dimension( d );
			if ( size > dimensionMax )
			{
				dimensionMax = size;
				dimensionArgMax = d;
				dimensionMin= tensor.min(d);
			}
		}

		final long stepSize = Math.max( dimensionMax / nTasks, 1 );
		final long stepSizeMinusOne = stepSize - 1;
		final long max = dimensionMin+dimensionMax - 1;
		//final ArrayList< Callable< RandomAccessibleInterval< T > > > tasks = new ArrayList<>();
		final ArrayList< Runnable > tasks_r = new ArrayList<>();
		for ( long currentMin = dimensionMin; currentMin < (dimensionMax+dimensionMin); currentMin += stepSize )
		{
			final long currentMax = Math.min( currentMin + stepSizeMinusOne, max );
			final long[] minT = new long[ tensorDims ];
			final long[] maxT = new long[ tensorDims ];
			final long[] minE = new long[ tensorDims ];
			final long[] maxE = new long[ tensorDims ];
			final long[] minW = new long[ tensorDims -1];
			final long[] maxW = new long[ tensorDims -1];
			tensor.min( minT );
			tensor.max( maxT );
			eVector.min( minE );
			eVector.max( maxE );
			eWeight.min( minW );
			eWeight.max( maxW );
			minW[ dimensionArgMax ] = minE[ dimensionArgMax ] = minT[ dimensionArgMax ] = currentMin;
			maxW[ dimensionArgMax ] = maxE[ dimensionArgMax ] = maxT[ dimensionArgMax ] = currentMax;
			
			final IntervalView< T > currentTensor = Views.interval( tensor, new FinalInterval( minT, maxT ) );
			final IntervalView< T > currentEigenVector = Views.interval( eVector, new FinalInterval( minE, maxE ) );
			final IntervalView< T > currentWeights = Views.interval( eWeight, new FinalInterval( minW, maxW ) );
			
			//tasks_r.add( () -> computeRAI( currentTensor, currentEigenVector, currentWeights ) );
			//tasks_r.add( () -> computeRAI( currentTensor, currentEigenVector, currentWeights, false ) );
			tasks_r.add( () -> computeRAIthreaded( currentTensor, currentEigenVector, currentWeights) );
		}
		

		Collection < Future  > futures = new HashSet<Future>();
		for (Runnable r : tasks_r) {

			futures.add(es.submit(r));
		}
	    for(Future future : futures) 
	    {
	        try {
	            future.get();
	        } catch (InterruptedException e) {
	            // Figure out if the interruption means we should stop.
	        	e.printStackTrace();
	        } catch (ExecutionException e) {
				// print error
				e.printStackTrace();
			}
	    }
		
   }
   
   
   public void computeVectorWeight( RealComposite< T > tensor, RealComposite< T > vector,  T weight)
   {
	   int nCount=0;
	  // RealCompositeSymmetricMatrix< T > m = new RealCompositeSymmetricMatrix<T>( null, n);
	   //m.setData(tensor);
	   int i;
	   for(i =0;i<n; i++)
		   for(int j =i;j<n; j++)
	   {
		   V[i][j] = tensor.get((long)(nCount)).getRealFloat();
		   //V[i][j] = m.get(i, j);
		   V[j][i]=V[i][j];
		   nCount++;
	   }
	   // Do the math
       // Tridiagonalize.
       tred2();
       // Diagonalize.
       tql2();
       
       // organize output
       // find the smallest absolute eigenvalue
       // and store corresponding vector
       int index = 0;
       double dMin = Math.abs(d[index]);
       for (i =1;i<n; i++)
       {
    	   if(Math.abs(d[i])<dMin)
    	   {
    		   index=i;
    		   dMin=Math.abs(d[index]);
    	   }
       }
       for (i =0;i<n; i++)
       {
    	   vector.get(i).setReal(V[i][index]);
       } 	   
       boolean bBothNegative = true;
       double dWeight = 0.0;
       for (i =0;i<n; i++)
       {
    	   if(i!=index)
    	   {
    		   if(d[i]>0)
    		   {
    			   bBothNegative = false;
    		   }
    		   else
    		   {
    			   dWeight-=d[i];
    		   }
    	   }
       }       
       if(bBothNegative)
       {
    	   weight.setReal(dWeight);
       }
       else
       {
    	   weight.setZero();
       }
       
   }
   public void computeCorners( RealComposite< T > tensor,  T harris)
   {
	   int nCount=0;
	  // RealCompositeSymmetricMatrix< T > m = new RealCompositeSymmetricMatrix<T>( null, n);
	   //m.setData(tensor);
	   int i;
	   for(i =0;i<n; i++)
		   for(int j =i;j<n; j++)
	   {
		   V[i][j] = tensor.get((long)(nCount)).getRealFloat();
		   //V[i][j] = m.get(i, j);
		   V[j][i]=V[i][j];
		   nCount++;
	   }
	   // Do the math
       // Tridiagonalize.
       tred2();
       // Diagonalize.
       tql2();
       
       
       // organize output
       // find the smallest absolute eigenvalue
       // and store corresponding vector
       int index = 0;
       double dMax = d[index];
       double dMin = Math.abs(d[index]);
       for (i =1;i<n; i++)
       {
    	   if(Math.abs(d[i])<dMin)
    	   {
    		   index=i;
    		   dMin=Math.abs(d[index]);
    	   }
    	   if(d[i]>dMax)
    	   {
    		   dMax=d[i];
    	   }
       }
   
       
       if(dMax<0)
       {
    	   harris.setReal(dMin);
       }
       else
       {
    	   harris.setZero();
       }
       /*
       boolean bBothNegative = true;
       double dWeight = 1.0;
       for (i =0;i<n; i++)
       {
    	   if(i!=index)
    	   {
    		   if(d[i]>0)
    		   {
    			   bBothNegative = false;
    		   }
    		   else
    		   {
    			   dWeight*=d[i];
    		   }
    	   }
       }       
       if(bBothNegative)
       {
    	   harris.setReal(dWeight*(-1.0));
       }
       else
       {
    	   harris.setZero();
       }
       */
       /*
       int index = 0;
       double dMax = Math.abs(d[index]);
       for (i =1;i<n; i++)
       {
    	   if(Math.abs(d[i])>dMax)
    	   {
    		   index=i;
    		   dMax=Math.abs(d[index]);
    	   }
       }

       if(dMax<0)
       {
    	   harris.setReal(dMax*(-1.0));
    	   
    	   //return;
       }
       else
       {
    	   harris.setZero();   
       }
       /*
       if(dMax<0)
       {
    	   harris.setZero();
    	   return;
       }
       else
       {
	       boolean bBothNegative = true;
	       for (i =0;i<n; i++)
	       {
	    	   if(i!=index)
	    	   {
	    		   if(d[i]>0)
	    		   {
	    			   bBothNegative = false;
	    		   }
	    	   }
	       }
	       if(bBothNegative)
	       {

	    	   harris.setReal(dMax);
	       }
	       else
	       {
	    	   harris.setZero();
	       }

       }
       */
   }
   
   public void computeTensor( RealComposite< T > tensor, final float [][] in_vals)
   {
	   int nCount=0;
	   //RealCompositeSymmetricMatrix< T > m = new RealCompositeSymmetricMatrix<T>( null, n);
	   //m.setData(tensor);
	   for(int i =0;i<n; i++)
		   for(int j =i;j<n; j++)
	   {
		   V[i][j] = tensor.get((long)(nCount)).getRealFloat();
		   //V[i][j] = m.get(i, j);
		   V[j][i]=V[i][j];
		   nCount++;
	   }
       // Tridiagonalize.
       tred2();
 
       // Diagonalize.
       tql2();
       
       
       System.out.println("eig "+Double.toString(d[0])+" "+Double.toString(d[1])+" "+Double.toString(d[2]));
	   for(int i =0;i<n; i++)
		   for(int j =0;j<n; j++)
		   {
			   //tensor.get((long)(nCount))
			   in_vals[i][j]=(float) V[j][i];
		   }

   }
   
   public float [] getLineDirection()
   {
	   int nIndex=0;
	   float [] vLineDirection = new float [3];
	   double dMin=Double.MAX_VALUE;
	   int i;
	   for(i=0;i<3;i++)
	   {
		   if(Math.abs(d[i])<dMin)
		   {
			   dMin=Math.abs(d[i]);
			   nIndex=i;
		   }
	   }
	   System.out.println("index "+Integer.toString(nIndex));
	   for(i=0;i<3;i++)
	   {
		   vLineDirection[i] = (float) V[i][nIndex];
	   }
	   return vLineDirection;
   }

/* ------------------------
   Public Methods
 * ------------------------ */

   /** Return the eigenvector matrix
   @return     V
   */

  /* public Matrix getV () {
      return new Matrix(V,n,n);
   }*/

   /** Return the real parts of the eigenvalues
   @return     real(diag(D))
   */

   public double[] getRealEigenvalues () {
      return d;
   }

   /** Return the imaginary parts of the eigenvalues
   @return     imag(diag(D))
   */

   public double[] getImagEigenvalues () {
      return e;
   }

   /** Return the block diagonal eigenvalue matrix
   @return     D
   */

   /*public Matrix getD () {
      Matrix X = new Matrix(n,n);
      double[][] D = X.getArray();
      for (int i = 0; i < n; i++) {
         for (int j = 0; j < n; j++) {
            D[i][j] = 0.0;
         }
         D[i][i] = d[i];
         if (e[i] > 0) {
            D[i][i+1] = e[i];
         } else if (e[i] < 0) {
            D[i][i-1] = e[i];
         }
      }
      return X;
   }*/
   
   /* ------------------------
   Private Methods
 * ------------------------ */

   // Symmetric Householder reduction to tridiagonal form.

   private void tred2 () {

   //  This is derived from the Algol procedures tred2 by
   //  Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
   //  Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
   //  Fortran subroutine in EISPACK.

      for (int j = 0; j < n; j++) {
         d[j] = V[n-1][j];
      }

      // Householder reduction to tridiagonal form.
   
      for (int i = n-1; i > 0; i--) {
   
         // Scale to avoid under/overflow.
   
         double scale = 0.0;
         double h = 0.0;
         for (int k = 0; k < i; k++) {
            scale = scale + Math.abs(d[k]);
         }
         if (scale == 0.0) {
            e[i] = d[i-1];
            for (int j = 0; j < i; j++) {
               d[j] = V[i-1][j];
               V[i][j] = 0.0;
               V[j][i] = 0.0;
            }
         } else {
   
            // Generate Householder vector.
   
            for (int k = 0; k < i; k++) {
               d[k] /= scale;
               h += d[k] * d[k];
            }
            double f = d[i-1];
            double g = Math.sqrt(h);
            if (f > 0) {
               g = -g;
            }
            e[i] = scale * g;
            h = h - f * g;
            d[i-1] = f - g;
            for (int j = 0; j < i; j++) {
               e[j] = 0.0;
            }
   
            // Apply similarity transformation to remaining columns.
   
            for (int j = 0; j < i; j++) {
               f = d[j];
               V[j][i] = f;
               g = e[j] + V[j][j] * f;
               for (int k = j+1; k <= i-1; k++) {
                  g += V[k][j] * d[k];
                  e[k] += V[k][j] * f;
               }
               e[j] = g;
            }
            f = 0.0;
            for (int j = 0; j < i; j++) {
               e[j] /= h;
               f += e[j] * d[j];
            }
            double hh = f / (h + h);
            for (int j = 0; j < i; j++) {
               e[j] -= hh * d[j];
            }
            for (int j = 0; j < i; j++) {
               f = d[j];
               g = e[j];
               for (int k = j; k <= i-1; k++) {
                  V[k][j] -= (f * e[k] + g * d[k]);
               }
               d[j] = V[i-1][j];
               V[i][j] = 0.0;
            }
         }
         d[i] = h;
      }
   
      // Accumulate transformations.
   
      for (int i = 0; i < n-1; i++) {
         V[n-1][i] = V[i][i];
         V[i][i] = 1.0;
         double h = d[i+1];
         if (h != 0.0) {
            for (int k = 0; k <= i; k++) {
               d[k] = V[k][i+1] / h;
            }
            for (int j = 0; j <= i; j++) {
               double g = 0.0;
               for (int k = 0; k <= i; k++) {
                  g += V[k][i+1] * V[k][j];
               }
               for (int k = 0; k <= i; k++) {
                  V[k][j] -= g * d[k];
               }
            }
         }
         for (int k = 0; k <= i; k++) {
            V[k][i+1] = 0.0;
         }
      }
      for (int j = 0; j < n; j++) {
         d[j] = V[n-1][j];
         V[n-1][j] = 0.0;
      }
      V[n-1][n-1] = 1.0;
      e[0] = 0.0;
   } 

   // Symmetric tridiagonal QL algorithm.
   
   private void tql2 () {

   //  This is derived from the Algol procedures tql2, by
   //  Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
   //  Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
   //  Fortran subroutine in EISPACK.
   
      for (int i = 1; i < n; i++) {
         e[i-1] = e[i];
      }
      e[n-1] = 0.0;
   
      double f = 0.0;
      double tst1 = 0.0;
      double eps = Math.pow(2.0,-52.0);
      for (int l = 0; l < n; l++) {

         // Find small subdiagonal element
   
         tst1 = Math.max(tst1,Math.abs(d[l]) + Math.abs(e[l]));
         int m = l;
         while (m < n) {
            if (Math.abs(e[m]) <= eps*tst1) {
               break;
            }
            m++;
         }
   
         // If m == l, d[l] is an eigenvalue,
         // otherwise, iterate.
   
         if (m > l) {
            int iter = 0;
            do {
               iter = iter + 1;  // (Could check iteration count here.)
   
               // Compute implicit shift
   
               double g = d[l];
               double p = (d[l+1] - g) / (2.0 * e[l]);
               double r = EigenValVecSymmDecomposition.hypot(p,1.0);
               if (p < 0) {
                  r = -r;
               }
               d[l] = e[l] / (p + r);
               d[l+1] = e[l] * (p + r);
               double dl1 = d[l+1];
               double h = g - d[l];
               for (int i = l+2; i < n; i++) {
                  d[i] -= h;
               }
               f = f + h;
   
               // Implicit QL transformation.
   
               p = d[m];
               double c = 1.0;
               double c2 = c;
               double c3 = c;
               double el1 = e[l+1];
               double s = 0.0;
               double s2 = 0.0;
               for (int i = m-1; i >= l; i--) {
                  c3 = c2;
                  c2 = c;
                  s2 = s;
                  g = c * e[i];
                  h = c * p;
                  r = EigenValVecSymmDecomposition.hypot(p,e[i]);
                  e[i+1] = s * r;
                  s = e[i] / r;
                  c = p / r;
                  p = c * d[i] - s * g;
                  d[i+1] = h + s * (c * g + s * d[i]);
   
                  // Accumulate transformation.
   
                  for (int k = 0; k < n; k++) {
                     h = V[k][i+1];
                     V[k][i+1] = s * V[k][i] + c * h;
                     V[k][i] = c * V[k][i] - s * h;
                  }
               }
               p = -s * s2 * c3 * el1 * e[l] / dl1;
               e[l] = s * p;
               d[l] = c * p;
   
               // Check for convergence.
   
            } while (Math.abs(e[l]) > eps*tst1);
         }
         d[l] = d[l] + f;
         e[l] = 0.0;
      }
     
      // Sort eigenvalues and corresponding vectors.
   
      for (int i = 0; i < n-1; i++) {
         int k = i;
         double p = d[i];
         for (int j = i+1; j < n; j++) {
            if (d[j] < p) {
               k = j;
               p = d[j];
            }
         }
         if (k != i) {
            d[k] = d[i];
            d[i] = p;
            for (int j = 0; j < n; j++) {
               p = V[j][i];
               V[j][i] = V[j][k];
               V[j][k] = p;
            }
         }
      }
   }

   
  public static double hypot(double paramDouble1, double paramDouble2) {
	  double d;
		if (Math.abs(paramDouble1) > Math.abs(paramDouble2)) {
				d = paramDouble2 / paramDouble1;
				d = Math.abs(paramDouble1) * Math.sqrt(1.0D + d * d);
			} else if (paramDouble2 != 0.0D) {
				d = paramDouble1 / paramDouble2;
				d = Math.abs(paramDouble2) * Math.sqrt(1.0D + d * d);
			} else {
				d = 0.0D;
			}
	     return d;
	}
}
package bigtrace.geometry;

/** This class interpolates a set of points using cubic splines
 *	with different conditions at end points 
 *  (zero second derivatives (natural or specified/calculated first derivatives). 
 *  Given a set of knots x (all different and arranged in increasing order)
 *  and function values y at these positions, the class build the spline
 *  that can be evaluated at any point xp within the range of x.
 *  In addition, it contains methods/functions for spline derivative
 *  calculation at xp locations (evalSlope or interpSlope).
 *  It is based on the publication 
 *  Haysn Hornbeck "Fast Cubic Spline Interpolation"
 *  https://arxiv.org/abs/2001.09253 
 *  Implemented by Eugene Katrukha (katpyxa@gmail.com)
 *  
 *  If multiple interpolations required, one can instantiate
 *  a class and calculate interpolating coefficients once,
 *  then use evalSpline/evalSlope functions.
 *  For one-time case use static methods interpSpline/interpSlope can be used.
 **/
public class CubicSpline {
	
	/** second derivatives **/	
	private double[] ypp;
	/** knots **/
	private double[] xpoints = null;
	/** function values **/
	private double[] ypoints = null;
	
	/** default constructor, assumes zero second derivatives at end points**/
	public CubicSpline(final double[] x, final double[] y) {
		ypp = initSpline(x, y, Double.MAX_VALUE, Double.MAX_VALUE);
		xpoints = x.clone();
		ypoints = y.clone();
	}
	/** spline constructor, uses provided values of derivatives at end points**/
	public CubicSpline(final double[] x, final double[] y, final double start_deriv, final double end_deriv) {
		ypp = initSpline(x, y, start_deriv,  end_deriv);
		xpoints = x.clone();
		ypoints = y.clone();
		
	}	
	/** spline constructor, estimates first derivatives at each end point
	 * using first (last) n_est points (with finite difference) **/
	public CubicSpline(final double[] x, final double[] y, final int n_est) {
		double [] firstDerivs = estimateEndsFirstDerivatives(x,y,n_est);
		ypp = initSpline(x, y, firstDerivs[0],  firstDerivs[1]);
		xpoints = x.clone();
		ypoints = y.clone();
		
	}
	
	/** Given arrays of data points x[0..n-1] and y[0..n-1], computes the
	values of the second derivative at each of the data points
	y2[0..n-1] for use in the evalSpline() function. 
	Uses provided (start_deriv, end_deriv) values of first derivatives at ends.
	If  start or end_deriv are larger than 0.99e30 , assumes corresponding second derivative is zero*/
	public static double [] initSpline(final double[] x, final double[] y, final double start_deriv, final double end_deriv)
	{
		int j;
		double new_x,new_y,old_x,old_y,new_dj, old_dj;
		double aj,bj,dj,cj;
		double inv_denom;
		int n = x.length;
		if (n<2)
		{
			System.err.println("more than two points required for spline interpolation");
			return null;
		}
		double [] y2 = new double[x.length];
		double[] c_p = new double[n];
		
		//recycle these values in later routines
		new_x = x[1];
		new_y = y[1];
		cj = x[1]-x[0];
		new_dj = (y[1]-y[0])/cj;

		//first derivative at start/beginning
		if(start_deriv>0.99e30)
		{
			c_p[0]=0;
			y2[0]=0;
		}
		else
		{
			c_p[0]=0.5;
			y2[0]=3*(new_dj-start_deriv)/cj;
		}
		
		//forward substitution portion
		j=1;
		while(j<(n-1))
		{
			old_x = new_x;
			old_y = new_y;
			aj=cj;
			old_dj = new_dj;
			//generate new quantities
			new_x = x[j+1];
			new_y = y[j+1];
			cj = new_x-old_x;
			new_dj = (new_y-old_y)/cj;
			bj = 2.0*(cj+aj);
			inv_denom = 1.0/(bj-aj*c_p[j-1]);
			dj = 6.0*(new_dj-old_dj);
			y2[j]= ( dj- aj*y2[j-1])*inv_denom;
			c_p[j] = cj*inv_denom;
			j+=1;
		}
		// end derivative
		if(end_deriv>0.99e30)
		{
			c_p[j]=0;
			y2[j]=0;
		}
		else
		{
			//old_x = new_x;
			//old_y = new_y;
			
			aj = cj;
			//old_cj = new_dj;
			
			//this has the same effect as skipping c_n
			cj = 0.0;
			//bj = 2*(cj+aj);
			bj = 2*aj;
			//new_dj = end_deriv;
			inv_denom = 1.0/(bj - aj*c_p[j-1]);
			//dj = 6*(new_dj - old_dj);
			dj = 6*(end_deriv - new_dj);
			y2[j] = (dj - aj*y2[j-1])*inv_denom;
			c_p[j] = cj * inv_denom;
		}
		
		// backward substitution portion
		while (j>0)
		{
			j-=1;
			y2[j]=y2[j]-c_p[j]*y2[j+1];
		}
		return y2;
	}
	
	/** provides interpolated function value at position xp**/
	public static double evalSpline(final double[] x, final double[] y, final double [] y2, final double xp)
	{
		int ls,rs,m;
		double ba,ba2,xa,bx, lower, C, D;
		int n = x.length;
		//binary search of the interval
		ls = 0;
		rs = n-1;
		while (rs>1+ls) 
		{
			m = (int) Math.floor(0.5*(ls+rs));
			if(x[m]<xp)
				ls=m;
			else rs = m;
		}
		ba = x[rs]-x[ls];
		xa = xp -x[ls];
		bx = x[rs]-xp;
		ba2 = ba*ba;
		lower = xa*y[rs]+bx*y[ls];
		C = (xa*xa-ba2)*xa*y2[rs];
		D = (bx*bx-ba2)*bx*y2[ls];
		
		return (lower +(C+D)/6.0)/ba;
	}
	
	/** provides interpolated function values at positions xp **/
	public static double [] evalSpline(final double[] x, final double[] y, final double [] y2, final double [] xp)
	{
		int k = xp.length;
		double [] out = new double[k];
		for (int i=0;i<k;i++)
		{
			out[i] = evalSpline(x, y, y2,  xp[i]);
		}
		return out;
	}
	
	public double evalSpline(final double xp)
	{	
		return evalSpline(xpoints,ypoints,ypp,xp);
	}
	public double [] evalSpline(final double [] xp)
	{	
		return evalSpline(xpoints,ypoints,ypp,xp);
	}
	/** function estimates first derivatives at two ends of 
	 * provided function y (tabulated at x values) 
	 * using first (or last) n_est points */
	public static double [] estimateEndsFirstDerivatives(final double [] x, final double [] y, int n_est)
	{
		int n = x.length;
		int i;
		double [] out = new double [2];
		double [] coeff;
		double [] alpha = new double[n_est];
		//left side (start)
		for (i=0;i<n_est;i++)
		{
			alpha[i]=x[i];
		}
		coeff = finitDiffCoeffFirstDeriv(alpha,x[0]);
		out[0] = 0.0;
		for(i=0;i<n_est;i++)
		{
			out[0]+=coeff[i]*y[i];
		}
		//right side (end)
		for(i=(n-n_est);i<n;i++)
		{
			alpha[i-n+n_est]=x[i];
		}
		coeff = finitDiffCoeffFirstDeriv(alpha,x[n-1]);
		out[1] = 0.0;
		for(i=(n-n_est);i<n;i++)
		{
			out[1]+=coeff[i-n+n_est]*y[i];
		}		
		
		return out;
	}
	
	
	/** adaptation of  Fornberg, Bengt (1988), "Generation of Finite Difference Formulas on Arbitrarily Spaced Grid
	 * for the first derivative. 
	 * Given a set of x values in alpha, function calculates finite difference coefficients 
	 * at point x0 (for the first derivative).
	 * The x values in alpha not necessary should be equally spaced,
	 * but must be distinct from each other. */
	public static double [] finitDiffCoeffFirstDeriv(double [] alpha, double x0)
	{
		int N = alpha.length;
		int n,v;
		double c2,c3;
		double [] coeff = new double [N];
		double [][][] delta = new double [N][N][2];
		delta[0][0][0] = 1.0;
		double c1 = 1.0;
		for (n=1;n<N;n++)
		{
			c2 = 1.0;
			for (v=0;v<n;v++)
			{
				c3 = alpha[n]-alpha[v];
				c2 *= c3;
				delta[n][v][0] = (alpha[n]-x0)*delta[n-1][v][0]/c3;
				delta[n][v][1] = ((alpha[n]-x0)*delta[n-1][v][1]-delta[n-1][v][0])/c3;
				
				delta[n][n][0] = (c1/c2)*((-1)*(alpha[n-1]-x0)*delta[n-1][n-1][0]);
				delta[n][n][1] = (c1/c2)*(delta[n-1][n-1][0]-(alpha[n-1]-x0)*delta[n-1][n-1][1]);
			}
			c1 = c2;
		}
		for (v=0;v<N;v++)
		{
			coeff[v] = delta[N-1][v][1];
		}
		return coeff;
	}
	
	/** function calculates derivative (slope) of interpolated spline at the point xp */
	public static double evalSlope(final double[] x, final double[] y, final double [] y2, final double xp)
	{
		int ls,rs,m;
		double ba,ba2,xa,bx, lower, C, D;
		int n = x.length;
		//binary search of the interval
		ls = 0;
		rs = n-1;
		while (rs>1+ls) 
		{
			m = (int) Math.floor(0.5*(ls+rs));
			if(x[m]<xp)
				ls=m;
			else rs = m;
		}
		ba = x[rs]-x[ls];
		xa = xp -x[ls];
		bx = x[rs]-xp;
		ba2 = ba*ba;
		lower = y[rs]-y[ls];
		C = (3.0*xa*xa-ba2)*y2[rs];
		D = (ba2-3.0*bx*bx)*y2[ls];
		
		return (lower +(C+D)/6.0)/ba;
	}
	
	/** function calculates derivatives (slopes) of interpolated spline at points xp */
	public static double [] evalSlope(final double[] x, final double[] y, final double [] y2, final double [] xp)
	{
		int k = xp.length;
		double [] out = new double[k];
		for (int i=0;i<k;i++)
		{
			out[i] = evalSlope(x, y, y2,  xp[i]);
		}
		return out;
	}
	
	public double evalSlope(final double xp)
	{	
		return evalSlope(xpoints,ypoints,ypp,xp);
	}
	public double [] evalSlope(final double [] xp)
	{	
		return evalSlope(xpoints,ypoints,ypp,xp);
	}
	
	/** convenience function for "run once" evaluation (natural spline) */
	public static double [] interpSpline(final double[] x, final double[] y, final double [] xp)
	{
		final double [] ypp = initSpline(x, y, Double.MAX_VALUE, Double.MAX_VALUE);
		return evalSpline(x,y,ypp,xp);
	}

	/** convenience function for "run once" evaluation (first derivatives at ends provided) */
	public static double [] interpSpline(final double[] x, final double[] y, final double start_deriv, final double end_deriv, final double [] xp)
	{
		final double [] ypp = initSpline(x, y, start_deriv, end_deriv);
		return evalSpline(x,y,ypp,xp);
	}
	
	/** convenience function for "run once" evaluation (first derivatives at ends estimated using n_est points) */
	public static double [] interpSpline(final double[] x, final double[] y, final int n_est, final double [] xp)
	{
		final double [] firstDerivs = estimateEndsFirstDerivatives(x,y,n_est);
		final double [] ypp = initSpline(x, y, firstDerivs[0],  firstDerivs[1]);
		return evalSpline(x,y,ypp,xp);
	}
	
	/** convenience function for "run once" derivatives calculation (natural spline) */
	public static double [] interpSlope(final double[] x, final double[] y, final double [] xp)
	{
		final double [] ypp = initSpline(x, y, Double.MAX_VALUE, Double.MAX_VALUE);
		return evalSlope(x,y,ypp,xp);
	}
	
	/** convenience function for "run once" derivatives calculation (first derivatives at ends provided) */
	public static double [] interpSlope(final double[] x, final double[] y, final double start_deriv, final double end_deriv, final double [] xp)
	{
		final double [] ypp = initSpline(x, y, start_deriv, end_deriv);
		return evalSlope(x,y,ypp,xp);
	}
	
	/** convenience function for "run once" derivatives calculation (first derivatives at ends estimated using n_est points) */
	public static double [] interpSlope(final double[] x, final double[] y, final int n_est, final double [] xp)
	{
		final double [] firstDerivs = estimateEndsFirstDerivatives(x,y,n_est);
		final double [] ypp = initSpline(x, y, firstDerivs[0],  firstDerivs[1]);
		return evalSlope(x,y,ypp,xp);
	}


}

package bigtrace.geometry;

public class LinearInterpolation {
	/** knots **/
	private double[] xpoints = null;
	/** function values **/
	private double[] ypoints = null;
	
	/** default constructor, saves coordinates **/
	public LinearInterpolation(final double[] x, final double[] y) {
	
		xpoints = x.clone();
		ypoints = y.clone();
	}
	/** provides interpolated function value at position xp**/
	public static double evalLinearInterp(final double[] x, final double[] y, final double xp)
	{
		int ls,rs,m;
		double ba,xa,bx;
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

		
		return (y[ls]*bx/ba)+(y[rs]*xa/ba);
	}
	/** provides interpolated function values at positions xp **/
	public static double [] evalLinearInterp(final double[] x, final double[] y, final double [] xp)
	{
		int k = xp.length;
		double [] out = new double[k];
		for (int i=0;i<k;i++)
		{
			out[i] = evalLinearInterp(x, y,  xp[i]);
		}
		return out;
	}
	
	public double evalLinearInterp(final double xp)
	{	
		return evalLinearInterp(xpoints,ypoints,xp);
	}
	
	public double [] evalSpline(final double [] xp)
	{	
		return evalLinearInterp(xpoints,ypoints,xp);
	}
}

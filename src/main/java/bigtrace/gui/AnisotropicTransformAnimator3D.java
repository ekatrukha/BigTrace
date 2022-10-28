package bigtrace.gui;

import bdv.util.Affine3DHelpers;
import bdv.viewer.animate.AbstractTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

public class AnisotropicTransformAnimator3D extends AbstractTransformAnimator
{
	
	
	private final double[] qStart;

	private final double[] qDiff;

	private final double[] xg0Start;

	private final double[] xg0Diff;

	private final double []  scaleStart;

	private final double [] scaleEnd;

	private final double [] scaleDiff;

	private final double [] scaleRate;

	private final double cX;

	private final double cY;

	public AnisotropicTransformAnimator3D( final AffineTransform3D transformStart, final AffineTransform3D transformEnd, final double cX, final double cY, final long duration )
	{
		super( duration );
		this.cX = cX;
		this.cY = cY;

		qStart = new double[ 4 ];
		final double[] qStartInv = new double[ 4 ];
		final double[] qEnd = new double[ 4 ];
		final double[] qEndInv = new double[ 4 ];
		
		scaleStart = new double[3];
		scaleEnd = new double[3];
		scaleDiff = new double[3];
		scaleRate = new double[3];
		
		qDiff = new double[ 4 ];
		Affine3DHelpers.extractRotationAnisotropic( transformStart, qStart );
		LinAlgHelpers.quaternionInvert( qStart, qStartInv );

		Affine3DHelpers.extractRotationAnisotropic( transformEnd, qEnd );
		LinAlgHelpers.quaternionInvert( qEnd, qEndInv );

		LinAlgHelpers.quaternionMultiply( qStartInv, qEnd, qDiff );
		if ( qDiff[ 0 ] < 0 )
			LinAlgHelpers.scale( qDiff, -1, qDiff );

		for (int d = 0;d<3; d++)
		{
			scaleStart[d] = Affine3DHelpers.extractScale( transformStart, d );
			scaleEnd[d] = Affine3DHelpers.extractScale( transformEnd, d );
			scaleDiff[d] = scaleEnd[d] - scaleStart[d];
			scaleRate[d] = scaleEnd[d] / scaleStart[d];
		}

		final double[] tStart = new double[ 3 ];
		final double[] tEnd = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
		{
			tStart[ d ] = transformStart.get( d, 3 ) / scaleStart[d];
			tEnd[ d ] = transformEnd.get( d, 3 ) / scaleEnd[d];
		}

		xg0Start = new double[3];
		final double[] xg0End = new double[3];
		xg0Diff = new double[3];

		final double[][] R = new double[ 3 ][ 3 ];
		LinAlgHelpers.quaternionToR( qStartInv, R );
		LinAlgHelpers.mult( R, tStart, xg0Start );
		LinAlgHelpers.scale( xg0Start, -1, xg0Start );
		LinAlgHelpers.quaternionToR( qEndInv, R );
		LinAlgHelpers.mult( R, tEnd, xg0End );
		LinAlgHelpers.scale( xg0End, -1, xg0End );
		LinAlgHelpers.subtract( xg0End, xg0Start, xg0Diff );
	}

	@Override
	public AffineTransform3D get( final double t )
	{
		final double[] qDiffCurrent = new double[ 4 ];
		final double[] qCurrent = new double[ 4 ];
		LinAlgHelpers.quaternionPower( qDiff, t, qDiffCurrent );
		LinAlgHelpers.quaternionMultiply( qStart, qDiffCurrent, qCurrent );

		final double [] alpha = new double [3];
		final double [] scaleCurrent = new double [3];
		
		for (int d=0;d<3;d++)
		{
			alpha[d]=  Math.pow( scaleRate[d], t );
			scaleCurrent[d] = scaleStart[d] * alpha[d];
		}
		//double alpha = Math.pow( scaleRate, t );
		//final double alpha = Math.pow( scaleRate, t );
		//final double scaleCurrent = scaleStart * alpha;

		final double[] xg0Current = new double[ 3 ];
		final double[] tCurrent = new double[ 3 ];
		//final double f = Math.abs( scaleRate - 1.0 ) < 0.0001 ? -t : ( scaleEnd / alpha - scaleEnd ) / scaleDiff;
		//LinAlgHelpers.scale( xg0Diff, f, xg0Current );
		final double [] f = new double [3];
		for (int d=0;d<3;d++)
		{
			f[d] = Math.abs( scaleRate[d] - 1.0 ) < 0.0001 ? -t : ( scaleEnd[d] / alpha[d] - scaleEnd[d] ) / scaleDiff[d];
			xg0Current[d]=f[d]*xg0Diff[d];
		}		
		
		
		for ( int r = 0; r < 3; ++r )
			xg0Current[ r ] -= xg0Start[ r ];
		final double[][] Rcurrent = new double[ 3 ][ 3 ];
		LinAlgHelpers.quaternionToR( qCurrent, Rcurrent );
		LinAlgHelpers.mult( Rcurrent, xg0Current, tCurrent );

		final double[][] m = new double[ 3 ][ 4 ];
		for ( int r = 0; r < 3; ++r )
		{
			for ( int c = 0; c < 3; ++c )
				m[ r ][ c ] = scaleCurrent[c] * Rcurrent[ r ][ c ];
				//m[ r ][ c ] = scaleCurrent[r] * Rcurrent[ r ][ c ];
			m[ r ][ 3 ] = scaleCurrent[r] * tCurrent[ r ];
		}
		m[ 0 ][ 3 ] += cX;
		m[ 1 ][ 3 ] += cY;

		final AffineTransform3D transform = new AffineTransform3D();
		transform.set( m );
		return transform;
	}
}

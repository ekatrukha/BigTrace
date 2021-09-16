package bigtrace.math;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class SecondDerivative {
	public static < T extends NumericType< T > > void secondDerivativeCentralDifference( final RandomAccessible< T > source, final RandomAccessibleInterval< T > derivative, final int dimension1, final int dimension2)
	{
		final Cursor< T > pp; //= Views.flatIterable( Views.interval( source, Intervals.translate( Intervals.translate( derivative, 1, dimension1 ),1,dimension2) ) ).cursor();
		final Cursor< T > mm; //= Views.flatIterable( Views.interval( source, Intervals.translate( derivative, -1, dimension1 ) ) ).cursor();
		final Cursor< T > pm;
		final Cursor< T > mp;
		final Cursor< T > nn;
		T val;
		final double dFactor = 1.0/12.0; 
		if(dimension1==dimension2)
		{
			pp=Views.flatIterable( Views.interval( source, Intervals.translate( Intervals.translate( derivative, 1, dimension1 ),1,dimension1) ) ).cursor();
			mm =Views.flatIterable( Views.interval( source, Intervals.translate( Intervals.translate( derivative, -1, dimension1 ),-1,dimension1) ) ).cursor();
			pm =Views.flatIterable( Views.interval( source, Intervals.translate( derivative, 1, dimension1 ) ) ).cursor();
			mp =Views.flatIterable( Views.interval( source, Intervals.translate( derivative, -1, dimension1 ) ) ).cursor();
			nn = Views.flatIterable( Views.interval( source,  derivative )).cursor();
			for ( final T t : Views.flatIterable( derivative ) )
			{
				t.set( pm.next() );
				t.add( mp.next() );
				t.mul( 16.0 );
				t.sub( pp.next() );
				t.sub( mm.next() );	
				val=nn.next();
				val.mul(30.0);
				t.sub(val);
				t.mul(dFactor);
			}
		}
		else
		{
			pp=Views.flatIterable( Views.interval( source, Intervals.translate( Intervals.translate( derivative, 1, dimension1 ),1,dimension2) ) ).cursor();
			mm =Views.flatIterable( Views.interval( source, Intervals.translate( Intervals.translate( derivative, -1, dimension1 ),-1,dimension2) ) ).cursor();
			pm =Views.flatIterable( Views.interval( source, Intervals.translate( Intervals.translate( derivative, 1, dimension1 ),-1,dimension2) ) ).cursor();
			mp =Views.flatIterable( Views.interval( source, Intervals.translate( Intervals.translate( derivative, -1, dimension1 ),1,dimension2) ) ).cursor();
			for ( final T t : Views.flatIterable( derivative ) )
			{
				t.set( pp.next() );
				t.sub( mp.next() );
				t.sub( pm.next() );
				t.add( mm.next() );
				t.mul( 0.25 );
			}
		}
	}
}

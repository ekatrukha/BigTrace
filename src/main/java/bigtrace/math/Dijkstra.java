package bigtrace.math;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
public class Dijkstra {

	//Img<BitType> istat;
	
	IntervalView< UnsignedByteType > trace_weights;
	IntervalView< FloatType > trace_vectors;
	/** Computes the shortest path based on the given cost values and
	 vectors.
	 	**/
	public Dijkstra(IntervalView< UnsignedByteType > trace_weights_, IntervalView< FloatType > trace_vectors_)
	{
		trace_weights = trace_weights_;
		trace_vectors = trace_vectors_;
		//istat =  new ArrayImgFactory<>( new BitType() ).create( trace_weights_ );
		
	}
}

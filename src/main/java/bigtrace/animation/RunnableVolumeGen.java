package bigtrace.animation;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class RunnableVolumeGen < T extends RealType< T > & NativeType< T > > implements Runnable
{

	final UncoilAnimation< T > unCoil;
	final int nFrame;
	
	
	public RunnableVolumeGen (final UncoilAnimation<T> unCoil_, final int nFrame_ )
	{
		unCoil = unCoil_;
		nFrame = nFrame_;
		
	}
	@Override
	public void run()
	{
		unCoil.generateSingleVolume( nFrame );
	}


}

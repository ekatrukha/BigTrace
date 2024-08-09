package bigtrace.animation;

import javax.swing.DefaultListModel;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;

public class KeyFrameAnimation < T extends RealType< T > & NativeType< T > >
{
	final BigTrace<T> bt;
	int nTotalTime;
	
	final DefaultListModel<KeyFrame> keyFrames;
	
	public KeyFrameAnimation(final BigTrace<T> bt_, final DefaultListModel<KeyFrame> keyFrames_)
	{
		this.bt = bt_;
		keyFrames = keyFrames_;
	}
	
	public void setTotalTime(int t)
	{
		nTotalTime = t;
	}
	public int getTotalTime()
	{
		return nTotalTime;
	}
}

package bigtrace.animation;

import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.SwingWorker;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;

public class AnimationPlayer < T extends RealType< T > & NativeType< T > >  extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	final BigTrace<T> bt;
	final AnimationPanel< T > aPanel;
	final JSlider timeSlider;

	JButton butPlayStop = null;
	ImageIcon tabIconPlay = null;
	
	@Override
	public String getProgressState()
	{
		return null;
	}

	@Override
	public void setProgressState( String state_ )
	{
		
	}
	public AnimationPlayer(BigTrace<T> bt, AnimationPanel< T > aPanel_)
	{
		this.bt = bt;
		this.aPanel = aPanel_;
		this.timeSlider =  aPanel.timeSlider;

	}

	@Override
	protected Void doInBackground() throws Exception
	{
		int currVal = timeSlider.getValue();
		float dNorm = (float)(aPanel.tsSpan)/aPanel.kfAnim.getTotalTime();
		float currentTime = (timeSlider.getValue())*dNorm;
		float dT = 1.0f/24.0f;
		
		//float dT = 0.5f;
		
		while(true)
		{
			Thread.sleep(Math.round( dT*1000.0f ));
			currentTime += dT;
			currVal  = Math.round( dNorm*currentTime );
			//currVal ++;
			if(currVal >timeSlider.getMaximum())
			{
				currVal = timeSlider.getMinimum();
				currentTime = 0.0f;
			}
			timeSlider.setValue( currVal );
			if(isCancelled())
			{
				return null;	
			}
		}
		
		//return null;
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
    	catch (ExecutionException e) 
    	{
    		e.getCause().printStackTrace();
    		String msg = String.format("Unexpected error during playing: %s", 
    				e.getCause().toString());
    		System.out.println(msg);
    	} 
    	catch (InterruptedException e) 
    	{
    		// Process e here
    	}
    	catch (Exception e)
    	{

    	}
	

    	if(butPlayStop!= null && tabIconPlay!= null)
    	{
    		butPlayStop.setIcon( tabIconPlay );
    		butPlayStop.setToolTipText( "Play" );
    	}

    	//unlock user interaction
    	bt.bInputLock = false;
    	bt.setLockMode(false);

    }
}

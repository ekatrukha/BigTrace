package bigtrace.animation;


import java.io.File;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingWorker;
import javax.imageio.ImageIO;


import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;


import bdv.util.Prefs;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;




public class AnimationRender  < T extends RealType< T > & NativeType< T > >  extends SwingWorker<Void, String> implements BigTraceBGWorker
{
	
	final BigTrace<T> bt;
	
	AnimationPanel< T > aPanel;
	
	private String progressState;
	
	JButton butRecord = null;
	
	ImageIcon tabIconRecord = null;
	boolean bSaveMultiBox = true;
	boolean bSaveTextOverlay = true;

	@Override
	public String getProgressState()
	{
		return progressState;
	}
	@Override
	public void setProgressState(String state_)
	{
		progressState = state_;
	}
	
	public AnimationRender(BigTrace<T> bt_,  AnimationPanel< T > aPanel_)
	{
		this.bt = bt_;
		this.aPanel = aPanel_;
	}
	
	@Override
	protected Void doInBackground() throws Exception 
	{
		if(aPanel.sRenderSavePath == null)
		{
			return null;
		}

		boolean bSaveMultiBox = Prefs.showMultibox();
		boolean bSaveTextOverlay = Prefs.showTextOverlay();
		int nTotFrames = aPanel.kfAnim.nTotalTime*aPanel.nRenderFPS;
		
		Prefs.showMultibox(false);
		Prefs.showTextOverlay(false);

		Component component = bt.viewer.getDisplayComponent();

		Rectangle rect = component.getBounds();
		BufferedImage bi =
                new BufferedImage(rect.width, rect.height,
                                    BufferedImage.TYPE_INT_ARGB);
		float fTimePoint;
		float dT = aPanel.kfAnim.nTotalTime/(float)(nTotFrames-1);
		for(int nFr = 0; nFr<nTotFrames; nFr++)
		{
			setProgress(nFr*100/(nTotFrames-1));
			setProgressState("rendering frames ("+Integer.toString( nFr+1 )+"/"+Integer.toString(nTotFrames)+")");

			fTimePoint = nFr*dT;
			bt.setScene(aPanel.kfAnim.getScene(fTimePoint));
			
			Thread.sleep( 2000 );
//			while(bt.viewer.getRepaintStatus() ! =RepaintType.NONE)
//			{
//				Thread.sleep( 1 );
//			}
	        component.paint(bi.getGraphics());
			//final BufferedImage bi = target.renderResult.getBufferedImage();
			//ImageIO.write( bi, "png", new File( String.format( "%s/img-%03d.png", dir, timepoint ) ) );
			ImageIO.write( bi, "png", new File( aPanel.sRenderSavePath+String.format("%0"+String.valueOf(nTotFrames).length()+"d", nFr)+".png") );
			if(isCancelled())
			{
				return null;	
			}	
		}


		return null;
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
    		String msg = String.format("Unexpected error during animation render: %s", 
    				e.getCause().toString());
    		System.out.println(msg);
    	} 
    	catch (InterruptedException e) 
    	{
    		// Process e here
    	}
    	catch (Exception e)
    	{

    		System.out.println("Animation render interrupted by user.");
        	setProgress(100);	
        	setProgressState("Render interrupted by user.");
    	}	
    	
    	if(butRecord != null && tabIconRecord!= null)
    	{
    		butRecord.setIcon( tabIconRecord );
    		butRecord.setToolTipText( "Render" );

    	}
		Prefs.showMultibox(bSaveMultiBox);
		Prefs.showTextOverlay(bSaveTextOverlay);
    	//unlock user interaction
    	bt.bInputLock = false;
    	bt.setLockMode(false);

    }

	
	
}

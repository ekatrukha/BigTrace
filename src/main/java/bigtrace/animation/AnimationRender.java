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

import bdv.ui.splitpanel.SplitPanel;
import bdv.util.Prefs;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import btbvv.core.render.VolumeRenderer.RepaintType;
import ij.IJ;




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

		bSaveMultiBox = Prefs.showMultibox();
		bSaveTextOverlay = Prefs.showTextOverlay();
		int nTotFrames = aPanel.kfAnim.nTotalTime*aPanel.nRenderFPS;
		
		//Prefs.showMultibox(false);
		Prefs.showTextOverlay(false);


		float fTimePoint;
		float dT = aPanel.kfAnim.nTotalTime/(float)(nTotFrames-1);

		bt.viewer.setRenderMode( true );

		SplitPanel splitPanel = (SplitPanel) bt.viewer.getParent();
		
		if(!splitPanel.isCollapsed())
		{
			splitPanel.setCollapsed( true );
		}
		//check if there is time slider => +25 in height
		//splitPanel.setPreferredSize( new Dimension(300, 300 ));
	
		bt.bvvFrame.pack();	
		bt.bvvFrame.setResizable( false );
		bt.bvvFrame.setEnabled( false );
		
		
		Component component = bt.viewer.getDisplayComponent();
		Rectangle rect = bt.viewer.getDisplayComponent().getBounds();
		BufferedImage bi =
                new BufferedImage(rect.width, rect.height,
                                    BufferedImage.TYPE_INT_ARGB);
		for(int nFr = 0; nFr<nTotFrames; nFr++)
		{
			setProgress(nFr*100/(nTotFrames-1));
			setProgressState("rendering frames ("+Integer.toString( nFr+1 )+"/"+Integer.toString(nTotFrames)+")");

			fTimePoint = nFr*dT;
			bt.setScene(aPanel.kfAnim.getScene(fTimePoint));

			//RepaintType status = bt.viewer.getRepaintStatus();
			long nTotalTime = 0;
			long nWaitTime = 30;
			boolean bWait = (bt.viewer.getRepaintStatus() != RepaintType.NONE);
			//while(bt.viewer.getRepaintStatus() != RepaintType.NONE)
			while(bWait)
			{
				
				//System.out.println(status);
				Thread.sleep( nWaitTime );
				//status = bt.viewer.getRepaintStatus();
				nTotalTime += nWaitTime;
				if(bt.viewer.getRepaintStatus() == RepaintType.NONE)
					{bWait = false;}
				if (nTotalTime>60000)
				{
					bWait = false;
					IJ.log( "Rendering of frame "+Integer.toString( nFr+1 )+" took more than a minute, proceeding with current result." );
				}
				if(isCancelled())
				{
					return null;	
				}	
			}
	        component.paint(bi.getGraphics());
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
    		bt.viewer.setRenderMode( false );
    	} 
    	catch (InterruptedException e) 
    	{
    		// Process e here
    	}
    	catch (Exception e)
    	{

    		System.out.println("Animation render interrupted by user.");
    		bt.viewer.setRenderMode( false );
        	setProgress(100);	
        	setProgressState("Render interrupted by user.");
    	}	
    	
    	bt.viewer.setRenderMode( false );
		bt.bvvFrame.setResizable( true );
		bt.bvvFrame.setEnabled( true );
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

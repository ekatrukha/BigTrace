package bigtrace.animation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.DefaultListModel;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;


public class StorylineLoad < T extends RealType< T > & NativeType< T > > 
{
	
	/** plugin instance **/
	BigTrace<T> bt;
	
	final AnimationPanel< T > aPanel;
	
	public StorylineLoad(final BigTrace<T> bt_,  AnimationPanel< T > aPanel_)
	{
		bt = bt_;	
		aPanel = aPanel_;
	}
	public void loadAnimation(String sFilename)
	{
		String[] line_array;
		DefaultListModel<KeyFrame> listModel = new  DefaultListModel<>();
		KeyFrameAnimation<T> kfAnim = aPanel.kfAnim;
		
		KeyFrame kfLoad = new KeyFrame("loading");
		
		try ( BufferedReader br = new BufferedReader(new FileReader(sFilename));) 
		{
			 String line = br.readLine();
			 line_array = line.split(",");
			 if(!line_array[0].equals("BigTrace_StoryLine"))
			 {
				 System.err.println("Not a BigTrace storyline file format, aborting");
				 return;
			 }
			 if(!line_array[2].equals(BigTraceData.sVersion))
			 {
				 System.out.println("Version mismatch: storyline file "+line_array[2]+", plugin "+BigTraceData.sVersion+". It should be fine in theory, so loading anyway.");
			 }
			  
		     line = br.readLine();
			 while (line != null) 
			 {				
				 // process the line.
				 line_array = line.split(",");
				 switch (line_array[0])
				 {
				 case "TotalTime":
					 kfAnim.keyFrames.clear();
					 aPanel.nfTotalTime.setText( line_array[1] );
					 aPanel.setNewTotalTime( Integer.parseInt(line_array[1]) );	
					 break;
				 
				 case "KeyFrameName":
					 kfLoad = new KeyFrame(line_array[1]);
					 break;
				 case "KeyFrameTime":
					 kfLoad.setMovieTimePoint( Float.parseFloat(  line_array[1] ));
					 Scene scLoad = new Scene();
					 //time frame
					 line = br.readLine();
					 line_array = line.split(",");
					 scLoad.setTimeFrame(  Integer.parseInt( line_array[1] ) );
					 //ViewTransform
					 line = br.readLine();
					 line_array = line.split(",");
					 final double [] transform = new double [12];
					 for(int m=0;m<12;m++)
					 {
						 transform[m] = Double.parseDouble( line_array[m+1] );
					 }
					 scLoad.setViewerTransform( transform );
					 long[][] clipBox = new long [2][3];
					 //ClipBox min/max
					 for (int ind =0; ind<2; ind++)
					 {
						 line = br.readLine();
						 line_array = line.split(",");
						 for (int m = 0; m<3; m++)
						 {
							 clipBox[ind][m] = Long.parseLong( line_array[m+1] );
						 }
					 }
					 scLoad.setClipBox( clipBox );
					 kfLoad.setScene( scLoad );
					 listModel.addElement( kfLoad );
					 break;
				 }
				 line = br.readLine(); 
			 }
			 br.close();
		}
		catch ( FileNotFoundException exc )
		{
			System.err.print(exc.getMessage());
			exc.printStackTrace();
		}
		catch ( IOException exc )
		{
			System.err.print(exc.getMessage());
			exc.printStackTrace();
		}
		//let's update everything
		aPanel.kfAnim.setListModel( listModel );
		aPanel.updateKeyIndices();
		aPanel.updateKeyMarks();
		kfAnim.updateTransitionTimeline();
		
		return;
	}
}

package bigtrace.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import ij.IJ;

public class ViewsIO
{

	/** saving current view as csv **/
	public static < T extends RealType< T > & NativeType< T > > void saveView(final BigTrace<T> bt, String sFilename)
	{		
		bt.bInputLock = true;
		bt.setLockMode(true);
        try {
			final File file = new File(sFilename);
			
			try (FileWriter writer = new FileWriter(file))
			{
				writer.write("BigTrace_View,version," + BigTraceData.sVersion + "\n");
				writer.write("End of BigTrace View\n");
				writer.close();
			}

			bt.btPanel.progressBar.setString("saving view done.");
		} catch (IOException e) {	
			IJ.log(e.getMessage());
			//e.printStackTrace();
		}
	}	
	
}

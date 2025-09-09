package bigtrace;

import java.util.ArrayList;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.io.ROIsIO;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.Roi3D;
import bigtrace.volume.StraightenCurve;
import ij.IJ;
import ij.ImageJ;
import ij.macro.ExtensionDescriptor;
import ij.macro.MacroExtension;


public class BigTraceMacro < T extends RealType< T > & NativeType< T > > 
{
	/** plugin instance **/
	BigTrace<T> bt;
	
	/** macro extensions **/
	public ExtensionDescriptor[] extensions;
	
	/** whether we run in the macro mode **/
	public boolean bMacroMode = false;
	
	public BigTraceMacro(final BigTrace<T> bt_)
	{
		bt = bt_;
		
		extensions = new ExtensionDescriptor[7];
		extensions[0] = ExtensionDescriptor.newDescriptor("btLoadROIs", bt, MacroExtension.ARG_STRING, MacroExtension.ARG_STRING);
		extensions[1] = ExtensionDescriptor.newDescriptor("btSaveROIs", bt, MacroExtension.ARG_STRING, MacroExtension.ARG_STRING);
		extensions[2] = ExtensionDescriptor.newDescriptor("btStraighten", bt, MacroExtension.ARG_NUMBER, MacroExtension.ARG_STRING, MacroExtension.ARG_STRING);
		extensions[3] = ExtensionDescriptor.newDescriptor("btShapeInterpolation", bt, MacroExtension.ARG_STRING, MacroExtension.ARG_NUMBER);
		extensions[4] = ExtensionDescriptor.newDescriptor("btIntensityInterpolation", bt, MacroExtension.ARG_STRING);
		extensions[5] = ExtensionDescriptor.newDescriptor("btTest", bt);
		extensions[6] = ExtensionDescriptor.newDescriptor("btClose", bt);
		//extensions[7] = ExtensionDescriptor.newDescriptor("btTest", bt);
	}
	
	public String handleExtension(String name, Object[] args) 
	{
		try
		{
			if (name.equals("btLoadROIs")) 
			{
				macroLoadROIs( (String)args[0],(String)args[1]);
			}
			if (name.equals("btSaveROIs")) 
			{
				macroSaveROIs( (String)args[0],(String)args[1]);
			}
			if (name.equals("btStraighten")) 
			{
				if(args.length == 2)
				{
					//backwards compartibility
					macroStraighten((int)Math.round(((Double)args[0]).doubleValue()), (String)args[1], "Square");					
				}
				else
				{
					macroStraighten((int)Math.round(((Double)args[0]).doubleValue()), (String)args[1], (String)args[2]);
				}
			}
			if (name.equals("btShapeInterpolation")) 
			{
				macroShapeInterpolation( (String)args[0],(int)Math.round(((Double)args[1]).doubleValue()));
			}
			if (name.equals("btIntensityInterpolation")) 
			{
				macroIntensityInterpolation( (String)args[0]);
			}
			if (name.equals("btClose")) 
			{
				macroCloseBT();			
			}
			if (name.equals("btTest")) 
			{
				macroTest();
			} 
		}
		catch ( InterruptedException exc )
		{
			exc.printStackTrace();
		}
		return null;
	}
	
	public void macroLoadROIs(String sFileName, String input) throws InterruptedException
	{
		while(bt.bInputLock)
		{
			Thread.sleep(1000);
		}
		
		//it should be later unlocked by  bt.roiManager.loadROIs
		bt.bInputLock = true;
		
        if(input == null)
        	return;
        int nLoadMode = 0;
        switch (input)
        {
        	case "Clean":
            	nLoadMode = 0;
        		break;
        	case "Append":
        		nLoadMode = 1;
        		break;  
        	default:
        		IJ.log( "Error! ROIs loading mode should be either Clean or Append. Loading failed." );
        		return;
        }
        ROIsIO.loadROIs( sFileName, nLoadMode, bt );
        IJ.log( "BigTrace ROIs loaded from " + sFileName);

	}
	
	public void macroSaveROIs(String sFileName, String output) throws InterruptedException
	{
		while(bt.bInputLock)
		{
			Thread.sleep(1000);
		}
		
		//it should be later unlocked by  bt.roiManager.saveROIs
		bt.bInputLock = true;
		
        if(output == null)
        	return;
        String out = output.toLowerCase();
        int nLoadMode = 0;
        switch (out)
        {
        	case "bigtrace":
            	nLoadMode = 0;
        		break;
        	case "csv":
        		nLoadMode = 1;
        		break;
           	case "swc":
        		nLoadMode = 2;
        		break;
        		
        	default:
        		IJ.log( "Error! ROIs saving mode should be either BigTrace, CSV or SWC. Saving aborted." );
        		return;
        }
        ROIsIO.saveROIs( sFileName, nLoadMode, bt );
        IJ.log( "BigTrace ROIs saved to " + sFileName);

	}
	
	void macroStraighten(final int nStraightenAxis, String sSaveDir, String sShape) throws InterruptedException
	{
		while(bt.bInputLock)
		{
			Thread.sleep(100);
		}
		
		//it should be later unlocked by StraightenCurve,
		//if we call it 
		bt.bInputLock = true;
		
		//build list of ROIs
		final ArrayList<AbstractCurve3D> curvesOut = new ArrayList<>();
		
		for (int nRoi = 0; nRoi<bt.roiManager.rois.size(); nRoi++)
		{
			Roi3D roi = bt.roiManager.rois.get(nRoi);
			if(bt.roiManager.groups.get(roi.getGroupInd()).bVisible)
			{
				if((roi.getType() == Roi3D.LINE_TRACE) || (roi.getType() == Roi3D.POLYLINE))
				{
					curvesOut.add((AbstractCurve3D) roi);
				}
			}
		}
		int nAxis = nStraightenAxis;
		if(nStraightenAxis<0 || nStraightenAxis>2)
		{
			nAxis = 0;
			IJ.log( "First axis parameter should be in the range of 0-2, wher 0 = X axis, 1 = Y axis, 2 = Z axis" );
			IJ.log( "Setting the value to 0, X axis." );
		}
		int nShape = 0 ;
		if(sShape =="Round")
		{
			nShape  = 1;
		}
		if(curvesOut.size()>0)
		{	
			StraightenCurve<T> straightBG = new StraightenCurve<>(curvesOut, bt, -1.0f, nAxis, nShape, 0, 1, sSaveDir);
			straightBG.addPropertyChangeListener(bt.btPanel);
			straightBG.execute();
		}
		else
		{
			IJ.log("Cannot find proper curve ROIs to straighten.");
			bt.btPanel.progressBar.setString("curve straightening aborted.");
			bt.bInputLock = false;
		}
		
	}

	public void macroShapeInterpolation(String sShapeInterpol, int nSmoothWindow) throws InterruptedException
	{
		while(bt.bInputLock)
		{
			Thread.sleep(100);
		}
		bt.bInputLock = true;
		switch (sShapeInterpol)
		{
		case "Voxel":
			BigTraceData.shapeInterpolation = BigTraceData.SHAPE_Voxel;
			IJ.log("BigTrace ROI Shape Interpolation set to Voxel.");
			break;
		case "Smooth":
			BigTraceData.shapeInterpolation = BigTraceData.SHAPE_Smooth;
			IJ.log("BigTrace ROI Shape Interpolation set to Smooth.");
			break;
		case "Spline":
			BigTraceData.shapeInterpolation = BigTraceData.SHAPE_Spline;
			IJ.log("BigTrace ROI Shape Interpolation set to Spline.");
			break;
		default:
			IJ.log( "Error! ROI Shape Interpolation values should be either Voxel, Smooth or Spline." );
			return;
		}
		BigTraceData.nSmoothWindow = Math.max( 1, Math.abs( Math.round( nSmoothWindow ) ));
		IJ.log("BigTrace ROI smoothing window set to "+Integer.toString( BigTraceData.nSmoothWindow )+".");
		bt.roiManager.updateROIsDisplay();
		bt.bInputLock = false;
	}
	
	void macroIntensityInterpolation(String sInterpol) throws InterruptedException
	{
		while(bt.bInputLock)
		{
			Thread.sleep(100);
		}
		bt.bInputLock = true;
		switch (sInterpol)
		{
		case "Neighbor":
			BigTraceData.intensityInterpolation = BigTraceData.INT_NearestNeighbor;
			IJ.log("BigTrace Intensity Interpolation set to Nearest Neighbor.");
			break;
		case "Linear":
			BigTraceData.intensityInterpolation = BigTraceData.INT_NLinear;
			IJ.log("BigTrace Intensity Interpolation set to Linear.");
			break;
		case "Lanczos":
			BigTraceData.intensityInterpolation = BigTraceData.INT_Lanczos;
			IJ.log("BigTrace Intensity Interpolation set to Lanczos.");
			break;
		default:
			IJ.log( "Error! Intensity interpolation values should be either Nearest, Linear or Lanczos." );
			return;
		}
		bt.btData.setInterpolationFactory();
		bt.bInputLock = false;
	}

	
	void macroCloseBT() throws InterruptedException
	{
		while(bt.bInputLock)
		{
			Thread.sleep(100);
		}
		bt.closeWindows();
		IJ.log("BigTrace closed.");
	}
	
	void macroTest() throws InterruptedException
	{
		while(bt.bInputLock)
		{
		  IJ.log( "not unlocked" );
		  Thread.sleep(100);
		}
		IJ.log( "unlocked" );
		bt.resetViewXY();
		IJ.log("test ok right away");

	}
	
	@SuppressWarnings("rawtypes")
	public static void main(String... args) throws Exception
	{
		
		new ImageJ();
		BigTrace testI = new BigTrace(); 
		
		testI.run("/home/eugene/Desktop/projects/BigTrace/BigTrace_data/ExM_MT.tif");
		try
		{
			testI.btMacro.bMacroMode = true;
			testI.btMacro.macroLoadROIs("/home/eugene/Desktop/FR21_SC_nuc10-1.tif_btrois.csv","Clean");
			testI.btMacro.macroSaveROIs("/home/eugene/Desktop/FR21_SC_nuc10-1.tif_btrois.swc","SWC");
		}
		catch ( Exception exc )
		{
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
	}
}

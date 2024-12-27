package bigtrace.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bdv.viewer.ConverterSetupBounds;
import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bvvpg.pguitools.ConverterSetupBoundsAlpha;
import bvvpg.pguitools.ConverterSetupBoundsGamma;
import bvvpg.pguitools.ConverterSetupBoundsGammaAlpha;
import bvvpg.pguitools.ConverterSetupsPG;
import bvvpg.pguitools.RealARGBColorGammaConverterSetup;
import ij.IJ;

public class ViewsIO
{

	/** saving current view as csv **/
	public static < T extends RealType< T > & NativeType< T > > void saveView(final BigTrace<T> bt, String sFilename)
	{		
		bt.bInputLock = true;
		bt.setLockMode(true);
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		DecimalFormat df3 = new DecimalFormat ("#.#####", symbols);
        try {
			final File file = new File(sFilename);
			
			try (FileWriter writer = new FileWriter(file))
			{
				writer.write("BigTrace_View,version," + BigTraceData.sVersion + "\n");
				writer.write("FileNameFull,"+bt.btData.sFileNameFullImg+"\n");
				writer.write("Render Type,"+Integer.toString( bt.btData.nRenderMethod )+"\n");
				writer.write("Canvas BG color,"+Integer.toString( bt.btData.canvasBGColor.getRGB() )+"\n");
				writer.write("Camera,"+df3.format(bt.btData.dCam )+","+df3.format(bt.btData.dClipNear )+","+df3.format(bt.btData.dClipFar )+"\n");
				writer.write("Zoom Box Size,"+Integer.toString( bt.btData.nZoomBoxSize)+"\n");
				writer.write("Zoom Box Fraction,"+df3.format(bt.btData.dZoomBoxScreenFraction)+"\n");
				writer.write("bROIDoubleClickClip,"+Integer.toString( BigTraceData.bROIDoubleClickClip ? 1 : 0 )+"\n");
				writer.write("nROIDoubleClickClipExpand,"+Integer.toString( BigTraceData.nROIDoubleClickClipExpand )+"\n");
				
				writer.write("Intensity Interpolation,"+Integer.toString( BigTraceData.intensityInterpolation )+"\n");
				writer.write("ROI Shape Interpolation,"+Integer.toString( BigTraceData.shapeInterpolation )+"\n");
				writer.write("Rotation min frame type,"+Integer.toString( BigTraceData.rotationMinFrame )+"\n");
				writer.write("Smooth window,"+Integer.toString( BigTraceData.nSmoothWindow )+"\n");
				writer.write("Sector number,"+Integer.toString( BigTraceData.sectorN )+"\n");
				writer.write("wireCountourStep,"+Integer.toString( BigTraceData.wireCountourStep )+"\n");
				writer.write("crossSectionGridStep,"+Integer.toString( BigTraceData.crossSectionGridStep )+"\n");
				writer.write("surfaceRender,"+Integer.toString( BigTraceData.surfaceRender )+"\n");
				writer.write("silhouetteRender,"+Integer.toString( BigTraceData.silhouetteRender )+"\n");
				writer.write("wireAntiAliasing,"+Integer.toString( BigTraceData.wireAntiAliasing ? 1 : 0)+"\n");
				writer.write("timeRender,"+Integer.toString( BigTraceData.timeRender )+"\n");
				writer.write("timeFade,"+Integer.toString( BigTraceData.timeFade )+"\n");
				writer.write("Scene, current\n");
				bt.getCurrentScene().save( writer );
				int nSourcesN = bt.bvv_sources.size();
				writer.write("Sources number," + Integer.toString(nSourcesN)+"\n");
				ConverterSetupsPG convSet = (ConverterSetupsPG) bt.bvv_main.getBvvHandle().getConverterSetups();
				final ConverterSetupBounds boundsMap = convSet.getBounds();
				final ConverterSetupBoundsGamma boundsGamma = convSet.getBoundsGamma();
				final ConverterSetupBoundsAlpha boundsAlpha = convSet.getBoundsAlpha();
				final ConverterSetupBoundsGammaAlpha boundsAlphaGamma = convSet.getBoundsGammaAlpha();
				for (int i=0; i<nSourcesN; i++)
				{
					writer.write("Source,"+Integer.toString(i+1)+"\n");
					RealARGBColorGammaConverterSetup converter = (RealARGBColorGammaConverterSetup)bt.bvv_sources.get( i ).getConverterSetups().get( 0 );
					
					writer.write("Display bounds," + df3.format(boundsMap.getBounds( converter ).getMinBound())
								  + "," + df3.format(boundsMap.getBounds( converter ).getMaxBound()) + "\n");
					writer.write("Display range," + df3.format(converter.getDisplayRangeMin())
					  + "," + df3.format(converter.getDisplayRangeMax()) + "\n");
					
					writer.write("Gamma bounds," + df3.format(boundsGamma.getBounds( converter ).getMinBound())
					  + "," + df3.format(boundsGamma.getBounds( converter ).getMaxBound()) + "\n");					
					writer.write("Gamma," + df3.format(converter.getDisplayGamma()) + "\n");
					
					writer.write("Alpha bounds," + df3.format(boundsAlpha.getBounds( converter ).getMinBound())
					  + "," + df3.format(boundsAlpha.getBounds( converter ).getMaxBound()) + "\n");	
					writer.write("Alpha range," + df3.format(converter.getAlphaRangeMin())
					  + "," + df3.format(converter.getAlphaRangeMax()) + "\n");
					
					writer.write("Alpha Gamma bounds," + df3.format(boundsAlphaGamma.getBounds( converter ).getMinBound())
					  + "," + df3.format(boundsAlphaGamma.getBounds( converter ).getMaxBound()) + "\n");	
					writer.write("Alpha Gamma," + df3.format(converter.getAlphaGamma()) + "\n");
					
					writer.write("Color," + Integer.toString(converter.getColor().get()) + "\n");

					writer.write("LUT size," + Integer.toString(converter.getLUTSize()) + "\n");
					writer.write("LUT," + converter.getLUTName() + "\n");
					
					if(converter.clipActive())
					{
						AffineTransform3D clipTransform = converter.getClipTransform();
						final double [] transform = new double [12];
						clipTransform.toArray(transform);
						writer.write("ClipTransform");
						for (int m = 0; m<12; m++)
						{
							writer.write("," + df3.format(transform[m]));
						}
						writer.write("\n");
					}
					writer.write("Voxel interpolation," + Integer.toString(converter.getVoxelRenderInterpolation()) + "\n");
				}
				writer.write("End of BigTrace View\n");
				writer.close();
			}

			bt.btPanel.progressBar.setString("saving view done.");
		} catch (IOException e) {	
			IJ.log(e.getMessage());
			//e.printStackTrace();
		}
		bt.bInputLock = false;
		bt.setLockMode(false);
	}	
	/** loading current view from csv **/
	public static < T extends RealType< T > & NativeType< T > > void loadView(final BigTrace<T> bt, String sFilename)
	{
		
	}
}

package tests;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;

import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;
import io.scif.img.SCIFIOImgPlus;

import javax.swing.UIManager;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;


public class testScifioWriter
{
	
	public static void main( final String[] args )
	{
		
		//switch to FlatLaf theme		
		//to show error message 
		try 
		{
		    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		    FlatLaf.registerCustomDefaultsSource( "flatlaf" );
		    FlatIntelliJLaf.setup();
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		

		String saveFolder = "/home/eugene/Desktop/";
		
		String sPathOutTif4d = saveFolder + "test4d.tif";
		String sPathOutTif4dext = saveFolder + "test4dext.tif";
		String sPathOutTif4dextMeta = saveFolder + "test4dextMeta.tif";
		
		long [] dim4D = new long [4];
		long [] dim4Dext = new long [4];
		
		for(int d=0; d<3;d++)
		{
			dim4D[d] = 2;
			dim4Dext[d] = 50;
		}
		dim4D[3] = 2;
		dim4Dext[3] = 2;
		
		ArrayImg<UnsignedByteType, ByteArray> img4D = ArrayImgs.unsignedBytes( dim4D);
		
		AxisType[] axisTypes4D = new AxisType[]{ Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL};

		final SCIFIOConfig config = new SCIFIOConfig();
		ImgSaver saver = new ImgSaver();
		Img< UnsignedByteType > imgViewWrap4D = ImgView.wrap(Views.interval( Views.extendZero( img4D ),new long[4],dim4Dext));

		//SCIFIOImgPlus 
		SCIFIOImgPlus<?> scImg4D = new SCIFIOImgPlus<>(imgViewWrap4D,"test4D",axisTypes4D);

		//without SCIFIOImgPlus there are no errors, but channel information is lost
		saver.saveImg( sPathOutTif4d, img4D, config );
		//without SCIFIOImgPlus there are no errors, but channel information is lost
		saver.saveImg( sPathOutTif4dext, imgViewWrap4D, config );
		//here gives an error 
		//Source dataset contains: 153 planes, but writer format only supports 3
		saver.saveImg( sPathOutTif4dextMeta, scImg4D, config );
		 
	}
}

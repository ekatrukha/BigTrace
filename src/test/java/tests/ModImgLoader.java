package tests;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import bdv.img.imagestack.ImageStackImageLoader;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;

public class ModImgLoader < T > implements TypedBasicImgLoader<T>
{

	final TypedBasicImgLoader< ? > imgLoader;
	final TypedBasicImgLoader< ? > imgLoader2;
	@Override
	public BasicSetupImgLoader< T > getSetupImgLoader( int setupId )
	{
		if(setupId==0)
		{
			return   ( BasicSetupImgLoader< T > ) imgLoader.getSetupImgLoader( 0 );
		}
		return   ( BasicSetupImgLoader< T > ) imgLoader2.getSetupImgLoader( 0 );
	}
	public ModImgLoader()
	{
		ImagePlus imp = IJ.openImage("/home/eugene/Desktop/testbdv/100x100x1fr.tif");
		imgLoader = ImageStackImageLoader.createUnsignedShortInstance( imp );
		ImagePlus imp2 = IJ.openImage("/home/eugene/Desktop/testbdv/50x50x1fr.tif");
		imgLoader2 = ImageStackImageLoader.createUnsignedShortInstance( imp2 );
	}
}

package bigtrace.io;

import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import ch.epfl.biop.bdv.img.OpenerSetupLoader;
import ch.epfl.biop.bdv.img.ResourcePool;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsArrayLoaders;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsOpener;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsSetupLoader;
import loci.formats.IFormatReader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

public class BioFormatsSetupLoaderBT <T extends NumericType<T> & NativeType<T>, V extends Volatile<T> & NumericType<V> & NativeType<V>, A extends DataAccess>
extends OpenerSetupLoader<T,V,A>
{
private static final Logger logger = LoggerFactory.getLogger(BioFormatsSetupLoader.class);

// -------- How to load an image
private final Function<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> cvtRaiToFloatRai;
private final Converter<T, FloatType> cvt;
private final ResourcePool<IFormatReader> readerPool;
private final Supplier<VolatileGlobalCellCache> cacheSupplier;
private final CacheArrayLoader<A> loader;

// -------- Resolution levels
private final double[][] mmResolutions;
private final int[] cellDimensions;
private final int numMipmapLevels;

// Channel index
private final int iChannel;

// -------- ViewSetup
private final int setup;

// Image dimension
private final Dimensions[] dimensions;


// Voxel physical dimensions
private final VoxelDimensions voxelsDimensions;

@SuppressWarnings("unchecked")
protected BioFormatsSetupLoaderBT(BioFormatsOpenerBT opener,
							 int channelIndex, int iSeries, int setup, T t, V v,
							 Supplier<VolatileGlobalCellCache> cacheSupplier) {
	super(t, v);
	this.setup = setup;
	this.cacheSupplier = cacheSupplier;
	this.readerPool = opener.getPixelReader();

	// set RandomAccessibleInterval
	if (t instanceof FloatType) {
		cvt = null;
		cvtRaiToFloatRai = null; // rai -> (RandomAccessibleInterval<FloatType>)
															// ((Object) rai); // Nothing to be done
	}
	else if (t instanceof ARGBType) {
		// Average of RGB value
		cvt = (input, output) -> {
			int val = ((ARGBType) input).get();
			int r = ARGBType.red(val);
			int g = ARGBType.green(val);
			int b = ARGBType.blue(val);
			output.set(r + g + b);
		};
		cvtRaiToFloatRai = rai -> Converters.convert(rai, cvt, new FloatType());
	}
	else if (t instanceof AbstractIntegerType) {
		cvt = (input, output) -> output.set(((AbstractIntegerType) input)
			.getRealFloat());
		cvtRaiToFloatRai = rai -> Converters.convert(rai, cvt, new FloatType());
	}
	else {
		cvt = null;
		cvtRaiToFloatRai = e -> {
			logger.error("Conversion of " + t.getClass() +
				" to FloatType unsupported.");
			return null;
		};
	}

	// channel options
	iChannel = channelIndex;

	// pixels characteristics
	boolean isLittleEndian = opener.isLittleEndian();
	voxelsDimensions = opener.getVoxelDimensions();

	// image dimensions
	dimensions = opener.getDimensions();

	// resolution levels and dimensions
	numMipmapLevels = opener.getNumMipmapLevels();
	cellDimensions = opener.getCellDimensions(0);
	mmResolutions = new double[numMipmapLevels][3];
	mmResolutions[0][0] = 1;
	mmResolutions[0][1] = 1;
	mmResolutions[0][2] = 1;

	// compute mipmap levels
	// Fix VSI specific issue see https://forum.image.sc/t/qupath-omero-weird-pyramid-levels/65484
	if (opener.getImageFormat().equals("CellSens VSI")) {
		for (int iLevel = 1; iLevel < numMipmapLevels; iLevel++) {
			double downscalingFactor = Math.pow(2, iLevel);
			mmResolutions[iLevel][0] = downscalingFactor;
			mmResolutions[iLevel][1] = downscalingFactor;
			mmResolutions[iLevel][2] = 1;
		}
	}
	else {
		int[] srcL0dims = new int[] { (int) dimensions[0].dimension(0),
									  (int) dimensions[0].dimension(1),
									  (int) dimensions[0].dimension(2) };
		for (int iLevel = 1; iLevel < numMipmapLevels; iLevel++) {
			int[] srcLidims = new int[] { (int) dimensions[iLevel].dimension(0),
										  (int) dimensions[iLevel].dimension(1),
					                      (int) dimensions[iLevel].dimension(2) };
			mmResolutions[iLevel][0] = (double) srcL0dims[0] / (double) srcLidims[0];
			mmResolutions[iLevel][1] = (double) srcL0dims[1] / (double) srcLidims[1];
			mmResolutions[iLevel][2] = (double) srcL0dims[2] / (double) srcLidims[2];
		}
	}

	// get the ArrayLoader corresponding to the pixelType
	if (t instanceof UnsignedByteType) {
		loader =
			(CacheArrayLoader<A>) new BioFormatsArrayLoadersBT.BioFormatsUnsignedByteArrayToShortLoader(
				readerPool, iChannel, iSeries);
	}
	else if (t instanceof UnsignedShortType) {
		loader =
			(CacheArrayLoader<A>) new BioFormatsArrayLoadersBT.BioFormatsUnsignedByteArrayToShortLoader(
				readerPool, iChannel, iSeries);
	}
	else if (t instanceof FloatType) {
		loader =
			(CacheArrayLoader<A>) new BioFormatsArrayLoadersBT.BioFormatsFloatArrayLoader(
				readerPool, iChannel, iSeries, isLittleEndian);
	}
	else if (t instanceof IntType) {
		loader =
			(CacheArrayLoader<A>) new BioFormatsArrayLoadersBT.BioFormatsIntArrayLoader(
				readerPool, iChannel, iSeries, isLittleEndian);
	}
	else if (t instanceof ARGBType) {
		loader =
			(CacheArrayLoader<A>) new BioFormatsArrayLoadersBT.BioFormatsRGBArrayLoader(
				readerPool, iChannel, iSeries);
	}
	else {
		throw new UnsupportedOperationException("Pixel type " + t.getClass()
			.getName() + " unsupported in " + BioFormatsSetupLoader.class
				.getName());
	}
}

@Override
public RandomAccessibleInterval<FloatType> getFloatImage(int timepointId,
	int level, boolean normalize, ImgLoaderHint... hints)
{
	return cvtRaiToFloatRai.apply(getImage(timepointId, level));
}

@Override
public Dimensions getImageSize(int timepointId, int level) {
	return dimensions[level];
}

@Override
public RandomAccessibleInterval<T> getImage(int timepointId, int level,
	ImgLoaderHint... hints)
{
	final long[] dims = dimensions[level].dimensionsAsLongArray();
	final int[] cellDimensions = this.cellDimensions;
	final CellGrid grid = new CellGrid(dims, cellDimensions);

	final int priority = this.numMipmapLevels - level;
	final CacheHints cacheHints = new CacheHints(LoadingStrategy.BLOCKING,
		priority, false);

	return cacheSupplier.get().createImg(grid, timepointId, setup, level,
		cacheHints, loader, type);
}

@Override
public RandomAccessibleInterval<V> getVolatileImage(int timepointId,
	int level, ImgLoaderHint... hints)
{
	final long[] dims = dimensions[level].dimensionsAsLongArray();
	final int[] cellDimensions = this.cellDimensions;
	final CellGrid grid = new CellGrid(dims, cellDimensions);

	final int priority = this.numMipmapLevels - level;
	final CacheHints cacheHints = new CacheHints(LoadingStrategy.BUDGETED,
		priority, false);

	return cacheSupplier.get().createImg(grid, timepointId, setup, level,
		cacheHints, loader, volatileType);
}

@Override
public double[][] getMipmapResolutions() {
	return mmResolutions;
}

@Override
public AffineTransform3D[] getMipmapTransforms() {
	AffineTransform3D[] ats = new AffineTransform3D[numMipmapLevels];

	for (int iLevel = 0; iLevel < numMipmapLevels; iLevel++) {
		AffineTransform3D at = new AffineTransform3D();
		at.scale(mmResolutions[iLevel][0], mmResolutions[iLevel][1],
			mmResolutions[iLevel][2]);
		ats[iLevel] = at;
	}

	return ats;
}

@Override
public int numMipmapLevels() {
	return numMipmapLevels;
}

@Override
public RandomAccessibleInterval<FloatType> getFloatImage(int timepointId,
	boolean normalize, ImgLoaderHint... hints)
{
	return cvtRaiToFloatRai.apply(getImage(timepointId, 0));
}

@Override
public Dimensions getImageSize(int timepointId) {
	return getImageSize(0, 0);
}

@Override
public VoxelDimensions getVoxelSize(int timepointId) {
	return voxelsDimensions;
}

}

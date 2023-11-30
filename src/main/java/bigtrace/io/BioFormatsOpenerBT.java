package bigtrace.io;

import static ch.epfl.biop.bdv.img.opener.OpenerHelper.memoize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.img.cache.VolatileGlobalCellCache;
import ch.epfl.biop.bdv.img.OpenerSetupLoader;
import ch.epfl.biop.bdv.img.ResourcePool;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsHelper;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsOpener;
import ch.epfl.biop.bdv.img.bioformats.entity.FileName;
import ch.epfl.biop.bdv.img.bioformats.entity.SeriesIndex;
import ch.epfl.biop.bdv.img.opener.ChannelProperties;
import ch.epfl.biop.bdv.img.opener.Opener;
import ch.epfl.biop.bdv.img.opener.OpenerHelper;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.Memoizer;
import loci.formats.meta.IMetadata;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.model.enums.PixelType;

public class BioFormatsOpenerBT  implements Opener<IFormatReader> {

	final protected static Logger logger = LoggerFactory.getLogger(
		BioFormatsOpener.class);

	// -------- How to open the dataset (reader pool, transforms)
	//protected Consumer<IFormatReader> readerModifier = (e) -> {};
	private final ReaderPool pool;
	// private AffineTransform3D rootTransform;

	// -------- Opener core options
	private final int nTimePoints;
	// private String imageName;
	private final String format;
	private final IMetadata omeMeta;
	private final String dataLocation;

	// -------- Pixels characteristics
	private final boolean isLittleEndian;
	private final boolean isRGB;
	private final VoxelDimensions voxelDimensions;
	private final Type<? extends NumericType<?>> t;

	// -------- Resolutions options
	private final int[] cellDimensions;
	private final int nMipMapLevels;

	// -------- Image dimensions
	private final Dimensions[] dimensions;

	// -------- Channel options and properties
	//private List<ChannelProperties> channelPropertiesList;
	private final boolean splitRGBChannels;
	private final int nChannels;

	// -------- Series
	private final int iSerie;

	// --------
	private final String rawPixelDataKey;
	private final String filename;
	private final int idxFilename;
	private final OpenerMeta meta;

	IFormatReader model;

	/**
	 *
	 * @param context
	 * @param dataLocation
	 * @param iSerie
	 * @param positionPreTransformMatrixArray
	 * @param positionPostTransformMatrixArray
	 * @param positionIsImageCenter
	 * @param defaultSpaceUnit
	 * @param defaultVoxelUnit
	 * @param unit
	 * @param poolSize
	 * @param useDefaultXYBlockSize
	 * @param cacheBlockSize
	 * @param splitRGBChannels
	 * @param cachedObjects
	 * @param defaultNumberOfChannels
	 * @param skipMeta
	 * @throws Exception
	 */
	public BioFormatsOpenerBT(
			Context context, // not used
			// opener core option
			String dataLocation,
			int iSerie,
			// Location of the image
			double[] positionPreTransformMatrixArray,
			double[] positionPostTransformMatrixArray,
			boolean positionIsImageCenter,
			// units
			Length defaultSpaceUnit,
			Length defaultVoxelUnit,
			String unit,
			// How to stream it
			int poolSize,
			boolean useDefaultXYBlockSize,
			int[] cacheBlockSize,
			// channel options
			boolean splitRGBChannels,
			// Optimisation : reuse from existing openers
			Map<String, Object> cachedObjects,
			int defaultNumberOfChannels,
			boolean skipMeta
	) throws Exception {

		if (iSerie<0) throw new IllegalStateException("Invalid series number for file "+dataLocation+" iSerie = "+iSerie+" requested");

		this.dataLocation = dataLocation;
		this.iSerie = iSerie;
		this.splitRGBChannels = splitRGBChannels;

		// Should be unique to raw pixel data, we don't care if the units are different
		String buildRawPixelDataKey = "opener.bioformats"
						+"."+splitRGBChannels
						+"."+dataLocation
						+"."+iSerie;

		if (!useDefaultXYBlockSize) {
			buildRawPixelDataKey += "."+ Arrays.toString(cacheBlockSize);
		}

		this.rawPixelDataKey = buildRawPixelDataKey;

		this.filename = new File(dataLocation).getName();
		Integer currentIndexFilename = memoize("opener.bioformats.currentfileindex", cachedObjects, () -> 0);
		this.idxFilename = memoize("opener.bioformats.fileindex."+dataLocation, cachedObjects, () -> {
			cachedObjects.put("opener.bioformats.currentfileindex", currentIndexFilename + 1 );
			return currentIndexFilename;
		});

		this.model = this.getNewReader();
		this.pool = memoize("opener.bioformats."+splitRGBChannels+"."+dataLocation,
				cachedObjects,
				() -> new ReaderPool(poolSize, true,
						this::getNewReader, model));

		{ // Indentation just for the pool / recycle operation -> force limiting the scope of reader
			IFormatReader reader = pool.takeOrCreate();
			reader.setSeries(iSerie);
			this.omeMeta = (IMetadata) reader.getMetadataStore();
			this.nChannels = this.omeMeta.getChannelCount(iSerie);
			this.nMipMapLevels = reader.getResolutionCount();
			this.nTimePoints = reader.getSizeT();

			Unit<Length> u = BioFormatsHelper.getUnitFromString(unit);
			if (u == null) {
				logger.error("Could not find matching length unit from String: "+unit);
				u = UNITS.REFERENCEFRAME;
			}

			this.voxelDimensions = BioFormatsHelper.getSeriesVoxelDimensions(this.omeMeta,
					this.iSerie, u, defaultVoxelUnit);
			this.isLittleEndian = reader.isLittleEndian();
			this.isRGB = reader.isRGB();
			this.format = reader.getFormat();

			this.cellDimensions = new int[] {
					useDefaultXYBlockSize ? reader.getOptimalTileWidth() : cacheBlockSize[0],
					useDefaultXYBlockSize ? reader.getOptimalTileHeight() : cacheBlockSize[1],
					useDefaultXYBlockSize ? 1 : cacheBlockSize[2] };

			this.dimensions = new Dimensions[this.nMipMapLevels];
			for (int level = 0; level < this.nMipMapLevels; level++) {
				reader.setResolution(level);
				this.dimensions[level] = getDimension(reader.getSizeX(), reader.getSizeY(), reader.getSizeZ());
			}
			pool.recycle(reader);
		}

		this.t = BioFormatsOpenerBT.getBioformatsBdvSourceType(this.omeMeta.getPixelsType(iSerie), this.isRGB, iSerie);

		if (!skipMeta) {
			AffineTransform3D positionPreTransform = null;
			if (positionPreTransformMatrixArray != null) {
				positionPreTransform = new AffineTransform3D();
				positionPreTransform.set(positionPreTransformMatrixArray);
			}

			AffineTransform3D positionPostTransform = null;
			if (positionPostTransformMatrixArray != null) {
				positionPostTransform = new AffineTransform3D();
				positionPostTransform.set(positionPostTransformMatrixArray);
			}

			AffineTransform3D voxSizePreTransform = new AffineTransform3D();

			AffineTransform3D voxSizePostTransform = new AffineTransform3D();
		

			AffineTransform3D rootTransform = BioFormatsHelper.getSeriesRootTransform(
					this.omeMeta, 
					model, 
					iSerie, 
					BioFormatsHelper.getUnitFromString(unit),
					// Bioformats location fix
					positionPreTransform, positionPostTransform, 
					defaultSpaceUnit,
					positionIsImageCenter,
					//Bioformats voxSize fix
					voxSizePreTransform, voxSizePostTransform, defaultVoxelUnit,
					new boolean[]{false, false, false});

			

			String imageName = getImageName(this.omeMeta,iSerie,dataLocation);
			List<ChannelProperties> channelPropertiesList = getChannelProperties(this.omeMeta, iSerie, this.nChannels);

			meta = new OpenerMeta() {

				@Override
				public ChannelProperties getChannel(int iChannel) {
					if(iChannel >= nChannels) {
						logger.error("You are trying to get the channel " + iChannel + " in an image with only " + nChannels);
						return null;
					}
					return channelPropertiesList.get(iChannel);
				}

				@Override
				public List<Entity> getEntities(int iChannel) {
					ArrayList<Entity> entityList = new ArrayList<>();
					entityList.add(new FileName(idxFilename, filename));
					entityList.add(new SeriesIndex(iSerie));
					return entityList;
				}

				@Override
				public String getImageName() {
					return imageName;
				}

				@Override
				public AffineTransform3D getTransform() {
					return rootTransform;
				}

			};
		} else meta = null;
	}

	/**
	 * Build a channelProperties object for each image channel.
	 * @param omeMeta = image metadata
	 * @param iSerie : serie ID
	 * @param nChannels : number of channel
	 * @return list of ChannelProperties objects
	 */
	private List<ChannelProperties> getChannelProperties(IMetadata omeMeta, int iSerie, int nChannels){
		List<ChannelProperties> channelPropertiesList = new ArrayList<>();
		for(int i = 0; i < nChannels; i++){
			channelPropertiesList.add(new ChannelProperties(i)
					.setNChannels(nChannels)
					.setChannelName(iSerie,omeMeta)
					.setEmissionWavelength(iSerie,omeMeta)
					.setExcitationWavelength(iSerie,omeMeta)
					.setChannelColor(iSerie,omeMeta)
					.setRGB(this.isRGB)
					.setPixelType(this.t)
					.setDisplayRange(0,255)
			);

		}
		return channelPropertiesList;
	}


	/**
	 *
	 * @param omeMeta = image metadata
	 * @param iSerie : serie ID
	 * @param dataLocation : path of the image
	 * @return the name of the image with the serie ID and without extension.
	 */
	private static String getImageName(IMetadata omeMeta, int iSerie, String dataLocation){
		String imageName = omeMeta.getImageName(iSerie);
		String fileNameWithoutExtension = FilenameUtils.removeExtension(new File(dataLocation).getName());
		fileNameWithoutExtension = fileNameWithoutExtension.replace(".ome", ""); // above only removes .tif

		if (imageName == null || imageName.equals("")) {
			imageName = fileNameWithoutExtension;
			return imageName + "-s" + iSerie;
		}
		else {
			return imageName;
		}
	}


	/**
	 * Build a new IFormatReader to retrieve all pixels and channels information of an image opened
	 * with BioFormats.
	 * <p>
	 * Be careful : calling this method can take some time.
	 *
	 * @return the reader
	 */
	public IFormatReader getNewReader() {
		logger.debug("Getting new reader for " + dataLocation);
		IFormatReader reader = new ImageReader();
		reader.setFlattenedResolutions(false);
		if (splitRGBChannels) {
			reader = new ChannelSeparator(reader);
		}
		Memoizer memo = new Memoizer(reader);
		try {
			memo.setId(dataLocation);
		}
		catch (FormatException | IOException e) {
			e.printStackTrace();
		}
		return memo;
	}

	/**
	 *
	 * @param pt bioformats compatible pixel type
	 * @param isReaderRGB is pixel RGB
	 * @param image_index
	 * @return the bdv compatible pixel type
	 * @throws UnsupportedOperationException
	 */
	private static Type<? extends NumericType<?>> getBioformatsBdvSourceType(PixelType pt, boolean isReaderRGB,
																		  int image_index) throws UnsupportedOperationException
	{
		if (isReaderRGB) {
			if (pt == PixelType.UINT8) {
				return new ARGBType();
			}
			else {
				throw new UnsupportedOperationException("Unhandled 16 bits RGB images");
			}
		}
		else {
			if (pt == PixelType.UINT8) {
				return new UnsignedShortType();
				//return new UnsignedByteType();
			}
			if (pt == PixelType.UINT16) {
				return new UnsignedShortType();
			}
			if (pt == PixelType.INT32) {
				return new IntType();
			}
			if (pt == PixelType.FLOAT) {
				return new FloatType();
			}
		}
		throw new UnsupportedOperationException("Unhandled pixel type for serie " +
				image_index + ": " + pt);
	}

	// GETTERS
	@Override
	public String getImageFormat() {return this.format;}

	/**
	 * @param sizeX
	 * @param sizeY
	 * @param sizeZ
	 * @return image dimensions
	 */
	static Dimensions getDimension(long sizeX, long sizeY, long sizeZ) {
		return new Dimensions() {

			@Override
			public long dimension(int d) {
				if (d == 0) return sizeX;
				if (d == 1) return sizeY;
				return sizeZ;
			}

			@Override
			public int numDimensions() {
				return 3;
			}
		};
	}

	// OVERRIDDEN METHODS
	@Override
	public int getNumMipmapLevels() {
		return this.nMipMapLevels;
	}

	@Override
	public int getNTimePoints() {
		return this.nTimePoints;
	}

	@Override
	public ResourcePool<IFormatReader> getPixelReader() {
		return this.pool;
	}

	@Override
	public VoxelDimensions getVoxelDimensions() {
		return this.voxelDimensions;
	}

	@Override
	public boolean isLittleEndian() {
		return this.isLittleEndian;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public OpenerSetupLoader<?, ?, ?> getSetupLoader(int channelIdx, int setupIdx, Supplier<VolatileGlobalCellCache> cacheSupplier) {
		return new BioFormatsSetupLoaderBT(this,
				channelIdx, this.iSerie, setupIdx, (NumericType) this.getPixelType(), OpenerHelper.getVolatileOf((NumericType) this.getPixelType()), cacheSupplier);
	}

	@Override
	public String getRawPixelDataKey() {
		return rawPixelDataKey;
	}

	@Override
	public OpenerMeta getMeta() {
		return meta;
	}

	@Override
	public int[] getCellDimensions(int level) {
		return cellDimensions;
	}

	@Override
	public Dimensions[] getDimensions() {
		return this.dimensions;
	}

	@Override
	public int getNChannels() {
		return nChannels;
	}

	@Override
	public Type<? extends NumericType<?>> getPixelType() {
		return this.t;
	}

	@Override
	public void close() {
		getPixelReader().shutDown(reader -> {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		if (model!=null) {
			try {
				model.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	private static class ReaderPool extends ResourcePool<IFormatReader> {

		final Supplier<IFormatReader> readerSupplier;
		final IFormatReader model;

		public ReaderPool(int size, Boolean dynamicCreation,
						  Supplier<IFormatReader> readerSupplier)
		{
			super(size, dynamicCreation);
			createPool();
			this.readerSupplier = readerSupplier;
			model = null;
		}

		public ReaderPool(int size, Boolean dynamicCreation,
						  Supplier<IFormatReader> readerSupplier,
						  IFormatReader model)
		{
			super(size, dynamicCreation);
			this.model = model;
			createPool();
			this.readerSupplier = readerSupplier;
		}

		@Override
		public IFormatReader createObject() {
			// Line below: optimisation for CZI reader and Lattice Light Sheet dataset
			// It is complicated because it needs to work for the standard bio-formats version
			// and for the modified bio-formats version with the lattice light sheet reader
			if ((model!=null)&&(BioFormatsHelper.hasCopyMethod(model))) {
				return BioFormatsHelper.copy(model);
			}
			return readerSupplier.get();
		}
	}
}

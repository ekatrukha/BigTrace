package bigtrace.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.ViewerImgLoader;
import bdv.cache.SharedQueue;
import bdv.img.cache.VolatileGlobalCellCache;
import ch.epfl.biop.bdv.img.CacheControlOverride;
import ch.epfl.biop.bdv.img.OpenerSetupLoader;
import ch.epfl.biop.bdv.img.OpenersImageLoader;
import ch.epfl.biop.bdv.img.opener.EmptyOpener;
import ch.epfl.biop.bdv.img.opener.Opener;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;

public class OpenersImageLoaderBT implements ViewerImgLoader, MultiResolutionImgLoader, Closeable, CacheControlOverride
{

	final protected static Logger logger = LoggerFactory.getLogger(OpenersImageLoader.class);


	// -------- ViewSetups core infos (pixel type, channels)
	final AbstractSequenceDescription<?, ?, ?> sequenceDescription;
	final Map<Integer, OpenerAndChannelIndex> viewSetupToOpenerChannel = new HashMap<>();
	int viewSetupCounter = 0;


	// -------- setupLoader registration
	final Map<Integer, OpenerSetupLoader<?,?,?>> setupLoaders = new HashMap<>();

	// -------- setupLoader optimisation
	final Map<String, Opener<?>> rawPixelDataChannelToOpener = new HashMap<>();
	final Map<String, OpenerSetupLoader<?,?,?>> rawPixelDataChannelToSetupLoader = new HashMap<>();

	// -------- How to open image (threads, cache)
	protected VolatileGlobalCellCache cache;
	protected final SharedQueue sq;
	public final int numFetcherThreads = 10;
	public final int numPriorities = 4;


	// -------- Openers core infos
	final  List<OpenerSettingsBT> openerSettings;
	final public List<Opener<?>> openers;


	// GETTER
	public List<OpenerSettingsBT> getOpenerSettings() {
		return openerSettings;
	}

	/**
	 *
	 * @return a map linking a view setup id to its associated opener
	 */
	public Map<Integer, OpenerAndChannelIndex> getViewSetupToOpenerAndChannelIndex() {
		return this.viewSetupToOpenerChannel;
	}
	/**
	 * Constructor
	 * @param openerSettings
	 * @param sequenceDescription
	 */
	public OpenersImageLoaderBT(List<OpenerSettingsBT> openerSettings,
							  final AbstractSequenceDescription<?, ?, ?> sequenceDescription)
	{
		this(openerSettings, createOpeners(openerSettings), sequenceDescription);
	}

	/**
	 * Constructor
	 * @param openerSettings
	 * @param sequenceDescription
	 */
	public OpenersImageLoaderBT(List<OpenerSettingsBT> openerSettings,
							  List<Opener<?>> openers,
							  final AbstractSequenceDescription<?, ?, ?> sequenceDescription)
	{
		this.openerSettings = openerSettings; // Need to keep a ref for serialization
		this.openers = openers;
		this.sequenceDescription = sequenceDescription;
		this.sq = new SharedQueue(numFetcherThreads, numPriorities);

		// for each opener
		IntStream openersIdxStream = IntStream.range(0, openers.size());
		if ((sequenceDescription != null)) {
			openersIdxStream.forEach(iF -> {
				try {
					// get the opener
					Opener<?> opener = openers.get(iF);
					final int iFile = iF;

					logger.debug("\t Number of timesteps = " + opener.getNTimePoints());
					logger.debug("\t Number of channels = " +opener.getNChannels());

					// Register Setups (one per channel and one per timepoint)
					IntStream channels = IntStream.range(0, opener.getNChannels());
					channels.forEach(iCh -> {
						rawPixelDataChannelToOpener.put(iCh+"."+opener.getRawPixelDataKey(), opener);
						OpenerAndChannelIndex oci = new OpenerAndChannelIndex(iFile, iCh);
						viewSetupToOpenerChannel.put(viewSetupCounter, oci);
						viewSetupCounter++;
					});
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		cache = new VolatileGlobalCellCache(sq);
	}

	public static List<Opener<?>> createOpeners(List<OpenerSettingsBT> openerSettings) {
		List<Opener<?>> openers = new ArrayList<>();
		Map<String, Object> cachedObjects = new HashMap<>();
		openerSettings.forEach(settings -> {
			try {
				Opener<?> opener = settings.create(cachedObjects);
				openers.add(opener);
			} catch (Exception e) {
				System.err.println("Error in opener "+e.getMessage()+" : "+settings.toString());
				logger.error(e.getMessage());
				int nChannels = settings.getNChannels()>0?settings.getNChannels():1;
				openers.add(new EmptyOpener(e.getMessage(), nChannels, e.getMessage(), false));
			}
		});
		assert openerSettings.size() == openers.size();
		return openers;
	}

	/**
	 * @param setupId : viewsetup id
	 * @return the setupLoader corresponding to the current viewsetup id
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public OpenerSetupLoader getSetupImgLoader(int setupId) {
		try {
			// if already registered setup loader
			if (setupLoaders.containsKey(setupId)) {
				return setupLoaders.get(setupId);
			}

			int iOpener = viewSetupToOpenerChannel.get(setupId).openerIndex;
			int iC = viewSetupToOpenerChannel.get(setupId).channelIndex;

			String keySetup = iC+"."+openers.get(viewSetupToOpenerChannel.get(setupId).openerIndex).getRawPixelDataKey();

			if (rawPixelDataChannelToSetupLoader.containsKey(keySetup)) {
				//System.out.println("Reuse "+keySetup);
				OpenerSetupLoader<?,?,?> loader = rawPixelDataChannelToSetupLoader.get(keySetup);
				setupLoaders.put(setupId, loader);
				return loader;
				//rawPixelDataChannelToSetupLoader.get(keySetup);.get(openers.get(viewSetupToOpenerChannel.get(setupId).openerIndex).getRawPixelDataKey());
				//problem : couleur et a mettre dans setupLoader
				//		faire la fonction synchronized peut etre
			}
			else {

				logger.debug("loading file number = " + iOpener + " setupId = " + setupId);

				// select the correct setup loader according to opener type
				try {
					OpenerSetupLoader<?,?,?> imgL = openers.get(iOpener).getSetupLoader(iC, setupId, this::getCacheControl);
					setupLoaders.put(setupId, imgL);
					rawPixelDataChannelToSetupLoader.put(keySetup, imgL);
					return imgL;
				}catch(Exception e){
					e.printStackTrace();
				}

				return null;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in setup loader creation: " + e
				.getMessage());
		}
	}

	@Override
	public VolatileGlobalCellCache getCacheControl() {
		return cache;
	}



	@Override
	public void close() {
		openers.forEach(opener -> {
			try {
				opener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		cache.clearCache();
		sq.shutdown();
	}

	@Override
	public void setCacheControl(VolatileGlobalCellCache cache)  {
		CacheControlOverride.Tools.shutdownCacheQueue(this.cache);
		this.cache.clearCache();
		this.cache = cache;
	}


	/**
	 * Small class to save opener and channel index together
	 */
	public static class OpenerAndChannelIndex {
		final int openerIndex;
		final int channelIndex;

		public OpenerAndChannelIndex(int openerIndex, int channelIndex) {
			this.openerIndex = openerIndex;
			this.channelIndex = channelIndex;
		}

		public int getChannelIndex() {
			return channelIndex;
		}

		public int getOpenerIndex() {
			return openerIndex;
		}
	}
}
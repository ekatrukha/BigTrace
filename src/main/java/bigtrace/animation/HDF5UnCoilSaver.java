package bigtrace.animation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;
import bdv.export.ExportScalePyramid.LoopbackHeuristic;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;


public class HDF5UnCoilSaver < T extends RealType< T > & NativeType< T > >
{
	final UnCoilAnimation<T> unCoil;
	final BigTrace<T> bt;

	public HDF5UnCoilSaver(final UnCoilAnimation<T> unCoil_, final BigTrace<T> bt_)
	{
		bt = bt_;
		unCoil = unCoil_;		
	}
	
	public void exportUnCoil()
	{
		String fullPathNoExt = unCoil.sSaveFolderPath +unCoil.inputROI.getName();
		final File hdf5File = new File( fullPathNoExt + ".h5" );
		final File xmlFile = new File( fullPathNoExt + ".xml" );
		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions( bt.btData.sVoxelUnit, BigTraceData.globCal[0], BigTraceData.globCal[1], BigTraceData.globCal[2] );
		final FinalDimensions size = new FinalDimensions( unCoil.unionInterval.dimension( 0 ), unCoil.unionInterval.dimension( 1 ), unCoil.unionInterval.dimension( 2 ) );
		
		// propose reasonable mipmap settings
		final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps.proposeMipmaps( new BasicViewSetup( 0, "", size, voxelSize ) );
		final ProgressWriter progressWriter = new ProgressWriterIJ();
		progressWriter.out().println( "starting export..." );
		final UnCoilFrameImgLoader< T > imgLoader = new UnCoilFrameImgLoader<>(unCoil, bt);
		final int numTimepoints = unCoil.nFrames;
		final int numSetups = unCoil.nFrames*bt.btData.nTotalChannels;
		
		// write hdf5
		final HashMap< Integer, BasicViewSetup > setups = new HashMap<>( numSetups );
		
		for ( int s = 0; s < numSetups; ++s )
		{
			//final int nTPInternal = ( int ) Math.floor(s/(double)(bt.btData.nTotalChannels));
			//final int nCh = s - nTPInternal* bt.btData.nTotalChannels;
			final int nCh = ( int ) Math.floor(s/(double)(unCoil.nFrames));
			final int nTPInternal = s - nCh*unCoil.nFrames;
			final BasicViewSetup setup = new BasicViewSetup( s, String.format( "tp %d ch %d", nTPInternal, nCh + 1 ), size, voxelSize );
			setup.setAttribute( new Channel( nCh + 1 ) );
			setups.put( s, setup );
		}
		final ArrayList< TimePoint > timepoints = new ArrayList<>( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( new TimePoint( t ) );
		//fill missing views
		final ArrayList<ViewId> listMissingViews = new ArrayList<>();
		for(int nTP = 0; nTP<numTimepoints; nTP++)
		{
			for(int nCh = 0; nCh<bt.btData.nTotalChannels; nCh++)
			{
				final int viewID = nTP + nCh*unCoil.nFrames;
				for(int nTPv = 0; nTPv<numTimepoints; nTPv++)
				{
					if(nTPv!=nTP)
					{
						listMissingViews.add(new ViewId( nTPv, viewID) );
					}
				}
			}
		}
		MissingViews missingViews = new MissingViews(listMissingViews);
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, missingViews );
		final Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo = new HashMap<>();
		final ExportMipmapInfo mipmapInfo = autoMipmapSettings;
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			perSetupExportMipmapInfo.put( setup.getId(), mipmapInfo );
		// LoopBackHeuristic:
		// - If saving more than 8x on pixel reads use the loopback image over
		//   original image
		// - For virtual stacks also consider the cache size that would be
		//   required for all original planes contributing to a "plane of
		//   blocks" at the current level. If this is more than 1/4 of
		//   available memory, use the loopback image.
		//final long planeSizeInBytes = unCoil.allInt.dimension( 0 ) *unCoil.allInt.dimension( 1 ) * 2;
		//final long ijMaxMemory = IJ.maxMemory();
		final int numCellCreatorThreads = Math.max( 1, PluginHelper.numThreads() - 1 );
		final LoopbackHeuristic loopbackHeuristic = new LoopbackHeuristic()
		{
			@Override
			public boolean decide( final RandomAccessibleInterval< ? > originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize )
			{
				if ( previousLevel < 0 )
					return false;

				if ( Intervals.numElements( factorsToOriginalImg ) / Intervals.numElements( factorsToPreviousLevel ) >= 8 )
					return true;

				return false;
			}
		};
		final ExportScalePyramid.AfterEachPlane afterEachPlane = usedLoopBack ->
		{ };

		WriteSequenceToHdf5.writeHdf5File( seq, perSetupExportMipmapInfo, true, hdf5File, loopbackHeuristic, afterEachPlane, numCellCreatorThreads, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );
		// write xml sequence description
		final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader( hdf5File, null, null, false );
		final SequenceDescriptionMinimal seqh5 = new SequenceDescriptionMinimal( seq, hdf5Loader );
		final ArrayList< ViewRegistration > registrations = new ArrayList<>();
		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				registrations.add( new ViewRegistration( t, s, getTransform(t) ) );
		final File basePath = xmlFile.getParentFile();
		final SpimDataMinimal spimData = new SpimDataMinimal( basePath, seqh5, new ViewRegistrations( registrations ) );
		try
		{
			new XmlIoSpimDataMinimal().save( spimData, xmlFile.getAbsolutePath() );
			progressWriter.setProgress( 1.0 );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}
		progressWriter.out().println( "done" );
	}
	
	AffineTransform3D getTransform(int nTP)
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();

		sourceTransform.set(
				BigTraceData.globCal[ 0 ], 0, 0, 0,
				0, BigTraceData.globCal[ 1 ], 0, 0,
				0, 0, BigTraceData.globCal[ 2 ], 0 );
		double[] minShift = unCoil.allIntervals.get( nTP ).minAsDoubleArray();
		for(int d=0;d<3;d++)
		{
			minShift[ d ] *= BigTraceData.globCal[ d ];
		}
		sourceTransform.translate( minShift );
		return sourceTransform;
	}

}

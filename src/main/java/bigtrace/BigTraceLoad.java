package bigtrace;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import bigtrace.volume.VolumeMisc;
import ch.epfl.biop.bdv.img.OpenersToSpimData;
import ch.epfl.biop.bdv.img.opener.OpenerSettings;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.LUT;
import loci.common.DebugTools;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.SequenceDescription;
import net.imglib2.AbstractInterval;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class BigTraceLoad < T extends RealType< T > >
{
	
	/** plugin instance **/
	BigTrace<T> bt;
	
	BigTraceData<T> btdata;
	
	/** colors for each channel **/
	public Color [] colorsCh;
	
	/** intensity range for channels **/
	public double [][] channelRanges;	
	
	public BigTraceLoad(final BigTrace<T> bt_)
	{
		
		bt = bt_;
		btdata = bt.btdata;
	}
	
	@SuppressWarnings("unchecked")
	public boolean initDataSourcesHDF5() throws SpimDataException
	{
		
		bt.sources = new ArrayList<IntervalView< T >>();
		
		bt.spimData = new XmlIoSpimData().load( btdata.sFileNameFullImg );

		
		final SequenceDescription seq = bt.spimData.getSequenceDescription();
		
		//get voxel size
		for (int d =0;d<3;d++)
		{
			BigTraceData.globCal[d] = seq.getViewDescription(0,0).getViewSetup().getVoxelSize().dimension(0);
		}
		//number of timepoints
		BigTraceData.nNumTimepoints = seq.getTimePoints().size();
		BigTraceData.dMinVoxelSize = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]), BigTraceData.globCal[2]);
		
		FinalInterval rai_int = new FinalInterval((RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(0).getImage(0));

		rai_int.min( btdata.nDimIni[0] );
		rai_int.max( btdata.nDimIni[1] );
		rai_int.min( btdata.nDimCurr[0] );
		rai_int.max( btdata.nDimCurr[1] );
		
		btdata.sVoxelUnit=seq.getViewSetupsOrdered().get(0).getVoxelSize().unit();
		
		btdata.nTotalChannels = seq.getViewSetupsOrdered().size();

		
		
		for (int setupN=0;setupN<seq.getViewSetupsOrdered().size();setupN++)
		{
			bt.sources.add(Views.interval((RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(setupN).getImage(0),rai_int));
		}
		
		//TODO FOR NOW, get it from the class
		//not really needed later, but anyway
		btdata.nBitDepth = 16;
		colorsCh = new Color[btdata.nTotalChannels];
		channelRanges = new double [2][btdata.nTotalChannels];
		
		
		return true;
		
	}
	
	@SuppressWarnings("unchecked")
	public boolean initDataSourcesBioFormats() throws SpimDataException
	{
		DebugTools.setRootLevel("INFO");
		bt.sources = new ArrayList<IntervalView< T >>();
		File f = new File(btdata.sFileNameFullImg);
		//int nSeries = BioFormatsHelper.getNSeries(f);
		//BioFormatsHelper.
		OpenerSettings settings = OpenerSettings.BioFormats()
		.location(f)
		.unit( "MICROMETER")
		.setSerie(0)
		.positionConvention("TOP LEFT");
		
		bt.spimData = (SpimData) OpenersToSpimData.getSpimData(settings);	

		final SequenceDescription seq = bt.spimData.getSequenceDescription();

		//get voxel size
		for (int d =0;d<3;d++)
		{
			BigTraceData.globCal[d] = seq.getViewDescription(0,0).getViewSetup().getVoxelSize().dimension(d);
		}
		BigTraceData.dMinVoxelSize = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]), BigTraceData.globCal[2]);
		
		//number of timepoints
		BigTraceData.nNumTimepoints = seq.getTimePoints().size();
		
		//see if data comes from LLS7
		String sTestLLS = seq.getViewDescription(0, 0).getViewSetup().getName();
		if(sTestLLS.length()>3)
		{
		
			String isLLS = sTestLLS.substring(sTestLLS.length()-4,sTestLLS.length()-1);
			
			if(isLLS.equals("LLS"))
			{
				if (JOptionPane.showConfirmDialog(null, "Looks like the input comes from Zeiss LLS7.\nDo you want to deskew it?\n"
						+ "(if it is already deskewed, click No)", "Loading option",
				        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				    // yes option
					bt.bTestLLSTransform = true;
				} else {
				    // no option
				}
				
			}

		}
		

		
		FinalInterval rai_int;
		RandomAccessibleInterval<T> raitest = (RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(0).getImage(0);
		
		if(bt.bTestLLSTransform)
		{
			//build LLS transform
			rai_int = makeLLS7Transform(BigTraceData.globCal,raitest);
		}
		else
		{
			rai_int = new FinalInterval(raitest);
		}
		
		rai_int.min( btdata.nDimIni[0] );
		rai_int.max( btdata.nDimIni[1] );
		rai_int.min( btdata.nDimCurr[0] );
		rai_int.max( btdata.nDimCurr[1] );
		
		btdata.sVoxelUnit = seq.getViewSetupsOrdered().get(0).getVoxelSize().unit();
		
		btdata.nTotalChannels = seq.getViewSetupsOrdered().size();	
		
		for (int setupN=0;setupN<seq.getViewSetupsOrdered().size();setupN++)
		{
			if(bt.bTestLLSTransform)
			{	
				RandomAccessibleInterval<T> rai_orig = (RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(setupN).getImage(0);
				RealRandomAccessible<T> rra = Views.interpolate(Views.extendZero(rai_orig), btdata.nInterpolatorFactory);
				RealRandomAccessible<T> rra_tr = RealViews.affine(rra, bt.afDataTransform);
				bt.sources.add(Views.interval(Views.raster(rra_tr),rai_int));
			}
			else
			{
				bt.sources.add(Views.interval((RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(setupN).getImage(0),rai_int));
			}
		}
		
		//TODO FOR NOW, get it from the class
		//not really needed later, but anyway
		btdata.nBitDepth = 16;
		colorsCh = new Color[btdata.nTotalChannels];
		channelRanges = new double [2][btdata.nTotalChannels];
		
		
		return true;
		
	}
	
	/** function assigns new LLS7 transform to bt.afDataTransform (using provided voxel size of original data) 
	 * and returns the new interval of transformed source **/
	FinalInterval makeLLS7Transform(final double [] voxelSize, final Interval orig_rai)
	{
		bt.afDataTransform = new AffineTransform3D();
		AffineTransform3D tShear = new AffineTransform3D();
		AffineTransform3D tRotate = new AffineTransform3D();
	
		
		//rotate 30 degrees
		tRotate.rotate(0, (-1.0)*Math.PI/6.0);
		//shearing transform
		tShear.set(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.7320508075688767, 0.0, 0.0, 0.0, 1.0, 0.0);
		//Z-step adjustment transform
		bt.afDataTransform.set(BigTraceData.globCal[0]/BigTraceData.dMinVoxelSize, 0.0, 0.0, 0.0, 
								0.0, BigTraceData.globCal[1]/BigTraceData.dMinVoxelSize, 0.0, 0.0, 
								0.0, 0.0, 0.5*BigTraceData.globCal[2]/BigTraceData.dMinVoxelSize, 0.0);
		
		bt.afDataTransform = tShear.concatenate(bt.afDataTransform);
		bt.afDataTransform = tRotate.concatenate(bt.afDataTransform);
		FinalRealInterval finReal = bt.afDataTransform.estimateBounds(orig_rai);
		double [][] dBounds = new double [2][3]; 
		long [][] lBounds = new long [2][3]; 
		finReal.realMin(dBounds[0]);
		AffineTransform3D tZeroMin = new AffineTransform3D();
		for (int i = 0;i<3;i++)
		{
			dBounds[0][i] = dBounds[0][i]*(-1);
		}			
		tZeroMin.translate(dBounds[0]);
		bt.afDataTransform = bt.afDataTransform.preConcatenate(tZeroMin);
		finReal = bt.afDataTransform.estimateBounds(orig_rai);
		finReal.realMin(dBounds[0]);
		finReal.realMax(dBounds[1]);
		for (int i = 0;i<3;i++)
			{
				lBounds[0][i] = (long) Math.floor(dBounds[0][i]);
				lBounds[1][i] = (long) Math.ceil(dBounds[1][i]);
			}
		return new FinalInterval(lBounds[0],lBounds[1]);
	}
	
	@SuppressWarnings("unchecked")
	public boolean initDataSourcesImageJ()
	{
		
		bt.sources = new ArrayList<IntervalView< T >>();
		
		final ImagePlus imp = IJ.openImage( btdata.sFileNameFullImg );
		
		if (imp == null)
		{
			IJ.showMessage("BigTrace: cannot open selected TIF file. Plugin terminated.");
			return false;
		}
		
		
		if(imp.getType()!=ImagePlus.GRAY8 && imp.getType()!=ImagePlus.GRAY16 && imp.getType()!=ImagePlus.GRAY32)
		{
			IJ.showMessage("Only 8-, 16- and 32-bit images supported for now.");
			return false;
		}

		
		BigTraceData.globCal[0] = imp.getCalibration().pixelWidth;
		BigTraceData.globCal[1] = imp.getCalibration().pixelHeight;
		BigTraceData.globCal[2] = imp.getCalibration().pixelDepth;
		BigTraceData.dMinVoxelSize = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]), BigTraceData.globCal[2]);
		btdata.sVoxelUnit = imp.getCalibration().getUnit();
		btdata.sTimeUnit = imp.getCalibration().getTimeUnit();
		btdata.dFrameInterval = imp.getCalibration().frameInterval;
		BigTraceData.nNumTimepoints = imp.getNFrames();
		
		Img<T> img_ImageJ;

				
		btdata.nBitDepth = imp.getBitDepth();
		if(btdata.nBitDepth<=16)
		{
			img_ImageJ = ImageJFunctions.wrapReal(imp);
		}
		else
		{
			img_ImageJ = (Img<T>) VolumeMisc.convertFloatToUnsignedShort(ImageJFunctions.wrapReal(imp));
		}
		//long[] test = img_ImageJ.dimensionsAsLongArray();
		
		//let's convert it to XYZTC for BVV to understand
		
		
		btdata.nTotalChannels=imp.getNChannels();
		if(btdata.nTotalChannels==1)
		{
			//add dimension for the channels
			bt.all_ch_RAI = Views.addDimension(img_ImageJ, 0, 0);
		}
		else
		{
			//change the order of C and Z
			bt.all_ch_RAI = Views.permute(img_ImageJ, 2,3);
		}
		//test = all_ch_RAI.dimensionsAsLongArray();
		

		getChannelsColors(imp);

		if(BigTraceData.nNumTimepoints==1)
		{
			bt.all_ch_RAI = Views.addDimension(bt.all_ch_RAI, 0, 0);
			//test = all_ch_RAI.dimensionsAsLongArray();
			if(btdata.nTotalChannels==1)
			{
				bt.all_ch_RAI =Views.permute(bt.all_ch_RAI, 3,4);
			}
			//test = all_ch_RAI.dimensionsAsLongArray();
		}
		//finally change C and T (or it can be already fine, if we added C dimension)
		if(btdata.nTotalChannels>1)
		{
			bt.all_ch_RAI =Views.permute(bt.all_ch_RAI, 4,3);
		}
		//test = all_ch_RAI.dimensionsAsLongArray();
		for(int i=0;i<btdata.nTotalChannels;i++)
		{
			bt.sources.add(Views.hyperSlice(Views.hyperSlice(bt.all_ch_RAI,4,i),3,0));
		}

		bt.sources.get(0).min( btdata.nDimIni[0] );
		bt.sources.get(0).max( btdata.nDimIni[1] );
		bt.sources.get(0).min( btdata.nDimCurr[0] );
		bt.sources.get(0).max( btdata.nDimCurr[1] );
		
		return true;
	}
	
	/** creates and fills array colorsCh with channel colors,
	 * taken from Christian Tischer reply in this thread
	 * https://forum.image.sc/t/composite-image-channel-color/45196/3 **/
	public void getChannelsColors(ImagePlus imp)
	{
		colorsCh = new Color[imp.getNChannels()];
		channelRanges = new double [2][imp.getNChannels()];
	      for ( int c = 0; c < imp.getNChannels(); ++c )
	        {
	            if ( imp instanceof CompositeImage )
	            {
	                CompositeImage compositeImage = ( CompositeImage ) imp;
					LUT channelLut = compositeImage.getChannelLut( c + 1 );
					int mode = compositeImage.getMode();
					if ( channelLut == null || mode == CompositeImage.GRAYSCALE )
					{
						colorsCh[c] = Color.WHITE;
					}
					else
					{
						IndexColorModel cm = channelLut.getColorModel();
						if ( cm == null )
						{
							colorsCh[c] = Color.WHITE;
						}
						else
						{
							int i = cm.getMapSize() - 1;
							colorsCh[c] = new Color(cm.getRed( i ) ,cm.getGreen( i ) ,cm.getBlue( i ) );

						}

					}

					compositeImage.setC( c + 1 );
					channelRanges[0][c]=(int)imp.getDisplayRangeMin();
					channelRanges[1][c]=(int)imp.getDisplayRangeMax();
	            }
	            else
	            {
	            	colorsCh[c] = Color.WHITE;
	            	channelRanges[0][c]=(int)imp.getDisplayRangeMin();
	            	channelRanges[1][c]=(int)imp.getDisplayRangeMax();
	            }
	        }
	}

}

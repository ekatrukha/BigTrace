package bigtrace.volume;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import bdv.spimdata.SequenceDescriptionMinimal;
import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.geometry.Intersections3D;
import bigtrace.rois.Box3D;
import bigtrace.rois.CrossSection3D;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import net.imagej.util.Images;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class SplitVolumePlane < T extends RealType< T > > extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	
	CrossSection3D crossSection;
	public BigTrace<T> bt;
	int nSliceType;
	
	
	public SplitVolumePlane(final CrossSection3D crossSection_, final BigTrace<T> bt_, final int nSliceType_)
	{
		super();
		crossSection = crossSection_;
		bt = bt_;
		nSliceType= nSliceType_;
	}
	
	@Override
	public String getProgressState()
	{
		return progressState;
	}
	@Override
	public void setProgressState(String state_)
	{
		progressState=state_;
	}
	
	
	@Override
	protected Void doInBackground() throws Exception {
		
		int i,d;
		//check if there is a fitted plane
		if (crossSection.fittedPlane==null)
			return null;
		
        bt.bInputLock = true;
        bt.roiManager.setLockMode(true);
        
		boolean nMultCh;
		if(bt.btdata.nTotalChannels==1)
		{
			nMultCh=false;
		}
		else
		{
			nMultCh=true;
		}

		try {
			  Thread.sleep(1);
		  } catch (InterruptedException ignore) {}
		setProgress(1);
		setProgressState("allocating two new volumes..");
		  
		
		
		//(for now)
		//make two copies

		if(bt.bBDVsource)
		{
			final SequenceDescriptionMinimal seq = bt.spimData.getSequenceDescription();
			
			List<RandomAccessibleInterval<T>> hyperslices = new ArrayList<RandomAccessibleInterval<T>> ();
			
			
			for (int setupN=0;setupN<seq.getViewSetupsOrdered().size();setupN++)
			{
				hyperslices.add((RandomAccessibleInterval<T>) seq.getImgLoader().getSetupImgLoader(setupN).getImage(0));
			}
			//for()
			bt.all_ch_RAI = Views.stack(hyperslices);
		}

	
		Img<T> out1 =  Util.getSuitableImgFactory(bt.all_ch_RAI, Util.getTypeFromInterval(bt.all_ch_RAI)).create(bt.all_ch_RAI);
		LoopBuilder.setImages(out1, bt.all_ch_RAI).forEachPixel(Type::set);
		Img<T> out2 =  Util.getSuitableImgFactory(bt.all_ch_RAI, Util.getTypeFromInterval(bt.all_ch_RAI)).create(bt.all_ch_RAI);
		LoopBuilder.setImages(out2, bt.all_ch_RAI).forEachPixel(Type::set);
		
		final long [] nTotPixArr = bt.all_ch_RAI.dimensionsAsLongArray();
		
		// total number of pixels for the progress bar 
		long nTotPixCount = nTotPixArr[0];
		for (i=1;i<nTotPixArr.length; i++)
		{
			nTotPixCount *= nTotPixArr[i];
		}
		
		long nCount = 0;
		
		Cursor<T> cursor1 = out1.localizingCursor();
		Cursor<T> cursor2 = out2.localizingCursor();
		RealPoint rp = new RealPoint(3);
		double [] position = new double [out1.numDimensions()];
		double [] position3 = new double [3];
		setProgressState("splitting volume in two..");
		while (cursor1.hasNext())
		{
			cursor1.fwd();
			cursor2.fwd();
			cursor1.localize(position);
			if(!nMultCh)
			{
				rp.setPosition(position);
			}
			else
			{
				for(d=0;d<3;d++)
				{
					position3[d]=position[d];
				}
				rp.setPosition(position3);
			}
			
			if(crossSection.fittedPlane.signedDistance(rp)>0.0)
			{
				cursor1.get().setZero();
			}
			else
			{
				cursor2.get().setZero();
			}
			nCount++;
			setProgress((int)(100.0*nCount/nTotPixCount));
		}
		Calibration cal = new Calibration();
		cal.setUnit(bt.btdata.sVoxelUnit);
		cal.pixelWidth= bt.btdata.globCal[0];
		cal.pixelHeight= bt.btdata.globCal[1];
		cal.pixelDepth= bt.btdata.globCal[2];
		Path p = Paths.get(bt.btdata.sFileNameFullImg);
		String fileName = p.getFileName().toString()+"_"+crossSection.getName();
		
		//original volume size
		if(nSliceType ==0)
		{

			VolumeMisc.wrapImgImagePlusCal(out1,fileName+"_vol1", cal).show();
			VolumeMisc.wrapImgImagePlusCal(out2,fileName+"_vol2", cal).show();

		}	
		//tight crop, let's calculate corresponding intervals
		else
		{
			//make arrays of points for each subvolume
			ArrayList<RealPoint> vertOut1= new ArrayList<RealPoint>();
			ArrayList<RealPoint> vertOut2= new ArrayList<RealPoint>();
			//add cross-section polygon vertices
			for (i=0;i<crossSection.polygonVert.size();i++)
			{
				vertOut1.add(crossSection.polygonVert.get(i));
				vertOut2.add(crossSection.polygonVert.get(i));
			}
			//add vertices of corners depending on the sign
			ArrayList<RealPoint> boxVert; 
			//one channel
			if(!nMultCh)
			{				
				boxVert = Box3D.getBoxVertices(out1);
			}
			//many channels
			else
			{
				boxVert = Box3D.getBoxVertices(Views.hyperSlice(out1, 3, 0));
			}
			
			//split vertexes of the bounding box based
			//on their relative position to the plane
			for (i=0;i<boxVert.size();i++)
			{
				//inverse here, since before we were putting zeros there 
				if(crossSection.fittedPlane.signedDistance(boxVert.get(i))>0.0)
				{
					vertOut2.add(boxVert.get(i));
				}
				else
				{
					vertOut1.add(boxVert.get(i));					
				}
			}
			long [][][] newBoundBoxes = new long [2][2][3];
			Intersections3D.makeBoundBox(vertOut1,newBoundBoxes[0]);
			Intersections3D.makeBoundBox(vertOut2,newBoundBoxes[1]);
			
			//crop intervals
			FinalInterval [] newCrop = new FinalInterval[2];
			//one channel
			if(!nMultCh)
			{
				for(i=0;i<2;i++)
				{
					newCrop[i]=new FinalInterval(newBoundBoxes[i][0],newBoundBoxes[i][1]);
				}
			}
			//many channels
			else
			{
				long [][] rangeCh = new long[2][4];
				for (int intervN = 0; intervN<2; intervN ++)
				{
					for(i=0;i<2;i++)
					{
						for(d=0;d<3;d++)
						{
							rangeCh[i][d]= newBoundBoxes[intervN][i][d];
						}
					}
					rangeCh[0][3]=out1.min(3);
					rangeCh[1][3]=out1.max(3);
					newCrop[intervN]=new FinalInterval(rangeCh[0],rangeCh[1]);
				}

			}
			
			VolumeMisc.wrapImgImagePlusCal(Views.interval(out1, newCrop[0]),fileName+"_vol1", cal).show();
			VolumeMisc.wrapImgImagePlusCal(Views.interval(out2, newCrop[1]),fileName+"_vol2", cal).show();
		}

		setProgressState("volume splitting done.");
		setProgress(100);

		
		return null;
	}
	
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() 
    {
		//unlock user interaction
    	bt.bInputLock = false;
        bt.roiManager.setLockMode(false);

    }

}

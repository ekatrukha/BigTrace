package bigtrace.rois;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.SwingWorker;

import bigtrace.BigTrace;
import bigtrace.BigTraceBGWorker;
import bigtrace.BigTraceData;
import net.imglib2.RealPoint;

public class ROIsLoadBG extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	public BigTrace<?> bt;
	public String sFilename;
	public int nLoadMode;
	String sFinalOut = "";
	double [] globCalNew;
	String sUnits = "";
	
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
		String[] line_array;
       
        int nRoiN=1;
        int nVertN=0;
        int i,j;

        
        ArrayList<RealPoint> vertices;
        ArrayList<RealPoint> segment;
        
        int nROIGroupInd = 0;
        int nTimePoint = 0;
        float pointSize = 0.0f;
        float lineThickness = 0.0f;
        Color pointColor = Color.BLACK;
        Color lineColor = Color.BLACK;
        String sName = "";
        int nRoiType = Roi3D.POINT;
        int nRenderType = 0;
    	String sUnits = "";
    	double [] globCalNew  = new double [3];
        globCalNew[0]= Double.NaN;
        globCalNew[1]= Double.NaN;
        globCalNew[2]= Double.NaN;
        
        
       
        Roi3DGroupManager roiGM;
	
        bt.bInputLock = true;
        bt.roiManager.setLockMode(true);
		try {
			
	        BufferedReader br = new BufferedReader(new FileReader(sFilename));

	        String line = "";
	        //read Groups first
	        if(nLoadMode == 0)
	        {
	        	roiGM = new Roi3DGroupManager(bt.roiManager);
	        	if(roiGM.loadGroups(br)<0)
	        	{
	        		 System.err.println("Not a BigTrace ROI Group file format or plugin/file version mismatch,\nloading Groups failed.\n"+
	        	"Try 'Append' loading mode.\n");
	        		 return null;
	        	}
	        	bt.roiManager.updateGroupsList();
	        	line = br.readLine();
	        }
	        //skip to ROI part
	        else
	        {
	        	boolean bBeginROIpart = false;
	        	line = br.readLine();
	        	
	        	while (!bBeginROIpart)
	        	{
	        		if (line == null)
	        		{
	        			bBeginROIpart = true;
	        			br.close();
	        			return null;
	        		}
	        		else
	        		{
		        		line_array = line.split(",");
		        		
		        		if(line_array.length==3 && line_array[0].equals("BigTrace_ROIs"))
		        		{
		        			bBeginROIpart = true;
		        		}
		        		else
		        		{
		        			line = br.readLine();
		        		}
	        			
	        		}
	        			
	        	}
	        			
	        }
	        
	        //initial part check
	        
			 line_array = line.split(",");
			 if(!line_array[0].equals("BigTrace_ROIs"))
			 {
				 System.err.println("Not a BigTrace ROI file format, aborting");
				 return null;
			 }
			 if(!line_array[2].equals(BigTraceData.sVersion))
			 {
				 System.out.println("Version mismatch: ROI file "+line_array[2]+", plugin "+BigTraceData.sVersion+". It should be fine in theory, so loading ROIs anyway.");
			 }
			  

			while (line != null) 
			{
				
				  line = br.readLine();
				  if(line == null)
				  {
					  break;
				  }
				   // process the line.
				  line_array = line.split(",");
				  
				  switch (line_array[0]){
				  case "ROIsNumber":
					  nRoiN = Integer.parseInt(line_array[1]);
					  break;
				  case "ImageUnits":
					  sUnits = new String(line_array[1]);
					  break;
				  case "ImageVoxelWidth":
					  globCalNew[0] = Double.parseDouble(line_array[1]);
					  break;
				  case "ImageVoxelHeight":
					  globCalNew[1] = Double.parseDouble(line_array[1]);
					  break;
				  case "ImageVoxelDepth":
					  globCalNew[2] = Double.parseDouble(line_array[1]);
					  break;
				  case "TimeUnits":
					  bt.btdata.sTimeUnit = new String(line_array[1]);
					  break;
				  case "FrameInterval":
					  bt.btdata.dFrameInterval = Double.parseDouble(line_array[1]);
					  break;					  
				  case "BT_Roi":
					  //Sleep for up to one second.
					  try {
						  Thread.sleep(1);
					  } catch (InterruptedException ignore) {}
					  setProgress((Integer.parseInt(line_array[1])-1)*100/nRoiN);
					  setProgressState("loading ROI #"+line_array[1]+" of "+Integer.toString(nRoiN));
					  break;
					  
				  case "Type":
					  nRoiType = Roi3D.stringTypeToInt(line_array[1]);
					  break;
					  
				  case "Name":					  
					  sName = line_array[1];
					  break;
				  case "TimePoint":					  
					  nTimePoint = Integer.parseInt(line_array[1]);
					  break;  
					  
				  case "GroupInd":
					  if(nLoadMode ==0)
					  {						  
						  nROIGroupInd = Integer.parseInt(line_array[1]);
					  }	
					  break;
					  
				  case "PointSize":			  
					  pointSize = Float.parseFloat(line_array[1]);
					  break;

				  case "LineThickness":  
					  lineThickness = Float.parseFloat(line_array[1]);
					  break;

				  case "PointColor":
					  pointColor = new Color(Integer.parseInt(line_array[1]),
							  Integer.parseInt(line_array[2]),
							  Integer.parseInt(line_array[3]),
							  Integer.parseInt(line_array[4]));
					  break;					  
				  case "LineColor":
					  lineColor = new Color(Integer.parseInt(line_array[1]),
							  Integer.parseInt(line_array[2]),
							  Integer.parseInt(line_array[3]),
							  Integer.parseInt(line_array[4]));
					  break;

				  case "RenderType":
					  nRenderType = Integer.parseInt(line_array[1]);
					  break;
					  
				  case "Vertices":
					  nVertN = Integer.parseInt(line_array[1]);
					  vertices =new ArrayList<RealPoint>(); 
					  for(i=0;i<nVertN;i++)
					  {
						  line = br.readLine();
						  line_array = line.split(",");
						  vertices.add(new RealPoint(Float.parseFloat(line_array[0]),
								  Float.parseFloat(line_array[1]),
								  Float.parseFloat(line_array[2])));
					  }

					  switch (nRoiType)
					  {
					  case Roi3D.POINT:

						  Point3D roiP = (Point3D) bt.roiManager.makeRoi(nRoiType, nTimePoint);						  
						  roiP.setGroup(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType));
						  roiP.setGroupInd(nROIGroupInd);
						  roiP.setName(sName);
						  roiP.setVertex(vertices.get(0));

						  bt.roiManager.addRoi(roiP);

						  break;
					  case Roi3D.POLYLINE:

						  PolyLine3D roiPL = (PolyLine3D) bt.roiManager.makeRoi(nRoiType, nTimePoint);						  
						  roiPL.setGroup(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType));
						  roiPL.setGroupInd(nROIGroupInd);
						  roiPL.setName(sName);
						  roiPL.setVertices(vertices);

						  bt.roiManager.addRoi(roiPL);
						  break;
					  case Roi3D.LINE_TRACE:

						  LineTrace3D roiLT = (LineTrace3D) bt.roiManager.makeRoi(nRoiType, nTimePoint);
						  
						  roiLT.setGroup(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType));
						  roiLT.setGroupInd(nROIGroupInd);
						  roiLT.setName(sName);
						  roiLT.addFirstPoint(vertices.get(0));
						  //segments number
						  line = br.readLine();
						  line_array = line.split(",");
						  int nTotSegm = Integer.parseInt(line_array[1]);
						  for (i=0;i<nTotSegm;i++)
						  {
							  //points number
							  line = br.readLine();
							  line_array = line.split(",");  
							  nVertN = Integer.parseInt(line_array[3]);
							  segment =new ArrayList<RealPoint>(); 
							  for(j=0;j<nVertN;j++)
							  {
								  line = br.readLine();
								  line_array = line.split(",");
								  segment.add(new RealPoint(Float.parseFloat(line_array[0]),
										  Float.parseFloat(line_array[1]),
										  Float.parseFloat(line_array[2])));
							  }
							  roiLT.addPointAndSegmentNoUpdate(vertices.get(i+1),segment); 
						  }
						  bt.roiManager.addRoi(roiLT);
						  break;
					  case Roi3D.PLANE:
						  CrossSection3D roiCS = (CrossSection3D) bt.roiManager.makeRoi(nRoiType, nTimePoint);
						  roiCS.setGroup(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType));
						  roiCS.setGroupInd(nROIGroupInd);
						  roiCS.setName(sName);
						  roiCS.setVertices(vertices);

						  bt.roiManager.addRoi(roiCS);
						  break;
					  }
					  break;
					  
				  }
				 
				}

	        br.close();
			setProgress(100);
			
			sFinalOut="loading ROIs done.";
			setProgressState(sFinalOut);
			/** load voxel calibration **/
			
			if((!Double.isNaN(globCalNew[0]))&&(!Double.isNaN(globCalNew[1]))&&(!Double.isNaN(globCalNew[2]))&&(!sUnits.equals("")))
			{

					bt.btdata.sVoxelUnit = new String(sUnits);
					bt.btpanel.voxelSizePanel.setVoxelSize(globCalNew, sUnits);
					sFinalOut = "loading ROIs done (+voxel calibration).";
					
					setProgressState(sFinalOut);		
			}
			else
			{
				bt.roiManager.updateROIsDisplay();
			}
			

		}
		//catching errors in file opening
		catch (FileNotFoundException e) {
			System.err.print(e.getMessage());
		}	        
		catch (IOException e) {
			System.err.print(e.getMessage());
		}
        
		//some error reading the file
      /*  if(bFirstPartCheck!=4)
        {
        	 System.err.println("Not a Bigtrace ROI file format or plugin/file version mismatch, loading ROIs could be incomplete.");
             //bt.bInputLock = false;
             //bt.roiManager.setLockMode(false);
        }*/

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
        setProgress(100);
        setProgressState(sFinalOut);

    }
}

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
import net.imglib2.RealPoint;

public class ROIsLoadBG extends SwingWorker<Void, String> implements BigTraceBGWorker{

	private String progressState;
	public BigTrace bt;
	public String sFilename;
	public int nLoadMode;
	
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
        int bFirstPartCheck = 0;
        int nRoiN=1;
        int nVertN=0;
        int i,j;

        
        ArrayList<RealPoint> vertices;
        ArrayList<RealPoint> segment;
        
        int nROIGroupInd = 0;
        float pointSize = 0.0f;
        float lineThickness = 0.0f;
        Color pointColor = Color.BLACK;
        Color lineColor = Color.BLACK;
        String sName = "";
        int nRoiType = Roi3D.POINT;
        int nRenderType = 0;
        int nSectorN = 16;
        
        Roi3D nextROI;
        Roi3DGroupManager roiGM;
	
        bt.bInputLock = true;
        bt.roiManager.setLockMode(true);
		try {
			
	        BufferedReader br = new BufferedReader(new FileReader(sFilename));
	        int nLineN = 0;

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
	        if(nLoadMode==0)
	        {
	        	line = br.readLine();
	        }

			while (line != null) 
				{
				   // process the line.
				  line_array = line.split(",");
				  nLineN++;
				  //first line check
				  if(line_array.length==3 && nLineN==1)
			      {
					  bFirstPartCheck++;
					  if(line_array[0].equals("BigTrace_ROIs")&& line_array[2].equals(bt.btdata.sVersion))
					  {
						  bFirstPartCheck++; 
					  }					  
			      }
				  //second line check
				  if(line_array.length==2 && nLineN==2)
			      {
					  bFirstPartCheck++;
					  if(line_array[0].equals("ROIsNumber"))
					  {
						  bFirstPartCheck++;
						  nRoiN=Integer.parseInt(line_array[1]);
					  }
			      }				  
				  if(line_array[0].equals("BT_Roi"))
				  {
					  //Sleep for up to one second.
						try {
							Thread.sleep(1);
						} catch (InterruptedException ignore) {}
						setProgress((Integer.parseInt(line_array[1])-1)*100/nRoiN);
						setProgressState("loading ROI #"+line_array[1]+" of "+Integer.toString(nRoiN));
				  }
				  if(line_array[0].equals("Type"))
				  {						  
					  nRoiType = Roi3D.stringTypeToInt(line_array[1]);
				  }
				  if(line_array[0].equals("Name"))
				  {						  
					  sName = line_array[1];
				  }
				  //append or rewrite
				  if(line_array[0].equals("GroupInd") && nLoadMode ==0)
				  {						  
					  nROIGroupInd = Integer.parseInt(line_array[1]);
				  }				  
				  if(line_array[0].equals("PointSize"))
				  {						  
					  pointSize = Float.parseFloat(line_array[1]);
				  }
				  if(line_array[0].equals("LineThickness"))
				  {						  
					  lineThickness = Float.parseFloat(line_array[1]);
				  }
				  if(line_array[0].equals("PointColor"))
				  {						  
					  pointColor = new Color(Integer.parseInt(line_array[1]),
							  				 Integer.parseInt(line_array[2]),
							  				 Integer.parseInt(line_array[3]),
							  				 Integer.parseInt(line_array[4]));
				  }
				  if(line_array[0].equals("LineColor"))
				  {						  
					  lineColor = new Color(Integer.parseInt(line_array[1]),
							  				 Integer.parseInt(line_array[2]),
							  				 Integer.parseInt(line_array[3]),
							  				 Integer.parseInt(line_array[4]));
				  }
				  if(line_array[0].equals("RenderType"))
				  {						  
					  nRenderType = Integer.parseInt(line_array[1]);
				  }
				  if(line_array[0].equals("SectorN"))
				  {						  
					  nSectorN = Integer.parseInt(line_array[1]);
				  }
				  if(line_array[0].equals("Vertices"))
				  {						  
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
						  //Point3D roiP = new Point3D(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType,nSectorN));
						  //roiP.setName(sName);
						  //roiP.setVertex(vertices.get(0));
						  Point3D roiP = (Point3D) bt.roiManager.makeRoi(nRoiType);						  
						  roiP.setGroup(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType,nSectorN));
						  roiP.setGroupInd(nROIGroupInd);
						  roiP.setName(sName);
						  roiP.setVertex(vertices.get(0));
						  
						  bt.roiManager.addRoi(roiP);
						  
					  	  break;
					  case Roi3D.POLYLINE:
						  //PolyLine3D roiPL = new PolyLine3D(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType,nSectorN));
						  //roiPL.setName(sName);
						  //roiPL.setVertices(vertices);
						  //bt.roiManager.addRoi(roiPL);
						  PolyLine3D roiPL = (PolyLine3D) bt.roiManager.makeRoi(nRoiType);						  
						  roiPL.setGroup(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType,nSectorN));
						  roiPL.setGroupInd(nROIGroupInd);
						  roiPL.setName(sName);
						  roiPL.setVertices(vertices);
						  
						  bt.roiManager.addRoi(roiPL);
						  break;
					  case Roi3D.LINE_TRACE:
						  
						  LineTrace3D roiLT = (LineTrace3D) bt.roiManager.makeRoi(nRoiType);
						  
						 // LineTrace3D roiLT = new LineTrace3D(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType,nSectorN));
						  roiLT.setGroup(new Roi3DGroup("",pointSize,pointColor, lineThickness, lineColor,nRenderType,nSectorN));
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
							  roiLT.addPointAndSegment(vertices.get(i+1),segment); 
						  }
						  bt.roiManager.addRoi(roiLT);
						  break;
						  
					  }
					  
				  }
				  line = br.readLine();
				}

	        br.close();
			setProgress(100);
			setProgressState("loading ROIs done.");
		}
		//catching errors in file opening
		catch (FileNotFoundException e) {
			System.err.print(e.getMessage());
		}	        
		catch (IOException e) {
			System.err.print(e.getMessage());
		}
        
		//some error reading the file
        if(bFirstPartCheck!=4)
        {
        	 System.err.println("Not a Bigtrace ROI file format or plugin/file version mismatch, loading ROIs could be incomplete.");
             //bt.bInputLock = false;
             //bt.roiManager.setLockMode(false);
        }

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

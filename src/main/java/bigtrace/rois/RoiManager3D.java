package bigtrace.rois;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.joml.Matrix4fc;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.jogamp.opengl.GL3;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.gui.NumberField;
import bigtrace.gui.PanelTitle;
import bigtrace.math.TraceBoxMath;
import bigtrace.scene.VisPolyLineScaled;
import ij.IJ;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import net.imglib2.RealPoint;



public class RoiManager3D extends JPanel implements ListSelectionListener, ActionListener {
	
	 
	
	 
	 BigTrace bt;

	 private static final long serialVersionUID = -2843907862066423151L;
	 public static final int ADD_POINT=0, ADD_POINT_LINE=1, ADD_POINT_SEMIAUTOLINE=2;
	 public ArrayList<Roi3D> rois =  new ArrayList<Roi3D >();
	 public int activeRoi = -1;
	 public Color activePointColor = Color.YELLOW;
	 public Color defaultPointColor = Color.GREEN;
	 public Color activeLineColor = Color.RED;
	 public Color defaultLineColor = Color.BLUE;
	/* public Color activeLineColor = Color.WHITE;
	 public Color nonActiveLineColor = Color.WHITE;
	 public Color activePointColor = Color.WHITE;
	 public Color nonActivePointColor = Color.WHITE;*/
	 public int mode;
	 public float currLineThickness = 4.0f;
	 public float currPointSize = 6.0f;
	 public int currRenderType = VisPolyLineScaled.WIRE;
	 public int currSectorN = 16;
	 public boolean bShowAll = true;

	 
	 //GUI
	 public DefaultListModel<String> listModel; 
	 JList<String> jlist;
	 JScrollPane listScroller;
	 public static interface Listener {
		public void activeRoiChanged(int nRoi);				
	 }
	 JButton butDelete;
	 JButton butRename;
	 JButton butDeselect;
	 JButton butProperties;
	 JToggleButton butShowAll;
	 JButton butSaveROIs;
	 JButton butLoadROIs;
	 
	 JToggleButton roiPointMode;
	 JToggleButton roiPolyLineMode;
	 JToggleButton roiPolySemiAMode;
	 
	 
	 private ArrayList<Listener> listeners =	new ArrayList<Listener>();

		
	 public RoiManager3D(BigTrace bt)
	 {
		 
		 
		 this.bt = bt;
		 try {
		     UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		 } catch( Exception ex ) {
		     System.err.println( "Failed to initialize LaF" );
		 }
		
		 int nButtonSize = 40;

		 JPanel panTracing = new JPanel(new GridBagLayout());  
		 panTracing.setBorder(new PanelTitle(" Tracing type "));
		 
		 ButtonGroup roiTraceMode = new ButtonGroup();
		 
		// UIManager.put("ToggleButton.select", Color.WHITE);
		 ClassLoader classLoader = getClass().getClassLoader();
	     String icon_path = classLoader.getResource("icons/dot.png").getFile();
		 ImageIcon tabIcon = new ImageIcon(icon_path);
		 roiPointMode = new JToggleButton(tabIcon);
		 roiPointMode .setToolTipText("Trace single point");
		 roiPointMode.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		 //roiPointMode.setSelected(true);
			 
	     icon_path = classLoader.getResource("icons/polyline.png").getFile();
		 tabIcon = new ImageIcon(icon_path);
		 roiPolyLineMode = new JToggleButton(tabIcon);
		 roiPolyLineMode.setToolTipText("Trace polyline");
		 roiPolyLineMode.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		 roiPolyLineMode.setSelected(true);
	     
		 icon_path = classLoader.getResource("icons/semiauto.png").getFile();
		 tabIcon = new ImageIcon(icon_path);
		 roiPolySemiAMode = new JToggleButton(tabIcon);
		 roiPolySemiAMode.setToolTipText("Semi auto trace");
		 roiPolySemiAMode.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		 //roiPolySemiAMode.setSelected(true);		 

		 //button group	 
		 roiTraceMode.add(roiPointMode);
		 roiTraceMode.add(roiPolyLineMode);
		 roiTraceMode.add(roiPolySemiAMode);
		 
		 roiPointMode.addActionListener(this);
		 roiPolyLineMode.addActionListener(this);
		 roiPolySemiAMode.addActionListener(this);
		 //add to the panel
		 GridBagConstraints ct = new GridBagConstraints();
		 ct.gridx=0;
		 ct.gridy=0;
		 panTracing.add(roiPointMode,ct);
		 ct.gridx++;
		 panTracing.add(roiPolyLineMode,ct);
		 ct.gridx++;
		 panTracing.add(roiPolySemiAMode,ct);
		 //filler
		 ct.gridx++;
		 ct.weightx = 0.01;
		 panTracing.add(new JLabel(), ct);


		 ///RoiLIST and buttons
		 mode = RoiManager3D.ADD_POINT_LINE;
		 //mode = RoiManager3D.ADD_POINT;
		 listModel = new  DefaultListModel<String>();
		 jlist = new JList<String>(listModel);
		 jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		 jlist.setLayoutOrientation(JList.VERTICAL);
		 jlist.setVisibleRowCount(-1);
		 jlist.addListSelectionListener(this);
		 listScroller = new JScrollPane(jlist);
		 listScroller.setPreferredSize(new Dimension(170, 400));
		 listScroller.setMinimumSize(new Dimension(170, 250));
		 JPanel roiList = new JPanel(new GridBagLayout());
		 roiList.setBorder(new PanelTitle(" ROI Manager "));

		 GridBagConstraints cr = new GridBagConstraints();
		 cr.gridx=0;
		 cr.gridy=0;
		 //cr.weighty=0.5;
		 cr.gridheight=GridBagConstraints.REMAINDER;
		 roiList.add(listScroller,cr);
		 butDelete = new JButton("Delete");
		 butDelete.addActionListener(this);
		 cr.gridx++;
		 cr.gridy++;
		 cr.gridheight=1;
		 //cr.fill = GridBagConstraints.NONE;
		 roiList.add(butDelete,cr);
		 butRename = new JButton("Rename");
		 butRename.addActionListener(this);
		 cr.gridy++;
		 roiList.add(butRename,cr);
		 
		 butDeselect = new JButton("Deselect");
		 butDeselect.addActionListener(this);
		 cr.gridy++;
		 roiList.add(butDeselect,cr);
		 
		 butProperties = new JButton("Properties");
		 butProperties.addActionListener(this);
		 cr.gridy++;
		 roiList.add(butProperties ,cr);
		 
		 butShowAll = new JToggleButton("Show all");
		 butShowAll.addActionListener(this);
		 butShowAll.setSelected(true);
		 cr.gridy++;
		 roiList.add(butShowAll ,cr);
	
		 butSaveROIs = new JButton("Save ROIs");
		 butSaveROIs.addActionListener(this);
		 cr.gridy++;
		 roiList.add(butSaveROIs ,cr);
		 
		 butLoadROIs = new JButton("Load ROIs");
		 butLoadROIs.addActionListener(this);
		 cr.gridy++;
		 roiList.add(butLoadROIs ,cr);
		 
		 // a solution for now
		 butDelete.setMinimumSize(butProperties.getPreferredSize());
		 butDelete.setPreferredSize(butProperties.getPreferredSize());
		 butRename.setMinimumSize(butProperties.getPreferredSize());		 
		 butRename.setPreferredSize(butProperties.getPreferredSize());
		 butDeselect.setMinimumSize(butProperties.getPreferredSize());		 
		 butDeselect.setPreferredSize(butProperties.getPreferredSize());
		 butShowAll.setMinimumSize(butProperties.getPreferredSize());		 
		 butShowAll.setPreferredSize(butProperties.getPreferredSize());

	     // Blank/filler component
		 cr.gridx++;
		 cr.gridy++;
		 cr.weightx = 0.01;
	     cr.weighty = 0.01;
	     roiList.add(new JLabel(), cr);
		
	     
		 GridBagConstraints c = new GridBagConstraints();
		 setLayout(new GridBagLayout());
		 c.insets=new Insets(4,4,2,2);
	     c.gridx=0;
		 c.gridy=0;
		 //c.weightx=1.0;
		 //c.gridwidth=GridBagConstraints.REMAINDER;
		 c.fill = GridBagConstraints.HORIZONTAL;
		 //c.fill=GridBagConstraints.REMAINDER;
		 //tracing
		 add(panTracing,c);
		 //roi list
		 c.gridy++;
		 add(roiList,c);
	      // Blank/filler component
		 c.gridy++;
		 c.weightx = 0.01;
	     c.weighty = 0.01;
	     add(new JLabel(), c);
		 
	 }

	 
	 public void addRoi(Roi3D roi_in)
	 {
		 rois.add(roi_in);		 
		 listModel.addElement(roi_in.getName());
		 jlist.setSelectedIndex(rois.size()-1);
		 activeRoi = rois.size()-1;

	 }

	 public Roi3D getActiveRoi()
	 {
		 return rois.get(activeRoi);
	 }
	 /** removes ROI and updates ListModel
	  * does not update activeRoi index! **/
	 public void removeRoi(int roiIndex)
	 {
		 int nVal=activeRoi;
		 if(roiIndex<rois.size())
		 {
			 rois.remove(roiIndex);
			 listModel.removeElementAt(roiIndex);
			 //activeRoi = -1;
		 }
		 //not sure what is going on here (why activeRoi becomes -1),
		 //but workaround for now
		 activeRoi=nVal-1;
		 if(activeRoi<0)
		 {
			 jlist.clearSelection();
		 }
	 }
	 /** removes active ROI and updates ListModel
	  * and activeRoi index **/
	 public void removeActiveRoi()
	 {
		 
		 if(activeRoi>=0)
		 {
			 rois.remove(activeRoi);
			 listModel.removeElementAt(activeRoi);
			 activeRoi--;
			 if(activeRoi<0)
			 {
				 jlist.clearSelection();
			 }
			 fireActiveRoiChanged(activeRoi);
		 }

	 }
	 public void removeAll()
	 {
		 rois =  new ArrayList<Roi3D >();
	 }
	 
	 /** Draw all ROIS **/
	 public void draw(GL3 gl, Matrix4fc pvm, int[] screen_size)
	 {
	       Roi3D roi;
	       Color savePointColor= defaultPointColor;
	       Color saveLineColor = defaultLineColor;
	       int i;
	       for (i=0;i<rois.size();i++) 
	       {
	    	   roi=rois.get(i);
	    	   if(i==activeRoi)
	    	   {
	    		   savePointColor = roi.getPointColor();
	    		   saveLineColor = roi.getLineColor();
	    		   roi.setPointColorRGB(activePointColor);
	    		   roi.setLineColorRGB(activeLineColor);
	    	   }
	    	   if(bShowAll)
	    	   {
	    		   roi.draw(gl, pvm, screen_size);
	    	   }
	    	   else
	    	   {
	    		   if(i==activeRoi)
	    		   {
	    			   roi.draw(gl, pvm, screen_size);
	    		   }
	    	   }
	    	   if(i==activeRoi)
	    	   {
	    		   roi.setPointColor(savePointColor);
	    		   roi.setLineColor(saveLineColor);
	    	   }
	       }
	 }
	 
	 public void addPoint(RealPoint point_)
	 {
		 if(mode ==RoiManager3D.ADD_POINT)
		 {
			 addPoint3D(point_);
		 }
		 if(mode ==RoiManager3D.ADD_POINT_LINE)
		 {
			 addPointToLine(point_);
		 }
	 }
	 public void addPoint3D(RealPoint point_)
	 {
		 addRoi( new Point3D(point_, currPointSize, activePointColor));
	 }
	 
	 public void addSegment(RealPoint point_, ArrayList<RealPoint> segments_)
	 {
		 LineTrace3D tracing;
		 //new Line
		 if(activeRoi<0 || rois.get(activeRoi).getType()!=Roi3D.LINE_TRACE)
		 {
			 tracing = new LineTrace3D(currLineThickness, currPointSize, activeLineColor, activePointColor, currRenderType, currSectorN);
			 addRoi(tracing);
			 tracing.addFirstPoint(point_);
			 //activeRoi = rois.size()-1; 
			 return;
		 }
		 //active ROI is not a line
		 /*if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 return;
		 }*/
		 //add point to line
		 else
		 {
			 tracing = (LineTrace3D) rois.get(activeRoi);
			 tracing.addPointAndSegment(point_,segments_);
		 }
	 }
	 public RealPoint getLastTracePoint()
	 { 
		 LineTrace3D tracing;
		 tracing = (LineTrace3D) rois.get(activeRoi);
		 return tracing.vertices.get(tracing.vertices.size()-1);
	 }

	 
	 public boolean removeSegment()
	 {
		 LineTrace3D tracing;
		 tracing = (LineTrace3D) rois.get(activeRoi);
		 return tracing.removeLastSegment();
	 }

	 /** adds point to active polyline
	  *  if active ROI is not a polyline, does nothing
	  *  if there are no active ROIS, starts new polyline **/
	 public void addPointToLine(RealPoint point_)
	 {
		 PolyLine3D polyline;
		 //new Line
		 if(activeRoi<0 || rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 polyline = new PolyLine3D(currLineThickness, currPointSize, activeLineColor, activePointColor, currRenderType, currSectorN);
			 addRoi(polyline);
			 polyline.addPointToEnd(point_);
			 //activeRoi = rois.size()-1; 
			 return;
		 }
		 //active ROI is not a line
		 /*if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 return;
		 }*/
		 //add point to line
		 else
		 {
			 polyline = (PolyLine3D) rois.get(activeRoi);
			 polyline.addPointToEnd(point_);
		 }
	 
	 }
	 
	 /** removes point from the active polyline
	  *  if active ROI is not a polyline, does nothing
	  *  if it is a last point, removes polyline object
	  *  and activates previous Roi in the list (if any) 
	  * **/
	 public void removePointFromLine()
	 {
		 PolyLine3D polyline;
		 if(activeRoi<0)
			 return;
		 //active ROI is not a line or none ROI selected
		 if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 return;
		 }
		 else
		 {
			 polyline = (PolyLine3D) rois.get(activeRoi);
			 if(!polyline.removeEndPoint())
			 {
				 //rois.remove(activeRoi);
				 removeRoi(activeRoi);
				 if(activeRoi>=0)
				 {
					 if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
					 {
						 activeRoi=-1;
						 jlist.clearSelection();
					 }
					 else
					 {
						 jlist.setSelectedIndex(activeRoi);
					 }
				 }
			 }
		 }
	 }
	 public void setLockMode(boolean bLockMode)
	 {
		 	 boolean bState = !bLockMode;
			 roiPointMode.setEnabled(bState);
			 roiPolyLineMode.setEnabled(bState);
			 butDelete.setEnabled(bState);
			 butRename.setEnabled(bState);
			 butDeselect.setEnabled(bState);
			 butProperties.setEnabled(bState);
			 butSaveROIs.setEnabled(bState);
			 butLoadROIs.setEnabled(bState);
			 listScroller.setEnabled(bState);
			 jlist.setEnabled(bState);

	 }
	 public void unselect()
	 {
		 activeRoi=-1;
		 jlist.clearSelection();
		 fireActiveRoiChanged(activeRoi);
	 }
	 
	 public void addRoiManager3DListener(Listener l) {
		 listeners.add(l);
	 }
	 
	 private void fireActiveRoiChanged(int nRoi) 
	 {
		for(Listener l : listeners)
			l.activeRoiChanged(nRoi);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) 
	{
		
		if (e.getValueIsAdjusting() == false) 
		{
			 
            if (jlist.getSelectedIndex() == -1) 
            {
            	activeRoi=-1;
            //No selection: disable delete, up, and down buttons.
                //deleteButton.setEnabled(false);
                //upButton.setEnabled(false);
                //downButton.setEnabled(false);
                //nameField.setText("");
 
            } else if (jlist.getSelectedIndices().length > 1) {
            //Multiple selection: disable up and down buttons.
                //deleteButton.setEnabled(true);
                //upButton.setEnabled(false);
                //downButton.setEnabled(false);
 
            } else {
            //Single selection: permit all operations.
                //deleteButton.setEnabled(true);
                //upButton.setEnabled(true);
                //downButton.setEnabled(true);
                //nameField.setText(list.getSelectedValue().toString());
            	activeRoi=jlist.getSelectedIndex();
            	fireActiveRoiChanged(jlist.getSelectedIndex()); 
            }
        }
    }

	//buttons
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		//TRACING TYPE
		if(e.getSource() == roiPointMode)
		{
			this.mode = RoiManager3D.ADD_POINT;
			unselect();
		}
		if(e.getSource() == roiPolyLineMode)
		{
			this.mode = RoiManager3D.ADD_POINT_LINE;
			unselect();
		}
		if(e.getSource() == roiPolySemiAMode)
		{
			this.mode = RoiManager3D.ADD_POINT_SEMIAUTOLINE;
			unselect();
		}
		
		//SHOW ALL BUTTON
		if(e.getSource() == butShowAll)
		{
			this.bShowAll=butShowAll.isSelected();
			fireActiveRoiChanged(activeRoi); 
		}
		
		//SAVE ROIS
		if(rois.size()>0)
		{
			
			if(e.getSource() == butSaveROIs)
			{
				diagSaveROIs();
			}
		}
		//LOAD ROIS
		if(e.getSource() == butLoadROIs)
		{
			diagLoadROIs();
		}
		
		///SIDE ROI SPECIFIC LIST BUTTONS
		if(activeRoi>=0)
		{
			//DELETE
			if(e.getSource() == butDelete)
			{
				 removeRoi(activeRoi);
				 if(activeRoi>=0)
				 {
					 jlist.setSelectedIndex(activeRoi);
				 }
				 else
				 {
					 jlist.clearSelection();
				 }
				 fireActiveRoiChanged(activeRoi); 
			}
			//RENAME
			if(e.getSource() == butRename)
			{
		
				String s = (String)JOptionPane.showInputDialog(
						this,
						"New name:",
						"Rename ROI",
						JOptionPane.PLAIN_MESSAGE,
						null,
						null,
						rois.get(activeRoi).getName());

				//If a string was returned, rename
				if ((s != null) && (s.length() > 0)) 
				{
					rois.get(activeRoi).setName(s);
					listModel.set(activeRoi,s);
					return;
				}

			}
			//DESELECT
			if(e.getSource() == butDeselect)
			{
				unselect();
				 
			}
			//PROPERTIES
			if(e.getSource() == butProperties)
			{
				dialProperties();
			}

		}

		
	}
	
	/** show ROI Properties dialog**/
	public void dialProperties()
	{
		JPanel dialProperties = new JPanel(new GridBagLayout());
		GridBagConstraints cd = new GridBagConstraints();
		NumberField nfPointSize = new NumberField(4);
		NumberField nfLineThickness = new NumberField(4);
		NumberField nfOpacity = new NumberField(4);
		String[] sRenderType = { "Center line", "Wire", "Surface" };
		JComboBox<String> renderTypeList = new JComboBox<String>(sRenderType);
		nfPointSize.setText(Float.toString(rois.get(activeRoi).getPointSize()));
		nfLineThickness.setText(Float.toString(rois.get(activeRoi).getLineThickness()));
		DecimalFormat df = new DecimalFormat("0.00");
		nfOpacity.setText(df.format(rois.get(activeRoi).getOpacity()));
		nfOpacity.setLimits(0.0, 1.0);
		
		cd.gridx=0;
		cd.gridy=0;
		//cd.anchor=GridBagConstraints.WEST;
		dialProperties.add(new JLabel("Point size: "),cd);
		cd.gridx++;
		dialProperties.add(nfPointSize,cd);
		if(rois.get(activeRoi).getType()>Roi3D.POINT)
		{
			cd.gridx=0;
			cd.gridy++;
			dialProperties.add(new JLabel("Line thickness: "),cd);
			cd.gridx++;
			dialProperties.add(nfLineThickness,cd);
		}
		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Opacity: "),cd);
		cd.gridx++;
		dialProperties.add(nfOpacity,cd);
		
		if(rois.get(activeRoi).getType()>Roi3D.POINT)
		{
			cd.gridx=0;
			cd.gridy++;
			dialProperties.add(new JLabel("Render as: "),cd);
			renderTypeList.setSelectedIndex(rois.get(activeRoi).getRenderType());
			cd.gridx++;
			dialProperties.add(renderTypeList,cd);
		}
		
		int reply = JOptionPane.showConfirmDialog(null, dialProperties, "ROI Properties", 
		        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			//point size 
			rois.get(activeRoi).setPointSize(Float.parseFloat(nfPointSize.getText()));
			//opacity
			float fNewOpacity= Float.parseFloat(nfOpacity.getText());
			if(fNewOpacity<0.0f)
				{fNewOpacity=0.0f;}
			if(fNewOpacity>1.0f)
				{fNewOpacity=1.0f;}
			rois.get(activeRoi).setOpacity(fNewOpacity);

			//line
			if(rois.get(activeRoi).getType()>Roi3D.POINT)
			{
				//line thickness
				float fNewLineThickess = Float.parseFloat(nfLineThickness.getText());
				if(Math.abs(fNewLineThickess-rois.get(activeRoi).getLineThickness())>0.00001)
				{
					rois.get(activeRoi).setLineThickness(fNewLineThickess );
				}
				//render type
				if(renderTypeList.getSelectedIndex()!=rois.get(activeRoi).getRenderType())
				{
					rois.get(activeRoi).setRenderType(renderTypeList.getSelectedIndex());
				}
				
				//if both are changed, we rebuild mesh twice
				//possibly optimize it in the future
			}
			
			fireActiveRoiChanged(activeRoi); 
		}
	}
	
	/** Save ROIS dialog and saving **/
	public void diagSaveROIs()
	{
		String filename;
		
		filename = bt.btdata.sFileNameImg + "_btrois";
		SaveDialog sd = new SaveDialog("Save ROIs ", filename, ".csv");
        String path = sd.getDirectory();
        if (path==null)
        	return;
        filename = path+sd.getFileName();
        bt.bInputLock = true;
        this.setLockMode(true);
        ROIsSaveBG saveTask = new ROIsSaveBG();
        saveTask.sFilename=filename;
        saveTask.bt=this.bt;
        saveTask.addPropertyChangeListener(bt.btpanel);
        saveTask.execute();
        this.setLockMode(false);
	}
	/** Save ROIS dialog and saving **/
	public void diagLoadROIs()
	{
		String filename;
		String[] line_array;
        int bFirstPartCheck = 0;
        int nRoiN=0;
        int nVertN=0;
        int i,j;
        
        ArrayList<RealPoint> vertices;
        ArrayList<RealPoint> segment;
        
        float pointSize=0.0f;
        float lineThickness =0.0f;
        Color pointColor = Color.BLACK;
        Color lineColor = Color.BLACK;
        String sName = "";
        int nRoiType = Roi3D.POINT;
        int nRenderType = 0;
        int nSectorN = 16;
        //Roi3D roiIn;
        
		OpenDialog openDial = new OpenDialog("Load BigTrace ROIs","", "*.csv");
		
        String path = openDial.getDirectory();
        if (path==null)
        	return;

        filename = path+openDial.getFileName();
        
        bt.bInputLock = true;
        this.setLockMode(true);


		try {
			
	        BufferedReader br = new BufferedReader(new FileReader(filename));
	        int nLineN = 0;

	        String line;

			while ((line = br.readLine()) != null) 
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

				  }
				  if(line_array[0].equals("Type"))
				  {						  
					  nRoiType = Roi3D.stringTypeToInt(line_array[1]);
				  }
				  if(line_array[0].equals("Name"))
				  {						  
					  sName = line_array[1];
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
						  Point3D roiP = new Point3D(pointSize,pointColor);
						  roiP.setName(sName);
						  roiP.setVertex(vertices.get(0));
						  this.addRoi(roiP);
					  	  break;
					  case Roi3D.POLYLINE:
						  PolyLine3D roiPL = new PolyLine3D(lineThickness,pointSize,lineColor,pointColor,nRenderType,nSectorN);
						  roiPL.setName(sName);
						  roiPL.setVertices(vertices);
						  this.addRoi(roiPL);
						  break;
					  case Roi3D.LINE_TRACE:
						  LineTrace3D roiLT = new LineTrace3D(lineThickness,pointSize,lineColor,pointColor,nRenderType,nSectorN);
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
						  this.addRoi(roiLT);
						  break;
						  
					  }
					  
				  }
				  
				}

	        br.close();
		}
		//catching errors in file opening
		catch (FileNotFoundException e) {
			IJ.error(""+e);
		}	        
		catch (IOException e) {
			IJ.error(""+e);
		}
        
		//some error reading the file
        if(bFirstPartCheck!=4)
        {
        	 System.err.println("Not a Bigtrace ROI file format or plugin/version mismatch, loading ROIs aborted.");
        }
        bt.bInputLock = false;
        this.setLockMode(false);
        
	}
		
	
}

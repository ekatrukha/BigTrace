package bigtrace.rois;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.joml.Matrix4fc;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.jogamp.opengl.GL3;

import bdv.tools.brightness.ColorIcon;
import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.geometry.ShapeInterpolation;
import bigtrace.gui.NumberField;
import bigtrace.gui.PanelTitle;
import bigtrace.scene.VisPolyLineScaled;
import ij.Prefs;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import net.imglib2.RealPoint;



public class RoiManager3D extends JPanel implements ListSelectionListener, ActionListener {
	

	 BigTrace bt;

	 private static final long serialVersionUID = -2843907862066423151L;
	 public static final int ADD_POINT=0, ADD_POINT_LINE=1, ADD_POINT_SEMIAUTOLINE=2, ADD_POINT_PLANE=3;
	 public static final int SECTORS_DEF=16;
	 
	 public ArrayList<Roi3D> rois =  new ArrayList<Roi3D >();
	 public int activeRoi = -1;
	 
	 public ArrayList<Roi3DGroup> groups = new ArrayList<Roi3DGroup>();
	 
	 final static String sUndefinedGroupName = "*undefined*";
	 
	 public int nActiveGroup = 0;
	 	 
	 public Color activePointColor = Color.YELLOW;
	 public Color activeLineColor = Color.RED;
	 
	 public ColorUserSettings selectColors = new ColorUserSettings(); 
	 public int mode;
	 public boolean bShowAll = true;

	 //MEASURE OBJECT
	 public RoiMeasure3D roiMeasure = null;
	 
	 //GUI
	 public DefaultListModel<String> listModel; 
	 public JList<String> jlist;
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
	 JButton butROIGroups;
	 JButton butApplyGroup;
	 JButton butDisplayGroup;
	 
	 public JComboBox<String> cbActiveChannel;
	 JComboBox<String> cbActiveGroup;
	 
	 JToggleButton roiPointMode;
	 JToggleButton roiPolyLineMode;
	 JToggleButton roiPolySemiAMode;
	 JToggleButton roiPlaneMode;
	 JButton roiSettings;
	 
	 
	 private ArrayList<Listener> listeners = new ArrayList<Listener>();

		
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
		 
	     //initialize new *undefined* ROI group
	     groups.add(new Roi3DGroup(sUndefinedGroupName, 6.0f, Color.GREEN, 4.0f, Color.BLUE, VisPolyLineScaled.WIRE,RoiManager3D.SECTORS_DEF) );
	     nActiveGroup = 0;
	     
		 URL icon_path = bigtrace.BigTrace.class.getResource("/icons/dot.png");
		 ImageIcon tabIcon = new ImageIcon(icon_path);
		 roiPointMode = new JToggleButton(tabIcon);
		 roiPointMode.setToolTipText("Trace single point");
		 roiPointMode.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		 //roiPointMode.setSelected(true);
			 
	     //icon_path = classLoader.getResource("icons/polyline.png").getFile();
		 icon_path =bigtrace.BigTrace.class.getResource("/icons/polyline.png");
		 //icon_path = RoiManager3D.class.getResource("/polyline.png").getFile();
		 //icon_path = RoiManager3D.class.getResource("icons/polyline.png").getFile();
		 tabIcon = new ImageIcon(icon_path);
		 roiPolyLineMode = new JToggleButton(tabIcon);
		 roiPolyLineMode.setToolTipText("Trace polyline");
		 roiPolyLineMode.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		 roiPolyLineMode.setSelected(true);
	     
		 icon_path = bigtrace.BigTrace.class.getResource("/icons/semiauto.png");
		 tabIcon = new ImageIcon(icon_path);
		 roiPolySemiAMode = new JToggleButton(tabIcon);
		 roiPolySemiAMode.setToolTipText("Semi auto trace");
		 roiPolySemiAMode.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		 //roiPolySemiAMode.setSelected(true);	
		 
		 icon_path = bigtrace.BigTrace.class.getResource("/icons/plane.png");
		 tabIcon = new ImageIcon(icon_path);
		 roiPlaneMode = new JToggleButton(tabIcon);
		 roiPlaneMode.setToolTipText("Plane");
		 roiPlaneMode.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		 //roiPolySemiAMode.setSelected(true);	
		 
		 icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		 tabIcon = new ImageIcon(icon_path);
		 roiSettings = new JButton(tabIcon);
		 roiSettings.setToolTipText("Settings");
		 roiSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));

		 //button group	 
		 roiTraceMode.add(roiPointMode);
		 roiTraceMode.add(roiPolyLineMode);
		 roiTraceMode.add(roiPolySemiAMode);
		 roiTraceMode.add(roiPlaneMode);
		 
		 roiPointMode.addActionListener(this);
		 roiPolyLineMode.addActionListener(this);
		 roiPolySemiAMode.addActionListener(this);
		 roiPlaneMode.addActionListener(this);
		 
		 roiSettings.addActionListener(this);
		 //add to the panel
		 GridBagConstraints ct = new GridBagConstraints();
		 ct.gridx=0;
		 ct.gridy=0;
		 panTracing.add(roiPointMode,ct);
		 ct.gridx++;
		 panTracing.add(roiPolyLineMode,ct);
		 ct.gridx++;
		 panTracing.add(roiPolySemiAMode,ct);
		 ct.gridx++;
		 panTracing.add(roiPlaneMode,ct);
		 ct.gridx++;
		 JSeparator sp = new JSeparator(SwingConstants.VERTICAL);
		 sp.setPreferredSize(new Dimension((int) (nButtonSize*0.5),nButtonSize));
		 panTracing.add(sp,ct);
		 ct.gridx++;
		 //panTracing.add(roiSettings,ct);
		 //filler
		 //ct.gridx++;
		 ct.weightx = 0.01;
		 panTracing.add(new JLabel(), ct);
		 ct.gridx++;
		 panTracing.add(roiSettings,ct);

		

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
		 cr.anchor = GridBagConstraints.NORTHWEST;
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
		 
		 butROIGroups = new JButton("Groups");
		 butROIGroups.addActionListener(this);
		 cr.gridy++;
		 roiList.add(butROIGroups ,cr);
		 
		 
		 

	     // Blank/filler component
		 cr.gridx++;
		 cr.gridy++;
		 cr.weightx = 0.01;
	     cr.weighty = 0.01;
	     roiList.add(new JLabel(), cr);		 
		 
		 
		 // a solution for now
		 butDelete.setMinimumSize(butProperties.getPreferredSize());
		 butDelete.setPreferredSize(butProperties.getPreferredSize());
		 butRename.setMinimumSize(butProperties.getPreferredSize());		 
		 butRename.setPreferredSize(butProperties.getPreferredSize());
		 butDeselect.setMinimumSize(butProperties.getPreferredSize());		 
		 butDeselect.setPreferredSize(butProperties.getPreferredSize());
		 butShowAll.setMinimumSize(butProperties.getPreferredSize());		 
		 butShowAll.setPreferredSize(butProperties.getPreferredSize());
		 butROIGroups.setMinimumSize(butProperties.getPreferredSize());
		 butROIGroups.setPreferredSize(butProperties.getPreferredSize());

		 JPanel panChannel = new JPanel(new GridBagLayout());
		 panChannel.setBorder(new PanelTitle(""));
		 
		 String[] nCh = new String[bt.btdata.nTotalChannels];
		 for(int i=0;i<nCh.length;i++)
		 {
			 nCh[i] = "channel "+Integer.toString(i+1);
		 }
		 cbActiveChannel = new JComboBox<>(nCh);
		 cbActiveChannel.setSelectedIndex(0);
		 cbActiveChannel.addActionListener(this);
		 
		 
		 cr = new GridBagConstraints();
	     cr.gridx=0;
		 cr.gridy=0;
		 panChannel.add(new JLabel("Active"),cr);
		 cr.gridx++;
		 panChannel.add(cbActiveChannel,cr);

		 JPanel panGroup = new JPanel(new GridBagLayout());
		 panGroup.setBorder(new PanelTitle(" Groups "));
		 
		 String[] nGroupNames = new String[groups.size()];
		 for(int i=0;i<nGroupNames.length;i++)
		 {
			 nGroupNames[i] = groups.get(i).getName();
		 }
		 cbActiveGroup = new JComboBox<>(nGroupNames);
		 cbActiveGroup.setSelectedIndex(0);
		 cbActiveGroup.setPrototypeDisplayValue("tyrosinated");
		 cbActiveGroup.addActionListener(this);
		 butApplyGroup = new JButton("Apply");
		 butApplyGroup.addActionListener(this);
		 icon_path =bigtrace.BigTrace.class.getResource("/icons/group_visibility.png");
		 tabIcon = new ImageIcon(icon_path);
		 butDisplayGroup = new JButton(tabIcon);
		 butDisplayGroup.setToolTipText("Toggle ROI groups visibility");
		 butDisplayGroup.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		 butDisplayGroup.addActionListener(this);

		 
		 cr = new GridBagConstraints();
	     cr.gridx=0;
		 cr.gridy=0;
		 panGroup.add(butDisplayGroup,cr);
		 cr.gridx++;
		 panGroup.add(cbActiveGroup,cr);
		 cr.gridx++;
		 panGroup.add(butApplyGroup,cr);
 
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
		 c.gridy++;
		 add(panChannel,c);
		 c.gridy++;
		 add(panGroup,c);
	      // Blank/filler component
		 c.gridy++;
		 c.weightx = 0.01;
	     c.weighty = 0.01;
	     add(new JLabel(), c);    
		 
	 }
	 /** makes an empty initial ROI of specific type **/
	 public synchronized void addRoi(Roi3D newRoi)
	 {		
		 rois.add(newRoi);		 
		 //listModel.addElement(newRoi.getName());
		 listModel.addElement(getGroupRoiName(newRoi));
		 jlist.setSelectedIndex(rois.size()-1);
		 activeRoi = rois.size()-1;

	 }
	 
	 
	 public Roi3D makeRoi(int nRoiType)
	 {
		 Roi3D newRoi;
		 
		 switch (nRoiType)
		 {
		 case Roi3D.POINT:
			 newRoi= new Point3D(groups.get(nActiveGroup));
			 break;
		 case Roi3D.POLYLINE:
			 newRoi = new PolyLine3D(groups.get(nActiveGroup));
			 break;
		 case Roi3D.LINE_TRACE:
			 newRoi = new LineTrace3D(groups.get(nActiveGroup));
			 break;
		 case Roi3D.PLANE:
			 newRoi = new Plane3D(groups.get(nActiveGroup));
			 break;
		 case Roi3D.CUBE:
			 newRoi = new Cube3D(groups.get(nActiveGroup));
			 break;
		 default:
			 newRoi= new Point3D(groups.get(nActiveGroup));
		 }
		 newRoi.setGroupInd(nActiveGroup);
		 return newRoi;
	 }
	 
	 /** returns name of roi with prefix of group name in squared brackets**/
	 public String getGroupRoiName(Roi3D nRoi)
	 {
		 String nFullName;
		 final int nInd = nRoi.getGroupInd(); 
		 if(nInd == 0 || (nInd> groups.size()-1))
		 {
			 nFullName = nRoi.getName();
		 }
		 else
		 {
			 int nGNameLength = groups.get(nInd).getName().length();
			 nGNameLength = Math.min(nGNameLength, 3);
			 nFullName = "["+groups.get(nInd).getName().substring(0, nGNameLength)+"]"+ nRoi.getName();
		 }
		 return nFullName;
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
	       Color savePointColor= null;
	       Color saveLineColor = null;
	       int i;
	       for (i=0;i<rois.size();i++) 
	       {
	    	   roi=rois.get(i);
	    	   //save colors in case ROI is active
	    	   if(i==activeRoi)
	    	   {
	    		   savePointColor = roi.getPointColor();
	    		   saveLineColor = roi.getLineColor();
	    		   roi.setPointColorRGB(activePointColor);
	    		   roi.setLineColorRGB(activeLineColor);
	    	   }
	    	   if(bShowAll)
	    	   {
	    		   if(groups.get(roi.getGroupInd()).bVisible)
	    		   {
	    			   roi.draw(gl, pvm, screen_size);
	    		   }
	    		   else
	    		   {
	    			   //still draw active ROI
		    		   if(i==activeRoi)
		    		   {
		    			   roi.draw(gl, pvm, screen_size);
		    		   }	    			   
	    		   }
	    	   }
	    	   else
	    	   {
	    		   if(i==activeRoi)
	    		   {
	    			   roi.draw(gl, pvm, screen_size);
	    		   }
	    	   }
	    	  
	    	   //restore colors in case ROI is active
	    	   if(i==activeRoi)
	    	   {
	    		   roi.setPointColor(savePointColor);
	    		   roi.setLineColor(saveLineColor);
	    	   }
	       }
	 }
	 
	 /**adds new point to Point3D, Polyline3D or Plane3D ROI **/
	 public void addPoint(RealPoint point_)
	 {
		 
		 switch (mode){
		 
		 case RoiManager3D.ADD_POINT:
			 addPoint3D(point_);
			 break;
		 case RoiManager3D.ADD_POINT_LINE:
			 addPointToLine(point_);
			 break;
		 case RoiManager3D.ADD_POINT_PLANE:
			 addPointToPlane(point_);
			 break;

		 }
		 
	 }
	 public void addPoint3D(RealPoint point_)
	 {
		 //addRoi( new Point3D(point_, presets.get(nActiveGroup).pointSize, presets.get(nActiveGroup).pointColor));
		 //addRoi( new Point3D(point_, groups.get(nActiveGroup)));
		 //Point3D pointROI =new Point3D(groups.get(nActiveGroup)); 
		 //addRoi( pointROI);
		 Point3D pointROI =(Point3D)makeRoi( Roi3D.POINT); 
		 pointROI.setVertex(point_);
		 addRoi(pointROI);
	 }
	 
	 public void addSegment(RealPoint point_, ArrayList<RealPoint> segments_)
	 {
		 LineTrace3D tracing;
		 //new Line
		 if(activeRoi<0 || rois.get(activeRoi).getType()!=Roi3D.LINE_TRACE)
		 {
			 //tracing = new LineTrace3D(currLineThickness, currPointSize, defaultLineColor, defaultPointColor, currRenderType, currSectorN);
			 //tracing = new LineTrace3D(groups.get(nActiveGroup));
			 //addRoi(tracing);
			 tracing = (LineTrace3D) makeRoi(Roi3D.LINE_TRACE);
			 tracing.addFirstPoint(point_);
			 addRoi(tracing);
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

	 /** adds point to active Polyline3D ROI
	  *  if active ROI is not a polyline, does nothing
	  *  if there are no active ROIS, starts new polyline **/
	 public void addPointToLine(RealPoint point_)
	 {

		 PolyLine3D polyline;
		 //new Line
		 if(activeRoi<0 || rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 polyline  = (PolyLine3D) makeRoi(Roi3D.POLYLINE);
			 polyline.addPointToEnd(point_);
			 addRoi(polyline);
			 //activeRoi = rois.size()-1; 
			 return;
		 }

		 //add point to line
		 else
		 {
			 polyline = (PolyLine3D) rois.get(activeRoi);
			 polyline.addPointToEnd(point_);
		 }
			
	 
	 }
	 
	 /** adds point to active plane3D ROI
	  *  if active ROI is not a plane, does nothing
	  *  if there are no active ROIS, starts new polyline **/
	 public void addPointToPlane(RealPoint point_)
	 {

		 Plane3D plane;
		 //new Plane
		 if(activeRoi<0 || rois.get(activeRoi).getType()!=Roi3D.PLANE)
		 {	
			 plane  = (Plane3D) makeRoi(Roi3D.PLANE);
			 plane.addPoint(point_);
			 addRoi(plane);
			 //activeRoi = rois.size()-1; 
			 return;
		 }

		 //add point to plane
		 else
		 {
			 plane = (Plane3D) rois.get(activeRoi);
			 plane.addPoint(point_);
		 }
			
	 
	 }
	 
	 /** removes point from the active polyline3D/plane3D ROIS
	  *  if active ROI is not that type, does nothing
	  *  if it is a last point, removes ROI object
	  *  and activates previous Roi in the list (if any of the same type) 
	  * **/
	 public void removePointLinePlane()
	 {
		 boolean bPointRemoved =false;
		 
		 if(activeRoi<0)
			 return;
		 final int nRoiType = rois.get(activeRoi).getType();
		 //active ROI is not a line or none ROI selected
		 if(nRoiType==Roi3D.POLYLINE || nRoiType==Roi3D.PLANE)
		 {
			 switch (nRoiType)
			 {
			 case Roi3D.POLYLINE:
				 bPointRemoved = ((PolyLine3D) rois.get(activeRoi)).removeEndPoint();
				 break;
			 case Roi3D.PLANE:
				 bPointRemoved = ((Plane3D) rois.get(activeRoi)).removePoint();
				 break;
			 }
			
			 if(!bPointRemoved)
			 {
				 removeRoi(activeRoi);
				 if(activeRoi>=0)
				 {
					 if(rois.get(activeRoi).getType()!=nRoiType)
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
			 roiPolySemiAMode.setEnabled(bState);
			 roiPlaneMode.setEnabled(bState);
			 roiSettings.setEnabled(bState);
			 butDelete.setEnabled(bState);
			 butRename.setEnabled(bState);
			 butDeselect.setEnabled(bState);
			 butProperties.setEnabled(bState);
			 butSaveROIs.setEnabled(bState);
			 butLoadROIs.setEnabled(bState);
			 butROIGroups.setEnabled(bState);
			 cbActiveChannel.setEnabled(bState);
			 cbActiveGroup.setEnabled(bState);
			 butApplyGroup.setEnabled(bState);
			 butDisplayGroup.setEnabled(bState);
			 
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
			
			roiMeasure.jlist.setSelectedIndex(jlist.getSelectedIndex());
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
			if(this.mode != RoiManager3D.ADD_POINT)
			{
				this.mode = RoiManager3D.ADD_POINT;
				unselect();
			}
		}
		if(e.getSource() == roiPolyLineMode)
		{
			if(this.mode != RoiManager3D.ADD_POINT_LINE)
			{
				this.mode = RoiManager3D.ADD_POINT_LINE;
				unselect();
			}
		}
		if(e.getSource() == roiPolySemiAMode)
		{
			if(this.mode != RoiManager3D.ADD_POINT_SEMIAUTOLINE)
			{
				this.mode = RoiManager3D.ADD_POINT_SEMIAUTOLINE;
				unselect();
			}
		}
		if(e.getSource() == roiPlaneMode)
		{
			if(this.mode != RoiManager3D.ADD_POINT_PLANE)
			{
				this.mode = RoiManager3D.ADD_POINT_PLANE;
				unselect();
			}
		}
		//SETTINGS
		if(e.getSource() == roiSettings)
		{
			dialSettings();
		}
		//ACTIVE CHANNEL
		if(e.getSource() == cbActiveChannel)
		{
			bt.btdata.nChAnalysis=cbActiveChannel.getSelectedIndex();
			roiMeasure.cbActiveChannel.setSelectedIndex(bt.btdata.nChAnalysis);
		}
		
		//ACTIVE PRESET
		if(e.getSource() == cbActiveGroup)
		{
			//Dimension size= cbActiveGroup.getSize();
			if(nActiveGroup!=cbActiveGroup.getSelectedIndex())
			{
				nActiveGroup=cbActiveGroup.getSelectedIndex();
				//unselect();
				
			}
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
		
		//Groups Manager
		if(e.getSource() == butROIGroups)
		{
			Roi3DGroupManager dialGroup = new Roi3DGroupManager(this);
			dialGroup.initGUI();
			dialGroup.show();
			int nGroupSave = nActiveGroup;
			cbActiveGroup.removeAllItems();
			for(int i=0;i<groups.size();i++)
			{
				cbActiveGroup.addItem(groups.get(i).getName());
			}
			if(nGroupSave>(cbActiveGroup.getItemCount()-1))
			{
				cbActiveGroup.setSelectedIndex(0);
			}
			else
			{
				cbActiveGroup.setSelectedIndex(nGroupSave);
			}
			
		}
		//GROUP VISIBILITY
		if(e.getSource() == butDisplayGroup)
		{
			dialGroupVisibility();
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
			//APPLY GROUP
			
			if(e.getSource() == butApplyGroup)
			{
				Roi3D activeROI = rois.get(activeRoi);
				activeROI.setGroup(groups.get(nActiveGroup));
				activeROI.setGroupInd(nActiveGroup);
				listModel.setElementAt(getGroupRoiName(activeROI), activeRoi);
			}	
			

		}

		
	}
	
	/** show ROI Properties dialog**/
	public void dialSettings()
	{
		
		JTabbedPane tabPane = new JTabbedPane();
		GridBagConstraints cd = new GridBagConstraints();
	
		
		////////////GERERAL COLORS/INTERFACE
		JPanel pGeneral = new JPanel(new GridBagLayout());
		

		JButton butPointActiveColor = new JButton( new ColorIcon( activePointColor ) );	
		butPointActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.btpanel.finFrame, "Choose active point color", activePointColor );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 0);
				//setNewPointColor(newColor);
				butPointActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineActiveColor = new JButton( new ColorIcon( activeLineColor ) );	
		butLineActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.btpanel.finFrame, "Choose active line color", activeLineColor );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 1);

				butLineActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});

		NumberField nfZoomBoxSize = new NumberField(4);
		nfZoomBoxSize.setIntegersOnly(true);
		nfZoomBoxSize.setText(Integer.toString(bt.btdata.nZoomBoxSize));

		NumberField nfZoomBoxScreenFraction = new NumberField(4);
		nfZoomBoxScreenFraction.setText(Double.toString(bt.btdata.dZoomBoxScreenFraction));
		
		
		String[] sShapeInterpolationType = { "Voxel", "Subvoxel"};
		JComboBox<String> shapeInterpolationList = new JComboBox<String>(sShapeInterpolationType);
		shapeInterpolationList.setSelectedIndex(BigTraceData.shapeInterpolation);
		
		NumberField nfSmoothWindow = new NumberField(2);
		nfSmoothWindow.setIntegersOnly(true);
		nfSmoothWindow.setText(Integer.toString(BigTraceData.nSmoothWindow));
			
		
		cd.gridx=0;
		cd.gridy=0;
		pGeneral.add(new JLabel("Selected ROI point color: "),cd);
		cd.gridx++;
		pGeneral.add(butPointActiveColor,cd);
		cd.gridx=0;
		cd.gridy++;
		pGeneral.add(new JLabel("Selected ROI line color: "),cd);
		cd.gridx++;
		pGeneral.add(butLineActiveColor,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pGeneral.add(new JLabel("Zoom volume size (px): "),cd);
		cd.gridx++;
		pGeneral.add(nfZoomBoxSize,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pGeneral.add(new JLabel("Zoom screen fraction (0-1): "),cd);
		cd.gridx++;
		pGeneral.add(nfZoomBoxScreenFraction,cd);		
		cd.gridx=0;
		cd.gridy++;
		pGeneral.add(new JLabel("ROI Shape interpolation: "),cd);
		cd.gridx++;
		pGeneral.add(shapeInterpolationList,cd);	
		
		cd.gridx=0;
		cd.gridy++;
		pGeneral.add(new JLabel("Trace smoothing window (points): "),cd);
		cd.gridx++;
		pGeneral.add(nfSmoothWindow,cd);	
		
		////////////TRACING OPTIONS
		JPanel pTrace = new JPanel(new GridBagLayout());

		

		NumberField nfSigmaX = new NumberField(4);
		NumberField nfSigmaY = new NumberField(4);
		NumberField nfSigmaZ = new NumberField(4);
		NumberField nfGammaTrace = new NumberField(4);
		NumberField nfTraceBoxSize = new NumberField(4);
		NumberField nfTraceBoxScreenFraction = new NumberField(4);
		NumberField nfTBAdvance = new NumberField(4);
		JCheckBox cbTraceOnlyCrop = new JCheckBox();

		nfTraceBoxSize.setText(Integer.toString((int)(2.0*bt.btdata.lTraceBoxSize)));
		nfTraceBoxScreenFraction.setText(Double.toString(bt.btdata.dTraceBoxScreenFraction));
		nfSigmaX.setText(Double.toString(bt.btdata.sigmaTrace[0]));
		nfSigmaY.setText(Double.toString(bt.btdata.sigmaTrace[1]));
		nfSigmaZ.setText(Double.toString(bt.btdata.sigmaTrace[2]));
		nfGammaTrace.setText(Double.toString(bt.btdata.gammaTrace));
		nfTBAdvance.setText(Float.toString(bt.btdata.fTraceBoxAdvanceFraction));
		cbTraceOnlyCrop.setSelected(bt.btdata.bTraceOnlyCrop);
		
		cd.gridx=0;
		cd.gridy=0;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Curve thickness X axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaX,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pTrace.add(new JLabel("Curve thickness Y axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaY,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pTrace.add(new JLabel("Curve thickness Z axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaZ,cd);
		cd.gridx=0;
		
		cd.gridy++;
		pTrace.add(new JLabel("Orientation weight(0-1): "),cd);
		cd.gridx++;
		pTrace.add(nfGammaTrace,cd);
		
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Trace box size (px): "),cd);
		cd.gridx++;
		pTrace.add(nfTraceBoxSize,cd);
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Trace box screen fraction (0-1): "),cd);
		cd.gridx++;
		pTrace.add(nfTraceBoxScreenFraction,cd);
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Trace box advance [0-center..1-edge]: "),cd);
		cd.gridx++;
		pTrace.add(nfTBAdvance,cd);
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Trace only cropped volume: "),cd);
		cd.gridx++;
		pTrace.add(cbTraceOnlyCrop,cd);
		
		tabPane.addTab("General",pGeneral);
		tabPane.addTab("Tracing",pTrace);

		int reply = JOptionPane.showConfirmDialog(null, tabPane, "ROI Manager Settings", 
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

//		int reply = JOptionPane.showConfirmDialog(null, dialRoiSet, "ROI Manager Settings", 
		        //JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			

			Color tempC;
			
			tempC=selectColors.getColor(0);
			if(tempC!=null)
			{
				activePointColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				selectColors.setColor(null, 0);
			}
			tempC=selectColors.getColor(1);
			if(tempC!=null)
			{
				activeLineColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				selectColors.setColor(null, 1);
			}
			
			//SAVE SELECTIONS

			bt.btdata.nZoomBoxSize = Integer.parseInt(nfZoomBoxSize.getText());
			Prefs.set("BigTrace.nZoomBoxSize", bt.btdata.nZoomBoxSize);
			
			bt.btdata.dZoomBoxScreenFraction = Double.parseDouble(nfZoomBoxScreenFraction.getText());
			Prefs.set("BigTrace.dZoomBoxScreenFraction", (double)(bt.btdata.dZoomBoxScreenFraction));
			
			
			if(BigTraceData.nSmoothWindow != Integer.parseInt(nfSmoothWindow.getText())||BigTraceData.shapeInterpolation!= shapeInterpolationList.getSelectedIndex())
			{
				BigTraceData.nSmoothWindow = Integer.parseInt(nfSmoothWindow.getText());
				Prefs.set("BigTrace.nSmoothWindow", BigTraceData.nSmoothWindow);
				BigTraceData.shapeInterpolation= shapeInterpolationList.getSelectedIndex();
				Prefs.set("BigTrace.ShapeInterpolation",BigTraceData.shapeInterpolation);
				updateROIsDisplay();
			}
			
/*
			if(BigTraceData.nSmoothWindow != Integer.parseInt(nfSmoothWindow.getText()))
			{
				
				BigTraceData.nSmoothWindow = Integer.parseInt(nfSmoothWindow.getText());
				updateROIsDisplay();
				Prefs.set("BigTrace.nSmoothWindow", BigTraceData.nSmoothWindow);
			}*/
			
			
			bt.btdata.sigmaTrace[0] = Double.parseDouble(nfSigmaX.getText());
			Prefs.set("BigTrace.sigmaTraceX", (double)(bt.btdata.sigmaTrace[0]));
			
			bt.btdata.sigmaTrace[1] = Double.parseDouble(nfSigmaY.getText());
			Prefs.set("BigTrace.sigmaTraceY", (double)(bt.btdata.sigmaTrace[1]));
			
			bt.btdata.sigmaTrace[2] = Double.parseDouble(nfSigmaZ.getText());
			Prefs.set("BigTrace.sigmaTraceZ", (double)(bt.btdata.sigmaTrace[2]));
			
			bt.btdata.gammaTrace = Double.parseDouble(nfGammaTrace.getText());
			Prefs.set("BigTrace.gammaTrace", (double)(bt.btdata.gammaTrace));
			
			bt.btdata.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", (double)(bt.btdata.lTraceBoxSize));
			
			bt.btdata.dTraceBoxScreenFraction = Double.parseDouble(nfTraceBoxScreenFraction.getText());
			Prefs.set("BigTrace.dTraceBoxScreenFraction", (double)(bt.btdata.dTraceBoxScreenFraction));
			
			bt.btdata.fTraceBoxAdvanceFraction = Float.parseFloat(nfTBAdvance.getText());
			Prefs.set("BigTrace.fTraceBoxAdvanceFraction", (double)(bt.btdata.fTraceBoxAdvanceFraction));
			
			bt.btdata.bTraceOnlyCrop = cbTraceOnlyCrop.isSelected();
			Prefs.set("BigTrace.bTraceOnlyCrop", bt.btdata.bTraceOnlyCrop);
			
			
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
		
		Roi3D currentROI = rois.get(activeRoi);

		String[] sRenderType = { "Center line", "Wire", "Surface" };
		JComboBox<String> renderTypeList = new JComboBox<String>(sRenderType);
		nfPointSize.setText(Float.toString(currentROI.getPointSize()));
		nfLineThickness.setText(Float.toString(currentROI.getLineThickness()));
		DecimalFormat df = new DecimalFormat("0.00");
		nfOpacity.setText(df.format(currentROI.getOpacity()));
		nfOpacity.setLimits(0.0, 1.0);
		

		JButton butPointColor = new JButton( new ColorIcon( currentROI.getPointColor() ) );
		
		butPointColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.btpanel.finFrame, "Choose point color", currentROI.getPointColor() );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 0);
				butPointColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineColor  = new JButton( new ColorIcon( currentROI.getLineColor()) );

		
		butLineColor.addActionListener( e -> {
				Color newColor = JColorChooser.showDialog(bt.btpanel.finFrame, "Choose line color", currentROI.getPointColor() );
				if (newColor!=null)
				{	
					selectColors.setColor(newColor, 1);							
					butLineColor.setIcon(new ColorIcon(newColor));
				}
				
		});
		
		cd.gridx=0;
		cd.gridy=0;
		dialProperties.add(new JLabel("ROI Type: "),cd);
		cd.gridx++;
		dialProperties.add(new JLabel(Roi3D.intTypeToString(currentROI.getType())),cd);

		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Group: "),cd);
		cd.gridx++;
		//just in case
		if(currentROI.getGroupInd()> groups.size()-1)
		{
			dialProperties.add(new JLabel(RoiManager3D.sUndefinedGroupName),cd);
		}
		else
		{
			dialProperties.add(new JLabel(groups.get(currentROI.getGroupInd()).getName()),cd);	
		}
		
		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Point size: "),cd);
		cd.gridx++;
		dialProperties.add(nfPointSize,cd);
		
		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Point color: "),cd);
		cd.gridx++;
		dialProperties.add(butPointColor,cd);
		
		if(currentROI.getType()>Roi3D.POINT)
		{
			cd.gridx=0;
			cd.gridy++;
			dialProperties.add(new JLabel("Line thickness: "),cd);
			cd.gridx++;
			dialProperties.add(nfLineThickness,cd);
			cd.gridx=0;
			cd.gridy++;
			dialProperties.add(new JLabel("Line color: "),cd);
			cd.gridx++;
			dialProperties.add(butLineColor,cd);
		}
		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Opacity: "),cd);
		cd.gridx++;
		dialProperties.add(nfOpacity,cd);
		
		if(currentROI.getType()>Roi3D.POINT)
		{
			cd.gridx=0;
			cd.gridy++;
			dialProperties.add(new JLabel("Render as: "),cd);
			renderTypeList.setSelectedIndex(currentROI.getRenderType());
			cd.gridx++;
			dialProperties.add(renderTypeList,cd);
		}
		
		
		int reply = JOptionPane.showConfirmDialog(null, dialProperties, "ROI Properties", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		
		
		if (reply == JOptionPane.OK_OPTION) 
		{
			//point size 
			currentROI.setPointSize(Float.parseFloat(nfPointSize.getText()));
			
			//point color
			if(selectColors.getColor(0)!=null)
			{
				currentROI.setPointColorRGB(selectColors.getColor(0));
				selectColors.setColor(null, 0);
				//newPointColor = null;
			}
			//opacity
			float fNewOpacity= Float.parseFloat(nfOpacity.getText());
			if(fNewOpacity<0.0f)
				{fNewOpacity=0.0f;}
			if(fNewOpacity>1.0f)
				{fNewOpacity=1.0f;}
			currentROI.setOpacity(fNewOpacity);

			//line
			if(currentROI.getType()>Roi3D.POINT)
			{
				//line thickness
				float fNewLineThickess = Float.parseFloat(nfLineThickness.getText());
				if(Math.abs(fNewLineThickess-currentROI.getLineThickness())>0.00001)
				{
					currentROI.setLineThickness(fNewLineThickess );
				}
				//line color
				if(selectColors.getColor(1)!=null)
				{				
					currentROI.setLineColorRGB(selectColors.getColor(1));
					selectColors.setColor(null, 1);
				
				}
				//render type
				if(renderTypeList.getSelectedIndex()!=currentROI.getRenderType())
				{
					currentROI.setRenderType(renderTypeList.getSelectedIndex());
				}
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
        //this.setLockMode(true);
        ROIsSaveBG saveTask = new ROIsSaveBG();
        saveTask.sFilename=filename;
        saveTask.bt=this.bt;
        saveTask.addPropertyChangeListener(bt.btpanel);
        saveTask.execute();
        //this.setLockMode(false);
	}
	/** Load ROIS dialog and saving **/
	public void diagLoadROIs()
	{
		String filename;

   
		OpenDialog openDial = new OpenDialog("Load BigTrace ROIs","", "*.csv");
		
        String path = openDial.getDirectory();
        if (path==null)
        	return;

        filename = path+openDial.getFileName();
        
        bt.bInputLock = true;
        
        
        String [] sRoiLoadOptions = new String [] {"Clean load ROIs and groups","Append ROIs as undefined group"};
		
        String input = (String) JOptionPane.showInputDialog(this, "Loading ROIs",
                "Load mode:", JOptionPane.QUESTION_MESSAGE, null, // Use
                                                                                // default
                                                                                // icon
                sRoiLoadOptions, // Array of choices
                sRoiLoadOptions[(int)Prefs.get("BigTrace.LoadRoisMode", 0)]);
        
        if(input.isEmpty())
        	 return;
        int nLoadMode;
        if(input.equals("Clean load ROIs and groups"))
        {
        	nLoadMode = 0;
        	this.groups = new ArrayList<Roi3DGroup>();
        	this.rois = new ArrayList<Roi3D >();
        	listModel.clear();
        }
        else
        {
        	nLoadMode = 1;
        }
        
        Prefs.set("BigTrace.LoadRoisMode", nLoadMode);

        ROIsLoadBG loadTask = new ROIsLoadBG();
        
        loadTask.sFilename=filename;
        loadTask.nLoadMode = nLoadMode;
        loadTask.bt=this.bt;
        loadTask.addPropertyChangeListener(bt.btpanel);
        loadTask.execute();
        
	}

	/** updates ROIs image for a specific group **/
	void updateROIsGroupDisplay(int nGroupN)
	{
		Roi3DGroup updateGroup =groups.get(nGroupN);
		for (Roi3D roi : rois)
		{
			if(roi.getGroupInd()==nGroupN)
			{
				roi.setGroup(updateGroup);
			}
		}
	}
	
	/** updates all ROIs images**/
	void updateROIsDisplay()
	{

			for (Roi3D roi : rois)
			{
				roi.updateRenderVertices();
				//roi.setGroup(groups.get(roi.getGroupInd()));
			}
	}
	/** marks ROIs of specific group as undefined and updates ROI indexes**/
	public void markROIsUndefined(int nGroupN)
	{
		for (int i=0;i<rois.size();i++)
		{
			if(rois.get(i).getGroupInd()==nGroupN)
			{
				rois.get(i).setGroupInd(0);
				listModel.setElementAt(rois.get(i).getName(), i);
			}
		}
	}
	/** deletes ROIs of specific group **/
	public void deleteROIGroup(int nGroupN)
	{
		for (int i=(rois.size()-1);i>=0;i--)
		{
			if(rois.get(i).getGroupInd()==nGroupN)
			{
				 rois.remove(i);
				 listModel.removeElementAt(i);
			}
		}
		jlist.clearSelection();
	}
	
	/** show Group visibility dialog **/
	public void dialGroupVisibility()
	{
		Roi3DGroupVisibility groupVis = new Roi3DGroupVisibility(this);
		groupVis.show();
	}
	
	public void updateGroupsList()
	{
		 
		 cbActiveGroup.removeAllItems();
		 for(int i=0;i<groups.size();i++)
		 {
			 cbActiveGroup.addItem(groups.get(i).getName());
		 }
		 cbActiveGroup.setSelectedIndex(0);
		 nActiveGroup = 0;

	}
	public void setRoiMeasure3D( RoiMeasure3D roiMeasure_)
	{
		 this.roiMeasure=roiMeasure_;
	}
}

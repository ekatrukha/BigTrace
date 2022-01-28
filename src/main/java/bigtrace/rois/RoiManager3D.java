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
	 public static final int ADD_POINT=0, ADD_POINT_LINE=1, ADD_POINT_SEMIAUTOLINE=2;
	 public ArrayList<Roi3D> rois =  new ArrayList<Roi3D >();
	 public int activeRoi = -1;
	 
	 public Color activePointColor = Color.YELLOW;
	 public Color defaultPointColor = Color.GREEN;
	 public Color activeLineColor = Color.RED;
	 public Color defaultLineColor = Color.BLUE;
	 
	 public ColorUserSettings selectColors = new ColorUserSettings(); 

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
	 JButton roiSettings;
	 
	 
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
		 

		 URL icon_path = bigtrace.BigTrace.class.getResource("/icons/dot.png");
		 ImageIcon tabIcon = new ImageIcon(icon_path);
		 roiPointMode = new JToggleButton(tabIcon);
		 roiPointMode .setToolTipText("Trace single point");
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
		 
		 icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		 tabIcon = new ImageIcon(icon_path);
		 roiSettings = new JButton(tabIcon);
		 roiSettings.setToolTipText("Settings");
		 roiSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));

		 //button group	 
		 roiTraceMode.add(roiPointMode);
		 roiTraceMode.add(roiPolyLineMode);
		 roiTraceMode.add(roiPolySemiAMode);
		 
		 roiPointMode.addActionListener(this);
		 roiPolyLineMode.addActionListener(this);
		 roiPolySemiAMode.addActionListener(this);
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

	 
	 public synchronized void addRoi(Roi3D roi_in)
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
		 addRoi( new Point3D(point_, currPointSize, defaultPointColor));
	 }
	 
	 public void addSegment(RealPoint point_, ArrayList<RealPoint> segments_)
	 {
		 LineTrace3D tracing;
		 //new Line
		 if(activeRoi<0 || rois.get(activeRoi).getType()!=Roi3D.LINE_TRACE)
		 {
			 tracing = new LineTrace3D(currLineThickness, currPointSize, defaultLineColor, defaultPointColor, currRenderType, currSectorN);
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
			 polyline = new PolyLine3D(currLineThickness, currPointSize, defaultLineColor, defaultPointColor, currRenderType, currSectorN);
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
			 roiSettings.setEnabled(bState);
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
		//SETTINGS
		if(e.getSource() == roiSettings)
		{
			dialSettings();
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
	public void dialSettings()
	{
		
		JTabbedPane tabPane = new JTabbedPane();
		GridBagConstraints cd = new GridBagConstraints();
		
		
		////////////DEFAULT COLORS AND RENDER		
		JPanel pDefaults = new JPanel(new GridBagLayout());
		
		JButton butPointDefColor = new JButton( new ColorIcon( defaultPointColor ) );	
		butPointDefColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.finFrame, "Choose default point color", defaultPointColor );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 0);
				//setNewPointColor(newColor);
				butPointDefColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineDefColor = new JButton( new ColorIcon( defaultLineColor ) );	
		butLineDefColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.finFrame, "Choose default line color", defaultLineColor );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 1);
				//setNewPointColor(newColor);
				butLineDefColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butPointActiveColor = new JButton( new ColorIcon( activePointColor ) );	
		butPointActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.finFrame, "Choose active point color", activePointColor );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 2);
				//setNewPointColor(newColor);
				butPointActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineActiveColor = new JButton( new ColorIcon( activeLineColor ) );	
		butLineActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.finFrame, "Choose active line color", activeLineColor );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 3);
				//setNewPointColor(newColor);
				butLineActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		NumberField nfPointSize = new NumberField(4);
		NumberField nfLineThickness = new NumberField(4);
		nfPointSize.setText(Float.toString(currPointSize));
		nfLineThickness.setText(Float.toString(currLineThickness));

		
		String[] sRenderType = { "Center line", "Wire", "Surface" };
		JComboBox<String> renderTypeList = new JComboBox<String>(sRenderType);
		renderTypeList.setSelectedIndex(currRenderType);
		
		
		cd.gridx=0;
		cd.gridy=0;
		pDefaults.add(new JLabel("Default new point color: "),cd);
		cd.gridx++;
		pDefaults.add(butPointDefColor,cd);
		cd.gridx=0;
		cd.gridy++;
		pDefaults.add(new JLabel("Default new line color: "),cd);
		cd.gridx++;
		pDefaults.add(butLineDefColor,cd);
		cd.gridx=0;
		cd.gridy++;
		pDefaults.add(new JLabel("Active point color: "),cd);
		cd.gridx++;
		pDefaults.add(butPointActiveColor,cd);
		cd.gridx=0;
		cd.gridy++;
		pDefaults.add(new JLabel("Active line color: "),cd);
		cd.gridx++;
		pDefaults.add(butLineActiveColor,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pDefaults.add(new JLabel("Default point size: "),cd);
		cd.gridx++;
		pDefaults.add(nfPointSize,cd);
		cd.gridx=0;
		cd.gridy++;
		pDefaults.add(new JLabel("Default line thickness: "),cd);
		cd.gridx++;
		pDefaults.add(nfLineThickness,cd);
		cd.gridx=0;
		cd.gridy++;
		pDefaults.add(new JLabel("Default line render: "),cd);
		cd.gridx++;
		pDefaults.add(renderTypeList,cd);
		
		////////////TRACING OPTIONS
		JPanel pTrace = new JPanel(new GridBagLayout());

		
		NumberField nfTraceBoxSize = new NumberField(4);
		NumberField nfSigma = new NumberField(4);
		//nTraceBoxSize.setText(Integer.toString((int)(2.0*Prefs.get("BigTrace.lTraceBoxSize", 50))));
		//nfTraceBoxSize.setText(Integer.toString((int)(2.0*Prefs.get("BigTrace.lTraceBoxSize", 50))));
		nfTraceBoxSize.setText(Integer.toString((int)(2.0*bt.btdata.lTraceBoxSize)));
		nfSigma.setText(Double.toString(bt.btdata.sigmaGlob));
		
		cd.gridx=0;
		cd.gridy=0;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Curve size/scale (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigma,cd);
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Trace box size (px): "),cd);
		cd.gridx++;
		pTrace.add(nfTraceBoxSize,cd);
		
		tabPane.addTab("Defaults",pDefaults);
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
				defaultPointColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				selectColors.setColor(null, 0);
			}
			tempC=selectColors.getColor(1);
			if(tempC!=null)
			{
				defaultLineColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				selectColors.setColor(null, 1);
			}
			tempC=selectColors.getColor(2);
			if(tempC!=null)
			{
				activePointColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				selectColors.setColor(null, 2);
			}
			tempC=selectColors.getColor(3);
			if(tempC!=null)
			{
				activeLineColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				selectColors.setColor(null, 3);
			}
			
			currPointSize = Float.parseFloat(nfPointSize.getText());
			currLineThickness = Float.parseFloat(nfLineThickness.getText());
			currRenderType = renderTypeList.getSelectedIndex();
			
			
			bt.btdata.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", (double)(bt.btdata.lTraceBoxSize));
			
			bt.btdata.sigmaGlob = Double.parseDouble(nfSigma.getText());
			Prefs.set("BigTrace.sigmaGlob", (double)(bt.btdata.sigmaGlob));
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
		

		JButton butPointColor = new JButton( new ColorIcon( rois.get(activeRoi).getPointColor() ) );
		
		butPointColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.finFrame, "Choose point color", rois.get(activeRoi).getPointColor() );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 0);
				//setNewPointColor(newColor);
				butPointColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineColor  = new JButton( new ColorIcon( rois.get(activeRoi).getLineColor()) );

		
		butLineColor.addActionListener( e -> {
				Color newColor = JColorChooser.showDialog(bt.finFrame, "Choose line color", rois.get(activeRoi).getPointColor() );
				if (newColor!=null)
				{	
					selectColors.setColor(newColor, 1);							
					butLineColor.setIcon(new ColorIcon(newColor));
				}
				
		});
		

		cd.gridx=0;
		cd.gridy=0;
		//cd.anchor=GridBagConstraints.WEST;
		dialProperties.add(new JLabel("Point size: "),cd);
		cd.gridx++;
		dialProperties.add(nfPointSize,cd);
		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Point color: "),cd);
		cd.gridx++;
		dialProperties.add(butPointColor,cd);
		
		if(rois.get(activeRoi).getType()>Roi3D.POINT)
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
			
			//point color
			if(selectColors.getColor(0)!=null)
			{
				rois.get(activeRoi).setPointColorRGB(selectColors.getColor(0));
				selectColors.setColor(null, 0);
				//newPointColor = null;
			}
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
				//line color
				if(selectColors.getColor(1)!=null)
				{				
					rois.get(activeRoi).setLineColorRGB(selectColors.getColor(1));
					selectColors.setColor(null, 1);
				
				}
				//render type
				if(renderTypeList.getSelectedIndex()!=rois.get(activeRoi).getRenderType())
				{
					rois.get(activeRoi).setRenderType(renderTypeList.getSelectedIndex());
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
	/** Save ROIS dialog and saving **/
	public void diagLoadROIs()
	{
		String filename;

        //Roi3D roiIn;
        
		OpenDialog openDial = new OpenDialog("Load BigTrace ROIs","", "*.csv");
		
        String path = openDial.getDirectory();
        if (path==null)
        	return;

        filename = path+openDial.getFileName();
        
        bt.bInputLock = true;
        //this.setLockMode(true);
        ROIsLoadBG loadTask = new ROIsLoadBG();
        loadTask.sFilename=filename;
        loadTask.bt=this.bt;
        loadTask.addPropertyChangeListener(bt.btpanel);
        loadTask.execute();
        //this.setLockMode(false);
        
	}

	
}

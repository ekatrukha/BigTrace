package bigtrace.rois;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
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

import bigtrace.gui.NumberField;
import bigtrace.gui.PanelTitle;
import net.imglib2.RealPoint;



public class RoiManager3D extends JPanel implements ListSelectionListener, ActionListener {
	
	

	 private static final long serialVersionUID = -2843907862066423151L;
	 public static final int ADD_POINT=0, ADD_POINT_LINE=1, ADD_POINT_SEMIAUTOLINE=2;
	 public ArrayList<Roi3D> rois =  new ArrayList<Roi3D >();
	 public int activeRoi = -1;
	 public Color activeLineColor = Color.RED;
	 public Color nonActiveLineColor = Color.BLUE;
	 public Color activePointColor = Color.YELLOW;
	 public Color nonActivePointColor = Color.GREEN;
	/* public Color activeLineColor = Color.WHITE;
	 public Color nonActiveLineColor = Color.WHITE;
	 public Color activePointColor = Color.WHITE;
	 public Color nonActivePointColor = Color.WHITE;*/
	 public int mode;
	 public float currLineThickness = 3.0f;
	 public float currPointSize = 5.0f;
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
	 
	 JToggleButton roiPointMode;
	 JToggleButton roiPolyLineMode;
	 JToggleButton roiPolySemiAMode;
	 
	 
	 private ArrayList<Listener> listeners =	new ArrayList<Listener>();

		
	 public RoiManager3D()
	 {
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
		 activeRoi = rois.size()-1;
		 if(roi_in.getType()==Roi3D.POINT)
			 roi_in.setName("point"+Integer.toString(roi_in.hashCode()));

		 if(roi_in.getType()==Roi3D.POLYLINE)
			 roi_in.setName("polyl"+Integer.toString(roi_in.hashCode()));
		 if(roi_in.getType()==Roi3D.LINE_TRACE)
			 roi_in.setName("trace"+Integer.toString(roi_in.hashCode()));

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
	       int i;
	       for (i=0;i<rois.size();i++) 
	       {
	    	   roi=rois.get(i);
	    	   if(i==activeRoi)
	    	   {
	    		   roi.setPointColor(activePointColor);
	    		   roi.setLineColor(activeLineColor);
	    	   }
	    	   else
	    	   {
	    		   
	    		   roi.setPointColor(nonActivePointColor);
	    		   roi.setLineColor(nonActiveLineColor);	    		   
	    	   }
	    	   if(bShowAll)
	    	   {
	    		   roi.draw(gl, pvm, screen_size);
	    	   }
	    	   else
	    		   if(i==activeRoi)
	    			   roi.draw(gl, pvm, screen_size);
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
		 LineTracing3D tracing;
		 //new Line
		 if(activeRoi<0 || rois.get(activeRoi).getType()!=Roi3D.LINE_TRACE)
		 {
			 tracing = new LineTracing3D(currLineThickness, currPointSize, activeLineColor, activePointColor);
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
			 tracing = (LineTracing3D) rois.get(activeRoi);
			 tracing.addPointAndSegment(point_,segments_);
		 }
	 }
	 public RealPoint getLastTracePoint()
	 { 
		 LineTracing3D tracing;
		 tracing = (LineTracing3D) rois.get(activeRoi);
		 return tracing.vertices.get(tracing.vertices.size()-1);
	 }

	 
	 public boolean removeSegment()
	 {
		 LineTracing3D tracing;
		 tracing = (LineTracing3D) rois.get(activeRoi);
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
			 polyline = new PolyLine3D(currLineThickness, currPointSize, activeLineColor, activePointColor);
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
	 public void setTraceMode(boolean bTraceMode)
	 {
		 	 boolean bState = !bTraceMode;
			 roiPointMode.setEnabled(bState);
			 roiPolyLineMode.setEnabled(bState);
			 butDelete.setEnabled(bState);
			 butRename.setEnabled(bState);
			 butDeselect.setEnabled(bState);
			 butProperties.setEnabled(bState);
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
		
		///SIDE ROI LIST BUTTONS
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
				JPanel dialProperties = new JPanel(new GridBagLayout());
				GridBagConstraints cd = new GridBagConstraints();
				NumberField nfPointSize = new NumberField(3);
				NumberField nfLineThickness = new NumberField(2);
				nfPointSize.setText(Integer.toString(Math.round(rois.get(activeRoi).getPointSize())));
				nfPointSize.setIntegersOnly(true);
				//for now, since using simple shader
				nfLineThickness.setLimits(0.0, 10.);
				nfLineThickness.setText(Integer.toString(Math.round(rois.get(activeRoi).getLineThickness())));
				
				
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
				int reply = JOptionPane.showConfirmDialog(null, dialProperties, "ROI Properties", 
				        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (reply == JOptionPane.OK_OPTION) 
				{
					rois.get(activeRoi).setPointSize(Integer.parseInt(nfPointSize.getText()));
					if(rois.get(activeRoi).getType()>Roi3D.POINT)
					{
						rois.get(activeRoi).setLineThickness(Integer.parseInt(nfLineThickness.getText()));
					}
					fireActiveRoiChanged(activeRoi); 
				}
			}
		}
		
	}
		
		
		
	
}

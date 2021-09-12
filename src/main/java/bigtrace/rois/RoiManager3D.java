package bigtrace.rois;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.metal.MetalToggleButtonUI;

import org.joml.Matrix4fc;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.jidesoft.swing.RangeSlider;
import com.jogamp.opengl.GL3;


import net.imglib2.RealPoint;



public class RoiManager3D extends JPanel implements ListSelectionListener {
	
	

	private static final long serialVersionUID = -2843907862066423151L;
	//private static final long serialVersionUID = 1L;
	 public static final int ADD_POINT=0, ADD_POINT_LINE=1;
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
	 public float currLineThickness = 15.0f;
	 public float currPointSize = 40.0f;
	 public boolean bShowAll = true;
	 
	 //GUI
	 public DefaultListModel<String> listModel; 
	 JList<String> jlist;
	 JScrollPane listScroller;
	 public static interface Listener {
		public void activeRoiChanged(int nRoi);				
	 }
	 
	 private ArrayList<Listener> listeners =	new ArrayList<Listener>();

		
	 public RoiManager3D()
	 {
			try {
			    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
			} catch( Exception ex ) {
			    System.err.println( "Failed to initialize LaF" );
			}
		 int nButtonSize = 28;
		 GridBagLayout gridbag = new GridBagLayout();
		 GridBagConstraints c = new GridBagConstraints();

		 setLayout(gridbag);
		 JPanel panTracing = new JPanel();
		 ButtonGroup roiTraceMode = new ButtonGroup();
		 
		// UIManager.put("ToggleButton.select", Color.WHITE);
		 ClassLoader classLoader = getClass().getClassLoader();
	     String icon_path = classLoader.getResource("icons/dot.png").getFile();
		 ImageIcon tabIcon = new ImageIcon(icon_path);
		 JToggleButton roiPointMode = new JToggleButton(tabIcon);
		 roiPointMode.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		  
		 roiPointMode.setSelected(true);
	
		 
	     icon_path = classLoader.getResource("icons/polyline3D.png").getFile();
		 tabIcon = new ImageIcon(icon_path);
		 JToggleButton roiPolyLineMode = new JToggleButton(tabIcon);
		 roiPolyLineMode.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		// roiPolyLineMode.setContentAreaFilled(false);
		 
		 roiTraceMode.add(roiPointMode);
		 roiTraceMode.add(roiPolyLineMode);
		 c.gridx=0;
		 c.gridy=0;
		 panTracing.add(roiPointMode,c);
		 panTracing.add(roiPolyLineMode,c);

		 add(panTracing,c);

		 JPanel roiList = new JPanel();
		 mode = ADD_POINT_LINE;
		 listModel = new  DefaultListModel<String>();
		 jlist = new JList<String>(listModel);
		 jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		 jlist.setLayoutOrientation(JList.VERTICAL);
		 jlist.setVisibleRowCount(-1);
		 jlist.addListSelectionListener(this);
		 listScroller = new JScrollPane(jlist);
		 listScroller.setPreferredSize(new Dimension(150, 250));
		 roiList.add(listScroller);
		 c.gridy++;
		 add(roiList,c);

	 }
	 
	 public void addRoi(Roi3D roi_in)
	 {
		 rois.add(roi_in);		 
		 activeRoi = rois.size()-1;
		 
		 roi_in.setName("polyl"+Integer.toString(roi_in.hashCode()));
		 listModel.addElement(roi_in.getName());
		 jlist.setSelectedIndex(activeRoi);
		

	 }
	 /** removes ROI and updates ListModel
	  * does not update activeRoi index! **/
	 public void removeRoi(int roiIndex)
	 {
		 if(roiIndex<rois.size())
		 {
			 rois.remove(roiIndex);
			 listModel.removeElementAt(roiIndex);
			 //activeRoi = -1;
		 }
	 }
	 
	 public void removeAll()
	 {
		 rois =  new ArrayList<Roi3D >();
	 }
	 
	 /** Draw all ROIS **/
	 public void draw(GL3 gl, Matrix4fc pvm, double[] screen_size, double dNear, double dFar)
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
	    		   roi.draw(gl, pvm, screen_size, dNear, dFar);
	    	   }
	    	   else
	    		   if(i==activeRoi)
	    			   roi.draw(gl, pvm, screen_size, dNear, dFar);
	       }
	 }
	 /** adds point to active polyline
	  *  if active ROI is not a polyline, does nothing
	  *  if there are no active ROIS, starts new polyline **/
	 public void addPointToLine(RealPoint point_)
	 {
		 PolyLine3D polyline;
		 //new Line
		 if(activeRoi<0)
		 {
			 polyline = new PolyLine3D(currLineThickness, currPointSize, activeLineColor, activePointColor);
			 polyline.addPointToEnd(point_);
			 addRoi(polyline);
			 //activeRoi = rois.size()-1; 
			 return;
		 }
		 //active ROI is not a line
		 if(rois.get(activeRoi).getType()!=Roi3D.POLYLINE)
		 {
			 return;
		 }
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
				 activeRoi--;
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
	 
	 public void unselect()
	 {
		 activeRoi=-1;
		 jlist.clearSelection();
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
		
		
		
	
}

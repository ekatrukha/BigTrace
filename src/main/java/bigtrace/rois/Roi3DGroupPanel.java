package bigtrace.rois;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bdv.tools.brightness.ColorIcon;
import bigtrace.gui.NumberField;
import ij.Prefs;
import ij.io.OpenDialog;
import ij.io.SaveDialog;



public class Roi3DGroupPanel implements ListSelectionListener, ActionListener {

	private JDialog dialog;
	private JOptionPane optionPane;
	
	public DefaultListModel<String> listModel; 
	JList<String> jlist;
	JScrollPane listScroller;
	
	JPanel presetList;
	
	JButton butEdit;
	JButton butCopyNew;
	JButton butDelete;
	JButton butRename;
	JButton butSave;
	JButton butLoad;
	
	
	RoiManager3D roiManager;
	
	public ColorUserSettings selectColors = new ColorUserSettings();
	
	public Roi3DGroupPanel(RoiManager3D roiManager_)	
	{
		 roiManager  = roiManager_;
		 listModel = new  DefaultListModel<String>();
		 jlist = new JList<String>(listModel);
		 jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		 jlist.setLayoutOrientation(JList.VERTICAL);
		 jlist.setVisibleRowCount(-1);
		 jlist.addListSelectionListener(this);
		 listScroller = new JScrollPane(jlist);
		 listScroller.setPreferredSize(new Dimension(170, 400));
		 listScroller.setMinimumSize(new Dimension(170, 250));
		 
		 
		 
		 for (int i = 0;i<roiManager.groups.size();i++)
		 {
			 listModel.addElement(roiManager.groups.get(i).getName());
		 }
		 jlist.setSelectedIndex(roiManager.nActiveGroup);
		 
		 presetList = new JPanel(new GridBagLayout());
		 //presetList.setBorder(new PanelTitle(" Groups Manager "));

		 butEdit = new JButton("Edit");
		 butEdit.addActionListener(this);
		 butCopyNew = new JButton("Copy/New");
		 butCopyNew.addActionListener(this);
		 butRename = new JButton("Rename");
		 butRename.addActionListener(this);		 
		 butDelete = new JButton("Delete");
		 butDelete.addActionListener(this);
		 butSave = new JButton("Save");
		 butSave.addActionListener(this);
		 butLoad = new JButton("Load");
		 butLoad.addActionListener(this);

		 
		 GridBagConstraints cr = new GridBagConstraints();
		 cr.gridx=0;
		 cr.gridy=0;
		 //cr.weighty=0.5;
		 cr.gridheight=GridBagConstraints.REMAINDER;
		 presetList.add(listScroller,cr);

		 cr.gridx++;
		 cr.gridy++;
		 cr.gridheight=1;
		 //cr.fill = GridBagConstraints.NONE;
		 presetList.add(butEdit,cr);		 
		 cr.gridy++;
		 presetList.add(butCopyNew,cr);		 
		 cr.gridy++;
		 presetList.add(butRename,cr);		 
		 cr.gridy++;
		 presetList.add(butDelete,cr);
		 cr.gridy++;
		 presetList.add(butSave,cr);
		 cr.gridy++;
		 presetList.add(butLoad,cr);
		 
		 
	     // Blank/filler component
		 cr.gridx++;
		 cr.gridy++;
		 cr.weightx = 0.01;
	     cr.weighty = 0.01;
	     presetList.add(new JLabel(), cr);		
		 
		 optionPane = new JOptionPane(presetList);
	     dialog = optionPane.createDialog("ROI Groups manager");
	     dialog.setModal(true);
		 
    }
	
	public void show()
	{ 
		dialog.setVisible(true); 
	}

    private void hide()
    { 
    	dialog.setVisible(false); 
    }
    
	/** show Group Properties dialog**/
	public boolean dialProperties(final Roi3DGroup preset)	
	{
		JPanel dialProperties = new JPanel(new GridBagLayout());
		GridBagConstraints cd = new GridBagConstraints();
		JTextField tfName = new JTextField(10); 
		NumberField nfPointSize = new NumberField(4);
		NumberField nfLineThickness = new NumberField(4);
		NumberField nfOpacity = new NumberField(4);

		String[] sRenderType = { "Center line", "Wire", "Surface" };
		JComboBox<String> renderTypeList = new JComboBox<String>(sRenderType);
		
		
		tfName.setText(preset.getName());
		nfPointSize.setText(Float.toString(preset.getPointSize()));
		nfLineThickness.setText(Float.toString(preset.getLineThickness()));
		DecimalFormat df = new DecimalFormat("0.00");
		nfOpacity.setText(df.format(preset.getOpacity()));
		nfOpacity.setLimits(0.0, 1.0);
		

		JButton butPointColor = new JButton( new ColorIcon( preset.getPointColor() ) );
		
		butPointColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(dialog, "Choose point color", preset.getPointColor() );
			if (newColor!=null)
			{
				selectColors.setColor(newColor, 0);
				butPointColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineColor  = new JButton( new ColorIcon( preset.getLineColor()) );

		
		butLineColor.addActionListener( e -> {
				Color newColor = JColorChooser.showDialog(dialog, "Choose line color", preset.getPointColor() );
				if (newColor!=null)
				{	
					selectColors.setColor(newColor, 1);							
					butLineColor.setIcon(new ColorIcon(newColor));
				}
				
		});
		

		cd.gridx=0;
		cd.gridy=0;

		dialProperties.add(new JLabel("Name: "),cd);
		cd.gridx++;
		dialProperties.add(tfName,cd);
		
		
		
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

		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Opacity: "),cd);
		cd.gridx++;
		dialProperties.add(nfOpacity,cd);
		
		cd.gridx=0;
		cd.gridy++;
		dialProperties.add(new JLabel("Render as: "),cd);
		renderTypeList.setSelectedIndex(preset.getRenderType());
		cd.gridx++;
		dialProperties.add(renderTypeList,cd);

		
		
		int reply = JOptionPane.showConfirmDialog(null, dialProperties, "ROI Properties", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		
		
		if (reply == JOptionPane.OK_OPTION) 
		{
			
			//name 
			
			String newName = tfName.getText();
			if ((newName != null) && (newName.length() > 0)) 
			{
				preset.setName(newName);
			}
			//point size 
			preset.setPointSize(Float.parseFloat(nfPointSize.getText()));
			
			//point color
			if(selectColors.getColor(0)!=null)
			{
				preset.setPointColorRGB(selectColors.getColor(0));
				selectColors.setColor(null, 0);
				//newPointColor = null;
			}
			//opacity
			float fNewOpacity= Float.parseFloat(nfOpacity.getText());
			if(fNewOpacity<0.0f)
				{fNewOpacity=0.0f;}
			if(fNewOpacity>1.0f)
				{fNewOpacity=1.0f;}
			preset.setOpacity(fNewOpacity);

			//line

			//line thickness
			float fNewLineThickess = Float.parseFloat(nfLineThickness.getText());
			if(Math.abs(fNewLineThickess-preset.getLineThickness())>0.00001)
			{
				preset.setLineThickness(fNewLineThickess );
			}
			//line color
			if(selectColors.getColor(1)!=null)
			{				
				preset.setLineColorRGB(selectColors.getColor(1));
				selectColors.setColor(null, 1);
			
			}
			//render type
			if(renderTypeList.getSelectedIndex()!=preset.getRenderType())
			{
				preset.setRenderType(renderTypeList.getSelectedIndex());
			}
			return true;
		}
		
		return false;			
	}
	
	/** Save Groups dialog and saving **/
	public void diagSaveGroups()
	{
		String filename;
		int nGroupN, nGroup;
		
		filename = roiManager.bt.btdata.sFileNameImg + "_btgroups";
		SaveDialog sd = new SaveDialog("Save ROIs ", filename, ".csv");
        String path = sd.getDirectory();
        if (path==null)
        	return;
        filename = path+sd.getFileName();
        
        try {
			final File file = new File(filename);
			
			final FileWriter writer = new FileWriter(file);
			writer.write("BigTrace_groups,version," + roiManager.bt.btdata.sVersion + "\n");
			nGroupN=roiManager.groups.size();
			writer.write("GroupsNumber,"+Integer.toString(nGroupN)+"\n");
			for(nGroup=0;nGroup<nGroupN;nGroup++)
			{
				  //Sleep for up to one second.
				try {
					Thread.sleep(1);
				} catch (InterruptedException ignore) {}
				writer.write("BT_Group,"+Integer.toString(nGroup+1)+"\n");
				
				roiManager.groups.get(nGroup).saveGroup(writer);
			}
			writer.write("End of BigTrace Groups\n");
			writer.close();

		} catch (IOException e) {	
			System.err.print(e.getMessage());
			//e.printStackTrace();
		}
		return;

	}
	
	/** Load Groups dialog and saving **/
	public void diagLoadGroups()
	{
		String filename;
		String[] line_array;
        int bFirstPartCheck = 0;
        int nGroupN, nGroup;
		OpenDialog openDial = new OpenDialog("Load BigTrace Groups","", "*.csv");
		///
        float pointSize=0.0f;
        float lineThickness =0.0f;
        Color pointColor = Color.BLACK;
        Color lineColor = Color.BLACK;
        String sName = "";
        int nRenderType = 0;
        int nSectorN = 16;
        
        
		Roi3DGroup readGroup;
		
        String path = openDial.getDirectory();
        if (path==null)
        	return;
        
        
        String [] sGroupLoadOptions = new String [] {"Overwite current groups","Append to list"};
		
        String input = (String) JOptionPane.showInputDialog(optionPane, "Loading ROI Groups",
                "Loaded groups:", JOptionPane.QUESTION_MESSAGE, null, // Use
                                                                                // default
                                                                                // icon
                sGroupLoadOptions, // Array of choices
                sGroupLoadOptions[(int)Prefs.get("BigTrace.LoadGroup", 0)]);
        
        if(input.isEmpty())
        	 return;
        int nLoadMode;
        if(input.equals("Overwrite current groups"))
        {
        	nLoadMode = 0;
        	roiManager.groups = new ArrayList<Roi3DGroup>();
        	listModel.removeAllElements();
        }
        else
        {
        	nLoadMode = 1;
        }
        
       // GenericDialog dgGroupLoadMode = new GenericDialog("Loading ROI Groups");
		//dgGroupLoadMode.addChoice("Loaded groups: ",sGroupLoadOptions, Prefs.get("BigTrace.LoadGroup", "Overwite current"));
		//dgGroupLoadMode.showDialog();
        
		//if (dgGroupLoadMode.wasCanceled())
        //    return;
		//int nLoadMode = dgGroupLoadMode.getNextChoiceIndex();
		//Prefs.set("BigTrace.LoadGroup", sGroupLoadOptions[nLoadMode]);
        Prefs.set("BigTrace.LoadGroup", nLoadMode);

		
		
		
        filename = path+openDial.getFileName();
        
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
					  if(line_array[0].equals("BigTrace_groups")&& line_array[2].equals(roiManager.bt.btdata.sVersion))
					  {
						  bFirstPartCheck++; 
					  }					  
			      }
				  //second line check
				  if(line_array.length==2 && nLineN==2)
			      {
					  bFirstPartCheck++;
					  if(line_array[0].equals("GroupsNumber"))
					  {
						  bFirstPartCheck++;
						  nGroupN=Integer.parseInt(line_array[1]);
					  }
			      }				  
				  if(line_array[0].equals("BT_Group"))
				  {

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
					  //read it all hopefully
					  readGroup = new Roi3DGroup(sName, pointSize, pointColor, lineThickness, lineColor,  nRenderType, nSectorN);
					  roiManager.groups.add(readGroup);
					  listModel.addElement(readGroup.getName());
				  }
					  				  
				}

	        br.close();
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
        	 System.err.println("Not a Bigtrace ROI file format or plugin/version mismatch, loading ROIs aborted.");
             //bt.bInputLock = false;
             //bt.roiManager.setLockMode(false);
        }

		return;

	}


	@Override
	public void valueChanged(ListSelectionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		// TODO Auto-generated method stub

		int indList = jlist.getSelectedIndex();
		
		if(indList>-1)
		{
			//EDIT
			if(ae.getSource() == butEdit)
			{
				dialProperties(roiManager.groups.get(indList));
				listModel.set(indList,roiManager.groups.get(indList).getName());
				
			}
			//COPY/NEW
			if(ae.getSource() == butCopyNew)
			{
				Roi3DGroup newGroup = new Roi3DGroup(roiManager.groups.get(indList), roiManager.groups.get(indList).getName()+"_copy"); 
				if(dialProperties(newGroup))
				{
					roiManager.groups.add(newGroup);
					listModel.addElement(newGroup.getName());					
				}
				
			}			
			//RENAME
			if(ae.getSource() == butRename)
			{
		
				String s = (String)JOptionPane.showInputDialog(
						presetList,
						"New name:",
						"Rename Group",
						JOptionPane.PLAIN_MESSAGE,
						null,
						null,
						roiManager.groups.get(indList).getName());
	
				//If a string was returned, rename
				if ((s != null) && (s.length() > 0)) 
				{
					roiManager.groups.get(indList).setName(s);
					listModel.set(indList,s);
					return;
				}
	
			}
			//DELETE
			if(ae.getSource() == butDelete)
			{
				if(jlist.getModel().getSize()>1)
				{
					
					 roiManager.groups.remove(indList);
					 listModel.removeElementAt(indList);
				}
				if(indList==0)
				{
					jlist.setSelectedIndex(0);
				}
				else
				{
					jlist.setSelectedIndex(indList-1);
				}
			}
			//SAVE
			if(ae.getSource() == butSave)
			{
			
				diagSaveGroups();
			}
			//LOAD
			if(ae.getSource() == butLoad)
			{
			
				diagLoadGroups();
			}
			
		}
	}
	
}
package bigtrace.rois;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

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


public class Roi3DPresetPanel implements ListSelectionListener, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4884102591722778043L;
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
	
	RoiManager3D roiManager;
	
	public ColorUserSettings selectColors = new ColorUserSettings();
	
	public Roi3DPresetPanel(RoiManager3D roiManager_)	
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
		 
		 
		 
		 for (int i = 0;i<roiManager.presets.size();i++)
		 {
			 listModel.addElement(roiManager.presets.get(i).getName());
		 }
		 jlist.setSelectedIndex(roiManager.nActivePreset);
		 
		 presetList = new JPanel(new GridBagLayout());
		 //presetList.setBorder(new PanelTitle(" Presets Manager "));

		 butEdit = new JButton("Edit");
		 butEdit.addActionListener(this);
		 butCopyNew = new JButton("Copy/New");
		 butCopyNew.addActionListener(this);
		 butRename = new JButton("Rename");
		 butRename.addActionListener(this);		 
		 butDelete = new JButton("Delete");
		 butDelete.addActionListener(this);


		 
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
		 
		 
	     // Blank/filler component
		 cr.gridx++;
		 cr.gridy++;
		 cr.weightx = 0.01;
	     cr.weighty = 0.01;
	     presetList.add(new JLabel(), cr);		
		 
		 optionPane = new JOptionPane(presetList);
	     dialog = optionPane.createDialog("Presets manager");
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
    
	/** show Preset Properties dialog**/
	public boolean dialProperties(final Roi3DPreset preset)	
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
				dialProperties(roiManager.presets.get(indList));
				listModel.set(indList,roiManager.presets.get(indList).getName());
				
			}
			//COPY/NEW
			if(ae.getSource() == butCopyNew)
			{
				Roi3DPreset newPreset = new Roi3DPreset(roiManager.presets.get(indList), roiManager.presets.get(indList).getName()+"_copy"); 
				if(dialProperties(newPreset))
				{
					roiManager.presets.add(newPreset);
					listModel.addElement(newPreset.getName());					
				}
				
			}			
			//RENAME
			if(ae.getSource() == butRename)
			{
		
				String s = (String)JOptionPane.showInputDialog(
						presetList,
						"New name:",
						"Rename Preset",
						JOptionPane.PLAIN_MESSAGE,
						null,
						null,
						roiManager.presets.get(indList).getName());
	
				//If a string was returned, rename
				if ((s != null) && (s.length() > 0)) 
				{
					roiManager.presets.get(indList).setName(s);
					listModel.set(indList,s);
					return;
				}
	
			}
			//DELETE
			if(ae.getSource() == butDelete)
			{
				if(jlist.getModel().getSize()>1)
				{
					
					 roiManager.presets.remove(indList);
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
		}
	}
	
}

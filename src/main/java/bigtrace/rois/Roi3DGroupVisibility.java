package bigtrace.rois;


import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;


import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;



public class Roi3DGroupVisibility < T extends RealType< T > & NativeType< T > > implements  ActionListener {

	private JDialog dialog;
	private JOptionPane optionPane;
	
	ArrayList<JLabel> groupNames;
	ArrayList<JCheckBox> groupCheckbox;
	
	RoiManager3D<T> roiManager;
	
	
	public Roi3DGroupVisibility(RoiManager3D<T> roiManager_)	
	{
		 roiManager  = roiManager_;
		 
		 groupNames = new ArrayList<JLabel>();
		 groupCheckbox = new ArrayList<JCheckBox>();
		 JPanel panelGroupVis = new JPanel(new GridLayout(0,2));


		 for(int i = 0;i<roiManager.groups.size();i++)
		 {
			 //cd.gridx=0;
			 groupNames.add(new JLabel(roiManager.groups.get(i).getName()));
			 groupCheckbox.add(new JCheckBox());
			 
			 groupCheckbox.get(i).setSelected(roiManager.groups.get(i).bVisible);
			 groupCheckbox.get(i).addActionListener(this);
			 panelGroupVis.add(groupNames.get(i));
			 panelGroupVis.add(groupCheckbox.get(i));

		 }

		 
		 optionPane = new JOptionPane(panelGroupVis);
	     dialog = optionPane.createDialog("ROI Group Visibility");
	     dialog.setModal(true);
		 
    }
	
	public void show()
	{ 
		dialog.setVisible(true); 
	}


	@Override
	public void actionPerformed(ActionEvent ae) {
		//switch off visibility
		for(int i=0;i< groupCheckbox.size();i++)
		{
			if(ae.getSource() == groupCheckbox.get(i))
			{
	
				roiManager.groups.get(i).bVisible =groupCheckbox.get(i).isSelected(); 
				roiManager.repaintBVV();
			}

		}
	}
	

	
}

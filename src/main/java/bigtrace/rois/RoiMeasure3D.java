package bigtrace.rois;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bigtrace.BigTrace;
import bigtrace.gui.PanelTitle;

public class RoiMeasure3D extends JPanel implements ListSelectionListener, ActionListener{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4635723145578489755L;
	BigTrace bt;
	
	JButton butLineProfile;
	JButton butSettings;
	JButton butMeasure;
	JButton butMeasureAll;
	JList<String> jlist ;
	JScrollPane listScroller;
	
	JComboBox<String> cbActiveChannel;
	

	public RoiMeasure3D(BigTrace bt)
	{
		this.bt = bt;


		int nButtonSize = 40;

		JPanel panLineTools = new JPanel(new GridBagLayout());  
		panLineTools.setBorder(new PanelTitle(" Line tools "));

		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/line_profile.png");
		ImageIcon tabIcon = new ImageIcon(icon_path);
		butLineProfile = new JButton(tabIcon);
		butLineProfile.setToolTipText("Plot Profile");
		butLineProfile.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));

		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		tabIcon = new ImageIcon(icon_path);
		butSettings = new JButton(tabIcon);
		butSettings.setToolTipText("Settings");
		butSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));

		butLineProfile.addActionListener(this);
		butSettings.addActionListener(this);

		//this.setBorder(new PanelTitle(" Measurements "));

		//add to the panel
		GridBagConstraints cr = new GridBagConstraints();
		cr.gridx=0;
		cr.gridy=0;
		panLineTools.add(butLineProfile,cr);
		cr.gridx++;
		JSeparator sp = new JSeparator(SwingConstants.VERTICAL);
		sp.setPreferredSize(new Dimension((int) (nButtonSize*0.5),nButtonSize));
		panLineTools.add(sp,cr);
		cr.gridx++;
		//panTracing.add(roiSettings,ct);
		//filler
		//ct.gridx++;
		cr.weightx = 0.01;
		panLineTools.add(new JLabel(), cr);
		cr.gridx++;
		panLineTools.add(butSettings,cr);

		
		JPanel panMeasure = new JPanel(new GridBagLayout());
		panMeasure.setBorder(new PanelTitle(" Measure "));
		cr = new GridBagConstraints();
		
		//synchronized ROI list
		jlist = new JList<String>(bt.roiManager.listModel);
		listScroller = new JScrollPane(jlist);
		jlist.addListSelectionListener(this);
		listScroller.setPreferredSize(new Dimension(170, 400));
		listScroller.setMinimumSize(new Dimension(170, 250));

		this.add(listScroller);	
		cr.gridx=0;
		cr.gridy=0;
		cr.gridheight=GridBagConstraints.REMAINDER;
		panMeasure.add(listScroller,cr);

		butMeasure = new JButton("Measure");
		butMeasure.addActionListener(this);
		cr.gridx++;
		cr.gridy++;
		cr.gridheight=1;

		panMeasure.add(butMeasure,cr);

		butMeasureAll = new JButton("Measure All");
		butMeasureAll.addActionListener(this);
		cr.gridy++;
		panMeasure.add(butMeasureAll,cr);

		// Blank/filler component
		cr.gridx++;
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		panMeasure.add(new JLabel(), cr);		

		// a solution for now
		butMeasure.setMinimumSize(butMeasureAll.getPreferredSize());
		butMeasure.setPreferredSize(butMeasureAll.getPreferredSize()); 
		
		
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
		panChannel.add(new JLabel("Measure"),cr);
		cr.gridx++;
		panChannel.add(cbActiveChannel,cr);
		
		
		//put all panels together
		cr = new GridBagConstraints();
		setLayout(new GridBagLayout());
		cr.insets=new Insets(4,4,2,2);
		cr.gridx=0;
		cr.gridy=0;
		cr.fill = GridBagConstraints.HORIZONTAL;


		
		//Line Tools
		add(panLineTools,cr);
		//roi list
		cr.gridy++;
		add(panMeasure,cr);
		cr.gridy++;
		add(panChannel,cr);
		// Blank/filler component
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		add(new JLabel(), cr);    
	}

	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		
		// TODO Auto-generated method stub
		if (e.getValueIsAdjusting() == false) 
		{
			bt.roiManager.jlist.setSelectedIndex(jlist.getSelectedIndex());
        }
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
		// MEASURE CHANNEL
		if(e.getSource() == cbActiveChannel)
		{
			//bt.btdata.nChAnalysis=cbActiveChannel.getSelectedIndex();
		}
	}

}

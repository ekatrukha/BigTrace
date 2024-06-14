package bigtrace.tracks;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.gui.PanelTitle;


public class BigTraceTracksPanel < T extends RealType< T > & NativeType< T > > extends JPanel implements ListSelectionListener, ActionListener
{
	final BigTrace<T> bt;

	JButton butTrack;
	JButton butSettings;
	JButton butColor;
	public JList<String> jlist;
	JScrollPane listScroller;
	
	public BigTraceTracksPanel(BigTrace<T> bt)
	{
		this.bt = bt;
		
		JPanel panTrackTools = new JPanel(new GridBagLayout());  
		panTrackTools.setBorder(new PanelTitle(" Tracking "));
		int nButtonSize = 40;
		
		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/train.png");
		ImageIcon tabIcon = new ImageIcon(icon_path);
		butTrack = new JButton(tabIcon);
		butTrack.setToolTipText("Track");
		butTrack.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		tabIcon = new ImageIcon(icon_path);
		butSettings = new JButton(tabIcon);
		butSettings.setToolTipText("Settings");
		butSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		
		butTrack.addActionListener(this);
		butSettings.addActionListener(this);
		
		GridBagConstraints cr = new GridBagConstraints();
		cr.gridx=0;
		cr.gridy=0;
		panTrackTools.add(butTrack,cr);
		
		cr.gridx++;
		JSeparator sp = new JSeparator(SwingConstants.VERTICAL);
		sp.setPreferredSize(new Dimension((int) (nButtonSize*0.5),nButtonSize));
		panTrackTools.add(sp,cr);
		//filler
		cr.gridx++;
		cr.weightx = 0.01;
		panTrackTools.add(new JLabel(), cr);
		cr.gridx++;
		panTrackTools.add(butSettings,cr);
		
		JPanel panTracksChange = new JPanel(new GridBagLayout());
		panTracksChange.setBorder(new PanelTitle(" Tracks "));
		cr = new GridBagConstraints();
		
		//synchronized ROI list
		jlist = new JList<>(bt.roiManager.listModel);
		listScroller = new JScrollPane(jlist);
		jlist.addListSelectionListener(this);
		listScroller.setPreferredSize(new Dimension(300, 400));
		listScroller.setMinimumSize(new Dimension(170, 250));
		
		jlist.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					// Double-click detected
					int index = jlist.locationToIndex(evt.getPoint());
					bt.roiManager.focusOnRoi(bt.roiManager.rois.get(index));
				} 
			}
		});

		cr.gridx=0;
		cr.gridy=0;
		cr.gridheight=GridBagConstraints.REMAINDER;

		cr.fill  = GridBagConstraints.BOTH;
		cr.weightx=0.99;
		cr.weighty=0.99;
		panTracksChange.add(listScroller,cr);
		cr.weightx=0.0;
		cr.weighty=0.0;
		cr.fill = GridBagConstraints.NONE;

		butColor = new JButton("Color");
		butColor.addActionListener(this);
		cr.gridx++;
		cr.gridy++;
		cr.gridheight=1;
		panTracksChange.add(butColor,cr);


		// Blank/filler component
		cr.gridx++;
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		panTracksChange.add(new JLabel(), cr);		

		// a solution for now
		panTracksChange.setMinimumSize(butColor.getPreferredSize());
		panTracksChange.setPreferredSize(butColor.getPreferredSize()); 


		//put all panels together
		cr = new GridBagConstraints();
		setLayout(new GridBagLayout());
		cr.insets=new Insets(4,4,2,2);
		cr.gridx=0;
		cr.gridy=0;
		cr.fill = GridBagConstraints.HORIZONTAL;

		//Line Tools
		add(panTrackTools,cr);
		//roi list
		cr.gridy++;
		cr.weighty = 0.99;
		cr.fill = GridBagConstraints.BOTH;
		add(panTracksChange,cr);

		// Blank/filler component
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		add(new JLabel(), cr);    
	}
	@Override
	public void actionPerformed( ActionEvent e )
	{
		
	}
	@Override
	public void valueChanged( ListSelectionEvent e )
	{
		if (e.getValueIsAdjusting() == false) 
		{
			bt.roiManager.jlist.setSelectedIndex(jlist.getSelectedIndex());
        }
		
	}

}

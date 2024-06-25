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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.gui.GBCHelper;
import bigtrace.gui.NumberField;
import bigtrace.gui.PanelTitle;
import bigtrace.gui.RangeSliderTF;
import bigtrace.rois.Roi3D;
import ij.Prefs;



public class BigTraceTracksPanel < T extends RealType< T > & NativeType< T > > extends JPanel implements ListSelectionListener, ActionListener
{
	final BigTrace<T> bt;

	JButton butTrack;
	JButton butSettings;
	JButton butDelete;
	JButton butRename;
	JButton butGroups;
	public JList<String> jlist;
	JScrollPane listScroller;
	CurveTracker<T> btTracker;
	
	ImageIcon tabIconTrain;
	ImageIcon tabIconCancel;
	
	public BigTraceTracksPanel(BigTrace<T> bt)
	{
		this.bt = bt;
		this.btTracker = null;// = new CurveTracker< >(bt);
		JPanel panTrackTools = new JPanel(new GridBagLayout());  
		panTrackTools.setBorder(new PanelTitle(" Tracking "));
		int nButtonSize = 40;
		
		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/train.png");
		tabIconTrain = new ImageIcon(icon_path);
		icon_path = bigtrace.BigTrace.class.getResource("/icons/cancel.png");
		tabIconCancel = new ImageIcon(icon_path);
		butTrack = new JButton(tabIconTrain);
		butTrack.setToolTipText("Track");
		butTrack.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		ImageIcon tabIcon = new ImageIcon(icon_path);
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
				if (evt.getClickCount() == 2) 
				{
					// Double-click detected
					int index = jlist.locationToIndex(evt.getPoint());
					bt.roiManager.focusOnRoi(bt.roiManager.rois.get(index));
				}
		        //right click
		    	if (SwingUtilities.isRightMouseButton(evt))
				{
					if(bt.roiManager.activeRoi.intValue()>=0)
					{
						bt.roiManager.dialProperties();
					}
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

		butDelete = new JButton("Delete");
		butDelete.addActionListener(this);
		cr.gridx++;
		cr.gridy++;
		cr.gridheight=1;
		panTracksChange.add(butDelete,cr);
		
		butRename = new JButton("Rename");
		butRename.addActionListener(this);
		cr.gridy++;
		panTracksChange.add(butRename,cr);
		
		butGroups = new JButton("Groups");
		butGroups.addActionListener(this);
		cr.gridy++;
		panTracksChange.add(butGroups,cr);

		// a solution for now
		butDelete.setMinimumSize(butRename.getPreferredSize());
		butDelete.setPreferredSize(butRename.getPreferredSize());
		butRename.setMinimumSize(butRename.getPreferredSize());		 
		butRename.setPreferredSize(butRename.getPreferredSize());
		butGroups.setMinimumSize(butRename.getPreferredSize());		 
		butGroups.setPreferredSize(butRename.getPreferredSize());


		// Blank/filler component
		cr.gridx++;
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		panTracksChange.add(new JLabel(), cr);		

		// a solution for now
		panTracksChange.setMinimumSize(butGroups.getPreferredSize());
		panTracksChange.setPreferredSize(butGroups.getPreferredSize()); 


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
	
	void simpleTracking()
	{
		JPanel dialogTrackSettings = new JPanel();
		
		NumberField nfBoxExpand = new NumberField(4);
		
		dialogTrackSettings.setLayout(new GridBagLayout());
		
		GridBagConstraints cd = new GridBagConstraints();

		GBCHelper.alighLeft(cd);
		
		cd.gridx=0;
		cd.gridy=0;	
		String[] sTrackDirection = { "all timepoints", "forward in time", "backward in time", "range below" };
		JComboBox<String> trackDirectionList = new JComboBox<>(sTrackDirection);
		int [] nRange = new int [2];
		nRange[0] = 0;
		nRange[1] = BigTraceData.nNumTimepoints-1;
		RangeSliderTF timeRange = new RangeSliderTF(nRange, nRange);
		timeRange.makeConstrained( bt.btData.nCurrTimepoint, bt.btData.nCurrTimepoint );
	
		dialogTrackSettings.add(new JLabel("Tracking:"),cd);
		trackDirectionList.setSelectedIndex((int)Prefs.get("BigTrace.trackDirection", 0));
		cd.gridx++;
		dialogTrackSettings.add(trackDirectionList,cd);
		cd.gridy++;
		cd.gridx=0;
		//cd.gridx++;

		cd.gridwidth=2;
		dialogTrackSettings.add(timeRange,cd);
		cd.gridwidth=1;
		cd.gridy++;
		
		cd.gridx=0;	
		dialogTrackSettings.add(new JLabel("Expand ROI box search by (px):"),cd);
		cd.gridx++;
		nfBoxExpand.setIntegersOnly( true );
		nfBoxExpand.setText(Integer.toString((int)(Prefs.get("BigTrace.nTrackExpandBox", 0))));
		dialogTrackSettings.add(nfBoxExpand,cd);
	
		int reply = JOptionPane.showConfirmDialog(null, dialogTrackSettings, "Track settings", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			int nTrackDirection = trackDirectionList.getSelectedIndex();
			Prefs.set("BigTrace.trackDirection", nTrackDirection);
			
			
			this.btTracker = new CurveTracker< >(bt);
			
			//tracking range
			switch(nTrackDirection)
			{
				//all timepoints
				case 0:
					btTracker.nFirstTP = 0;
					btTracker.nLastTP = BigTraceData.nNumTimepoints-1;
					break;
				//forward 
				case 1:
					btTracker.nFirstTP = bt.btData.nCurrTimepoint;
					btTracker.nLastTP = BigTraceData.nNumTimepoints-1;
					break;
				//backwards
				case 2:
					btTracker.nFirstTP = 0;
					btTracker.nLastTP = bt.btData.nCurrTimepoint;
					break;
				//range
				case 3:
					btTracker.nFirstTP = timeRange.getMin();
					btTracker.nLastTP = timeRange.getMax();
					break;

					
			}
			// box expand
			int nBoxExpand = Integer.parseInt(nfBoxExpand.getText());
			
			bt.bInputLock = true;
			bt.roiManager.setLockMode(true);
			butTrack.setEnabled( true );
			Prefs.set("BigTrace.nTrackExpandBox", nBoxExpand);	
			btTracker.nBoxExpand = nBoxExpand;
			butTrack.setIcon( tabIconCancel );
			butTrack.setToolTipText( "Stop tracking" );
			btTracker.addPropertyChangeListener( bt.btPanel );
			btTracker.butTrack = butTrack;
			btTracker.tabIconTrain = tabIconTrain; 
			btTracker.execute();
		}

				
	}
	@Override
	public void actionPerformed( ActionEvent e )
	{
		// RUN TRACKING
		if(e.getSource() == butTrack && jlist.getSelectedIndex()>-1)
		{
			
			if(!bt.bInputLock && BigTraceData.nNumTimepoints > 1 && bt.roiManager.getActiveRoi().getType()==Roi3D.LINE_TRACE)
			{
				simpleTracking();
			}
			else
			{
				if(bt.bInputLock && butTrack.isEnabled() && btTracker!=null && !btTracker.isCancelled() && !btTracker.isDone())
				{
					btTracker.cancel( false );
				}
			}
		}
		
		//Groups Manager
		if(e.getSource() == butGroups)
		{
			bt.roiManager.showGroupsDialog();
			
		}
		
		//DELETE
		if(e.getSource() == butDelete)
		{
			bt.roiManager.deleteActiveROI();
		}
		//RENAME
		if(e.getSource() == butRename)
		{
			bt.roiManager.renameActiveROIDialog();
		}
		
		//Settings
		if(e.getSource() == butSettings)
		{
			bt.roiManager.rmDiag.dialOneClickProperties();
		}
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

package bigtrace.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.math.FullAutoTrace;
import ij.Prefs;

public class PanelFullAutoTrace < T extends RealType< T > & NativeType< T > > implements ActionListener
{
	final BigTrace<T> bt;
	
	ImageIcon tabIconAuto;
	ImageIcon tabIconCancel;
	FullAutoTrace<T> fullAutoTrace;
	JButton butAuto;

	
	public PanelFullAutoTrace (final BigTrace<T> bt_)
	{
		bt = bt_;
	}
	
	public void initButton(final JButton butAutoTrace)
	{
		butAuto = butAutoTrace;
		butAuto.addActionListener( this );		
	}
	
	public  void launchFullAutoTrace()
	{
		final JTabbedPane tabPane = new JTabbedPane();

		final JPanel dialogFullAutoSettings = new JPanel();
		
		dialogFullAutoSettings.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();

		GBCHelper.alighLeft(gbc);
		
		NumberField nfMaxIntFullTraceStart = new NumberField(5);
		NumberField nfAutoMinCurvePoints = new NumberField(5);
		
		nfMaxIntFullTraceStart.setIntegersOnly( true );
		nfMaxIntFullTraceStart.setText( Integer.toString((int)Prefs.get("BigTrace.dAutoMinStartTraceInt",128.0)) );
		nfAutoMinCurvePoints.setIntegersOnly( true );
		nfAutoMinCurvePoints.setText( Integer.toString((int)Prefs.get("BigTrace.nAutoMinPointsCurve",3)) );
	
		gbc.gridx = 0;
		gbc.gridy = 0;
		dialogFullAutoSettings.add(new JLabel("Min intensity trace start:"),gbc);
		gbc.gridx++;
		
		dialogFullAutoSettings.add(nfMaxIntFullTraceStart,gbc);
		
		gbc.gridx = 0;
		gbc.gridy ++;
		dialogFullAutoSettings.add(new JLabel("Min # points in curve:"),gbc);
		gbc.gridx++;
		dialogFullAutoSettings.add(nfAutoMinCurvePoints,gbc);
		
		
		RangeSliderPanel timeRange = null;
		if(BigTraceData.nNumTimepoints>1)
		{
			
			final int [] nRange = new int [2];
			nRange[0] = 0;
			nRange[1] = BigTraceData.nNumTimepoints-1;
			timeRange = new RangeSliderPanel(nRange, nRange);
			//timeRange.makeConstrained( bt.btData.nCurrTimepoint, bt.btData.nCurrTimepoint );
			
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			dialogFullAutoSettings.add(timeRange,gbc);
			gbc.gridwidth = 1;
			gbc.gridy++;
		}
		
		////////////TRACING OPTIONS		
		final PanelTracingOptions panelGeneralTrace = new PanelTracingOptions(bt);
			
		////////////ONE-CLICK TRACING OPTIONS		
		final PanelOneClickTraceOptions panelOneClickOptions = new PanelOneClickTraceOptions(bt);

		//assemble pane
		tabPane.addTab("AutoTrace", dialogFullAutoSettings);
		tabPane.addTab("Tracing", panelGeneralTrace);
		tabPane.addTab("One click trace", panelOneClickOptions);
		
		int reply = JOptionPane.showConfirmDialog(null, tabPane, "Full auto tracing", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			panelGeneralTrace.getSetOptions();		
			panelOneClickOptions.getSetOptions();
			
			URL icon_path = this.getClass().getResource("/icons/autotrace.png");
			tabIconAuto = new ImageIcon(icon_path);
			icon_path = this.getClass().getResource("/icons/cancel.png");
			tabIconCancel = new ImageIcon(icon_path);

			fullAutoTrace = new FullAutoTrace<>(bt);
			
			if(timeRange != null)
			{
				fullAutoTrace.nFirstTP = timeRange.getMin();
				fullAutoTrace.nLastTP = timeRange.getMax();
			}
			fullAutoTrace.dAutoMinStartTraceInt = Integer.parseInt(nfMaxIntFullTraceStart.getText());
			Prefs.set("BigTrace.dAutoMinStartTraceInt",fullAutoTrace.dAutoMinStartTraceInt );

			fullAutoTrace.nAutoMinPointsCurve = Integer.parseInt(nfAutoMinCurvePoints.getText());
			Prefs.set("BigTrace.nAutoMinPointsCurve",fullAutoTrace.nAutoMinPointsCurve );
			
			bt.bInputLock = true;
			bt.setLockMode(true);
			fullAutoTrace.addPropertyChangeListener( bt.btPanel );
			butAuto.setEnabled( true );
			butAuto.setIcon( tabIconCancel );
			butAuto.setToolTipText( "Stop auto trace" );
			fullAutoTrace.butAuto = butAuto;
			fullAutoTrace.tabIconRestore = tabIconAuto;
			fullAutoTrace.execute();
		}

	}

	@Override
	public void actionPerformed( ActionEvent e )
	{
		// RUN TRACKING
		if(e.getSource() == butAuto)
		{
			if(!bt.bInputLock)
			{
				launchFullAutoTrace();
			}
			else
			{
				if(bt.bInputLock && butAuto.isEnabled() && fullAutoTrace!=null && !fullAutoTrace.isCancelled() && !fullAutoTrace.isDone())
				{
					fullAutoTrace.cancel( false );
				}
			}
		}
		
	}
}

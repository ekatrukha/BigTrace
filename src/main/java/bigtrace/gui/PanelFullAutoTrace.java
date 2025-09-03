package bigtrace.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

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

public class PanelFullAutoTrace
{
	
	static public <T extends RealType< T > & NativeType< T >> void launchFullAutoTrace(final BigTrace< T > bt)
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
			timeRange.makeConstrained( bt.btData.nCurrTimepoint, bt.btData.nCurrTimepoint );
			
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
			
			FullAutoTrace<T> fullAutoTrace = new FullAutoTrace<>(bt);
			if(timeRange != null)
			{
				fullAutoTrace.nFirstTP = timeRange.getMin();
				fullAutoTrace.nLastTP = timeRange.getMax();
			}
			fullAutoTrace.dAutoMinStartTraceInt = Integer.parseInt(nfMaxIntFullTraceStart.getText());
			Prefs.set("BigTrace.dAutoMinStartTraceInt",fullAutoTrace.dAutoMinStartTraceInt );

			fullAutoTrace.nAutoMinPointsCurve = Integer.parseInt(nfAutoMinCurvePoints.getText());
			Prefs.set("BigTrace.nAutoMinPointsCurve",fullAutoTrace.nAutoMinPointsCurve );
			
			fullAutoTrace.addPropertyChangeListener( bt.btPanel );
			fullAutoTrace.execute();
		}

	}
}

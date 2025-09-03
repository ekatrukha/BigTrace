package bigtrace.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
		
		JPanel dialogFullAutoSettings = new JPanel();
		
		dialogFullAutoSettings.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();

		GBCHelper.alighLeft(gbc);
		
		NumberField nfMaxIntFullTraceStart = new NumberField(4);		
		
		nfMaxIntFullTraceStart.setText( Integer.toString((int)Prefs.get("BigTrace.dMinStartTraceInt",128.0)) );
	
		gbc.gridx = 0;
		gbc.gridy = 0;
		dialogFullAutoSettings.add(new JLabel("Min intensity trace start:"),gbc);
		gbc.gridx++;
		nfMaxIntFullTraceStart.setIntegersOnly( true );
		nfMaxIntFullTraceStart.setText(Integer.toString((int)(Prefs.get("BigTrace.nTrackExpandBox", 0))));
		dialogFullAutoSettings.add(nfMaxIntFullTraceStart,gbc);
		
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
		
		int reply = JOptionPane.showConfirmDialog(null, dialogFullAutoSettings, "Full auto tracing", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			FullAutoTrace<T> fullAutoTrace = new FullAutoTrace<>(bt);
			if(timeRange != null)
			{
				fullAutoTrace.nFirstTP = timeRange.getMin();
				fullAutoTrace.nLastTP = timeRange.getMax();
			}
			fullAutoTrace.dMinStartTraceInt = Integer.parseInt(nfMaxIntFullTraceStart.getText());
			Prefs.set("BigTrace.dMinStartTraceInt",fullAutoTrace.dMinStartTraceInt );
			fullAutoTrace.addPropertyChangeListener( bt.btPanel );
			fullAutoTrace.execute();
		}

	}
}

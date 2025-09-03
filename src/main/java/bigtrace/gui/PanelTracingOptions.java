package bigtrace.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bigtrace.BigTrace;
import ij.Prefs;


/** General tracing options, common for all tracings **/
public class PanelTracingOptions extends JPanel
{
	final BigTrace<?> bt;
	
	final NumberField nfSigmaX = new NumberField(4);
	final NumberField nfSigmaY = new NumberField(4);
	final NumberField nfSigmaZ = new NumberField(4);
	
	final JCheckBox cbTraceOnlyClipped = new JCheckBox();
	
	public PanelTracingOptions(final BigTrace<?> bt_)
	{
		super();
		bt = bt_;
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.000", decimalFormatSymbols);
	
		nfSigmaX.setText(df.format(bt.btData.sigmaTrace[0]));
		nfSigmaY.setText(df.format(bt.btData.sigmaTrace[1]));
		nfSigmaZ.setText(df.format(bt.btData.sigmaTrace[2]));

		cbTraceOnlyClipped.setSelected(bt.btData.bTraceOnlyClipped);
		
		gbc.gridx=0;
		gbc.gridy=0;

		this.add(new JLabel("Curve thickness X axis (SD, px): "),gbc);
		gbc.gridx++;
		this.add(nfSigmaX,gbc);
		
		gbc.gridx=0;
		gbc.gridy++;
		this.add(new JLabel("Curve thickness Y axis (SD, px): "),gbc);
		gbc.gridx++;
		this.add(nfSigmaY,gbc);
		
		gbc.gridx=0;
		gbc.gridy++;
		this.add(new JLabel("Curve thickness Z axis (SD, px): "),gbc);
		gbc.gridx++;
		this.add(nfSigmaZ,gbc);

		
		gbc.gridx=0;
		gbc.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		this.add(new JLabel("Trace only clipped volume: "),gbc);
		gbc.gridx++;
		this.add(cbTraceOnlyClipped,gbc);
	}
	
	public void getSetOptions()
	{
		bt.btData.sigmaTrace[0] = Double.parseDouble(nfSigmaX.getText());
		Prefs.set("BigTrace.sigmaTraceX", bt.btData.sigmaTrace[0]);
		
		bt.btData.sigmaTrace[1] = Double.parseDouble(nfSigmaY.getText());
		Prefs.set("BigTrace.sigmaTraceY", bt.btData.sigmaTrace[1]);
		
		bt.btData.sigmaTrace[2] = Double.parseDouble(nfSigmaZ.getText());
		Prefs.set("BigTrace.sigmaTraceZ", bt.btData.sigmaTrace[2]);
		
		bt.btData.bTraceOnlyClipped = cbTraceOnlyClipped.isSelected();
		Prefs.set("BigTrace.bTraceOnlyClipped", bt.btData.bTraceOnlyClipped);	
	}
}

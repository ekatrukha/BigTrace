package bigtrace.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;




public class VoxelSizePanel extends JPanel implements NumberField.Listener, FocusListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7324190494486161144L;
	
	private double [] vxAllSize;
	public NumberField [] nfAllSize;
	public JTextField tfUnits; 
	private ArrayList<Listener> listeners =	new ArrayList<Listener>();
	
	public static interface Listener {
		public void voxelSizeChanged(double [] newVoxelSize);

	}
	
	public VoxelSizePanel(double [] dVoxelSize, String sUnits)
	{
		super();
		int i;
		
		vxAllSize = new double [3];
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.000", decimalFormatSymbols);
		
		nfAllSize = new NumberField[3];
		for (i=0;i<3;i++)
		{
			vxAllSize[i]=dVoxelSize[i];
			nfAllSize[i]=new NumberField(4);
			nfAllSize[i].setText(df.format( dVoxelSize[i]));
			nfAllSize[i].setMinimumSize(nfAllSize[i].getPreferredSize());
			nfAllSize[i].addListener(this);
			nfAllSize[i].addNumberFieldFocusListener(this);
			
		}

		tfUnits = new JTextField(8);
		tfUnits.setText(sUnits);	

		setLayout(gridbag);

		
		c.fill=GridBagConstraints.HORIZONTAL;
		//c.weightx = 0.5;
		c.gridx=0;
		c.gridy=0;
		this.add(new JLabel("X"),c);
		c.gridx++;
		this.add(nfAllSize[0],c);
		//c.gridx=0;		
		c.gridx++;
		this.add(new JLabel("Y"),c);
		c.gridx++;
		this.add(nfAllSize[1],c);
		//c.gridx=0;
		c.gridx++;
		this.add(new JLabel("Z"),c);
		c.gridx++;
		this.add(nfAllSize[2],c);
		c.gridwidth=3;
		c.gridx=0;
		c.gridy++;
		//c.weightx = 0.0;
		JLabel jlUnits =new JLabel("Units");
		jlUnits.setHorizontalAlignment(SwingConstants.RIGHT);
		this.add(jlUnits,c);
		c.gridx+=3;
		this.add(tfUnits,c);
	}

	@Override
	public void valueChanged(double v) {
	
		if(updateVoxelSize())
			fireVoxelSizeChanged(vxAllSize);
	}
	
	private void fireVoxelSizeChanged(double [] newVoxelSize) 
	{
		for(Listener l : listeners)
			l.voxelSizeChanged(newVoxelSize);
	}
	public void addVoxelSizePanelListener(Listener l) {
        listeners.add(l);
    }

	@Override
	public void focusGained(FocusEvent arg0) {

	
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		if(updateVoxelSize())
			fireVoxelSizeChanged(vxAllSize);
	}
	public void setVoxelSize(double [] dVoxelSize, String sUnits)
	{
		tfUnits.setText(sUnits);
		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.000", decimalFormatSymbols);
		for (int i=0;i<3;i++)
		{
			nfAllSize[i].setText(df.format( dVoxelSize[i]));
			vxAllSize[i]= dVoxelSize[i];
		}
		fireVoxelSizeChanged(vxAllSize);
	}
	
	private boolean updateVoxelSize()
	{
		double diff = 0.0;
		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.00", decimalFormatSymbols);
		
		for(int i=0;i<3;i++)
		{
			//just in case
			if(Double.parseDouble(nfAllSize[i].getText())<=0)
			{
				nfAllSize[i].setText(df.format(vxAllSize[i]));
			}
			diff += Math.abs(vxAllSize[i]-Double.parseDouble(nfAllSize[i].getText()));
			vxAllSize[i]=Double.parseDouble(nfAllSize[i].getText());
		}
		if(diff>0.001)
			return true;
		else 
			return false;
	}
	
	public void allowVoxelSizeChange(boolean allow)
	{
		for(int i=0;i<3;i++)
		{
			nfAllSize[i].setTFEnabled(allow);
		}
		tfUnits.setEnabled(allow);
	}

}

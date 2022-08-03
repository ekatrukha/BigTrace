package bigtrace.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
		
		nfAllSize = new NumberField[3];
		for (i=0;i<3;i++)
		{
			vxAllSize[i]=dVoxelSize[i];
			nfAllSize[i]=new NumberField(5);
			nfAllSize[i].setText(String.format("%.3f", dVoxelSize[i]));
			nfAllSize[i].addListener(this);
			nfAllSize[i].addNumberFieldFocusListener(this);
			
		}

		tfUnits = new JTextField(8);
		tfUnits.setText(sUnits);	
		tfUnits.setHorizontalAlignment(SwingConstants.LEFT);
		setLayout(gridbag);
		c.gridx=0;
		c.gridy=0;
		this.add(new JLabel("Voxel width"),c);
		c.gridx++;
		this.add(nfAllSize[0],c);
		c.gridx=0;
		c.gridy++;
		this.add(new JLabel("Voxel height"),c);
		c.gridx++;
		this.add(nfAllSize[1],c);
		c.gridx=0;
		c.gridy++;
		this.add(new JLabel("Voxel depth"),c);
		c.gridx++;
		this.add(nfAllSize[2],c);
		c.gridx=0;
		c.gridy++;
		this.add(new JLabel("Units"),c);
		c.gridx++;
		this.add(tfUnits,c);
				

	}

	@Override
	public void valueChanged(double v) {
		
		
		// TODO Auto-generated method stub
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

		// TODO Auto-generated method stub
	
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		// TODO Auto-generated method stub
		if(updateVoxelSize())
			fireVoxelSizeChanged(vxAllSize);
	}
	public void setVoxelSize(double [] dVoxelSize, String sUnits)
	{
		tfUnits.setText(sUnits);
		for (int i=0;i<3;i++)
		{
			nfAllSize[i].setText(String.format("%.3f", dVoxelSize[i]));
			vxAllSize[i]= dVoxelSize[i];
		}
		fireVoxelSizeChanged(vxAllSize);
	}
	
	private boolean updateVoxelSize()
	{
		double diff = 0.0;
		
		for(int i=0;i<3;i++)
		{
			//just in case
			if(Double.parseDouble(nfAllSize[i].getText())<=0)
			{
				nfAllSize[i].setText(String.format("%.3f", vxAllSize[i]));
			}
			diff += Math.abs(vxAllSize[i]-Double.parseDouble(nfAllSize[i].getText()));
			vxAllSize[i]=Double.parseDouble(nfAllSize[i].getText());
		}
		if(diff>0.001)
			return true;
		else 
			return false;
	}
}

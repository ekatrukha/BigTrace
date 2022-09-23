package bigtrace.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import bigtrace.BigTrace;

import net.imglib2.type.numeric.RealType;


public class RenderMethodPanel < T extends RealType< T > > extends JPanel implements ActionListener{
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 7367842640615289454L;
	public JComboBox<String> cbRenderMethod;
	BigTrace<T> bt;
	
	public RenderMethodPanel(BigTrace<T> bt_)
	{
		super();
		bt =bt_;
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		String[] sMethods = new String[2];
		sMethods[0]="Maximum intensity";
		sMethods[1]="Volumetric";
	
		cbRenderMethod = new JComboBox<>(sMethods);
		cbRenderMethod.setSelectedIndex(bt.btdata.nRenderMethod);
		cbRenderMethod.addActionListener(this);
		
		setLayout(gridbag);
		c.gridx=0;
		c.gridy=0;
		this.add(cbRenderMethod,c);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cbRenderMethod)
		{
			bt.btpanel.setRenderMethod(cbRenderMethod.getSelectedIndex());
		
		}
	}

}

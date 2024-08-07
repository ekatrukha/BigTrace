package bigtrace.animation;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.gui.PanelTitle;

public class AnimationPanel < T extends RealType< T > & NativeType< T > > extends JPanel implements ListSelectionListener, ActionListener
{
	final BigTrace<T> bt;
	
	JButton butRecord;
	JButton butUncoil;
	JButton butSettings;
	JSlider timeSlider;
	

	public AnimationPanel(final BigTrace<T> bt)
	{
		this.bt = bt;
		JPanel panAnimTools = new JPanel(new GridBagLayout());  
		panAnimTools.setBorder(new PanelTitle(" Animation "));
		int nButtonSize = 40;		
		GridBagConstraints cr = new GridBagConstraints();

		
		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/camera.png");
		ImageIcon tabIcon = new ImageIcon(icon_path);		
		butRecord = new JButton(tabIcon);
		butRecord.setToolTipText("Record video");
		butRecord.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		tabIcon = new ImageIcon(icon_path);
		butSettings = new JButton(tabIcon);
		butSettings.setToolTipText("Settings");
		butSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		
		butRecord.addActionListener( this );
		butSettings.addActionListener( this );
				
		cr.gridx=0;
		cr.gridy=0;
		panAnimTools.add(butRecord,cr);
		
		cr.gridx++;
		JSeparator sp = new JSeparator(SwingConstants.VERTICAL);
		sp.setPreferredSize(new Dimension((int) (nButtonSize*0.5),nButtonSize));
		panAnimTools.add(sp,cr);
		//filler
		cr.gridx++;
		cr.weightx = 0.01;
		panAnimTools.add(new JLabel(), cr);
		cr.gridx++;
		panAnimTools.add(butSettings,cr);
		
		
		JPanel panAnimPlot = new JPanel(new GridBagLayout());
		panAnimPlot.setBorder(new PanelTitle(" Key Frames "));
		
		timeSlider = new JSlider(SwingConstants.VERTICAL,1,100,1);
		timeSlider.setMajorTickSpacing(10);
		timeSlider.setMinorTickSpacing(1);
		timeSlider.setPaintTicks(true);
		timeSlider.setPaintLabels(true);
		timeSlider.setInverted( true );
		panAnimPlot.add( timeSlider );
		
		//put all panels together
		cr = new GridBagConstraints();
		setLayout(new GridBagLayout());
		cr.insets=new Insets(4,4,2,2);
		cr.gridx=0;
		cr.gridy=0;
		cr.fill = GridBagConstraints.HORIZONTAL;

		//Line Tools
		add(panAnimTools,cr);
		
		//roi list
		cr.gridy++;
		cr.weighty = 0.99;
		cr.fill = GridBagConstraints.BOTH;
		add(panAnimPlot,cr);

		// Blank/filler component
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		add(new JLabel(), cr);    
	}
	
	@Override
	public void actionPerformed( ActionEvent arg0 )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void valueChanged( ListSelectionEvent arg0 )
	{
		// TODO Auto-generated method stub
		
	}



}

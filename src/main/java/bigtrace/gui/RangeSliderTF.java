package bigtrace.gui;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import com.jidesoft.swing.RangeSlider;



public class RangeSliderTF extends JPanel implements FocusListener, NumberField.Listener, ChangeListener{

	public static void main(String[] args) {
		
		JFrame frame = new JFrame();
		RangeSliderTF slider = new RangeSliderTF(new int[] {-100, 100}, new int[] {20, 50});
		frame.getContentPane().add(slider);
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;



	public static interface Listener {
		public void sliderChanged();
	}
	
	private RangeSlider slider;
	private NumberField minTF = new NumberField(4);
	private NumberField maxTF = new NumberField(4);

	private boolean bConstrained = false;
	private int nLowerMax;
	private int nHigherMin;

	
	private ArrayList<Listener> listeners = new ArrayList<>();
	
	public RangeSliderTF(int[] realMinMax, int[] setMinMax) {
		super();

		minTF.setIntegersOnly(true);
		minTF.addListener(this);
		minTF.addNumberFieldFocusListener(this);
		maxTF.setIntegersOnly(true);
		maxTF.addListener(this);
		maxTF.addNumberFieldFocusListener(this);
		slider = new RangeSlider(realMinMax[0], realMinMax[1], setMinMax[0], setMinMax[1]);
		nLowerMax = realMinMax[0];
		nHigherMin = realMinMax[1];
		slider.addChangeListener(this);
		
		/*setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
		slider.setAlignmentY(Component.LEFT_ALIGNMENT);
		minTF.setAlignmentY(Component.CENTER_ALIGNMENT);
		add(minTF);//, BorderLayout.WEST );
		add(slider);//, BorderLayout.CENTER );
		add(maxTF);//, BorderLayout.EAST );*/
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);
		c.gridx = 0;
		c.gridy = 0;		
		//c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		//c.
		minTF.setMinimumSize(minTF.getPreferredSize());
		add(minTF, c);
		
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.99;

		add(slider, c);

		c.fill = GridBagConstraints.NONE;
		c.gridx ++;
		c.weightx = 0.0;
		maxTF.setMinimumSize(maxTF.getPreferredSize());
		add(maxTF, c);

		updateTextfieldsFromSliders();
	}
	public NumberField getMinField() {
		return minTF;
	}

	public NumberField getMaxField() {
		return maxTF;
	}

	public int getMin() {
		return slider.getLowValue();
	}

	public int getMax() {
		return slider.getHighValue();
	}
	
	public void setMinAndMax(int min, int max) {
		slider.setLowValue(min);
		minTF.setText(Integer.toString(min));

		slider.setHighValue(max);
		maxTF.setText(Integer.toString(max));

		slider.repaint();
	}
	
	public void makeConstrained(int nLowerMax_, int nHigherMin_)
	{
		nLowerMax = nLowerMax_;
		nHigherMin = nHigherMin_;
		bConstrained = true;
	}
	
	@Override
	public void valueChanged(double v) {
	
			
			// TODO Auto-generated method stub
			try {
				//slider.getMaximum();
				int nMinV = Math.min(Integer.parseInt(minTF.getText()),slider.getMaximum());
				int nMaxV = Integer.parseInt(maxTF.getText());
			
				slider.setLowValue(nMinV);
				slider.setHighValue(nMaxV);
				slider.repaint();
				fireSliderChanged();
				
			} catch(Exception ex) 
			{
				System.out.println(ex.getMessage());
			}
		
	}

	@Override
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub
		JTextField tf = (JTextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		// TODO Auto-generated method stub
		valueChanged(0);
	}
	private void updateTextfieldsFromSliders() {
		minTF.setText(Integer.toString(slider.getLowValue()));
		maxTF.setText(Integer.toString(slider.getHighValue()));
		fireSliderChanged();
	}
	public void set(final int[] realMinMax, final int[] setMinMax) {		
		slider.setLowValue(realMinMax[0]);
		slider.setLowValue(realMinMax[1]);
		slider.setMinimum(setMinMax[0]);
		slider.setMaximum(setMinMax[1]);
	}

	public void addSliderChangeListener(Listener l) {
		listeners.add(l);
	}

	public void removeSliderChangeListener(Listener l) {
		listeners.remove(l);
	}
	
	private void fireSliderChanged() {
		for(Listener l : listeners)
			l.sliderChanged();
	}
	@Override
	public void stateChanged(ChangeEvent e) {

		if(bConstrained)
		{
			if(slider.getLowValue()>nLowerMax)
			{
				slider.setLowValue( nLowerMax );
			}
			if(slider.getHighValue()<nHigherMin)
			{
				slider.setHighValue( nHigherMin );
			}
		}
		updateTextfieldsFromSliders();
	}
}

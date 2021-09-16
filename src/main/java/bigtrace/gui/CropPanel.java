package bigtrace.gui;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;


public class CropPanel extends JPanel {
	
	public static void main(String[] args) {
	
		try {
		    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		JFrame frame = new JFrame();

		CropPanel slider = new CropPanel(new long[] {60,80,100});
		frame.getContentPane().add(slider);
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1885320351623882576L;
	private RangeSliderTF bbX;
	private RangeSliderTF bbY;
	private RangeSliderTF bbZ;
	private ArrayList<Listener> listeners =	new ArrayList<Listener>();
	
	public static interface Listener {
		public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1);

	}
	
	private RangeSliderTF addRangeSlider(String label, int[] realMinMax, int[] setMinMax, GridBagConstraints c) {
		RangeSliderTF slider = new RangeSliderTF(realMinMax, setMinMax);

		GridBagLayout layout = (GridBagLayout)getLayout();

		c.gridx = 0;
		if(label != null) {
			JLabel theLabel = new JLabel(label);
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.EAST;
			c.gridwidth = 1;
			c.weightx = 0;
			layout.setConstraints(theLabel, c);
			add(theLabel);
			c.gridx++;
		}
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		layout.setConstraints(slider, c);
		add(slider);
		c.gridy++;
		return slider;
	}
	
	//public CropPanel(int nW, int nH, int nSl) {
	public CropPanel(long [] maxDim) {
		super();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		setLayout(gridbag);

		c.gridy = 0;
		

		bbX = addRangeSlider(
				"X",
				new int[] {0, (int) maxDim[0]},
				new int[] {0, (int) maxDim[0]},
				c);
		bbY = addRangeSlider(
				"Y",
				new int[] {0, (int) maxDim[1]},
				new int[] {0, (int) maxDim[1]},
				c);
		bbZ = addRangeSlider(
				"Z",
				new int[] {0, (int) maxDim[2]},
				new int[] {0, (int) maxDim[2]},
				c);




		RangeSliderTF.Listener bbListener = new RangeSliderTF.Listener() {
			@Override
			public void sliderChanged() {
				fireBoundingBoxChanged(
						bbX.getMin(),
						bbY.getMin(),
						bbZ.getMin(),
						bbX.getMax(),
						bbY.getMax(),
						bbZ.getMax());
			}
		};
		bbX.addSliderChangeListener(bbListener);
		bbY.addSliderChangeListener(bbListener);
		bbZ.addSliderChangeListener(bbListener);
	}


	public int getBBXMin() {
		return bbX.getMin();
	}

	public int getBBYMin() {
		return bbY.getMin();
	}

	public int getBBZMin() {
		return bbZ.getMin();
	}

	public int getBBXMax() {
		return bbX.getMax();
	}

	public int getBBYMax() {
		return bbY.getMax();
	}

	public int getBBZMax() {
		return bbZ.getMax();
	}
	public void setBoundingBox(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		bbX.setMinAndMax(bbx0, bbx1);
		bbY.setMinAndMax(bby0, bby1);
		bbZ.setMinAndMax(bbz0, bbz1);
	}

	public void addCropPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireBoundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		for(Listener l : listeners)
			l.boundingBoxChanged(bbx0, bby0, bbz0, bbx1, bby1, bbz1);
	}

}

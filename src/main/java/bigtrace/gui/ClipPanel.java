package bigtrace.gui;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;

import net.imglib2.Interval;


public class ClipPanel extends JPanel {
	
	public static void main(String[] args) {
	
		try {
		    UIManager.setLookAndFeel( new FlatIntelliJLaf() );
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		JFrame frame = new JFrame();

		ClipPanel slider = new ClipPanel(new long[] {60,80,100});
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
	
	public JButton butExtractClipped;
	public JCheckBox showClippedBox;
	public JCheckBox clipROIBox;
	
	public static interface Listener {
		public void boundingBoxChanged(long [][] box);

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
		//c.gridx++;
		c.gridwidth=3;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		layout.setConstraints(slider, c);
		add(slider);
		c.gridy++;
		return slider;
	}
	
	//public ClipPanel(int nW, int nH, int nSl) {
	public ClipPanel(long [] maxDim) {
		super();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints cd = new GridBagConstraints();

		setLayout(gridbag);

		cd.gridy = 0;
		//cd.gridwidth=3;

		bbX = addRangeSlider(
				"X",
				new int[] {0, (int) maxDim[0]},
				new int[] {0, (int) maxDim[0]},
				cd);
		bbY = addRangeSlider(
				"Y",
				new int[] {0, (int) maxDim[1]},
				new int[] {0, (int) maxDim[1]},
				cd);
		bbZ = addRangeSlider(
				"Z",
				new int[] {0, (int) maxDim[2]},
				new int[] {0, (int) maxDim[2]},
				cd);
		cd.gridwidth=1;
		cd.weightx = 0.1;
		cd.fill = GridBagConstraints.NONE;
		cd.anchor = GridBagConstraints.WEST;
		showClippedBox = new JCheckBox("Box", false);
		
		clipROIBox = new JCheckBox("Clip ROIs", false);
		cd.gridx=1;
		//c.gridy++;
		this.add(showClippedBox,cd);
		cd.gridx=2;
		this.add(clipROIBox,cd);
		cd.gridx=3;
		cd.anchor = GridBagConstraints.EAST;
		butExtractClipped = new JButton("Extract");
		this.add(butExtractClipped,cd);


		RangeSliderTF.Listener bbListener = new RangeSliderTF.Listener() {
			@Override
			public void sliderChanged() {
				long [][] new_box = new long [2][3];
				new_box [0][0]=bbX.getMin();
				new_box [1][0]=bbX.getMax();
				new_box [0][1]=bbY.getMin();
				new_box [1][1]=bbY.getMax();
				new_box [0][2]=bbZ.getMin();
				new_box [1][2]=bbZ.getMax();
				fireBoundingBoxChanged(new_box);
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
	
	public void setBoundingBox(long [][] box) {
		bbX.setMinAndMax((int)box[0][0], (int)box[1][0]);
		bbY.setMinAndMax((int)box[0][1], (int)box[1][1]);
		bbZ.setMinAndMax((int)box[0][2], (int)box[1][2]);
	}
	public void setBoundingBox(Interval interval) {
		long [][] box = new long[2][3];
		box[0]=interval.minAsLongArray();
		box[1]=interval.maxAsLongArray();
		bbX.setMinAndMax((int)box[0][0], (int)box[1][0]);
		bbY.setMinAndMax((int)box[0][1], (int)box[1][1]);
		bbZ.setMinAndMax((int)box[0][2], (int)box[1][2]);
	}

	public void addClipPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireBoundingBoxChanged(long [][] box) {
		for(Listener l : listeners)
			l.boundingBoxChanged(box);
	}

}

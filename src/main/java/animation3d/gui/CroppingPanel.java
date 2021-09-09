package animation3d.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ij.IJ;
import ij.ImagePlus;

public class CroppingPanel extends JPanel {

	/*public static void main(String[] args) {
		ImagePlus imp = IJ.openImage("d:/flybrain.tif");
		JFrame frame = new JFrame();

		//CroppingPanel slider = new CroppingPanel(imp);
		//frame.getContentPane().add(slider);
		frame.pack();
		frame.setVisible(true);
	}*/

	private static final long serialVersionUID = 1L;


	private DoubleSlider nearfar;
	private DoubleSlider bbX;
	private DoubleSlider bbY;
	private DoubleSlider bbZ;
	private JCheckBox allChannels;

	public static interface Listener {
		public void nearFarChanged(int near, int far);
		public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1);
		//public void cutOffROI();
	}

	private ArrayList<Listener> listeners =	new ArrayList<Listener>();

	private DoubleSlider addDoubleSlider(String label, int[] realMinMax, int[] setMinMax, Color color, GridBagConstraints c) {
		DoubleSlider slider = new DoubleSlider(realMinMax, setMinMax, color);

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

	public boolean applyToAllChannels() {
		return allChannels.isSelected();
	}

	public static double getNear(ImagePlus image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int d = image.getNSlices();
		return -0.5 * Math.sqrt(w * w + h * h + d * d);
	}

	public static double getFar(ImagePlus image) {
		return -getNear(image);
	}
/*
	public CroppingPanel(ImagePlus image) {
		super();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		int near = (int)Math.floor(getNear(image));
		int far = (int)Math.ceil(getFar(image));

		c.gridy = 0;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		//c.fill = GridBagConstraints.NONE;
		
		nearfar = addDoubleSlider(
				"near/far",
				new int[] {near, far},
				new int[] {near, far},
				new Color(255, 0, 0, 100),
				c);
		c.gridy = 1;
		bbX = addDoubleSlider(
				"x_range",
				new int[] {0, image.getWidth()},
				new int[] {0, image.getWidth()},
				new Color(255, 0, 0, 100),
				c);
		bbY = addDoubleSlider(
				"y_range",
				new int[] {0, image.getHeight()},
				new int[] {0, image.getHeight()},
				new Color(255, 0, 0, 100),
				c);
		bbZ = addDoubleSlider(
				"z_range",
				new int[] {0, image.getNSlices()},
				new int[] {0, image.getNSlices()},
				new Color(255, 0, 0, 100),
				c);

		nearfar.addSliderChangeListener(new DoubleSlider.Listener() {
			@Override
			public void sliderChanged() {
				fireNearFarChanged(nearfar.getMin(), nearfar.getMax());
			}
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		allChannels = new JCheckBox("Apply to all channels", true);
		buttons.add(allChannels);

		JButton b = new JButton("Cut off ROI");
		buttons.add(b);

		c.gridx = 0;
		c.insets = new Insets(7, 0, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(buttons, c);
		add(buttons);
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						fireCutOffROI();
					}
				}.start();
			}
		});
		

		DoubleSlider.Listener bbListener = new DoubleSlider.Listener() {
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
	}*/
	public CroppingPanel(int [] nf, int nW, int nH, int nSl) {
		super();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		setLayout(gridbag);

		int near = nf[0];
		int far = nf[1];

		c.gridy = 0;
		

		nearfar = addDoubleSlider(
				"near/far",
				new int[] {near, far},
				new int[] {near, far},
				new Color(0, 0, 255, 100),
				c);

		bbX = addDoubleSlider(
				"x_range",
				new int[] {0, nW},
				new int[] {0, nW},
				new Color(0, 0, 255, 100),
				c);
		bbY = addDoubleSlider(
				"y_range",
				new int[] {0, nH},
				new int[] {0, nH},
				new Color(0, 0, 255, 100),
				c);
		bbZ = addDoubleSlider(
				"z_range",
				new int[] {0, nSl},
				new int[] {0, nSl},
				new Color(0, 0, 255, 100),
				c);

		nearfar.addSliderChangeListener(new DoubleSlider.Listener() {
			@Override
			public void sliderChanged() {
				fireNearFarChanged(nearfar.getMin(), nearfar.getMax());
			}
		});

		/*
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		allChannels = new JCheckBox("Apply to all channels", true);
		buttons.add(allChannels);

		JButton b = new JButton("Cut off ROI");
		buttons.add(b);

		c.gridx = 0;
		c.insets = new Insets(7, 0, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(buttons, c);
		add(buttons);
		
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						fireCutOffROI();
					}
				}.start();
			}
		});
*/
		DoubleSlider.Listener bbListener = new DoubleSlider.Listener() {
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

	public int getNear() {
		return nearfar.getMin();
	}

	public int getFar() {
		return nearfar.getMax();
	}

	public void setBoundingBox(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		bbX.setMinAndMax(bbx0, bbx1);
		bbY.setMinAndMax(bby0, bby1);
		bbZ.setMinAndMax(bbz0, bbz1);
	}

	public void setNearAndFar(int near, int far) {
		nearfar.setMinAndMax(near, far);
	}

	public void addCroppingPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireBoundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		for(Listener l : listeners)
			l.boundingBoxChanged(bbx0, bby0, bbz0, bbx1, bby1, bbz1);
	}

	private void fireNearFarChanged(int near, int far) {
		for(Listener l : listeners)
			l.nearFarChanged(near, far);
	}
/*
	private void fireCutOffROI() {
		for(Listener l : listeners)
			l.cutOffROI();
	}*/
}
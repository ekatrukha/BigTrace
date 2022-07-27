package bigtrace.rois;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bigtrace.BigTrace;
import bigtrace.gui.PanelTitle;
import ij.Prefs;

import ij.gui.Plot;
import ij.measure.ResultsTable;
import net.imglib2.RandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.RealType;

import net.imglib2.view.IntervalView;

public class RoiMeasure3D < T extends RealType< T > > extends JPanel implements ListSelectionListener, ActionListener, Measurements { 
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4635723145578489755L;
	BigTrace bt;
	
	JButton butLineProfile;
	JButton butSettings;
	JButton butMeasure;
	JButton butMeasureAll;
	JList<String> jlist ;
	JScrollPane listScroller;
	
	public JComboBox<String> cbActiveChannel;
	// Order must agree with order of checkboxes in Set Measurements dialog box
	private static final int[] list = { LENGTH,  DIST_ENDS, MEAN, STD_DEV, STRAIGHTNESS, ENDS_COORDS, ENDS_DIR};
	private static final String[] colTemplates = { "Length", "Distance_between_ends", "Mean_intensity", "SD_intensity", "Straightness", "End_","Direction_"};

	InterpolatorFactory<T, RandomAccessible< T >> nInterpolatorFactory;
	
	public static final int INT_NearestNeighbor=0, INT_NLinear=1, INT_Lanczos=2; // Intensity interpolation types
	public static final int SHAPE_Voxel=0, SHAPE_Subvoxel=1; // Shape interpolation types

	private static int systemMeasurements = Prefs.getInt("BigTrace.Measurements",LENGTH+MEAN);
	
	private static int intensityInterpolation = Prefs.getInt("BigTrace.IntInterpolation",INT_NearestNeighbor);
	private static int shapeInterpolation = Prefs.getInt("BigTrace.ShapeInterpolation",SHAPE_Voxel);
	
	private static ResultsTable systemRT = new ResultsTable();
	private ResultsTable rt;

	public RoiMeasure3D(BigTrace bt)
	{
		this.bt = bt;


		int nButtonSize = 40;
		
		rt = systemRT;
		
		setInterpolationFactory();
		JPanel panLineTools = new JPanel(new GridBagLayout());  
		panLineTools.setBorder(new PanelTitle(" Tools "));

		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/line_profile.png");
		ImageIcon tabIcon = new ImageIcon(icon_path);
		butLineProfile = new JButton(tabIcon);
		butLineProfile.setToolTipText("Plot Profile");
		butLineProfile.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));

		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		tabIcon = new ImageIcon(icon_path);
		butSettings = new JButton(tabIcon);
		butSettings.setToolTipText("Settings");
		butSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));

		butLineProfile.addActionListener(this);
		butSettings.addActionListener(this);

		//this.setBorder(new PanelTitle(" Measurements "));

		//add to the panel
		GridBagConstraints cr = new GridBagConstraints();
		cr.gridx=0;
		cr.gridy=0;
		panLineTools.add(butLineProfile,cr);
		cr.gridx++;
		JSeparator sp = new JSeparator(SwingConstants.VERTICAL);
		sp.setPreferredSize(new Dimension((int) (nButtonSize*0.5),nButtonSize));
		panLineTools.add(sp,cr);
		cr.gridx++;
		//panTracing.add(roiSettings,ct);
		//filler
		//ct.gridx++;
		cr.weightx = 0.01;
		panLineTools.add(new JLabel(), cr);
		cr.gridx++;
		panLineTools.add(butSettings,cr);

		
		JPanel panMeasure = new JPanel(new GridBagLayout());
		panMeasure.setBorder(new PanelTitle(" Measure "));
		cr = new GridBagConstraints();
		
		//synchronized ROI list
		jlist = new JList<String>(bt.roiManager.listModel);
		listScroller = new JScrollPane(jlist);
		jlist.addListSelectionListener(this);
		listScroller.setPreferredSize(new Dimension(170, 400));
		listScroller.setMinimumSize(new Dimension(170, 250));

		this.add(listScroller);	
		cr.gridx=0;
		cr.gridy=0;
		cr.gridheight=GridBagConstraints.REMAINDER;
		panMeasure.add(listScroller,cr);

		butMeasure = new JButton("Measure");
		butMeasure.addActionListener(this);
		cr.gridx++;
		cr.gridy++;
		cr.gridheight=1;

		panMeasure.add(butMeasure,cr);

		butMeasureAll = new JButton("Measure All");
		butMeasureAll.addActionListener(this);
		cr.gridy++;
		panMeasure.add(butMeasureAll,cr);

		// Blank/filler component
		cr.gridx++;
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		panMeasure.add(new JLabel(), cr);		

		// a solution for now
		butMeasure.setMinimumSize(butMeasureAll.getPreferredSize());
		butMeasure.setPreferredSize(butMeasureAll.getPreferredSize()); 
		
		
		JPanel panChannel = new JPanel(new GridBagLayout());
		panChannel.setBorder(new PanelTitle(""));

		String[] nCh = new String[bt.btdata.nTotalChannels];
		for(int i=0;i<nCh.length;i++)
		{
			nCh[i] = "channel "+Integer.toString(i+1);
		}
		cbActiveChannel = new JComboBox<>(nCh);
		cbActiveChannel.setSelectedIndex(0);
		cbActiveChannel.addActionListener(this);
		
		cr = new GridBagConstraints();
	    cr.gridx=0;
		cr.gridy=0;
		panChannel.add(new JLabel("Measure"),cr);
		cr.gridx++;
		panChannel.add(cbActiveChannel,cr);
		
		
		//put all panels together
		cr = new GridBagConstraints();
		setLayout(new GridBagLayout());
		cr.insets=new Insets(4,4,2,2);
		cr.gridx=0;
		cr.gridy=0;
		cr.fill = GridBagConstraints.HORIZONTAL;


		
		//Line Tools
		add(panLineTools,cr);
		//roi list
		cr.gridy++;
		add(panMeasure,cr);
		cr.gridy++;
		add(panChannel,cr);
		// Blank/filler component
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		add(new JLabel(), cr);    
	}

	/** show Measure settings dialog**/
	public void dialSettings()
	{
		
		JPanel pMeasureSettings = new JPanel(new GridLayout(0,2,6,0));
	
		ArrayList<JCheckBox> measureStates = new ArrayList<JCheckBox>();
		

		String[] labels = new String[7];
		boolean[] states = new boolean[7];
		labels[0]="Length"; states[0]=(systemMeasurements&LENGTH)!=0;
		labels[1]="Distance between ends"; states[1]=(systemMeasurements&DIST_ENDS)!=0;
		labels[2]="Mean intensity"; states[2]=(systemMeasurements&MEAN)!=0;
		labels[3]="SD of intensity"; states[3]=(systemMeasurements&STD_DEV)!=0;
		labels[4]="Straightness"; states[4]=(systemMeasurements&STRAIGHTNESS)!=0;
		labels[5]="Ends coordinates"; states[5]=(systemMeasurements&ENDS_COORDS)!=0;
		labels[6]="End-end direction"; states[6]=(systemMeasurements&ENDS_DIR)!=0;
		
		for(int i = 0;i<list.length;i++)
		{

			measureStates.add(new JCheckBox(labels[i]));
			measureStates.get(i).setSelected(states[i]);
			pMeasureSettings.add(measureStates.get(i));
			
		}
		
		String[] sIntInterpolationType = { "Nearest Neighbor", "Linear", "Lanczos" };
		JComboBox<String> intensityInterpolationList = new JComboBox<String>(sIntInterpolationType);
		pMeasureSettings.add(new JLabel(" "));
		pMeasureSettings.add(new JLabel("Intensity interpolation: "));
		intensityInterpolationList.setSelectedIndex(intensityInterpolation);
		pMeasureSettings.add(intensityInterpolationList);
		
		String[] sShapeInterpolationType = { "Voxel", "Subvoxel"};
		JComboBox<String> shapeInterpolationList = new JComboBox<String>(sShapeInterpolationType);
		shapeInterpolationList.setSelectedIndex(shapeInterpolation);
		pMeasureSettings.add(new JLabel("Roi Shape interpolation: "));
		pMeasureSettings.add(shapeInterpolationList);
		
		int reply = JOptionPane.showConfirmDialog(null, pMeasureSettings, "Set Measurements", 
		        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (reply == JOptionPane.OK_OPTION) 
		{

			//boolean b = false;
			for (int i=0; i<list.length; i++) {
				states[i] = measureStates.get(i).isSelected();
				if (states[i])
					systemMeasurements |= list[i];
				else
					systemMeasurements &= ~list[i];
			}
			Prefs.set("BigTrace.Measurements", systemMeasurements);
			
			//intensity interpolation
			intensityInterpolation=intensityInterpolationList.getSelectedIndex();
			Prefs.set("BigTrace.IntInterpolation",intensityInterpolation);
			setInterpolationFactory();
			
			//Roi shape interpolation
			shapeInterpolation= shapeInterpolationList.getSelectedIndex();
			Prefs.set("BigTrace.ShapeInterpolation",shapeInterpolation);
			if (rt!=null)
			{	
				boolean bUpdate=false;
				for (int i=0; i<list.length; i++) 
				{
					//column not selected
					//remove it from results table
					if(!states[i])
					{
						String sColName;
						for (int nCol=0;nCol<=rt.getLastColumn();nCol++)
						{
							sColName = rt.getColumnHeading(nCol);
							if(sColName.startsWith(colTemplates[i]))
							{
								rt.deleteColumn(sColName);
								bUpdate=true;
							}
						}
						
					}
				}
				if(bUpdate)
				{
					rt.updateResults();
				}
			}
		}
	
	}
	
	MeasureValues measureRoi(final Roi3D roi)
	{

		MeasureValues val = new MeasureValues();
		val.setRoiName(roi.getName());
		val.setRoiType(roi.getType());
		if(systemMeasurements>0)
		{
			if (roi!=null)//should not be, but just in case
			{
				if((systemMeasurements&LENGTH)!=0) 
				{
					measureLength(roi,val);
				}
				if((systemMeasurements&DIST_ENDS)!=0) 
				{
					measureEndsDistance(roi,val);
				}
				if((systemMeasurements&MEAN)!=0) 
				{
					measureMeanIntensity(roi,val);
				}
				if((systemMeasurements&STD_DEV)!=0) 
				{
					measureSDIntensity(roi,val);
				}
				if((systemMeasurements&STRAIGHTNESS)!=0) 
				{
					measureStraightness(roi,val);
				}
				if((systemMeasurements&ENDS_COORDS)!=0) 
				{
					measureEndsCoords(roi,val);
				}
				if((systemMeasurements&ENDS_DIR)!=0) 
				{
					measureEndsDirection(roi,val);
				}

			}
			
			//update Results Table
			//updateTable(val);
		}
		return val;
		
	}
	void measureAll()
	{
		ArrayList<MeasureValues> vals = new ArrayList<MeasureValues>();
		for(int i = 0; i<bt.roiManager.rois.size();i++)
		{
			vals.add(measureRoi(bt.roiManager.rois.get(i)));
		}
		resetTable(vals);
	}
	
	void updateTable(final MeasureValues val, final boolean bShow)
	{
		rt.incrementCounter();
		int row = rt.getCounter()-1;
		rt.setValue("ROI_Name", row, val.getRoiName());
		rt.setValue("ROI_Type", row, Roi3D.intTypeToString(val.getRoiType()));
		if ((systemMeasurements&LENGTH)!=0)
		{
			rt.setValue("Length", row, val.length);
		}
		if ((systemMeasurements&DIST_ENDS)!=0)
		{
			rt.setValue("Distance_between_ends", row, val.endsDistance);
		}
		if ((systemMeasurements&MEAN)!=0)
		{
			rt.setValue("Mean_intensity", row, val.mean);
		}
		if ((systemMeasurements&STD_DEV)!=0)
		{
			rt.setValue("SD_intensity", row, val.stdDev);
		}
		if ((systemMeasurements&STRAIGHTNESS)!=0)
		{
			rt.setValue("Straightness", row, val.straightness);
		}
		if ((systemMeasurements&ENDS_COORDS)!=0)
		{
			for(int nEnd = 0;nEnd<2;nEnd++)
			{
				rt.setValue("End_"+Integer.toString(nEnd+1)+"_X", row, val.ends[nEnd].getDoublePosition(0));
				rt.setValue("End_"+Integer.toString(nEnd+1)+"_Y", row, val.ends[nEnd].getDoublePosition(1));
				rt.setValue("End_"+Integer.toString(nEnd+1)+"_Z", row, val.ends[nEnd].getDoublePosition(2));
			}
		}

		if ((systemMeasurements&ENDS_DIR)!=0)
		{

			rt.setValue("Direction_X", row, val.direction.getDoublePosition(0));
			rt.setValue("Direction_Y", row, val.direction.getDoublePosition(1));
			rt.setValue("Direction_Z", row, val.direction.getDoublePosition(2));

		}
		
		if(bShow)		{
			rt.show("Results");
			rt.updateResults();
		}
	}
	void resetTable(final ArrayList<MeasureValues> vals)
	{
		rt.reset();
		for (int i = 0;i<vals.size();i++)
		{
			updateTable(vals.get(i), false);
		}
		rt.show("Results");
		rt.updateResults();
	}
	void measureLength(final Roi3D roi, final MeasureValues val)
	{
		switch (roi.getType())
		{
			case Roi3D.POINT:
				val.length=0.0;
				break;
			case Roi3D.POLYLINE:
				val.length = ((PolyLine3D)roi).getLength(shapeInterpolation, bt.btdata.globCal);
				break;
			case Roi3D.LINE_TRACE:
				val.length = ((LineTrace3D)roi).getLength(bt.btdata.globCal);
				break;			
			default:
				val.length = 0.0;
		}
			
		
	}
	void measureEndsDistance(final Roi3D roi, final MeasureValues val)
	{
		switch (roi.getType())
		{
			case Roi3D.POINT:
				val.endsDistance = Double.NaN;
				break;
		case Roi3D.POLYLINE:
				val.endsDistance = ((PolyLine3D)roi).getEndsDistance(bt.btdata.globCal);
				break;
			case Roi3D.LINE_TRACE:
				val.endsDistance = ((LineTrace3D)roi).getEndsDistance( bt.btdata.globCal);
				break;			
			default:
				val.endsDistance = Double.NaN;
		}
	}
	
	void measureMeanIntensity(final Roi3D roi, final MeasureValues val)
	{
		IntervalView< T > source =(IntervalView<T>) bt.sources.get(bt.btdata.nChAnalysis);
		double [][] li_profile;
		val.mean = Double.NaN;
		switch (roi.getType())
		{
		
			case Roi3D.POINT:
				val.mean = ((Point3D)roi).getMeanIntensity(source, nInterpolatorFactory);
				break;
			case Roi3D.POLYLINE:
				
				li_profile = ((PolyLine3D)roi).getIntensityProfile(source, bt.btdata.globCal, nInterpolatorFactory, shapeInterpolation);
				if (li_profile!=null)
				{
					val.mean= getMeanDoubleArray(li_profile[1]);
				}
				break;
				
			case Roi3D.LINE_TRACE:				
				
				li_profile = ((LineTrace3D)roi).getIntensityProfile(source, bt.btdata.globCal, nInterpolatorFactory, shapeInterpolation);
				if(li_profile!=null)
				{
					val.mean= getMeanDoubleArray(li_profile[1]);
				}
				break;			
		}
	}
	
	void measureSDIntensity(final Roi3D roi, final MeasureValues val)
	{
		IntervalView< T > source =(IntervalView<T>) bt.sources.get(bt.btdata.nChAnalysis);
		double [][] li_profile;
		val.stdDev = Double.NaN;
		switch (roi.getType())
		{
		
			case Roi3D.POINT:
				val.stdDev = 0.0;
				break;
			case Roi3D.POLYLINE:
				
				li_profile = ((PolyLine3D)roi).getIntensityProfile(source, bt.btdata.globCal, nInterpolatorFactory, shapeInterpolation);
				if (li_profile!=null)
				{
					if((systemMeasurements&MEAN)!=0) 
					{
						val.stdDev= getSDDoubleArray(val.mean,li_profile[1]);
					}
					else
					{
						val.stdDev= getSDDoubleArray(getMeanDoubleArray(li_profile[1]),li_profile[1]);
					}
				}
				break;
				
			case Roi3D.LINE_TRACE:				
				
				li_profile = ((LineTrace3D)roi).getIntensityProfile(source, bt.btdata.globCal, nInterpolatorFactory, shapeInterpolation);
				if (li_profile!=null)
				{
					if((systemMeasurements&MEAN)!=0) 
					{
						val.stdDev= getSDDoubleArray(val.mean,li_profile[1]);
					}
					else
					{
						val.stdDev= getSDDoubleArray(getMeanDoubleArray(li_profile[1]),li_profile[1]);
					}
				}
				break;			
		}
	}
	void measureStraightness(final Roi3D roi, final MeasureValues val)
	{
		switch (roi.getType())
		{
			case Roi3D.POINT:
				val.straightness = Double.NaN;
				break;
			case Roi3D.POLYLINE:
				val.straightness = ((PolyLine3D)roi).getEndsDistance(bt.btdata.globCal)/((PolyLine3D)roi).getLength(shapeInterpolation,bt.btdata.globCal);
				break;
			case Roi3D.LINE_TRACE:
				val.straightness = ((LineTrace3D)roi).getEndsDistance( bt.btdata.globCal)/((LineTrace3D)roi).getLength(bt.btdata.globCal);
				break;			
			default:
				val.straightness = Double.NaN;
		}
	}
	void measureEndsCoords(final Roi3D roi, final MeasureValues val)
	{
		switch (roi.getType())
		{
			case Roi3D.POINT:
				((Point3D)roi).getEnds(val, bt.btdata.globCal);
				break;
			case Roi3D.POLYLINE:
				((PolyLine3D)roi).getEnds(val, bt.btdata.globCal);
				break;
			case Roi3D.LINE_TRACE:
				((LineTrace3D)roi).getEnds(val, bt.btdata.globCal);
				break;			
		}
			
		
	}
	void measureEndsDirection(final Roi3D roi, final MeasureValues val)
	{
		switch (roi.getType())
		{
			case Roi3D.POINT:
				val.direction = Roi3D.getNaNPoint();
				break;
			case Roi3D.POLYLINE:
				((PolyLine3D)roi).getEndsDirection(val, bt.btdata.globCal);
				break;
			case Roi3D.LINE_TRACE:
				((LineTrace3D)roi).getEndsDirection(val, bt.btdata.globCal);
				break;			
		}
			
		
	}
	
	void measureLineProfile(final Roi3D roi)
	{
		IntervalView< T > source =(IntervalView<T>) bt.sources.get(bt.btdata.nChAnalysis);
		double [][] li_profile;
		Plot plotProfile;
		switch (roi.getType())
		{
		
			case Roi3D.POINT:
				//val.direction = Roi3D.getNaNPoint();
				break;
			case Roi3D.POLYLINE:
				
				li_profile = ((PolyLine3D)roi).getIntensityProfile(source, bt.btdata.globCal, nInterpolatorFactory, shapeInterpolation);
				if (li_profile!=null)
				{
					plotProfile = new Plot("Profile ROI "+roi.getName(),"Distance along line ("+bt.btdata.sVoxelUnit+")","Intensity");
					plotProfile.addPoints(li_profile[0],li_profile[1], Plot.LINE);
					plotProfile.show();
				}
				break;
				
			case Roi3D.LINE_TRACE:				
				
				li_profile = ((LineTrace3D)roi).getIntensityProfile(source, bt.btdata.globCal, nInterpolatorFactory, shapeInterpolation);
				if(li_profile!=null)
				{
					plotProfile = new Plot("Profile ROI "+roi.getName(),"Distance along line ("+bt.btdata.sVoxelUnit+")","Intensity");
					plotProfile.addPoints(li_profile[0],li_profile[1], Plot.LINE);
					plotProfile.show();
				}
				break;			
		}
	}
	
	public void setInterpolationFactory()
	{
		switch (intensityInterpolation)
		{
			case INT_NearestNeighbor:
				nInterpolatorFactory = new NearestNeighborInterpolatorFactory<T>();
				break;
			case INT_NLinear:
				nInterpolatorFactory = new NLinearInterpolatorFactory<T>();
				break;
			case INT_Lanczos:
				break;
			default:
				nInterpolatorFactory = new LanczosInterpolatorFactory<T>();
				break;
				
		}
	}
	
	public static double getMeanDoubleArray(final double [] values)
	{
		double out = 0.0;
		for(int i=0;i<values.length;i++)
		{
			out+=values[i];
		}
		out /=values.length;
		return out;
	}
	
	public static double getSDDoubleArray(final double mean, final double [] values)
	{
		double out = 0.0;
		for(int i=0;i<values.length;i++)
		{
			out+=(values[i]-mean)*(values[i]-mean);
		}
		out =Math.sqrt(out/(values.length-1));
		return out;
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		
		// TODO Auto-generated method stub
		if (e.getValueIsAdjusting() == false) 
		{
			bt.roiManager.jlist.setSelectedIndex(jlist.getSelectedIndex());
        }
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
		// MEASUREMENT CHANNEL
		if(e.getSource() == cbActiveChannel)
		{
			bt.btdata.nChAnalysis=cbActiveChannel.getSelectedIndex();
			bt.roiManager.cbActiveChannel.setSelectedIndex(bt.btdata.nChAnalysis);
		}
		
		//LineProfile
		if(e.getSource() == butLineProfile)
		{
			if (jlist.getSelectedIndex()>-1)
			{
				measureLineProfile(bt.roiManager.rois.get(jlist.getSelectedIndex()));
			}
		}
		//Measure one ROI
		if(e.getSource() == butMeasure)
		{
			if (jlist.getSelectedIndex()>-1)
			{
				if(systemMeasurements>0)
				{
					updateTable(measureRoi(bt.roiManager.rois.get(jlist.getSelectedIndex())), true);
				}
			}
		}
		//Measure all
		if(e.getSource() == butMeasureAll)
		{
			measureAll();
		}
		//SETTINGS
		if(e.getSource() == butSettings)
		{
			dialSettings();
		}
	}

}

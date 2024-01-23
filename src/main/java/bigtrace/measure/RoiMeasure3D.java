package bigtrace.measure;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import javax.swing.BoxLayout;
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
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.gui.GBCHelper;
import bigtrace.gui.NumberField;
import bigtrace.gui.PanelTitle;
import bigtrace.rois.AbstractCurve3D;
import bigtrace.rois.CrossSection3D;
import bigtrace.rois.Point3D;
import bigtrace.rois.Roi3D;
import bigtrace.volume.ExtractROIBox;
import bigtrace.volume.SplitVolumePlane;
import bigtrace.volume.StraightenCurve;
import ij.IJ;
import ij.Prefs;
import ij.gui.Plot;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;

public class RoiMeasure3D < T extends RealType< T > > extends JPanel implements ListSelectionListener, ActionListener, Measurements { 
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4635723145578489755L;
	final BigTrace<T> bt;
	final BigTraceData<T> btdata;
	
	JButton butLineProfile;
	JButton butLineAlignment;
	JToggleButton butMeasureFile;
	JButton butExtractBox;
	JButton butStraighten;
	JButton butSlice;
	JButton butSettings;
	JButton butMeasure;
	JButton butMeasureAll;
	public JList<String> jlist;
	JScrollPane listScroller;
	
	public JComboBox<String> cbActiveChannel;
	// Order must agree with order of checkboxes in Set Measurements dialog box
	private static final int[] list = { LENGTH,  DIST_ENDS, MEAN, STD_DEV, STRAIGHTNESS, ENDS_COORDS, ENDS_DIR};
	private static final String[] colTemplates = { "Length", "Distance_between_ends", "Mean_intensity", "SD_intensity", "Straightness", "End_","Direction_"};
	

	private static int systemMeasurements = (int)Prefs.get("BigTrace.Measurements",LENGTH+MEAN);


	private double [] coalignVector;
	private boolean bAlignCosine = Prefs.get("BigTrace.bAlignCosine", true);
	
	private static ResultsTable systemRT = new ResultsTable();
	private ResultsTable rt;
	

	public RoiMeasure3D(BigTrace<T> bt)
	{
		this.bt = bt;
		this.btdata = bt.btdata;
		int nButtonSize = 40;
			
		coalignVector = new double [3];
		
		for(int d=0;d<2;d++)
		{
			coalignVector[d]  = Prefs.get("BigTrace.coalignVec"+Integer.toString(d),0.0);
		}
		coalignVector[2]  = Prefs.get("BigTrace.coalignVec2",1.0);
		
		rt = systemRT;
		
		btdata.setInterpolationFactory();
		JPanel panLineTools = new JPanel(new GridBagLayout());  
		panLineTools.setBorder(new PanelTitle(" Tools "));

		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/line_profile.png");
		ImageIcon tabIcon = new ImageIcon(icon_path);
		butLineProfile = new JButton(tabIcon);
		butLineProfile.setToolTipText("Plot Intensity Profile");
		butLineProfile.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));

		icon_path = bigtrace.BigTrace.class.getResource("/icons/line_align.png");
		tabIcon = new ImageIcon(icon_path);
		butLineAlignment = new JButton(tabIcon);
		butLineAlignment.setToolTipText("Curve Coalignment");
		butLineAlignment.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));

		icon_path = bigtrace.BigTrace.class.getResource("/icons/make_plot.png");
		tabIcon = new ImageIcon(icon_path);
		butMeasureFile = new JToggleButton(tabIcon);
		butMeasureFile.setToolTipText("Make plot, press for file export");
		butMeasureFile.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		

		icon_path = bigtrace.BigTrace.class.getResource("/icons/pipe.png");
		tabIcon = new ImageIcon(icon_path);
		butStraighten = new JButton(tabIcon);
		butStraighten.setToolTipText("Straighten");
		butStraighten.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/extract_box.png");
		tabIcon = new ImageIcon(icon_path);
		butExtractBox = new JButton(tabIcon);
		butExtractBox.setToolTipText("Extract ROI Box");
		butExtractBox.setPreferredSize(new Dimension(nButtonSize, nButtonSize));

		icon_path = bigtrace.BigTrace.class.getResource("/icons/slice_volume.png");
		tabIcon = new ImageIcon(icon_path);
		butSlice = new JButton(tabIcon);
		butSlice.setToolTipText("Split volume");
		butSlice.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		


		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		tabIcon = new ImageIcon(icon_path);
		butSettings = new JButton(tabIcon);
		butSettings.setToolTipText("Settings");
		butSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));

		butLineProfile.addActionListener(this);
		butLineAlignment.addActionListener(this);
		butMeasureFile.addActionListener(this);
		butStraighten.addActionListener(this);
		butExtractBox.addActionListener(this);
		butSlice.addActionListener(this);
		butSettings.addActionListener(this);

		//this.setBorder(new PanelTitle(" Measurements "));

		//add to the panel
		GridBagConstraints cr = new GridBagConstraints();
		cr.gridx=0;
		cr.gridy=0;
		panLineTools.add(butLineProfile,cr);
		cr.gridx++;
		panLineTools.add(butLineAlignment,cr);
		cr.gridx++;
		panLineTools.add(butMeasureFile,cr);
		cr.gridx++;
		JSeparator sp = new JSeparator(SwingConstants.VERTICAL);
		sp.setPreferredSize(new Dimension((int) (nButtonSize*0.5),nButtonSize));
		panLineTools.add(sp,cr);
		
		cr.gridx++;
		panLineTools.add(butExtractBox,cr);
		cr.gridx++;
		panLineTools.add(butStraighten,cr);

		cr.gridx++;
		panLineTools.add(butSlice,cr);
		
		//filler
		cr.gridx++;
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
		listScroller.setPreferredSize(new Dimension(300, 400));
		listScroller.setMinimumSize(new Dimension(170, 250));
		
		 jlist.addMouseListener(new MouseAdapter() {
			    public void mouseClicked(MouseEvent evt) {
			        if (evt.getClickCount() == 2) {
			            // Double-click detected
			            int index = jlist.locationToIndex(evt.getPoint());
			          bt.roiManager.focusOnRoi(bt.roiManager.rois.get(index));
			        } 
			    }
			});

		//this.add(listScroller);	
		cr.gridx=0;
		cr.gridy=0;
		cr.gridheight=GridBagConstraints.REMAINDER;
		
		cr.fill  = GridBagConstraints.BOTH;
		cr.weightx=0.99;
		cr.weighty=0.99;
		panMeasure.add(listScroller,cr);
		cr.weightx=0.0;
		cr.weighty=0.0;
		cr.fill = GridBagConstraints.NONE;
		
		
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
		cr.weighty = 0.99;
		cr.fill = GridBagConstraints.BOTH;
		add(panMeasure,cr);
		cr.weighty = 0.0;
		cr.fill = GridBagConstraints.HORIZONTAL;
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
		intensityInterpolationList.setSelectedIndex(BigTraceData.intensityInterpolation);
		pMeasureSettings.add(intensityInterpolationList);
		
		String[] sShapeInterpolationType = { "Voxel", "Smooth", "Spline"};
		JComboBox<String> shapeInterpolationList = new JComboBox<String>(sShapeInterpolationType);
		shapeInterpolationList.setSelectedIndex(BigTraceData.shapeInterpolation);
		pMeasureSettings.add(new JLabel("ROI Shape interpolation: "));
		pMeasureSettings.add(shapeInterpolationList);
		
		NumberField nfSmoothWindow = new NumberField(2);
		nfSmoothWindow.setIntegersOnly(true);
		nfSmoothWindow.setText(Integer.toString(BigTraceData.nSmoothWindow));
		pMeasureSettings.add(new JLabel("Smoothing window/spline points (points): "));
		pMeasureSettings.add(nfSmoothWindow);	
		
		String[] sRotationFrame = { "Wang et al 2008", "Experimental"};
		JComboBox<String> rotationFrameList = new JComboBox<String>(sRotationFrame);
		rotationFrameList.setSelectedIndex(BigTraceData.rotationMinFrame);
		pMeasureSettings.add(new JLabel("Rotation minimizing frame: "));
		pMeasureSettings.add(rotationFrameList);
		
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
			BigTraceData.intensityInterpolation = intensityInterpolationList.getSelectedIndex();
			Prefs.set("BigTrace.IntInterpolation",BigTraceData.intensityInterpolation);
			btdata.setInterpolationFactory();
			
			
			if(BigTraceData.nSmoothWindow != Integer.parseInt(nfSmoothWindow.getText())||
					BigTraceData.shapeInterpolation!= shapeInterpolationList.getSelectedIndex()||
							BigTraceData.rotationMinFrame!=rotationFrameList.getSelectedIndex())
			{
				BigTraceData.nSmoothWindow = Integer.parseInt(nfSmoothWindow.getText());
				Prefs.set("BigTrace.nSmoothWindow", BigTraceData.nSmoothWindow);
				BigTraceData.shapeInterpolation= shapeInterpolationList.getSelectedIndex();
				Prefs.set("BigTrace.ShapeInterpolation",BigTraceData.shapeInterpolation);
				BigTraceData.rotationMinFrame= rotationFrameList.getSelectedIndex();
				Prefs.set("BigTrace.RotationMinFrame",BigTraceData.rotationMinFrame);
				bt.roiManager.updateROIsDisplay();
			}
			
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
	
	public boolean dialCoalignmentVector()
	{
		JPanel pCoalignVector = new JPanel(new GridLayout(0,2,6,0));
		
		ArrayList<NumberField> nfCoalignVector = new ArrayList<NumberField>();
		int d;
		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.000", decimalFormatSymbols);
		for(d=0;d<3;d++)
		{
			nfCoalignVector.add( new NumberField(4));
			nfCoalignVector.get(d).setText(df.format(coalignVector[d]));
		}
		pCoalignVector.add(new JLabel("X coord: "));
		pCoalignVector.add(nfCoalignVector.get(0));
		pCoalignVector.add(new JLabel("Y coord: "));
		pCoalignVector.add(nfCoalignVector.get(1));
		pCoalignVector.add(new JLabel("Z coord: "));
		pCoalignVector.add(nfCoalignVector.get(2));
		
		int reply = JOptionPane.showConfirmDialog(null, pCoalignVector, "Coalignment vector coordinates", 
		        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			double [] inVector = new double[3];
			for(d=0;d<3;d++)
				inVector[d]=Double.parseDouble(nfCoalignVector.get(d).getText());
			double len = LinAlgHelpers.length(inVector);
			if(len<0.000001)
			{
				IJ.error("Vector length should be more than zero!");
			}
			else
			{
				LinAlgHelpers.normalize(inVector);
				for(d=0;d<3;d++)
				{
					coalignVector[d] =inVector[d];
					Prefs.set("BigTrace.coalignVec"+Integer.toString(d), coalignVector[d]);				
				}
			}
			return true;
		}
		return false;
		
	}
	MeasureValues measureRoi(final Roi3D roi)
	{

		MeasureValues val = new MeasureValues();
		val.setRoiName(roi.getName());
		val.setRoiType(roi.getType());
		val.setRoiGroupName(bt.roiManager.getGroupName(roi));
		val.setTimePoint(roi.getTimePoint());
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
	void measureROIs(final ArrayList<Roi3D> rois, final boolean resetTable)
	{
	
		if(rois.size()>0)
		{

			ROIsMeasureBG measureBG = new ROIsMeasureBG();		
			measureBG.rois = rois;
			measureBG.bt=bt;
			measureBG.resetTable = resetTable;
			measureBG.addPropertyChangeListener(bt.btpanel);
			measureBG.execute();
			
		}
	}
	
	void measureAllProfiles()
	{
		String filename;
		
		filename = bt.btdata.sFileNameFullImg + "_int_profiles";
		SaveDialog sd = new SaveDialog("Save ROI Plot Profiles ", filename, ".csv");
        String path = sd.getDirectory();
        if (path==null)
        	return;
        filename = path+sd.getFileName();
        LineProfileBG profileBG = new LineProfileBG();
        profileBG.bt = bt;
        profileBG.sFilename = filename;
        profileBG.addPropertyChangeListener(bt.btpanel);
        profileBG.execute();
        
	}
	
	void measureAllCoalignment()
	{
		String filename;
		int j,k;
		
		filename = bt.btdata.sFileNameFullImg + "_coalign";
		SaveDialog sd = new SaveDialog("Save ROI Plot Profiles ", filename, ".csv");
        String path = sd.getDirectory();
        if (path==null)
        	return;
        filename = path+sd.getFileName();
        bt.roiManager.setLockMode(true);
        bt.bInputLock = true;
        try 
        {
			final File file = new File(filename);
			
			final FileWriter writer = new FileWriter(file);
			double [][] profile;
			String sPrefix;
			String out;
			Roi3D roi;
			final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setDecimalSeparator('.');
			final DecimalFormat df3 = new DecimalFormat ("#.###", symbols);
			
			if(bAlignCosine)
			{
				writer.write("ROI_Name,ROI_Type,ROI_Group,ROI_TimePoint,Length,Cos(Angle_with_"+df3.format(coalignVector[0])+"_"+df3.format(coalignVector[1])+"_"+df3.format(coalignVector[2])+"),X_coord,Y_coord,Z_coord\n");
			}
			else
			{
				writer.write("ROI_Name,ROI_Type,ROI_Group,ROI_TimePoint,Length,Angle_with_"+df3.format(coalignVector[0])+"_"+df3.format(coalignVector[1])+"_"+df3.format(coalignVector[2])+"(Rad),X_coord,Y_coord,Z_coord\n");
			}
			for(int i = 0; i<bt.roiManager.rois.size();i++)
			{
				roi = bt.roiManager.rois.get(i);
				sPrefix = roi.getName() + ","+Roi3D.intTypeToString(roi.getType())+","+bt.roiManager.getGroupName(roi)+","+Integer.toString(roi.getTimePoint());
				profile=measureLineCoalignment(roi, false);
				if(profile!=null)
				{
					for(j=0;j<profile[0].length;j++)
					{
						out="".concat(sPrefix);
						
						for(k=0;k<5;k++)
						{
							out = out.concat(","+df3.format(profile[k][j]));
						}
						out = out.concat("\n");
						writer.write(out);
					}
				}
			}
			writer.close();
    	
        } catch (IOException e) {	
			IJ.log(e.getMessage());
			//e.printStackTrace();
		}
        bt.roiManager.setLockMode(false);
        bt.bInputLock = false;
        bt.btpanel.progressBar.setValue(100);
        bt.btpanel.progressBar.setString("Measured and saved coalignment angles of "+Integer.toString(bt.roiManager.rois.size())+" ROIs");
	}
	void updateTable(final MeasureValues val, final boolean bShow)
	{
		rt.incrementCounter();
		int row = rt.getCounter()-1;
		rt.setValue("ROI_Name", row, val.getRoiName());
		rt.setValue("ROI_Type", row, Roi3D.intTypeToString(val.getRoiType()));
		rt.setValue("ROI_Group", row, val.getRoiGroupName());
		if(BigTraceData.nNumTimepoints>1)
		{
			rt.setValue("ROI_TimePoint", row, val.getTimePoint());
		}
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
			case Roi3D.LINE_TRACE:
				val.length = ((AbstractCurve3D)roi).getLength(BigTraceData.shapeInterpolation, BigTraceData.globCal);
				break;		
			default:
				val.length = Double.NaN;
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
			case Roi3D.LINE_TRACE:
				val.endsDistance = ((AbstractCurve3D)roi).getEndsDistance(BigTraceData.globCal);
				break;			
			default:
				val.endsDistance = Double.NaN;
		}
	}
	
	void measureMeanIntensity(final Roi3D roi, final MeasureValues val)
	{
		//IntervalView< T > source =(IntervalView<T>) bt.sources.get(bt.btdata.nChAnalysis);
		IntervalView< T > source =(IntervalView<T>) bt.btdata.getDataSourceClipped(bt.btdata.nChAnalysis, roi.getTimePoint());
		double [][] li_profile;
		val.mean = Double.NaN;
		switch (roi.getType())
		{
		
			case Roi3D.POINT:
				val.intensity_values = ((Point3D)roi).getIntensityValues(source, btdata.nInterpolatorFactory);
				//val.intensity_values = ((Point3D)roi).getIntensityValuesTEST(bt,source, nInterpolatorFactory);
				if(val.intensity_values != null)
				{
					val.mean = getMeanDoubleArray(val.intensity_values);
				}
				break;
			case Roi3D.POLYLINE:
			case Roi3D.LINE_TRACE:
				
				//li_profile = ((AbstractCurve3D)roi).getIntensityProfile(source, BigTraceData.globCal, nInterpolatorFactory, BigTraceData.shapeInterpolation);
				li_profile = ((AbstractCurve3D)roi).getIntensityProfilePipe(source, BigTraceData.globCal, (int) Math.floor(0.5*roi.getLineThickness()),btdata.nInterpolatorFactory, BigTraceData.shapeInterpolation);

				if (li_profile!=null)
				{
					val.mean= getMeanDoubleArray(li_profile[1]);
					val.intensity_values = li_profile[1].clone();
				}
				
				break;	
			default:
				val.mean = Double.NaN;
		}
	}
	
	void measureSDIntensity(final Roi3D roi, final MeasureValues val)
	{
		//IntervalView< T > source =(IntervalView<T>) bt.sources.get(bt.btdata.nChAnalysis);
		IntervalView< T > source =(IntervalView<T>) bt.btdata.getDataSourceClipped(bt.btdata.nChAnalysis, roi.getTimePoint());
		double [][] li_profile;
		val.stdDev = Double.NaN;
		switch (roi.getType())
		{
		
			case Roi3D.POINT:
				if(val.intensity_values == null)
				{
					val.intensity_values = ((Point3D)roi).getIntensityValues(source, btdata.nInterpolatorFactory);
					
				}
				if((systemMeasurements&MEAN)!=0) 
				{
					val.stdDev = getSDDoubleArray(val.mean,val.intensity_values);
				}
				else
				{
					val.stdDev= getSDDoubleArray(getMeanDoubleArray(val.intensity_values),val.intensity_values);
				}

				break;
			case Roi3D.POLYLINE:
			case Roi3D.LINE_TRACE:
				if(val.intensity_values==null)
				{
					li_profile = ((AbstractCurve3D)roi).getIntensityProfilePipe(source, BigTraceData.globCal, (int) Math.floor(0.5*roi.getLineThickness()),btdata.nInterpolatorFactory, BigTraceData.shapeInterpolation);
					if(li_profile!=null)
					{
						val.intensity_values = li_profile[1].clone();
					}
					
				}

				if (val.intensity_values!=null)
				{
					if((systemMeasurements&MEAN)!=0) 
					{
						val.stdDev= getSDDoubleArray(val.mean,val.intensity_values);
					}
					else
					{
						val.stdDev= getSDDoubleArray(getMeanDoubleArray(val.intensity_values),val.intensity_values);
					}
				}
				break;
				
			default:
				val.stdDev = Double.NaN;
				
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
			case Roi3D.LINE_TRACE:
				val.straightness = ((AbstractCurve3D)roi).getEndsDistance( BigTraceData.globCal)/((AbstractCurve3D)roi).getLength(BigTraceData.shapeInterpolation,BigTraceData.globCal);
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
				((Point3D)roi).getEnds(val, BigTraceData.globCal);
				break;
			case Roi3D.POLYLINE:
			case Roi3D.LINE_TRACE:
				((AbstractCurve3D)roi).getEnds(val, BigTraceData.globCal);
				break;	
			default:
				val.ends = new RealPoint [2];
				val.ends[0] =Roi3D.getNaNPoint();
				val.ends[1] =Roi3D.getNaNPoint();
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
			case Roi3D.LINE_TRACE:
				((AbstractCurve3D)roi).getEndsDirection(val, BigTraceData.globCal);
				break;	
			default:
				val.direction = Roi3D.getNaNPoint();
		}
			
		
	}
	
	double [][] measureLineProfile(final Roi3D roi, final boolean bMakePlot)
	{
		//IntervalView< T > source =(IntervalView<T>) bt.sources.get(bt.btdata.nChAnalysis);
		IntervalView< T > source =(IntervalView<T>) bt.btdata.getDataSourceClipped(bt.btdata.nChAnalysis, roi.getTimePoint());
		double [][] li_profile = null;
		Plot plotProfile;
		switch (roi.getType())
		{
		
			case Roi3D.POINT:
				//val.direction = Roi3D.getNaNPoint();
				break;
			case Roi3D.POLYLINE:
			case Roi3D.LINE_TRACE:				
				
				li_profile = ((AbstractCurve3D)roi).getIntensityProfilePipe(source, BigTraceData.globCal, (int) Math.floor(0.5*roi.getLineThickness()),btdata.nInterpolatorFactory, BigTraceData.shapeInterpolation);
				
				break;			
		}
		if (li_profile!=null && bMakePlot)
		{
			plotProfile = new Plot("Profile ROI "+bt.roiManager.getGroupPrefixRoiName(roi),"Distance along line ("+bt.btdata.sVoxelUnit+")","Intensity");
			plotProfile.addPoints(li_profile[0],li_profile[1], Plot.LINE);
			plotProfile.show();
		}
		return li_profile;
	}
	
	double [][] measureLineCoalignment(final Roi3D roi, final boolean bMakePlot)
	{
		
		double [][] li_profile = null;
		Plot plotProfile;
		String sUnit;
		
		if(bAlignCosine)
			sUnit = "cosine of angle";
		else
			sUnit="Angle (rad)";
		switch (roi.getType())
		{
		
			case Roi3D.POINT:
				//val.direction = Roi3D.getNaNPoint();
				break;
			case Roi3D.POLYLINE:
			case Roi3D.LINE_TRACE:				
				
				li_profile = ((AbstractCurve3D)roi).getCoalignmentProfile(coalignVector, BigTraceData.globCal, BigTraceData.shapeInterpolation, bAlignCosine);
				break;			
		}
		if (li_profile!=null && bMakePlot)
		{

			plotProfile = new Plot("Alignment angles ROI "+bt.roiManager.getGroupPrefixRoiName(roi),"Distance along line ("+bt.btdata.sVoxelUnit+")",sUnit);
			plotProfile.addPoints(li_profile[0],li_profile[1], Plot.LINE);
			plotProfile.show();
		}
		return li_profile;
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

	/** dialog for the box around ROI extraction**/
	public void extractROIBox(final Roi3D roiIn)
	{
		
		int nExtractBoxROIList;
		int nExtractRoiType;
		int nExpandROIBox;
		int nTimeRange;
		int nExtractBoxOutput;
		
		
		final JPanel extractROISettings = new JPanel();
		
		extractROISettings.setLayout(new GridBagLayout());
		
		GridBagConstraints cd = new GridBagConstraints();

		final NumberField nfBoxExpand = new NumberField(4);
		nfBoxExpand.setIntegersOnly(true);

		GBCHelper.alighLeft(cd);
		cd.gridx=0;
		cd.gridy=0;	
		extractROISettings.add(new JLabel("Extract box around:"),cd);
		cd.gridx++;
		final String[] sExtractBoxROIsRange = { "Selected ROI", "All visible ROIs" };
		JComboBox<String> extractBoxRoiList = new JComboBox<String>(sExtractBoxROIsRange);
		extractBoxRoiList.setSelectedIndex((int)Prefs.get("BigTrace.nExtractBoxROIList", 0));
		extractROISettings.add(extractBoxRoiList, cd);	
		
		cd.gridy++;
		cd.gridx=0;	
		extractROISettings.add(new JLabel("Box size:"),cd);
		cd.gridx++;
		String[] sExtractRoiType = { "Tight", "Enlarge/shrink below" };
		JComboBox<String> extractBoxTypeList = new JComboBox<String>(sExtractRoiType);
		extractBoxTypeList.setSelectedIndex((int)Prefs.get("BigTrace.nExtractRoiType", 0));
		extractROISettings.add(extractBoxTypeList,cd);
		
		nfBoxExpand.setText(Integer.toString((int)Prefs.get("BigTrace.nExpandROIBox", 5)));
		cd.gridy++;
		cd.gridx=0;
		extractROISettings.add(new JLabel("Expand box by (px):"),cd);
		cd.gridx++;
		extractROISettings.add(nfBoxExpand,cd);
		
		String[] sExtractBoxTime = { "Single ROI's time point", "All time points" };
		JComboBox<String> extractBoxTimeList = new JComboBox<String>(sExtractBoxTime);
		if(BigTraceData.nNumTimepoints>1)
		{
			cd.gridy++;
			cd.gridx=0;
			extractROISettings.add(new JLabel("Time range per ROI:"),cd);
			extractBoxTimeList.setSelectedIndex((int)Prefs.get("BigTrace.nExtractBoxTime", 0));
			cd.gridx++;
			extractROISettings.add(extractBoxTimeList,cd);
			
		}
		cd.gridy++;
		cd.gridx=0;	
		extractROISettings.add(new JLabel("Output:"),cd);
		cd.gridx++;
		String[] sExtractBoxOutput = { "show in ImageJ", "save as TIF" };
		JComboBox<String> extractBoxOutputList = new JComboBox<String>(sExtractBoxOutput);
		extractBoxOutputList.setSelectedIndex((int)Prefs.get("BigTrace.nExtractBoxOutput", 0));
		extractROISettings.add(extractBoxOutputList,cd);
		
		if(bt.bApplyLLSTransform)
		{
			cd.gridy++;
			cd.gridx=0;	
			extractROISettings.add(new JLabel("Intensity interpolation:"),cd);
			cd.gridx++;
			String[] sIntInterpolationType = { "Nearest Neighbor", "Linear", "Lanczos" };
			extractROISettings.add(new JLabel(sIntInterpolationType[BigTraceData.intensityInterpolation]),cd);
		}
		
		int reply = JOptionPane.showConfirmDialog(null, extractROISettings, "Extract box around ROI(s)", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			nExtractBoxROIList = extractBoxRoiList.getSelectedIndex();
			Prefs.set("BigTrace.nExtractBoxROIList", nExtractBoxROIList);
			
			nExtractRoiType = extractBoxTypeList.getSelectedIndex();
			Prefs.set("BigTrace.nExtractRoiType", nExtractRoiType);
			if(nExtractRoiType==0)
			{
				nExpandROIBox = 0;
			}
			else
			{
				nExpandROIBox = Integer.parseInt(nfBoxExpand.getText());
				Prefs.set("BigTrace.nExpandROIBox", nExpandROIBox);				
			}
			nTimeRange = 0;
			if(BigTraceData.nNumTimepoints>1)
			{
				nTimeRange = extractBoxTimeList.getSelectedIndex();
				Prefs.set("BigTrace.nExtractBoxTime", nTimeRange);
			}

			nExtractBoxOutput = extractBoxOutputList.getSelectedIndex();
			Prefs.set("BigTrace.nExtractBoxOutput", nExtractBoxOutput);
			//if saving, ask for the path
			String sSaveDir = "";
			if(nExtractBoxOutput > 0)
			{
				sSaveDir = IJ.getDirectory("Save straightened TIF to..");
				if(sSaveDir == null)
				{
					bt.btpanel.progressBar.setString("curve straightening aborted.");
					return;
				}
			}
			
			//build list of ROIs
			final ArrayList<Roi3D> roiOut = new ArrayList<Roi3D>();
			//single ROI
			if(nExtractBoxROIList == 0)
			{
		
				roiOut.add(roiIn);
			}
			//all curve ROIs
			else
			{
				for (int nRoi = 0; nRoi<bt.roiManager.rois.size(); nRoi++)
				{
					Roi3D roi = bt.roiManager.rois.get(nRoi);
					if(bt.roiManager.groups.get(roi.getGroupInd()).bVisible)
					{
						roiOut.add(roi);
					}
				}
				
			}
			if(roiOut.size()>0)
			{			
				//run in a separate thread
				ExtractROIBox<T> extractBoxBG = new ExtractROIBox<T>(roiOut, bt, nExpandROIBox, nTimeRange, nExtractBoxOutput, sSaveDir);				
				extractBoxBG.addPropertyChangeListener(bt.btpanel);
				extractBoxBG.execute();
			}
			else
			{
				IJ.log("Cannot find ROIs for box extraction.");
				bt.btpanel.progressBar.setString("extract ROI box aborted.");
			}

		}
	}
	
	/** dialog for the Straighten procedure **/
	public void straightenCurve(final AbstractCurve3D curveLine)
	{
		
		float fRadiusStraighted = 0.0f;
		int nRadiusType = 0;
		int nTimeRange = -1;
		int nROIList = 0;
		int nStraightenOutput = 0;
		
		NumberField nfRadius = new NumberField(4);
		
		
		JPanel straightenSettings = new JPanel();
		
		straightenSettings.setLayout(new GridBagLayout());
	
		GridBagConstraints cd = new GridBagConstraints();

		GBCHelper.alighLeft(cd);

		cd.gridx=0;
		cd.gridy=0;	
		straightenSettings.add(new JLabel("Straighten:"),cd);
		cd.gridx++;
		String[] sStraightenROIsRange = { "Selected ROI", "All visible ROIs" };
		JComboBox<String> straightenRoiList = new JComboBox<String>(sStraightenROIsRange);
		straightenRoiList.setSelectedIndex((int)Prefs.get("BigTrace.nStraightROIList", 0));
		straightenSettings.add(straightenRoiList,cd);	
	
		cd.gridy++;
		cd.gridx=0;	
		straightenSettings.add(new JLabel("Curve thickness:"),cd);
		cd.gridx++;
		String[] sStraightenType = { "Use ROI settings", "Override here" };
		JComboBox<String> straightenRadiusList = new JComboBox<String>(sStraightenType);
		straightenRadiusList.setSelectedIndex((int)Prefs.get("BigTrace.nRadiusType", 0));
		straightenSettings.add(straightenRadiusList,cd);
		
		
		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.00", decimalFormatSymbols);
		nfRadius.setText(df.format(Prefs.get("BigTrace.fRadiusStraighted", 5)));
		cd.gridy++;
		cd.gridx=0;
		straightenSettings.add(new JLabel("Override radius (px):"),cd);
		cd.gridx++;
		straightenSettings.add(nfRadius,cd);
		
		String[] sStraightenTime = { "Single ROI's time point", "All time points" };
		JComboBox<String> straightenTimeList = new JComboBox<String>(sStraightenTime);
		if(BigTraceData.nNumTimepoints>1)
		{
			cd.gridy++;
			cd.gridx=0;
			straightenSettings.add(new JLabel("Time range per ROI:"),cd);
			straightenTimeList.setSelectedIndex((int)Prefs.get("BigTrace.nStraightenTime", 0));
			cd.gridx++;
			straightenSettings.add(straightenTimeList,cd);
			
		}
		cd.gridy++;
		cd.gridx=0;	
		straightenSettings.add(new JLabel("Output:"),cd);
		cd.gridx++;
		String[] sStraightenOutput = { "show in ImageJ", "save as TIF" };
		JComboBox<String> straightenOutputList = new JComboBox<String>(sStraightenOutput);
		straightenOutputList.setSelectedIndex((int)Prefs.get("BigTrace.nStraightenOutput", 0));
		straightenSettings.add(straightenOutputList,cd);
		
		cd.gridy++;
		cd.gridx=0;	
		straightenSettings.add(new JLabel("ROI Shape:"),cd);
		cd.gridx++;
		String[] sShapeInterpolationType = { "Voxel", "Smooth", "Spline"};
		straightenSettings.add(new JLabel(sShapeInterpolationType[BigTraceData.shapeInterpolation]),cd);
		
		cd.gridy++;
		cd.gridx=0;	
		straightenSettings.add(new JLabel("Intensity interpolation:"),cd);
		cd.gridx++;
		String[] sIntInterpolationType = { "Nearest Neighbor", "Linear", "Lanczos" };
		straightenSettings.add(new JLabel(sIntInterpolationType[BigTraceData.intensityInterpolation]),cd);
		
		int reply = JOptionPane.showConfirmDialog(null, straightenSettings, "Straighten curve(s)", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			nROIList = straightenRoiList.getSelectedIndex();
			Prefs.set("BigTrace.nStraightROIList", nROIList);
			nRadiusType = straightenRadiusList.getSelectedIndex();
			Prefs.set("BigTrace.nRadiusType", nRadiusType);
			if(nRadiusType==0)
			{
				fRadiusStraighted = Math.round(0.5*curveLine.getLineThickness());
			}
			else
			{
				fRadiusStraighted = Float.parseFloat(nfRadius.getText());
				Prefs.set("BigTrace.fRadiusStraighted", fRadiusStraighted);				
			}
			nTimeRange = 0;
			if(BigTraceData.nNumTimepoints>1)
			{
				nTimeRange = straightenTimeList.getSelectedIndex();
				Prefs.set("BigTrace.nStraightenTime", nTimeRange);
			}

			nStraightenOutput = straightenOutputList.getSelectedIndex();
			Prefs.set("BigTrace.nStraightenOutput", nStraightenOutput);
			//if saving, ask for the path
			String sSaveDir = "";
			if(nStraightenOutput > 0)
			{
				sSaveDir = IJ.getDirectory("Save straightened TIF to..");
				if(sSaveDir == null)
				{
					bt.btpanel.progressBar.setString("curve straightening aborted.");
					return;
				}
			}
			
			//build list of ROIs
			final ArrayList<AbstractCurve3D> curvesOut = new ArrayList<AbstractCurve3D>();
			//single ROI
			if(nROIList == 0)
			{
				if(curveLine.vertices.size()<2)					
				{
					IJ.log("Curve ROI must have more then two vertices.");
					bt.btpanel.progressBar.setString("curve straightening aborted.");
					return;
				}
				curvesOut.add(curveLine);
			}
			//all curve ROIs
			else
			{
				for (int nRoi = 0; nRoi<bt.roiManager.rois.size(); nRoi++)
				{
					Roi3D roi = bt.roiManager.rois.get(nRoi);
					if(bt.roiManager.groups.get(roi.getGroupInd()).bVisible)
					{
						if((roi.getType() == Roi3D.LINE_TRACE) || (roi.getType() == Roi3D.POLYLINE))
						{
							curvesOut.add((AbstractCurve3D) roi);
						}
					}
				}
				
			}
			if(curvesOut.size()>0)
			{			
				//run in a separate thread
				StraightenCurve<T> straightBG = new StraightenCurve<T>(curvesOut, bt, fRadiusStraighted, nTimeRange, nStraightenOutput, sSaveDir);
				straightBG.addPropertyChangeListener(bt.btpanel);
				straightBG.execute();
			}
			else
			{
				IJ.log("Cannot find proper curve ROIs to straighten.");
				bt.btpanel.progressBar.setString("curve straightening aborted.");
			}

		}

	}
	
	/** given cross-section ROI, splits provided volume in two and shows them (dialog) **/
	public void sliceVolume(final CrossSection3D crossSection)
	{
		int nSliceType = 0;
		JPanel sliceSettings = new JPanel();
		sliceSettings.setLayout(new BoxLayout(sliceSettings, BoxLayout.PAGE_AXIS));
		
		String[] sSliceType = { "As original", "Tight crop" };
		JComboBox<String> sliceTypeList = new JComboBox<String>(sSliceType);
		
		sliceSettings.add(new JLabel("Output volumes size: "));
		sliceTypeList.setSelectedIndex((int)Prefs.get("BigTrace.nSliceType", 0));
		sliceSettings.add(sliceTypeList);
		
		int reply = JOptionPane.showConfirmDialog(null, sliceSettings, "Split/slice volume crop", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (reply == JOptionPane.OK_OPTION) 
		{
			nSliceType = sliceTypeList.getSelectedIndex();
			Prefs.set("BigTrace.nSliceType", nSliceType);
			//run in a separate thread
			SplitVolumePlane<T> splitBG = new SplitVolumePlane<T>(crossSection, bt, nSliceType);		
	        splitBG.addPropertyChangeListener(bt.btpanel);
	        splitBG.execute();
		}

	}
	
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		
		if (e.getValueIsAdjusting() == false) 
		{
			bt.roiManager.jlist.setSelectedIndex(jlist.getSelectedIndex());
        }
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		
		// MEASUREMENT CHANNEL
		if(e.getSource() == cbActiveChannel)
		{
			bt.btdata.nChAnalysis=cbActiveChannel.getSelectedIndex();
			bt.roiManager.cbActiveChannel.setSelectedIndex(bt.btdata.nChAnalysis);
		}
		
		//measurement mode for line profile and coalignment
		if(e.getSource() == butMeasureFile)
		{
			if(butMeasureFile.isSelected())
			{
				URL icon_path = bigtrace.BigTrace.class.getResource("/icons/file_export.png");
				ImageIcon tabIcon = new ImageIcon(icon_path);
				butMeasureFile.setIcon(tabIcon);
				butMeasureFile.setToolTipText("Export to file, press for plot");
			}
			else
			{
				URL icon_path = bigtrace.BigTrace.class.getResource("/icons/make_plot.png");
				ImageIcon tabIcon = new ImageIcon(icon_path);
				butMeasureFile.setIcon(tabIcon);
				butMeasureFile.setToolTipText("Make plot, press for file export");
			}
		}
		
		//LineProfile
		if(e.getSource() == butLineProfile && jlist.getModel().getSize()>0)
		{
			if(butMeasureFile.isSelected())
			{
				measureAllProfiles();
			}
			else
			{
				if (jlist.getSelectedIndex()>-1)
				{
					measureLineProfile(bt.roiManager.rois.get(jlist.getSelectedIndex()), true);
				}
			}
		}
		
		//Coalignment
		if(e.getSource() == butLineAlignment && jlist.getModel().getSize()>0)
		{
			if(butMeasureFile.isSelected())
			{
				if(dialCoalignmentVector())
				{
					measureAllCoalignment();
				}
			}
			else
			{
				if (jlist.getSelectedIndex()>-1)
				{
					
					if(dialCoalignmentVector())
					{
						measureLineCoalignment(bt.roiManager.rois.get(jlist.getSelectedIndex()),true);
					}
				}
			}
		}
		
		//Extract box around ROI
		if(e.getSource() == butExtractBox)
		{
			if (jlist.getSelectedIndex()>-1)
			{				
				extractROIBox(bt.roiManager.rois.get(jlist.getSelectedIndex()));
			}
		}
		
		//Straignten
		if(e.getSource() == butStraighten)
		{
			if (jlist.getSelectedIndex()>-1)
			{
					if(bt.roiManager.rois.get(jlist.getSelectedIndex()).getType()==Roi3D.LINE_TRACE||bt.roiManager.rois.get(jlist.getSelectedIndex()).getType()==Roi3D.POLYLINE)
					{
						straightenCurve((AbstractCurve3D)bt.roiManager.rois.get(jlist.getSelectedIndex()));
					}
			}
		}
		//Slice volume
		if(e.getSource() == butSlice)
		{
			if (jlist.getSelectedIndex()>-1)
			{
					if(bt.roiManager.rois.get(jlist.getSelectedIndex()).getType()==Roi3D.PLANE)
					{
						sliceVolume((CrossSection3D)bt.roiManager.rois.get(jlist.getSelectedIndex()));
					}
			}
		}
		//Measure one ROI
		if(e.getSource() == butMeasure)
		{
			if (jlist.getSelectedIndex()>-1)
			{
				if(systemMeasurements>0)
				{
					ArrayList<Roi3D> temp = new ArrayList<Roi3D>();
					temp.add(bt.roiManager.rois.get(jlist.getSelectedIndex()));
					measureROIs(temp, false);
				}
			}
		}
		//Measure all
		if(e.getSource() == butMeasureAll)
		{
			measureROIs(bt.roiManager.rois, true);
		}
		//SETTINGS
		if(e.getSource() == butSettings)
		{
			dialSettings();
		}
	}

}

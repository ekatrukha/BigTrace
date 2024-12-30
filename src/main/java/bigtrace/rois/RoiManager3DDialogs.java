package bigtrace.rois;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import bdv.tools.brightness.ColorIcon;
import bigtrace.BigTrace;
import bigtrace.BigTraceData;
import bigtrace.gui.GBCHelper;
import bigtrace.gui.NumberField;
import bigtrace.io.ROIsExportCSV;
import bigtrace.io.ROIsExportSWC;
import bigtrace.io.ROIsImportTrackMateBG;
import bigtrace.io.ROIsSaveBG;
import ij.Prefs;
import ij.io.OpenDialog;
import ij.io.SaveDialog;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class RoiManager3DDialogs < T extends RealType< T > & NativeType< T > > 
{	
	
	final BigTrace<T> bt;	

	public RoiManager3DDialogs(BigTrace<T> bt_)
	{
		bt = bt_;
	}
	
	public void dialSemiAutoProperties()
	{
		
		JTabbedPane tabPane = new JTabbedPane();
		GridBagConstraints cd = new GridBagConstraints();

		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.000", decimalFormatSymbols);
		
		////////////TRACING OPTIONS
		JPanel pTrace = new JPanel(new GridBagLayout());

		NumberField nfSigmaX = new NumberField(4);
		NumberField nfSigmaY = new NumberField(4);
		NumberField nfSigmaZ = new NumberField(4);
		JCheckBox cbTraceOnlyClipped = new JCheckBox();

		
		nfSigmaX.setText(df.format(bt.btData.sigmaTrace[0]));
		nfSigmaY.setText(df.format(bt.btData.sigmaTrace[1]));
		nfSigmaZ.setText(df.format(bt.btData.sigmaTrace[2]));

		cbTraceOnlyClipped.setSelected(bt.btData.bTraceOnlyClipped);
		
		cd.gridx=0;
		cd.gridy=0;

		pTrace.add(new JLabel("Curve thickness X axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaX,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pTrace.add(new JLabel("Curve thickness Y axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaY,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pTrace.add(new JLabel("Curve thickness Z axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaZ,cd);

		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Trace only clipped volume: "),cd);
		cd.gridx++;
		pTrace.add(cbTraceOnlyClipped,cd);
		
		////////////SEMI-AUTO TRACING OPTIONS
		JPanel pSemiAuto = new JPanel(new GridBagLayout());
		
		NumberField nfGammaTrace = new NumberField(4);
		NumberField nfTraceBoxSize = new NumberField(4);
		NumberField nfTraceBoxScreenFraction = new NumberField(4);
		NumberField nfTBAdvance = new NumberField(4);
		
		nfTraceBoxSize.setText(Integer.toString((int)(2.0*bt.btData.lTraceBoxSize)));
		nfTraceBoxScreenFraction.setText(df.format(bt.btData.dTraceBoxScreenFraction));
		nfGammaTrace.setText(df.format(bt.btData.gammaTrace));
		nfTBAdvance.setText(df.format(bt.btData.fTraceBoxAdvanceFraction));
			
		cd.gridx=0;
		cd.gridy=0;
		//cd.anchor=GridBagConstraints.WEST;
		pSemiAuto.add(new JLabel("Trace box size (px): "),cd);
		cd.gridx++;
		pSemiAuto.add(nfTraceBoxSize,cd);
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pSemiAuto.add(new JLabel("Trace box screen fraction (0-1): "),cd);
		cd.gridx++;
		pSemiAuto.add(nfTraceBoxScreenFraction,cd);
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pSemiAuto.add(new JLabel("Trace box advance [0-center..1-edge]: "),cd);
		cd.gridx++;
		pSemiAuto.add(nfTBAdvance,cd);	

		cd.gridx=0;		
		cd.gridy++;
		pSemiAuto.add(new JLabel("Orientation weight(0-1): "),cd);
		cd.gridx++;
		pSemiAuto.add(nfGammaTrace,cd);
	
		//assemble pane
		tabPane.addTab("Tracing",pTrace);
		tabPane.addTab("Semi auto",pSemiAuto);

		int reply = JOptionPane.showConfirmDialog(null, tabPane, "Semi-auto tracing settings", 
													JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (reply == JOptionPane.OK_OPTION) 
		{
				
			bt.btData.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", bt.btData.lTraceBoxSize);
			
			bt.btData.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", bt.btData.lTraceBoxSize);
		
			//TRACING OPTIONS
			
			bt.btData.sigmaTrace[0] = Double.parseDouble(nfSigmaX.getText());
			Prefs.set("BigTrace.sigmaTraceX", bt.btData.sigmaTrace[0]);
			
			bt.btData.sigmaTrace[1] = Double.parseDouble(nfSigmaY.getText());
			Prefs.set("BigTrace.sigmaTraceY", bt.btData.sigmaTrace[1]);
			
			bt.btData.sigmaTrace[2] = Double.parseDouble(nfSigmaZ.getText());
			Prefs.set("BigTrace.sigmaTraceZ", bt.btData.sigmaTrace[2]);
			
			bt.btData.bTraceOnlyClipped = cbTraceOnlyClipped.isSelected();
			Prefs.set("BigTrace.bTraceOnlyClipped", bt.btData.bTraceOnlyClipped);			
			
			bt.btData.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", bt.btData.lTraceBoxSize);
			
			bt.btData.dTraceBoxScreenFraction = Double.parseDouble(nfTraceBoxScreenFraction.getText());
			Prefs.set("BigTrace.dTraceBoxScreenFraction", bt.btData.dTraceBoxScreenFraction);
			
			bt.btData.fTraceBoxAdvanceFraction = Float.parseFloat(nfTBAdvance.getText());
			Prefs.set("BigTrace.fTraceBoxAdvanceFraction", bt.btData.fTraceBoxAdvanceFraction);
			
			bt.btData.gammaTrace = Double.parseDouble(nfGammaTrace.getText());
			Prefs.set("BigTrace.gammaTrace", bt.btData.gammaTrace);
			
			
		}
	}
	
	
	public void dialOneClickProperties()
	{
		JTabbedPane tabPane = new JTabbedPane();
		GridBagConstraints cd = new GridBagConstraints();

		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.000", decimalFormatSymbols);
		
		////////////TRACING OPTIONS
		JPanel pTrace = new JPanel(new GridBagLayout());

		NumberField nfSigmaX = new NumberField(4);
		NumberField nfSigmaY = new NumberField(4);
		NumberField nfSigmaZ = new NumberField(4);
		

		NumberField nfIntensityThreshold = new NumberField(5);
		JCheckBox cbTraceOnlyClipped = new JCheckBox();
		JCheckBox cbIntensityStop = new JCheckBox();

		
		nfSigmaX.setText(df.format(bt.btData.sigmaTrace[0]));
		nfSigmaY.setText(df.format(bt.btData.sigmaTrace[1]));
		nfSigmaZ.setText(df.format(bt.btData.sigmaTrace[2]));

		cbTraceOnlyClipped.setSelected(bt.btData.bTraceOnlyClipped);
		

		cd.gridx=0;
		cd.gridy=0;

		pTrace.add(new JLabel("Curve thickness X axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaX,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pTrace.add(new JLabel("Curve thickness Y axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaY,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pTrace.add(new JLabel("Curve thickness Z axis (SD, px): "),cd);
		cd.gridx++;
		pTrace.add(nfSigmaZ,cd);

		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pTrace.add(new JLabel("Trace only clipped volume: "),cd);
		cd.gridx++;
		pTrace.add(cbTraceOnlyClipped,cd);
		
		
		////////////ONE-CLICK TRACING OPTIONS
		JPanel pOneCLick = new JPanel(new GridBagLayout());
		
		NumberField nfPlaceVertex = new NumberField(4);
		NumberField nfDirectionalityOneClick = new NumberField(4);
		
		nfPlaceVertex.setIntegersOnly(true);
		nfPlaceVertex.setText(Integer.toString(bt.btData.nVertexPlacementPointN));
		nfDirectionalityOneClick.setText(df.format(bt.btData.dDirectionalityOneClick));
		cbIntensityStop.setSelected(bt.btData.bOCIntensityStop);
		nfIntensityThreshold.setText(df.format(bt.btData.dOCIntensityThreshold));
		nfIntensityThreshold.setTFEnabled( bt.btData.bOCIntensityStop);
		
		cd.gridx=0;
		cd.gridy=0;		
		pOneCLick.add(new JLabel("Intermediate vertex placement (px, >=3): "),cd);
		cd.gridx++;
		pOneCLick.add(nfPlaceVertex,cd);
		
		cd.gridx=0;		
		cd.gridy++;
		pOneCLick.add(new JLabel("Constrain directionality (0-1): "),cd);
		cd.gridx++;
		pOneCLick.add(nfDirectionalityOneClick,cd);
		
		cd.gridx=0;
		cd.gridy++;
		//cd.anchor=GridBagConstraints.WEST;
		pOneCLick.add(new JLabel("Use intensity threshold: "),cd);
		cd.gridx++;
		pOneCLick.add(cbIntensityStop,cd);
		cbIntensityStop.addActionListener( new ActionListener()
				{

					@Override
					public void actionPerformed( ActionEvent arg0 )
					{
						
						nfIntensityThreshold.setTFEnabled( cbIntensityStop.isSelected() );
					}
			
				});
		
		cd.gridx = 0;
		cd.gridy++;
		pOneCLick.add(new JLabel("Minimum intensity: "),cd);
		cd.gridx++;
		pOneCLick.add(nfIntensityThreshold,cd);
		
		//assemble pane
		tabPane.addTab("Tracing",pTrace);
		tabPane.addTab("One click trace",pOneCLick);
		int reply = JOptionPane.showConfirmDialog(null, tabPane, "One click tracing settings", 
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


		if (reply == JOptionPane.OK_OPTION) 
		{
	
			//TRACING OPTIONS
			
			bt.btData.sigmaTrace[0] = Double.parseDouble(nfSigmaX.getText());
			Prefs.set("BigTrace.sigmaTraceX", bt.btData.sigmaTrace[0]);
			
			bt.btData.sigmaTrace[1] = Double.parseDouble(nfSigmaY.getText());
			Prefs.set("BigTrace.sigmaTraceY", bt.btData.sigmaTrace[1]);
			
			bt.btData.sigmaTrace[2] = Double.parseDouble(nfSigmaZ.getText());
			Prefs.set("BigTrace.sigmaTraceZ", bt.btData.sigmaTrace[2]);
			
			bt.btData.bTraceOnlyClipped = cbTraceOnlyClipped.isSelected();
			Prefs.set("BigTrace.bTraceOnlyClipped", bt.btData.bTraceOnlyClipped);			
			
			bt.btData.nVertexPlacementPointN = Math.max(3, Integer.parseInt(nfPlaceVertex.getText()));
			Prefs.set("BigTrace.nVertexPlacementPointN", (double)(bt.btData.nVertexPlacementPointN));
			
			bt.btData.dDirectionalityOneClick = Math.min(1.0, (Math.max(0, Double.parseDouble(nfDirectionalityOneClick.getText()))));
			Prefs.set("BigTrace.dDirectionalityOneClick",bt.btData.dDirectionalityOneClick);
			
			bt.btData.bOCIntensityStop = cbIntensityStop.isSelected();
			Prefs.set("BigTrace.bOCIntensityStop", bt.btData.bOCIntensityStop);	
			if(bt.btData.bOCIntensityStop)
			{
				bt.btData.dOCIntensityThreshold = Math.max(0, Double.parseDouble(nfIntensityThreshold.getText()));
				Prefs.set("BigTrace.dOCIntensityThreshold",bt.btData.dOCIntensityThreshold);		
			}
		}
	}
	/** show ROI Appearance dialog**/
	public void dialRenderSettings()
	{
		final RoiManager3D<T> rm = bt.roiManager;
		JTabbedPane tabPane = new JTabbedPane();
		GridBagConstraints cd = new GridBagConstraints();

		DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
		decimalFormatSymbols.setDecimalSeparator('.');
		DecimalFormat df = new DecimalFormat("0.000", decimalFormatSymbols);
		////////////ROI RENDER OPTIONS
		JPanel pROIrender = new JPanel(new GridBagLayout());
		
		JButton butPointActiveColor = new JButton( new ColorIcon( rm.activePointColor ) );	
		butPointActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.btPanel.finFrame, "Choose active point color", rm.activePointColor );
			if (newColor!=null)
			{
				rm.selectColors.setColor(newColor, 0);
				butPointActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineActiveColor = new JButton( new ColorIcon( rm.activeLineColor ) );	
		butLineActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.btPanel.finFrame, "Choose active line color", rm.activeLineColor );
			if (newColor!=null)
			{
				rm.selectColors.setColor(newColor, 1);

				butLineActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JCheckBox cbRoiDoubleClickClip = new JCheckBox();
		cbRoiDoubleClickClip.setSelected(BigTraceData.bROIDoubleClickClip);
		
		NumberField nfRoiDoubleClickExpand = new NumberField(2);
		nfRoiDoubleClickExpand.setIntegersOnly(true);
		nfRoiDoubleClickExpand.setText(Integer.toString(BigTraceData.nROIDoubleClickClipExpand));
		
		String[] sShapeInterpolationType = { "Voxel", "Smooth", "Spline"};
		JComboBox<String> shapeInterpolationList = new JComboBox<>(sShapeInterpolationType);
		shapeInterpolationList.setSelectedIndex(BigTraceData.shapeInterpolation);
		
		NumberField nfSmoothWindow = new NumberField(2);
		nfSmoothWindow.setIntegersOnly(true);
		nfSmoothWindow.setText(Integer.toString(BigTraceData.nSmoothWindow));
		
		
		String[] sTimeRenderROIs = { "current timepoint", "backward in time", "forward in time"};
		JComboBox<String> sTimeRenderROIsList = new JComboBox<>(sTimeRenderROIs);
		sTimeRenderROIsList.setSelectedIndex(BigTraceData.timeRender);
		
		NumberField nfTimeFadeROIs = new NumberField(4);
		nfTimeFadeROIs.setIntegersOnly(true);
		nfTimeFadeROIs.setText(Integer.toString(Math.abs(BigTraceData.timeFade)));
		
		
		
		cd.gridx = 0;
		cd.gridy = 0;
		GBCHelper.alighLoose(cd);
		pROIrender.add(new JLabel("Selected ROI point color: "),cd);
		cd.gridx++;
		pROIrender.add(butPointActiveColor,cd);
		cd.gridx = 0;
		cd.gridy++;
		pROIrender.add(new JLabel("Selected ROI line color: "),cd);
		cd.gridx++;
		pROIrender.add(butLineActiveColor,cd);
		
		cd.gridx = 0;
		cd.gridy++;
		pROIrender.add(new JLabel("Clip volume on ROI double-click: "),cd);
		cd.gridx++;
		pROIrender.add(cbRoiDoubleClickClip,cd);
		
		cd.gridx = 0;
		cd.gridy++;
		pROIrender.add(new JLabel("Expand clipping by (voxels): "),cd);
		cd.gridx++;
		pROIrender.add(nfRoiDoubleClickExpand,cd);
		
		cd.gridx = 0;
		cd.gridy++;
		pROIrender.add(new JLabel("ROI Shape interpolation: "),cd);
		cd.gridx++;
		pROIrender.add(shapeInterpolationList,cd);	
		
		cd.gridx = 0;
		cd.gridy++;
		pROIrender.add(new JLabel("Trace smoothing window (points): "),cd);
		cd.gridx++;
		pROIrender.add(nfSmoothWindow,cd);
		

		if(BigTraceData.nNumTimepoints > 1)
		{
			cd.gridx = 0;
			cd.gridy++;
			cd.gridwidth = 2;
			cd.anchor = GridBagConstraints.CENTER;
			pROIrender.add(new JLabel("<html>----  <B>Time</B>  ----</html>"),cd);
			cd.gridwidth=1;
			cd.anchor = GridBagConstraints.WEST;
			
			
			cd.gridx = 0;
			cd.gridy++;
			pROIrender.add(new JLabel("Show ROIs over time: "),cd);
			cd.gridx++;
			pROIrender.add(sTimeRenderROIsList,cd);
			cd.gridx = 0;
			cd.gridy++;
			pROIrender.add(new JLabel("Time fade range (frames): "),cd);
			cd.gridx++;
			pROIrender.add(nfTimeFadeROIs,cd);
		}

		////////////ROI SURFACE RENDER 
		JPanel pROIsurface = new JPanel(new GridBagLayout());
		
		NumberField nfSectorNLines = new NumberField(4);
		nfSectorNLines.setIntegersOnly(true);
		nfSectorNLines.setText(Integer.toString(BigTraceData.sectorN));
		
		JCheckBox cbWireAA = new JCheckBox();
		cbWireAA.setSelected( BigTraceData.wireAntiAliasing );
		
		NumberField nfWireContourStep = new NumberField(4);
		nfWireContourStep.setIntegersOnly(true);
		nfWireContourStep.setText(Integer.toString(BigTraceData.wireCountourStep));
		
		NumberField nfCrossSectionGridStep = new NumberField(4);
		nfCrossSectionGridStep.setIntegersOnly(true);
		nfCrossSectionGridStep.setText(Integer.toString(BigTraceData.crossSectionGridStep));
		
		String[] sSilhouetteRenderType = { "transparent", "front culling"};
		JComboBox<String> sSilhouetteRenderList = new JComboBox<>(sSilhouetteRenderType);
		sSilhouetteRenderList.setSelectedIndex(BigTraceData.silhouetteRender);
		
		NumberField nfSilhouetteDecay = new NumberField(4);
		nfSilhouetteDecay.setText(df.format(BigTraceData.silhouetteDecay));
		
		cd.gridx = 0;
		cd.gridy = 0;
		pROIsurface.add(new JLabel("# edges at curve cross-section:"),cd);
		cd.gridx++;
		pROIsurface.add(nfSectorNLines,cd);
		
		cd.gridx = 0;
		cd.gridy++;
		cd.gridwidth = 2;
		cd.anchor = GridBagConstraints.CENTER;
		pROIsurface.add(new JLabel("<html>----  <B>Wire</B> mode  ----</html>"),cd);
		cd.gridwidth = 1;
		cd.anchor = GridBagConstraints.WEST;
		
		cd.gridx = 0;
		cd.gridy++;
		pROIsurface.add(new JLabel("Use anti-aliased lines :"),cd);
		cd.gridx++;
		pROIsurface.add(cbWireAA,cd);
		
		cd.gridx = 0;
		cd.gridy++;
		pROIsurface.add(new JLabel("Distance between curve contours (px):"),cd);
		cd.gridx++;
		pROIsurface.add(nfWireContourStep,cd);
		
		cd.gridx = 0;
		cd.gridy++;
		pROIsurface.add(new JLabel("Plane/cross-section ROI grid step (px): "),cd);
		cd.gridx++;
		pROIsurface.add(nfCrossSectionGridStep,cd);
		
		cd.gridx = 0;
		cd.gridy++;
		cd.gridwidth = 2;
		cd.anchor = GridBagConstraints.CENTER;
		pROIsurface.add(new JLabel("<html>----  <B>Silhouette</B> surface ----</html>"),cd);
		cd.gridwidth=1;
		cd.anchor = GridBagConstraints.WEST;
	
		
		cd.gridx = 0;
		cd.gridy++;
		pROIsurface.add(new JLabel("Silhouette render:"),cd);
		cd.gridx++;
		pROIsurface.add(sSilhouetteRenderList,cd);	
		
		cd.gridx = 0;		
		cd.gridy++;
		pROIsurface.add(new JLabel("Silhouette decay: "),cd);
		cd.gridx++;
		pROIsurface.add(nfSilhouetteDecay,cd);
				
		//assemble pane
		tabPane.addTab("ROI render", pROIrender);
		tabPane.addTab("ROI surface", pROIsurface);
		int reply = JOptionPane.showConfirmDialog(null, tabPane, "ROI Manager render settings", 
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


		if (reply == JOptionPane.OK_OPTION) 
		{
	
			//ROI appearance
			boolean bUpdateROIs = false;
			boolean bUpdateBVV = false;
			
			Color tempC;
			
			tempC = rm.selectColors.getColor(0);
			if(tempC!=null)
			{
				rm.activePointColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				rm.selectColors.setColor(null, 0);
				Prefs.set("BigTrace.activePointColor", tempC.getRGB());
			}
			
			tempC = rm.selectColors.getColor(1);
			if(tempC != null)
			{
				rm.activeLineColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				rm.selectColors.setColor(null, 1);
				Prefs.set("BigTrace.activeLineColor", tempC.getRGB());
			}
			
			BigTraceData.bROIDoubleClickClip = cbRoiDoubleClickClip.isSelected();
			Prefs.set("BigTrace.bROIDoubleClickClip", BigTraceData.bROIDoubleClickClip );
			
			if(BigTraceData.nROIDoubleClickClipExpand != Integer.parseInt(nfRoiDoubleClickExpand.getText()))
			{
				BigTraceData.nROIDoubleClickClipExpand = Integer.parseInt(nfRoiDoubleClickExpand.getText());
				Prefs.set("BigTrace.nROIDoubleClickClipExpand", BigTraceData.nROIDoubleClickClipExpand);
			}
			
			if(BigTraceData.sectorN != Integer.parseInt(nfSectorNLines.getText()))
			{
				BigTraceData.sectorN = Integer.parseInt(nfSectorNLines.getText());
				Prefs.set("BigTrace.nSectorN", BigTraceData.sectorN);
				bUpdateROIs  = true;
			}
			
			if(BigTraceData.wireAntiAliasing != cbWireAA.isSelected())
			{
				BigTraceData.wireAntiAliasing = cbWireAA.isSelected();
				Prefs.set("BigTrace.wireAntiAliasing", BigTraceData.wireAntiAliasing);
				bUpdateROIs  = true;
			}
			
			if(BigTraceData.wireCountourStep != Integer.parseInt(nfWireContourStep.getText()))
			{
				BigTraceData.wireCountourStep = Integer.parseInt(nfWireContourStep.getText());
				Prefs.set("BigTrace.wireCountourStep", BigTraceData.wireCountourStep);
				bUpdateROIs  = true;
			}
			
			if(BigTraceData.crossSectionGridStep != Integer.parseInt(nfCrossSectionGridStep.getText()))
			{
				BigTraceData.crossSectionGridStep = Integer.parseInt(nfCrossSectionGridStep.getText());
				Prefs.set("BigTrace.crossSectionGridStep", BigTraceData.crossSectionGridStep);
				bUpdateROIs  = true;
			}
			
			
			if(BigTraceData.silhouetteRender != sSilhouetteRenderList.getSelectedIndex())
			{
				BigTraceData.silhouetteRender = sSilhouetteRenderList.getSelectedIndex();
				Prefs.set("BigTrace.silhouetteRender", BigTraceData.silhouetteRender);
				bUpdateBVV = true;
				//bUpdateROIs  = true;
			}
			
			if(Math.abs(BigTraceData.silhouetteDecay - Double.parseDouble(nfSilhouetteDecay.getText()))>0.0001)
			{
				BigTraceData.silhouetteDecay = Double.parseDouble(nfSilhouetteDecay.getText());
				Prefs.set("BigTrace.silhouetteDecay",BigTraceData.silhouetteDecay);
				bUpdateBVV = true;
				//bUpdateROIs  = true;
			}
			//INTERPOLATION
			
			if(BigTraceData.nSmoothWindow != Integer.parseInt(nfSmoothWindow.getText())||BigTraceData.shapeInterpolation!= shapeInterpolationList.getSelectedIndex())
			{
				BigTraceData.nSmoothWindow = Integer.parseInt(nfSmoothWindow.getText());
				Prefs.set("BigTrace.nSmoothWindow", BigTraceData.nSmoothWindow);
				BigTraceData.shapeInterpolation = shapeInterpolationList.getSelectedIndex();
				Prefs.set("BigTrace.ShapeInterpolation",BigTraceData.shapeInterpolation);
				bUpdateROIs  = true;				
			}
			
			//TIME RENDER
			if(BigTraceData.nNumTimepoints>1)
			{
				if(BigTraceData.timeFade != Integer.parseInt(nfTimeFadeROIs.getText())||BigTraceData.timeRender!= sTimeRenderROIsList.getSelectedIndex())
				{
					BigTraceData.timeRender= sTimeRenderROIsList.getSelectedIndex();
					Prefs.set("BigTrace.timeRender",BigTraceData.timeRender);
					if(BigTraceData.timeRender==0)
					{
						BigTraceData.timeFade = 0;
					}
					else
					{
						if(BigTraceData.timeRender == 1)
						{
							BigTraceData.timeFade = (-1)*Integer.parseInt(nfTimeFadeROIs.getText());
						}
						else
						{
							BigTraceData.timeFade = Integer.parseInt(nfTimeFadeROIs.getText());
						}
						Prefs.set("BigTrace.timeFade", BigTraceData.timeFade);
					}
					bUpdateBVV = true;	
					//bUpdateROIs  = true;	
				}
			}
			
			
			if(bUpdateROIs)
			{
				rm.updateROIsDisplay();
			}
			else
			{
				if(bUpdateBVV)
				{
					bt.repaintBVV();
				}
			}
			
		}
	}
	
	/** show Group visibility dialog **/
	public void dialGroupVisibility()
	{
		Roi3DGroupVisibility<T> groupVis = new Roi3DGroupVisibility<>(bt.roiManager);
		groupVis.show();
	}
	
	/** Save ROIS dialog and saving **/
	public void diagSaveROIs()
	{
		
		String [] sRoiSaveOptions = new String [] {"Save ROIs BigTrace format","Export interpolated traces CSV", "Export neurite SWC"};	

		String output = (String) JOptionPane.showInputDialog(bt.roiManager, "Save/export ROIs",
				"Load mode:", JOptionPane.QUESTION_MESSAGE, null,
				sRoiSaveOptions, // Array of choices
				sRoiSaveOptions[(int)Prefs.get("BigTrace.SaveRoisMode", 0)]);
		if(output == null)
			return;

		int nSaveMode = 0;

		for (int i=0;i<3; i++)
		{
			if(output.equals( sRoiSaveOptions[i] ))
			{
				nSaveMode = i;
			}
		}
        Prefs.set("BigTrace.SaveRoisMode", nSaveMode);

        
		String filename;
		SaveDialog sd;
		String path;
		switch (nSaveMode)
		{
		case 0: 
			filename = bt.btData.sFileNameFullImg + "_btrois";
			sd = new SaveDialog("Save ROIs ", filename, ".csv");
			path = sd.getDirectory();
			if (path == null)
				return;
			filename = path + sd.getFileName();

			bt.setLockMode(true);
			bt.bInputLock = true;

			ROIsSaveBG<T> saveTask = new ROIsSaveBG<>();
			saveTask.sFilename = filename;
			saveTask.bt = this.bt;
			saveTask.addPropertyChangeListener(bt.btPanel);
			saveTask.execute();
			break;
		case 1: 
			filename = bt.btData.sFileNameFullImg + "_traces";
			sd = new SaveDialog("Export ROIs ", filename, ".csv");
			path = sd.getDirectory();
			if (path == null)
				return;
			filename = path + sd.getFileName();

			bt.setLockMode(true);
			bt.bInputLock = true;

			ROIsExportCSV<T> exportTask = new ROIsExportCSV<>();
			exportTask.sFilename = filename;
			exportTask.bt = this.bt;
			exportTask.addPropertyChangeListener(bt.btPanel);
			exportTask.execute();
			break;
		case 2: 
			filename = bt.btData.sFileNameFullImg + "_traces";
			sd = new SaveDialog("Export ROIs to SWC ", filename, ".swc");
			path = sd.getDirectory();
			if (path == null)
				return;
			filename = path + sd.getFileName();

			bt.setLockMode(true);
			bt.bInputLock = true;

			ROIsExportSWC<T> exportSWCTask = new ROIsExportSWC<>();
			exportSWCTask.sFilename = filename;
			exportSWCTask.bt = this.bt;
			exportSWCTask.addPropertyChangeListener(bt.btPanel);
			exportSWCTask.execute();
			break;
		}
	}
	
	
	/** Load ROIS dialog and saving **/
    void diagLoadROIs()
	{
		String filename;
		
		OpenDialog openDial = new OpenDialog("Load BigTrace ROIs",bt.btData.lastDir, "*.csv");
		
        String path = openDial.getDirectory();
        if (path==null)
        	return;
        
        bt.btData.lastDir = path;
        Prefs.set( "BigTrace.lastDir", bt.btData.lastDir );
        
        filename = path+openDial.getFileName();
     
        String [] sRoiLoadOptions = new String [] {"Clean load ROIs and groups","Append ROIs as undefined group"};	
        String input = (String) JOptionPane.showInputDialog(bt.roiManager, "Loading ROIs",
                "Load mode:", JOptionPane.QUESTION_MESSAGE, null,
                sRoiLoadOptions, // Array of choices
                sRoiLoadOptions[(int)Prefs.get("BigTrace.LoadRoisMode", 0)]);
        
        if(input == null)
        	 return;
        
        int nLoadMode = 0;
        
        if(input.equals(sRoiLoadOptions[1]))
        {
        	nLoadMode = 1;
        }
        
        Prefs.set("BigTrace.LoadRoisMode", nLoadMode);
        bt.roiManager.loadROIs(filename, nLoadMode);        
	}
    
	/** Import ROIs dialog **/
	public void diagImportROIs()
	{
	      
        String [] sRoiImportOptions = new String [] {"Points from TrackMate XML (Export)","Points from CSV (coming soon)"};
		
        String input = (String) JOptionPane.showInputDialog(bt.roiManager, "Importing ROIs",
                "Import:", JOptionPane.QUESTION_MESSAGE, null, // Use default icon
                sRoiImportOptions, // Array of choices
                sRoiImportOptions[(int)Prefs.get("BigTrace.ImportRoisMode", 0)]);

        if(input == null)
        	return;
        if(input.isEmpty())
        	return;
        int nImportMode;
        if(input.equals("Points from TrackMate XML (Export)"))
        {
        	nImportMode = 0;
        	diagImportTrackMate();
        }
        else
        {
        	nImportMode = 1;
        }
        Prefs.set("BigTrace.ImportRoisMode", nImportMode);
	}
	
	public void diagImportTrackMate()
	{
		String filename;
		OpenDialog openDial = new OpenDialog("Import TrackMate XML",bt.btData.lastDir, "*.xml");
		
        String path = openDial.getDirectory();
        if (path==null)
        	return;
        bt.btData.lastDir = path;
        Prefs.set( "BigTrace.lastDir", bt.btData.lastDir );
        filename = path + openDial.getFileName();
        
        String [] sTMColorOptions = new String [] {"Random color per track","Current active group color"};
		
        String inputColor = (String) JOptionPane.showInputDialog(bt.roiManager, "Coloring ROIs",
                "For color, use:", JOptionPane.QUESTION_MESSAGE, null, // Use
                                                                                // default
                                                                                // icon
                sTMColorOptions, // Array of choices
                sTMColorOptions[(int)Prefs.get("BigTrace.ImportTMColorMode", 0)]);
        
        if(inputColor == null)
        	return;
        if(inputColor.isEmpty())
        	return;
        int nImportColor;
        
        if(inputColor.equals("Random color per track"))
        {
        	nImportColor = 0;
        }
        else
        {
        	nImportColor = 1;
        }
        
        Prefs.set("BigTrace.ImportTMColorMode", nImportColor);		

       	bt.roiManager.rois = new ArrayList< >();
        bt.roiManager.listModel.clear();
        
        ROIsImportTrackMateBG importTask = new ROIsImportTrackMateBG();
        importTask.nImportColor = nImportColor;
        importTask.sFilename = filename;
        importTask.bt = this.bt;
        importTask.addPropertyChangeListener(bt.btPanel);
        importTask.execute();
	}
}

package bigtrace.rois;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

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
import ij.Prefs;
import net.imglib2.type.numeric.RealType;

public class RoiManager3DDialogs < T extends RealType< T > > {
	final BigTrace<T> bt;
	
	

	public RoiManager3DDialogs(BigTrace<T> bt_)
	{
		bt=bt_;
		
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
		JCheckBox cbTraceOnlyCrop = new JCheckBox();

		
		nfSigmaX.setText(df.format(bt.btdata.sigmaTrace[0]));
		nfSigmaY.setText(df.format(bt.btdata.sigmaTrace[1]));
		nfSigmaZ.setText(df.format(bt.btdata.sigmaTrace[2]));

		cbTraceOnlyCrop.setSelected(bt.btdata.bTraceOnlyCrop);
		
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
		pTrace.add(new JLabel("Trace only cropped volume: "),cd);
		cd.gridx++;
		pTrace.add(cbTraceOnlyCrop,cd);
		
		////////////SEMI-AUTO TRACING OPTIONS
		JPanel pSemiAuto = new JPanel(new GridBagLayout());
		
		NumberField nfGammaTrace = new NumberField(4);
		NumberField nfTraceBoxSize = new NumberField(4);
		NumberField nfTraceBoxScreenFraction = new NumberField(4);
		NumberField nfTBAdvance = new NumberField(4);
		
		nfTraceBoxSize.setText(Integer.toString((int)(2.0*bt.btdata.lTraceBoxSize)));
		nfTraceBoxScreenFraction.setText(df.format(bt.btdata.dTraceBoxScreenFraction));
		nfGammaTrace.setText(df.format(bt.btdata.gammaTrace));
		nfTBAdvance.setText(df.format(bt.btdata.fTraceBoxAdvanceFraction));
			
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
				
			bt.btdata.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", (double)(bt.btdata.lTraceBoxSize));
			
			bt.btdata.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", (double)(bt.btdata.lTraceBoxSize));
		
			//TRACING OPTIONS
			
			bt.btdata.sigmaTrace[0] = Double.parseDouble(nfSigmaX.getText());
			Prefs.set("BigTrace.sigmaTraceX", (double)(bt.btdata.sigmaTrace[0]));
			
			bt.btdata.sigmaTrace[1] = Double.parseDouble(nfSigmaY.getText());
			Prefs.set("BigTrace.sigmaTraceY", (double)(bt.btdata.sigmaTrace[1]));
			
			bt.btdata.sigmaTrace[2] = Double.parseDouble(nfSigmaZ.getText());
			Prefs.set("BigTrace.sigmaTraceZ", (double)(bt.btdata.sigmaTrace[2]));
			
			bt.btdata.bTraceOnlyCrop = cbTraceOnlyCrop.isSelected();
			Prefs.set("BigTrace.bTraceOnlyCrop", bt.btdata.bTraceOnlyCrop);			
			
			bt.btdata.lTraceBoxSize=(long)(Integer.parseInt(nfTraceBoxSize.getText())*0.5);
			Prefs.set("BigTrace.lTraceBoxSize", (double)(bt.btdata.lTraceBoxSize));
			
			bt.btdata.dTraceBoxScreenFraction = Double.parseDouble(nfTraceBoxScreenFraction.getText());
			Prefs.set("BigTrace.dTraceBoxScreenFraction", (double)(bt.btdata.dTraceBoxScreenFraction));
			
			bt.btdata.fTraceBoxAdvanceFraction = Float.parseFloat(nfTBAdvance.getText());
			Prefs.set("BigTrace.fTraceBoxAdvanceFraction", (double)(bt.btdata.fTraceBoxAdvanceFraction));
			
			bt.btdata.gammaTrace = Double.parseDouble(nfGammaTrace.getText());
			Prefs.set("BigTrace.gammaTrace", (double)(bt.btdata.gammaTrace));
			
			
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
		JCheckBox cbTraceOnlyCrop = new JCheckBox();

		
		nfSigmaX.setText(df.format(bt.btdata.sigmaTrace[0]));
		nfSigmaY.setText(df.format(bt.btdata.sigmaTrace[1]));
		nfSigmaZ.setText(df.format(bt.btdata.sigmaTrace[2]));

		cbTraceOnlyCrop.setSelected(bt.btdata.bTraceOnlyCrop);
		
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
		pTrace.add(new JLabel("Trace only cropped volume: "),cd);
		cd.gridx++;
		pTrace.add(cbTraceOnlyCrop,cd);
		
		
		////////////ONE-CLICK TRACING OPTIONS
		JPanel pOneCLick = new JPanel(new GridBagLayout());
		
		NumberField nfPlaceVertex = new NumberField(4);
		NumberField nfDirectionalityOneClick = new NumberField(4);
		
		nfPlaceVertex.setIntegersOnly(true);
		nfPlaceVertex.setText(Integer.toString((int)(bt.btdata.nVertexPlacementPointN)));
		nfDirectionalityOneClick.setText(df.format(bt.btdata.dDirectionalityOneClick));
		
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
		
		//assemble pane
		tabPane.addTab("Tracing",pTrace);
		tabPane.addTab("One click trace",pOneCLick);
		int reply = JOptionPane.showConfirmDialog(null, tabPane, "One click tracing settings", 
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


		if (reply == JOptionPane.OK_OPTION) 
		{
	
			//TRACING OPTIONS
			
			bt.btdata.sigmaTrace[0] = Double.parseDouble(nfSigmaX.getText());
			Prefs.set("BigTrace.sigmaTraceX", (double)(bt.btdata.sigmaTrace[0]));
			
			bt.btdata.sigmaTrace[1] = Double.parseDouble(nfSigmaY.getText());
			Prefs.set("BigTrace.sigmaTraceY", (double)(bt.btdata.sigmaTrace[1]));
			
			bt.btdata.sigmaTrace[2] = Double.parseDouble(nfSigmaZ.getText());
			Prefs.set("BigTrace.sigmaTraceZ", (double)(bt.btdata.sigmaTrace[2]));
			
			bt.btdata.bTraceOnlyCrop = cbTraceOnlyCrop.isSelected();
			Prefs.set("BigTrace.bTraceOnlyCrop", bt.btdata.bTraceOnlyCrop);			
			
			bt.btdata.nVertexPlacementPointN=(int)(Math.max(3, Integer.parseInt(nfPlaceVertex.getText())));
			Prefs.set("BigTrace.nVertexPlacementPointN", (double)(bt.btdata.nVertexPlacementPointN));
			
			bt.btdata.dDirectionalityOneClick=Math.min(1.0, (Math.max(0, Double.parseDouble(nfDirectionalityOneClick.getText()))));
			Prefs.set("BigTrace.dDirectionalityOneClick",bt.btdata.dDirectionalityOneClick);
			
			
		}
	}
	/** show ROI Appearance dialog**/
	public void dialRenderSettings()
	{
		final RoiManager3D rm = bt.roiManager;
		JTabbedPane tabPane = new JTabbedPane();
		GridBagConstraints cd = new GridBagConstraints();

		
		////////////ROI RENDER OPTIONS
		JPanel pROIrender = new JPanel(new GridBagLayout());
		
		JButton butPointActiveColor = new JButton( new ColorIcon( rm.activePointColor ) );	
		butPointActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.btpanel.finFrame, "Choose active point color", rm.activePointColor );
			if (newColor!=null)
			{
				rm.selectColors.setColor(newColor, 0);
				butPointActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		JButton butLineActiveColor = new JButton( new ColorIcon( rm.activeLineColor ) );	
		butLineActiveColor.addActionListener( e -> {
			Color newColor = JColorChooser.showDialog(bt.btpanel.finFrame, "Choose active line color", rm.activeLineColor );
			if (newColor!=null)
			{
				rm.selectColors.setColor(newColor, 1);

				butLineActiveColor.setIcon(new ColorIcon(newColor));
			}
			
		});
		
		String[] sShapeInterpolationType = { "Voxel", "Smooth", "Spline"};
		JComboBox<String> shapeInterpolationList = new JComboBox<String>(sShapeInterpolationType);
		shapeInterpolationList.setSelectedIndex(BigTraceData.shapeInterpolation);
		
		NumberField nfSmoothWindow = new NumberField(2);
		nfSmoothWindow.setIntegersOnly(true);
		nfSmoothWindow.setText(Integer.toString(BigTraceData.nSmoothWindow));
		
		
		String[] sTimeRenderROIs = { "current timepoint", "backward in time", "forward in time"};
		JComboBox<String> sTimeRenderROIsList = new JComboBox<String>(sTimeRenderROIs);
		sTimeRenderROIsList.setSelectedIndex(BigTraceData.timeRender);
		
		NumberField nfTimeFadeROIs = new NumberField(4);
		nfTimeFadeROIs.setIntegersOnly(true);
		nfTimeFadeROIs.setText(Integer.toString((int)Math.abs(BigTraceData.timeFade)));
		
		
		
		cd.gridx=0;
		cd.gridy=0;
		GBCHelper.alighLoose(cd);
		pROIrender.add(new JLabel("Selected ROI point color: "),cd);
		cd.gridx++;
		pROIrender.add(butPointActiveColor,cd);
		cd.gridx=0;
		cd.gridy++;
		pROIrender.add(new JLabel("Selected ROI line color: "),cd);
		cd.gridx++;
		pROIrender.add(butLineActiveColor,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pROIrender.add(new JLabel("ROI Shape interpolation: "),cd);
		cd.gridx++;
		pROIrender.add(shapeInterpolationList,cd);	
		
		cd.gridx=0;
		cd.gridy++;
		pROIrender.add(new JLabel("Trace smoothing window (points): "),cd);
		cd.gridx++;
		pROIrender.add(nfSmoothWindow,cd);
		

		if(BigTraceData.nNumTimepoints > 1)
		{
			cd.gridx=0;
			cd.gridy++;
			cd.gridwidth=2;
			cd.anchor = GridBagConstraints.CENTER;
			pROIrender.add(new JLabel("<html>----  <B>Time</B>  ----</html>"),cd);
			cd.gridwidth=1;
			cd.anchor = GridBagConstraints.WEST;
			
			
			cd.gridx=0;
			cd.gridy++;
			pROIrender.add(new JLabel("Show ROIs over time: "),cd);
			cd.gridx++;
			pROIrender.add(sTimeRenderROIsList,cd);
			cd.gridx=0;
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
		
		NumberField nfWireContourStep = new NumberField(4);
		nfWireContourStep.setIntegersOnly(true);
		nfWireContourStep.setText(Integer.toString(BigTraceData.wireCountourStep));
		
		NumberField nfCrossSectionGridStep = new NumberField(4);
		nfCrossSectionGridStep.setIntegersOnly(true);
		nfCrossSectionGridStep.setText(Integer.toString(BigTraceData.crossSectionGridStep));
		
		String[] sSurfaceRenderType = { "plain", "silhouette", "shaded", "shiny"};
		JComboBox<String> sSurfaceRenderList = new JComboBox<String>(sSurfaceRenderType);
		sSurfaceRenderList.setSelectedIndex(BigTraceData.surfaceRender);
		
		cd.gridx=0;
		cd.gridy=0;
		pROIsurface.add(new JLabel("# edges at curve cross-section:"),cd);
		cd.gridx++;
		pROIsurface.add(nfSectorNLines,cd);
		
		cd.gridx=0;
		cd.gridy++;
		cd.gridwidth=2;
		cd.anchor = GridBagConstraints.CENTER;
		pROIsurface.add(new JLabel("<html>----  <B>Wire</B> mode  ----</html>"),cd);
		cd.gridwidth=1;
		cd.anchor = GridBagConstraints.WEST;
		
		cd.gridx=0;
		cd.gridy++;
		pROIsurface.add(new JLabel("Distance between curve contours (px):"),cd);
		cd.gridx++;
		pROIsurface.add(nfWireContourStep,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pROIsurface.add(new JLabel("Plane/cross-section ROI grid step (px): "),cd);
		cd.gridx++;
		pROIsurface.add(nfCrossSectionGridStep,cd);
		
		cd.gridx=0;
		cd.gridy++;
		cd.gridwidth=2;
		cd.anchor = GridBagConstraints.CENTER;
		pROIsurface.add(new JLabel("<html>----  <B>Surface</B> mode  ----</html>"),cd);
		cd.gridwidth=1;
		cd.anchor = GridBagConstraints.WEST;
		
		cd.gridx=0;
		cd.gridy++;
		pROIsurface.add(new JLabel("ROI surface render type: "),cd);
		cd.gridx++;
		pROIsurface.add(sSurfaceRenderList,cd);	
				
		//assemble pane
		tabPane.addTab("ROI render", pROIrender);
		tabPane.addTab("ROI surface", pROIsurface);
		int reply = JOptionPane.showConfirmDialog(null, tabPane, "ROI Manager render settings", 
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


		if (reply == JOptionPane.OK_OPTION) 
		{
	
			//ROI appearance
			boolean bUpdateROIs = false;
			
			Color tempC;
			
			tempC = rm.selectColors.getColor(0);
			if(tempC!=null)
			{
				rm.activePointColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				rm.selectColors.setColor(null, 0);
			}
			tempC = rm.selectColors.getColor(1);
			if(tempC!=null)
			{
				rm.activeLineColor = new Color(tempC.getRed(),tempC.getGreen(),tempC.getBlue(),tempC.getAlpha());
				rm.selectColors.setColor(null, 1);
			}
			
			
			if(BigTraceData.sectorN!= Integer.parseInt(nfSectorNLines.getText()))
			{
				BigTraceData.sectorN= Integer.parseInt(nfSectorNLines.getText());
				Prefs.set("BigTrace.nSectorN", BigTraceData.sectorN);
				bUpdateROIs  = true;
			}
			if(BigTraceData.wireCountourStep!= Integer.parseInt(nfWireContourStep.getText()))
			{
				BigTraceData.wireCountourStep= Integer.parseInt(nfWireContourStep.getText());
				Prefs.set("BigTrace.wireCountourStep", BigTraceData.wireCountourStep);
				bUpdateROIs  = true;
			}
			
			if(BigTraceData.crossSectionGridStep!= Integer.parseInt(nfCrossSectionGridStep.getText()))
			{
				BigTraceData.crossSectionGridStep= Integer.parseInt(nfCrossSectionGridStep.getText());
				Prefs.set("BigTrace.crossSectionGridStep", BigTraceData.crossSectionGridStep);
				bUpdateROIs  = true;
			}
			
			if(BigTraceData.surfaceRender != sSurfaceRenderList.getSelectedIndex())
			{
				BigTraceData.surfaceRender = sSurfaceRenderList.getSelectedIndex();
				Prefs.set("BigTrace.surfaceRender", BigTraceData.surfaceRender);
				bt.btpanel.renderMethodPanel.cbSurfaceRenderList.setSelectedIndex(BigTraceData.surfaceRender);
				bUpdateROIs  = true;
			}
			
			//INTERPOLATION
			
			if(BigTraceData.nSmoothWindow != Integer.parseInt(nfSmoothWindow.getText())||BigTraceData.shapeInterpolation!= shapeInterpolationList.getSelectedIndex())
			{
				BigTraceData.nSmoothWindow = Integer.parseInt(nfSmoothWindow.getText());
				Prefs.set("BigTrace.nSmoothWindow", BigTraceData.nSmoothWindow);
				BigTraceData.shapeInterpolation= shapeInterpolationList.getSelectedIndex();
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

					bUpdateROIs  = true;	
				}
			}
			
			
			if(bUpdateROIs)
			{
				rm.updateROIsDisplay();
			}
			
		}
	}
}

package bigtrace;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import bdv.util.Affine3DHelpers;
import bigtrace.gui.AnisotropicTransformAnimator3D;
import bigtrace.gui.CropPanel;
import bigtrace.gui.NumberField;
import bigtrace.gui.PanelTitle;
import bigtrace.gui.RenderMethodPanel;
import bigtrace.gui.VoxelSizePanel;
import bigtrace.measure.RoiMeasure3D;
import bigtrace.rois.RoiManager3D;
import bigtrace.BigTraceData;
import btbvv.vistools.BvvStackSource;
import ij.Prefs;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.LinAlgHelpers;


public class BigTraceControlPanel< T extends RealType< T > > extends JPanel
//public class BigTraceControlPanel extends JFrame
								implements ActionListener, 
									PropertyChangeListener {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8992158095263652259L;
	
	BigTrace<T> bt;
	BigTraceData<T> btdata;	
	
	public CropPanel cropPanel;	
	public VoxelSizePanel voxelSizePanel;
	public RenderMethodPanel<T> renderMethodPanel;
	
	RoiManager3D roiManager;
	RoiMeasure3D<T> roiMeasure;
	

	double [][] nDisplayMinMax;

	public JFrame bvv_frame;
	public JFrame finFrame;
	public JProgressBar progressBar;
	JButton butSettings;

	
	public BigTraceControlPanel(final BigTrace<T> bt_,final BigTraceData<T> btd_, final RoiManager3D roiManager_)//, int locx, int locy) 
	{
		//finalPanel = new JPanel(new GridBagLayout());
		super(new GridBagLayout());

		btdata=btd_;
		bt=bt_;		
		roiManager=roiManager_;
		roiMeasure = new RoiMeasure3D<T>(bt);
		roiManager.setRoiMeasure3D(roiMeasure);
		
		JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);
		
		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/cube_icon.png");
	    ImageIcon tabIcon = new ImageIcon(icon_path);

	    tabPane.addTab("",tabIcon,panelNavigation(), "View/Crop");

	    //ROI MANAGER
	    icon_path = bigtrace.BigTrace.class.getResource("/icons/node.png");
	    tabIcon = new ImageIcon(icon_path);
	    tabPane.addTab("",tabIcon ,roiManager,"Tracing");
	    
	    //MEASUREMENTS
	    
	    icon_path = bigtrace.BigTrace.class.getResource("/icons/measure.png");
	    tabIcon = new ImageIcon(icon_path);
	    tabPane.addTab("",tabIcon ,roiMeasure,"Measure");
	    
	    icon_path = bigtrace.BigTrace.class.getResource("/icons/shortcut.png");
	    tabIcon = new ImageIcon(icon_path);
	    tabPane.addTab("",tabIcon ,panelInformation(),"Help/Shortcuts");

	    roiManager.addRoiManager3DListener(new RoiManager3D.Listener() {

			@Override
			public void activeRoiChanged(int nRoi) {
				//render_pl();
			}
	    	
	    });
	    
	    
	    tabPane.setSize(350, 300);
	    tabPane.setSelectedIndex(1);

	    
	    progressBar = new JProgressBar(0,100);
	    //progressBar.setIndeterminate(true);
	    progressBar.setValue(0);
	    progressBar.setStringPainted(true);
	    progressBar.setString("BigTrace version " + BigTraceData.sVersion);
		

	    GridBagConstraints cv = new GridBagConstraints();
	    cv.gridx=0;
	    cv.gridy=0;	    
	    cv.weightx=0.5;
	    cv.weighty=1.0;
	    cv.anchor = GridBagConstraints.NORTHWEST;
	    cv.gridwidth = GridBagConstraints.REMAINDER;
	    cv.fill = GridBagConstraints.HORIZONTAL;
	    cv.fill = GridBagConstraints.BOTH;

	    this.add(tabPane,cv);
	    cv.gridx=0;
	    cv.gridy=1;	    
	    cv.gridwidth = GridBagConstraints.RELATIVE;
	    cv.gridheight = GridBagConstraints.RELATIVE;
	    cv.weighty=0.01;
	    cv.anchor = GridBagConstraints.SOUTHEAST;
	    cv.fill = GridBagConstraints.HORIZONTAL;

	    this.add(progressBar,cv);
	
	    
	    //install actions from BVV
	    
	    this.setActionMap(bt.actions.getActionMap());
	    this.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,bt.actions.getInputMap());
	    
	}
	

	public JPanel panelNavigation()
	{
		
		JPanel panNavigation = new JPanel(new GridBagLayout());
		//Interface
		
		//RENDER METHOD PANEL
		
		renderMethodPanel = new RenderMethodPanel<T>(bt);
		
		
		//VOXEL SIZE PANEL		
		voxelSizePanel = new VoxelSizePanel(BigTraceData.globCal,btdata.sVoxelUnit);
		voxelSizePanel.addVoxelSizePanelListener(new VoxelSizePanel.Listener() {
			
			@Override
			public void voxelSizeChanged(double [] newVoxelSize) {
		
				voxelChanged(newVoxelSize);
			}
		});
		
		//CROP PANEL
		cropPanel = new CropPanel(btdata.nDimIni[1]);
		
		cropPanel.addCropPanelListener(new CropPanel.Listener() {
			@Override
			public void boundingBoxChanged(long [][] new_box) {
				bbChanged(new_box);				
				}
		});
		
		
		GridBagConstraints c = new GridBagConstraints();

		
		//VIEW PANEL
		JPanel panView=new JPanel(new GridBagLayout()); 
		panView.setBorder(new PanelTitle(" View "));		
		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/orig.png");
	    ImageIcon tabIcon = new ImageIcon(icon_path);
	    JToggleButton butOrigin = new JToggleButton(tabIcon);
	    butOrigin.setSelected(btdata.bShowOrigin);
	    butOrigin.setToolTipText("Show XYZ axes");
	    butOrigin.addItemListener(new ItemListener() {
	
	    @Override
		public void itemStateChanged(ItemEvent e) {
	    	      if(e.getStateChange()==ItemEvent.SELECTED){
	    	    	  btdata.bShowOrigin=true;
	    	    	  bt.repaintBVV();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	btdata.bShowOrigin=false;
	    	        	bt.repaintBVV();
	    	        }
			}
	    	});
	    c.gridx=0;
	    c.gridy=0;
		panView.add(butOrigin,c);
		
		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/boxvolume.png");
	    tabIcon = new ImageIcon(icon_path);
	    JToggleButton butVBox = new JToggleButton(tabIcon);
	    butVBox.setSelected(btdata.bVolumeBox);
	    butVBox.setToolTipText("Volume Box");
	    butVBox.addItemListener(new ItemListener() {
	
	    @Override
		public void itemStateChanged(ItemEvent e) {
	    	      if(e.getStateChange()==ItemEvent.SELECTED){
	    	    	  btdata.bVolumeBox=true;
	    	    	  bt.repaintBVV();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	btdata.bVolumeBox=false;
	    	        	bt.repaintBVV();
	    	        }
			}
	    	});
	    c.gridx++;
	    
		panView.add(butVBox,c);
		
		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
	    tabIcon = new ImageIcon(icon_path);
	    butSettings = new JButton(tabIcon);
	    //butWorld.setSelected(btdata.bShowWorldGrid);
	    butSettings.setToolTipText("Settings");
	    butSettings.addActionListener(this);
	    
	    c.gridx++;
		panView.add(butSettings,c);
		
		
		//Render method panel
	    JPanel panRender=new JPanel(new GridBagLayout()); 
	    panRender.setBorder(new PanelTitle(" Render method "));
		
	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.fill=GridBagConstraints.HORIZONTAL;
	    panRender.add(renderMethodPanel,c);
		
		//Voxel size panel
	    JPanel panVoxel=new JPanel(new GridBagLayout()); 
	    panVoxel.setBorder(new PanelTitle(" Voxel size "));
		
	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.fill=GridBagConstraints.HORIZONTAL;
	    panVoxel.add(voxelSizePanel,c);
		
		
		//Cropping Panel
		JPanel panCrop=new JPanel(new GridBagLayout()); 
		panCrop.setBorder(new PanelTitle(" Crop "));

	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.fill=GridBagConstraints.HORIZONTAL;
	    panCrop.add(cropPanel,c);
	    
	    


	    //add panels to Navigation
	    c.insets=new Insets(4,4,2,2);
	    //View
	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.gridwidth=1;
	    c.anchor = GridBagConstraints.WEST;
	    panNavigation.add(panView,c);
	    
	    //Render method size
	    c.gridy++;	
	    panNavigation.add(panRender,c);
	    
	    //Voxel size
	    c.gridy++;	
	    panNavigation.add(panVoxel,c);
	    
	    
	    //Crop
	    c.gridy++;	
	    panNavigation.add(panCrop,c);
	    

	    
        // Blank/filler component
	    c.gridx++;
	    c.gridy++;
	    c.weightx = 0.01;
        c.weighty = 0.01;
        panNavigation.add(new JLabel(), c);

		return panNavigation;
		
	}
	public void dialSettings()
	{
		JPanel pViewSettings = new JPanel(new GridLayout(0,2,0,0));
		
		GridBagConstraints cd = new GridBagConstraints();
	

		
		NumberField nfClickArea = new NumberField(4);
		nfClickArea.setIntegersOnly(true);
		nfClickArea.setText(Integer.toString(bt.btdata.nHalfClickSizeWindow*2));
		
		NumberField nfAnimationDuration = new NumberField(5);
		nfAnimationDuration.setIntegersOnly(true);
		nfAnimationDuration.setText(Long.toString(bt.btdata.nAnimationDuration));
		
		JCheckBox cbZoomCrop = new JCheckBox();
		cbZoomCrop.setSelected(bt.btdata.bZoomCrop);
		
		
		NumberField nfZoomBoxSize = new NumberField(4);
		nfZoomBoxSize.setIntegersOnly(true);
		nfZoomBoxSize.setText(Integer.toString(bt.btdata.nZoomBoxSize));
		nfZoomBoxSize.setMaximumSize(nfZoomBoxSize.getPreferredSize());

		NumberField nfZoomBoxScreenFraction = new NumberField(4);
		nfZoomBoxScreenFraction.setText(Double.toString(bt.btdata.dZoomBoxScreenFraction));
		nfZoomBoxScreenFraction.setMaximumSize(nfZoomBoxScreenFraction.getPreferredSize());
		
		NumberField nfCamera = new NumberField(4);
		nfCamera.setText(Double.toString(bt.btdata.dCam));
		
		NumberField nfClipNear = new NumberField(4);
		nfClipNear.setText(Double.toString(bt.btdata.dClipNear));
		
		NumberField nfClipFar = new NumberField(4);
		nfClipFar.setText(Double.toString(bt.btdata.dClipFar));
	
		cd.gridx=0;
		cd.gridy=0;	
		pViewSettings.add(new JLabel("Snap area size on click (screen px): "),cd);
		cd.gridx++;
		pViewSettings.add(nfClickArea,cd);
	
		cd.gridx=0;
		cd.gridy++;
		pViewSettings.add(new JLabel("Transform animation duration (ms): "),cd);
		cd.gridx++;
		pViewSettings.add(nfAnimationDuration,cd);

		//cd.gridx=0;
		//cd.gridy++;
		//pViewSettings.add(new JLabel("Zoom settings: "),cd);

		
		cd.gridx=0;
		cd.gridy++;
		pViewSettings.add(new JLabel("Crop volume when zooming in? "),cd);
		cd.gridx++;
		pViewSettings.add(cbZoomCrop,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pViewSettings.add(new JLabel("Zoom box volume size (px): "),cd);
		cd.gridx++;
		pViewSettings.add(nfZoomBoxSize,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pViewSettings.add(new JLabel("Zoom box screen fraction (0-1): "),cd);
		cd.gridx++;
		pViewSettings.add(nfZoomBoxScreenFraction,cd);
		
		//cd.gridx=0;
		//cd.gridy++;
		//pViewSettings.add(new JLabel("Render box: "),cd);
		
		cd.gridx=0;
		cd.gridy++;
		pViewSettings.add(new JLabel("Camera position: "),cd);
		cd.gridx++;
		pViewSettings.add(nfCamera,cd);
		
		cd.gridx=0;
		cd.gridy++;
		pViewSettings.add(new JLabel("Near clipping plane position: "),cd);
		cd.gridx++;
		pViewSettings.add(nfClipNear,cd);
		cd.gridx=0;
		cd.gridy++;
		pViewSettings.add(new JLabel("Far clipping plane position: "),cd);
		cd.gridx++;
		pViewSettings.add(nfClipFar,cd);
		
		
		int reply = JOptionPane.showConfirmDialog(null, pViewSettings, "View/Navigation Settings", 
		        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);


		if (reply == JOptionPane.OK_OPTION) 
		{
			
			bt.btdata.nHalfClickSizeWindow = (int)(0.5*Integer.parseInt(nfClickArea.getText()));
			Prefs.set("BigTrace.nHalfClickSizeWindow",bt.btdata.nHalfClickSizeWindow);

			bt.btdata.nAnimationDuration = Integer.parseInt(nfAnimationDuration.getText());
			Prefs.set("BigTrace.nAnimationDuration", bt.btdata.nAnimationDuration);

			//ZOOM BOX
			bt.btdata.bZoomCrop = cbZoomCrop.isSelected();
			Prefs.set("BigTrace.bZoomCrop", bt.btdata.bZoomCrop );

			bt.btdata.nZoomBoxSize = Integer.parseInt(nfZoomBoxSize.getText());
			Prefs.set("BigTrace.nZoomBoxSize", bt.btdata.nZoomBoxSize);
			
			bt.btdata.dZoomBoxScreenFraction = Double.parseDouble(nfZoomBoxScreenFraction.getText());
			Prefs.set("BigTrace.dZoomBoxScreenFraction", (double)(bt.btdata.dZoomBoxScreenFraction));
			
			bt.btdata.dCam = Math.abs(Double.parseDouble(nfCamera.getText()));
			bt.btdata.dClipNear = Math.abs(Double.parseDouble(nfClipNear.getText()));
			bt.btdata.dClipFar = Math.abs(Double.parseDouble(nfClipFar.getText()));
			if(bt.btdata.dCam<=bt.btdata.dClipNear)
				bt.btdata.dCam = bt.btdata.dClipNear+1;
			
			Prefs.set("BigTrace.dCam", (double)(bt.btdata.dCam));
			Prefs.set("BigTrace.dClipNear", (double)(bt.btdata.dClipNear));
			Prefs.set("BigTrace.dClipFar", (double)(bt.btdata.dClipFar));

			bt.panel.setCamParams(bt.btdata.dCam, bt.btdata.dClipNear, bt.btdata.dClipFar);

			bt.repaintBVV();
		}
	}

	
	public JPanel panelInformation()
	{
		JPanel panInformation = new JPanel(new GridLayout());
		panInformation.setBorder(new PanelTitle(" Main keys "));
		String shortCutInfo ="<html><center><b>ROI Manager</b></center><br>"
					+"&nbsp;<b>F</b> - add new point/start/continue<br>"
					+"&nbsp;<b>G</b> - delete last point<br>"
					+"&nbsp;<b>H</b> - finish tracing (deselect)<br>"
					+"&nbsp;<b>E</b> - select ROI in BVV<br>"
					+"&nbsp;<b>Y</b> - reverse active trace end<br>"
					+"&nbsp;<b>T</b> - advance tracebox<br>"
					+"&nbsp;<b>R</b> - straight line (in trace mode)<br>"
					+"&nbsp;<b>V</b> - new trace (in trace mode)<br>"
					 +"<br>"
					+"<center><b>View/Navigation</b></center><br>"
					+"&nbsp;<b>S</b> - brightness/color<br>"
					+"&nbsp;<b>D</b> - zoom in to a point<br>"
					+"&nbsp;<b>C</b> - center the view (zoom out)<br>"
					+"&nbsp;<b>X</b> - reset crop<br>"
					+"&nbsp;<b>P</b> - toggle render method<br>"
					+"&nbsp;<b>1</b> - show centered XY view<br>"
					+"&nbsp;<b>2</b> - show centered YZ view<br>"
					+"&nbsp;<b>3</b> - show centered XZ view<br>"
					+"&nbsp;<b>F6</b> - sources </html>";
		JLabel jlInfo = new JLabel(shortCutInfo);
		jlInfo.setVerticalAlignment(SwingConstants.CENTER);
		jlInfo.setHorizontalAlignment(SwingConstants.CENTER);
		panInformation.add(jlInfo);
		return panInformation;
	}
	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{
	        if ("progress" == evt.getPropertyName()) 
	        {
	            int progress = (Integer) evt.getNewValue();
	            if (progress == 0) 
	            {
	            	progressBar.setIndeterminate(true);
	            }
	            else
	            {
	            	progressBar.setIndeterminate(false);
	            	progressBar.setValue(progress);
	            }
	            
	            if(evt.getSource() instanceof bigtrace.BigTraceBGWorker)
	            {
	            	progressBar.setString(((bigtrace.BigTraceBGWorker)(evt.getSource())).getProgressState());
	            }
	        }
	        
	        
	        
	}

	public synchronized void bbChanged(long [][] box )
	{
		int i;
		
		// not sure we really need to check it, but just in case...
		boolean bWithinRange = true;
		for(i=0;i<3;i++)
		{
			if(box[0][i]<btdata.nDimIni[0][i])
			{
				bWithinRange = false;
				break;
			}
			if(box[1][i]>btdata.nDimIni[1][i])
			{
				bWithinRange = false;
				break;
			}
				
		}		
		
		boolean bNewOne = false;
		for(i=0;i<3;i++)
		{
			for(int j=0;j<2;j++)
			{
				if(btdata.nDimCurr[j][i]!=box[j][i])
				{
					bNewOne = true;
					break;
				}
			}
			if(bNewOne)
				break;
		}
		
		if(bWithinRange && bNewOne)
		{
				for(i=0;i<3;i++)
				{
					btdata.nDimCurr[0][i]=box[0][i];
					btdata.nDimCurr[1][i]=box[1][i];
				}
				
				updateViewDataSources();
			}
	}
	
	
	/** updates data sources/bvvsources to the current state**/
	@SuppressWarnings("rawtypes")
	public void updateViewDataSources()
	{
		int i;
		//update data sources
		for(i=0;i<bt.btdata.nTotalChannels;i++)
		{
				bt.sources.set(i,btdata.getDataSourceCropped(i, btdata.nCurrTimepoint));
		}

		//update bvv sources crop
		double [][] doubleCrop = new double [2][3];
		for (i=0;i<3;i++)
			for(int j=0;j<2;j++)
			    doubleCrop[j][i] = (double)btdata.nDimCurr[j][i];

		final FinalRealInterval cropInt = new FinalRealInterval(doubleCrop[0],doubleCrop[1]);
		for(i=0;i<bt.bvv_sources.size();i++)
		{
			((BvvStackSource)bt.bvv_sources.get(i)).setCropInterval(cropInt);
		}		
	}
	
	synchronized void voxelChanged(double [] newVoxelSize)
	{
		
		//change the scale of the volume
		final AffineTransform3D transform = new AffineTransform3D();
		
		
		final AffineTransform3D newtransform = new AffineTransform3D();
		bt.panel.state().getViewerTransform(transform);

		
		double[] scaleChange = new double [3];
		
		for (int d = 0;d<3; d++)
		{
			scaleChange[d] = Affine3DHelpers.extractScale( transform, d );
			scaleChange[d] /= BigTraceData.globCal[d];
			BigTraceData.globCal[d]=newVoxelSize[d];
			//IJ.log("voxel "+Integer.toString(d)+" "+Double.toString(newVoxelSize[d]));
			scaleChange[d]*=BigTraceData.globCal[d];
		}
		BigTraceData.dMinVoxelSize = Math.min(Math.min(BigTraceData.globCal[0], BigTraceData.globCal[1]), BigTraceData.globCal[2]);
		final double[][] Rcurrent = new double[ 3 ][ 3 ];
		double[] qStart = new double[ 4 ];
		Affine3DHelpers.extractRotationAnisotropic( transform, qStart );
		LinAlgHelpers.quaternionToR( qStart, Rcurrent );

		
		final double[][] m = new double[ 3 ][ 4 ];
		for ( int r = 0; r < 3; ++r )
		{
			for ( int c = 0; c < 3; ++c )
				m[ r ][ c ] = scaleChange[c] * Rcurrent[ r ][ c ];
			m[ r ][ 3 ] = transform.get(r, 3);
		}
		newtransform.set(m);
		
		AffineTransform3D final_transform = bt.getCenteredViewTransform(newtransform, bt.sources.get(btdata.nChAnalysis), 1.0);
		bt.panel.setTransformAnimator(new AnisotropicTransformAnimator3D(transform,final_transform ,0,0,btdata.nAnimationDuration));
		//recalculate ROI shapes
		bt.roiManager.updateROIsDisplay();
	}
	
	@SuppressWarnings("rawtypes")
	public void setRenderMethod(int nRenderType)
	{
		btdata.nRenderMethod = nRenderType;
		Prefs.set("BigTrace.nRenderMethod",btdata.nRenderMethod);
		for(int i=0;i<bt.bvv_sources.size();i++)
		{
			((BvvStackSource)bt.bvv_sources.get(i)).setRenderType(nRenderType);
		}	
		if(bt.bvv_trace!=null)
			bt.bvv_trace.setRenderType(nRenderType);
		
		
	}
	@Override
	public void actionPerformed(ActionEvent e) {

		//SETTINGS
		if(e.getSource() == butSettings)
		{
			dialSettings();
		}
		
	}


}

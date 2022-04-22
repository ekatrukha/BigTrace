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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import bdv.util.Affine3DHelpers;
import bigtrace.gui.AnisotropicTransformAnimator3D;
import bigtrace.gui.CropPanel;

import bigtrace.gui.PanelTitle;
import bigtrace.gui.VoxelSizePanel;
import bigtrace.rois.RoiManager3D;
import bigtrace.BigTraceData;
import bvv.util.Bvv;
import bvv.util.BvvFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;

public class BigTraceControlPanel extends JPanel
//public class BigTraceControlPanel extends JFrame
								implements ActionListener, 
									PropertyChangeListener {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8992158095263652259L;
	
	BigTrace btrace;
	BigTraceData btdata;	
	
	public CropPanel cropPanel;	
	public VoxelSizePanel voxelSizePanel;
	
	RoiManager3D roiManager;
	

	double [][] nDisplayMinMax;

	public JFrame bvv_frame;
	public JFrame finFrame;
	JProgressBar progressBar;

	
	public BigTraceControlPanel(final BigTrace bt_,final BigTraceData btd_, final RoiManager3D roiManager_)//, int locx, int locy) 
	{
		//finalPanel = new JPanel(new GridBagLayout());
		super(new GridBagLayout());
		roiManager=roiManager_;
	
		btdata=btd_;
		btrace=bt_;
		
		
		
		//Interface
		
		//CropPanel
		//cropPanel = new CropPanel( nW-1, nH-1, nD-1);
		cropPanel = new CropPanel(btdata.nDimIni[1]);
		
		cropPanel.addCropPanelListener(new CropPanel.Listener() {
			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				bbChanged(bbx0, bby0, bbz0,  bbx1,  bby1,  bbz1);				
				}
		});
		
		//VOXEL SIZE PANEL		
		voxelSizePanel = new VoxelSizePanel(btdata.globCal,btdata.sVoxelUnit);
		voxelSizePanel.addVoxelSizePanelListener(new VoxelSizePanel.Listener() {
			
			@Override
			public void voxelSizeChanged(double [] newVoxelSize) {
		
				voxelChanged(newVoxelSize);
			}
		});
		
		
		JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);
		
		JPanel panNavigation = new JPanel(new GridBagLayout());

	    
		GridBagConstraints c = new GridBagConstraints();

		
		//View Panel
	  
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
	    	    	  //render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	btdata.bShowOrigin=false;
	    	        	//render_pl();
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
	    	    	  //render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	btdata.bVolumeBox=false;
	    	        	//render_pl();
	    	        }
			}
	    	});
	    c.gridx++;
	    
		panView.add(butVBox,c);
		icon_path = bigtrace.BigTrace.class.getResource("/icons/worldgrid.png");
	    tabIcon = new ImageIcon(icon_path);
	    JToggleButton butWorld = new JToggleButton(tabIcon);
	    butWorld.setSelected(btdata.bShowWorldGrid);
	    butWorld.setToolTipText("World Grid");
	    butWorld.addItemListener(new ItemListener() {
	
	    @Override
		public void itemStateChanged(ItemEvent e) {
	    	      if(e.getStateChange()==ItemEvent.SELECTED){
	    	    	  btdata.bShowWorldGrid=true;
	    	    	  //render_pl();
	    	        } else if(e.getStateChange()==ItemEvent.DESELECTED){
	    	        	btdata.bShowWorldGrid=false;
	    	        	//render_pl();
	    	        }
			}
	    	});
	    c.gridx++;
		panView.add(butWorld,c);
		
		
		
		//Cropping Panel
		JPanel panCrop=new JPanel(new GridBagLayout()); 
		panCrop.setBorder(new PanelTitle(" Cropping "));

	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.fill=GridBagConstraints.HORIZONTAL;
	    panCrop.add(cropPanel,c);
	    
	    
		//Voxel size panel
	    JPanel panVoxel=new JPanel(new GridBagLayout()); 
	    panVoxel.setBorder(new PanelTitle(" Voxel size "));
		
	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.fill=GridBagConstraints.HORIZONTAL;
	    panVoxel.add(voxelSizePanel,c);


	    /*
		NumberField nfXSize = new NumberField(5);
		NumberField nfYSize = new NumberField(5);
		NumberField nfZSize = new NumberField(5);

		JTextField tfUnits = new JTextField(8);
		

		
		nfXSize.setText(String.format("%.3f", btdata.globCal[0]));
		nfYSize.setText(String.format("%.3f", btdata.globCal[1]));
		nfZSize.setText(String.format("%.3f", btdata.globCal[2]));
		tfUnits.setText(btdata.sVoxelUnit);
		
		tfUnits.setHorizontalAlignment(SwingConstants.LEFT);
	    
	    c.gridx=0;
	    c.gridy=0;
	    panVoxel.add(new JLabel("Pixel width"),c);
	    c.gridx++;
	    panVoxel.add(nfXSize,c);
	    c.gridx=0;
	    c.gridy++;
	    panVoxel.add(new JLabel("Pixel height"),c);
	    c.gridx++;
	    panVoxel.add(nfYSize,c);
	    c.gridx=0;
	    c.gridy++;
	    panVoxel.add(new JLabel("Voxel depth"),c);
	    c.gridx++;
	    panVoxel.add(nfZSize,c);
	    c.gridx=0;
	    c.gridy++;
	    panVoxel.add(new JLabel("Units"),c);
	    c.gridx++;
	    panVoxel.add(tfUnits,c);	
	    */	
	    //add panels to Navigation
	    c.insets=new Insets(4,4,2,2);
	    //View
	    c.gridx=0;
	    c.gridy=0;
	    c.weightx=1.0;
	    c.gridwidth=1;
	    c.anchor = GridBagConstraints.WEST;
	    panNavigation.add(panView,c);
	    
	    //Crop
	    c.gridy++;	
	    panNavigation.add(panCrop,c);
	    
	    //Voxel size
	    c.gridy++;	
	    panNavigation.add(panVoxel,c);
	    
        // Blank/filler component
	    c.gridx++;
	    c.gridy++;
	    c.weightx = 0.01;
        c.weighty = 0.01;
        panNavigation.add(new JLabel(), c);
        
        
        icon_path = bigtrace.BigTrace.class.getResource("/icons/cube_icon.png");
	    tabIcon = new ImageIcon(icon_path);

	    tabPane.addTab("",tabIcon,panNavigation, "View/Crop");

	    //ROI MANAGER
	    icon_path = bigtrace.BigTrace.class.getResource("/icons/node.png");
	    tabIcon = new ImageIcon(icon_path);
	    tabPane.addTab("",tabIcon ,roiManager,"Tracing");
	    
	    icon_path = bigtrace.BigTrace.class.getResource("/icons/shortcut.png");
	    tabIcon = new ImageIcon(icon_path);
	    tabPane.addTab("",tabIcon ,informationPanel(),"Help/Shortcuts");

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
	    progressBar.setString("BigTrace version "+btdata.sVersion);
		
	    //JPanel finalPanel = new JPanel(new GridBagLayout());
	    GridBagConstraints cv = new GridBagConstraints();
	    cv.gridx=0;
	    cv.gridy=0;	    
	    cv.weightx=0.5;
	    cv.weighty=1.0;
	    cv.anchor = GridBagConstraints.NORTHWEST;
	    cv.gridwidth = GridBagConstraints.REMAINDER;
	    //cv.gridheight = GridBagConstraints.REMAINDER;
	    cv.fill = GridBagConstraints.HORIZONTAL;
	    cv.fill = GridBagConstraints.BOTH;
	    //finalPanel.add(tabPane,cv);
	    this.add(tabPane,cv);
	    cv.gridx=0;
	    cv.gridy=1;	    
	    cv.gridwidth = GridBagConstraints.RELATIVE;
	    cv.gridheight = GridBagConstraints.RELATIVE;
	    cv.weighty=0.01;
	    cv.anchor = GridBagConstraints.SOUTHEAST;
	    cv.fill = GridBagConstraints.HORIZONTAL;
	    //finalPanel.add(tabPane,cv);
	    this.add(progressBar,cv);
	    
	    //JFrame bvv_frame=(JFrame) SwingUtilities.getWindowAncestor(bvv.getBvvHandle().getViewerPanel());
	    //final JDialog mainWindow = new JDialog(bvv_frame,"BigTrace");
	    
	    //mainWindow.add(finalPanel);
	    //mainWindow.setVisible(true);
	    //mainWindow.setSize(400, 500);
	    //mainWindow.requestFocusInWindow();
	    //final JFrame frame = new JFrame( "BigTrace" );
	    //roiManager.setParentFrame(frame);
		//this.add(finalPanel);
	    //this.setSize(400,500);

	    //java.awt.Point bvv_p = bvv_frame.getLocationOnScreen();
	    //java.awt.Dimension bvv_d = bvv_frame.getSize();
	
	    //frame.setLocation(bvv_p.x+bvv_d.width, bvv_p.y);
	    //this.setLocation(locx, locy);

	    
	    //frame.setVisible( true );
		//frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		/*frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	{
		    		
		    		//panel.setRenderScene(null);
		    		//panel.setVisible(false);		    				    		
		    		//bvv.close();
		            //System.exit(0);
		        }
		    }
		});*/
	    
	    //install actions from BVV
	    
	    this.setActionMap(btrace.actions.getActionMap());
	    this.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,btrace.actions.getInputMap());
	    
	}
	
	public JPanel informationPanel()
	{
		JPanel panInformation = new JPanel(new GridLayout());
		panInformation.setBorder(new PanelTitle(" Main keys "));
		String shortCutInfo ="<html><center><b>ROI Manager</b></center><br>"
					+"&nbsp;<b>F</b> - add new point/start/continue<br>"
					+"&nbsp;<b>G</b> - delete last point<br>"
					+"&nbsp;<b>H</b> - finish tracing (deselect)<br>"
					+"&nbsp;<b>T</b> - advance tracebox<br>"
					+"&nbsp;<b>Y</b> - reverse active trace end<br>"
					+"&nbsp;<b>R</b> - straight line (in trace mode)<br>"
					 +"<br>"
					+"<center><b>View/Navigation</b></center><br>"
					+"&nbsp;<b>1</b> - show centered XY view<br>"
					+"&nbsp;<b>2</b> - show centered YZ view<br>"
					+"&nbsp;<b>D</b> - zoom in to a point<br>"
					+"&nbsp;<b>C</b> - zoom out <br>"
					+"&nbsp;<b>S</b> - brightness/color<br>"
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

	synchronized void bbChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1)
	{
		if(bbx0>=0 && bby0>=0 &&bbz0>=0 && bbx1<=(btdata.nDimIni[1][0]) && bby1<=(btdata.nDimIni[1][1]) && bbz1<=(btdata.nDimIni[1][2]))
		{

				int i;
				btdata.nDimCurr[0]=new long[] { bbx0, bby0, bbz0 };
				btdata.nDimCurr[1]=new long[] { bbx1, bby1, bbz1 };
				
				//save display range settings
				
				nDisplayMinMax = new double [btrace.btdata.nTotalChannels][2];
				for(i=0;i<btrace.btdata.nTotalChannels;i++)
				{
					if(btrace.bvv_sources.get(i)!=null)
					{
						nDisplayMinMax[i][0]=btrace.bvv_sources.get(i).getConverterSetups().get(0).getDisplayRangeMin();
						nDisplayMinMax[i][1]=btrace.bvv_sources.get(i).getConverterSetups().get(0).getDisplayRangeMax();
					}
				}
				//update sources
				if(btrace.btdata.nTotalChannels==1)
				{
					btrace.sources.set(0,Views.interval(btrace.img_in, btdata.nDimCurr[0], btdata.nDimCurr[1] ));
					
				}
				else
				{
					for(i=0;i<btrace.btdata.nTotalChannels;i++)
					{
						btrace.sources.set(i,Views.interval(Views.hyperSlice(btrace.img_in,2,i), btdata.nDimCurr[0], btdata.nDimCurr[1] ));
					}
				}
		
				//update bvv sources
				for(i=0;i<btrace.bvv_sources.size();i++)
				{		

					btrace.bvv_sources.get(i).removeFromBdv();
					btrace.bvv_sources.set(i, BvvFunctions.show( btrace.sources.get(i), "ch_"+Integer.toString(i+1), Bvv.options().addTo(btrace.bvv_main)));
					btrace.bvv_sources.get(i).setDisplayRange(nDisplayMinMax[i][0], nDisplayMinMax[i][1]);
					btrace.bvv_sources.get(i).setColor( new ARGBType(btrace.colorsCh[i].getRGB() ));
					btrace.bvv_sources.get(i).setDisplayRangeBounds(0, 255);
				}	
				/**/					

				//just in case
				System.gc();
				
			}
	}
	
	
	synchronized void voxelChanged(double [] newVoxelSize)
	{
		
		//change the scale of the volume
		final AffineTransform3D transform = new AffineTransform3D();
		
		
		final AffineTransform3D newtransform = new AffineTransform3D();
		btrace.panel.state().getViewerTransform(transform);

		
		double[] scaleChange = new double [3];
		
		for (int d = 0;d<3; d++)
		{
			scaleChange[d] = Affine3DHelpers.extractScale( transform, d );
			scaleChange[d] /= btdata.globCal[d];
			btdata.globCal[d]=newVoxelSize[d];
			scaleChange[d]*=btdata.globCal[d];
		}
		
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
		btrace.panel.setTransformAnimator(new AnisotropicTransformAnimator3D(transform,newtransform,0,0,1000));
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}


}

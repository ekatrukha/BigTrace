package bigtrace;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import org.scijava.ui.behaviour.util.Actions;

import bigtrace.gui.CropPanel;
import bigtrace.gui.PanelTitle;
import bigtrace.rois.RoiManager3D;
import bigtrace.BigTraceData;
import bvv.util.Bvv;
import bvv.util.BvvFunctions;
import net.imglib2.type.numeric.ARGBType;
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
	
	CropPanel cropPanel;	
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

		/*	@Override
			public void nearFarChanged(int near, int far) {
				// TO DO Auto-generated method stub
				//VolumeViewer
				dClipNear = Math.abs(near);
				dClipFar = (double)far;
				bvv.getBvvHandle().getViewerPanel().setCamParams(dCam, dClipNear, dClipFar);
				handl.requestRepaint();
				//handl.state().setViewerTransform(transform);
				
			}
			*/

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				
				bbChanged(bbx0, bby0, bbz0,  bbx1,  bby1,  bbz1);		

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

        icon_path = bigtrace.BigTrace.class.getResource("/icons/cube_icon.png");
	    tabIcon = new ImageIcon(icon_path);

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
	    
	    //Crop
	    c.gridy++;	
	    panNavigation.add(panCrop,c);
	    
        // Blank/filler component
	    c.gridx++;
	    c.gridy++;
	    c.weightx = 0.01;
        c.weighty = 0.01;
        panNavigation.add(new JLabel(), c);

	    tabPane.addTab("",tabIcon,panNavigation, "View/Crop");

	    //ROI MANAGER
	    icon_path = bigtrace.BigTrace.class.getResource("/icons/node.png");
	    tabIcon = new ImageIcon(icon_path);
	    tabPane.addTab("",tabIcon ,roiManager,"Tracing");

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
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}


}

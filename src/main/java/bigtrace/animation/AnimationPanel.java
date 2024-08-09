package bigtrace.animation;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;
import bigtrace.gui.NumberField;
import bigtrace.gui.PanelTitle;

public class AnimationPanel < T extends RealType< T > & NativeType< T > > extends JPanel implements ListSelectionListener,  NumberField.Listener, ChangeListener, ActionListener
{
	final BigTrace<T> bt;
	
	final JButton butRecord;
	//final JButton butUncoil;
	final JButton butSettings;
	final JSlider timeSlider;
	
	final JButton butAdd;
	final JButton butReplace;
	final JButton butEdit;
	final JButton butDelete;
	
	//keyFrame list
	final public DefaultListModel<KeyFrame> listModel; 
	final public JList<KeyFrame> jlist;
	final JScrollPane listScroller;
	
	final DrawKeyPoints keyMarks;
	
	NumberField nfTotalTime;
	
	final KeyFrameAnimation<T> kfAnim;
	
	int tsSpan = 100;

	public AnimationPanel(final BigTrace<T> bt)
	{
		this.bt = bt;
		
		int nInitialTotalTime = 5;
		
		listModel = new  DefaultListModel<>();
		jlist = new JList<>(listModel);
		
		kfAnim = new KeyFrameAnimation<>(bt,listModel);
		kfAnim.setTotalTime( nInitialTotalTime );
		
		JPanel panAnimTools = new JPanel(new GridBagLayout());  
		panAnimTools.setBorder(new PanelTitle(" Animation "));
		
		int nButtonSize = 40;		
		GridBagConstraints cr = new GridBagConstraints();

		
		URL icon_path = bigtrace.BigTrace.class.getResource("/icons/camera.png");
		ImageIcon tabIcon = new ImageIcon(icon_path);		
		butRecord = new JButton(tabIcon);
		butRecord.setToolTipText("Record video");
		butRecord.setPreferredSize(new Dimension(nButtonSize , nButtonSize ));
		
		icon_path = bigtrace.BigTrace.class.getResource("/icons/settings.png");
		tabIcon = new ImageIcon(icon_path);
		butSettings = new JButton(tabIcon);
		butSettings.setToolTipText("Settings");
		butSettings.setPreferredSize(new Dimension(nButtonSize, nButtonSize));
		
		butRecord.addActionListener( this );
		butSettings.addActionListener( this );
				
		cr.gridx=0;
		cr.gridy=0;
		panAnimTools.add(butRecord,cr);
		
		cr.gridx++;
		JSeparator sp = new JSeparator(SwingConstants.VERTICAL);
		sp.setPreferredSize(new Dimension((int) (nButtonSize*0.5),nButtonSize));
		panAnimTools.add(sp,cr);
		//filler
		cr.gridx++;
		cr.weightx = 0.01;
		panAnimTools.add(new JLabel(), cr);
		cr.gridx++;
		cr.weightx = 0.0;
		panAnimTools.add(butSettings,cr);
		
		
		JPanel panAnimPlot = new JPanel(new GridBagLayout());
		panAnimPlot.setBorder(new PanelTitle(" Key Frames "));
		
		JPanel sliderPanel = new JPanel(new BorderLayout());
		//sliderPanel.setPreferredSize(new Dimension(50, 1250));
		
		timeSlider = new JSlider(SwingConstants.VERTICAL,0,tsSpan,1);
		
		timeSlider.setInverted( true );
		setSliderTotalTime();
		timeSlider.setValue(0);
		
		timeSlider.setPaintTicks(true);
		timeSlider.setPaintLabels(true);
		timeSlider.addChangeListener( this );

		//timeSlider.setMinimumSize(new Dimension(50, 1250));
		//timeSlider.setPreferredSize(new Dimension(50, 1250));
		sliderPanel.add(timeSlider);
		
		cr = new GridBagConstraints();
		cr.gridx=0;
		cr.gridy=0;
		cr.gridheight = 5;
		//cr.gridheight = GridBagConstraints.REMAINDER;

		cr.fill  = GridBagConstraints.BOTH;
		//cr.weightx=1.00;
		cr.weighty=0.99;
		

		keyMarks = new DrawKeyPoints();
		keyMarks.setMinimumSize( new Dimension(30,250));
	    keyMarks.setPreferredSize( new Dimension(30,250));
		//keyMarks.setBorder(new PanelTitle(" Keys"));
		
		//cr.weightx=0.4;
		panAnimPlot.add( keyMarks,cr );
		cr.gridx++;
		panAnimPlot.add( sliderPanel,cr );
		
		cr.gridx++;
		///RoiLIST and buttons

		jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlist.setLayoutOrientation(JList.VERTICAL);
		jlist.setVisibleRowCount(-1);
		jlist.addListSelectionListener(this);
		jlist.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {

					// Double-click detected
					//int index = jlist.locationToIndex(evt.getPoint());
					bt.setScene( jlist.getSelectedValue().getScene());
					int nPos = Math.round( tsSpan*(jlist.getSelectedValue().fMovieTimePoint/kfAnim.getTotalTime()));
					timeSlider.setValue( nPos);
					//focusOnRoi(rois.get(index));
				} 
				if (SwingUtilities.isRightMouseButton(evt))
				{
				}
			}
		});
		
		
		
		listScroller = new JScrollPane(jlist);
		listScroller.setPreferredSize(new Dimension(170, 250));
		//listScroller.setMinimumSize(new Dimension(170, 250));
		
		
		
		
		cr.weightx=0.5;
		//cr.weighty=0.5;
		panAnimPlot.add(listScroller,cr);
		
		//BUTTONS
		cr = new GridBagConstraints();
		//cr.weightx = 0;
		//cr.weighty = 0;
		cr.gridy=0;
		cr.gridx=3;
		cr.fill = GridBagConstraints.NONE;

		cr.gridheight = 1;
		butAdd = new JButton("Add");
		butAdd.addActionListener(this);
		panAnimPlot.add( butAdd, cr );
		
		cr.gridy++;
		butReplace = new JButton("Replace");
		butReplace.addActionListener(this);
		panAnimPlot.add( butReplace, cr );

		cr.gridy++;
		butEdit = new JButton("Edit");
		butEdit.addActionListener(this);
		panAnimPlot.add( butEdit, cr );
		
		
		cr.gridy++;
		butDelete = new JButton("Delete");
		butDelete.addActionListener(this);
		panAnimPlot.add( butDelete, cr );
		
		// a solution for now
		final Dimension butDim = butReplace.getPreferredSize();
		butAdd.setMinimumSize(butDim);
		butAdd.setPreferredSize(butDim);
		butReplace.setMinimumSize(butDim); 
		butReplace.setPreferredSize(butDim); 
		butEdit.setMinimumSize(butDim);
		butEdit.setPreferredSize(butDim); 
		butDelete.setMinimumSize(butDim);
		butDelete.setPreferredSize(butDim); 
		
		
		// Blank/filler component
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		panAnimPlot.add(new JLabel(), cr);	
		
		
		JPanel panTotTime = new JPanel(new GridBagLayout());
		panTotTime.setBorder(new PanelTitle(""));
		cr = new GridBagConstraints();
		cr.gridx=0;
		cr.gridy=0;
		panTotTime.add(new JLabel("Total time (s)"),cr);
		cr.gridx++;
		nfTotalTime = new NumberField(4);
		nfTotalTime.setIntegersOnly( true );
		nfTotalTime.setText(Integer.toString( (int)Math.ceil( kfAnim.getTotalTime())));
		nfTotalTime.setMinimumSize(nfTotalTime.getPreferredSize());
		nfTotalTime.addListener(this);
		panTotTime.add(nfTotalTime,cr);
		
		
		//put all panels together
		cr = new GridBagConstraints();
		setLayout(new GridBagLayout());
		cr.insets = new Insets(4,4,2,2);
		cr.gridx = 0;
		cr.gridy = 0;
		cr.fill = GridBagConstraints.HORIZONTAL;

		//TOP BUTTONS MENU 
		add(panAnimTools,cr);
		
		//KEYFRAMES list
		cr.gridy++;
		cr.weighty = 0.99;
		cr.fill = GridBagConstraints.BOTH;
		add(panAnimPlot,cr);

		cr.gridy++;
		cr.weighty = 0.0;
		cr.fill = GridBagConstraints.BOTH;
		add(panTotTime,cr);
		
		
		// Blank/filler component
		cr.gridy++;
		cr.weightx = 0.01;
		cr.weighty = 0.01;
		add(new JLabel(), cr);    
	}
	
	@Override
	public void actionPerformed( ActionEvent e )
	{
		//add keyframe
		if(e.getSource() == butAdd)
		{
			addCurrentKeyFrame();
		}
		//replace keyframe
		if(e.getSource() == butReplace)
		{
			replaceSelectedKeyFrame();
		}
		//delete keyframe
		if(e.getSource() == butDelete)
		{
			deleteSelectedKeyFrame();
		}
		
	}

	@Override
	public void valueChanged( ListSelectionEvent arg0 )
	{

		
	}
	


	void addCurrentKeyFrame()
	{
		float nTimeMovie =  ((float)timeSlider.getValue()/(float)(tsSpan))*kfAnim.getTotalTime();
		KeyFrame newKeyFrame = new KeyFrame(bt.getCurrentScene(),nTimeMovie);
		if(listModel.size()==0)
		{
			listModel.addElement(newKeyFrame);
		}
		else
		{
			boolean bAdded = false;
			for(int i = 0; i<listModel.size();i++)
			{
				if(listModel.get( i ).fMovieTimePoint>nTimeMovie)
				{
					listModel.add(i,newKeyFrame);
					bAdded = true;
					break;
				}
			}
			if(!bAdded)
			{
				listModel.addElement(newKeyFrame);
			}
		}
		updateKeyIndices();
		updateKeyMarks();
		kfAnim.updateTransitionTimeline();
		
	}
	
	void replaceSelectedKeyFrame()
	{
		int nInd = jlist.getSelectedIndex();
		if(nInd>=0)
		{
			float nTimeMovie = listModel.get( nInd ).fMovieTimePoint;
			KeyFrame newKeyFrame = new KeyFrame(bt.getCurrentScene(),nTimeMovie);
			listModel.set( nInd, newKeyFrame );
			kfAnim.updateTransitionTimeline();
		}
	}
	void deleteSelectedKeyFrame()
	{
		int nInd = jlist.getSelectedIndex();
		if(nInd>=0)
		{
			listModel.remove( nInd );
			updateKeyIndices();
			updateKeyMarks();
			kfAnim.updateTransitionTimeline();
			//updateScene();
		}
	}

	@Override
	public void valueChanged( double v )
	{
		
		int nNewTime= ( int ) Math.round( Math.abs( v ) );
		//int nOldTime = kfAnim.getTotalTime();
		kfAnim.setTotalTime(nNewTime );
		setSliderTotalTime();
		//check that keyframes are still in range
		for (int i=0;i<listModel.size(); i++)
		{
			if(listModel.get( i ).fMovieTimePoint>nNewTime)
			{
				listModel.get( i ).fMovieTimePoint = nNewTime;
			}
		}
		updateKeyMarks();
		kfAnim.updateTransitionTimeline();
	}
	
	public void setSliderTotalTime()
	{
		
		//int nTickTime = ( int ) Math.ceil( kfAnim.getTotalTime()/10. );
		int nTickTime = getTickTime();
		if( kfAnim.getTotalTime()<100)
			tsSpan = kfAnim.getTotalTime()*10;
		else
			tsSpan = kfAnim.getTotalTime();
		timeSlider.setMaximum( tsSpan );
		
		int oneTick = Math.round(nTickTime* tsSpan/kfAnim.getTotalTime() );
		timeSlider.setMajorTickSpacing(oneTick);
		//timeSlider.setMajorTickSpacing(oneSec);
		//timeSlider.setMinorTickSpacing(Math.round( oneSec/2 ));
		Hashtable< Integer, JLabel > labelTable = new Hashtable<>();
		
		for(int i = 0;i<=kfAnim.getTotalTime();i+=nTickTime)
		{
			int kk = i* tsSpan/kfAnim.getTotalTime();
			labelTable.put( new Integer(kk ), new JLabel(Integer.toString( i )) );	
		}
		//labelTable.put( new Integer(13 ), new JLabel("test") );	
		timeSlider.setLabelTable( labelTable );
	}
	
	int getTickTime()
	{
		int nTickTime = ( int ) Math.ceil( kfAnim.getTotalTime()/10. );
		int nDigits = Integer.toString( nTickTime ).length();
		int firstDigit = Integer.parseInt(Integer.toString(nTickTime).substring(0, 1));
		
		if(firstDigit==1)
			return ( int ) ( Math.pow( 10, nDigits-1 ) );
		if(firstDigit<4)
			return ( int ) ( 2*Math.pow( 10, nDigits-1 ) );
		if(firstDigit<=5)
			return ( int ) ( 5*Math.pow( 10, nDigits-1 ) );
		return ( int ) ( Math.pow( 10, nDigits ) );
		
	}
	

	void updateKeyMarks()
	{
		ArrayList<Float> keyPoints = new ArrayList<>();
		for (int i=0;i<listModel.size();i++)
		{
			keyPoints.add( listModel.get( i ).fMovieTimePoint/ kfAnim.getTotalTime());
		}
		keyMarks.setkeyPoints( keyPoints );
		keyMarks.repaint();
	}
	
	void updateKeyIndices()
	{
		for(int i=0;i<listModel.size(); i++)
		{
			listModel.get( i ).nIndex = i;
		}
		
	}
	
	class DrawKeyPoints extends JPanel
	{
		
		boolean bLocked = false;
		
		/** values as fraction of total time **/
		ArrayList<Float> keyPoints = new ArrayList<>();
		
		DrawKeyPoints()
		{
			super();
		}
		
		public void setkeyPoints(ArrayList<Float> keyPoints_)
		{
			if(bLocked)
			{
				while (bLocked)
				{
					try
					{
						Thread.sleep( 1 );
					}
					catch ( InterruptedException exc )
					{
						exc.printStackTrace();
					}
				}
			}	
			bLocked = true;
			keyPoints = keyPoints_;
			bLocked = false;
		}

	    @Override
	    protected void paintComponent(Graphics g)
	    {
	    	
	        super.paintComponent(g);
	        
	        Rectangle bounds = this.getBounds();

	        Graphics2D g2 = (Graphics2D) g;
	        g2.setRenderingHint(
	                RenderingHints.KEY_TEXT_ANTIALIASING,
	                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	        

	        Font currentFont = g.getFont();
	        FontMetrics m = g.getFontMetrics(currentFont);
	        int nMaxAscent = m.getAscent();
	        int nMaxDescent = m.getDescent();
	        int nTotalRange = (int)bounds.getHeight() - nMaxDescent - nMaxAscent;
	        if(bLocked)
	        {
	    		while (bLocked)
	    		{
	    			try
	    			{
	    				Thread.sleep( 1 );
	    			}
	    			catch ( InterruptedException exc )
	    			{
	    				exc.printStackTrace();
	    			}
	    		}
	        }	
	        bLocked = true;
	        for(int i = 0; i<keyPoints.size();i++)
	        {
	        	//int h = Math.round( nMaxAscent+keyPoints.get( i ).floatValue()*nTotalRange);
	        	g.drawString("["+Integer.toString( i )+"]",0,Math.round( nMaxAscent+keyPoints.get( i ).floatValue()*nTotalRange));
	        	//g.drawString( "top",  0, nMaxAscent);
	        	//g.drawString( "bottom",  0, (int)(bounds.getHeight())-nMaxDescent);
	        }
	        bLocked = false;

	    }
	}

	void updateScene()
	{			
		if(listModel.size()>0)
		{
			float fTimePoint = (kfAnim.getTotalTime())*(timeSlider.getValue()/(float)tsSpan);
			bt.setScene( kfAnim.getScene( fTimePoint ) );
		}
	}
	@Override
	public void stateChanged( ChangeEvent e )
	{
		if(e.getSource()==timeSlider)
		{
			updateScene();
		}
		
	}
}

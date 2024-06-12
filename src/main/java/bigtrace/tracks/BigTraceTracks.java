package bigtrace.tracks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import bigtrace.BigTrace;


public class BigTraceTracks < T extends RealType< T > & NativeType< T > > extends JPanel implements ActionListener
{
	final BigTrace<T> bt;

	
	public BigTraceTracks(BigTrace<T> bt)
	{
		this.bt = bt;
	}
	@Override
	public void actionPerformed( ActionEvent e )
	{
		
		
	}

}

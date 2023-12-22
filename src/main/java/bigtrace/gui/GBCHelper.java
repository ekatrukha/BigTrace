package bigtrace.gui;

import java.awt.Insets;
import java.awt.GridBagConstraints;


public class GBCHelper {
	private static final Insets WEST_INSETS = new Insets(2, 0, 2, 2);
	private static final Insets EAST_INSETS = new Insets(2, 2, 2, 0);
	
	public static void alighLeft(final GridBagConstraints gbc)	
	{
		int x = gbc.gridx;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;

		gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
		gbc.fill = (x == 0) ? GridBagConstraints.BOTH
				: GridBagConstraints.HORIZONTAL;

		gbc.insets = (x == 0) ? WEST_INSETS : EAST_INSETS;
		gbc.weightx = (x == 0) ? 0.1 : 1.0;
		gbc.weighty = 1.0;
		
	}
	public static void alighLoose(final GridBagConstraints gbc)	
	{
		int x = gbc.gridx;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;

		gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
		//gbc.fill = (x == 0) ? GridBagConstraints.BOTH
		//		: GridBagConstraints.HORIZONTAL;

		gbc.insets = (x == 0) ? WEST_INSETS : EAST_INSETS;
		//gbc.weightx = (x == 0) ? 0.1 : 1.0;
		//gbc.weighty = 1.0;
		
	}
}

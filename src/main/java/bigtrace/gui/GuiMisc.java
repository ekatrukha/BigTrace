package bigtrace.gui;

import java.awt.Component;
import java.awt.Container;

public class GuiMisc {

	public static void setPanelStatusAllComponents(Container panel, boolean bStatus)
	{
		
		for(Component c : panel.getComponents())
		{
			if(c instanceof Component)
				setPanelStatusAllComponents((Container) c, bStatus);
			c.setEnabled(bStatus);
		}
	}
}

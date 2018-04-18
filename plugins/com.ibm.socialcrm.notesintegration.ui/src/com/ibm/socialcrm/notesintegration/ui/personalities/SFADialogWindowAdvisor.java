package com.ibm.socialcrm.notesintegration.ui.personalities;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;

import com.ibm.rcp.platform.personality.popup.PopupWindowAdvisor;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;

public class SFADialogWindowAdvisor extends PopupWindowAdvisor {

	public SFADialogWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);

		configurer.setShellStyle(SWT.RESIZE | SWT.TITLE | SWT.MIN | SWT.MAX | SWT.CLOSE | SWT.MODELESS);
	}

	/**
	 * set the new shell's default bound to -1000, -1000 to avoid flash, it need to be reset by the caller.
	 * 
	 */
	public void postWindowCreate() {
		IWorkbenchWindow window = getWindowConfigurer().getWindow();
		Shell shell = window.getShell();

		Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();
		int xPref = prefs.getInt(SugarItemsDashboard.WIDTH_PREFERENCE);
		int yPref = prefs.getInt(SugarItemsDashboard.HEIGHT_PREFERENCE);
		if (xPref == 0) {
			xPref = SugarItemsDashboard.DEFAULT_WIDTH;
		}
		if (yPref == 0) {
			yPref = SugarItemsDashboard.DEFAULT_HEIGHT;
		}
		shell.setSize(xPref, yPref);

		Monitor primary = Display.getDefault().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		int x = bounds.x + (bounds.width - xPref) / 2;
		int y = bounds.y + (bounds.height - yPref) / 2;

		shell.setLocation(new Point(x, y));
	}

}

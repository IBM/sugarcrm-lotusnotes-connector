package com.ibm.socialcrm.notesintegration.activitystream;

import com.ibm.rcp.toolbox.Widget;
import com.ibm.rcp.toolbox.internal.stores.PalleteStore;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.advisor.ISFAPerspectiveFactoryAdvisor;

public abstract class AbstractActivityStreamPerspectiveFactoryAdvisor implements ISFAPerspectiveFactoryAdvisor {

	public abstract String getViewPartId();

	public boolean isVisible() {

		boolean visible = false;
		
		Widget widgets[] = PalleteStore.getInstance().getPalleteItems();
		String activityStreamWidgetName = SugarWebservicesOperations.getInstance().getActivityStreamWidgetName();
		String[] serverWidgets = activityStreamWidgetName.split("\\|"); //$NON-NLS-1$
		for (Widget installedWidget : widgets) {
			for (String serverWidget : serverWidgets) {
				if (installedWidget.getTitle().equals(serverWidget)) {
					visible = true;
					break;
				}
			}
		}
		return visible;
	}

}
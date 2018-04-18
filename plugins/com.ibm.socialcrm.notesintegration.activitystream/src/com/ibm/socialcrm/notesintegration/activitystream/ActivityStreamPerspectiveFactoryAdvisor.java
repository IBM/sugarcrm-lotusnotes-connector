package com.ibm.socialcrm.notesintegration.activitystream;

import com.ibm.socialcrm.notesintegration.ui.advisor.ISFAPerspectiveFactoryAdvisor;

public class ActivityStreamPerspectiveFactoryAdvisor extends AbstractActivityStreamPerspectiveFactoryAdvisor {

	public ActivityStreamPerspectiveFactoryAdvisor() {
	}

	@Override
	public String getViewPartId() {
		return "com.ibm.socialcrm.notesintegration.activitystream.ActivityStreamView"; //$NON-NLS-1$
	}
}

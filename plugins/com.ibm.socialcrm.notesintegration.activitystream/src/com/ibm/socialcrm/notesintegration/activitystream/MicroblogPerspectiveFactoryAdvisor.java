package com.ibm.socialcrm.notesintegration.activitystream;

public class MicroblogPerspectiveFactoryAdvisor extends AbstractActivityStreamPerspectiveFactoryAdvisor {

	@Override
	public String getViewPartId() {
		return "com.ibm.socialcrm.notesintegration.activitystream.MicroblogView"; //$NON-NLS-1$
	}

}

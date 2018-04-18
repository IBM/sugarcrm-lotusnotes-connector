package com.ibm.socialcrm.notesintegration.ui.extensionpoints;

import com.ibm.socialcrm.notesintegration.ui.advisor.ISFAPerspectiveFactoryAdvisor;

public class SFAPerspectiveFactoryAdvisorContribution {

	private String viewId;
	private ISFAPerspectiveFactoryAdvisor advisor;

	public SFAPerspectiveFactoryAdvisorContribution() {
	}

	public String getViewId() {
		return viewId;
	}

	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	public ISFAPerspectiveFactoryAdvisor getAdvisor() {
		return advisor;
	}

	public void setAdvisor(ISFAPerspectiveFactoryAdvisor advisor) {
		this.advisor = advisor;
	}

}

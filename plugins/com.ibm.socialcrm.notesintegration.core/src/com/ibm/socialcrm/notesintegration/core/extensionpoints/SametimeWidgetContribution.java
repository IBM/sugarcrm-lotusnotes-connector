package com.ibm.socialcrm.notesintegration.core.extensionpoints;

/****************************************************************
 * IBM Confidential
 *
 * SFA050-Collaboration Source Materials
 *
 * (C) Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office
 *
 ***************************************************************/

import org.osgi.framework.Bundle;

public class SametimeWidgetContribution {
	private String builderClass;
	private Bundle bundle;

	public void setBuilderClass(String builderClass) {
		this.builderClass = builderClass;
	}

	public String getBuilderClass() {
		return builderClass;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return bundle;
	}
}

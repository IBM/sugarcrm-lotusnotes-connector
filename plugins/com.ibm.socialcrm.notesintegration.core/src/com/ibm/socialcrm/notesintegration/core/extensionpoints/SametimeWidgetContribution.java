package com.ibm.socialcrm.notesintegration.core.extensionpoints;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

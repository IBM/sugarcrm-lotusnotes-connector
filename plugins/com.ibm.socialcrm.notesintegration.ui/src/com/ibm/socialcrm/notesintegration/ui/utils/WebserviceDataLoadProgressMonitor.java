package com.ibm.socialcrm.notesintegration.ui.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

/**
 * There are a number of places in the cards where we have internal links to other sugar objects. If those objects aren't already loaded, we need to call the sugar webservices to get them. This can
 * take a non-trivial amount of time and we want to alert the user that something is going on.
 * 
 * This is a very specialized progress monitor design to work with an AbstractDashboardComposite.
 */
public class WebserviceDataLoadProgressMonitor extends NullProgressMonitor {
	private String progressId;
	private String message;
	private AbstractDashboardComposite dashboardComposite;

	public WebserviceDataLoadProgressMonitor(AbstractDashboardComposite dashboardComposite, String message) {
		setDashboardComposite(dashboardComposite);
		setMessage(message);
	}

	@Override
	public void beginTask(String name, int totalWork) {
		if (getDashboardComposite() != null) {
			String id = getDashboardComposite().getProgressDisplayer().createProgressIndicator(getMessage());
			setProgressId(id);
		}
	}

	@Override
	public void done() {
		if (getDashboardComposite() != null) {
			getDashboardComposite().getProgressDisplayer().removeProgressIndicator(getProgressId());
		}
	}

	private String getProgressId() {
		if (progressId == null) {
			progressId = ConstantStrings.EMPTY_STRING;
		}
		return progressId;
	}

	private void setProgressId(String progressId) {
		this.progressId = progressId;
	}

	public String getMessage() {
		if (message == null) {
			message = ConstantStrings.EMPTY_STRING;
		}
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public AbstractDashboardComposite getDashboardComposite() {
		return dashboardComposite;
	}

	public void setDashboardComposite(AbstractDashboardComposite dashboardComposite) {
		this.dashboardComposite = dashboardComposite;
	}

}

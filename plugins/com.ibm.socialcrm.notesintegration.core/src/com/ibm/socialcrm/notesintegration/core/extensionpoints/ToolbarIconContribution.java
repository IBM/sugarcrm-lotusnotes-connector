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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;

import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * Java object that represents a toolbar icon contribution for a sugar dashboard.
 * 
 * @author hipatel
 */
public class ToolbarIconContribution {
	private String id;

	private String enabledIcon;
	private String disabledIcon;
	private String actionClass;
	private int weight;
	private List<SugarType> sugarTypes;
	private List<String> dashboardIds;

	private Bundle bundle;

	public ToolbarIconContribution() {

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public String getActionClass() {
		return actionClass;
	}

	public void setActionClass(String actionClass) {
		this.actionClass = actionClass;
	}

	public String getEnabledIcon() {
		return enabledIcon;
	}

	public void setEnabledIcon(String enabledIcon) {
		this.enabledIcon = enabledIcon;
	}

	public String getDisabledIcon() {
		return disabledIcon;
	}

	public void setDisabledIcon(String disabledIcon) {
		this.disabledIcon = disabledIcon;
	}

	public List<SugarType> getSugarTypes() {
		if (sugarTypes == null) {
			sugarTypes = new ArrayList<SugarType>();
		}
		return sugarTypes;
	}

	public List<String> getDashboardIds() {
		if (dashboardIds == null) {
			dashboardIds = new ArrayList<String>();
		}
		return dashboardIds;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj != null && obj instanceof ToolbarIconContribution) {
			return getId().equals(((ToolbarIconContribution) obj).getId());
		}
		return equals;
	}

}

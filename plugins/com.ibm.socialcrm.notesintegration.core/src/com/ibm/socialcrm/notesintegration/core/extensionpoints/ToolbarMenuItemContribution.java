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
 * Java object that represents a toolbar menu item contribution for a sugar dashboard.
 * 
 * @author hipatel
 */
public class ToolbarMenuItemContribution {
	private String id;

	private String actionClass;
	private int weight;
	private List<SugarType> sugarTypes;
	private List<String> dashboardIds;
	private String categoryId;
	private Bundle bundle;
	private boolean visibleWhenDocked = true;
	private boolean visibleWhenUndocked = true;

	public ToolbarMenuItemContribution() {
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

	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj != null && obj instanceof ToolbarMenuItemContribution) {
			return getId().equals(((ToolbarMenuItemContribution) obj).getId());
		}
		return equals;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public boolean isVisibleWhenDocked() {
		return visibleWhenDocked;
	}

	public void setVisibleWhenDocked(boolean visibleWhenDocked) {
		this.visibleWhenDocked = visibleWhenDocked;
	}

	public boolean isVisibleWhenUndocked() {
		return visibleWhenUndocked;
	}

	public void setVisibleWhenUndocked(boolean visibleWhenUndocked) {
		this.visibleWhenUndocked = visibleWhenUndocked;
	}

	@Override
	public String toString() {
		return getActionClass();
	}

}

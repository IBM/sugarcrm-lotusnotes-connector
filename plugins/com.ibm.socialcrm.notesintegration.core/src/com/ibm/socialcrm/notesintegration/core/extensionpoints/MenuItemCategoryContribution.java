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

/**
 * Java object that represents a menu item category
 * 
 * @author hipatel
 */
public class MenuItemCategoryContribution {
	private String id;
	private int weight;

	private Bundle bundle;

	public MenuItemCategoryContribution() {

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

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj != null && obj instanceof MenuItemCategoryContribution) {
			return getId().equals(((MenuItemCategoryContribution) obj).getId());
		}
		return equals;
	}

}

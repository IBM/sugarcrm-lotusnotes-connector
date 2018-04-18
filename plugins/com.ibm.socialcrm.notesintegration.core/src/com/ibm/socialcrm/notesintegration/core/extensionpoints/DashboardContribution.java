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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * Java object that represents a sugar dashboard contribution
 * 
 * @author bcbull
 */
public class DashboardContribution implements Comparable<DashboardContribution> {
	private String id;

	private int weight;
	private String viewClass;
	private List<SugarType> applicableTypes;
	private String dockedDisplayName;
	private String displayName;
	private String viewPartId;

	private Bundle bundle;

	public DashboardContribution() {

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

	public String getViewClass() {
		return viewClass;
	}

	public void setViewClass(String viewClass) {
		this.viewClass = viewClass;
	}

	public List<SugarType> getApplicableTypes() {
		if (applicableTypes == null) {
			applicableTypes = new ArrayList<SugarType>();
		}
		return applicableTypes;
	}

	public String getDockedDisplayName() {
		if (dockedDisplayName == null) {
			dockedDisplayName = ConstantStrings.EMPTY_STRING;
		}
		return dockedDisplayName;
	}

	public void setDockedDisplayName(String dockedDisplayName) {
		this.dockedDisplayName = dockedDisplayName;
	}

	public String getDisplayName() {
		if (displayName == null) {
			displayName = ConstantStrings.EMPTY_STRING;
		}
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public String getViewPartId() {
		return viewPartId;
	}

	public void setViewPartId(String viewPartId) {
		this.viewPartId = viewPartId;
	}

	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj != null && obj instanceof DashboardContribution) {
			return getId().equals(((DashboardContribution) obj).getId());
		}
		return equals;
	}

	@Override
	public int compareTo(DashboardContribution other) {
		return getWeight() - other.getWeight();
	}

}

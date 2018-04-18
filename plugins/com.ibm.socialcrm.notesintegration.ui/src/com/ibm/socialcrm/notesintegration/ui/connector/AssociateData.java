package com.ibm.socialcrm.notesintegration.ui.connector;

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

/**
 * This Object contains one association information for an item: Account/Opportunity/Contact. See AssociateDataMap class for more information.
 * 
 */
public class AssociateData {
	private String name = null;
	private String id = null;
	private String extended = null;
	private boolean associated;

	public AssociateData() {
	}

	public AssociateData(String name, String extended, String id, boolean associated) {
		this.name = name;
		this.extended = extended; // mainly used for association update. For example: client Id when association with a contact.
		this.id = id;
		this.associated = associated;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String n) {
		this.name = n;
	}

	public String getExtended() {
		return this.extended;
	}

	public void setExtended(String n) {
		this.extended = n;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isAssociated() {
		return this.associated;
	}

	public void setAssociated(boolean b) {
		this.associated = b;
	}

	public String toString() {
		return String.format("[ConnectorItem: name='%s', id=%s, associated=%s]", name, id, Boolean.valueOf(associated)); //$NON-NLS-1$
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		boolean equals = false;
		if (other != null && other instanceof AssociateData) {
			equals = getName().equals(((AssociateData) other).getName());
		}
		return equals;
	}

}

package com.ibm.socialcrm.notesintegration.core;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class ConnectionsFolder {
	private String folderId;
	private String folderName;
	private SugarType type;
	private boolean expanded = true;

	public String getFolderId() {
		if (folderId == null) {
			folderId = ConstantStrings.EMPTY_STRING;
		}
		return folderId;
	}

	public String getFolderName() {
		if (folderName == null) {
			folderName = ConstantStrings.EMPTY_STRING;
		}
		// TODO: This is a temporary hack
		folderName = folderName.replaceAll(ConstantStrings.GREATER_THAN, ConstantStrings.EMPTY_STRING);
		return folderName;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public SugarType getType() {
		if (type == null) {
			type = SugarType.NONE;
		}
		return type;
	}

	public void setType(SugarType type) {
		this.type = type;
	}

	public String toString() {
		return getFolderName();
	}

	// ---------------------------------------------
	// This is a little crappy, but we're stuffing the UI expansion state in here
	// so we can remember it as the user moves between tabs.
	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	// ---------------------------------------------

	@Override
	public boolean equals(Object other) {
		boolean equals = false;
		if (other != null && other instanceof ConnectionsFolder) {
			ConnectionsFolder otherFolder = (ConnectionsFolder) other;
			equals = this.getFolderId().equals(otherFolder.getFolderId()) && this.getFolderName().equals(otherFolder.getFolderName()) && this.getType().equals(otherFolder.getType());
		}
		return equals;
	}

	@Override
	public int hashCode() {
		return this.getFolderId().hashCode();
	}

}

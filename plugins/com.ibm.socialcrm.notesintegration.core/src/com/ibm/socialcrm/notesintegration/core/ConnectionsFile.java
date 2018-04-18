package com.ibm.socialcrm.notesintegration.core;

/****************************************************************
 * IBM Confidential
 * 
 * SFA050-Collaboration Source Materials
 * 
 * (C) Copyright IBM Corp. 2012
 * 
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office
 * 
 ***************************************************************/

public class ConnectionsFile {
	private String id;
	private String filename;

	public ConnectionsFile(String id, String filename) {
		this.id = id;
		this.filename = filename;
	}

	public String getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}

	public String toString() {
		return getFilename();
	}

	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj != null && obj instanceof ConnectionsFile) {
			equals = getId().equals(((ConnectionsFile) obj).getId());
		}
		return equals;
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

}
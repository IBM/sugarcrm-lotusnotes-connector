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
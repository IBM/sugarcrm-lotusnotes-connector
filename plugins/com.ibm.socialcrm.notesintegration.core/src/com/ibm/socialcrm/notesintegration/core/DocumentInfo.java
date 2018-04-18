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
public class DocumentInfo {
	/*
	 * Information retrieved from the Connections server
	 */

	String documentName = null;
	String version = "1";  //$NON-NLS-1$
	String connectionsUUID = null;
	String sugarDocumentID = null;

	public DocumentInfo(String documentName, String versionI, String connectionsUUID) {
		this.documentName = documentName;
		this.version = versionI;
		this.connectionsUUID = connectionsUUID;
	}
	public String getDocumentName() {
		return documentName;
	}
	public String getVersion() {
		return version;
	}
	public String getConnectionsUUID() {
		return connectionsUUID;
	}
	public String getSugarDocumentID() {
		return sugarDocumentID;
	}
	public void setSugarDocumentID(String s) {
		sugarDocumentID = s;
	}

}
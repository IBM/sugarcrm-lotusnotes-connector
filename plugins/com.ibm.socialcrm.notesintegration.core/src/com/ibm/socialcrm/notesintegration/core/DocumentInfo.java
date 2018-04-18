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
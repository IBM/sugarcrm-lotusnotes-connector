package com.ibm.socialcrm.notesintegration.files.utils;

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

public class DocumentUploadObject {
	private String _fileName = null; // ofiginal file name
	private String _connectionsFileName = null; // if file was renamed
	private String _language = null;
	private String _uploadedDocumentName = null; // from upload web service response
	private String _uploadedDocumentSugarId = null; // from upload web service response
	private String _uploadMsg = null; // from upload web service response

	public DocumentUploadObject(String fileName, String connectionsFileName, String language) {
		_fileName = fileName;
		_connectionsFileName = connectionsFileName;
		_language = language;

	}

	public DocumentUploadObject(String fileName, String connectionsFileName, String language, String uploadedDocumentSugarId) {
		_fileName = fileName;
		_connectionsFileName = connectionsFileName;
		_language = language;
		_uploadedDocumentSugarId = uploadedDocumentSugarId;
	}

	public String getFileName() {
		return _fileName;
	}

	public String getConnectionsFileName() {
		return _connectionsFileName;
	}

	public String getLanguage() {
		return _language;
	}

	public void setLanguage(String s) {
		_language = s;
	}

	public String getUploadedDocumentName() {
		return _uploadedDocumentName;
	}

	public void setUploadedDocumentName(String s) {
		_uploadedDocumentName = s;
	}

	public String getUploadedDocumentSugarId() {
		return _uploadedDocumentSugarId;
	}

	public void setUploadedDocumentSugarId(String s) {
		_uploadedDocumentSugarId = s;
	}

	public String getUploadMsg() {
		return _uploadMsg;
	}

	public void setUploadMsg(String s) {
		_uploadMsg = s;
	}

}

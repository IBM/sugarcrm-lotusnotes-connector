package com.ibm.socialcrm.notesintegration.files.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

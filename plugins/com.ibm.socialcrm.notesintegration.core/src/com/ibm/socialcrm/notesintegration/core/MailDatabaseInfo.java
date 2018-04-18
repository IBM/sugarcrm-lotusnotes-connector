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

public class MailDatabaseInfo {

	private static MailDatabaseInfo instance = null;
	private String mailDbReplicaId = null;

	private MailDatabaseInfo() {
	}

	public static MailDatabaseInfo getInstance() {
		if (instance == null) {
			instance = new MailDatabaseInfo();
		}
		return instance;
	}

	public String getMailDbReplicaId() {
		return mailDbReplicaId;
	}

	public void setMailDbReplicaId(String mailDbReplicaId) {
		this.mailDbReplicaId = mailDbReplicaId;
	}
}

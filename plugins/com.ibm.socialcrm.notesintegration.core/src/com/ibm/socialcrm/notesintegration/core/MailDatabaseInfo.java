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

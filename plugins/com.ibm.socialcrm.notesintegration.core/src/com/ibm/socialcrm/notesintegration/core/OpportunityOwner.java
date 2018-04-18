package com.ibm.socialcrm.notesintegration.core;

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

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class OpportunityOwner {
	private String email;
	private String name;

	public String getEmail() {
		if (email == null) {
			email = ConstantStrings.EMPTY_STRING;
		}
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		if (name == null) {
			name = ConstantStrings.EMPTY_STRING;
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public OpportunityOwner() {
	}

}

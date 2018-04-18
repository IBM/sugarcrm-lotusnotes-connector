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

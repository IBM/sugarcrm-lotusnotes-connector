package com.ibm.socialcrm.notesintegration.sametime.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import com.ibm.collaboration.realtime.people.Person;

public class Member {
	private String id;
	private String name;
	private String email;
	private Person person;

	public Member(String id, String name, String email) {
		setId(id);
		setName(name);
		setEmail(email);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Person getSametimePerson() {
		if (person == null) {
			person = SametimeUtils.getSametimePerson(getEmail());
		}
		return person;
	}
}

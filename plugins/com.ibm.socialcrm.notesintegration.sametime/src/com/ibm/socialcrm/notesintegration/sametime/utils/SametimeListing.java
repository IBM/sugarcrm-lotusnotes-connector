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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.collaboration.realtime.people.Person;

/**
 * This class represents a collection of accounts and opportunities that will be displayed in a custom sametime viewer
 * 
 * @author bcbull
 */
public class SametimeListing {
	private List<Account> accounts;
	private List<Opportunity> opportunities;

	public List<Account> getAccounts() {
		if (accounts == null) {
			accounts = new ArrayList<Account>();
		}
		return accounts;
	}

	public List<Opportunity> getOpportunities() {
		if (opportunities == null) {
			opportunities = new ArrayList<Opportunity>();
		}
		return opportunities;
	}

	/**
	 * Returns a list of all the Person objects in this listing
	 * 
	 * @return
	 */
	public Set<Person> recursiveGetSametimePersons() {
		Set<Person> set = new HashSet<Person>();

		for (Account account : getAccounts()) {
			set.addAll(account.recursiveGetSametimePersons());
		}

		for (Opportunity opportunity : getOpportunities()) {
			set.addAll(opportunity.recursiveGetSametimePersons());
		}
		return set;
	}
}

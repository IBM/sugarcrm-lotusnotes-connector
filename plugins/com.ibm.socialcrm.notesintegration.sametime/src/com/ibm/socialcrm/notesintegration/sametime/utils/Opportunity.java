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
import java.util.List;

import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class Opportunity extends AbstractSametimeGroup {
	private String id;
	private String accountId;
	private String description;

	private OpportunityOwners opportunityOwners;
	private OpportunityTeam opportunityTeam;

	public Opportunity(String id, String name, String accountId, String description) {
		super(name);
		setId(id);
		setAccountId(accountId);
		setDescription(description);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public List getSubGroups() {
		List subGroups = new ArrayList();
		subGroups.add(getOpportunityOwners());
		subGroups.add(getOpportunityTeam());
		return subGroups;
	}

	public OpportunityOwners getOpportunityOwners() {
		if (opportunityOwners == null) {
			opportunityOwners = new OpportunityOwners();
		}
		return opportunityOwners;
	}

	public OpportunityTeam getOpportunityTeam() {
		if (opportunityTeam == null) {
			opportunityTeam = new OpportunityTeam();
		}
		return opportunityTeam;
	}

	class OpportunityOwners extends AbstractSametimeGroup {
		public OpportunityOwners() {
			super(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_OPPORTUNITY_OWNER));
		}
	}

	class OpportunityTeam extends AbstractSametimeGroup {
		public OpportunityTeam() {
			super(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_OPPORTUNITY_TEAM));
		}
	}

}

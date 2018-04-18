package com.ibm.socialcrm.notesintegration.sametime.utils;

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

import java.util.ArrayList;
import java.util.List;

import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class Account extends AbstractSametimeGroup {
	private String id;
	private AccountTeamMembers accountTeamMembers;

	public Account(String id, String name) {
		super(name);
		setId(id);
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public List getSubGroups() {
		List subGroups = new ArrayList();
		subGroups.add(getAccountTeamMembers());
		subGroups.addAll(getOpportunitiesForCurrentListing());
		return subGroups;
	}

	@Override
	public boolean hasSubGroups() {
		return getOpportunitiesForCurrentListing().size() > 0 || getAccountTeamMembers().getMembers().size() > 0;
	}

	/**
	 * Helper method to determine whether there are sub groups for this Sametime group. This list maybe different based on the current Notes document selection, since we don't want to show all the
	 * opportunities for an account.
	 * 
	 * @return
	 */
	private List<Opportunity> getOpportunitiesForCurrentListing() {
		List<Opportunity> opportunities = new ArrayList<Opportunity>();
		if (SametimeInfo.getInstance().getCurrentSametimeListing() != null) {
			for (Opportunity opp : SametimeInfo.getInstance().getCurrentSametimeListing().getOpportunities()) {
				if (opp != null && opp.getAccountId() != null && opp.getAccountId().equals(getId())) {
					opportunities.add(opp);
				}
			}
		}
		return opportunities;
	}

	public AccountTeamMembers getAccountTeamMembers() {
		if (accountTeamMembers == null) {
			accountTeamMembers = new AccountTeamMembers();
		}
		return accountTeamMembers;
	}

	class AccountTeamMembers extends AbstractSametimeGroup {
		public AccountTeamMembers() {
			super(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_ACCOUNT_TEAM_MEMBERS));
		}
	}

}
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.sametime.SametimePluginActivator;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class SametimeInfo {
	private List<Account> accounts;
	private List<Member> members;
	private List<Opportunity> opportunities;

	private SametimeListing currentSametimeListing;

	private List<String> opportunityIdsNotInSugar;

	private static SametimeInfo instance;

	private SametimeInfo() {
	}

	public Member getMemberById(String id) {
		Member member = null;
		for (Member m : getMembers()) {
			if (m.getId().equals(id)) {
				member = m;
				break;
			}
		}
		return member;
	}

	public Account getAccountById(String id) {
		Account account = null;
		for (Account a : getAccounts()) {
			if (a.getId().equals(id)) {
				account = a;
				break;
			}
		}
		return account;
	}

	public Opportunity getOpportunityById(String id) {
		Opportunity opportunity = null;
		for (Opportunity o : getOpportunities()) {
			if (o.getId().equals(id)) {
				opportunity = o;
				break;
			}
		}
		return opportunity;
	}

	private void processJsonDataFromWebservice(JSONObject jsonObject) {
		try {
			if (jsonObject.containsKey(ConstantStrings.MEMBERS)) {
				JSONObject membersJson = jsonObject.getJSONObject(ConstantStrings.MEMBERS);
				Iterator<String> iter = membersJson.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					JSONObject memberJson = membersJson.getJSONObject(key);
					Member oldMember = getMemberById(key);
					Member m = new Member(key, (String) memberJson.get(ConstantStrings.DATABASE_NAME), (String) memberJson.get(ConstantStrings.USER_NAME));
					if (oldMember == null) {
						getMembers().add(m);
					} else {
						getMembers().remove(oldMember);
						getMembers().add(m);
					}
				}
			}

			if (jsonObject.containsKey(ConstantStrings.ACCOUNTS)) {
				JSONObject accountsJson = jsonObject.getJSONObject(ConstantStrings.ACCOUNTS);
				Iterator<String> iter = accountsJson.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					JSONObject accountJson = accountsJson.getJSONObject(key);
					Account a = getAccountById(key);
					if (a == null) {
						a = new Account(key, (String) accountJson.get(ConstantStrings.DATABASE_NAME));
					} else {
						a.getAccountTeamMembers().clear();
					}
					JSONArray accountMembersJson = accountJson.getJSONArray(ConstantStrings.MEMBERS);
					for (int j = 0; j < accountMembersJson.length(); j++) {
						JSONObject accountMemberJson = accountMembersJson.getJSONObject(j);
						String id = accountMemberJson.getString(ConstantStrings.DATABASE_ID);
						Member m = getMemberById(id);
						if (m != null) {
							a.getAccountTeamMembers().addMember(m);
						}
					}
					getAccounts().add(a);
				}
			}
			if (jsonObject.containsKey(ConstantStrings.OPPORTUNITIES)) {
				JSONObject opportunitiesJson = jsonObject.getJSONObject(ConstantStrings.OPPORTUNITIES);
				Iterator<String> iter = opportunitiesJson.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					JSONObject opportunityJson = opportunitiesJson.getJSONObject(key);
					SugarOpportunity opportunity = (SugarOpportunity) SugarWebservicesOperations.getInstance().getSugarEntryById(key);
					Opportunity opp = new Opportunity(key, (String) opportunityJson.get(ConstantStrings.DATABASE_NAME), (String) opportunityJson.get(ConstantStrings.DATABASE_ACCOUNT_ID),
							(String) opportunityJson.get(ConstantStrings.DATABASE_DESCRIPTION));
					JSONArray oppMembersArray = opportunityJson.getJSONArray(ConstantStrings.MEMBERS);
					for (int j = 0; j < oppMembersArray.length(); j++) {
						JSONObject accountMemberJson = oppMembersArray.getJSONObject(j);
						String id = accountMemberJson.getString(ConstantStrings.DATABASE_ID);
						Member m = getMemberById(id);
						if (m != null) {
							if (opportunity != null && opportunity.getAssignedUserID() != null && opportunity.getAssignedUserID().equals(id)) {
								opp.getOpportunityOwners().addMember(m);
							} else {
								opp.getOpportunityTeam().addMember(m);
							}
						}
					}
					Opportunity oldOppty = getOpportunityById(key);
					if (oldOppty == null) {
						getOpportunities().add(opp);
					} else {
						getOpportunities().remove(oldOppty);
						getOpportunities().add(opp);
					}
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
		}
	}

	public SametimeListing createSametimeListing(BaseSugarEntry sugarEntry) {
		currentSametimeListing = new SametimeListing();

		if (sugarEntry != null) {
			// First check to see which items we need to pull from the webservice.
			getUpdatesFromWebservice(sugarEntry);

			if (sugarEntry instanceof SugarAccount) {
				Account account = getAccountById(sugarEntry.getId());
				if (account != null && !currentSametimeListing.getAccounts().contains(account)) {
					currentSametimeListing.getAccounts().add(account);
				}
			} else if (sugarEntry instanceof SugarOpportunity) {
				Opportunity opportunity = getOpportunityById(sugarEntry.getId());
				if (opportunity != null) {
					if (!currentSametimeListing.getOpportunities().contains(opportunity)) {
						currentSametimeListing.getOpportunities().add(opportunity);
					}
					Account account = getAccountById(opportunity.getAccountId());
					if (account != null && !currentSametimeListing.getAccounts().contains(account)) {
						currentSametimeListing.getAccounts().add(account);
					}
				}
			}
		}
		return currentSametimeListing;
	}

	private void getUpdatesFromWebservice(BaseSugarEntry sugarEntry) {
		List<String> accounts = new ArrayList<String>();
		List<String> opportunities = new ArrayList<String>();

		if (sugarEntry instanceof SugarAccount) {
			// Account account = getAccountById(sugarEntry.getId());
			// if (account == null)
			// {
			accounts.add(sugarEntry.getId());
			// }
		} else if (sugarEntry instanceof SugarOpportunity) {
			// Opportunity opportunity = getOpportunityById(sugarEntry.getId());
			// if (opportunity == null && !getOpportunityIdsNotInSugar().contains(sugarEntry.getId()))
			if (!getOpportunityIdsNotInSugar().contains(sugarEntry.getId())) {
				opportunities.add(sugarEntry.getId());
			}
		}

		if (!accounts.isEmpty() || !opportunities.isEmpty()) {
			String output = SugarWebservicesOperations.getInstance().getSametimeInfoFromWebservice(accounts, opportunities, null);
			if (output != null && !output.equals("[]")) //$NON-NLS-1$
			{
				try {
					processJsonDataFromWebservice(new JSONObject(output));
					// Check if we got entries back for the requested opportunities. If not, then include them in opportunityIdsNotInSugar so we don't check for them again.
					for (String opportunity : opportunities) {
						boolean addToList = getOpportunityById(opportunity) == null;
						if (addToList && !getOpportunityIdsNotInSugar().contains(opportunity)) {
							getOpportunityIdsNotInSugar().add(opportunity);
						}
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
				}
			}
		}
	}

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

	public List<Member> getMembers() {
		if (members == null) {
			members = new ArrayList<Member>();
		}
		return members;
	}

	public List<String> getOpportunityIdsNotInSugar() {
		if (opportunityIdsNotInSugar == null) {
			opportunityIdsNotInSugar = new ArrayList<String>();
		}
		return opportunityIdsNotInSugar;
	}

	public SametimeListing getCurrentSametimeListing() {
		return currentSametimeListing;
	}

	public static SametimeInfo getInstance() {
		if (instance == null) {
			instance = new SametimeInfo();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					// SugarItemsDashboard.getInstance().getSugarItemsDashboardShell().addDisposeListener(new DisposeListener() {
					// @Override
					// public void widgetDisposed(DisposeEvent arg0) {
					// instance = null;
					// }
					// });
				}
			});
		}
		return instance;
	}
}

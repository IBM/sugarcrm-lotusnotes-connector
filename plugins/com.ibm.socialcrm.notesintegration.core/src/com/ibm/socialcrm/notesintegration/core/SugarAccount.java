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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SugarAccount extends BaseSugarEntry {
	private List<String> opportunityIDs;
	private Map<String, String> industryMap;
	// 80623 - ci simplification
	private String indus_industry;

	private String clientId;
	private String primaryContactId;
	private String fax;
	private int totalOpportunities;

	// 16845
	private boolean isParent; // true if non-site (DC/SC), 'NA' if not
	private int numOfRelatedClients; // number of child accounts a non-site account has, 'NA' if account is sIte
	private String following_parent_ccms_id; // DC/SC ccms id user is following if this is a site client, 'NA' otherwise
	private String following_parent_name; // DC/SC name user is following if this is a site client, 'NA' otherwise
	private String following_parent_link; // link to a DC/SC user is following if this is a site client, 'NA' otherwise
	private String following_parent_id; // DC/SC sugar id user is following if this is a site client, 'NA' otherwise

	public SugarAccount(String id, String name) {
		super(id, name);
	}

	public List<String> getOpportunityIDs() {
		if (opportunityIDs == null) {
			opportunityIDs = new ArrayList<String>();
		}
		return opportunityIDs;
	}

	public Map<String, String> getIndustryMap() {
		if (industryMap == null) {
			industryMap = new HashMap<String, String>();
		}
		return industryMap;
	}

	public String getIndustryString() {
		String industryString = ConstantStrings.EMPTY_STRING;
		Collection<String> industries = getIndustryMap().values();
		if (null != industries) {
			if (industries.size() > 1) {
				industryString = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MULTIPLE_INDUSTRIES);
			} else if (industries.size() == 1) {
				industryString = industries.iterator().next();
			}
		}

		return industryString;
	}

	public String getIndustryTooltip() {
		String industryTooltip = ConstantStrings.EMPTY_STRING;
		
		// 80623 - CI simplification, new field as key for industry lookup
		industryTooltip = getIndustryString();

		return industryTooltip;
	}

	public void setIndustryMap(Map<String, String> map) {
		this.industryMap = map;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getPrimaryContactId() {
		return primaryContactId;
	}

	public void setPrimaryContactId(String primaryContactId) {
		this.primaryContactId = primaryContactId;
	}

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	@Override
	public String getSugarUrl() {
		String aUrl = NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Accounts&action=DetailView&record=" + getId(); //$NON-NLS-1$ 
		if (GenericUtils.isUseEmbeddedBrowserPreferenceSet())
			return aUrl;
		else
			return SugarWebservicesOperations.getInstance().buildV10SeamlessURL(aUrl);
	}

	@Override
	public SugarType getSugarType() {
		return SugarType.ACCOUNTS;
	}

	public int getTotalOpportunities() {
		return totalOpportunities;
	}

	public void setTotalOpportunities(int totalOpportunities) {
		this.totalOpportunities = totalOpportunities;
	}

	// 16845
	public void setIsParent(boolean b) {
		isParent = b;
	}
	public boolean isParent() {
		return isParent;
	}
	public void setNumOfRelatedClients(int i) {
		numOfRelatedClients = i;
	}
	public int getNumOfRelatedClients() {
		return numOfRelatedClients;
	}
	public void setFollowingParentCCMSId(String s) {
		following_parent_ccms_id = s;
	}
	public String getFollowingParentCCMSId() {
		return following_parent_ccms_id;
	}
	public void setFollowingParentName(String s) {
		following_parent_name = s;
	}
	public String getFollowingParentName() {
		return following_parent_name;
	}
	public void setFollowingParentLink(String s) {
		following_parent_link = s;
	}
	public String getFollowingParentLink() {
		return following_parent_link;
	}
	public void setFollowingParentId(String s) {
		following_parent_id = s;
	}
	public String getFollowingParentId() {
		return following_parent_id;
	}

	public String getIndusIndustry() {
		return indus_industry;
	}

	public void setIndusIndustry(String indusIndustry) {
		this.indus_industry = indusIndustry;
	}

}
